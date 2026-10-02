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
            .mapNotNull { it.toDetectedLine() }
            .filter { LineGeometry.isValidTextLine(it, imgW, imgH) }

        val filtered = LineGeometry.filterStrictlyContained(rawLines)
        val merged   = LineGeometry.mergeLines(filtered)
        val sorted   = LineGeometry.sortReadingOrder(merged)

        return sorted.mapNotNull { buildLineResult(bitmap, it, scaleFactor) }
    }

    // Build LineResult
    private fun buildLineResult(
        bitmap: Bitmap,
        group: List<DetectedLine>,
        scaleFactor: Float,
    ): LineResult? {
        if (group.isEmpty()) return null

        val mergedBox = group.map { it.box }.reduce { acc, b -> acc.union(b) }
        val quad = if (group.size == 1) group[0].quad else LineGeometry.mergedQuad(group)

        val warped = if (quad != null)
            warpLinePerspective(bitmap, quad.map { Point(it.x, it.y) }.toTypedArray())
        else
            cropWithPad(bitmap, mergedBox.toRect())

        return LineResult(warpedBitmap = warped, boundingBox = scaleBox(mergedBox.toRect(), scaleFactor))
    }

    // Helpers
    private fun Text.Line.toDetectedLine(): DetectedLine? {
        val b = boundingBox ?: return null
        return DetectedLine(
            text    = text,
            box     = Box(b.left, b.top, b.right, b.bottom),
            corners = cornerPoints?.map { Pt(it.x, it.y) },
        )
    }

    private fun Box.toRect() = Rect(left, top, right, bottom)

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