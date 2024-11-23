package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobdeve.s11.mco3.mco3javaversion.databinding.CoordinateListItemBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoordinatesAdapter extends RecyclerView.Adapter<CoordinatesAdapter.ViewHolder> {

    private List<Map<String, Double>> coordinatesList;
    private ArrayList<String> anomalyList;
    private Context context;
    private CoordinatesInterface coordinatesInterface;

    public CoordinatesAdapter(Context context, List<Map<String, Double>> coordinatesList, ArrayList<String> anomalyList,
                              CoordinatesInterface coordinatesInterface) {
        this.context = context;
        this.coordinatesList = coordinatesList;
        this.anomalyList = anomalyList;
        this.coordinatesInterface = coordinatesInterface;

    }


    @NonNull
    @Override
    public CoordinatesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        CoordinateListItemBinding view = CoordinateListItemBinding.inflate(inflater, parent, false);
        return new ViewHolder(view.getRoot(), coordinatesInterface);

    }

    @Override
    public void onBindViewHolder(@NonNull CoordinatesAdapter.ViewHolder holder, int position) {
        Map<String, Double> coordinate = coordinatesList.get(position);
        double latitude = coordinate.get("lat");
        double longitude = coordinate.get("lon");

        holder.coordinateTextViewLat.setText(Double.toString(latitude));
        holder.coordinateTextViewLon.setText(Double.toString(longitude));
        holder.anomalyTextView.setText(anomalyList.get(position));
    }

    @Override
    public int getItemCount() {
        return coordinatesList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView coordinateTextViewLat;
        TextView coordinateTextViewLon;
        TextView anomalyTextView;

        public ViewHolder(@NonNull View itemView, CoordinatesInterface coordinatesInterface) {
            super(itemView);
            coordinateTextViewLat = itemView.findViewById(R.id.coordinateTextViewLat);
            coordinateTextViewLon =  itemView.findViewById(R.id.coordinateTextViewLon);
            anomalyTextView = itemView.findViewById(R.id.anomalyTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (coordinatesInterface != null) {
                        int pos = getAdapterPosition();

                        if (pos != RecyclerView.NO_POSITION) {
                            coordinatesInterface.onItemClick(pos);
                        }
                    }
                }
            });
        }
    }
}
