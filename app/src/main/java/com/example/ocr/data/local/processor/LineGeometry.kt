package com.example.ocr.data.local.processor

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Pure-Kotlin geometry types so the line-grouping logic can be unit tested on the JVM
data class Pt(val x: Int, val y: Int)

data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Int get() = width * height

    // Same semantics as android.graphics.Rect.contains(Rect)
    fun contains(other: Box): Boolean =
        left < right && top < bottom &&
            left <= other.left && top <= other.top &&
            right >= other.right && bottom >= other.bottom

    fun union(other: Box) = Box(
        min(left, other.left), min(top, other.top),
        max(right, other.right), max(bottom, other.bottom),
    )
}

/** A detected text line: [corners] are TL, TR, BR, BL when available. */
data class DetectedLine(
    val text: String,
    val box: Box,
    val corners: List<Pt>? = null,
) {
    val quad: List<Pt>? get() = corners?.takeIf { it.size == 4 }
}

object LineGeometry {

    const val MERGE_GAP_X = 50
    const val STRAIGHT_OVERLAP_MIN = 0.80f
    const val TILT_THRESHOLD = 4
    const val TILT_JOIN_TOLERANCE = 0.25f

    fun isValidTextLine(line: DetectedLine, imgW: Int, imgH: Int): Boolean {
        val box = line.box
        val text = line.text.trim()
        if (text.length < 2) return false
        if (!text.any { it.isLetterOrDigit() }) return false
        if (box.height < 8 || box.height > imgH / 3) return false
        if (box.width < imgW * 0.03f) return false
        val alphaRatio = text.count { it.isLetterOrDigit() }.toFloat() / text.length
        return alphaRatio >= 0.3f
    }

    /** Drops lines whose box lies entirely inside a larger, already kept box. */
    fun filterStrictlyContained(lines: List<DetectedLine>): List<DetectedLine> {
        val result = mutableListOf<DetectedLine>()
        for (current in lines.sortedByDescending { it.box.area }) {
            if (result.none { it.box.contains(current.box) }) result.add(current)
        }
        return result
    }

    /** Groups horizontally adjacent fragments that belong to the same visual line. */
    fun mergeLines(lines: List<DetectedLine>): List<List<DetectedLine>> {
        val sorted = lines.sortedWith(compareBy<DetectedLine> { it.box.left }.thenBy { it.box.top })
        val groups = mutableListOf<MutableList<DetectedLine>>()
        outer@ for (line in sorted) {
            for (group in groups) {
                if (canMerge(group.last(), line)) {
                    group.add(line)
                    continue@outer
                }
            }
            groups.add(mutableListOf(line))
        }
        return groups
    }

    /** Reading order: top-to-bottom (10px bands), then left-to-right. */
    fun sortReadingOrder(groups: List<List<DetectedLine>>): List<List<DetectedLine>> =
        groups.sortedWith(
            compareBy<List<DetectedLine>> { g -> (g.minOfOrNull { it.box.top } ?: 0) / 10 }
                .thenBy { g -> g.minOfOrNull { it.box.left } ?: 0 }
        )

    fun canMerge(left: DetectedLine, right: DetectedLine): Boolean {
        val leftBox = left.box
        val rightBox = right.box

        val gapX = rightBox.left - leftBox.right
        if (gapX > MERGE_GAP_X) return false
        if (gapX < -leftBox.width * 0.5f) return false

        val avgHeight = (leftBox.height + rightBox.height) / 2f
        if (avgHeight < 1f) return false

        val isStraight = tilt(left) <= TILT_THRESHOLD && tilt(right) <= TILT_THRESHOLD
        return if (isStraight) {
            checkStraightOverlap(leftBox, rightBox, avgHeight)
        } else {
            checkTiltedJoin(left, right, avgHeight)
        }
    }

    /** Bounding quad (TL, TR, BR, BL) of a group, or null if any line lacks corners. */
    fun mergedQuad(group: List<DetectedLine>): List<Pt>? {
        val quads = group.mapNotNull { it.quad }
        if (quads.isEmpty() || quads.size != group.size) return null
        return listOf(
            quads.minBy { it[0].x + it[0].y }[0],
            quads.maxBy { it[1].x - it[1].y }[1],
            quads.maxBy { it[2].x + it[2].y }[2],
            quads.minBy { it[3].x - it[3].y }[3],
        )
    }

    private fun tilt(line: DetectedLine): Int =
        line.quad?.let { abs(it[0].y - it[1].y) } ?: 0

    private fun checkStraightOverlap(leftBox: Box, rightBox: Box, avgHeight: Float): Boolean {
        val overlap = (min(leftBox.bottom, rightBox.bottom) - max(leftBox.top, rightBox.top))
            .coerceAtLeast(0)
        return overlap >= avgHeight * STRAIGHT_OVERLAP_MIN
    }

    private fun checkTiltedJoin(left: DetectedLine, right: DetectedLine, avgHeight: Float): Boolean {
        val lc = left.quad
        val rc = right.quad
        if (lc == null || rc == null) return checkStraightOverlap(left.box, right.box, avgHeight)

        val leftRightEdgeMidY = (lc[1].y + lc[2].y) / 2f
        val rightLeftEdgeMidY = (rc[0].y + rc[3].y) / 2f
        return abs(leftRightEdgeMidY - rightLeftEdgeMidY) <= avgHeight * TILT_JOIN_TOLERANCE
    }
}
