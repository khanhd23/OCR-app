package com.example.ocr.presentation.feature.camera

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.ocr.core.common.Constants
import com.example.ocr.presentation.component.CameraOverlay
import com.example.ocr.presentation.component.LoadingView
import com.example.ocr.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onOCRSuccess: (Long) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    LaunchedEffect(uiState) {
        if (uiState is CameraUiState.Success) {
            onOCRSuccess((uiState as CameraUiState.Success).documentId)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink900)) {
        when (val state = uiState) {
            is CameraUiState.Idle -> {
                if (cameraPermission.status.isGranted) {
                    CameraPreviewContent(
                        onPhotoTaken = { uri -> viewModel.onImageCaptured(context, uri) },
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onBack = onNavigateBack
                    )
                } else {
                    PermissionRequest(
                        onRequest = { cameraPermission.launchPermissionRequest() },
                        onGallery = { galleryLauncher.launch("image/*") },
                        onBack = onNavigateBack
                    )
                }
            }
            is CameraUiState.Preview -> {
                ImagePreviewContent(
                    imageUri = state.imageUri,
                    onOCR = { viewModel.performOCR(context) },
                    onRetake = { viewModel.retake() }
                )
            }
            is CameraUiState.Processing -> {
                LoadingView(state.message)
            }
            is CameraUiState.Error -> {
                ErrorContent(message = state.message, onRetry = { viewModel.retake() })
            }
            is CameraUiState.Success -> {
                LoadingView("Hoàn tất!")
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    onPhotoTaken: (Uri) -> Unit,
    onGalleryClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }   // ← thêm để điều khiển torch
    var flashEnabled by remember { mutableStateOf(false) }

    // Flashlight  on/off
    LaunchedEffect(flashEnabled) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    imageCapture = capture
                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(  // ← gán camera
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Bind failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 70.dp)
        ) {
            CameraOverlay()
        }

        // Top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = White)
            }

            Text(
                "Hướng camera vào văn bản",
                color = White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Box(modifier = Modifier.size(44.dp))
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                )
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .size(52.dp)
                        .background(SurfaceCard.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Thư viện", tint = White)
                }

                // Shutter button
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(3.dp, White.copy(alpha = 0.6f), CircleShape)
                    )
                    IconButton(
                        onClick = {
                            val ic = imageCapture ?: return@IconButton
                            val name = SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.getDefault())
                                .format(System.currentTimeMillis())
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            }
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                context.contentResolver,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            ).build()
                            ic.takePicture(
                                outputOptions, cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        output.savedUri?.let { onPhotoTaken(it) }
                                    }
                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("CameraScreen", "Capture error", exc)
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(White, CircleShape)
                    ) {
                        Icon(Icons.Filled.Camera, contentDescription = "Chụp", tint = Ink900, modifier = Modifier.size(32.dp))
                    }
                }

                // Flash button
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier
                        .size(52.dp)
                        .background(SurfaceCard.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (flashEnabled) Amber400 else SurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) Amber400 else White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Nhấn nút để chụp ảnh", color = White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ImagePreviewContent(
    imageUri: Uri,
    onOCR: () -> Unit,
    onRetake: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Ink900)) {
        // Preview image
        AsyncImage(
            model = imageUri,
            contentDescription = "Ảnh xem trước",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Dark gradient at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))
                )
        )

        // Bottom actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ảnh đã chọn",
                color = White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Nhấn OCR để nhận dạng hoặc chụp lại",
                color = White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, White.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Chụp lại", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onOCR,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal400, contentColor = Ink900),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("OCR", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }

        // Top back button
        IconButton(
            onClick = onRetake,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = White)
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequest: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Teal400, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(20.dp))
        Text("Cần quyền truy cập Camera", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Cho phép ứng dụng dùng camera để chụp ảnh văn bản", color = TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal400, contentColor = Ink900),
            shape = RoundedCornerShape( 14.dp)
        ) { Text("Cấp quyền Camera",color = Color.Black,  fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onGallery,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal400),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Teal400),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Chọn từ thư viện", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Quay lại", color = TextSecondary) }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Đã có lỗi xảy ra", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Teal400, contentColor = Ink900),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Thử lại", fontWeight = FontWeight.Bold) }
    }
}