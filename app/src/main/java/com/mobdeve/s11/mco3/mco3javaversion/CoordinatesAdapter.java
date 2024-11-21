package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CoordinatesAdapter extends RecyclerView.Adapter<CoordinatesAdapter.ViewHolder> {

    private ArrayList<String> coordinatesList;
    private Context context;

    public CoordinatesAdapter(Context context, ArrayList<String> coordinatesList) {
        this.context = context;
        this.coordinatesList = coordinatesList;
    }


    @NonNull
    @Override
    public CoordinatesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.coordinate_list_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull CoordinatesAdapter.ViewHolder holder, int position) {
        holder.coordinateTextView.setText(coordinatesList.get(position));
    }

    @Override
    public int getItemCount() {
        return coordinatesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView coordinateTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            coordinateTextView = itemView.findViewById(R.id.coordinateTextView);
        }
    }
}
