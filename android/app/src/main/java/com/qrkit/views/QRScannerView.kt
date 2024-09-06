package com.qrkit.views

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.camera.core.Camera

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import com.qrkit.R
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.path

import androidx.compose.material3.Slider
import kotlin.math.max
import kotlin.math.min

@Composable
fun QRScannerView(onResult: (String) -> Unit) {
    var qrCode by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewView { scannedCode ->
            qrCode = scannedCode
        }

        LaunchedEffect(qrCode) {
            if (qrCode.isNotEmpty()) {
                onResult(qrCode)
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    onQRCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var zoomRatio by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        var camera by remember { mutableStateOf<Camera?>(null) }
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    val imageAnalysis = ImageAnalysis.Builder()
                        .build()
                        .also {
                            it.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                { imageProxy ->
                                    processImageProxy(barcodeScanner, imageProxy, onQRCodeScanned)
                                }
                            )
                        }

                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Adjust padding as needed for visual effect
        )
        val widthInPx: Float
        val heightInPx: Float
        val radiusInPx: Float
        with(LocalDensity.current) {
            widthInPx = 250.dp.toPx()
            heightInPx = 250.dp.toPx()
            radiusInPx = 16.dp.toPx()
        }
        var isFlashOn by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .5f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(250.dp)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                    .onGloballyPositioned {
                        (it.boundsInRoot())
                    }
            ) {
                val offset = Offset(
                    x = (size.width - widthInPx) / 2,
                    y = (size.height - heightInPx) / 2,
                )
                val cutoutRect = Rect(offset, Size(widthInPx, heightInPx))
                // Source
                drawRoundRect(
                    topLeft = cutoutRect.topLeft,
                    size = cutoutRect.size,
                    cornerRadius = CornerRadius(radiusInPx, radiusInPx),
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }

        }
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp, bottom = 180.dp)
                    .background(Color.White.copy(alpha = 0.2f), shape=CircleShape)
            ) {
                FlashButton(
                    isFlashOn = isFlashOn,
                    onFlashToggle = {
                        camera?.let {
                            isFlashOn = !isFlashOn
                            it.cameraControl.enableTorch(isFlashOn)
                        }
                    }
                )
                ImagePickerButton { scannedCode ->
                    // Xử lý kết quả mã QR đã quét từ ảnh
                    onQRCodeScanned(scannedCode)
                }
            }
        }

        // Zoom control
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Slider(
                value = zoomRatio,
                onValueChange = { newZoomRatio ->
                    zoomRatio = newZoomRatio
                    camera?.cameraControl?.setZoomRatio(
                        lerp(
                            1f,
                            camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f,
                            newZoomRatio
                        )
                    )
                },
                modifier = Modifier
                    .width(200.dp)
                    .padding(horizontal = 16.dp)
            )
        }

    }
}

// Helper function to interpolate between two values
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

fun processImageProxy(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onQRCodeScanned: (String) -> Unit
) {
    val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                barcode.rawValue?.let { onQRCodeScanned(it) }
            }
        }
        .addOnFailureListener { e ->
            Log.e("QRCodeScanner", "Error processing image: ${e.message}")
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

@Composable
fun ImagePickerButton(onQRCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                
                // Thêm các ứng dụng cụ thể vào intent
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryIntent.type = "image/*"
                galleryIntent.setPackage("com.google.android.apps.photos") // Ứng dụng Google Photos

                val chooserIntent = Intent.createChooser(intent, context.getString(R.string.choose_image))
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(galleryIntent))

                launcher.launch(chooserIntent)
            },
            modifier = Modifier
                .size(48.dp)
                
        ) {
            Icon(
                imageVector = Gallery_thumbnail,
                contentDescription = stringResource(R.string.choose_image),
                tint = Color.White
            )
        }

        imageUri?.let { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Quét mã QR từ ảnh đã chọn
            LaunchedEffect(uri) {
                val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
                val image = InputImage.fromBitmap(bitmap, 0)

                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isEmpty()) {
                            onQRCodeScanned("No Result")
                        }
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { qrCode ->
                                onQRCodeScanned(qrCode)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Xử lý lỗi
                        onQRCodeScanned("QR Scanner error!")
                    }
            }
        }
    }
}

@Composable
fun FlashButton(
    isFlashOn: Boolean,
    onFlashToggle: () -> Unit
) {
    IconButton(
        onClick = onFlashToggle,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = if (!isFlashOn) Flash_on else Flash_off,
            contentDescription = if (isFlashOn) "Turn off flash" else "Turn on flash",
            tint = if (isFlashOn) Color.Yellow else Color.White,
            modifier = Modifier.alpha(if (isFlashOn) 1f else 0.5f)
        )
    }
}

