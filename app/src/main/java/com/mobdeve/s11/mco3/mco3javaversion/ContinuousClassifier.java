package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;

import java.util.LinkedList;
import java.util.Queue;

public class ContinuousClassifier {
    private static final int WINDOW_SIZE = 150;
    private Queue<Float> slidingWindow = new LinkedList<>();
    private double[] features = new double[3]; // [mean, variance, stdDev]
    private Context context;
    private FeatureClassifier featureClassifier;

    public ContinuousClassifier(Context context) {
        this.context = context;
        featureClassifier = new FeatureClassifier(this.context);
    }

    // Process new data points as they arrive
    public void processNewData(float newData) {
        // Add new data to the sliding window
        slidingWindow.add(newData);
        if (slidingWindow.size() > WINDOW_SIZE) {
            slidingWindow.poll(); // Remove the oldest data point
        }

        // Process only when the window is full
        if (slidingWindow.size() == WINDOW_SIZE) {
            computeFeatures();
            classify(features); // Classify using computed features
        }
    }

    // Compute features for the current window
    public void computeFeatures() {
        double sum = 0.0;
        double sumOfSquares = 0.0;

        for (float value : slidingWindow) {
            sum += value;
            sumOfSquares += Math.pow(value, 2);
        }

        double mean = sum / WINDOW_SIZE;
        double variance = (sumOfSquares / WINDOW_SIZE) - Math.pow(mean, 2);
        double stdDev = Math.sqrt(variance);

        features[0] = mean;
        features[1] = stdDev;
        features[2] = variance;
    }

    // Classify the current features
    private void classify(double[] features) {
        System.out.printf("Classifying with features - Mean: %.3f, Variance: %.3f, StdDev: %.3f%n",
                features[0], features[1], features[2]);

        // Pass the features to your classifier
        featureClassifier.classify((float) features[0], (float) features[1], (float) features[2]);
    }
}

