package net.cmwang.mlkit.barcode;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.cmwang.mlkit.FrameMetadata;
import net.cmwang.mlkit.GraphicOverlay;
import net.cmwang.mlkit.VisionProcessorBase;

import java.nio.ByteBuffer;
import java.util.List;

public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {
    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return null;
    }

    @Override
    protected void onSuccess(@NonNull List<FirebaseVisionBarcode> results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay, @NonNull ByteBuffer data) {

    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }
}
