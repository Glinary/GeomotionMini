package com.mobdeve.s11.mco3.mco3javaversion;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecordingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordingsFragment extends Fragment implements RecordingAdapterInterface{

    RecyclerView recyclerview;
    MyDatabaseHelper myDB;
    ArrayList<String> recordingDate, recordingTimestamp;
    ArrayList<Integer> recordingId;
    RecordingCustomAdapter recordingCustomAdapter;
    private Button manageRecordingsButton;

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
        manageRecordingsButton = view.findViewById(R.id.manageRecordingsButton);

        myDB = new MyDatabaseHelper(requireContext());
        recordingDate = new ArrayList<String>();
        recordingTimestamp = new ArrayList<String>();
        recordingId = new ArrayList<Integer>();

        storeDataInArrays();
        recordingCustomAdapter = new RecordingCustomAdapter(requireContext(),
                                                            recordingDate, recordingTimestamp, recordingId, this);
        recyclerview.setAdapter(recordingCustomAdapter);
        recyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        manageRecordingsButton.setOnClickListener(v -> {
            // Toggle the visibility of buttons in the adapter
            recordingCustomAdapter.toggleManageRecordings();
        });
        return view;
    }

    void storeDataInArrays() {
        Cursor cursor = myDB.readAllData();
        if (cursor.getCount() == 0) {
            Toast.makeText(requireContext(), "No Data ", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                recordingId.add(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                recordingDate.add(cursor.getString(cursor.getColumnIndexOrThrow("recording_date")));
                recordingTimestamp.add(cursor.getString(cursor.getColumnIndexOrThrow("recording_timestamp")));
            }
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(requireContext(), RecordingItem.class);
        intent.putExtra("RECORDING_ID", recordingId.get(position));
        intent.putExtra("DATE", recordingDate.get(position));
        intent.putExtra("TIMESTAMP", recordingTimestamp.get(position));

        startActivity(intent);
    }

    @Override
    public void onDeleteClick(int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to delete this recording and all related data?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    int idToDelete = recordingId.get(position);

                    // Delete from database
                    myDB.deleteRecording(idToDelete);

                    // Remove from the list
                    recordingId.remove(position);
                    recordingDate.remove(position);
                    recordingTimestamp.remove(position);

                    // Notify adapter
                    recordingCustomAdapter.notifyItemRemoved(position);

                    Toast.makeText(requireContext(), "Recording deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void onUpdateClick(int position) {
        // Create an AlertDialog with input fields
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Recording");

        // Set up the input field
        final EditText inputField = new EditText(requireContext());
        inputField.setHint("Enter new recording name");
        builder.setView(inputField);

        // Set existing value (optional)
        inputField.setText(recordingDate.get(position)); // Assuming you're updating the date/name

        // Set up buttons
        builder.setPositiveButton("rename", (dialog, which) -> {
            String updatedValue = inputField.getText().toString();

            // Update in the database
            int idToUpdate = recordingId.get(position);
            myDB.renameRecording(idToUpdate, updatedValue); // Implement this in your DB helper

            // Update in the local list
            recordingDate.set(position, updatedValue);

            // Notify the adapter
            recordingCustomAdapter.notifyItemChanged(position);

            // Show a confirmation message
            Toast.makeText(requireContext(), "Recording updated successfully", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }



}