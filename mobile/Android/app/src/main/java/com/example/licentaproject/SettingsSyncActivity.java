package com.example.licentaproject;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.licentaproject.models.Tracker;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        int reconnectId = manager.getConnectionInfo().getNetworkId();
        StringBuilder status = new StringBuilder();
        if (addedId >= 0) {
            status.append("Successfully added Arduino\n");
        } else {
            status.append("Could not add Arduino\n");
        }

        if (manager.disconnect()) {
            status.append("Successfully disconnected from Original AP\n");
        } else {
            status.append("Failed to disconnect AP\n");
        }

        Log.d("NETWORK_ID", String.format("%d %d %d", origId, addedId, conf.networkId));
        if (manager.enableNetwork(addedId, true)) {
            status.append("Successfully enabled AP\n");
        } else {
            status.append("Failed to enable AP\n");
        }

        syncStatus.setText(status.toString());
        new SocketJob(this).execute();

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

        @Override
        protected Boolean doInBackground(Tracker... trackers) {
            // send an instruction to the Arduino
            Map<String, Object> comm = new HashMap<>();
            try {
                Socket socket = new Socket("192.168.4.1", 80);
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
}
