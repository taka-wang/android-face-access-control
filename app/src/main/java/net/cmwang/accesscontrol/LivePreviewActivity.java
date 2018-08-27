package net.cmwang.accesscontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;
import io.fotoapparat.view.CameraView;

public class LivePreviewActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    @BindView(R.id.cameraView) CameraView cameraView;
    @BindView(R.id.overlayView) GraphicOverlay overlayView;
    @BindView(R.id.toggleButton) FloatingActionButton floatButton;
    @BindString(R.string.extra_door_number) String EXTRA_DOOR_NUMBER;
    @BindString(R.string.extra_server_address) String EXTRA_SERVER_ADDRESS;

    @BindString(R.string.face_uri) String faceURI; // REST URI
    @BindString(R.string.qrcode_uri) String qrCodeURI; // REST URI
    @BindString(R.string.user_name_json_field) String userNameJsonField; // response field
    @BindString(R.string.image_json_field) String imageJsonField; // request field
    @BindString(R.string.door_json_field) String doorJsonField; // request field
    @BindString(R.string.uuid_json_field) String uuidJsonField; // request field

    @BindString(R.string.face_title) String faceAppTitle; // activity title
    @BindString(R.string.qrcode_title) String qrCodeAppTitle; // activity title
    @BindString(R.string.disable_back_press) String disableBackPressMsg;
    @BindString(R.string.user_not_found) String userNotFoundMsg;
    @BindString(R.string.welcome_msg) String welcomeMsg;
    @BindString(R.string.anonymous) String unknownUserName;

    public static final String intentFilterNewFrame = "new_face_frame"; // broadcast filter
    public static final String intentFilterNewUUID = "new_user_uuid"; // broadcast filter
    public static final String intentExtraFrame = "face"; // broadcast payload
    public static final String intentExtraUUID = "user_uuid"; // broadcast payload

    public static String doorNumber; // user input
    public static String serverURL; // user input

    private static final String TAG = "LivePreviewActivity";
    private static Context context;
    private boolean isFaceMode = true; // current machine learning mode
    private CameraSource cameraSource;

    private final int toastFontSize = 24;
    private final String toastFontType = "sans-serif-smallcaps";
    private final int connectionTimeoutMS = 2000;
    private final int delayMS = 2000;

    /* get current application context */
    public static Context getAppContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livepreview);

        context = this; // set context

        ButterKnife.bind(this); // Binds

        setToastyStyle(); // change toasty style

        new PermissionDelegate(this).getPermissions(); // get runtime permissions

        cameraSource = new CameraSource(getApplicationContext(), cameraView, overlayView);

        /* StrictMode.ThreadPolicy.Builder */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getAllExtras(); // get user inputs from the intent
        registerAllBroadcast(); // register frame and qr code broadcasts
        cameraSource.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterAllBroadcast(); // unregister frame and qr code broadcasts
        cameraSource.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cameraSource.release();
    }

    /* Disable back press key function */
    @Override
    public void onBackPressed() {
        // super.onBackPressed(); /* commented this line in order to disable back press */
        Toasty.error(this, disableBackPressMsg).show();
    }

    /* Handle mode change button */
    @OnClick(R.id.toggleButton)
    protected void onToggleClick(View v) {
        if (isFaceMode) {
            isFaceMode = false;
        } else {
            isFaceMode = true;
        }
        setVisionMode(isFaceMode);
    }

    /* change toasty default style */
    private void setToastyStyle() {
        Toasty.Config.getInstance()
                .setTextSize(toastFontSize)
                .setToastTypeface(Typeface.create(toastFontType, Typeface.NORMAL))
                .apply();
    }

    /* get user inputs from the intent */
    private void getAllExtras() {
        Intent intent = getIntent();
        doorNumber = intent.getStringExtra(EXTRA_DOOR_NUMBER);
        serverURL = intent.getStringExtra(EXTRA_SERVER_ADDRESS);
        Log.d(TAG, "door: " + doorNumber + ", address: " + serverURL);
    }

    /* register all broadcasts */
    private void registerAllBroadcast() {
        LocalBroadcastManager.getInstance(this).registerReceiver(userUUIDReceiver, new IntentFilter(intentFilterNewUUID));
        LocalBroadcastManager.getInstance(this).registerReceiver(frameReceiver, new IntentFilter(intentFilterNewFrame));
    }

    /* unregister all broadcasts */
    private void unregisterAllBroadcast() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(frameReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userUUIDReceiver);
    }

    /* change machine learning mode */
    private void setVisionMode(boolean doFaceRecognition) {
        ActionBar bar = getSupportActionBar();
        int black = getColor(R.color.colorBlack);
        int red = getColor(R.color.colorRed);

        if (doFaceRecognition) {
            cameraSource.setDefaultProcessor(CameraSource.faceDetection);
            // floating button
            floatButton.setImageResource(R.drawable.ic_barcode_scan);
            floatButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorBlack));
            // action bar
            bar.setTitle(faceAppTitle);
            bar.setBackgroundDrawable(new ColorDrawable(black));
            // status
            getWindow().setStatusBarColor(black);
            // menu
            getWindow().setNavigationBarColor(black);

        } else {
            cameraSource.setDefaultProcessor(CameraSource.qrcodeScanning);
            // floating button
            floatButton.setImageResource(R.drawable.ic_face);
            floatButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorRed));
            // action bar
            bar.setTitle(qrCodeAppTitle);
            bar.setBackgroundDrawable(new ColorDrawable(red));
            // status
            getWindow().setStatusBarColor(red);
            // menu
            getWindow().setNavigationBarColor(red);
        }
    }

    /* qr code scanning broadcast */
    public BroadcastReceiver userUUIDReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // throttle broadcast rate: unregister
            LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext()).unregisterReceiver(userUUIDReceiver);

            // stop camera stream
            cameraSource.stop();

            // get uuid from intent
            String userUUID = intent.getStringExtra(intentExtraUUID);
            Log.d(TAG, "Got new UUID: " + userUUID);

            // prepare POST content
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.accumulate(uuidJsonField, userUUID);
                jsonObject.accumulate(doorJsonField, doorNumber);
            } catch (JSONException e) {
                Log.e(TAG, "Prepare POST content error: " + e);
                return;
            }

            // TODO: refactor
            String url = "http://" + serverURL + "/" + qrCodeURI;

            // Send HTTP POST request
            HttpPost(url, jsonObject);

            // delay
            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // throttle broadcast rate: register
                        LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                                .registerReceiver(userUUIDReceiver, new IntentFilter(intentFilterNewUUID));

                        // resume camera stream
                        cameraSource.start();
                    }
                }, delayMS
            );
        }
    };

    /* face detection broadcast */
    public BroadcastReceiver frameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new face broadcast");

            // throttle broadcast rate: unregister
            LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                    .unregisterReceiver(frameReceiver);

            // get frame from intent
            VisionFrame frame = intent.getParcelableExtra(intentExtraFrame);

            // convert bitmap to base64
            String base64String = frame.toBase64();

            /* trick! stop camera after conversion for better user experience
             * stop camera stream
             */
            cameraSource.stop();

            // prepare POST content
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.accumulate(imageJsonField, base64String);
                jsonObject.accumulate(doorJsonField, doorNumber);
            } catch (JSONException e) {
                Log.e(TAG, "Prepare POST content error: " + e);
                return;
            }

            // TODO: refactor
            String url = "http://" + serverURL + "/" + faceURI;

            // Send HTTP POST request
            HttpPost(url, jsonObject);

            // delay
            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // throttle broadcast rate: register
                        LocalBroadcastManager.getInstance(LivePreviewActivity.getAppContext())
                                .registerReceiver(frameReceiver, new IntentFilter(intentFilterNewFrame));

                        // resume camera stream
                        cameraSource.start();
                    }
                }, delayMS
            );
        }
    };

    /* Send HTTP POST with JSON object to remote server */
    public void HttpPost(String uri, JSONObject requestObject) {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Illegal URL");
            return;
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setConnectTimeout(connectionTimeoutMS);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            setPostRequestContent(conn, requestObject);

            conn.connect();

            // handle the response
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                // show error message
                Toasty.error(LivePreviewActivity.getAppContext(), userNotFoundMsg, Toast.LENGTH_SHORT, true).show();
            } else {
                // parse JSON response
                JSONObject jsonObj = new JSONObject(getPostResponseString(conn));
                String username;
                if (jsonObj.has(userNameJsonField)) {
                    username = jsonObj.getString(userNameJsonField);
                } else {
                    // cannot obtain username from the backend?!
                    username = unknownUserName;
                }

                // open the door and show success message
                Bluetooth.OpenDoor();
                Toasty.success(LivePreviewActivity.getAppContext(), welcomeMsg + " " + username, Toast.LENGTH_SHORT, true).show();
            }
        } catch (Exception e) {
            // show error message and terminate the activity
            Toasty.error(LivePreviewActivity.getAppContext(), e.toString(), Toast.LENGTH_SHORT, true).show();
            LivePreviewActivity.this.finish();
        } finally {
            // disconnect the HTTP POST
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /* get HTTP JSON response */
    private String getPostResponseString(HttpURLConnection conn) throws IOException {
        StringBuffer response = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    /* set HTTP JSON POST object */
    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(jsonObject.toString());
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }
}
