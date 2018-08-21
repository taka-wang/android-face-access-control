package net.cmwang.face.access;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothSerial {

    private static final String TAG = "Bluetooth";
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP
    private static final int relayDelayMs = 300; // relay on off delay in ms

    private static boolean isConnected = false;
    private static BluetoothAdapter mAdapter = null;
    private static BluetoothSocket mSerialSocket = null;

    /* singleton */
    private BluetoothSerial() { }

    /* lazy initialization */
    private static BluetoothAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        Log.d(TAG, "getAdapter: " + mAdapter);
        return mAdapter;
    }

    /* Send bluetooth serial command */
    private static boolean sendCommand(String cmd) {
        try {
            mSerialSocket.getOutputStream().write(cmd.getBytes());
            return true;
        } catch (IOException e) {
            Log.d(TAG, "Serial command: " + cmd + " : " + e);
        }
        return false;
    }

    /* Check bluetooth device is available or not */
    public static boolean HasAdapter() {
        return getAdapter() != null;
    }

    /* Check bluetooth device is enable or not */
    public static boolean IsEnabled() {
        return getAdapter().isEnabled();
    }

    /* Get paired bluetooth devices */
    public static Set<BluetoothDevice> GetPairedDevices() {
        Set<BluetoothDevice> pairedSet = getAdapter().getBondedDevices();
        Log.d(TAG, "GetPairedDevices: " + pairedSet);
        return pairedSet;
    }

    /* Check SPP is connect or not */
    public static boolean isConnected() {
        return isConnected;
    }

    /* Connect to remote bluetooth device */
    public static boolean Connect(String address) {
        Log.d(TAG, "Connect");

        try {
            if (mSerialSocket == null || !isConnected) {
                BluetoothDevice remoteDevice = getAdapter().getRemoteDevice(address);
                mSerialSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(uuid); //create a RFCOMM (SPP) connection
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                mSerialSocket.connect(); // start connection
                isConnected = true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Connect: " + e);
        }

        return isConnected;
    }

    /* Disconnect from remote device */
    public static boolean Disconnect() {
        Log.d(TAG, "Disconnect");
        // set connect flag
        isConnected = false;

        if (mSerialSocket != null) {
            try {
                mSerialSocket.close();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Disconnect: " + e);
            }
        }
        return false;
    }

    /* Send open door serial command */
    public static boolean OpenDoor() {
        Log.d(TAG, "Open Door");

        final boolean[] isSuccess = {false};
        if (mSerialSocket != null) {
            isSuccess[0] = sendCommand("1");
            // wait a moment
            new Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        isSuccess[0] = sendCommand("0");
                    }
                }, relayDelayMs
            );
        }
        return isSuccess[0];
    }
}
