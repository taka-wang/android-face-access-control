package net.cmwang.accesscontrol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import net.cmwang.vision.CameraSource;
import net.cmwang.vision.GraphicOverlay;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fotoapparat.view.CameraView;

public class LivePreviewActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    @BindView(R.id.cameraView) CameraView cameraView;
    @BindView(R.id.overlayView) GraphicOverlay overlayView;

    private static final String TAG = "LivePreviewActivity";
    public static final String REFRESH_ACTIVITY = "net.cmwang.action.REFRESH_UI"; // dummy action
    private static Context context;
    private CameraSource cameraSource;
    private boolean isFaceDetecting = true;


    public static Context getAppContext() {
        return context;
    }

    private void switchActionBar() {
        ColorDrawable colorDrawable;
        String title;
        if (isFaceDetecting) {
            colorDrawable = new ColorDrawable(Color.parseColor("#212121"));
            title = "Face Recognition";
            this.getWindow().setStatusBarColor(Color.parseColor("#212121"));
            this.getWindow().setNavigationBarColor(Color.parseColor("#212121"));
        } else {
            colorDrawable = new ColorDrawable(Color.parseColor("#f44336"));
            title = "QRCode Scan";
            this.getWindow().setStatusBarColor(Color.parseColor("#f44336"));
            this.getWindow().setNavigationBarColor(Color.parseColor("#f44336"));

        }
        ActionBar bar = getSupportActionBar();
        bar.setTitle(title);
        bar.setBackgroundDrawable(colorDrawable);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livepreview);

        context = this;

        ButterKnife.bind(this); // Binds

        switchActionBar(); // Change actionbar text and color

        // get runtime permissions
        new PermissionDelegate(this).getPermissions();

        cameraSource = new CameraSource(getApplicationContext(), cameraView, overlayView);


        // after camera source
        if (!isFaceDetecting) {
            cameraSource.setDefaultProcessor("qrcode");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //getAllExtras();
        cameraSource.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraSource.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.unregisterReceiver(broadcastReceiver);
        cameraSource.release();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed(); commented this line in order to disable back press
        //Write your code here
        Toast.makeText(getApplicationContext(), "Back press disabled!", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.test)
    protected void onStartClick(View view) {
        Log.d(TAG, "click");
        if (isFaceDetecting) {
            isFaceDetecting = false;
        } else {
            isFaceDetecting = true;
        }
        // after camera source
        if (isFaceDetecting) {
            cameraSource.setDefaultProcessor("face");
        } else {
            cameraSource.setDefaultProcessor("qrcode");
        }
        switchActionBar();
    }

}
