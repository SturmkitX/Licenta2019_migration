package com.example.licentaproject;


import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private List<Tracker> trackers;
    private GoogleMap mMap;
    private MapView mapView;


    public MapFragment() {
        // Required empty public constructor
    }

    public void setTrackers(List<Tracker> trackers) {
        this.trackers = trackers;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.trackers = new ArrayList<>();

        new LostTrackerTask(trackers).execute();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);


        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
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

    private class LostTrackerTask extends AsyncTask<Object, Void, List<Tracker>> {

        private List<Tracker> original;

        public LostTrackerTask(List<Tracker> fragment) {
            this.original = fragment;
        }

        @Override
        protected List<Tracker> doInBackground(Object... objects) {
            return (List<Tracker>) HttpRequestUtil.sendRequest("resource/lost/tracker", "GET", null, Tracker.class, true);
        }

        @Override
        protected void onPostExecute(List<Tracker> trackers) {
            // we shouldn't change the pointer to the array
            original.clear();
            original.addAll(trackers);
        }
    }

}
