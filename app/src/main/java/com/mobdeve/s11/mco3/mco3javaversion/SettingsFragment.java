package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment implements SettingsAdapterInterface{
    RecyclerView recyclerView;
    MyDatabaseHelper myDB;
    ArrayList<String> anomalyLabel;
    SettingsCustomAdapter settingsCustomAdapter;
    Button addLabelButton;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        recyclerView = view.findViewById(R.id.configRecyclerView);
        addLabelButton = view.findViewById(R.id.addLabelButtonOrigin);

        myDB = new MyDatabaseHelper(requireContext());
        anomalyLabel = new ArrayList<String>();

        storeDataInArrays();

        addLabelButton.setOnClickListener( v -> {
            Intent intent = new Intent(requireContext(), AddLabel.class);
            startActivity(intent);
        });
        settingsCustomAdapter = new SettingsCustomAdapter(requireContext(),
                anomalyLabel, this);
        recyclerView.setAdapter(settingsCustomAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        return view;
    }

    void storeDataInArrays() {
        Cursor cursor = myDB.getAllAnomalyNames();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String anomalyName = cursor.getString(cursor.getColumnIndexOrThrow("anomaly_name"));
                anomalyLabel.add(anomalyName);
            }
            cursor.close();
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(requireContext(), SettingsConfigurationItem.class);
        intent.putExtra("ANOMALY_LABEL", anomalyLabel.get(position));

        startActivity(intent);
    }

    @Override
    public void onButtonClick(int position) {
        Intent intent = new Intent(requireContext(), SettingsConfigurationItem.class);
        intent.putExtra("ANOMALY_LABEL", anomalyLabel.get(position));

        startActivity(intent);
    }
}