package com.example.giovanni.bttest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.giovanni.bttest.Libraries.BluAdapter;
import com.example.giovanni.bttest.Libraries.SerialProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * Created by userk on 07/04/15.
 */
public class Bluetooth {
    public static int TURNED_ON = 1;
    public static int TURNED_OFF = 0;
    public static int ASSOCIATED = 2;
    public static int NOT_ASSOCIATED = 3;

    private static boolean serialProtocol = true;

    private static final String TAG_NAME = "name";
    private static final String TAG_ID = "id";

    private static final int TENZO = 1;
    private static final int MATLAB = 3;
    private static final int ANDROID = 2;
    private static final int CON_CH = 31;

    private static byte[] header, footer, command, message;

    private static final int REQUEST_ENABLE_BT = 0;
    private static BluetoothAdapter mBluetoothAdapter;
    private static Activity activity;
    public static BluetoothDevice device;
    public static BluetoothSocket mmSocket;
    public static OutputStream mmOutputStream;
    public static InputStream mmInputStream;
    public static boolean associated = false;
    public static boolean watchdog = false;
    public String data = "";


    static Handler handler;
    static final byte delimiter = 10;

    static public byte[] readBuffer;
    static int readBufferPosition;
    static int counter;

    static public Thread workerThread;
    static public boolean stopWorker;

    public static ArrayList<HashMap<String, String>> devicesList;
    public static SerialProtocol serial;

    private static Context ctx;

    public Bluetooth(Context context, Activity acti) {
        super();
        ctx = context;
        this.activity = acti;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(ctx, "The device does not support Bluetooth. Attaccati al ca'..."
                    , Toast.LENGTH_LONG).show();
        }

        // If transmission protocol = robust serial initialize class
        if (serialProtocol)
        {
            serial = new SerialProtocol(ctx, acti);
        }
    }

    public int turnOn() {
        int result;
        Log.e("Settings Report", "Turning On Bluetooth");
        // Turn On Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(ctx, "Turned on" , Toast.LENGTH_LONG).show();
            result = 1;
        } else {
            Toast.makeText(ctx, "Already on",
                    Toast.LENGTH_LONG).show();
            result = 0;
        }
        return result;
    }

    public void turnOff()
    {
        Log.e("Bluetooth Class Report", "Turning Off Bluetooth");
        if (mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
            Toast.makeText(activity.getApplicationContext(), "Bluetooth turned off",
                    Toast.LENGTH_LONG).show();
            associated = false;
        }
    }

    public static int getVisibility()
    {
        Log.e("Bluetooth Class Report", "Requesting visibility");
        // Turn device visible - Bluetooth
        Intent getVisible = new Intent(mBluetoothAdapter.
                ACTION_REQUEST_DISCOVERABLE);
        getVisible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        activity.startActivityForResult(getVisible, 0);
        return 1;
    }

    public static ArrayList<HashMap<String, String>> getDevices(ListView list) {
        Log.e("Bluetooth Class Report", "Requesting bluetooth devices");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices == null || pairedDevices.size() == 0) {
            Toast.makeText(ctx, " No paired devices found! ", Toast.LENGTH_LONG)
                    .show();
        } else
        {
            devicesList = new ArrayList<HashMap<String, String>>();
            //ArrayList list = new ArrayList();
            if (pairedDevices.size() > 0)
            {
                HashMap<String, String> devices = new HashMap<String, String>();
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices)
                {
                    devices.put(TAG_ID, device.getAddress());
                    devices.put(TAG_NAME, device.getName());
                    devicesList.add(devices);
                    Log.e("Bluetooth Class Report", "Added: " + device.getName() + "with ID: " + device.getName());
                }
            }

            BluAdapter adapter = new BluAdapter(activity, devicesList);
            list.setAdapter(adapter);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String map = (String) parent.getItemAtPosition(position).toString();
                    Log.e("Bluetooth Class Report", "Clicked list item: " +
                            position + " Device's name: \n" + devicesList.get(position).get(TAG_NAME).toString());

                    try {
                        connect(devicesList.get(position).get(TAG_ID).toString());
                    } catch (IOException ex) {
                        Log.e("Bluetooth Class Report", " !!! Something went wrong - Device's name: \n" + devicesList.get(position).get(TAG_NAME).toString());
                    }

                    //String item = (String) parent.getItemAtPosition(position);
                    Log.e("Bluetooth Class Report", "Requesting connection to device: " + devicesList.get(position).get(TAG_NAME).toString());
                    Toast.makeText(ctx, "Connected: " + devicesList.get(position).get(TAG_NAME).toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
        return devicesList;
    }

    public static void connect(String uid) throws IOException
    {
        BluetoothSocket mmSocket = null;

        //UUID uuid = UUID.fromString(uid);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(uid);
        try {
            mmSocket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            // Qualcosa
            associated = false;
        }
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        // If it's not connected then connect
        if (!mmSocket.isConnected()) {
            try {
                mmSocket.connect();
                //associated = true;
                //out.append("\n...Connection established and data link opened...");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    //associated = false;
                } catch (IOException e2) {
                    Toast.makeText(activity.getApplicationContext(), "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".", Toast.LENGTH_LONG)
                            .show();
                    associated = false;
                }
            }
        }
        if (mmSocket.isConnected()) {
            try {
                mmInputStream = mmSocket.getInputStream();
                beginListenForData();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                mmOutputStream = mmSocket.getOutputStream();
                if (!serialProtocol) {
                    blueWrite("c");
                }
                else {
                    header = serial.createHeader(ANDROID, TENZO, 1);
                    command = serial.createCommand(CON_CH, (float) 1, 0, 0, 0);
                    footer = serial.createFooter();
                    message = serial.assembleMess(header, command, footer);
                    blueWrite(message);
                }
            } catch (IOException e)
            {
                Log.e("Settings report", "Output stream creation failed:" + e.getMessage() + ".");
            }
        }
    }

    public boolean isAssociated() {
        if (associated)
            return true;
        else
            return false;
    }

    public static boolean blueWrite(String s)
    {
        try
        {
            mmOutputStream.write(s.getBytes());
            return true;
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean blueWrite(byte[] s)
    {
        try
        {
            mmOutputStream.write(s);
            return true;
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public InputStream getInput(){
        return mmInputStream;
    }

    public static void beginListenForData()
    {
        final byte delimiter = 10;
        handler = new Handler();
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[50];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            char first = data.charAt(0);
                                            if (first=='K') {
                                                associated = true;
                                                // Send second part of the handshake
                                                blueWrite("K");
                                                Toast.makeText(activity.getApplicationContext(), "Associated!", Toast.LENGTH_LONG)
                                                        .show();
                                                //stopWorker = true;
                                            }
                                            else if (first=='w' && !watchdog) {
                                                watchdog = true;
                                                // Send second part of the handshake
                                                //blueWrite("K");
                                                //Toast.makeText(activity.getApplicationContext(), "Associated!", Toast.LENGTH_SHORT).show();
                                                //stopWorker = true;
                                            }
                                            else {
                                                Toast.makeText(activity.getApplicationContext(), data, Toast.LENGTH_SHORT                                                )
                                                        .show();
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }

                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    /*
    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }
    */
}
