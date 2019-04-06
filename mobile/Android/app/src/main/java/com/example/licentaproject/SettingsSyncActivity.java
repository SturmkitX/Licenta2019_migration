package com.example.licentaproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.SyncUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
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

    private TextView syncStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_sync);

        syncStatus = findViewById(R.id.syncStatus);

        tracker = getIntent().getParcelableExtra("tracker");
        apName = SyncUtil.computeSsid(tracker);

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        syncStatus.setText(SyncUtil.connectHiddenNetwork(getApplicationContext(), apName) ? "SUCCESSFULLY CONNECTED" : "Could not connect!");

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(new WifiBroadcastReceiver(manager), filter);
//        new SocketJob(this).execute();

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(new NetworkChangeReceiver(manager), filter2);

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
                socket.connect(new InetSocketAddress("192.168.4.1", 80));
                comm.put("action", "AP_UPDATE");
                comm.put("id", tracker.getRfId());
                comm.put("apList", tracker.getAps());

                ObjectMapper mapper = new ObjectMapper();
                byte[] msg = mapper.writeValueAsBytes(comm);
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                out.write(msg);
                out.flush();
                socket.close();

                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            Toast.makeText(context, response ? "SYNC SUCCESS" : "SYNC UNSUCCESSFUL", Toast.LENGTH_LONG).show();
        }
    }

    private class WifiBroadcastReceiver extends BroadcastReceiver {

        private WifiManager manager;

        public WifiBroadcastReceiver(WifiManager manager) {
            this.manager = manager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (SupplicantState.isValidState(state) && state == SupplicantState.COMPLETED) {
                    // we are connected to a new network
                    // check if the connected network is the good one
                    String ssid = manager.getConnectionInfo().getSSID();
                    Log.d("RECEIVER_HIDDEN_SSID", manager.getConnectionInfo().getHiddenSSID() ? "YES" : "NO");
                    Log.d("RECEIVER_SUPP_STATE", manager.getConnectionInfo().getSupplicantState().toString());
                    Log.d("RECEIVER_SUPP_BSSID", manager.getConnectionInfo().getBSSID());
                    Log.d("RECEIVER_SSID", ssid);

                    if (ssid.equals(apName)) {
                        new SocketJob(context).execute();
                    }
                }
            }
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
                Log.d("NETWORK_DETAILED_STATE", info.getDetailedState().name());
                if (info.isConnected()) {
                    Log.d("NETWORK_SSID_RECV", manager.getConnectionInfo().getSSID());
                    new SocketJob(context).execute();
                }
            }
        }
    }
}
