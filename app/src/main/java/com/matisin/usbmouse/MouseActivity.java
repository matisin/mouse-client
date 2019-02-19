package com.matisin.usbmouse;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MouseActivity extends AppCompatActivity {
    boolean mBounded;
    MyService mService;
    Button clickIzq;
    Button clickDer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse);

        clickIzq = (Button) findViewById(R.id.click_izq);
        clickIzq.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mService.setClick(0);
            }
        });
        clickDer = (Button) findViewById(R.id.click_der);
        clickDer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mService.setClick(1);
            }
        });
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MouseActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MouseActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            MyService.LocalBinder mLocalBinder = (MyService.LocalBinder)service;
            mService = mLocalBinder.getServerInstance();
        }
    };
    @Override
    protected void onStart() {
        super.onStart();

        Intent mIntent = new Intent(this, MyService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    };
}
