package com.phc.mnistdroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DrawingView extends View {

    private final List<Path> paths = new ArrayList<>();
    private final List<Paint> paints = new ArrayList<>();

    private Path currentPath;
    private Paint currentPaint;

    private final Random random = new Random();
    private boolean useRandomColors = false;
    private int userSelectedColor = Color.BLACK;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);

        // default pen settings
        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);
        currentPaint.setDither(true);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(22f);
        currentPaint.setColor(Color.BLACK);
    }

    public void setPen(Paint paint) {
        if (paint == null) return;

        // clone so outside changes won't break our stored strokes
        Paint p = new Paint(paint);
        p.setAntiAlias(true);
        p.setDither(true);
        if (p.getStyle() == null) p.setStyle(Paint.Style.STROKE);
        if (p.getStrokeJoin() == null) p.setStrokeJoin(Paint.Join.ROUND);
        if (p.getStrokeCap() == null) p.setStrokeCap(Paint.Cap.ROUND);

        currentPaint = p;
    }

    // Set the drawing color (disables random colors)
    public void setColor(int color) {
        userSelectedColor = color;
        useRandomColors = false;
        currentPaint.setColor(color);
    }

    // Enable random colors per stroke
    public void enableRandomColors(boolean enable) {
        useRandomColors = enable;
    }

    // Clear everything
    public void clear() {
        paths.clear();
        paints.clear();
        currentPath = null;
        invalidate();
    }

    // Undo last stroke
    public void undo() {
        if (!paths.isEmpty()) {
            paths.remove(paths.size() - 1);
            paints.remove(paints.size() - 1);
            invalidate();
        }
    }

    // Export to bitmap for classifier (28x28)
    public Bitmap exportToBitmap(int outWidth, int outHeight) {
        int w = Math.max(getWidth(), 1);
        int h = Math.max(getHeight(), 1);

        Bitmap full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(full);

        // force white background
        c.drawColor(Color.WHITE);

        // draw stored strokes
        for (int i = 0; i < paths.size(); i++) {
            c.drawPath(paths.get(i), paints.get(i));
        }
        // draw current stroke if in progress
        if (currentPath != null) {
            c.drawPath(currentPath, currentPaint);
        }

        return Bitmap.createScaledBitmap(full, outWidth, outHeight, true);
    }

    private int randomStrokeColor() {
        // bright-ish random colors (avoid white)
        int r = 80 + random.nextInt(176); // 80..255
        int g = 80 + random.nextInt(176);
        int b = 80 + random.nextInt(176);
        return Color.rgb(r, g, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        if (currentPath != null) {
            canvas.drawPath(currentPath, currentPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);

                // give each stroke its own paint
                currentPaint = new Paint(currentPaint);

                // Use user-selected color or random color
                if (useRandomColors) {
                    currentPaint.setColor(randomStrokeColor());
                } else {
                    currentPaint.setColor(userSelectedColor);
                }

                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    // save finished stroke
                    paths.add(currentPath);
                    paints.add(new Paint(currentPaint));
                    currentPath = null;
                    invalidate();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }
}