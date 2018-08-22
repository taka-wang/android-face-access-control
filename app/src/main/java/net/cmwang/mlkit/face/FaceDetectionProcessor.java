package net.cmwang.mlkit.face;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import net.cmwang.mlkit.FrameMetadata;
import net.cmwang.mlkit.GraphicOverlay;
import net.cmwang.mlkit.VisionProcessorBase;

import java.nio.ByteBuffer;
import java.util.List;

public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return null;
    }

    @Override
    protected void onSuccess(@NonNull List<FirebaseVisionFace> results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay, @NonNull ByteBuffer data) {

    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }
}
