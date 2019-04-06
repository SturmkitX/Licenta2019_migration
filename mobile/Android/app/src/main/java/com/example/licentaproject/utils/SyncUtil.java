package com.example.licentaproject.utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.example.licentaproject.models.Tracker;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class SyncUtil {

    private SyncUtil() {

    }

    public static String computeSsid(Tracker tracker) {
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

    public static boolean connectHiddenNetwork(Context context, String apName) {
        // add the WiFi network
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!manager.isWifiEnabled() && manager.setWifiEnabled(true)) {
            Toast.makeText(context, "WiFi is not / could not be enabled!", Toast.LENGTH_LONG).show();
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
        if (addedId >= 0) {
            Log.d("ADD_NETWORK","Successfully added Arduino\n");
        } else {
            Log.d("ADD_NETWORK","Could not add Arduino\n");
            return false;
        }
        int reconnectId = manager.getConnectionInfo().getNetworkId();

        if (manager.disconnect()) {
            Log.d("NET_DISCONNECT","Successfully disconnected from Original AP\n");
        } else {
            Log.d("NET_DISCONNECT", "Failed to disconnect AP\n");
            return false;
        }

        Log.d("NETWORK_ID", String.format("%d %d %d", origId, addedId, conf.networkId));
        if (manager.enableNetwork(addedId, true)) {
            Log.d("NET_ENABLE","Successfully enabled AP\n");
        } else {
            Log.d("NET_ENABLE","Failed to enable AP\n");
            return false;
        }
        manager.reconnect();

        return true;
    }
}
