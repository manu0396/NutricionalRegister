@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.nutricionalregister.ui.theme.NutriLogSpacing
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * The barcode reader: the camera, one code and the fallback (ADR-0017, FR-026
 * to FR-029).
 *
 * The view asks for the camera permission at runtime and never loops the
 * request. When the user refuses, the permission is unavailable or the device
 * has no camera, the view says so and offers to enter the code by hand, so the
 * flow is never a dead end and the app still works without a camera.
 *
 * The camera is bound with CameraX and the codes are read with the bundled ML
 * Kit scanner, so nothing leaves the device. The first code is reported once
 * through [onBarcodeDetected] and the reader then stops: a repeated detection
 * of the same code never fires the callback again, because a scanner that
 * keeps firing would resolve a code the user has already acted on.
 *
 * @param onBarcodeDetected invoked once with the code the camera read, or with
 *   the code the user typed by hand.
 * @param modifier the modifier applied to the view.
 */
@Composable
public fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    var refused by remember { mutableStateOf(false) }
    var cameraUnavailable by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            refused = !granted
        }

    LaunchedEffect(hasPermission) {
        if (!hasPermission && !refused) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().testTag("barcode-scanner"),
    ) {
        when {
            refused ->
                ManualEntry(
                    title = "The camera is not available",
                    body =
                        "Allow the camera in the settings to read a barcode, or type the " +
                            "number under the barcode instead.",
                    onEnter = onBarcodeDetected,
                )

            cameraUnavailable ->
                ManualEntry(
                    title = "No camera to use",
                    body =
                        "This device cannot open a camera. Type the number under the " +
                            "barcode instead.",
                    onEnter = onBarcodeDetected,
                )

            hasPermission ->
                Column {
                    CameraPreview(
                        onBarcodeDetected = onBarcodeDetected,
                        onUnavailable = { cameraUnavailable = true },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                    Text(
                        text = "Hold the barcode inside the frame until it is read.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(NutriLogSpacing.medium),
                    )
                }

            else ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(NutriLogSpacing.extraLarge),
                ) {
                    Text(
                        text = "Waiting for the camera",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }
    }
}

/** The camera itself: the preview, the analyser and the one code it reports. */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var reported by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { factoryContext ->
            PreviewView(factoryContext).also { previewView ->
                val providerFuture = ProcessCameraProvider.getInstance(factoryContext)
                providerFuture.addListener(
                    {
                        try {
                            val cameraProvider = providerFuture.get()
                            provider = cameraProvider

                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            val analysis =
                                ImageAnalysis
                                    .Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                            analysis.setAnalyzer(executor) { proxy ->
                                readBarcode(scanner, proxy) { code ->
                                    if (!reported) {
                                        reported = true
                                        onBarcodeDetected(code)
                                    }
                                }
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        } catch (exception: Exception) {
                            onUnavailable()
                        }
                    },
                    ContextCompat.getMainExecutor(factoryContext),
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * Reads the first barcode of [proxy] and reports it through [onRead].
 *
 * The analyser runs off the main thread, and the proxy is always closed, so the
 * camera never stalls behind a frame that was not released.
 */
@OptIn(ExperimentalGetImage::class)
private fun readBarcode(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    proxy: ImageProxy,
    onRead: (String) -> Unit,
) {
    val mediaImage = proxy.image
    if (mediaImage == null) {
        proxy.close()
        return
    }
    val rotation = proxy.imageInfo.rotationDegrees
    val input = InputImage.fromMediaImage(mediaImage, rotation)

    scanner
        .process(input)
        .addOnSuccessListener { barcodes ->
            barcodes
                .mapNotNull { barcode -> barcode.rawValue?.trim() }
                .firstOrNull { code -> code.isNotEmpty() }
                ?.let(onRead)
        }.addOnCompleteListener { proxy.close() }
}

/** The fallback: the code typed by hand, which goes through the same callback. */
@Composable
private fun ManualEntry(
    title: String,
    body: String,
    onEnter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier = modifier.fillMaxWidth().padding(NutriLogSpacing.extraLarge),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = code,
            onValueChange = { value -> code = value.filter { it.isDigit() } },
            label = { Text(text = "Barcode number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("barcode-manual-entry"),
        )
        OutlinedButton(
            onClick = { onEnter(code.trim()) },
            enabled = code.isNotBlank(),
            modifier = Modifier.testTag("barcode-manual-submit"),
        ) {
            Text(text = "Look up this code")
        }
    }
}
