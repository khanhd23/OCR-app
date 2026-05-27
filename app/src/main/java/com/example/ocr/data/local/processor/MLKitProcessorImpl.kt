package com.example.ocr.data.local.processor

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class MLKitProcessorImpl @Inject constructor() {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class LineResult(
        val warpedBitmap: Bitmap,
        val boundingBox: Rect,
    )

    private val PAD_LEFT   = 60
    private val PAD_RIGHT  = 20
    private val PAD_TOP    = 20
    private val PAD_BOTTOM = 20

    private val MERGE_GAP_X          = 50
    private val STRAIGHT_OVERLAP_MIN = 0.80f
    private val TILT_THRESHOLD       = 4
    private val TILT_JOIN_TOLERANCE  = 0.25f

    // Public API
    suspend fun detectAndWarpLines(bitmap: Bitmap): List<LineResult> {
        val results = detectFromBitmap(bitmap, scaleFactor = 1f)
        if (results.isNotEmpty()) return results
        val scaled = bitmap.scale(bitmap.width * 2, bitmap.height * 2)
        val results2x = detectFromBitmap(scaled, scaleFactor = 2f)
        scaled.recycle()
        return results2x
    }

    suspend fun detectLineRects(bitmap: Bitmap): List<Rect> =
        detectAndWarpLines(bitmap).map { it.boundingBox }

    suspend fun warmup() {
        val bmp = createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        detectLineRects(bmp)
        bmp.recycle()
    }

    // Core pipeline
    private suspend fun detectFromBitmap(bitmap: Bitmap, scaleFactor: Float): List<LineResult> {
        val imgW = bitmap.width
        val imgH = bitmap.height

        val mlResult = try {
            recognizeText(InputImage.fromBitmap(bitmap, 0))
        } catch (e: Exception) {
            return emptyList()
        }

        val rawLines = mlResult.textBlocks
            .flatMap { it.lines }
            .filter { isValidTextLine(it, imgW, imgH) }

        val filtered = filterStrictlyContainedBoxes(rawLines)
        val merged   = mergeLines(filtered)

        val sorted = merged.sortedWith(
            compareBy<List<Text.Line>> {
                it.mapNotNull { l -> l.boundingBox?.top }.minOrNull()?.div(10) ?: 0
            }.thenBy {
                it.mapNotNull { l -> l.boundingBox?.left }.minOrNull() ?: 0
            }
        )

        return sorted.mapNotNull { buildLineResult(bitmap, it, scaleFactor) }
    }

    // Merge line
    private fun mergeLines(lines: List<Text.Line>): List<List<Text.Line>> {
        if (lines.isEmpty()) return emptyList()
        val sorted = lines.sortedWith(
            compareBy<Text.Line> { it.boundingBox?.left ?: 0 }
                .thenBy { it.boundingBox?.top ?: 0 }
        )

        val groups = mutableListOf<MutableList<Text.Line>>()

        outer@ for (line in sorted) {
            for (group in groups) {
                val last = group.last()
                if (canMerge(last, line)) {
                    group.add(line)
                    continue@outer
                }
            }
            groups.add(mutableListOf(line))
        }

        return groups
    }


    private fun canMerge(left: Text.Line, right: Text.Line): Boolean {
        val leftBox  = left.boundingBox  ?: return false
        val rightBox = right.boundingBox ?: return false

        val gapX = rightBox.left - leftBox.right
        if (gapX > MERGE_GAP_X) return false
        if (gapX < -leftBox.width() * 0.5f) return false

        val avgHeight = ((leftBox.height()) + (rightBox.height())) / 2f
        if (avgHeight < 1f) return false

        val leftCorners  = left.cornerPoints
        val rightCorners = right.cornerPoints

        val leftTilt  = if (leftCorners  != null && leftCorners.size  == 4) abs(leftCorners[0].y  - leftCorners[1].y)  else 0
        val rightTilt = if (rightCorners != null && rightCorners.size == 4) abs(rightCorners[0].y - rightCorners[1].y) else 0
        val isStraight = leftTilt <= TILT_THRESHOLD && rightTilt <= TILT_THRESHOLD

        return if (isStraight) {
            checkStraightOverlap(leftBox, rightBox, avgHeight)
        } else {
            checkTiltedJoin(leftBox, leftCorners, rightBox, rightCorners, avgHeight)
        }
    }


    private fun checkStraightOverlap(leftBox: Rect, rightBox: Rect, avgHeight: Float): Boolean {
        val overlapTop    = max(leftBox.top,    rightBox.top)
        val overlapBottom = min(leftBox.bottom, rightBox.bottom)
        val overlap       = (overlapBottom - overlapTop).coerceAtLeast(0)
        return overlap >= avgHeight * STRAIGHT_OVERLAP_MIN
    }

    private fun checkTiltedJoin(
        leftBox: Rect, leftCorners: Array<Point>?,
        rightBox: Rect, rightCorners: Array<Point>?,
        avgHeight: Float,
    ): Boolean {
        if (leftCorners == null || leftCorners.size != 4 ||
            rightCorners == null || rightCorners.size != 4) {
            return checkStraightOverlap(leftBox, rightBox, avgHeight)
        }

        val leftRightEdgeMidY  = (leftCorners[1].y  + leftCorners[2].y)  / 2f
        val rightLeftEdgeMidY  = (rightCorners[0].y + rightCorners[3].y) / 2f

        val yDiff = abs(leftRightEdgeMidY - rightLeftEdgeMidY)
        return yDiff <= avgHeight * TILT_JOIN_TOLERANCE
    }

    // Build LineResult
    private fun buildLineResult(
        bitmap: Bitmap,
        group: List<Text.Line>,
        scaleFactor: Float,
    ): LineResult? {
        if (group.isEmpty()) return null

        if (group.size == 1) {
            val line    = group[0]
            val box     = line.boundingBox ?: return null
            val corners = line.cornerPoints
            val warped  = if (corners != null && corners.size == 4)
                warpLinePerspective(bitmap, corners)
            else
                cropWithPad(bitmap, box)
            return LineResult(warpedBitmap = warped, boundingBox = scaleBox(box, scaleFactor))
        }

        val allBoxes = group.mapNotNull { it.boundingBox }
        val mergedBox = allBoxes.reduce { acc, r ->
            Rect(min(acc.left, r.left), min(acc.top, r.top), max(acc.right, r.right), max(acc.bottom, r.bottom))
        }

        val allCorners = group.mapNotNull { it.cornerPoints?.takeIf { c -> c.size == 4 } }

        val warped = if (allCorners.size == group.size) {
            val tl = allCorners.minBy { it[0].x + it[0].y }[0]
            val tr = allCorners.maxBy { it[1].x - it[1].y }[1]
            val br = allCorners.maxBy { it[2].x + it[2].y }[2]
            val bl = allCorners.minBy { it[3].x - it[3].y }[3]
            warpLinePerspective(bitmap, arrayOf(tl, tr, br, bl))
        } else {
            cropWithPad(bitmap, mergedBox)
        }

        return LineResult(warpedBitmap = warped, boundingBox = scaleBox(mergedBox, scaleFactor))
    }

    // Helpers
    private fun filterStrictlyContainedBoxes(lines: List<Text.Line>): List<Text.Line> {
        if (lines.isEmpty()) return emptyList()
        val sortedByArea = lines.sortedByDescending {
            val b = it.boundingBox ?: return@sortedByDescending 0
            b.width() * b.height()
        }
        val result = mutableListOf<Text.Line>()
        for (current in sortedByArea) {
            val currentBox = current.boundingBox ?: continue
            val isContained = result.any { it.boundingBox?.contains(currentBox) == true }
            if (!isContained) result.add(current)
        }
        return result
    }

    private fun isValidTextLine(line: Text.Line, imgW: Int, imgH: Int): Boolean {
        val box  = line.boundingBox ?: return false
        val text = line.text.trim()
        if (text.length < 2) return false
        if (!text.any { it.isLetterOrDigit() }) return false
        if (box.height() < 8 || box.height() > imgH / 3) return false
        if (box.width() < imgW * 0.03f) return false
        val alphaRatio = text.count { it.isLetterOrDigit() }.toFloat() / text.length
        return alphaRatio >= 0.3f
    }

    private fun scaleBox(box: Rect, scaleFactor: Float): Rect =
        if (scaleFactor == 1f) box else Rect(
            (box.left   / scaleFactor).roundToInt(),
            (box.top    / scaleFactor).roundToInt(),
            (box.right  / scaleFactor).roundToInt(),
            (box.bottom / scaleFactor).roundToInt(),
        )

    private fun warpLinePerspective(src: Bitmap, corners: Array<Point>): Bitmap {
        val tl = corners[0]; val tr = corners[1]
        val br = corners[2]; val bl = corners[3]

        val topDx = (tr.x - tl.x).toFloat(); val topDy = (tr.y - tl.y).toFloat()
        val topLen = hypot(topDx.toDouble(), topDy.toDouble()).toFloat().coerceAtLeast(1f)
        val hNx = topDx / topLen; val hNy = topDy / topLen

        val leftDx = (bl.x - tl.x).toFloat(); val leftDy = (bl.y - tl.y).toFloat()
        val leftLen = hypot(leftDx.toDouble(), leftDy.toDouble()).toFloat().coerceAtLeast(1f)
        val vNx = leftDx / leftLen; val vNy = leftDy / leftLen

        fun ep(p: Point, dH: Float, dV: Float) = floatArrayOf(
            p.x + hNx * dH + vNx * dV,
            p.y + hNy * dH + vNy * dV,
        )

        val eTL = ep(tl, -PAD_LEFT.toFloat(),  -PAD_TOP.toFloat())
        val eTR = ep(tr,  PAD_RIGHT.toFloat(),  -PAD_TOP.toFloat())
        val eBR = ep(br,  PAD_RIGHT.toFloat(),   PAD_BOTTOM.toFloat())
        val eBL = ep(bl, -PAD_LEFT.toFloat(),    PAD_BOTTOM.toFloat())

        val dstW = max(
            hypot((eTR[0]-eTL[0]).toDouble(), (eTR[1]-eTL[1]).toDouble()),
            hypot((eBR[0]-eBL[0]).toDouble(), (eBR[1]-eBL[1]).toDouble()),
        ).roundToInt().coerceAtLeast(1)

        val dstH = max(
            hypot((eBL[0]-eTL[0]).toDouble(), (eBL[1]-eTL[1]).toDouble()),
            hypot((eBR[0]-eTR[0]).toDouble(), (eBR[1]-eTR[1]).toDouble()),
        ).roundToInt().coerceAtLeast(1)

        val srcPts = floatArrayOf(eTL[0],eTL[1], eTR[0],eTR[1], eBR[0],eBR[1], eBL[0],eBL[1])
        val dstPts = floatArrayOf(0f,0f, dstW.toFloat(),0f, dstW.toFloat(),dstH.toFloat(), 0f,dstH.toFloat())

        val matrix = android.graphics.Matrix()
        return if (matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)) {
            val out = createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(out).apply {
                drawColor(android.graphics.Color.WHITE)
                drawBitmap(src, matrix, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
            }
            out
        } else {
            cropWithPad(src, Rect(eTL[0].roundToInt(), eTL[1].roundToInt(), eBR[0].roundToInt(), eBR[1].roundToInt()))
        }
    }

    private fun cropWithPad(src: Bitmap, rect: Rect): Bitmap {
        val left   = (rect.left   - PAD_LEFT  ).coerceAtLeast(0)
        val top    = (rect.top    - PAD_TOP   ).coerceAtLeast(0)
        val right  = (rect.right  + PAD_RIGHT ).coerceAtMost(src.width)
        val bottom = (rect.bottom + PAD_BOTTOM).coerceAtMost(src.height)
        return Bitmap.createBitmap(src, left, top, (right-left).coerceAtLeast(1), (bottom-top).coerceAtLeast(1))
    }

    private suspend fun recognizeText(inputImage: InputImage): Text =
        suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
}