package com.mobdeve.s11.mco3.mco3javaversion;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.mobdeve.s11.mco3.mco3javaversion.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity implements NavigationControl {

    ActivityMainBinding binding;
    private boolean allowNavigation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        replaceFragment(new HomeFragment());


        // Initialize Python
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize Python: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }



        binding.bottomNavigationView.setOnItemSelectedListener(item -> {

            if (!allowNavigation) { // Check if navigation is disabled
                Toast.makeText(this, "Navigation is disabled during recording.", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (item.getItemId() == R.id.mn_home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.mn_recordings) {
                replaceFragment(new RecordingsFragment());
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

    @Override
    public void setAllowNavigation(boolean allow) {
        this.allowNavigation = allow;
    }
}