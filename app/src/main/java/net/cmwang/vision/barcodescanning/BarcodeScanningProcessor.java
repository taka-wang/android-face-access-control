package net.cmwang.vision.barcodescanning;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.cmwang.vision.GraphicOverlay;
import net.cmwang.vision.VisionFrame;
import net.cmwang.vision.VisionProcessorBase;

import java.util.List;

public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return null;
    }

    @Override
    protected void onSuccess(@NonNull List<FirebaseVisionBarcode> results, @NonNull VisionFrame frame, @NonNull GraphicOverlay graphicOverlay) {

    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }
}

