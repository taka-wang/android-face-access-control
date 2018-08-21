package net.cmwang.face.access;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class DoorControlActivity extends AppCompatActivity {
    //String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door_control);

        /*
        // get mac address
        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);

        // connect to remote device
        BluetoothSerial.Connect(address);
        */
    }

    public void onOpenDoor(View view) {
        BluetoothSerial.OpenDoor();
    }
}
