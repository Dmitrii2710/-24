package com.system.monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayDeque;
import java.util.Deque;

public class LiveGraphView extends View {

    private static final int MAX_POINTS = 60;

    private final Deque<Float> values = new ArrayDeque<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private int lineColor = Color.parseColor("#4FC3F7");
    private int fillColor1 = Color.parseColor("#554FC3F7");
    private int fillColor2 = Color.parseColor("#004FC3F7");

    public LiveGraphView(Context context) {
        super(context);
        init();
    }

    public LiveGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setColor(lineColor);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f);
        gridPaint.setColor(Color.parseColor("#1AFFFFFF"));
    }

    public void setColor(int color) {
        lineColor = color;
        String hex = String.format("%06X", (0xFFFFFF & color));
        fillColor1 = Color.parseColor("#55" + hex);
        fillColor2 = Color.parseColor("#00" + hex);
        linePaint.setColor(lineColor);
        invalidate();
    }

    public void addValue(float value) {
        if (values.size() >= MAX_POINTS) values.pollFirst();
        values.addLast(Math.max(0, Math.min(100, value)));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0 || values.isEmpty()) return;

        // Grid lines at 25%, 50%, 75%
        for (float pct : new float[]{0.25f, 0.5f, 0.75f}) {
            float y = h - h * pct;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        float[] vals = new float[values.size()];
        int i = 0;
        for (Float v : values) vals[i++] = v;

        float step = (float) w / (MAX_POINTS - 1);

        linePath.reset();
        fillPath.reset();

        float startX = (MAX_POINTS - vals.length) * step;

        fillPath.moveTo(startX, h);

        for (int j = 0; j < vals.length; j++) {
            float x = startX + j * step;
            float y = h - (vals[j] / 100f) * h;
            if (j == 0) {
                linePath.moveTo(x, y);
                fillPath.lineTo(x, y);
            } else {
                // smooth curve
                float prevX = startX + (j - 1) * step;
                float prevY = h - (vals[j - 1] / 100f) * h;
                float cx = (prevX + x) / 2f;
                linePath.cubicTo(cx, prevY, cx, y, x, y);
                fillPath.cubicTo(cx, prevY, cx, y, x, y);
            }
        }

        float lastX = startX + (vals.length - 1) * step;
        fillPath.lineTo(lastX, h);
        fillPath.close();

        // Gradient fill
        LinearGradient gradient = new LinearGradient(0, 0, 0, h, fillColor1, fillColor2, Shader.TileMode.CLAMP);
        fillPaint.setShader(gradient);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}