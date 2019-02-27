package com.example.licentaproject;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class BlankFragment extends Fragment {

    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techList;

    public BlankFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adapter = NfcAdapter.getDefaultAdapter(getActivity());
        this.pendingIntent =
                PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            filter.addDataType("*/*");
        } catch(IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        this.intentFilters = new IntentFilter[] { filter };
        this.techList = new String[][] { new String[] { NfcF.class.getName() } };

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.adapter.disableForegroundDispatch(getActivity());
        Log.d("PAUSE_NFC", "On Pause, tag is " + (adapter.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void onResume() {
        super.onResume();
        this.adapter.enableForegroundDispatch(getActivity(), this.pendingIntent, null, null);
        Log.d("RESUME_NFC", "On Resume, tag is " + (adapter.isEnabled() ? "ON" : "OFF"));
        Log.d("FRAG_ACTIVITY", getClass().getSimpleName());
    }
}
