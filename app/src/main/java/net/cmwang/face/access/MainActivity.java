package net.cmwang.face.access;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUESTS = 1;
    public static String EXTRA_ADDRESS = "device_address";
    public static String EXTRA_DOOR = "door_number";
    public static String EXTRA_SERVER = "server_address";
    private ProgressDialog progress;

    /* Check Bluetooth status */
    private void checkBluetooth() {
        if (!BluetoothSerial.HasAdapter()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            finish();
        } else if (!BluetoothSerial.IsEnabled()) {
            //Ask the user to turn the bluetooth on
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        }
    }

    /* Show short toast message */
    private void showToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        // set toast position to center
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // disconnect Bluetooth connection
        BluetoothSerial.Disconnect();
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String addressText = info.substring(info.length() - 17);

            // get door number from EditText
            EditText doorView = (EditText)findViewById(R.id.door_number);
            String doorNumberText = doorView.getText().toString().trim();
            if (doorNumberText.matches("")) {
                showToast("Empty door number!");
                return;
            }

            // get server address from EditText
            EditText serverAddressView = (EditText)findViewById(R.id.server_address);
            String serverAddressText = serverAddressView.getText().toString().trim();
            if (serverAddressText.matches("")) {
                showToast("Empty server address!");
                return;
            }

            Log.i(TAG, "MAC: " + addressText + " Door: " + doorNumberText + " Server: " + serverAddressText);

            // async connect
            new connectTask(addressText, doorNumberText, serverAddressText).execute();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Handle get paired device button */
    public void onGetPairedDevices(View view) {
        ArrayList list = new ArrayList();
        Set<BluetoothDevice> pairedDevicesSet = BluetoothSerial.GetPairedDevices();

        if (pairedDevicesSet.size() > 0) {
            for(BluetoothDevice bt : pairedDevicesSet) {
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);

        ListView deviceListView = (ListView)findViewById(R.id.devices_list_view);
        deviceListView.setAdapter(adapter);
        deviceListView.setOnItemClickListener(myListClickListener);
    }

    /* Async connect to remote bluetooth device */
    private class connectTask extends AsyncTask<Void, Void, Void> {
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
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... voids) {
            BluetoothSerial.Connect(this.mMacAddress);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (BluetoothSerial.isConnected()) {
                // start new activity
                Intent intent = new Intent(MainActivity.this, DoorControlActivity.class);
                intent.putExtra(EXTRA_DOOR, this.mDoorNumber);
                intent.putExtra(EXTRA_SERVER, this.mServerAddress);
                startActivity(intent);
            } else {
                showToast("Connection Failed!");
            }
            progress.dismiss();
        }
    }
}
