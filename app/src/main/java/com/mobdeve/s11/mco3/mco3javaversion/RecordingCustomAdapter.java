package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecordingCustomAdapter extends RecyclerView.Adapter<RecordingCustomAdapter.MyViewHolder> {
    private final RecordingAdapterInterface recordingAdapterInterface;
    private Context context;
    private ArrayList<String> recordingDate, recordingTimestamp;

    RecordingCustomAdapter(Context context, ArrayList<String> recordingDate, ArrayList<String> recordingTimestamp,
                            RecordingAdapterInterface recordingAdapterInterface) {
        this.context = context;
        this.recordingDate = recordingDate;
        this.recordingTimestamp = recordingTimestamp;
        this.recordingAdapterInterface = recordingAdapterInterface;
    }

    @NonNull
    @Override
    public RecordingCustomAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recording_list_item, parent, false);
        return new MyViewHolder(view, recordingAdapterInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingCustomAdapter.MyViewHolder holder, int position) {
        holder.recordingDate.setText(String.valueOf(recordingDate.get(position)));
        holder.recordingTimestamp.setText(String.valueOf(recordingTimestamp.get(position)));
    }

    @Override
    public int getItemCount() {
        return recordingDate.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView recordingDate, recordingTimestamp;

        public MyViewHolder(@NonNull View itemView, RecordingAdapterInterface recordingAdapterInterface) {
            super(itemView);
            recordingDate = itemView.findViewById(R.id.tv_recDate);
            recordingTimestamp = itemView.findViewById(R.id.tv_recTimestamp);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (recordingAdapterInterface != null) {
                        int pos = getAdapterPosition();

                        if (pos != RecyclerView.NO_POSITION) {
                            recordingAdapterInterface.onItemClick(pos);
                        }
                    }
                }
            });

        }
    }
}
