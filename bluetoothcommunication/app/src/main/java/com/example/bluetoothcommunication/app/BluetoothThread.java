package com.example.bluetoothcommunication.app;

import android.bluetooth.BluetoothSocket;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by matthijs on 12-6-14.
 */
public class BluetoothThread extends Thread {

    //private Listener listener;

    /**
     * Listsner interface
     */
    public static interface Listener {
        void onConnected();
        void onReceived(byte[] buffer, int length);
        void onDisconnected();
        void onError(IOException e);
        void onMessage(String message);
    }

    private static final int BUFFER_SIZE = 1024;
    private BluetoothSocket socket;
    private Listener listener;

    /**
     * Create new instance
     */
    public static BluetoothThread newInstance(BluetoothSocket socket, Listener listener) {
        BluetoothThread instance = new BluetoothThread(socket, listener);
        instance.start();
        //instance.run();
        return instance;
    }

    protected BluetoothThread(BluetoothSocket socket, Listener listener) {
        this.socket = socket;
        this.listener = listener;
    }

    public void run() {
        try {
            // Connect the device through the socket.
            // This will block until it succeeds or throws an exception
            isClosing = false;
            socket.connect();
        } catch (IOException connectException) {
            try {
                cancel();
            } catch (IOException e) { }
            listener.onError(connectException);
            return;
        }

        manageConnectedSocket();
    }

    private InputStream inputStream;
    private OutputStream outputStream;

    private boolean isClosing;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int curLength;
    private int bytes;

    private void manageConnectedSocket() {
        try {
            inputStream = socket.getInputStream();
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            listener.onConnected();

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                bytes = inputStream.read(buffer, curLength, buffer.length - curLength);
                if (bytes > 0) {
                    // still reading
                    curLength += bytes;
                }

                // check if reading is done
                if (curLength > 0) {
        listener.onDisconnected();
        // reading finished
        listener.onReceived(buffer, curLength);
        curLength = bytes = 0;
    }
}
} catch (IOException e) {
        if (isClosing)
        return;

        listener.onError(e);
        throw new RuntimeException(e);
        }
        }

public void read(byte[] bytes) throws IOException {
    DataInputStream dis2 = new DataInputStream(socket.getInputStream());
    InputStreamReader disR2 = new InputStreamReader(dis2);
    BufferedReader br = new BufferedReader(disR2);//create a BufferReader object for input
    //toastShort(br.toString());

}

    /* Call this from the main Activity to send data to the remote device */
public void write(byte[] bytes) throws IOException {
        if (outputStream == null)
        throw new IllegalStateException("Wait connection to be opened");

        outputStream.write(bytes);
        outputStream.flush();
        }


/** Will cancel an in-progress connection, and close the socket */
public void cancel() throws IOException {
        isClosing = true;
        socket.close();

        inputStream = null;
        outputStream = null;
    }

}
