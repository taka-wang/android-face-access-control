package net.cmwang.vision;

import android.content.Context;
import android.util.Log;

import com.google.firebase.ml.common.FirebaseMLException;

import net.cmwang.vision.barcodescanning.BarcodeScanningProcessor;
import net.cmwang.vision.facedetection.FaceDetectionProcessor;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.view.CameraView;

public class CameraSource {
    private static final String TAG = "CameraSource";
    private static  String defaultProcessor = "face_detection_processor";
    private static int cameraFacing = GraphicOverlay.LENS_FACING_FRONT;

    private final Object processorLock = new Object();
    private static VisionImageProcessor processor; // @GuardedBy("processorLock")
    private static CameraView cameraView;
    private static GraphicOverlay overlayView;
    private static Fotoapparat fotoapparat;
    private static Context context;

    public static final String faceDetection = "face_detection_processor";
    public static final String qrcodeScanning = "qrcode_scanning_processor";

    private Fotoapparat createFotoApparat(Context ctx) {
        return Fotoapparat
                .with(ctx)
                .into(cameraView)
                //.previewScaleType(ScaleType.CenterCrop)
                //.photoResolution(ResolutionSelectorsKt.highestResolution())
                .lensPosition(LensPositionSelectorsKt.front())
                .frameProcessor(new FrameProcessor() {
                    @Override
                    public void process(Frame frame) {
                        synchronized (processorLock) {
                            try {
                                processor.process(
                                        new VisionFrame.Builder().
                                                setWidth(frame.getSize().width).
                                                setHeight(frame.getSize().height).
                                                setCameraFacing(cameraFacing).
                                                setRotation(frame.getRotation()).
                                                setData(frame.getImage()).
                                                build(),
                                        overlayView);
                            } catch (FirebaseMLException e) {
                                Log.e(TAG, "process: " + e);
                            }
                        }
                    }
                })
                .build();
    }

    public CameraSource(Context ctx, CameraView c, GraphicOverlay o) {
        this.context = ctx;
        this.cameraView = c;
        this.overlayView = o;
        setProcessor(defaultProcessor);
        fotoapparat = createFotoApparat(this.context);
    }

    public void stop() {
        //Log.d(TAG, "stop");
        if (processor != null) {
            processor.stop();
        }
        fotoapparat.stop();
    }

    public void start() {
        //Log.d(TAG, "start");
        setProcessor(defaultProcessor);
        fotoapparat.start();
    }

    public void release() {
        if (processor != null) {
            processor.stop();
        }
        fotoapparat.stop();
    }

    public void setDefaultProcessor(String s) {
        defaultProcessor = s;
        setProcessor(defaultProcessor);
    }

    public void setProcessor(String model) {
        Log.d(TAG, "setProcessor: " + model);
        synchronized (processorLock) {
            overlayView.clear();
            if (processor != null) {
                processor.stop();
            }

            switch (model) {
                case faceDetection:
                    processor = new FaceDetectionProcessor();
                    break;
                case qrcodeScanning:
                    processor = new BarcodeScanningProcessor();
                    break;
                default:
                    Log.e(TAG, "wrong model: " + model);
                    processor = new FaceDetectionProcessor();
            }
        }
    }
}
