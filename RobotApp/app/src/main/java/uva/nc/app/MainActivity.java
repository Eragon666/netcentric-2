package uva.nc.app;

import uva.nc.app.CameraPreview;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.ActivityInfo;

import java.io.IOException;
import java.io.Serializable;
import java.lang.Math;
import java.util.UUID;

import uva.nc.ServiceActivity;
import uva.nc.bluetooth.BluetoothService;
import uva.nc.bluetooth.MasterManager;
import uva.nc.bluetooth.SlaveManager;
import uva.nc.mbed.MbedManager;
import uva.nc.mbed.MbedRequest;
import uva.nc.mbed.MbedResponse;
import uva.nc.mbed.MbedService;

import android.widget.FrameLayout;
import android.view.View.OnClickListener;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;


public class MainActivity extends ServiceActivity {
    private static final String TAG = MainActivity.class.getName();

    // Receiver implemented in separate class, see bottom of file.
    private final MainActivityReceiver receiver = new MainActivityReceiver();

    // ID's for commands on mBed.
    private static final int COMMAND_DRIVE = 1;

    // BT Controls.
    private TextView listenerStatusText;
    private TextView ownAddressText;
    private Button listenerButton;

    private TextView deviceCountText;
    private Button devicesButton;

    // mBed controls.
    private TextView mbedConnectedText;

    // Layout Controls
    private Button showCameraLayout;
    private Button showMbedLayout;
    private Button showDebugLayout;

    // Layout variables to identify the frames
    private LinearLayout cameraLayout;
    private LinearLayout mbedLayout;
    private LinearLayout debugLayout;

    // Declare monitoring log variable
    private TextView logComm;

    // Accessory to connect to when service is connected.
    private UsbAccessory toConnect;

    //Declare variables for the Camera and QR scanner
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    // TODO These buttons can be removed in the final version
    TextView scanText;
    Button scanButton;

    ImageScanner scanner;

    // Initialize variables for the QR scanner
    private boolean barcodeScanned = false;
    private boolean previewing = true;

    // Initialize variables for bluetooth connection
    private String deviceAddress = "00:15:83:15:A3:10";
    private UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");

    private BluetoothAdapter BA;
    private BluetoothThread connection;

    BluetoothDevice device;

    BluetoothSocket socket;

