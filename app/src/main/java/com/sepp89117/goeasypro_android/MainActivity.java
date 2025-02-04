package com.sepp89117.goeasypro_android;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {
    private Button btn_pair;

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<GoProDevice> goProDevices = new ArrayList<>();

    private GoListAdapter goListAdapter;
    private ListView goListView;
    private View mLayout;

    private static final int BT_PERMISSIONS_CODE = 87;
    String[] PERMISSIONS_S_UP = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
    };
    String[] PERMISSIONS_R_DOWN = {
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_pair = findViewById(R.id.btn_pair);
        btn_pair.setEnabled(false);

        mLayout = findViewById(R.id.main_layout);

        //get bluetoothManager and bluetoothAdapter
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported! Please use other device!", Toast.LENGTH_LONG).show();
            return; //TODO exit app
        }

        ((MyApplication) this.getApplication()).setBluetoothAdapter(bluetoothAdapter);
        ((MyApplication) this.getApplication()).setGoProDevices(goProDevices);

        goListAdapter = new GoListAdapter(goProDevices, getApplicationContext());

        goListView = (ListView) findViewById(R.id.goListView);
        goListView.setAdapter(goListAdapter);
        registerForContextMenu(goListView);

        //request maybe bluetooth permission
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }

        if (isBtEnabled()) {
            startApp();
        }

        setOnDataChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Single Control");

        // add(int groupId, int itemId, int order, CharSequence title)
        menu.add(0, 0, 0, "Locate: on");
        menu.add(0, 1, 0, "Locate: off");
        menu.add(0, 2, 0, "Shutter: on");
        menu.add(0, 3, 0, "Shutter: off");
        menu.add(0, 4, 0, "Put to sleep");
        menu.add(0, 5, 0, "Set Date/Time");
    }

    // menu item select listener
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        int itemId = item.getItemId();

        switch (itemId) {
            case 0:
                //Locate on
                goProDevices.get(position).locateOn();
                break;
            case 1:
                //Locate: off
                goProDevices.get(position).locateOff();
                break;
            case 2:
                //Shutter: on
                goProDevices.get(position).shutterOn();
                break;
            case 3:
                //Shutter: off
                goProDevices.get(position).shutterOff();
                break;
            case 4:
                //Put to sleep
                goProDevices.get(position).sleep();
                break;
            case 5:
                //Set Date/Time
                goProDevices.get(position).setDateTime();
                break;
        }
        return true;
    }

    private void setOnDataChanged() {
        for (int i = 0; i < goProDevices.size(); i++) {
            GoProDevice goProDevice = goProDevices.get(i);
            goProDevice.getDataChanges(() -> runOnUiThread(() -> goListView.setAdapter(goListAdapter)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        goListView.setAdapter(goListAdapter);
        setOnDataChanged();
    }

    public void onClickPair(View v) {
        Intent i = new Intent(getApplicationContext(), PairActivity.class);
        startActivity(i);
    }

    @SuppressLint("MissingPermission")
    public void onClickShutterOn(View v) {
        if (goProDevices.size() <= 0) {
            Toast.makeText(getApplicationContext(), "Please connect/pair devices", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < goProDevices.size(); i++) {
            goProDevices.get(i).shutterOn();
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickShutterOff(View v) {
        if (goProDevices.size() <= 0) {
            Toast.makeText(getApplicationContext(), "Please connect/pair devices", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < goProDevices.size(); i++) {
            goProDevices.get(i).shutterOff();
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickSleep(View v) {
        if (goProDevices.size() <= 0) {
            Toast.makeText(getApplicationContext(), "Please connect/pair devices", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < goProDevices.size(); i++) {
            goProDevices.get(i).sleep();
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickWifiApOn(View v) {
        if (goProDevices.size() <= 0) {
            Toast.makeText(getApplicationContext(), "Please connect/pair devices", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < goProDevices.size(); i++) {
            goProDevices.get(i).wifiApOn();
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickWifiApOff(View v) {
        if (goProDevices.size() <= 0) {
            Toast.makeText(getApplicationContext(), "Please connect/pair devices", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < goProDevices.size(); i++) {
            goProDevices.get(i).wifiApOff();
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickHighlight(View v) {
        if (goProDevices.size() <= 0) {
            Toast.makeText(getApplicationContext(), "Please connect/pair devices", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < goProDevices.size(); i++) {
            goProDevices.get(i).highlight();
        }
    }

    public boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (String permission : PERMISSIONS_S_UP) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        } else {
            for (String permission : PERMISSIONS_R_DOWN) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
    }

    private void startApp() {
        btn_pair.setEnabled(true);
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName != null && deviceName.contains("GoPro ")) {
                    boolean inList = false;
                    if (goProDevices.size() > 0) {
                        for (int i = 0; i < goProDevices.size(); i++) {
                            GoProDevice goProDevice = goProDevices.get(i);
                            if (Objects.equals(goProDevice.Address, device.getAddress())) {
                                inList = true;
                            }
                        }
                    }
                    if (!inList) {
                        GoProDevice goProDevice = new GoProDevice();
                        goProDevice.context = getApplicationContext();
                        goProDevice.bluetoothDevice = device;
                        goProDevice.paired = true;
                        goProDevice.Name = deviceName;
                        goProDevice.Address = deviceHardwareAddress;
                        goProDevice.connected = MyApplication.checkBtDevConnected(device);
                        goProDevices.add(goProDevice);
                        goListView.setAdapter(goListAdapter);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BT_PERMISSIONS_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                switch (permission) {
                    case Manifest.permission.BLUETOOTH: //granted
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH}, BT_PERMISSIONS_CODE);
                        break;
                    case Manifest.permission.BLUETOOTH_ADMIN: //granted
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, BT_PERMISSIONS_CODE);
                        break;
                    case Manifest.permission.BLUETOOTH_SCAN: //not granted
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, BT_PERMISSIONS_CODE);
                        break;
                    case Manifest.permission.BLUETOOTH_CONNECT: //not granted
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BT_PERMISSIONS_CODE);
                        break;
                    case Manifest.permission.ACCESS_FINE_LOCATION: //granted
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BT_PERMISSIONS_CODE);
                        break;
                    case Manifest.permission.ACCESS_COARSE_LOCATION: //granted
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, BT_PERMISSIONS_CODE);
                        break;
                }

                if (hasPermissions()) {
                    if (isBtEnabled()) {
                        startApp();
                    }
                    setOnDataChanged();
                }

//                if (permission.equals(Manifest.permission.BLUETOOTH_CONNECT)) {
//                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                        if (isBtEnabled()) {
//                            startApp();
//                        }
//                        setOnDataChanged();
//                    } else {
//                        requestPermissions();
//                    }
//                }
            }
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_CONNECT)) {
                Snackbar.make(mLayout, "Bluetooth permission is needed to connect GoPros.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", view -> ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_S_UP, BT_PERMISSIONS_CODE))
                        .show();
            } else {
                // Camera permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(this, PERMISSIONS_S_UP, BT_PERMISSIONS_CODE);
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH)) {
                Snackbar.make(mLayout, "Bluetooth permission is needed to connect GoPros.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", view -> ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_R_DOWN, BT_PERMISSIONS_CODE))
                        .show();
            } else {
                // Camera permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(this, PERMISSIONS_R_DOWN, BT_PERMISSIONS_CODE);
            }
        }
    }

    private boolean isBtEnabled() {
        //check bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            //request bluetooth enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityIntent.launch(enableBtIntent);
            return false;
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;

            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {
            }

            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ignored) {
            }

            if (!gps_enabled && !network_enabled) {
                new AlertDialog.Builder(this)
                        .setMessage("Localization is required for the Bluetooth functions. Please turn localization on!")
                        .setPositiveButton("OK", (paramDialogInterface, paramInt) -> MainActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    // BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    isBtEnabled();
                }
            }
        }
    };

    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (isBtEnabled()) {
                    startApp();
                }
            });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception ex) {

        }
    }
}