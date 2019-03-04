package com.example.licentaproject;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.HttpRequestUtil;

import java.util.Arrays;
import java.util.List;

public class TrackerSettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private TextView idView;
    private EditText nameView;
    private CheckBox lostView;
    private CheckBox gpsView;
    private CheckBox wifiView;
    private CheckBox alarmView;
    private Spinner methodView;
    private Button updateBtn;

    private Tracker tracker;
    final List<String> methodOptions = Arrays.asList("WPS + GPS", "WPS Only", "GPS Only", "None");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_settings);

        idView = (TextView) findViewById(R.id.idView);
        nameView = (EditText) findViewById(R.id.nameView);
        lostView = (CheckBox) findViewById(R.id.lostView);
        gpsView = (CheckBox) findViewById(R.id.gpsView);
        wifiView = (CheckBox) findViewById(R.id.wifiView);
        alarmView = (CheckBox) findViewById(R.id.alarmView);
        methodView = (Spinner) findViewById(R.id.methodView);
        updateBtn = (Button) findViewById(R.id.updateBtn);

        methodView.setOnItemSelectedListener(this);
        updateBtn.setOnClickListener(this);

        // update initial page
        tracker = getIntent().getParcelableExtra("tracker");
        idView.setText(tracker.getRfId().toString());
        nameView.setText(tracker.getName());
        lostView.setChecked(tracker.isLost());
        gpsView.setChecked(tracker.isGpsActive());
        wifiView.setChecked(tracker.isWifiActive());
        alarmView.setChecked(tracker.isAlarmActive());

        methodView.setSelection(methodOptions.indexOf(tracker.getPreferredMethod()));

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String chosen = (String) parent.getItemAtPosition(position);
        Toast.makeText(this, chosen, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        tracker.setName(nameView.getText().toString());
        tracker.setPreferredMethod((String) methodView.getSelectedItem());

        // may need to update it
        new UpdateTask().execute(tracker);
    }

    private class UpdateTask extends AsyncTask<Tracker, Void, Object> {

        @Override
        protected Object doInBackground(Tracker... trackers) {
            return HttpRequestUtil.sendRequest("resource/tracker", "PUT", trackers[0], Object.class, false);
        }

        @Override
        protected void onPostExecute(Object tracker) {
            Log.d("TRACKER_UPDATE", "Success");
        }
    }
}
