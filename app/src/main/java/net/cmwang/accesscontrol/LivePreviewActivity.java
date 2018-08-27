package net.cmwang.accesscontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.cmwang.vision.CameraSource;
import net.cmwang.vision.GraphicOverlay;
import net.cmwang.vision.VisionFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;
import io.fotoapparat.view.CameraView;

public class LivePreviewActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    @BindView(R.id.cameraView) CameraView cameraView;
    @BindView(R.id.overlayView) GraphicOverlay overlayView;
    @BindView(R.id.toggleButton) FloatingActionButton floatButton;

    private static final String TAG = "LivePreviewActivity";
    private static Context context;
    protected CameraSource cameraSource;
    private boolean isFaceDetecting = true;

    public static Context getAppContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livepreview);

        context = this;
        ButterKnife.bind(this); // Binds
        Toasty.Config.getInstance().setTextSize(32).apply();

        // get runtime permissions
        new PermissionDelegate(this).getPermissions();

        cameraSource = new CameraSource(getApplicationContext(), cameraView, overlayView);

        // https://stackoverflow.com/questions/22395417/error-strictmodeandroidblockguardpolicy-onnetwork
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // switch theme color and detector
        //setVisionMode(isFaceDetecting);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register
        LocalBroadcastManager.getInstance(this).registerReceiver(frameReceiver, new IntentFilter("newface"));
        LocalBroadcastManager.getInstance(this).registerReceiver(qrcodeReceiver, new IntentFilter("newqrcode"));

        //getAllExtras();
        cameraSource.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister
        LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(qrcodeReceiver);

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
        Toasty.error(this, "Back press disabled!").show();
    }

    @OnClick(R.id.toggleButton)
    protected void onToggleClick(View view) {
        if (isFaceDetecting) {
            isFaceDetecting = false;
        } else {
            isFaceDetecting = true;
        }
        setVisionMode(isFaceDetecting);
    }

    protected void setVisionMode(boolean facedetection) {
        ActionBar bar = getSupportActionBar();
        int black = getColor(R.color.colorBlack);
        int red = getColor(R.color.colorRed);

        if (facedetection) {
            cameraSource.setDefaultProcessor("face");
            // floating button
            floatButton.setImageResource(R.drawable.ic_barcode_scan);
            floatButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorBlack));
            // action bar
            bar.setTitle("Face Recognition");
            bar.setBackgroundDrawable(new ColorDrawable(black));
            // status
            getWindow().setStatusBarColor(black);
            // menu
            getWindow().setNavigationBarColor(black);

        } else {
            cameraSource.setDefaultProcessor("qrcode");
            // floating button
            floatButton.setImageResource(R.drawable.ic_face);
            floatButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorRed));
            // action bar
            bar.setTitle("QR Code Scanning");
            bar.setBackgroundDrawable(new ColorDrawable(red));
            // status
            getWindow().setStatusBarColor(red);
            // menu
            getWindow().setNavigationBarColor(red);
        }
    }

    public BroadcastReceiver qrcodeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new QR Code");

            // throttle broadcast rate: unregister
            LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                    .unregisterReceiver(qrcodeReceiver);
            // stop camera stream
            cameraSource.stop();
            // get frame from intent
            String action = intent.getAction();
            String userUUID = intent.getStringExtra("user_uuid");

            Log.d(TAG, "User UUID: " + userUUID);

            // check
            try {
                URL url = new URL("http://192.168.41.118:5000/qrcode");

                // 1. create HttpURLConnection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                // 2. build JSON object
                JSONObject jsonObject = buidJsonObject();

                // 3. add JSON content to POST request body
                setPostRequestContent(conn, jsonObject);

                // 4. make POST request to the given URL
                conn.connect();

                // 5. return response message
                int ret = conn.getResponseCode();
                String result =  conn.getResponseMessage() + "";
                // 6. disconnect
                conn.disconnect();

                if (ret == 200) {
                    // open the door
                    Bluetooth.OpenDoor();
                    Toasty.success(LivePreviewActivity.getAppContext(), "Welcome, Jamie Wang", Toast.LENGTH_SHORT, true).show();
                } else {
                    Toasty.error(LivePreviewActivity.getAppContext(), "User not found!", Toast.LENGTH_SHORT, true).show();
                }

                Log.d(TAG, "POST :" + ret + " msg: " + result);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            // delay
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            // throttle broadcast rate: register
                            LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                                    .registerReceiver(qrcodeReceiver, new IntentFilter("newqrcode"));

                            // resume camera stream
                            cameraSource.start();
                        }
                    },
                    2000);
        }
    };

    public BroadcastReceiver frameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new face broadcase");

            // throttle broadcast rate: unregister
            LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                    .unregisterReceiver(frameReceiver);
            // stop camera stream
            cameraSource.stop();

            // get frame from intent
            String action = intent.getAction();
            VisionFrame frame = intent.getParcelableExtra("face");

            // check
            try {
                URL url = new URL("http://192.168.41.118:5000/qrcode");

                // 1. create HttpURLConnection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                // 2. build JSON object
                JSONObject jsonObject = buidJsonObject();

                // 3. add JSON content to POST request body
                setPostRequestContent(conn, jsonObject);

                // 4. make POST request to the given URL
                conn.connect();

                // 5. return response message
                int ret = conn.getResponseCode();
                String result =  conn.getResponseMessage() + "";
                // 6. disconnect
                conn.disconnect();

                if (ret == 200) {
                    // open the door
                    Bluetooth.OpenDoor();
                    Toasty.success(LivePreviewActivity.getAppContext(), "Welcome, Jamie Wang", Toast.LENGTH_SHORT, true).show();
                } else {
                    Toasty.error(LivePreviewActivity.getAppContext(), "User not found!", Toast.LENGTH_SHORT, true).show();
                }

                Log.d(TAG, "POST :" + ret + " msg: " + result);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // action

            // delay
            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // throttle broadcast rate: register
                        LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                                .registerReceiver(frameReceiver, new IntentFilter("newface"));

                        // resume camera stream
                        cameraSource.start();
                    }
                },
                2000);
        }


    };

    private void setPostRequestContent(HttpURLConnection conn,
                                       JSONObject jsonObject) throws IOException {

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(jsonObject.toString());
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    public String HttpPost(String myUrl) throws IOException, JSONException {
        String result = "";

        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        // 2. build JSON object
        JSONObject jsonObject = buidJsonObject();

        // 3. add JSON content to POST request body
        setPostRequestContent(conn, jsonObject);

        // 4. make POST request to the given URL
        conn.connect();

        //int ret = conn.getResponseCode();

        // 5. return response message
        return conn.getResponseMessage() + "";

    }

    private JSONObject buidJsonObject() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("name", "jamie");
        jsonObject.accumulate("country",  "taiwan");
        jsonObject.accumulate("twitter",  "new taipei");

        return jsonObject;
    }

}
