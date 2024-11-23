package com.mobdeve.s11.mco3.mco3javaversion;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mobdeve.s11.mco3.mco3javaversion.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private MyDatabaseHelper myDB;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize the database helper
        myDB = new MyDatabaseHelper(this);

        // Set the default fragment to HomeFragment
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        // Bottom navigation item selection handling
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            // Retrieve the isRecording state from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
            boolean isRecording = prefs.getBoolean("isRecording", false);

            // Prevent navigation when recording is in progress
            if (isRecording) {
                return false;  // Prevent navigation
            }

            if (item.getItemId() == R.id.mn_home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.mn_recordings) {
                replaceFragment(new RecordingsFragment());
            } else if (item.getItemId() == R.id.mn_settings) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainConstraintLayout, fragment);
        fragmentTransaction.commit();
    }
}
