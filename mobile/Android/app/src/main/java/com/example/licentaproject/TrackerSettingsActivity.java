package com.example.licentaproject;

import android.content.Context;
import android.content.Intent;
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

import com.example.licentaproject.models.APPreference;
import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.utils.HttpRequestUtil;

import java.util.ArrayList;
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

    private EditText trackerSsid;
    private EditText trackerPass;
    private CheckBox trackerSsidActive;

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

        trackerSsid = (EditText) findViewById(R.id.trackerSsid0);
        trackerPass = (EditText) findViewById(R.id.trackerPass0);
        trackerSsidActive = (CheckBox) findViewById(R.id.trackerActive0);

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

        // update AP list
        List<APPreference> newAps = new ArrayList<>();
        APPreference pref = new APPreference();
        pref.setSsid(trackerSsid.getEditableText().toString());
        pref.setPassword(trackerPass.getEditableText().toString());
        pref.setActive(trackerSsidActive.isChecked());

        if (!pref.getSsid().isEmpty() && pref.isActive()) {
            newAps.add(pref);
        }
        tracker.setAps(newAps);

        // may need to update it
        new UpdateTask(tracker, this).execute(tracker);
    }

    private class UpdateTask extends AsyncTask<Tracker, Void, Object> {

        private Tracker tracker;
        private Context context;

        public UpdateTask(Tracker tracker, Context context) {
            this.tracker = tracker;
            this.context = context;
        }

        @Override
        protected Object doInBackground(Tracker... trackers) {
            return HttpRequestUtil.sendRequest("resource/tracker", "PUT", trackers[0], Object.class, false);
        }

        @Override
        protected void onPostExecute(Object status) {
            Log.d("TRACKER_UPDATE", "Success");

            Intent intent = new Intent(context, SettingsSyncActivity.class);
            intent.putExtra("tracker", tracker);
            startActivity(intent);
            finish();
        }
    }
}
