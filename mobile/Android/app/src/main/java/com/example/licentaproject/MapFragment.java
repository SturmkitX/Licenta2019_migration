package com.example.licentaproject;


import android.annotation.SuppressLint;
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
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.licentaproject.models.History;
import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.HttpRequestUtil;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleMap.OnCircleClickListener {

//    private List<Tracker> trackers;
    private GoogleMap mMap;
    private MapView mapView;
    private LocationManager locationManager;
    private Circle activeTracker;

    private TextView mapUserField;
    private ToggleButton mapStatusField;

    // will run periodically a network scan routine
    private Timer scanTimer;
    private TimerTask currentTask;


    public MapFragment() {
        // Required empty public constructor
    }

//    public void setTrackers(List<Tracker> trackers) {
//        this.trackers = trackers;
//    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        this.trackers = new ArrayList<>();
        this.locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);

        scanTimer = new Timer();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);


        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapUserField = view.findViewById(R.id.mapUserField);
        mapStatusField = view.findViewById(R.id.mapStatusField);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnCircleClickListener(this);
        new LostTrackerTask(mMap, getContext()).execute();

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setMyLocationEnabled(true);
        mMap.addCircle(new CircleOptions().center(sydney).fillColor(Color.RED).radius(200000).strokeWidth(0));
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
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
//        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
//        mMap.addCircle(new CircleOptions().fillColor(Color.BLUE).clickable(false).radius(location.getAccuracy()));
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
        if (activeTracker != null) {
            activeTracker.setFillColor(Color.RED);
            currentTask.cancel();
        }

        activeTracker = circle;
        activeTracker.setFillColor(Color.GREEN);

        scanTimer.purge();
        currentTask = new FindAPTask((Tracker) activeTracker.getTag());
        scanTimer.scheduleAtFixedRate(currentTask, 15000, 15000);
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
                }
            }
        }
    }

    private class FindAPTask extends TimerTask {

        private Tracker tracker;

        public FindAPTask(Tracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public void run() {
            String apName = SyncUtil.computeSsid(tracker);
//            boolean connected = SyncUtil.connectHiddenNetwork(getContext(), apName);
//            Log.d("FOUND_AP_PASSIVE", connected ? "YES" : "NO");
            WifiManager manager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            ScanResult found = null;
            for (ScanResult result : manager.getScanResults()) {
                Log.d("RESULT_SSID", result.SSID);
                if (result.SSID.equals(apName)) {
                    found = result;
                    break;
                }
            }

            if (found == null) {
                return;
            }

            // create a Network config
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = String.format("\"%s\"", found.SSID);
            conf.preSharedKey = "\"\"";     // should be added later
            int networkId = manager.addNetwork(conf);
            if (networkId < 0) {
                Log.d("MAP_NET_ADD","Unable to add new network");
                return;
            }

            int reconnectId = manager.getConnectionInfo().getNetworkId();
            manager.disconnect();
            if (manager.enableNetwork(networkId, true)) {
                Log.d("MAP_NET_CONN", "Successfully enabled network");
            } else {
                Log.d("MAP_NET_CONN", "Failed to enable network");
            }

            // perform data exchange with Arduino
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            getContext().registerReceiver(new NetworkChangeReceiver(manager, tracker), filter);

        }
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {

        private WifiManager manager;
        private Tracker tracker;

        public NetworkChangeReceiver(WifiManager manager, Tracker tracker) {
            this.manager = manager;
            this.tracker = tracker;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d("NETWORK_DETAILED_STATE", info.getDetailedState().name());
                if (info.isConnected()) {
                    Log.d("NETWORK_SSID_RECV", manager.getConnectionInfo().getSSID());
                    new SocketJob(context, tracker).execute();
                }
            }
        }
    }

    private class SocketJob extends AsyncTask<Tracker, Void, Map> {

        private Context context;
        private Tracker tracker;

        public SocketJob(Context context, Tracker tracker) {
            this.context = context;
            this.tracker = tracker;
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
        protected void onPostExecute(Map response) {
            Toast.makeText(context, response == null ? "POS QUERY SUCCESS" : "POS QUERY UNSUCCESSFUL", Toast.LENGTH_LONG).show();
            if (response == null) {
                return;
            }

            // send data to the server
            // server side should be refined in order to recompense those who find lost devices
            HttpRequestUtil.sendRequest("public/history", "POST", response, Map.class, false);
        }
    }

}
