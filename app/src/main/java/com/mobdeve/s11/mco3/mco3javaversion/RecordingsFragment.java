package com.mobdeve.s11.mco3.mco3javaversion;

import android.database.Cursor;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecordingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordingsFragment extends Fragment {

    RecyclerView recyclerview;
    MyDatabaseHelper myDB;
    ArrayList<String> recordingDate, recordingTimestamp;
    RecordingCustomAdapter recordingCustomAdapter;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RecordingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RecordingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RecordingsFragment newInstance(String param1, String param2) {
        RecordingsFragment fragment = new RecordingsFragment();
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
        View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        recyclerview = view.findViewById(R.id.recordingRecyclerView);

        myDB = new MyDatabaseHelper(requireContext());
        recordingDate = new ArrayList<String>();
        recordingTimestamp = new ArrayList<String>();

        storeDataInArrays();
//
        recordingCustomAdapter = new RecordingCustomAdapter(requireContext(),recordingDate, recordingTimestamp);
        recyclerview.setAdapter(recordingCustomAdapter);
        recyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        return view;
    }

    void storeDataInArrays() {
        Cursor cursor = myDB.readAllData();
        if (cursor.getCount() == 0) {
            Toast.makeText(requireContext(), "No Data ", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                recordingDate.add(cursor.getString(cursor.getColumnIndexOrThrow("recording_date")));
                recordingTimestamp.add(cursor.getString(cursor.getColumnIndexOrThrow("recording_timestamp")));
            }
        }
    }
}