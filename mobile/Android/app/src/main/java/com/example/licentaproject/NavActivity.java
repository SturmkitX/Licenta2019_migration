package com.example.licentaproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.models.User;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.example.licentaproject.utils.SessionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavActivity extends AppCompatActivity {

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentTransaction tx = null;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    tx = getSupportFragmentManager().beginTransaction();
                    tx.replace(R.id.fragment, new BlankFragment());
                    tx.commit();
                    return true;
                case R.id.navigation_dashboard:
                    tx = getSupportFragmentManager().beginTransaction();
                    tx.replace(R.id.fragment, new ProfileFragment());
                    tx.commit();
                    return true;
                case R.id.navigation_notifications:
                    tx = getSupportFragmentManager().beginTransaction();
                    tx.replace(R.id.fragment, new MapFragment());
                    tx.commit();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.add(R.id.fragment, new BlankFragment());
        tx.commit();

//        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
//        Log.d("TAG_CREATE", "Tag is in onCreate: " + (tag == null ? "NO" : "YES"));

        new UserRequestTask().execute();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private class UserRequestTask extends AsyncTask<Object, Void, User> {

        @Override
        protected User doInBackground(Object... params) {
            return (User) HttpRequestUtil.sendRequest("resource/me/user", "GET", null, User.class, false);
        }

        protected void onPostExecute(User user) {
            if (user == null) {
                Log.d("AUTH_USER_DATA_STATUS", "User fetch error!");
            } else {
                Log.d("AUTH_USER_DATA_STATUS", "User fetch successful");
                Log.d("USER_FETCH_ID", user.getId());
                SessionData.setUser(user);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onNewIntent(Intent intent) {
        // comes from the Map fragment
        if (SessionData.getActiveTracker() == null) {
            Log.d("NFC_MAP_UNNEC", "We are not currently looking for a tracker");
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d("Tag_ID", tag.toString());
        Tracker tracker = (Tracker) SessionData.getActiveTracker().getTag();
        if (tracker == null) {
            return;
        }

        // check if the found RFID is the one searched for
        byte[] bytes = tag.getId();
        if (bytes.length != tracker.getRfId().size()) {
            Log.d("NFC_MAP_SIZE", "Incorrect RFID size");
            return;
        }

        for (int i=0; i < bytes.length; i++) {
            if (bytes[i] != tracker.getRfId().get(i)) {
                Log.d("NFC_MAP_CONTENT", "Invalid content at position " + i);
                return;
            }
        }

        // the RFID is the correct one
        Log.d("NFC_MAP_FOUND", "Tracker found by RFID");

        // get 5 WIFI APs and the current GPS location and send them to the server
        // the APs should be sorted by their power level
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> scanResults = manager.getScanResults();

        Map<String, Object> toSend = new HashMap<>();
        toSend.put("rfId", tracker.getRfId());

        List<Map<String, Object>> posList = new ArrayList<>();
        Map<String, Object> wifiList = new HashMap<>();
        wifiList.put("source", "WIFI");

        List<Map<String, Object>> macList = new ArrayList<>();
        int maxScan = Math.min(scanResults.size(), 5);
        for (int i=0; i < maxScan; i++) {
            Map<String, Object> macMap = new HashMap<>();
            macMap.put("mac", scanResults.get(i).BSSID);
            macMap.put("rssi", scanResults.get(i).level);
            macList.add(macMap);
        }
        wifiList.put("macs", macList);

//        Location lastLocation = SessionData.getLastLocation();
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        @SuppressLint("MissingPermission") Location fineLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (fineLocation != null) {
            Map<String, Object> fineGpsList = new HashMap<>();
            fineGpsList.put("source", "GPS");
            fineGpsList.put("latitude", fineLocation.getLatitude());
            fineGpsList.put("longitude", fineLocation.getLongitude());
            fineGpsList.put("range", fineLocation.getAccuracy());
            posList.add(fineGpsList);
        }

        @SuppressLint("MissingPermission") Location coarseLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (coarseLocation == null) {
            Map<String, Object> coarseGpsList = new HashMap<>();
            coarseGpsList.put("source", "COARSE");
            coarseGpsList.put("latitude", coarseLocation.getLatitude());
            coarseGpsList.put("longitude", coarseLocation.getLongitude());
            coarseGpsList.put("range", coarseLocation.getAccuracy());
            posList.add(coarseGpsList);
        }

        posList.add(wifiList);

        toSend.put("positions", posList);
        ObjectMapper mapper = new ObjectMapper();
        try {
            Log.d("NFC_SEND_VALUES", mapper.writeValueAsString(toSend));

            // send the updated values
            SessionData.setConfigStep(SessionData.ConfigStep.ATTEMPT_UPDATE);
            SessionData.getFoundPool().add(toSend);
            manager.reconnect();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
