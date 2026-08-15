package com.docuvio.app.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun QRScannerScreen(
    onCodeScanned: (String) -> Unit,
    onBack: () -> Unit,
    statusMessage: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isProcessing by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf(statusMessage) }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            scanError = statusMessage
            isProcessing = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> 
            android.util.Log.d("QRScanner", "Permission granted: $granted")
            hasCameraPermission = granted 
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            android.util.Log.d("QRScanner", "Requesting permission...")
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            val previewView = remember { PreviewView(context) }
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
            val scanner = remember {
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
                    .build()
                BarcodeScanning.getClient(options)
            }

            var isFlashOn by remember { mutableStateOf(false) }
            var camera: Camera? by remember { mutableStateOf(null) }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            DisposableEffect(Unit) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isProcessing) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty() && !isProcessing) {
                                        barcodes.firstOrNull()?.rawValue?.let { code ->
                                            android.util.Log.d("QRScanner", "Scanned: $code")
                                            isProcessing = true
                                            onCodeScanned(code) 
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("QRScanner", "Scan failed", e)
                                    scanError = "Scanner error: ${e.localizedMessage}"
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        scanError = "Camera initialization failed"
                    }
                }, ContextCompat.getMainExecutor(context))

                onDispose {
                    cameraExecutor.shutdown()
                    scanner.close()
                }
            }

            QRScannerOverlay()

            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            
            if (scanError != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
                    action = {
                        TextButton(onClick = { scanError = null }) { Text("Retry") }
                    }
                ) { Text(scanError!!) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    "Scan Shop QR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Align the QR code within the frame",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission required", color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun QRScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxSize = size.minDimension * 0.7f

        val boxRect = Rect(
            offset = Offset((width - boxSize) / 2f, (height - boxSize) / 2f),
            size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
        )

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = boxRect,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            )
        }

        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.5f))
        }

        val cornerLength = 40.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val color = Color.White

        drawLine(color, boxRect.topLeft, boxRect.topLeft.copy(x = boxRect.left + cornerLength), strokeWidth)
        drawLine(color, boxRect.topLeft, boxRect.topLeft.copy(y = boxRect.top + cornerLength), strokeWidth)

        drawLine(color, boxRect.topRight, boxRect.topRight.copy(x = boxRect.right - cornerLength), strokeWidth)
        drawLine(color, boxRect.topRight, boxRect.topRight.copy(y = boxRect.top + cornerLength), strokeWidth)

        drawLine(color, boxRect.bottomLeft, boxRect.bottomLeft.copy(x = boxRect.left + cornerLength), strokeWidth)
        drawLine(color, boxRect.bottomLeft, boxRect.bottomLeft.copy(y = boxRect.bottom - cornerLength), strokeWidth)

        drawLine(color, boxRect.bottomRight, boxRect.bottomRight.copy(x = boxRect.right - cornerLength), strokeWidth)
        drawLine(color, boxRect.bottomRight, boxRect.bottomRight.copy(y = boxRect.bottom - cornerLength), strokeWidth)
    }
}
