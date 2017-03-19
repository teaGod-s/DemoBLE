package com.example.teagod.bletest;

/**
 * Created by teaGod on 2016/11/17.
 * 邮箱：1515979434@qq.com
 * 功能：这个Activity是用来显示已连接的蓝牙设备的数据
 */

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 对于一个给定的BLE设备，这个Activity提供了一个用来连接设备，显示数据，显示设备所支持
 * 的GATT services 和 characteristics的用户界面
 * 这个Activity用来与{@code BluetoothLeService}通信。
 * 而{@code BluetoothLeService}用来与Bluetooth LE API交互
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = "DeviceControlActivity";
    //请求打开蓝牙
    private static final int REQUEST_SCAN_BLUETOOTH = 1;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataDisplay;
    private Button mScanButton;
    private static String mDeviceName;
    private static String mDeviceAddress;// = "ED:26:7F:5A:14:87"

    private BluetoothLeService mBluetoothLeService;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    // 管理service生命周期的代码.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // 当成功启动初始化后自动连接设备.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // 处理由service引发的各种事件 .
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // 在用户界面上显示所有支持的services 和 characteristics.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.i(TAG, "前台发现可用服务并展示！！！！！！！！！！！");
                Log.i(TAG, mGattCharacteristics.toString());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "发现可用数据！！");
                if (!intent.getStringExtra(BluetoothLeService.HEART_RATE_DATA).isEmpty())
                    displayData(intent.getStringExtra(BluetoothLeService.HEART_RATE_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.

    private void clearUI() {
        mDataDisplay.setText(R.string.no_data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_SCAN_BLUETOOTH && resultCode == Activity.RESULT_OK) {
            NameAndAddressPreference.setStoredDeviceName(getApplicationContext(),
                    data.getStringExtra(EXTRAS_DEVICE_NAME));
            NameAndAddressPreference.setStoredDeviceAddress(getApplicationContext(),
                    data.getStringExtra(EXTRAS_DEVICE_ADDRESS));
            mDeviceName = NameAndAddressPreference.getStoredDeviceName(getApplicationContext());
            mDeviceAddress = NameAndAddressPreference.getStoredDeviceAddress(getApplicationContext());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        // Sets up UI references.
        showDeviceDetails();
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataDisplay = (TextView) findViewById(R.id.data_display);

        mScanButton = (Button) findViewById(R.id.scan_button);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceAddress = null;
                Intent tempI = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
                startActivityForResult(tempI, REQUEST_SCAN_BLUETOOTH);
            }
        });

        //设置返回上一层菜单
        //getActionBar().setDisplayHomeAsUpEnabled(false);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        showDeviceDetails();
    }

    private void showDeviceDetails() {
        //显示连接到的设备的信息，包括设备地址，设备名称
        mDeviceName = NameAndAddressPreference.getStoredDeviceName(getApplicationContext());
        mDeviceAddress = NameAndAddressPreference.getStoredDeviceAddress(getApplicationContext());
        getActionBar().setTitle(mDeviceName);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
//            case android.R.id.home:
//                onBackPressed();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataDisplay.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
//        String uuid = null;
//        String unknownServiceString = getResources().getString(R.string.unknown_service);
//        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

        // 迭代所有可用的GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            //临时存放当前service中所有的characteristic
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<>();

            // 查找当前service中所有的characteristic，并将每个characteristic都加到charas中
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);

                if (gattCharacteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT))) {
                    Log.i(TAG, "找到了心率characteristic！！！");
                    final int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // 如果这个characteristic上有一个主动的通知，
                        // 首先要清除它，以确保他不会更新用户界面上的数据域.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = gattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                gattCharacteristic, true);
                    }
                }
            }

            //charas中临时存放当前service中所有的characteristic，这行代码将蓝牙设备中所有的characteristic都加到一个集合中
            mGattCharacteristics.add(charas);
        }

//        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
//                this,
//                gattServiceData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[] {LIST_NAME, LIST_UUID},
//                new int[] { android.R.id.text1, android.R.id.text2 },
//                gattCharacteristicData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[] {LIST_NAME, LIST_UUID},
//                new int[] { android.R.id.text1, android.R.id.text2 }
//        );
//        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
