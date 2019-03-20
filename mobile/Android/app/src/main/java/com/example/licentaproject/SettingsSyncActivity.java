package com.example.licentaproject;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
        apName = computeSsid();

        // add the WiFi network
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!manager.isWifiEnabled() && manager.setWifiEnabled(true)) {
            Toast.makeText(this, "WiFi is not / could not be enabled!", Toast.LENGTH_LONG).show();
        }

        // see the status of all configured networks
        manager.disconnect();
        List<WifiConfiguration> confList = manager.getConfiguredNetworks();
        Log.d("CONF_INFO_LEN", "" + confList.size());
        for (WifiConfiguration conf : confList) {
            Log.d("CONF_INFO", String.format("%d %s %s", conf.networkId, conf.SSID, WifiConfiguration.Status.strings[conf.status]));
            manager.removeNetwork(conf.networkId);
        }

        WifiConfiguration conf = new WifiConfiguration();
        conf.hiddenSSID = true;
        conf.SSID = String.format("\"%s\"", apName);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP | WifiConfiguration.GroupCipher.TKIP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.status = WifiConfiguration.Status.ENABLED;

        int origId = conf.networkId;
        int addedId = manager.addNetwork(conf);
        StringBuilder status = new StringBuilder();
        if (addedId >= 0) {
            status.append("Successfully added Arduino\n");
        } else {
            status.append("Could not add Arduino\n");
        }
        int reconnectId = manager.getConnectionInfo().getNetworkId();

        if (manager.disconnect()) {
            status.append("Successfully disconnected from Original AP\n");
        } else {
            status.append("Failed to disconnect AP\n");
        }

//        confList = manager.getConfiguredNetworks();
//        for (WifiConfiguration confIter : confList) {
//            if (confIter.networkId == addedId) {
//                confIter.status = WifiConfiguration.Status.CURRENT;
//                manager.updateNetwork(confIter);
//                break;
//            }
//
//        }

        Log.d("NETWORK_ID", String.format("%d %d %d", origId, addedId, conf.networkId));
        if (manager.enableNetwork(addedId, true)) {
            status.append("Successfully enabled AP\n");
        } else {
            status.append("Failed to enable AP\n");
        }
        manager.reconnect();

        syncStatus.setText(status.toString());

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(new WifiBroadcastReceiver(manager), filter);
//        new SocketJob(this).execute();

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(new NetworkChangeReceiver(manager), filter2);

    }

    private String computeSsid() {
        List<Byte> rfId = tracker.getRfId();
        String apName = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder builder = new StringBuilder();

            builder.append(rfId.get(0) & 0xFF);
            for (int i=1; i < rfId.size(); i++) {
                builder.append(String.format(":%d", rfId.get(i) & 0xFF));
            }

            String preDigest = builder.toString();
            Log.d("HEX_PRE_PROC", preDigest);
            Log.d("HEX_PRE_PROC_LEN", "" + preDigest.getBytes(Charset.forName("UTF-8")).length);

            builder = new StringBuilder();

            md.update(preDigest.getBytes(Charset.forName("UTF-8")));
            byte[] result = md.digest();

            byte[] expanded = new byte[result.length * 2];
            for (int i=0; i < result.length; i++) {
                expanded[i*2] = (byte)((result[i] & 0xFF) >>> 4);
                expanded[i*2+1] = (byte)(result[i] & 0x0F);
                Log.d("HEX_PROC", String.format("%d %d %d", result[i], expanded[i*2], expanded[i*2+1]));
            }

            char[] hex = "0123456789abcdef".toCharArray();

            for (int i=0; i < 8; i++) {
                builder.append(hex[ expanded[i] ]);
            }
            for (int i=16; i < 24; i++) {
                builder.append(hex[ expanded[i] ]);
            }
            for (int i=32; i < 40; i++) {
                builder.append(hex[ expanded[i] ]);
            }

            apName = builder.toString();
            Log.d("AP_HEX_NAME", apName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return apName;
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
