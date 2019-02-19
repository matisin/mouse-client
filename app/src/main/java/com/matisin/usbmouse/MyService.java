package com.matisin.usbmouse;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MyService extends Service implements SensorEventListener {

    IBinder mBinder = new LocalBinder();

    public static final int notify = 1;  //interval between two services(Here Service run every 5 seconds)
    private Handler mHandler = new Handler();   //run on another Thread to avoid crash
    private Timer mTimer = null;    //timer handling
    private BluetoothAdapter mBluetoothAdapter;
    private OutputStream outputStream;

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    private BluetoothSocket socket;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    public class LocalBinder extends Binder {
        public MyService getServerInstance() {
            return MyService.this;
        }
    }

    public void setClick(int click) {
        String clickString = "1*"+ click;
        /*try {
            outputStream.write(clickString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        Log.d("Click", String.valueOf(click));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        int position = intent.getExtras().getInt("position");

        mBluetoothAdapter.startDiscovery();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);

        BluetoothDevice device = devices[position];
        Toast.makeText(MyService.this, device.getName(), Toast.LENGTH_SHORT).show();
        ParcelUuid[] uuids = device.getUuids();
        UUID uuid = uuids[0].getUuid();
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (mTimer != null) // Cancel if already existed
            mTimer.cancel();
        else
            mTimer = new Timer();   //recreate new
        mTimer.scheduleAtFixedRate(new TimeDisplay(), 0, notify);   //Schedule task

        return Service.START_STICKY;
    }

    private float accelX = 0;
    private float accelY = 0;
    private float timestamp = 0;
    private float dT;
    private static final float NS2S = 1.0f / 1000000000.0f;

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST, mSensorHandler);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    private float[] referenciaCero = {0,0};
    private float[] sampleValues = {0,0};
    private int noMovementCount = 0;
    private int rollingSize = 4;
    private int calibrateTime = 1024;


    private LinkedList<Float> samplesX = new LinkedList<Float>();
    private LinkedList<Float> samplesY = new LinkedList<Float>();

    private float sumX = 0;
    private float sumY = 0;
    private float accX = 0;
    private float accY = 0;

    private float auxAccX = 0;
    private float auxAccY = 0;

    private float velX = 0;
    private float velY = 0;

    private int countZeroX = 0;
    private int countZeroY = 0;
    private int maxZero = 4;

    private String velXY = null;

    boolean calibrationOn = true;


    private void getSamples(SensorEvent event){
        dT = (event.timestamp - timestamp) * NS2S;
        timestamp = event.timestamp;
        sampleValues[0] = event.values[0]-referenciaCero[0];
        sampleValues[1] = event.values[1]-referenciaCero[1];
    }



    private void calibrateSum(SensorEvent event){
        if(noMovementCount < calibrateTime){
            referenciaCero[0]+= event.values[0];
            referenciaCero[1]+= event.values[1];
            noMovementCount++;
            Log.d("Cal", String.valueOf(noMovementCount));
        }
        else{
            referenciaCero[0]/=calibrateTime;
            referenciaCero[1]/=calibrateTime;
            calibrationOn = false;
        }
    }

    private void filtering(){
        auxAccX = accelX;
        auxAccY = accelY;
        if(samplesX.size() >= rollingSize){
            float firstSampleX = samplesX.removeFirst();
            float firstSampleY = samplesY.removeFirst();

            sumX-= firstSampleX;
            sumY-= firstSampleY;
        }
        samplesX.add(sampleValues[0]);
        samplesY.add(sampleValues[1]);

        sumX+= sampleValues[0];
        sumY+= sampleValues[1];

        accelX = sumX / samplesX.size();
        accelY = sumY / samplesY.size();
    }

    private void mechanicalFilter(){
        if(Math.abs(accelX) < 0.15){
            accelX = 0;
        }
        if(Math.abs(accelY) < 0.15){
            accelY = 0;
        }
    }

    private void endMovementCheck(){
        if(accelX == 0){
            countZeroX++;
        }else{
            countZeroX = 0;
        }

        if(accelY == 0){
            countZeroY++;
        }else{
            countZeroY = 0;
        }

        if(countZeroX > maxZero){
            velX = 0;
        }

        if(countZeroY == maxZero){
            velY = 0;
        }

    }

    private void positioning(){

        velX = velX + auxAccX + (accelX - auxAccX / 2);
        velY = velY + auxAccY + (accelY - auxAccY / 2);

    }

    public void onSensorChanged(SensorEvent event) {
        if(outputStream == null){
            return;
        }
        if(calibrationOn){
            calibrateSum(event);
            Log.d("Cal", String.valueOf(noMovementCount));
        }else{

            getSamples(event);

            filtering();

            mechanicalFilter();

            positioning();

            endMovementCheck();

            velXY = String.format("%d*%d",Math.round(velX/10),Math.round(-velY/10));

            Log.d("accX",String.valueOf(accelX));
            Log.d("accY",String.valueOf(accelY));

            Log.d("velX",String.valueOf(Math.round(velX)));
            Log.d("velY",String.valueOf(Math.round(velY)));

            Log.d("StringV",velXY);
        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();    //For Cancel Timer
        mSensorManager.unregisterListener(this);
        mSensorThread.quit();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Service is Destroyed", Toast.LENGTH_SHORT).show();
    }
    class SensorThread extends Thread {
        private SensorManager mSensorManager;
        private Sensor mSensor;

        @Override
        public void run() {
            super.run();
        }
    }

    //class TimeDisplay for handling task
    class TimeDisplay extends TimerTask {
        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (velXY != null) {
                        try {
                            outputStream.write(velXY.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        }

    }

}

