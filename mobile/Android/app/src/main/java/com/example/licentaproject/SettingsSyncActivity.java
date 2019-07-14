package com.example.licentaproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.licentaproject.models.APPreference;
import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.SyncUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsSyncActivity extends AppCompatActivity {

    /*
    Steps to be followed:
    1. Get the tracker RFID code
    2. Compute the AP name and password
    3. Connect to the AP and the server
    4. Synchronize settings (TBD)
     */

    private Tracker tracker;
    private String apName;
    private String apPass;

    private TextView syncStatus;
    private boolean connected;

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_sync);

        syncStatus = findViewById(R.id.syncStatus);

        tracker = getIntent().getParcelableExtra("tracker");
        apName = SyncUtil.computeSsid(tracker);
        apPass = SyncUtil.computePassword(tracker);

        Log.d("NETWORK_CREDENTIALS", String.format("Name: %s, Password: %s", apName, apPass));


        syncStatus.setText(SyncUtil.connectNetwork(getApplicationContext(), apName, apPass, false) ? "SUCCESSFULLY CONNECTED" : "Could not connect!");

        connected = false;

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        broadcastReceiver = new NetworkChangeReceiver(manager);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    private class SocketJob extends AsyncTask<Tracker, Void, Boolean> {

        private Context context;

        public SocketJob(Context context) {
            this.context = context;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected Boolean doInBackground(Tracker... trackers) {
            // send an instruction to the Arduino
            Map<String, Object> comm = new HashMap<>();
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                Network selected = null;    // may need to check more
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        selected = network;
                        break;
                    }
                }

                Socket socket = new Socket();
                selected.bindSocket(socket);
                socket.connect(new InetSocketAddress("192.168.4.1", 80), 20000);
                comm.put("action", "AP_UPDATE");
                comm.put("id", tracker.getRfId());

                List<APPreference> filteredAps = new ArrayList<>();
                for (APPreference pref : tracker.getAps()) {
                    if (pref.isActive()) {
                        filteredAps.add(pref);
                    }
                }
                comm.put("apList", filteredAps);

                ObjectMapper mapper = new ObjectMapper();
                byte[] msg = mapper.writeValueAsBytes(comm);
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                out.write(msg);
                out.flush();

                // get the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response = reader.readLine();
                socket.close();

                Log.d("SOCKET_RESPONSE", response);

                return (response.equals("ACK"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            Log.d("SYNC_STATUS", response ? "SYNC SUCCESS" : "SYNC UNSUCCESSFUL");
            Toast.makeText(context, response ? "SYNC SUCCESS" : "SYNC UNSUCCESSFUL", Toast.LENGTH_LONG).show();
        }
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {

        private WifiManager manager;

        public NetworkChangeReceiver(WifiManager manager) {
            this.manager = manager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                int address = manager.getConnectionInfo().getIpAddress();
                int stub1 = (address >>> 24);
                int stub2 = ((address >>> 16) & 0xFF);
                int stub3 = ((address >>> 8) & 0xFF);
                int stub4 = (address & 0xFF);
                Log.d("NETWORK_DETAILED_STATE", info.getDetailedState().name());
                Log.d("NETWORK_IP_ADDR", String.format("%d.%d.%d.%d", stub4, stub3, stub2, stub1));
                if (info.isConnected() && !connected) {
                    connected = true;
                    Log.d("NETWORK_SSID_RECV", manager.getConnectionInfo().getSSID());
                    new SocketJob(context).execute();
                }
            }
        }
    }
}
