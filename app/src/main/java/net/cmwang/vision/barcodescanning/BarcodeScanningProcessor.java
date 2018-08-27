package net.cmwang.vision.barcodescanning;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.cmwang.accesscontrol.LivePreviewActivity;
import net.cmwang.vision.GraphicOverlay;
import net.cmwang.vision.VisionFrame;
import net.cmwang.vision.VisionProcessorBase;

import java.io.IOException;
import java.util.List;

public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {
    private static final String TAG = "BarcodeScanProc";
    private final FirebaseVisionBarcodeDetector detector;

    public BarcodeScanningProcessor() {
        FirebaseVisionBarcodeDetectorOptions options =
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                        .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close QR Code Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(@NonNull List<FirebaseVisionBarcode> barcodes, @NonNull VisionFrame frame, @NonNull GraphicOverlay graphicOverlay) {
        // clear overlay before update
        graphicOverlay.clear();

        for (int i = 0; i < barcodes.size(); ++i) {
            FirebaseVisionBarcode barcode = barcodes.get(i);
            BarcodeGraphic barcodeGraphic = new BarcodeGraphic(graphicOverlay, barcode);
            graphicOverlay.add(barcodeGraphic);

            Log.d(TAG, "QR Code: " + barcode.getRawValue());
            Intent intent = new Intent(LivePreviewActivity.intentFilterNewUUID);
            intent.putExtra(LivePreviewActivity.intentExtraUUID, barcode.getRawValue());
            LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext()).sendBroadcast(intent);
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "QR Code detection failed " + e);
    }
}

