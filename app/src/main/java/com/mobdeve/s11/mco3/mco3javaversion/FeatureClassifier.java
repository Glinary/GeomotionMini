package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.util.Log;

import com.mobdeve.s11.mco3.mco3javaversion.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class FeatureClassifier {

    private Model model;

    // Constructor to initialize the classifier with a context
    public FeatureClassifier(Context context) {
        try {
            // Load the model once when the object is created
            this.model = Model.newInstance(context);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FeatureClassifier", "Error while loading the model: " + e.getMessage());
        }
    }

    // Method to classify features based on the input mean, variance, and stdDev
    public String classify(float mean, float variance, float stdDev) {
        if (model == null) {
            Log.e("FeatureClassifier", "Model is not initialized.");
            return null;
        }

        // Prepare input features in ByteBuffer format
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(3 * 4); // 3 features * 4 bytes (FLOAT32)
        byteBuffer.order(ByteOrder.nativeOrder()); // Ensure correct byte order

        // Add feature values to ByteBuffer
        byteBuffer.putFloat(mean);
        byteBuffer.putFloat(variance);
        byteBuffer.putFloat(stdDev);

        // Create TensorBuffer and load the ByteBuffer
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3}, DataType.FLOAT32);
        inputFeature0.loadBuffer(byteBuffer);

        // Run model inference
        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        // Retrieve predictions
        float[] predictedValues = outputFeature0.getFloatArray();

        String outputClass = predictedValues[0] > predictedValues[1] ? "Bump" : "Normal";

        // Log the predictions for debugging
        Log.d("FeatureClassifier", "Model output: " + Arrays.toString(predictedValues));
        Log.d("FeatureClassifier", "Prediction: " + outputClass);

        // Return the classification result
        return outputClass;

    }

    // Release model resources when no longer needed
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}