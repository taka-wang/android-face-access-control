package net.cmwang.vision;

import com.google.firebase.ml.common.FirebaseMLException;

public interface VisionImageProcessor {

    /** Processes the images with the underlying machine learning models. */
    void process(VisionFrame frameMetadata, GraphicOverlay graphicOverlay) throws FirebaseMLException;

    /** Stops the underlying machine learning model and release resources. */
    void stop();
}
