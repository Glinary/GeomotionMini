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

import com.mobdeve.s11.mco3.mco3javaversion.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnRecordingListener {

    ActivityMainBinding binding;
    private MyDatabaseHelper myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
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

    @Override
    public void onRecordingStateChanged(boolean isRecording) {
        if (isRecording) {
            binding.bottomNavigationView.setEnabled(false); // Disable BottomNavigationView
        } else {
            binding.bottomNavigationView.setEnabled(true); // Enable BottomNavigationView
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainConstraintLayout, fragment);
        fragmentTransaction.commit();
    }
}