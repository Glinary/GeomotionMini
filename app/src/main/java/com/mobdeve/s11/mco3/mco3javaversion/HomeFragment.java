package com.mobdeve.s11.mco3.mco3javaversion;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import android.Manifest;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements SensorEventListener, LocationListener {

    Button recordingButton;
    private SensorManager sensorManager;
    private MyDatabaseHelper myDB;
    private long recordingId; // Track the current recording ID
    private boolean isRecording = false; // Flag to track recording state
    private LocationManager locationManager;  // Add LocationManager for GPS
    private Location currentLocation;  // Store the current GPS location
    private NavigationControl navigationControl;

    private AsyncHttpServer httpServer;
    private int serverPort = 8000; // Choose a port


    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupHttpServer();

        // Check for location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Initialize database helper
        myDB = new MyDatabaseHelper(requireContext());

        // Initialize LocationManager for GPS
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        // Uncomment drop when using version used by other
//        myDB.dropTable();
//        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPreferences", MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.clear();  // Clears all data in SharedPreferences
//        editor.apply();

    }

    private void setupHttpServer() {
        httpServer = new AsyncHttpServer();

        httpServer.post("/data", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    String jsonStr = request.getBody().get().toString();
                    Log.d("SensorLogger", "Received: " + jsonStr);

                    JSONObject json = new JSONObject(jsonStr);
                    JSONArray payload = json.getJSONArray("payload");

                    ArrayList<ArrayList<Float>> accelBatch = new ArrayList<>();
                    ArrayList<ArrayList<Float>> gyroBatch = new ArrayList<>();

                    for (int i = 0; i < payload.length(); i++) {
                        JSONObject sensorData = payload.getJSONObject(i);
                        String sensorName = sensorData.getString("name");

                        Object valuesObj = sensorData.get("values");
                        float x = 0, y = 0, z = 0;

                        if (valuesObj instanceof JSONObject) {
                            JSONObject values = (JSONObject) valuesObj;
                            x = (float) values.optDouble("x", 0.0);
                            y = (float) values.optDouble("y", 0.0);
                            z = (float) values.optDouble("z", 0.0);
                        }

                        ArrayList<Float> row = new ArrayList<>();
                        row.add(x);
                        row.add(y);
                        row.add(z);

                        // Only take uncalibrated data
                        if (sensorName.equals("accelerometeruncalibrated")) {
                            accelBatch.add(row);
                        } else if (sensorName.equals("gyroscopeuncalibrated")) {
                            gyroBatch.add(row);
                        }
                    }

                    if (accelBatch.size() >= 10 && isRecording) {
                        Log.d("sensorbatch", "running sensor batch 100");
                        processSensorBatch(accelBatch, gyroBatch);
                    }

                    response.send("success");

                } catch (Exception e) {
                    Log.e("SensorLogger", "Error processing data", e);
                    response.code(500).send("Error: " + e.getMessage());
                }
            }
        });

        httpServer.listen(serverPort);
        Log.i("HTTP Server", "Listening on port " + serverPort);
    }

    private void processSensorBatch(ArrayList<ArrayList<Float>> accelData,
                                    ArrayList<ArrayList<Float>> gyroData) {
        // Call Python for feature extraction
        Python py = Python.getInstance();
        PyObject pyModule = py.getModule("feature_extraction");
        List<Double> features = null;

        try {
            PyObject result = pyModule.callAttr("extract_features", accelData, gyroData);
            features = result.asList().stream()
                    .map(obj -> ((Number) obj.toJava(Double.class)).doubleValue())
                    .collect(Collectors.toList());

            Log.d("Features", "Extracted: " + features.toString());

            // Run ONNX classification
            runOnnxInference(features);
        } catch (Exception e) {
            Log.e("FeatureExtraction", "Error extracting features", e);
        }
    }

    private void runOnnxInference(List<Double> features) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // 1. Load model
                OrtEnvironment env = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
                InputStream modelInput = requireContext().getAssets().open("svm_model.onnx");
                byte[] modelBytes = new byte[modelInput.available()];
                modelInput.read(modelBytes);
                modelInput.close();

                OrtSession session = env.createSession(modelBytes, opts);

                // 2. Prepare input tensor
                float[][] inputData = new float[1][features.size()];

                for (int i = 0; i < features.size(); i++) {
                    inputData[0][i] = features.get(i).floatValue();
                }

                Log.d("FinalFeaturesInput", Arrays.toString(inputData[0]));

                OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);

                // 3. Run inference
                OrtSession.Result output = session.run(Collections.singletonMap("float_input", inputTensor));
                long[] prediction = (long[]) output.get(0).getValue();
                int predictedClass = (int) prediction[0];

                if (currentLocation != null && (predictedClass == 1 || predictedClass == 2 || predictedClass == 3)) {
                    Log.d("DB_ADD_COORDINATE", "Adding coordinate for recordingId=" + recordingId +
                            ", lat=" + currentLocation.getLatitude() + ", lon=" + currentLocation.getLongitude() +
                            ", class=" + predictedClass);
                    myDB.addCoordinate((int) recordingId, currentLocation.getLatitude(),
                            currentLocation.getLongitude(), String.valueOf(predictedClass));
                }

                // 4. Post result back to UI
                requireActivity().runOnUiThread(() -> {
                    Log.d("ONNX", "Predicted class: " + predictedClass);
                });

            } catch (Exception e) {
                Log.e("ONNX", "Error during inference", e);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPreferences", MODE_PRIVATE);

        SwitchCompat accelerometer = view.findViewById(R.id.sw_accel);
        SwitchCompat  gyroscope = view.findViewById(R.id.sw_gyro);
        SwitchCompat  gps = view.findViewById(R.id.sw_gps);

        accelerometer.setChecked(prefs.getBoolean("isAccel", false));
        gyroscope.setChecked(prefs.getBoolean("isGyro", false));
        gps.setChecked(prefs.getBoolean("isGPS", false));


        accelerometer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isAccel", isChecked);
                editor.apply();
            }
        });

        gyroscope.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isGyro", isChecked);
                editor.apply();
            }
        });

        gps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isGPS", isChecked);
                editor.apply();
            }
        });


        recordingButton = view.findViewById(R.id.recordingButton);
        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean isAccActivated = prefs.getBoolean("isAccel", false);
                Boolean isGPSActivated = prefs.getBoolean("isGPS", false);
                Boolean isGyroActivated = prefs.getBoolean("isGyro", false);

                if (!isRecording && isAccActivated && isGPSActivated && isGyroActivated) {
                    startRecording();
                    navigationControl.setAllowNavigation(false); // Disable navigation
                    recordingButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A44661FF")));
                } else if (!isRecording && (!isAccActivated || !isGPSActivated || !isGyroActivated)) {
                    Toast.makeText(requireContext(), "Please enable all sensors to record.", Toast.LENGTH_SHORT).show();
                } else{
                    stopRecording();
                    recordingButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#474983")));
                    navigationControl.setAllowNavigation(true); // Enable navigation
                }
            }
        });

        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Ignored — data will only come from HTTP POST
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void startRecording() {
        // Create a new recording
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String currentDate = formatter1.format(date);
        String currentTimestamp = formatter2.format(date);

        recordingId = myDB.addRecording(currentDate, currentTimestamp); // Get the recording ID
        isRecording = true;
        recordingButton.setText("Stop Recording");

        // Do NOT register phone sensors — only use HTTP POST data

        // Keep GPS if needed
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        isRecording = false;
        recordingButton.setText("Start Recording");

        // No sensor unregister needed, since we never registered

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.currentLocation = location;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationControl) {
            navigationControl = (NavigationControl) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NavigationControl");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationControl = null;
    }
}
