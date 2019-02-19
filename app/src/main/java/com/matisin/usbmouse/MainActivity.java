package com.matisin.usbmouse;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    static final int REQUEST_ENABLE_BT = 1;
    private BluetoothDevice[] devices;
    private OutputStream outputStream;
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        setContentView(R.layout.activity_main);

        ListView mListaDispositivos = (ListView) findViewById(R.id.lista_dispositivos);

        mListaDispositivos.setOnItemClickListener(this);
        mListaDispositivos.setAdapter(itemsAdapter);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //NO HAU SUPPORT
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                devices = pairedDevices.toArray(new BluetoothDevice[0]);
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        itemsAdapter.add(device.getName() + "\n" + device.getAddress());

                        Log.d(device.getName(), device.getAddress());
                        //finish();
                    }
                }
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String item = (String)parent.getItemAtPosition(position);
            String lines[] = item.split("\\r?\\n");

        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("position",position);
        startService(serviceIntent);

        Intent intent = new Intent(this, MouseActivity.class);
        startActivity(intent);


    }
}