public val Flash_on: ImageVector
	get() {
		if (_Flash_on != null) {
			return _Flash_on!!
		}
		_Flash_on = ImageVector.Builder(
            name = "Flash_on",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
			path(
    			fill = SolidColor(Color.Black),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(480f, 624f)
				lineToRelative(128f, -184f)
				horizontalLineTo(494f)
				lineToRelative(80f, -280f)
				horizontalLineTo(360f)
				verticalLineToRelative(320f)
				horizontalLineToRelative(120f)
				close()
				moveTo(400f, 880f)
				verticalLineToRelative(-320f)
				horizontalLineTo(280f)
				verticalLineToRelative(-480f)
				horizontalLineToRelative(400f)
				lineToRelative(-80f, 280f)
				horizontalLineToRelative(160f)
				close()
				moveToRelative(80f, -400f)
				horizontalLineTo(360f)
				close()
			}
		}.build()
		return _Flash_on!!
	}

private var _Flash_on: ImageVector? = null



public val Flash_off: ImageVector
	get() {
		if (_Flash_off != null) {
			return _Flash_off!!
		}
		_Flash_off = ImageVector.Builder(
            name = "Flash_off",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
			path(
    			fill = SolidColor(Color.Black),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(280f, 80f)
				horizontalLineToRelative(400f)
				lineToRelative(-80f, 280f)
				horizontalLineToRelative(160f)
				lineTo(643f, 529f)
				lineToRelative(-57f, -57f)
				lineToRelative(22f, -32f)
				horizontalLineToRelative(-54f)
				lineToRelative(-47f, -47f)
				lineToRelative(67f, -233f)
				horizontalLineTo(360f)
				verticalLineToRelative(86f)
				lineToRelative(-80f, -80f)
				close()
				moveTo(400f, 880f)
				verticalLineToRelative(-320f)
				horizontalLineTo(280f)
				verticalLineToRelative(-166f)
				lineTo(55f, 169f)
				lineToRelative(57f, -57f)
				lineToRelative(736f, 736f)
				lineToRelative(-57f, 57f)
				lineToRelative(-241f, -241f)
				close()
				moveToRelative(73f, -521f)
			}
		}.build()
		return _Flash_off!!
	}

private var _Flash_off: ImageVector? = null




public val Gallery_thumbnail: ImageVector
	get() {
		if (_Gallery_thumbnail != null) {
			return _Gallery_thumbnail!!
		}
		_Gallery_thumbnail = ImageVector.Builder(
            name = "Gallery_thumbnail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
			path(
    			fill = SolidColor(Color.Black),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(120f, 760f)
				quadToRelative(-33f, 0f, -56.5f, -23.5f)
				reflectiveQuadTo(40f, 680f)
				verticalLineToRelative(-400f)
				quadToRelative(0f, -33f, 23.5f, -56.5f)
				reflectiveQuadTo(120f, 200f)
				horizontalLineToRelative(400f)
				quadToRelative(33f, 0f, 56.5f, 23.5f)
				reflectiveQuadTo(600f, 280f)
				verticalLineToRelative(400f)
				quadToRelative(0f, 33f, -23.5f, 56.5f)
				reflectiveQuadTo(520f, 760f)
				close()
				moveToRelative(600f, -320f)
				quadToRelative(-17f, 0f, -28.5f, -11.5f)
				reflectiveQuadTo(680f, 400f)
				verticalLineToRelative(-160f)
				quadToRelative(0f, -17f, 11.5f, -28.5f)
				reflectiveQuadTo(720f, 200f)
				horizontalLineToRelative(160f)
				quadToRelative(17f, 0f, 28.5f, 11.5f)
				reflectiveQuadTo(920f, 240f)
				verticalLineToRelative(160f)
				quadToRelative(0f, 17f, -11.5f, 28.5f)
				reflectiveQuadTo(880f, 440f)
				close()
				moveToRelative(40f, -80f)
				horizontalLineToRelative(80f)
				verticalLineToRelative(-80f)
				horizontalLineToRelative(-80f)
				close()
				moveTo(120f, 680f)
				horizontalLineToRelative(400f)
				verticalLineToRelative(-400f)
				horizontalLineTo(120f)
				close()
				moveToRelative(40f, -80f)
				horizontalLineToRelative(320f)
				lineTo(375f, 460f)
				lineToRelative(-75f, 100f)
				lineToRelative(-55f, -73f)
				close()
				moveToRelative(560f, 160f)
				quadToRelative(-17f, 0f, -28.5f, -11.5f)
				reflectiveQuadTo(680f, 720f)
				verticalLineToRelative(-160f)
				quadToRelative(0f, -17f, 11.5f, -28.5f)
				reflectiveQuadTo(720f, 520f)
				horizontalLineToRelative(160f)
				quadToRelative(17f, 0f, 28.5f, 11.5f)
				reflectiveQuadTo(920f, 560f)
				verticalLineToRelative(160f)
				quadToRelative(0f, 17f, -11.5f, 28.5f)
				reflectiveQuadTo(880f, 760f)
				close()
				moveToRelative(40f, -80f)
				horizontalLineToRelative(80f)
				verticalLineToRelative(-80f)
				horizontalLineToRelative(-80f)
				close()
				moveToRelative(-640f, 0f)
				verticalLineToRelative(-400f)
				close()
				moveToRelative(640f, -320f)
				verticalLineToRelative(-80f)
				close()
				moveToRelative(0f, 320f)
				verticalLineToRelative(-80f)
				close()
			}
		}.build()
		return _Gallery_thumbnail!!
	}

private var _Gallery_thumbnail: ImageVector? = null
