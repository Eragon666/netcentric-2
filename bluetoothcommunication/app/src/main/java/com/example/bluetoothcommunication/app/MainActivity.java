package com.example.bluetoothcommunication.app;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {

    //private String deviceAddress = "00:09:DD:50:8D:2A";
    private String deviceAddress = "00:15:83:15:A3:10";
    private UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");

    private Button On, Off, Visible, list;
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    private ListView lv;
    private BluetoothThread connection;

    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().
            getRemoteDevice(deviceAddress);

    BluetoothSocket socket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        On = (Button)findViewById(R.id.button1);
        Off = (Button)findViewById(R.id.button2);
        Visible = (Button)findViewById(R.id.button3);
        list = (Button)findViewById(R.id.button4);

        lv = (ListView)findViewById(R.id.listView1);

        BA = BluetoothAdapter.getDefaultAdapter();

        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().
                getRemoteDevice(deviceAddress);

        try {
            Log.i("Masterserver", "Connected");
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);

            //socket.connect();

        } catch (IOException e) {
            Log.e("Masterserver", "Error" + e);
        }

        connection = BluetoothThread.newInstance(socket, bluetoothListener);


    }

    public void on(View view){
        String string = "Daan is lui";

        byte[] b = string.getBytes();
        b = string.getBytes(Charset.forName("UTF-8"));

        try {
            connection.write(b);
        } catch (IOException e) {
            Log.e("Masterserver", "Error with write");
        }
    }
    public void list(View view){
        pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();
        for(BluetoothDevice bt : pairedDevices)
            list.add(bt.getName());

        Toast.makeText(getApplicationContext(),"Showing Paired Devices",
                Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter
                (this,android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);

    }
    public void off(View view){
        BA.disable();
        Toast.makeText(getApplicationContext(),"Turned off" ,
                Toast.LENGTH_LONG).show();
    }
    public void visible(View view){
        Intent getVisible = new Intent(BluetoothAdapter.
                ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private BluetoothThread.Listener bluetoothListener = new BluetoothThread.Listener() {
        public void onConnected() {
            Log.i("Masterserver", "Connected bluetooth 12");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Log.i("Masterserver", "Connected bluetooth");
                    //MainActivity.this.onConnected(true);
                }
            });
        }

        public void onDisconnected() {
            Log.i("Masterserver", "disConnected bluetooth");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    //MainActivity.this.onConnected(false);
                }
            });
        }

        public void onError(IOException e) {
        }

        public void onReceived(byte[] buffer, int length) {
            Log.i("Masterserver", "received message with length" + length);
            // copy to string
            final String stringData = new String(buffer, 0, length);
            Log.i("Masterserver", "received message = " + stringData);
            runOnUiThread(new Runnable() {
                public void run() {
                }
            });
        }

        public void onMessage(String message) {
            Log.i("Masterserver", "Message: " + message);
        }
    };


}
