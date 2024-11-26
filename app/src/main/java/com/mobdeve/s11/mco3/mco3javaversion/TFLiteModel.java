package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteModel {
    private Interpreter tflite;

    public TFLiteModel(Context context, String modelPath) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            tflite = new Interpreter(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public float[] runInference(float[] inputFeatures, int numWindows) {
        // TensorFlow Lite model expects input as [batch_size, num_features]
        float[][] output = new float[numWindows][1]; // Adjust output size based on your model's output shape

        // Run inference
        tflite.run(inputFeatures, output);

        // Flatten the output
        float[] predictions = new float[numWindows];
        for (int i = 0; i < numWindows; i++) {
            predictions[i] = output[i][0];
        }

        return predictions;
    }
}

