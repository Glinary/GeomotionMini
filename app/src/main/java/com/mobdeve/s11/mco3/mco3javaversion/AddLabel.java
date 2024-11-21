package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddLabel extends AppCompatActivity {
    MyDatabaseHelper myDB;
    EditText editTextAnomaly;
    Button addLabelItemButton, addActivityLabelBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_label);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        myDB = new MyDatabaseHelper(this);
        editTextAnomaly = findViewById(R.id.editTextAnomaly);
        addLabelItemButton = findViewById(R.id.addLabelButton);
        addActivityLabelBack = findViewById(R.id.addActivityLabelBack);

        addLabelItemButton.setOnClickListener(v -> {
            String anomalyName = editTextAnomaly.getText().toString().trim();

            if (!anomalyName.isEmpty()) {
                // Add anomaly to database
                myDB.addAllowedAnomaly(anomalyName);
                editTextAnomaly.setText(""); // Clear input field
            } else {
                Toast.makeText(this, "Anomaly name cannot be empty", Toast.LENGTH_SHORT).show();
            }

        });

        addActivityLabelBack.setOnClickListener(v -> {
            finish();
        });

    }
}