package net.cmwang.face.access;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class MainActivity extends ActionBarActivity {
    @BindView(R.id.door_number) EditText doorView;
    @BindView(R.id.server_address) EditText serverAddressView;
    @BindView(R.id.devices_list_view) ListView deviceListView;
    @BindString(R.string.bluetooth_not_available) String noBluetoothMessage;
    @BindString(R.string.empty_door_number) String noDoorNumberMessage;
    @BindString(R.string.empty_server_address) String noServerAddressMessage;
    @BindString(R.string.no_paired_devices) String noPairedDeviceMessage;
    @BindString(R.string.connect_fail) String connectionFailMessage;
    @BindString(R.string.connect_msg_title) String connectingTitleMessage;
    @BindString(R.string.wait_msg) String waitMessage;
    @BindString(R.string.extra_door_number) String EXTRA_DOOR_NUMBER;
    @BindString(R.string.extra_server_address) String EXTRA_SERVER_ADDRESS;

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUESTS = 1;
    private ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Binds
        ButterKnife.bind(this);

        // check Bluetooth status
        if (!BluetoothSerial.IsAvailable()) {
            longToast(noBluetoothMessage);
            finish(); // exit
        } else if (!BluetoothSerial.IsEnabled()) {
            //Ask the user to turn the bluetooth on
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothSerial.Disconnect(); // disconnect Bluetooth connection
    }

    /* Handle get paired device button */
    @OnClick(R.id.device_paired)
    protected void onGetPairedDevices(View view) {
        ArrayList pairedDevicesList = new ArrayList();
        Set<BluetoothDevice> pairedDevicesSet = BluetoothSerial.GetPairedDevices();

        if (pairedDevicesSet.size() > 0) {
            for (BluetoothDevice device : pairedDevicesSet) {
                pairedDevicesList.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            longToast(noPairedDeviceMessage);
        }

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDevicesList);
        deviceListView.setAdapter(adapter);
    }

    /* Handle device list item on click event */
    @OnItemClick(R.id.devices_list_view)
    protected void onItemClick(int position) {

        // Get the device MAC address, the last 17 chars in the View
        String info = adapter.getItem(position).toString();
        String macAddressText = info.substring(info.length() - 17);

        // get door number from EditText
        String doorNumberText = doorView.getText().toString().trim();
        if (doorNumberText.matches("")) {
            shortToast(noDoorNumberMessage);
            return;
        }

        // get server address from EditText
        String serverAddressText = serverAddressView.getText().toString().trim();
        if (serverAddressText.matches("")) {
            shortToast(noServerAddressMessage);
            return;
        }

        Log.i(TAG, "MAC: " + macAddressText + " Door: " + doorNumberText + " Server: " + serverAddressText);

        // async connect
        new connectTask(macAddressText, doorNumberText, serverAddressText).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* Async connect to remote bluetooth device */
    private class connectTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgress;
        private String mMacAddress;
        private String mDoorNumber;
        private String mServerAddress;

        public connectTask(String address, String doorNumber, String serverAddress) {
            super();
            this.mMacAddress = address;
            this.mDoorNumber = doorNumber;
            this.mServerAddress = serverAddress;
        }

        @Override
        protected void onPreExecute() {
            this.mProgress = ProgressDialog.show(MainActivity.this, connectingTitleMessage, waitMessage);  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... voids) {
            BluetoothSerial.Connect(this.mMacAddress);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (BluetoothSerial.IsConnected()) {
                // start new activity
                Intent intent = new Intent(MainActivity.this, DoorControlActivity.class);
                intent.putExtra(EXTRA_DOOR_NUMBER, this.mDoorNumber);
                intent.putExtra(EXTRA_SERVER_ADDRESS, this.mServerAddress);
                startActivity(intent);
            } else {
                shortToast(connectionFailMessage);
            }
            this.mProgress.dismiss();
        }
    }

    /* Show toast message */
    private void showToast(Context ctx, String msg, int duration) {
        Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        // set toast position to center
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    /* Show short toast message */
    private void shortToast(String msg) {
        showToast(getApplicationContext(), msg, Toast.LENGTH_SHORT);
    }

    /* Show long toast message */
    private void longToast(String msg) {
        showToast(getApplicationContext(), msg, Toast.LENGTH_SHORT);
    }
}
