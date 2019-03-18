package com.example.licentaproject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.licentaproject.models.Tracker;
import com.example.licentaproject.models.User;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.example.licentaproject.utils.SessionData;
import com.fasterxml.jackson.databind.util.ArrayIterator;
import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerAddActivity extends AppCompatActivity implements View.OnClickListener {

    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techList;

    private TextView statusText;
    private EditText trackerName;
    private Button pairBtn;

    private byte[] tagId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker_add);
        this.adapter = NfcAdapter.getDefaultAdapter(this);
        this.pendingIntent =
                PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter filter2 = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter filter3 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            filter.addDataType("*/*");
            filter2.addDataType("*/*");
            filter3.addDataType("*/*");
        } catch(IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        this.intentFilters = new IntentFilter[] { filter, filter2, filter3 };
        this.techList = new String[][] { new String[] { NfcF.class.getName() } };

        this.statusText = (TextView) findViewById(R.id.detectStatus);
        this.trackerName = (EditText) findViewById(R.id.trackerName);
        this.pairBtn = (Button) findViewById(R.id.pairBtn);

        this.pairBtn.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d("Tag_ID", tag.toString());

        this.tagId = tag.getId();
        String tagString = Arrays.toString(this.tagId);

        statusText.setText("RFID found: " + tagString);
        trackerName.setEnabled(true);
        pairBtn.setEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.adapter.disableForegroundDispatch(this);
        Log.d("PAUSE_NFC", "On Pause, tag is " + (adapter.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void onResume() {
        super.onResume();
        this.adapter.enableForegroundDispatch(this, this.pendingIntent, null, null);
        Log.d("RESUME_NFC", "On Resume, tag is " + (adapter.isEnabled() ? "ON" : "OFF"));
        Log.d("FRAG_ACTIVITY", getClass().getSimpleName());
    }

    @Override
    public void onClick(View v) {
        // get name
        String name = this.trackerName.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please provide a name", Toast.LENGTH_SHORT).show();
            return;
        }

        // pair the tracker
        new TrackerRequestTask(this).execute(this.tagId, name, SessionData.getUser().getId());

    }

    private class TrackerRequestTask extends AsyncTask<Object, Void, Tracker> {

        private Context context;

        public TrackerRequestTask(Context context) {
            this.context = context;
        }

        @Override
        protected Tracker doInBackground(Object... params) {
            List<Byte> idBytes = new ArrayList<>();
            byte[] origBytes = (byte[]) params[0];
            for (byte b : origBytes) {
                idBytes.add(b);
            }

            Map<String, Object> credMap = new HashMap<>();
            credMap.put("rfId", idBytes);
            credMap.put("name", params[1]);
            credMap.put("userId", params[2]);
            return (Tracker) HttpRequestUtil.sendRequest("resource/tracker", "POST", credMap, Tracker.class, false);
        }

        protected void onPostExecute(Tracker tracker) {
            if (tracker == null) {
                Log.d("TRACKER_DATA_STATUS", "Tracker save error!");
            } else {
                Log.d("TRACKER_DATA_STATUS", "Tracker save successful");
                Toast.makeText(context, "Successfully paired the tracker", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
