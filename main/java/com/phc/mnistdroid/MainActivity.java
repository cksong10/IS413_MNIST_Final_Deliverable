package com.phc.mnistdroid;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private DigitClassifier classifier;

    private LinearLayout topPredictionBox, prediction2Box, prediction3Box, prediction4Box;
    private TextView tvTopDigit, tvTopConfidence;
    private TextView tvDigit2, tvDigit3, tvDigit4;
    private TextView tvConfidence2, tvConfidence3, tvConfidence4;

    private View colorBlack, colorRed, colorBlue, colorGreen, colorPurple, colorOrange;
    private View selectedColorView;
    private int currentColor = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = findViewById(R.id.drawingView);
        Button btnClassify = findViewById(R.id.btnClassify);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnUndo = findViewById(R.id.btnUndo);

        topPredictionBox = findViewById(R.id.topPredictionBox);
        prediction2Box = findViewById(R.id.prediction2Box);
        prediction3Box = findViewById(R.id.prediction3Box);
        prediction4Box = findViewById(R.id.prediction4Box);

        tvTopDigit = findViewById(R.id.tvTopDigit);
        tvTopConfidence = findViewById(R.id.tvTopConfidence);

        tvDigit2 = findViewById(R.id.tvDigit2);
        tvDigit3 = findViewById(R.id.tvDigit3);
        tvDigit4 = findViewById(R.id.tvDigit4);

        tvConfidence2 = findViewById(R.id.tvConfidence2);
        tvConfidence3 = findViewById(R.id.tvConfidence3);
        tvConfidence4 = findViewById(R.id.tvConfidence4);

        // Color picker views
        colorBlack = findViewById(R.id.colorBlack);
        colorRed = findViewById(R.id.colorRed);
        colorBlue = findViewById(R.id.colorBlue);
        colorGreen = findViewById(R.id.colorGreen);
        colorPurple = findViewById(R.id.colorPurple);
        colorOrange = findViewById(R.id.colorOrange);

        // Set up color picker listeners
        setupColorPicker();

        // Default selected color is black
        selectedColorView = colorBlack;
        highlightSelectedColor();

        // Pen thickness
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(22);
        paint.setColor(currentColor);
        drawingView.setPen(paint);

        try {
            classifier = new DigitClassifier(this);
        } catch (IOException e) {
            Toast.makeText(this, "Model failed to load.", Toast.LENGTH_LONG).show();
            classifier = null;
        }

        btnClassify.setOnClickListener(v -> classifyDrawing());

        btnClear.setOnClickListener(v -> {
            drawingView.clear();
            clearResults();
        });

        btnUndo.setOnClickListener(v -> {
            drawingView.undo();
        });
    }

    private void setupColorPicker() {
        colorBlack.setOnClickListener(v -> selectColor(colorBlack, Color.BLACK));
        colorRed.setOnClickListener(v -> selectColor(colorRed, Color.rgb(255, 0, 0)));
        colorBlue.setOnClickListener(v -> selectColor(colorBlue, Color.rgb(0, 0, 255)));
        colorGreen.setOnClickListener(v -> selectColor(colorGreen, Color.rgb(0, 170, 0)));
        colorPurple.setOnClickListener(v -> selectColor(colorPurple, Color.rgb(170, 0, 170)));
        colorOrange.setOnClickListener(v -> selectColor(colorOrange, Color.rgb(255, 136, 0)));
    }

    private void selectColor(View colorView, int color) {
        selectedColorView = colorView;
        currentColor = color;

        // Update the drawing view pen color
        drawingView.setColor(color);

        // Highlight the selected color
        highlightSelectedColor();
    }

    private void highlightSelectedColor() {
        // Reset all color views
        colorBlack.setAlpha(0.5f);
        colorRed.setAlpha(0.5f);
        colorBlue.setAlpha(0.5f);
        colorGreen.setAlpha(0.5f);
        colorPurple.setAlpha(0.5f);
        colorOrange.setAlpha(0.5f);

        // Highlight selected color
        if (selectedColorView != null) {
            selectedColorView.setAlpha(1.0f);
            selectedColorView.setScaleX(1.2f);
            selectedColorView.setScaleY(1.2f);
        }

        // Reset scale for non-selected
        View[] allColors = {colorBlack, colorRed, colorBlue, colorGreen, colorPurple, colorOrange};
        for (View v : allColors) {
            if (v != selectedColorView) {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            }
        }
    }

    private void classifyDrawing() {
        if (classifier == null) {
            Toast.makeText(this, "Classifier not ready.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = drawingView.exportToBitmap(28, 28);
        List<DigitClassifier.Prediction> results = classifier.classify(bitmap);

        if (results == null || results.isEmpty()) {
            Toast.makeText(this, "No predictions.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show top prediction
        if (results.size() > 0) {
            DigitClassifier.Prediction top = results.get(0);
            tvTopDigit.setText(String.valueOf(top.digit));
            tvTopConfidence.setText(Math.round(top.confidence * 100) + "%");
            setBoxColor(topPredictionBox, top.confidence);
        }

        // Show other predictions
        showSmallResult(results, 1, tvDigit2, tvConfidence2, prediction2Box);
        showSmallResult(results, 2, tvDigit3, tvConfidence3, prediction3Box);
        showSmallResult(results, 3, tvDigit4, tvConfidence4, prediction4Box);
    }

    private void showSmallResult(List<DigitClassifier.Prediction> results, int index,
                                 TextView tvDigit, TextView tvConfidence, LinearLayout box) {
        if (index < results.size()) {
            DigitClassifier.Prediction p = results.get(index);
            tvDigit.setText(String.valueOf(p.digit));
            tvConfidence.setText(Math.round(p.confidence * 100) + "%");
            setBoxColor(box, p.confidence);
        } else {
            tvDigit.setText("-");
            tvConfidence.setText("-%");
            setBoxColor(box, 0);
        }
    }

    private void setBoxColor(View box, float confidence) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(12);

        int color;
        if (confidence >= 0.7f) {
            // High confidence - Green
            color = Color.rgb(200, 255, 200);
        } else if (confidence >= 0.4f) {
            // Medium confidence - Yellow/Orange
            color = Color.rgb(255, 240, 200);
        } else if (confidence > 0) {
            // Low confidence - Light Red
            color = Color.rgb(255, 220, 220);
        } else {
            // No prediction - Gray
            color = Color.rgb(245, 245, 245);
        }

        drawable.setColor(color);
        box.setBackground(drawable);
    }

    private void clearResults() {
        tvTopDigit.setText("-");
        tvTopConfidence.setText("Confidence: -");
        tvDigit2.setText("-");
        tvDigit3.setText("-");
        tvDigit4.setText("-");
        tvConfidence2.setText("-%");
        tvConfidence3.setText("-%");
        tvConfidence4.setText("-%");

        // Reset all boxes to gray
        setBoxColor(topPredictionBox, 0);
        setBoxColor(prediction2Box, 0);
        setBoxColor(prediction3Box, 0);
        setBoxColor(prediction4Box, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) classifier.close();
    }
}