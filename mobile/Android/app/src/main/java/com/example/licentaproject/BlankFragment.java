package com.example.licentaproject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.example.licentaproject.utils.MyAdapter;

import java.util.ArrayList;
import java.util.List;


public class BlankFragment extends Fragment {

    private List<Tracker> trackers;
    private RecyclerView trackerView;
    private MyAdapter adapter;

    public BlankFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.trackers = new ArrayList<>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_blank, container, false);

        // get NFC button
        FloatingActionButton addBtn = (FloatingActionButton) view.findViewById(R.id.addTrackerBtn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), TrackerAddActivity.class);
                startActivity(intent);
            }
        });

        this.trackerView = (RecyclerView) view.findViewById(R.id.trackerView);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        this.trackerView.setLayoutManager(manager);

        this.adapter = new MyAdapter(trackers);
        trackerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // get the updated list of trackers
        new TrackerTask(trackers, adapter).execute();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private class TrackerTask extends AsyncTask<Void, Void, List<Tracker>> {

        private List<Tracker> origList;
        private MyAdapter adapter;

        public TrackerTask(List<Tracker> origList, MyAdapter adapter) {
            this.origList = origList;
            this.adapter = adapter;
        }

        @Override
        protected List<Tracker> doInBackground(Void... voids) {
            return (List<Tracker>) HttpRequestUtil.sendRequest("resource/me/tracker", "GET", null, Tracker.class, true);
        }

        @Override
        protected void onPostExecute(List<Tracker> trackers) {
            // update the view
            List<String> names = new ArrayList<>();
            for (Tracker t : trackers) {
                names.add(t.getName());
            }

            origList.clear();
            origList.addAll(trackers);
            adapter.notifyDataSetChanged();

            Log.d("LIST_SIZE_ADAPTER", "" + adapter.getItemCount());
            Log.d("LIST_SIZE_COLLECTION", "" + origList.size());
        }
    }
}
