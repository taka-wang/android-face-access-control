package net.cmwang.accesscontrol;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;

public class LivePreviewActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "LivePreviewActivity";
    public static final String REFRESH_ACTIVITY = "net.cmwang.action.REFRESH_UI"; // dummy action
    private static Context context;

    public static Context getAppContext() {
        return context;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livepreview);

        ButterKnife.bind(this); // Binds

        // get runtime permissions
        new PermissionDelegate(this).getPermissions();

    }

}
