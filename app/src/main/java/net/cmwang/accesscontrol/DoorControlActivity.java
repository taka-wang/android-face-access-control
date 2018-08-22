package net.cmwang.accesscontrol;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class DoorControlActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door_control);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.button)
    public void onOpenDoor(View view) {
        Bluetooth.OpenDoor();
    }
}
