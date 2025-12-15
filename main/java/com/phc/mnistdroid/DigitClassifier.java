package com.phc.mnistdroid;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DigitClassifier {

    private static final String TAG = "DigitClassifier";

    private static final String MODEL_NAME = "mnist.tflite";
    private static final int IMAGE_WIDTH = 28;
    private static final int IMAGE_HEIGHT = 28;
    private static final int NUM_CLASSES = 10;

    private final Interpreter interpreter;
    private final ByteBuffer inputBuffer;

    public static class Prediction {
        public final int digit;
        public final float confidence; // 0.0–1.0

        public Prediction(int digit, float confidence) {
            this.digit = digit;
            this.confidence = confidence;
        }
    }

    public DigitClassifier(Context context) throws IOException {
        // 1. Load model file into MappedByteBuffer (as required by the slides)
        MappedByteBuffer model = loadModelFile(context);

        // 2. Create the Interpreter
        interpreter = new Interpreter(model);

        // 3. Pre-allocate input buffer for 28x28 floats
        inputBuffer = ByteBuffer.allocateDirect(4 * IMAGE_WIDTH * IMAGE_HEIGHT);
        inputBuffer.order(ByteOrder.nativeOrder());
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_NAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Classify a Bitmap drawn by the user. This method:
     *  - resizes to 28x28
     *  - converts pixels to floats (0 or 1)
     *  - runs the TFLite model
     *  - returns the TOP 4 predictions sorted by confidence
     */
    public List<Prediction> classify(Bitmap originalBitmap) {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter is not initialized.");
            return new ArrayList<>();
        }

        // 1. Resize the bitmap to 28x28
        Bitmap bitmap = Bitmap.createScaledBitmap(
                originalBitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true);

        // 2. Get pixels from Bitmap
        int[] pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        bitmap.getPixels(pixels, 0, IMAGE_WIDTH,
                0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 3. Fill inputBuffer with normalized pixel values (0 or 1)
        inputBuffer.rewind();
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];

            // From the MNIST processing slides:
            // if pixel == -1 (0xFFFFFFFF) → background → 0
            // else → stroke → 1
            float value = (color == -1) ? 0.0f : 1.0f;

            inputBuffer.putFloat(value);
        }

        // 4. Run inference
        float[][] output = new float[1][NUM_CLASSES];
        interpreter.run(inputBuffer, output);

        // 5. Build a list of predictions from the output
        float[] probabilities = output[0];
        List<Prediction> predictions = new ArrayList<>();

        for (int i = 0; i < NUM_CLASSES; i++) {
            predictions.add(new Prediction(i, probabilities[i]));
        }

        // 6. Sort predictions by confidence descending
        Collections.sort(predictions, new Comparator<Prediction>() {
            @Override
            public int compare(Prediction p1, Prediction p2) {
                // larger confidence first
                return Float.compare(p2.confidence, p1.confidence);
            }
        });

        // 7. Return only the top 4 predictions
        int maxResults = 4;
        if (predictions.size() > maxResults) {
            return new ArrayList<>(predictions.subList(0, maxResults));
        } else {
            return predictions;
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