    // Load QR scanner library
    static {
        System.loadLibrary("iconv");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        attachControls();

        // Attach logging TextView
        logComm = (TextView)findViewById(R.id.log_comm);

        //Bluetooth initialize
        device = BluetoothAdapter.getDefaultAdapter().
                getRemoteDevice(deviceAddress);


        /***
         * Attach layout buttons
         **/
        cameraLayout = (LinearLayout)findViewById(R.id.camera_layout);
        showCameraLayout = (Button)findViewById(R.id.show_camera_layout);
        showCameraLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(cameraLayout.getVisibility() == View.VISIBLE) {
                    cameraLayout.setVisibility(View.GONE);
                    showCameraLayout.setText("Show Camera Preview");
                } else {
                    cameraLayout.setVisibility(View.VISIBLE);
                    showCameraLayout.setText("Hide Camera Preview");
                    mbedLayout.setVisibility(View.GONE);
                    showMbedLayout.setText("Show Connections");
                    debugLayout.setVisibility(View.GONE);
                    showDebugLayout.setText("Show Monitoring");
                }
            }
        });
        mbedLayout = (LinearLayout)findViewById(R.id.mbed_layout);
        showMbedLayout = (Button)findViewById(R.id.show_mbed_layout);
        showMbedLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(mbedLayout.getVisibility() == View.VISIBLE) {
                    mbedLayout.setVisibility(View.GONE);
                    showMbedLayout.setText("Show Connections");
                } else {
                    mbedLayout.setVisibility(View.VISIBLE);
                    showMbedLayout.setText("Hide Connections");
                    cameraLayout.setVisibility(View.GONE);
                    showCameraLayout.setText("Show Camera Preview");
                    debugLayout.setVisibility(View.GONE);
                    showDebugLayout.setText("Show Monitoring");
                }
            }
        });
        debugLayout = (LinearLayout)findViewById(R.id.debug_layout);
        showDebugLayout = (Button)findViewById(R.id.show_debug_layout);
        showDebugLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(debugLayout.getVisibility() == View.VISIBLE) {
                    debugLayout.setVisibility(View.GONE);
                    showDebugLayout.setText("Show Monitoring");
                } else {
                    debugLayout.setVisibility(View.VISIBLE);
                    showDebugLayout.setText("Hide Monitoring");
                    cameraLayout.setVisibility(View.GONE);
                    showCameraLayout.setText("Show Camera Preview");
                    mbedLayout.setVisibility(View.GONE);
                    showMbedLayout.setText("Show Connections");
                }
            }
        });

        // TODO temporary random direction button, will be automated
        Button tempButton = (Button)findViewById(R.id.temp);
        tempButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 1.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("Sent Drive forward message to mbed\n");
            }
        });
        Button temp2Button = (Button)findViewById(R.id.temp2);
        temp2Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 2.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("Sent Drive backward message to mbed\n");
            }
        });
        Button temp3Button = (Button)findViewById(R.id.temp3);
        temp3Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 3.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("Sent Drive left message to mbed\n");
            }
        });
        Button temp4Button = (Button)findViewById(R.id.temp4);
        temp4Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 4.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("Sent Drive right message to mbed\n");
            }
        });

        // If this intent was started with an accessory, store it temporarily and clear once connected.
        UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            this.toConnect = accessory;
        }

        /***
         * Code for QR scanner
         * Source: ZBarAndroidSDK-0.2 example
         **/
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        // Add camera preview to layout
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        // TODO To be removed in the final version
        scanText = (TextView)findViewById(R.id.scanText);
        scanButton = (Button)findViewById(R.id.ScanButton);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (barcodeScanned) {
                    barcodeScanned = false;
                    scanText.setText("Scanning...");
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                    mCamera.autoFocus(autoFocusCB);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, receiver.getIntentFilter());
        refreshBluetoothControls();
        refreshMbedControls();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        unregisterReceiver(receiver);
    }


    @Override
    protected void onBluetoothReady(BluetoothService bluetooth) {
        refreshBluetoothControls();
    }

    @Override
    protected void onMbedReady(MbedService mbed) {
        if (toConnect != null) {
            mbed.manager.attach(toConnect);
            toConnect = null;
        }
        refreshMbedControls();
    }


    private void attachControls() {
        // Bluetooth controls.
        ownAddressText = (TextView)findViewById(R.id.own_address);
        listenerStatusText = (TextView)findViewById(R.id.listener_status);
        listenerButton = (Button)findViewById(R.id.listener);
        deviceCountText = (TextView)findViewById(R.id.device_count);
        devicesButton = (Button)findViewById(R.id.devices);
        devicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent launch = new Intent(MainActivity.this, DevicesActivity.class);
                startActivity(launch);
            }
        });


        mbedConnectedText = (TextView)findViewById(R.id.mbed_connected);

        // mBed controls.
        // TODO Attach control to mbed buttons?

    }

    private void refreshBluetoothControls() {
        String slaveStatus = "Status not available";
        String slaveButton = "Start listening";
        String ownAddress = "Not available";
        String connected = "0";
        boolean slaveButtonEnabled = false;
        boolean devicesButtonEnabled = false;

        // Well it's not pretty, but it (barely) avoids duplicate logic.
        final BluetoothService bluetooth = getBluetooth();
        if (bluetooth != null) {
            slaveButtonEnabled = true;
            devicesButtonEnabled = true;
            ownAddress = bluetooth.utility.getOwnAddress();

            int devConnected = bluetooth.master.countConnected();
            if (bluetooth.master.countConnected() > 0) {
                connected = String.valueOf(devConnected);
            }

            if (bluetooth.slave.isConnected()) {
                slaveStatus = "Connected to " + bluetooth.slave.getRemoteDevice();
                slaveButton = "Disconnect";
                listenerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bluetooth.slave.disconnect();
                    }
                });
            } else if (bluetooth.slave.isListening()) {
                slaveStatus = "Waiting for connection";
                slaveButton = "Stop listening";
                listenerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bluetooth.slave.stopAcceptOne();
                    }
                });
            } else {
                slaveStatus = "Master connecting";
                slaveButton = "Connect to master";
                listenerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        if (!bluetooth.utility.isDiscoverable()) {
//                            bluetooth.utility.setDiscoverable();
//                        }

                        try {
                            Log.i("Masterserver", "Connected");
                            socket = device.createRfcommSocketToServiceRecord(MY_UUID);

                            //socket.connect();
                        } catch (IOException e) {
                            Log.e("Masterserver", "Error" + e);
                        }

                        connection = BluetoothThread.newInstance(socket, bluetoothListener);
                    }
                });
            }
        }

        listenerStatusText.setText(slaveStatus);
        listenerButton.setText(slaveButton);
        listenerButton.setEnabled(slaveButtonEnabled);
        ownAddressText.setText(ownAddress);
        deviceCountText.setText(connected);
        devicesButton.setEnabled(devicesButtonEnabled);
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


    private void refreshMbedControls() {
        String connText = getString(R.string.not_connected); // if you want to localize
        boolean enableButtons = false;

        MbedService mbed = getMbed();
        if (mbed != null && mbed.manager.areChannelsOpen()) {
            connText = getString(R.string.connected);
            enableButtons = true;
        }

        mbedConnectedText.setText(connText);
        // TODO enable disabled buttons?
    }


    /***
     * Functions for QR scanner
     * Source: ZBarAndroidSDK-0.2 example
     **/
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            /* Hier wordt de QR-code gescand, maar dit moet dus verplaatst
               worden naar de plek waar MBED-signalen worden ontvangen. */
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    String QRdata = sym.getData();
                    scanText.setText("QR-Code: " + QRdata);
                    final BluetoothService bluetooth = getBluetooth();
                    if(bluetooth != null) {
                        if (bluetooth.slave.isConnected()) {
                            logComm.append("To Master: " + QRdata + "\n");
                            bluetooth.slave.sendToMaster("QRdata: " + QRdata);
                        }
                    }
                    logComm.append("Scanned:"+ QRdata + "\n");
                    barcodeScanned = true;
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };


    // Broadcast receiver which handles incoming events. If it were smaller, inline it.
    private class MainActivityReceiver extends BroadcastReceiver {
        String finalDestination = "[0, 0]";
        String currentLocation = "[0, 0]";
        String direction = "None";
        String confirmation = "False";
        boolean roaming = true;

        // Refresh BT controls on these events.
        private final String BLUETOOTH_REFRESH_ON[] = { MasterManager.DEVICE_ADDED,
                                                        MasterManager.DEVICE_REMOVED,
                                                        MasterManager.DEVICE_STATE_CHANGED,
                                                        SlaveManager.LISTENER_CONNECTED,
                                                        SlaveManager.LISTENER_DISCONNECTED,
                                                        SlaveManager.STARTED_LISTENING,
                                                        SlaveManager.STOPPED_LISTENING };

        private final String MBED_REFRESH_ON[] = {      MbedManager.DEVICE_ATTACHED,
                                                        MbedManager.DEVICE_DETACHED };


        // Returns intents this receiver responds to.
        protected IntentFilter getIntentFilter() {
            IntentFilter filter = new IntentFilter();

            // Notification updates.
            for (String action : BLUETOOTH_REFRESH_ON) {
                filter.addAction(action);
            }
            for (String action : MBED_REFRESH_ON) {
                filter.addAction(action);
            }

            // Data received events.
            filter.addAction(MbedManager.DATA_READ);
            filter.addAction(MasterManager.DEVICE_RECEIVED);
            filter.addAction(SlaveManager.LISTENER_RECEIVED);

            return filter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Refresh on most Bluetooth or mBed events.
            for (String update : BLUETOOTH_REFRESH_ON) {
                if (action.equals(update)) {
                    refreshBluetoothControls();
                    break;
                }
            }
            for (String update : MBED_REFRESH_ON) {
                if (action.equals(update)) {
                    refreshMbedControls();
                    break;
                }
            }

            // Process received data.
            if (action.equals(SlaveManager.LISTENER_RECEIVED)) {

                // Slave received data from master.
                Serializable obj = intent.getSerializableExtra(SlaveManager.EXTRA_OBJECT);
                if (obj != null) {
                    String Input = String.valueOf(obj);

                    if (Input.contains("finalDestination: ")) {
                        /* Hier wordt de finalDestination ontvangen. */
                        finalDestination = Input.replace("finalDestination: ", "");
                        roaming = false;
                    } else if (Input.contains("currentLocation: ")) {
                        /* Hier wordt de currentLocation ontvangen. */
                        currentLocation = Input.replace("currentLocation: ", "");

                        scanText.setText("Current Location: " + currentLocation);
                        if (roaming)
                            direction = RandomDirection();
                        else
                            direction = KortstePadSolver(currentLocation, finalDestination);
                        final BluetoothService bluetooth = getBluetooth();
                        if (bluetooth != null) {
                            if (bluetooth.slave.isConnected()) {
                                toastShort("To Master:\n" + direction);
                                /* bluetooth.slave.sendToMaster(currentLocation, direction); */
                                bluetooth.slave.sendToMaster("direction: " + direction);
                            }
                        }
                    } else if (Input.contains("confirmation: ")) {
                        /* Hier wordt de confirmation ontvangen. */
                        confirmation = Input.replace("confirmation: ", "");
                        toastShort("confirmation: " + confirmation);
                        if (confirmation.equals("True")) {
                            // TODO Send 'direction' to MBED through USB
					        float[] args = new float[1];

                            logComm.append("Direction: " + direction + "\n");

                            if(direction.equals("North")) {
                                args[0] = 1.0f;
                            } else if(direction.equals("South")) {
                                args[0] = 2.0f;
                            } else if(direction.equals("West")) {
                                args[0] = 3.0f;
                            } else if(direction.equals("East")) {
                                args[0] = 4.0f;
                            }

                            getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));
                            //currentLocation = UpdateLocation(currentLocation, direction);
                        }
                    }
                } else {
                    toastShort("From master:\nnull");
                }
            } else if (action.equals(MasterManager.DEVICE_RECEIVED)) {
                // Master received data from slave.
                Serializable obj = intent.getSerializableExtra(MasterManager.EXTRA_OBJECT);
                BluetoothDevice device = intent.getParcelableExtra(MasterManager.EXTRA_DEVICE);
                if (obj != null) {
                    toastShort("From " + device + "\n" + String.valueOf(obj));
                } else {
                    toastShort("From " + device + "\nnull!");
                }
            } else if (action.equals(MbedManager.DATA_READ)) {

                // mBed data received.
                MbedResponse response = intent.getParcelableExtra(MbedManager.EXTRA_DATA);
                if (response != null) {
                    // Errors handled as separate case, but this is just sample code.
                    if (response.hasError()) {
                        toastLong("Error! " + response);
                        return;
                    }

                    float[] values = response.getValues();
                    // TODO do something with received Mbed values
                    if (response.getCommandId() == COMMAND_DRIVE) {
                        if (values == null || values.length != 1) {
                            toastShort("Error!");
                        } else {
                            // TODO if ok to scan (need to use values[])
                            logComm.append("Received ok to scan message from mbed\n");
                            if((int)values[0] == 0) {
                                if (barcodeScanned) {
                                    barcodeScanned = false;
                                    scanText.setText("Scanning...");
                                    mCamera.setPreviewCallback(previewCb);
                                    mCamera.startPreview();
                                    previewing = true;
                                    mCamera.autoFocus(autoFocusCB);
                                }
                            }
                            logComm.append("Scanning...\n");
                        }
                    }
                }
            }
        }

        public String RandomDirection() {
            int direction = 1 + (int)(Math.random() * ((4 - 1) + 1));
            if (direction == 1)
                return "North";
            else if (direction == 2)
                return "East";
            else if (direction == 3)
                return "South";
            else if (direction == 4)
                return "West";
            return "None";
        }

        public String KortstePadSolver(String currentLocation, String finalDestination) {
            /* currentLocation -> "[x, y]" */
            /* finalDestination -> "[x, y]" */
            int currentX = Integer.valueOf(String.valueOf(currentLocation.charAt(1)));
            int currentY = Integer.valueOf(String.valueOf(currentLocation.charAt(4)));
            int finalX = Integer.valueOf(String.valueOf(finalDestination.charAt(1)));
            int finalY = Integer.valueOf(String.valueOf(finalDestination.charAt(4)));

            if (Math.abs(finalX - currentX) > Math.abs(finalY - currentY)) {
                if (finalX > currentX)
                    return "East";
                else
                    return "West";
            } else {
                if (finalY > currentY)
                    return "North";
                else
                    return "South";
            }
        }

        public String UpdateLocation(String currentLocation, String direction) {
            /* currentLocation -> "[x, y]" */
            int currentX = Integer.valueOf(String.valueOf(currentLocation.charAt(1)));
            int currentY = Integer.valueOf(String.valueOf(currentLocation.charAt(4)));
            if (direction == "North")
                return "[" + currentX + ", " + (currentY + 1) + "]";
            else if (direction == "East")
                return "[" + (currentX + 1) + ", " + currentY + "]";
            else if (direction == "South")
                return "[" + currentX + ", " + (currentY - 1) + "]";
            else if (direction == "West")
                return "[" + (currentX - 1) + ", " + currentY + "]";
            else
                return "[" + currentX + ", " + currentY + "]";
        }
    }
}
