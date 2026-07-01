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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr.R

@Composable
fun IntroScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(onNavigateToSettings = onNavigateToSettings)

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
            ) {
                CenterHero(onNavigateToCamera = onNavigateToCamera)
            }

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
    val tealGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val amberGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tealGlow, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * (0.1f + offset * 0.05f)),
                        radius = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * (0.1f + offset * 0.05f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(amberGlow, Color.Transparent),
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
private fun TopBar(onNavigateToSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) { append("OCR") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Light)) { append(" Pro") }
                },
                fontSize = 20.sp
            )
        }

        IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
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
        val pulseTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = 0.5f }
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.intro_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.intro_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeaturePill(icon = Icons.Outlined.AutoAwesome, label = "AI OCR", modifier = Modifier.weight(1f))
            FeaturePill(icon = Icons.Outlined.TextFields, label = "Word", modifier = Modifier.weight(1f))
            FeaturePill(icon = Icons.Filled.History, label = stringResource(R.string.history), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onNavigateToCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary, 
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.ocr_start), 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BottomSection(onNavigateToHistory: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToHistory() }
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.view_history), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.view_scans_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("v1.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
