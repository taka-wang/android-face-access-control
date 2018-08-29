package net.cmwang.accesscontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Set;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnTextChanged;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.doorNumberWrapper) TextInputLayout doorNumberWrapper;
    @BindView(R.id.serverAddressWrapper) TextInputLayout serverAddressWrapper;
    @BindView(R.id.devices_list_view) ListView deviceListView;

    @BindString(R.string.bluetooth_not_available_msg) String noBluetoothMessage;
    @BindString(R.string.empty_door_number_msg) String noDoorNumberMessage;
    @BindString(R.string.empty_server_address_msg) String noServerAddressMessage;
    @BindString(R.string.no_paired_devices_msg) String noPairedDeviceMessage;
    @BindString(R.string.connection_fail_msg) String connectionFailMessage;
    @BindString(R.string.connection_title_msg) String connectingTitleMessage;
    @BindString(R.string.wait_msg) String waitMessage;
    @BindString(R.string.extra_door_number) String EXTRA_DOOR_NUMBER;
    @BindString(R.string.extra_server_address) String EXTRA_SERVER_ADDRESS;

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "preference";
    private ArrayAdapter adapter;
    private SharedPreferences prefs;
    private ToastUtils toast;
    private static int clickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toast = new ToastUtils(getApplicationContext()); // inti toast util
        ButterKnife.bind(this); // Binds

        // Load preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        doorNumberWrapper.getEditText().setText(
                prefs.getString(EXTRA_DOOR_NUMBER,
                        doorNumberWrapper.getEditText().getText().toString().trim()));
        serverAddressWrapper.getEditText().setText(
                prefs.getString(EXTRA_SERVER_ADDRESS,
                        serverAddressWrapper.getEditText().getText().toString().trim()));

        // get runtime permissions
        new PermissionDelegate(this).getPermissions();

        // check Bluetooth status
        if (!Bluetooth.IsAvailable()) {
            toast.longMSG(noBluetoothMessage);
            finish(); // exit
        } else if (!Bluetooth.IsEnabled()) {
            //Ask the user to turn the bluetooth on
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bluetooth.Disconnect(); // disconnect Bluetooth connection
    }

    private void maintenanceCheck() {
        clickCount++;
        if (clickCount == 5) {
            toast.shortMSG("Almost there! " + (9-clickCount) + " more!");
        }
        if (clickCount == 9) {
            //clickCount = 0;
            doorNumberWrapper.setVisibility(View.VISIBLE);
            serverAddressWrapper.setVisibility(View.VISIBLE);
        }
    }

    /* Handle get paired device button */
    @OnClick(R.id.device_paired)
    protected void onGetPairedDevices(View view) {
        maintenanceCheck();

        ArrayList pairedDevicesList = new ArrayList();
        Set<BluetoothDevice> pairedDevicesSet = Bluetooth.GetPairedDevices();

        if (pairedDevicesSet.size() > 0) {
            for (BluetoothDevice device : pairedDevicesSet) {
                pairedDevicesList.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            toast.longMSG(noPairedDeviceMessage);
        }

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDevicesList);
        deviceListView.setAdapter(adapter);
    }


    @OnTextChanged(R.id.door_number)
    protected void validateDoorNumber(Editable editable) {
        if (editable.length() == 0) {
            doorNumberWrapper.setError(noDoorNumberMessage);
            return;
        }
        doorNumberWrapper.setErrorEnabled(false);
    }

    @OnTextChanged(R.id.server_address)
    protected void validateServerAddress(Editable editable) {
        if (editable.length() == 0) {
            serverAddressWrapper.setError(noServerAddressMessage);
            return;
        }
        serverAddressWrapper.setErrorEnabled(false);
    }

    /* Handle device list item on click event */
    @OnItemClick(R.id.devices_list_view)
    protected void onItemClick(int position) {

        // Get the device MAC address, the last 17 chars in the View
        String info = adapter.getItem(position).toString();
        String macAddressText = info.substring(info.length() - 17);

        // get door number from EditText
        String doorNumberText = doorNumberWrapper.getEditText().getText().toString().trim();
        if (doorNumberText.matches("")) {
            doorNumberWrapper.setError(noDoorNumberMessage);
            return;
        }
        doorNumberWrapper.setErrorEnabled(false);

        // get server address from EditText
        String serverAddressText = serverAddressWrapper.getEditText().getText().toString().trim();
        if (serverAddressText.matches("")) {
            serverAddressWrapper.setError(noServerAddressMessage);
            return;
        }
        serverAddressWrapper.setErrorEnabled(false);

        // save preference
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(EXTRA_DOOR_NUMBER, doorNumberText);
        editor.putString(EXTRA_SERVER_ADDRESS, serverAddressText);
        editor.apply();

        Log.i(TAG, "MAC: " + macAddressText + " Door: " + doorNumberText + " Server: " + serverAddressText);

        // async connect
        new connectToBluetoothTask(macAddressText, doorNumberText, serverAddressText).execute();
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
    private class connectToBluetoothTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgress;
        private String mMacAddress;
        private String mDoorNumber;
        private String mServerAddress;

        public connectToBluetoothTask(String address, String doorNumber, String serverAddress) {
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
            Bluetooth.Connect(this.mMacAddress);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (Bluetooth.IsSPPConnected()) {
                // start new activity

                //Intent intent = new Intent(MainActivity.this, DoorControlActivity.class);
                Intent intent = new Intent(MainActivity.this, LivePreviewActivity.class);
                intent.putExtra(EXTRA_DOOR_NUMBER, this.mDoorNumber);
                intent.putExtra(EXTRA_SERVER_ADDRESS, this.mServerAddress);
                startActivity(intent);
            } else {
                toast.shortMSG(connectionFailMessage);
            }
            this.mProgress.dismiss();
        }
    }
}
