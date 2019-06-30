package com.example.licentaproject.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.licentaproject.R;
import com.example.licentaproject.TrackerSettingsActivity;
import com.example.licentaproject.models.Tracker;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> implements View.OnClickListener {
    private List<Tracker> mDataset;
    private Context context;    // used for context switching

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public TextView textView2;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            textView2 = itemView.findViewById(R.id.textView2);
            itemView.setTag(this);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(List<Tracker> myDataset, Context cont) {
        mDataset = myDataset;
        context = cont;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tracker, parent, false);
        MyViewHolder vh = new MyViewHolder(v);

        v.setOnClickListener(this);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.setText(mDataset.get(position).getName());
        holder.textView2.setText(mDataset.get(position).getRfId().toString());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public void onClick(View v) {
        MyViewHolder holder = (MyViewHolder) v.getTag();
        int position = holder.getAdapterPosition();
        Tracker tracker = mDataset.get(position);

        // start a new TrackerSettingsActivity
        Log.d("ADAPTER_TRACKER_NULL", tracker == null ? "NULL" : "NOT NULL");
        Intent intent = new Intent(context, TrackerSettingsActivity.class);
        intent.putExtra("tracker", (Parcelable) tracker);
        context.startActivity(intent);
    }
}
