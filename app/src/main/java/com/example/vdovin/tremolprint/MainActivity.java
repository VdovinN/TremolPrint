package com.example.vdovin.tremolprint;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.vdovin.tremolprint.bluetooth.BluetoothActivity;
import com.example.vdovin.tremolprint.usb.UsbActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button btButton;
    Button usbButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btButton = (Button) findViewById(R.id.bluetooth_button);
        usbButton = (Button) findViewById(R.id.usb_button);

        btButton.setOnClickListener(this);
        usbButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){

            case R.id.bluetooth_button:

                intent = new Intent(this, BluetoothActivity.class);
                startActivity(intent);

                break;
            case R.id.usb_button:

                intent = new Intent(this, UsbActivity.class);
                startActivity(intent);
                break;
        }
    }
}
