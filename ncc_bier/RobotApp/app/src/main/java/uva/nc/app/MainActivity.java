/***
 * Heavily modified code from the provided framework (UvA Netcentric 2013/2014 course)
 */

package uva.nc.app;

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
import java.lang.Math;
import java.nio.charset.Charset;
import java.util.UUID;

import uva.nc.ServiceActivity;
import uva.nc.bluetooth.BluetoothService;
import uva.nc.mbed.MbedManager;
import uva.nc.mbed.MbedRequest;
import uva.nc.mbed.MbedResponse;
import uva.nc.mbed.MbedService;

import android.widget.FrameLayout;
import android.view.View.OnClickListener;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;

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
    private Button connectButton;
    private Button disconnectButton;

    // mBed Controls.
    private TextView mbedConnectedText;

    // Layout Controls
    private Button showCameraLayout;
    private Button showMbedLayout;
    private Button showDebugLayout;
    private Button showManualLayout;

    // Layout Frames
    private LinearLayout cameraLayout;
    private LinearLayout mbedLayout;
    private LinearLayout debugLayout;
    private LinearLayout manualLayout;

    // Manual Drive Controls
    private Button driveForward;
    private Button driveBackward;
    private Button driveLeft;
    private Button driveRight;

    // Logging TextView
    private TextView logComm;

    // Accessory to connect to when service is connected.
    private UsbAccessory toConnect;

    // Camera Variables
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    // QR scanner Variables
    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;
    private boolean connected = false;

    // Load QR scanner library
    static {
        System.loadLibrary("iconv");
    }

    // QR scanner Controls
    TextView scanText;
    Button scanButton;

    // Initialize variables for bluetooth connection

    /****
     * Device address Floris:   44:6D:57:96:64:F7
     * Device address Matthijs: 00:15:83:15:A3:10
     * Device address Mike:     44:6D:57:4A:81:D4
     * Device address Patrick:  A4:17:31:ED:18:F8
     * Device address Xander:   00:09:DD:50:8D:2A
     */
    private String deviceAddress = "00:15:83:15:A3:10";
    private UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private BluetoothAdapter BA;
    private BluetoothThread connection;

    BluetoothDevice device;
    BluetoothSocket socket;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        attachControls();

        //Bluetooth initialize
        device = BluetoothAdapter.getDefaultAdapter().
                getRemoteDevice(deviceAddress);

        // If this intent was started with an accessory, store it temporarily and clear once connected.
        UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            this.toConnect = accessory;
        }

        /***
         * Code for QR scanner
         * Source: ZBarAndroidSDK-0.2 example
         *         http://sourceforge.net/projects/zbar/
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
        connectButton = (Button)findViewById(R.id.listener);
        disconnectButton = (Button)findViewById(R.id.disconnect);

        // Mbed controls
        mbedConnectedText = (TextView)findViewById(R.id.mbed_connected);

        // Logging TextView
        logComm = (TextView)findViewById(R.id.log_comm);

        // Layout controls
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
                    manualLayout.setVisibility(View.GONE);
                    showManualLayout.setText("Show Manual Controls");
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
                    manualLayout.setVisibility(View.GONE);
                    showManualLayout.setText("Show Manual Controls");
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
                    manualLayout.setVisibility(View.GONE);
                    showManualLayout.setText("Show Manual Controls");
                }
            }
        });
        manualLayout = (LinearLayout)findViewById(R.id.manual_layout);
        showManualLayout = (Button)findViewById(R.id.show_manual_layout);
        showManualLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(manualLayout.getVisibility() == View.VISIBLE) {
                    manualLayout.setVisibility(View.GONE);
                    showManualLayout.setText("Show Manual Controls");
                } else {
                    manualLayout.setVisibility(View.VISIBLE);
                    showManualLayout.setText("Hide Manual Controls");
                    cameraLayout.setVisibility(View.GONE);
                    showCameraLayout.setText("Show Camera Preview");
                    mbedLayout.setVisibility(View.GONE);
                    showMbedLayout.setText("Show Connections");
                    debugLayout.setVisibility(View.GONE);
                    showDebugLayout.setText("Show Monitoring");
                }
            }
        });

        // QRscanner controls
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

        // Manual robot controls
        driveForward = (Button)findViewById(R.id.forward);
        driveForward.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 1.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("* Sent Drive forward message to mbed\n");
            }
        });
        driveBackward = (Button)findViewById(R.id.backward);
        driveBackward.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 2.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("* Sent Drive backward message to mbed\n");
            }
        });
        driveLeft = (Button)findViewById(R.id.left);
        driveLeft.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 3.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("* Sent Drive left message to mbed\n");
            }
        });
        driveRight = (Button)findViewById(R.id.right);
        driveRight.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                float[] args = new float[1];
                args[0] = 4.0f;

                getMbed().manager.write(new MbedRequest(COMMAND_DRIVE, args));

                logComm.append("* Sent Drive right message to mbed\n");
            }
        });
    }


    // Controls to connect and disconnect to Bluetooth server
    private void refreshBluetoothControls() {
        String slaveStatus = "Status not available";
        String ownAddress = "Not available";
        boolean slaveButtonEnabled = false;

        // Well it's not pretty, but it (barely) avoids duplicate logic.
        final BluetoothService bluetooth = getBluetooth();
        if (bluetooth != null) {
            slaveButtonEnabled = true;
            slaveStatus = "Master connecting";
            ownAddress = bluetooth.utility.getOwnAddress();

            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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

            disconnectButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        connection.cancel();
                        Log.i("Masterserver", "Disconnected");
                        connected = false;
                    } catch(IOException e) {
                        Log.e("Masterserver", "Error" + e);
                    }
                }
            });
        }

        listenerStatusText.setText(slaveStatus);
        connectButton.setEnabled(slaveButtonEnabled);
        disconnectButton.setEnabled(slaveButtonEnabled);
        ownAddressText.setText(ownAddress);
    }

    private void refreshMbedControls() {
        String connText = getString(R.string.not_connected); // if you want to localize

        MbedService mbed = getMbed();
        if (mbed != null && mbed.manager.areChannelsOpen()) {
            connText = getString(R.string.connected);
        }

        mbedConnectedText.setText(connText);
    }

    // Function receives message from Bluetooth server
    private BluetoothThread.Listener bluetoothListener = new BluetoothThread.Listener() {
        String currentLocation = "[0, 0]";
        String finalDestination = "[0, 0]";
        String direction = "None";
        String confirmation = "False";
        boolean roaming = true;

        public void onConnected() {
            Log.i("Masterserver", "Connected bluetooth 12");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Log.i("Masterserver", "Connected bluetooth");
                    connected = true;
                    //MainActivity.this.onConnected(true);
                }
            });
        }

        public void onDisconnected() {
            Log.i("Masterserver", "Disconnected bluetooth 12");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Log.i("Masterserver", "Disconnected bluetooth");
                    //MainActivity.this.onConnected(false);
                }
            });
        }

        public void onError(IOException e) {
        }

        public void onReceived(byte[] buffer, int length) {
            Log.i("Masterserver", "received message with length" + length);

            final String Input = new String(buffer, 0, length);

            Log.i("Masterserver", "received message = " + Input);

            // Hier wordt de finalDestination ontvangen.
            if (Input.contains("finalDestination: ")) {
                finalDestination = Input.replace("finalDestination: ", "");
                roaming = false;

                logComm.append("* Received Final Destination\n");
            // Hier word de current location ontvangen
            } else if (Input.contains("currentLocation: ")) {
                currentLocation = Input.replace("currentLocation: ", "");

                Log.i("Masterserver", "currenLocation: " + currentLocation);

                //scanText.setText("Current Location: " + currentLocation);

                //TODO: niet meer locatie/ richting gebruiken die al afgekeurd is
                if (roaming) {
                    direction = RandomDirection();
                    Log.i("Masterserver", "RandomDirection");
                } else {
                    direction = KortstePadSolver(currentLocation, finalDestination);
                    Log.i("Masterserver", "KorstePadSolver");
                }

                SendMessage("direction: " + direction);
            // Hier wordt de confirmation ontvangen.
            } else if (Input.contains("confirmation: ")) {
                confirmation = Input.replace("confirmation: ", "");
                logComm.append("* Direction is free\n");

                if (confirmation.equals("True")) {
                    float[] args = new float[1];

                    logComm.append("* Driving direction: " + direction + "\n");

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
                }
            }

            runOnUiThread(new Runnable() {
                public void run() {
                }
            });
        }

        public void onMessage(String message) {
            Log.i("Masterserver", "Message: " + message);
        }

        // Function for choosing a random direction to drive to
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

        // Function for finding the shortest path to the final destination of the omnibot
        public String KortstePadSolver(String currentLocation, String finalDestination) {
            /* currentLocation -> "[x, y]" */
            /* finalDestination -> "[x, y]" */
            int currentX = Integer.valueOf(String.valueOf(currentLocation.charAt(1)));
            int currentY = Integer.valueOf(String.valueOf(currentLocation.charAt(4)));
            int finalX = Integer.valueOf(String.valueOf(finalDestination.charAt(1)));
            int finalY = Integer.valueOf(String.valueOf(finalDestination.charAt(4)));

            if (Math.abs(finalX - currentX) == 0 && Math.abs(finalY - currentY) == 0) {
                return "Done";
            }

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

        // Sends message to Bluetooth server
        public void SendMessage(String Message) {
            byte[] b = Message.getBytes(Charset.forName("UTF-8"));

            try {
                connection.write(b);
                logComm.append("* Sent " + Message + " to master\n");
            } catch (IOException e) {
                Log.e("Masterserver", "Error with write");
            }
        }
    };


    /***
     * Functions for QR scanner
     * Source: ZBarAndroidSDK-0.2 example
     *         http://sourceforge.net/projects/zbar/
     *
     * Copied:
     *   getCamerainstance()
     *   releaseCamera()
     *   doAutoFocus()
     *   autoFocusCB()
     *
     * Modified:
     *   previewCb()
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

    // QR scanner automatically sends message to Bluetooth server after scanning a QR code
    // and pauses the scanner
    PreviewCallback previewCb = new PreviewCallback() {
        public void SendMessage(String Message) {
            byte[] b = Message.getBytes(Charset.forName("UTF-8"));

            try {
                Log.i("message", Message);
                connection.write(b);
                logComm.append("* Sent " + Message + " to master\n");
            } catch (IOException e) {
                Log.e("Masterserver", "Error with write");
            }
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0 && connected) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    String QRdata = sym.getData();
                    Log.i("message", QRdata);
                    scanText.setText("QR-Code: " + QRdata);
                    SendMessage("QRdata: " + QRdata);
                    barcodeScanned = true;
                }
            } else if(result != 0 && !connected) {
                Log.i("Bluetooth", "Scanned while not connected!");
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
        // Refresh Mbed controls on these events.
        private final String MBED_REFRESH_ON[] = {      MbedManager.DEVICE_ATTACHED,
                                                        MbedManager.DEVICE_DETACHED };


        // Returns intents this receiver responds to.
        protected IntentFilter getIntentFilter() {
            IntentFilter filter = new IntentFilter();

            // Notification updates.
            for (String action : MBED_REFRESH_ON) {
                filter.addAction(action);
            }

            // Data received events.
            filter.addAction(MbedManager.DATA_READ);

            return filter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Refresh on most mBed events.
            for (String update : MBED_REFRESH_ON) {
                if (action.equals(update)) {
                    refreshMbedControls();
                    break;
                }
            }

            // Process received data from Mbed
            if (action.equals(MbedManager.DATA_READ)) {

                // mBed data received.
                MbedResponse response = intent.getParcelableExtra(MbedManager.EXTRA_DATA);
                if (response != null) {
                    // Errors handled as separate case, but this is just sample code.
                    if (response.hasError()) {
                        toastLong("Error! " + response);
                        return;
                    }

                    float[] values = response.getValues();

                    if (response.getCommandId() == COMMAND_DRIVE) {
                        if (values == null || values.length != 1) {
                            toastShort("Error!");
                        } else {
                            // Resumes QR scanner when omnibot finished driving
                            logComm.append("* Finished driving, activate QR scanner\n");

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
                        }
                    }
                }
            }
        }
    }
}
