package com.example.vdovin.tremolprint.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.vdovin.tremolprint.R;
import com.example.vdovin.tremolprint.protocol.tremol.ZFPException;
import com.example.vdovin.tremolprint.protocol.tremol.ZFPLib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {

    private static final UUID APP_UUID = UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DEVICE = 2;
    public static final String CONNECTED = "Connected";
    public static final String CREATE_RFCOMM_SOCKET = "createRfcommSocket";

    private BluetoothSocket mBtSocket;
    private BluetoothAdapter bluetoothAdapter;

    private ZFPLib tremolPrint;

    private Button print;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        print = (Button) findViewById(R.id.print_bt);
        print.setOnClickListener(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                selectDevice();
            } else {
                enableBluetooth();
            }
        } else {
            Toast.makeText(this, R.string.msg_bluetooth_is_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                if (resultCode == RESULT_OK) {
                    selectDevice();
                } else {
                    finish();
                }
                break;
            }
            case REQUEST_DEVICE: {
                if (resultCode == RESULT_OK) {
                    String address = data.getStringExtra(DeviceActivity.EXTRA_ADDRESS);
                    connect(address);
                } else {
                    finish();
                }
                break;
            }
        }
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void selectDevice() {
        Intent selectDevice = new Intent(this, DeviceActivity.class);
        startActivityForResult(selectDevice, REQUEST_DEVICE);
    }

    private void postToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void connect(final String address) {
        invokeHelper(new MethodInvoker() {
            @Override
            public void invoke() throws InvocationTargetException, IllegalAccessException {

                BluetoothSocket socket;

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(APP_UUID);
                    socket.connect();
                    final InputStream inputStream = socket.getInputStream();
                    final OutputStream outputStream = socket.getOutputStream();
                    tremolPrint = new ZFPLib(inputStream, outputStream);
                    postToast(CONNECTED);
                } catch (IOException e) {
                    try {
                        socket = device.createRfcommSocketToServiceRecord(APP_UUID);
                        Class<?> clazz = socket.getRemoteDevice().getClass();
                        Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};

                        Method m = clazz.getMethod(CREATE_RFCOMM_SOCKET, paramTypes);
                        Object[] params = new Object[]{Integer.valueOf(1)};

                        BluetoothSocket fallbackSocket = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
                        fallbackSocket.connect();

                        mBtSocket = fallbackSocket;

                        final InputStream inputStream = fallbackSocket.getInputStream();
                        final OutputStream outputStream = fallbackSocket.getOutputStream();
                        tremolPrint = new ZFPLib(inputStream, outputStream);
                        postToast(CONNECTED);

                    } catch (IOException | NoSuchMethodException e1) {
                        e1.printStackTrace();
                        disconnect();
                        selectDevice();
                    }
                }

            }
        });
    }

    public synchronized void disconnect() {
        if (tremolPrint != null) {
            tremolPrint = null;
        }

        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void invokeHelper(final MethodInvoker invoker) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(getString(R.string.msg_please_wait));
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return true;
            }
        });
        dialog.show();

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    invoker.invoke();
                } catch (ZFPException e) { // Fiscal printer error
                    e.printStackTrace();
                    postToast("FiscalPrinterException: " + e.getMessage());
                } catch (Exception e) { // Critical exception
                    e.printStackTrace();
                    postToast("Exception: " + e.getMessage());
                    disconnect();
                    selectDevice();
                } finally {
                    dialog.dismiss();
                }
            }
        });
        t.start();
    }

    @Override
    public void onClick(View v) {

        try {
            tremolPrint.openFiscalBon(1, "0", false, false);
            tremolPrint.sellFree("Test article", '1', 2.34f, 1.0f, 0.0f);
            tremolPrint.sellFree("Test article2", '1', 1.0f, 3.54f, 0.0f);
            float sum = tremolPrint.calcIntermediateSum(false, false, false, 0.0f, '0');
            tremolPrint.payment(sum, 0, false);
            tremolPrint.closeFiscalBon();
        } catch (ZFPException e) {
            e.printStackTrace();
        }


    }

    private interface MethodInvoker {
        void invoke() throws Exception;
    }
}
