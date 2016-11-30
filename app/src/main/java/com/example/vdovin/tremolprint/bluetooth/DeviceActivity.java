package com.example.vdovin.tremolprint.bluetooth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.vdovin.tremolprint.R;

public class DeviceActivity extends Activity {
    public static final String EXTRA_ADDRESS = "bt_address";

    private LayoutInflater mInflater;
    private ListView mListView;
    private List<Pair<String, String>> mListItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        setResult(RESULT_CANCELED);

        mInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mListItems = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
                final Pair<String, String> data = mListItems.get(position);
                final String bthAddress = data.second;

                Intent resultData = new Intent();
                resultData.putExtra(EXTRA_ADDRESS, bthAddress);
                setResult(RESULT_OK, resultData);
                finish();
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadDevices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loadDevices() {
        final BluetoothAdapter bthAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bthAdapter == null) {
            return;
        }

        final ArrayAdapter<Pair<String, String>> arrayAdapter = new ArrayAdapter<Pair<String, String>>(this, android.R.layout.simple_list_item_2, mListItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row;
                if (convertView == null) {

                    row = mInflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = convertView;
                }

                final Pair<String, String> data = mListItems.get(position);
                TextView v = (TextView) row.findViewById(android.R.id.text1);
                v.setText(data.first);
                v = (TextView) row.findViewById(android.R.id.text2);
                v.setText(data.second);
                return row;
            }
        };
        mListView.setAdapter(arrayAdapter);

        final Set<BluetoothDevice> paired = bthAdapter.getBondedDevices();
        for (BluetoothDevice device : paired) {
            final Pair<String, String> data = new Pair<>(device.getName(), device.getAddress());
            mListItems.add(data);
        }

        arrayAdapter.notifyDataSetChanged();
    }
}
