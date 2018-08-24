package net.cmwang.accesscontrol;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.ml.common.FirebaseMLException;

import net.cmwang.mlkit.CameraSource;
import net.cmwang.mlkit.CameraSourcePreview;
import net.cmwang.mlkit.GraphicOverlay;
import net.cmwang.mlkit.barcode.BarcodeScanningProcessor;
import net.cmwang.mlkit.face.FaceDetectionProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

public class LivePreviewActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "LivePreviewActivity";
    private CameraSource cameraSource = null;

    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;

    private static boolean isRunningFaceDetection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livepreview);

        ButterKnife.bind(this); // Binds

        // get runtime permissions
        new PermissionDelegate(this).getPermissions();

        // start camera
        createCameraSource(isRunningFaceDetection);
        startCameraSource();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();

        preview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    /** camera ************************************************/

    private void createCameraSource(boolean isFaceDetection) {
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        if (isFaceDetection) {
            cameraSource.setFrameProcessor(new FaceDetectionProcessor());
        } else {
            cameraSource.setFrameProcessor(new BarcodeScanningProcessor());
        }
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }

    }

}
