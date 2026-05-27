package com.example.ocr.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr.presentation.theme.*

// Pulsing Loading Indicator
@Composable
fun LoadingView(message: String = "Đang xử lý...") {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().background(Ink900)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .background(TealGlow, CircleShape)
                .border(2.dp, Teal400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Teal400, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        }
        Spacer(Modifier.height(20.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp)
    }
}

// OCR Result Line Item
@Composable
fun ResultItem(lineText: String, lineNumber: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(TealGlow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$lineNumber",
                color = Teal400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = lineText,
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CameraOverlay() {
    val scanAreaPaddingH  = 40.dp
    val topCornerPaddingV = 20.dp
    val botCornerPaddingV = 160.dp
    val cornerArmLength   = 28.dp
    val cornerStroke      = 3.dp
    val cornerRadius      = 8.dp
    val cornerColor       = Teal400

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanFraction by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scanY"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = maxWidth
        val H = maxHeight

        val scanTop    = topCornerPaddingV
        val scanBottom = H - botCornerPaddingV
        val scanLeft   = scanAreaPaddingH
        val scanRight  = W - scanAreaPaddingH

        val scanLineY = scanTop + (scanBottom - scanTop) * scanFraction

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val arm    = cornerArmLength.toPx()
                    val stroke = cornerStroke.toPx()
                    val r      = cornerRadius.toPx()
                    val left   = scanLeft.toPx()
                    val right  = scanRight.toPx()
                    val top    = scanTop.toPx()
                    val bottom = scanBottom.toPx()
                    val c      = cornerColor

                    drawLCorner(this, left,  top,    arm, stroke, r, c, topLeft     = true)
                    drawLCorner(this, right, top,    arm, stroke, r, c, topRight    = true)
                    drawLCorner(this, left,  bottom, arm, stroke, r, c, bottomLeft  = true)
                    drawLCorner(this, right, bottom, arm, stroke, r, c, bottomRight = true)
                }
        )

        // Line scan
        Box(
            Modifier
                .offset(x = scanLeft, y = scanLineY)
                .width(scanRight - scanLeft)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Teal400, Teal300, Teal400, Color.Transparent)
                    )
                )
        )
    }
}

private fun drawLCorner(
    scope: DrawScope,
    x: Float, y: Float,
    arm: Float, stroke: Float, radius: Float,
    color: Color,
    topLeft: Boolean = false,
    topRight: Boolean = false,
    bottomLeft: Boolean = false,
    bottomRight: Boolean = false
) {
    val cap = StrokeCap.Round
    with(scope) {
        when {
            topLeft -> {
                drawLine(color, Offset(x + radius, y), Offset(x + arm, y), stroke, cap)
                drawLine(color, Offset(x, y + radius), Offset(x, y + arm), stroke, cap)
                drawArc(color = color, startAngle = 180f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(x, y), size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = cap))
            }
            topRight -> {
                drawLine(color, Offset(x - arm, y), Offset(x - radius, y), stroke, cap)
                drawLine(color, Offset(x, y + radius), Offset(x, y + arm), stroke, cap)
                drawArc(color = color, startAngle = 270f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(x - radius * 2, y), size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = cap))
            }
            bottomLeft -> {
                drawLine(color, Offset(x + radius, y), Offset(x + arm, y), stroke, cap)
                drawLine(color, Offset(x, y - arm), Offset(x, y - radius), stroke, cap)
                drawArc(color = color, startAngle = 90f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(x, y - radius * 2), size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = cap))
            }
            bottomRight -> {
                drawLine(color, Offset(x - arm, y), Offset(x - radius, y), stroke, cap)
                drawLine(color, Offset(x, y - arm), Offset(x, y - radius), stroke, cap)
                drawArc(color = color, startAngle = 0f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(x - radius * 2, y - radius * 2), size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = cap))
            }
        }
    }
}

// Teal Primary Button
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Teal400,
            contentColor = Ink900,
            disabledContainerColor = Ink600,
            disabledContentColor = TextHint
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// Ghost/Secondary Button
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal400),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Teal400),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}