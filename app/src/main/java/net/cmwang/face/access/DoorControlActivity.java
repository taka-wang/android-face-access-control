package net.cmwang.face.access;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class DoorControlActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door_control);
    }

    public void onOpenDoor(View view) {
        BluetoothSerial.OpenDoor();
    }
}
