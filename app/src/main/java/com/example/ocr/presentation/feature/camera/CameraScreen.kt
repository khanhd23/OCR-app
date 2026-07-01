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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.ocr.R
import com.example.ocr.core.common.Constants
import com.example.ocr.presentation.component.CameraOverlay
import com.example.ocr.presentation.component.LoadingView
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

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(context, uris)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CameraUiState.Success) {
            onOCRSuccess((uiState as CameraUiState.Success).documentId)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                    imageUris = state.imageUris,
                    onOCR = { viewModel.performOCR(context) },
                    onRetake = { viewModel.retake() }
                )
            }
            is CameraUiState.Processing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(state.messageRes),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.totalPages > 1) {
                            Text(
                                text = stringResource(R.string.page_info, state.currentPage, state.totalPages),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.width(200.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
            is CameraUiState.Error -> {
                ErrorContent(message = state.message, onRetry = { viewModel.retake() })
            }
            is CameraUiState.Success -> {
                LoadingView(stringResource(R.string.completed))
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
    var camera: Camera? by remember { mutableStateOf(null) }
    var flashEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(flashEnabled) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        camera = cameraProvider.bindToLifecycle(
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

        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 70.dp)) {
            CameraOverlay()
        }

        Row(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(stringResource(R.string.camera_hint), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
            Box(modifier = Modifier.size(44.dp))
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))).padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGalleryClick, modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color.White)
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(80.dp).border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape))
                    IconButton(
                        onClick = {
                            val ic = imageCapture ?: return@IconButton
                            val name = SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.getDefault()).format(System.currentTimeMillis())
                            val contentValues = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg") }
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
                            ic.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) { output.savedUri?.let { onPhotoTaken(it) } }
                                override fun onError(exc: ImageCaptureException) { Log.e("CameraScreen", "Capture error", exc) }
                            })
                        },
                        modifier = Modifier.size(68.dp).background(Color.White, CircleShape)
                    ) { Icon(Icons.Filled.Camera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp)) }
                }

                IconButton(onClick = { flashEnabled = !flashEnabled }, modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).border(1.dp, if (flashEnabled) Color.Yellow else Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))) {
                    Icon(if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff, contentDescription = null, tint = if (flashEnabled) Color.Yellow else Color.White)
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewContent(imageUris: List<Uri>, onOCR: () -> Unit, onRetake: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (imageUris.size == 1) {
            AsyncImage(model = imageUris[0], contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            LazyRow(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(imageUris) { uri ->
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillParentMaxWidth().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.FillWidth)
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (imageUris.size > 1) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(20.dp)) {
                    Text(stringResource(R.string.images_selected, imageUris.size), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetake, 
                    modifier = Modifier.weight(1f).height(54.dp), 
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), 
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)), 
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.retake))
                }
                Button(
                    onClick = onOCR, 
                    modifier = Modifier.weight(1f).height(54.dp), 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, 
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ), 
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("OCR", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.CameraAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.permission_camera), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest, 
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { 
            Text("Cấp quyền Camera", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onGallery, 
            modifier = Modifier.fillMaxWidth().height(52.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) { 
            Text(stringResource(R.string.gallery), color = MaterialTheme.colorScheme.primary) 
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.error_occurred), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry, 
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { 
            Text(stringResource(R.string.retry), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
        }
    }
}
