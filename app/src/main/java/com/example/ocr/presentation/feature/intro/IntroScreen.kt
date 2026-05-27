package com.example.ocr.presentation.feature.intro

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr.presentation.theme.*

@Composable
fun IntroScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink900)
    ) {
        // Background mesh gradient blobs
        MeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            TopBar()

            // Center content
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
            ) {
                CenterHero(onNavigateToCamera = onNavigateToCamera)
            }

            // Bottom section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(tween(800, delayMillis = 200)) { it / 3 }
            ) {
                BottomSection(onNavigateToHistory = onNavigateToHistory)
            }
        }
    }
}

@Composable
private fun MeshBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bgOffset"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Teal blob top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TealGlow, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * (0.1f + offset * 0.05f)),
                        radius = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * (0.1f + offset * 0.05f))
                )
                // Amber blob bottom-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AmberGlow, Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * (0.75f - offset * 0.05f)),
                        radius = size.width * 0.45f
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.15f, size.height * (0.75f - offset * 0.05f))
                )
            }
    )
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo mark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.linearGradient(listOf(Teal400, Teal200)),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DocumentScanner,
                    contentDescription = null,
                    tint = Ink900,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = White, fontWeight = FontWeight.Bold)) { append("OCR") }
                    withStyle(SpanStyle(color = Teal400, fontWeight = FontWeight.Light)) { append(" Pro") }
                },
                fontSize = 20.sp
            )
        }

        // Settings icon
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Settings, contentDescription = "Cài đặt", tint = TextSecondary)
        }
    }
}

@Composable
private fun CenterHero(onNavigateToCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big icon ring
        val pulseTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
        val ringAlpha by pulseTransition.animateFloat(
            initialValue = 0.3f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "ring"
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            // Outer ring pulse
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = ringAlpha }
                    .border(1.dp, Teal400.copy(alpha = 0.3f), CircleShape)
            )
            // Middle ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(1.dp, Teal400.copy(alpha = 0.5f), CircleShape)
            )
            // Inner filled circle
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(listOf(Ink600, Ink800)),
                        CircleShape
                    )
                    .border(2.dp, Teal400, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Teal400,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Nhận dạng văn bản\nchính xác tức thì",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 36.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Chụp ảnh hoặc chọn từ thư viện — AI sẽ\ntrích xuất toàn bộ nội dung cho bạn",
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        // Feature pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeaturePill(icon = Icons.Outlined.AutoAwesome, label = "AI OCR", modifier = Modifier.weight(1f))
            FeaturePill(icon = Icons.Outlined.TextFields, label = "Xuất Word", modifier = Modifier.weight(1f))
            FeaturePill(icon = Icons.Filled.History, label = "Lịch sử", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(40.dp))

        // CTA button
        Button(
            onClick = onNavigateToCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal400, contentColor = Ink900),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Bắt đầu OCR",color= Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Teal400, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BottomSection(onNavigateToHistory: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
        // Divider
        HorizontalDivider(thickness = 1.dp, color = DividerColor)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recent scans hint
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToHistory() }
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = Amber400, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Xem lịch sử quét", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Xem các tài liệu đã quét", fontSize = 11.sp, color = TextHint)
                }
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Version badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ink700)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("v1.0", fontSize = 11.sp, color = TextHint)
            }
        }
    }
}
