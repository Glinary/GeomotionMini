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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private MyDatabaseHelper myDB;
    private long recordingId; // Track the current recording ID
    private boolean isRecording = false; // Flag to track recording state
    private LocationManager locationManager;  // Add LocationManager for GPS
    private Location currentLocation;  // Store the current GPS location
    private NavigationControl navigationControl;

    private AsyncHttpServer httpServer;
    private final int serverPort = 8000; // Choose a port
    private Location lastInferenceLocation = null;
    private final int BATCH_SIZE_THRESHOLD = 50;
    private final float MIN_DISTANCE_METERS = 0.2f; // adjust threshold as needed (0.2f is 20cm)
    private final int MIN_TIME_MS = 1000; // 1 second minimum between updates
    private static final long MAX_LOCATION_UPDATE_INTERVAL_MS = 5000; // 5 seconds
    private long lastLocationUpdateTime = 0;
    private volatile boolean isHttpActive = false;
    private long lastHttpRequestTime = 0;
    private static final long HTTP_INACTIVITY_THRESHOLD = 3000; // 3 seconds

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

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 1);
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
                    // Mark HTTP as active and update last request time
                    isHttpActive = true;
                    lastHttpRequestTime = System.currentTimeMillis();

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

                        if (sensorName.equals("accelerometeruncalibrated")) {
                            accelBatch.add(row);
                        } else if (sensorName.equals("gyroscopeuncalibrated")) {
                            gyroBatch.add(row);
                        }
                    }

                    if (accelBatch.size() >= BATCH_SIZE_THRESHOLD && isRecording && currentLocation != null) {
                        boolean shouldProcess = false;

                        if (lastInferenceLocation == null) {
                            shouldProcess = true;
                        } else {
                            float distance = currentLocation.distanceTo(lastInferenceLocation);
                            if (distance >= MIN_DISTANCE_METERS) {
                                shouldProcess = true;
                            }
                        }

                        if (shouldProcess) {
                            Log.d("sensorbatch", "running sensor batch 50");
                            processSensorBatch(accelBatch, gyroBatch);
                            lastInferenceLocation = new Location(currentLocation); // update last location
                        }
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

        try {
            PyObject result = pyModule.callAttr("extract_features", accelData, gyroData);
            List<Double> features = result.asList().stream()
                    .map(obj -> ((Number) obj.toJava(Double.class)).doubleValue())
                    .collect(Collectors.toList());

            Log.d("Features", "Extracted: " + features.toString());

            // Only run inference if we got valid features
            if (features != null && !features.isEmpty() && features.size() >= 30) {
                runOnnxInference(features);
            } else {
                Log.w("FeatureExtraction", "Received invalid features - not running inference");
            }
        } catch (Exception e) {
            Log.e("FeatureExtraction", "Error extracting features", e);
            // Provide fallback features in case of error
            List<Double> fallbackFeatures = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                fallbackFeatures.add(0.0);
            }
            runOnnxInference(fallbackFeatures);
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
                    String anomalyLabel = "";

                    switch(predictedClass) {
                        case 0:
                            anomalyLabel = "bump";
                            break;
                        case 1:
                            anomalyLabel = "crack";
                            break;
                        case 2:
                            anomalyLabel = "normal";
                            break;
                        case 3:
                            anomalyLabel = "pothole";
                            break;
                        default:
                            anomalyLabel = "unknown";
                            break;
                    }

                    myDB.addCoordinate((int) recordingId, currentLocation.getLatitude(),
                            currentLocation.getLongitude(), anomalyLabel);

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

        Button checkStatusButton = view.findViewById(R.id.btn_check_status);
        checkStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkHttpServerStatus();
            }
        });

        recordingButton = view.findViewById(R.id.recordingButton);
        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    startRecording();
                    navigationControl.setAllowNavigation(false); // Disable navigation
                    recordingButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A44661FF")));
                } else if (!isRecording) {
                    Toast.makeText(requireContext(), "Please enable location and sensor connection to record.", Toast.LENGTH_SHORT).show();
                } else{
                    stopRecording();
                    recordingButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#474983")));
                    navigationControl.setAllowNavigation(true); // Enable navigation
                }
            }
        });

        return view;
    }

    private void checkHttpServerStatus() {
        long currentTime = System.currentTimeMillis();

        // Check if we've received a request recently
        boolean currentlyReceivingData = isHttpActive &&
                (currentTime - lastHttpRequestTime < HTTP_INACTIVITY_THRESHOLD);

        String statusMessage;
        if (httpServer == null) {
            statusMessage = "HTTP Server is not running";
        } else if (currentlyReceivingData) {
            statusMessage = "HTTP Server active - Receiving data!";
        } else if (isHttpActive) {
            statusMessage = "HTTP Server running (last data " +
                    ((currentTime - lastHttpRequestTime)/1000) + "s ago)";
        } else {
            statusMessage = "SensorLogger Status: No data received yet";
        }

        Toast.makeText(requireContext(), statusMessage, Toast.LENGTH_LONG).show();

        // Reset the active flag for next check
        isHttpActive = false;
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

        // Keep GPS if needed
        if (locationManager != null) {
            try {
                boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (gpsEnabled) {
                    // Try GPS first
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_METERS, this
                    );
                    Log.d("Location", "Using GPS provider");
                }

                if (!gpsEnabled && networkEnabled) {
                    // Fall back to network if GPS is not available
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_METERS, this
                    );
                    Log.d("Location", "Using network provider as fallback");
                }

                if (!gpsEnabled && !networkEnabled) {
                    Toast.makeText(requireContext(), "No location providers available", Toast.LENGTH_SHORT).show();
                }

                // Also request a single update to get the last known location immediately
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = null;
                    if (gpsEnabled) {
                        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    if (lastKnownLocation == null && networkEnabled) {
                        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (lastKnownLocation != null) {
                        onLocationChanged(lastKnownLocation);
                    }
                }

            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
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
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLocationUpdateTime > MAX_LOCATION_UPDATE_INTERVAL_MS) {
            this.currentLocation = location;
            lastLocationUpdateTime = currentTime;
        }
    }



    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d("Location", "Provider enabled: " + provider);
    }
    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d("Location", "Provider disabled: " + provider);
        if (isRecording && provider.equals(LocationManager.GPS_PROVIDER)) {
            // If GPS was disabled while recording, try to switch to network provider
            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_METERS, this
                    );
                    Toast.makeText(requireContext(), "Switched to network location provider", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Location", "Status changed: " + provider + " status: " + status);
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
