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

public class SettingsCustomAdapter extends RecyclerView.Adapter<SettingsCustomAdapter.ViewHolder> {
    private Context context;
    private ArrayList<String> anomalyLabel;
    private SettingsAdapterInterface settingsAdapterInterface;

    SettingsCustomAdapter(Context context, ArrayList<String> anomalyLabel, SettingsAdapterInterface settingsAdapterInterface) {
        this.context = context;
        this.anomalyLabel = anomalyLabel;
        this.settingsAdapterInterface = settingsAdapterInterface;

    }
    @NonNull
    @Override
    public SettingsCustomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.configuration_list, parent, false);
        return new SettingsCustomAdapter.ViewHolder(view, settingsAdapterInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsCustomAdapter.ViewHolder holder, int position) {
        holder.anomalyLabel.setText(String.valueOf(anomalyLabel.get(position)));
    }

    @Override
    public int getItemCount() {
        return anomalyLabel.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView anomalyLabel;
        Button anomalyConfigButtonSetting;
        public ViewHolder(@NonNull View itemView, SettingsAdapterInterface settingsAdapterInterface) {
            super(itemView);

            anomalyLabel = itemView.findViewById(R.id.anomalyTextViewSetting);
            anomalyConfigButtonSetting = itemView.findViewById(R.id.anomalyConfigButtonSetting);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (settingsAdapterInterface != null) {
                        int pos = getAdapterPosition();

                        if (pos != RecyclerView.NO_POSITION) {
                            settingsAdapterInterface.onItemClick(pos);
                        }
                    }
                }
            });

            // Handle button clicks
            anomalyConfigButtonSetting.setOnClickListener(view -> {
                if (settingsAdapterInterface != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        settingsAdapterInterface.onButtonClick(pos);
                    }
                }
            });

        }
    }
}
