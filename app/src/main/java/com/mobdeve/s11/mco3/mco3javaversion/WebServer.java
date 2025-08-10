package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.util.Log;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class WebServer {
    private AsyncHttpServer server;
    private Context context;
    private DataListener dataListener;
    private static final int PORT = 8080;

    public interface DataListener {
        void onSensorDataReceived(ArrayList<ArrayList<Float>> accelData, ArrayList<ArrayList<Float>> gyroData);
    }

    public WebServer(Context context, DataListener listener) {
        this.context = context;
        this.dataListener = listener;
        this.server = new AsyncHttpServer();
    }

    public void start() {
        server.post("/sensor", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    JSONObject json = new JSONObject(request.getBody().get().toString());
                    processSensorData(json);
                    response.send("OK");
                } catch (Exception e) {
                    Log.e("WebServer", "Error processing request", e);
                    response.code(500).send("Error");
                }
            }
        });
        server.listen(PORT);
        Log.d("WebServer", "Server started on port " + PORT);
    }

    public void stop() {
        server.stop();
    }

    private void processSensorData(JSONObject json) throws JSONException {
        ArrayList<ArrayList<Float>> accelData = new ArrayList<>();
        ArrayList<ArrayList<Float>> gyroData = new ArrayList<>();

        if (json.has("accelerometer")) {
            JSONArray accelArray = json.getJSONArray("accelerometer");
            for (int i = 0; i < accelArray.length(); i++) {
                JSONArray point = accelArray.getJSONArray(i);
                ArrayList<Float> values = new ArrayList<>();
                values.add((float) point.getDouble(0)); // x
                values.add((float) point.getDouble(1)); // y
                values.add((float) point.getDouble(2)); // z
                accelData.add(values);
            }
        }

        if (json.has("gyroscope")) {
            JSONArray gyroArray = json.getJSONArray("gyroscope");
            for (int i = 0; i < gyroArray.length(); i++) {
                JSONArray point = gyroArray.getJSONArray(i);
                ArrayList<Float> values = new ArrayList<>();
                values.add((float) point.getDouble(0)); // x
                values.add((float) point.getDouble(1)); // y
                values.add((float) point.getDouble(2)); // z
                gyroData.add(values);
            }
        }

        if (dataListener != null && !accelData.isEmpty() && !gyroData.isEmpty()) {
            dataListener.onSensorDataReceived(accelData, gyroData);
        }
    }
}