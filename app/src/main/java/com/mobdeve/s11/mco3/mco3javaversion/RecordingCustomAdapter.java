package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecordingCustomAdapter extends RecyclerView.Adapter<RecordingCustomAdapter.MyViewHolder> {
    private final RecordingAdapterInterface recordingAdapterInterface;
    private Context context;
    private ArrayList<String> recordingDate, recordingTimestamp;
    private ArrayList<Integer> recordingId;

    RecordingCustomAdapter(Context context, ArrayList<String> recordingDate, ArrayList<String> recordingTimestamp, ArrayList<Integer> recordingId,
                            RecordingAdapterInterface recordingAdapterInterface) {
        this.context = context;
        this.recordingDate = recordingDate;
        this.recordingTimestamp = recordingTimestamp;
        this.recordingId = recordingId;
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
        holder.recordingId.setText(String.valueOf(recordingId.get(position)));

        holder.deleteButton.setOnClickListener(v -> {
            if (recordingAdapterInterface != null) {
                recordingAdapterInterface.onDeleteClick(position);
            }
        });

        holder.renameButton.setOnClickListener(v -> {
            if (recordingAdapterInterface != null){
                recordingAdapterInterface.onUpdateClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordingDate.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView recordingDate, recordingTimestamp, recordingId;
        Button deleteButton;
        Button renameButton;

        public MyViewHolder(@NonNull View itemView, RecordingAdapterInterface recordingAdapterInterface) {
            super(itemView);
            recordingDate = itemView.findViewById(R.id.tv_recDate);
            recordingTimestamp = itemView.findViewById(R.id.tv_recTimestamp);
            recordingId = itemView.findViewById(R.id.tv_recordingId);
            deleteButton = itemView.findViewById(R.id.btn_delete);
            renameButton = itemView.findViewById(R.id.rename);

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
