package com.example.nutricionalregister.scan

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Decodes the barcode of one camera frame.
 *
 * The interface is the seam between the camera pipeline and the decoder. The
 * production implementation is ML Kit; the instrumented tests use a fake that
 * answers with a chosen code, so no test ever needs a camera or a real barcode
 * (ADR-0017).
 */
public interface BarcodeDecoder {
    /**
     * Decodes [input] and reports the first barcode it finds, or null when the
     * frame carries none or the decoding fails.
     */
    public fun decode(
        input: InputImage,
        onResult: (String?) -> Unit,
    )
}

/**
 * The ML Kit barcode decoder.
 *
 * Only the formats food packaging carries are requested, because asking for
 * every format the library knows only adds decoding work the app cannot use.
 */
public class MlKitBarcodeDecoder : BarcodeDecoder {
    private val scanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13 or
                        Barcode.FORMAT_EAN_8 or
                        Barcode.FORMAT_UPC_A or
                        Barcode.FORMAT_UPC_E,
                ).build(),
        )

    override fun decode(
        input: InputImage,
        onResult: (String?) -> Unit,
    ) {
        scanner
            .process(input)
            .addOnSuccessListener { barcodes -> onResult(barcodes.firstOrNull()?.rawValue) }
            .addOnFailureListener { onResult(null) }
    }
}

/**
 * The analyzer that feeds the camera frames to a [decoder].
 *
 * The frame is closed exactly once, after the decoder has finished with it,
 * because the frame belongs to the camera and the decoder holds onto its image
 * until it reports. Closing it earlier would hand the decoder a recycled
 * buffer.
 */
@OptIn(ExperimentalGetImage::class)
public class BarcodeImageAnalyzer(
    private val decoder: BarcodeDecoder,
    private val onBarcode: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        decoder.decode(input) { barcode ->
            image.close()
            if (barcode != null) onBarcode(barcode)
        }
    }
}
