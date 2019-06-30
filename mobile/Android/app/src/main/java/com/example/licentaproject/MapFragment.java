package com.example.licentaproject;


import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.licentaproject.models.History;
import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.example.licentaproject.utils.SessionData;
import com.example.licentaproject.utils.SyncUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleMap.OnCircleClickListener, View.OnClickListener {

//    private List<Tracker> trackers;
    private GoogleMap mMap;
    private MapView mapView;
    private LocationManager locationManager;

    private Button connectWifiBtn;

    private BroadcastReceiver networkReceiver;

    private WifiConfiguration originalNet;

    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] nfcIntentFilters;
    private String[][] nfcTechList;

    private Context lostTrackerContext;

    public MapFragment() {
        // Required empty public constructor
    }

//    public void setTrackers(List<Tracker> trackers) {
//        this.trackers = trackers;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        this.trackers = new ArrayList<>();
        this.locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        networkReceiver = new NetworkChangeReceiver();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);


        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        connectWifiBtn = view.findViewById(R.id.connectWifiBtn);

        connectWifiBtn.setOnClickListener(this);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
        this.nfcPendingIntent =
                PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            filter.addDataType("*/*");
        } catch(IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        this.nfcIntentFilters = new IntentFilter[] { filter };
        this.nfcTechList = new String[][] { new String[] { NfcF.class.getName() } };

        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        lostTrackerContext = getContext();

        mMap.setOnCircleClickListener(this);
        new LostTrackerTask(mMap, lostTrackerContext).execute();

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setMyLocationEnabled(true);
        mMap.addCircle(new CircleOptions().center(sydney).fillColor(Color.RED).radius(200000).strokeWidth(0));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getContext().registerReceiver(networkReceiver, filter);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        this.nfcAdapter.enableForegroundDispatch(getActivity(), this.nfcPendingIntent, null, null);
        Log.d("RESUME_NFC", "On Resume, tag is " + (nfcAdapter.isEnabled() ? "ON" : "OFF"));
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(networkReceiver);
        nfcAdapter.disableForegroundDispatch(getActivity());
        Log.d("PAUSE_NFC", "On Pause, tag is " + (nfcAdapter.isEnabled() ? "ON" : "OFF"));
        mapView.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onLocationChanged(Location location) {
        SessionData.setLastLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onCircleClick(Circle circle) {
        Log.d("CIRCLE_CLICK", "Clicked on circle " + circle.getTag());
        if (SessionData.getActiveTracker() != null) {
            SessionData.getActiveTracker().setFillColor(Color.RED);
        }

        SessionData.setConfigStep(SessionData.ConfigStep.ATTEMPT_CONNECT);
        SessionData.setActiveTracker(circle);
        circle.setFillColor(Color.GREEN);
    }

    private class LostTrackerTask extends AsyncTask<Object, Void, List<Tracker>> {

//        private List<Tracker> original;
        private GoogleMap map;
        private Context context;

        public LostTrackerTask(GoogleMap map, Context context) {
            this.map = map;
            this.context = context;
        }

        @Override
        protected List<Tracker> doInBackground(Object... objects) {
            return (List<Tracker>) HttpRequestUtil.sendRequest("resource/lost/tracker", "GET", null, Tracker.class, true);
        }

        @Override
        protected void onPostExecute(List<Tracker> trackers) {
            // we shouldn't change the pointer to the array
            Toast.makeText(context, "" + trackers.size(), Toast.LENGTH_LONG).show();
            map.clear();
            for (Tracker tracker : trackers) {
                History lastPosition = tracker.getLastPosition();
                if (lastPosition != null) {
                    LatLng pos = new LatLng(lastPosition.getLatitude(), lastPosition.getLongitude());
                    map.addMarker(new MarkerOptions().position(pos).title(tracker.getName()).snippet(tracker.getUserId()));
                    Circle circle = map.addCircle(new CircleOptions().radius(lastPosition.getRange() * 1000).clickable(true).center(pos).fillColor(Color.RED));
                    circle.setTag(tracker);

                    // update the active circle, if the case
                    Circle active = SessionData.getActiveTracker();
                    if (active != null && pos.equals(active.getCenter())) {
                        SessionData.setActiveTracker(circle);
                        circle.setFillColor(Color.GREEN);
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        // pressed the Connect Wifi button
        Tracker tracker = (Tracker) SessionData.getActiveTracker().getTag();
        WifiManager manager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        String apName = SyncUtil.computeSsid(tracker);
        String apPass = SyncUtil.computePassword(tracker);

        // memorize active network connection
        int netId = manager.getConnectionInfo().getNetworkId();
        for (WifiConfiguration conf : manager.getConfiguredNetworks()) {
            if (conf.networkId == netId) {
                originalNet = conf;
                break;
            }
        }

        if (SyncUtil.connectNetwork(getContext(), apName, apPass, false)) {
            Log.d("MAP_NET_CONN", "Successfully enabled network");
        } else {
            Log.d("MAP_NET_CONN", "Failed to enable network");

            // reconnect to old AP
            manager.disconnect();
            int origId = manager.addNetwork(originalNet);
            manager.enableNetwork(origId, true);
            manager.reconnect();
        }
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Tracker tracker = SessionData.getActiveTracker() == null ? null : (Tracker) SessionData.getActiveTracker().getTag();
                if (tracker == null) {
                    return;
                }

                WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                String apName = SyncUtil.computeSsid(tracker);
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d("NETWORK_DETAILED_STATE", info.getDetailedState().name());

                String currentSsid = manager.getConnectionInfo().getSSID();
                currentSsid = currentSsid.substring(1, currentSsid.length() - 1);
                Log.d("NETWORK_SSID_STATE", String.format("%s %s", apName, currentSsid));
                Log.d("CURRENT_DISCO_STEP", SessionData.getConfigStep().name());
                if (info.isConnected()) {
                    if (currentSsid.equals(apName) && SessionData.getConfigStep() == SessionData.ConfigStep.ATTEMPT_CONNECT) {
                        SessionData.setConfigStep(SessionData.ConfigStep.ATTEMPT_UPDATE);
                        Log.d("NETWORK_SSID_RECV", currentSsid);
                        new SocketJob(context).execute();
                    } else if (SessionData.getConfigStep() == SessionData.ConfigStep.ATTEMPT_UPDATE) {
                        // check Internet connection
                        try {
                            InetAddress addr = InetAddress.getByName(SessionData.getPingUrl());
                            if (addr.equals("")) {
                                return;
                            }

                            // get the history update pool
                            for (Object o : SessionData.getFoundPool()) {
                                new HistoryUpdateTask().execute((Map) o);
                            }
                            SessionData.getFoundPool().clear();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }

    private class SocketJob extends AsyncTask<Tracker, Void, Map> {

        private Context context;
        private Tracker tracker;

        public SocketJob(Context context) {
            this.context = context;
            this.tracker = (Tracker) SessionData.getActiveTracker().getTag();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected Map doInBackground(Tracker... trackers) {
            // send an instruction to the Arduino
            Map<String, Object> comm = new HashMap<>();
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
                comm.put("action", "POS_UPDATE");
                comm.put("id", tracker.getRfId());

                ObjectMapper mapper = new ObjectMapper();
                byte[] msg = mapper.writeValueAsBytes(comm);
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                out.write(msg);
                out.flush();

                // read data
                Map<String, Object> result = mapper.readValue(socket.getInputStream(), Map.class);
                Log.d("POS_UPDATE_RESULT", result.toString());
                socket.close();

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Map result) {
            Log.d("MAP_ARDUINO_QUERY", result != null ? "POS QUERY SUCCESS" : "POS QUERY UNSUCCESSFUL");
            if (result == null) {
                return;
            }

            // disconnect from the current AP
            WifiManager manager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            manager.disconnect();
            for (WifiConfiguration conf : manager.getConfiguredNetworks()) {
                manager.removeNetwork(conf.networkId);
            }
            int netId = manager.addNetwork(originalNet);
            if (netId >= 0) {
                manager.enableNetwork(netId, true);
                manager.reconnect();
            }
            originalNet = null;


            // send data to the server
            // server side should be refined in order to recompense those who find lost devices
//            new HistoryUpdateTask().execute(result);
            SessionData.getFoundPool().add(result);
        }
    }

    private class HistoryUpdateTask extends AsyncTask<Map, Void, Map> {

        @Override
        protected Map doInBackground(Map... results) {
            int tries = 0;
            while (tries < 5) {
                Map response = (Map<String, Object>) HttpRequestUtil.sendRequest("resource/history", "POST", results[0], Map.class, false);
                if (response == null) {
                    tries++;
                } else {
                    return response;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Map response) {
            if (response != null) {
                Log.d("POS_QUERY_SERVER", response.toString());
                SessionData.setConfigStep(SessionData.ConfigStep.IDLE);
                SessionData.setActiveTracker(null);

                new LostTrackerTask(mMap, lostTrackerContext).execute();
            }

        }
    }

}
