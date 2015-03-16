package com.example.jenniferdai.cookyourrice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    private static final int REQUEST_ENABLE_BT = 1;

    Button onoff;
    Button keepWarm;
    TextView cookerStatus;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get widgets to interact with
        onoff = ((Button) findViewById(R.id.onoff));
        keepWarm = ((Button) findViewById(R.id.warm));
        cookerStatus = ((TextView) findViewById(R.id.status));
//        setAlarm = ((Button) findViewById(R.id.button5));
//        help = ((Button) findViewById(R.id.button6));

        // Initializes Bluetooth Adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(!mScanning);
    }

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    UUID serviceUUID = UUID.fromString("00003003-0000-1000-8000-00805f9b34fb");
    UUID characteristicUUID = UUID.fromString("00004005-0000-1000-8000-00805f9b34fb");
    private String deviceMac = "E2:B8:CB:31:54:C3";
    BluetoothGatt mGatt = null;
    private boolean mScanning = false;
    private Handler mHandler = new Handler();

    private String status = "off";

//    String url = "http://[fe80::1cef:49e7:2661:ffda]:7001/a.MOV";
//    HttpParams httpParameters = new BasicHttpParams();
//    HttpClient client = new DefaultHttpClient(httpParameters);
//    HttpGet httpGet = new HttpGet(url);
//    HttpResponse response = client.execute(httpGet);

    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
             System.out.println("devie address " + device.getAddress());
            // System.out.println("uuids " + device.getUuids());
            if (device.getAddress().equals(deviceMac)) {
                System.out.println("found our firestorm");
                mGatt = device.connectGatt(getApplicationContext(), true, gattCallback);
                System.out.println(mGatt);
            }
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            System.out.println("discovered services!");
            // runOnUiThread(() {
//                    connecting.setVisibility(View.GONE);
//                    send.setVisibility(View.VISIBLE);
            // });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int stat) {
            super.onCharacteristicWrite(gatt, characteristic, stat);
            System.out.println("onCharWrite: "+stat+", "+characteristic.getStringValue(0));
            if(characteristic.getUuid().equals(characteristicUUID)){
                if(characteristic.getValue()[0] == 0x00){
                    status = "off";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onoff.setText("Start");
                            cookerStatus.setText("Off");
                        }
                    });
                } else if(characteristic.getValue()[0] == 0x01){
                    status = "on";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onoff.setText("Stop");
                            cookerStatus.setText("On");
                        }
                    });
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS){
                gatt.discoverServices();
            } else {
                System.out.println("gatt connection: " + status);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        start.setVisibility(View.GONE);
//                        connecting.setVisibility(View.VISIBLE);
//                        send.setVisibility(View.GONE);
                    }
                });
            }
        }
    };

    public void toggleRiceCooker(View view) {
        if(onoff.getText().equals("Sending..."))
            return;
        BluetoothGattCharacteristic writeChar = mGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
        if(status.equals("off"))
            writeChar.setValue(new byte[]{0x01});
        else
            writeChar.setValue(new byte[]{0x00});
        onoff.setText("Sending...");
        mGatt.writeCharacteristic(writeChar);
    }



}