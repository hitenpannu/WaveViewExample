package com.hitenderpannu.audioview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.Stack;

public class AudioPlot extends View {

  private int width;
  private float centerY;
  private Paint strokePaint;
  private Paint historyStrokePaint;
  private Path path;
  private Path historyPath;
  private float plotWidth = 30f;
  private int historyPathAlpha = 60;
  private int plotColor = Color.BLACK;

  public AudioPlot(Context context) {
    super(context);
    init();
  }

  public AudioPlot(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray array =
        context.getTheme().obtainStyledAttributes(attrs, R.styleable.audioPlotView, 0, 0);
    initializeFromAttributes(array);
    init();
  }

  public AudioPlot(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    TypedArray array =
        context.getTheme().obtainStyledAttributes(attrs, R.styleable.audioPlotView, defStyle, 0);
    initializeFromAttributes(array);
    init();
  }

  private void initializeFromAttributes(TypedArray array) {
    try {
      plotWidth = array.getFloat(R.styleable.audioPlotView_plotWidth, 30f);
      historyPathAlpha = array.getInt(R.styleable.audioPlotView_alphaForHistoryPath, 60);
      plotColor = array.getColor(R.styleable.audioPlotView_plotColor, Color.BLACK);
    } catch (Exception exception) {
      Log.e("EXCEPTION", exception.getMessage());
    } finally {
      array.recycle();
    }
  }

  private void init() {
    strokePaint = new Paint();
    strokePaint.setColor(plotColor);
    strokePaint.setStyle(Paint.Style.FILL);
    strokePaint.setStrokeCap(Paint.Cap.ROUND);
    strokePaint.setStrokeJoin(Paint.Join.MITER);
    strokePaint.setStrokeWidth(1f);
    strokePaint.setAntiAlias(true);

    historyStrokePaint = new Paint();
    historyStrokePaint.setColor(plotColor);
    historyStrokePaint.setAlpha(historyPathAlpha);
    historyStrokePaint.setStyle(Paint.Style.FILL);
    historyStrokePaint.setStrokeCap(Paint.Cap.ROUND);
    historyStrokePaint.setStrokeJoin(Paint.Join.MITER);
    historyStrokePaint.setStrokeWidth(1f);
    historyStrokePaint.setAntiAlias(true);
  }

  @Override protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
    super.onSizeChanged(w, h, oldWidth, oldHeight);

    width = getMeasuredWidth();
    centerY = getMeasuredHeight() / 2f;
  }

  @Override public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (path != null) {
      canvas.drawPath(path, strokePaint);
    }
    if (historyPath != null) {
      canvas.drawPath(historyPath, historyStrokePaint);
    }
  }

  public void setSamples(short[] buffer) {
    if (buffer == null) return;
    float max = Short.MAX_VALUE;
    Path tempPath = new Path();
    tempPath.moveTo(0, centerY);
    boolean phase = false;
    int i = 0;
    int segmentCount = 48;
    Stack<Point> points = new Stack<>();
    while (i < segmentCount) {
      int start = (width / segmentCount) * (i);
      int end = (width / segmentCount) * (++i);
      for (int x = start; x < end; x++) {
        int index = (int) (((float) x / width) * buffer.length);
        if (index < buffer.length) {
          short sample = buffer[index];
          float y = centerY - ((sample / max) * centerY);
          if (phase) {
            tempPath.lineTo(x, y);
            points.add(new Point(x, y));
          }
          phase = !phase;
        }
      }
    }

    tempPath.lineTo(width, centerY);
    tempPath.lineTo(width, centerY + plotWidth);

    while (!points.isEmpty()) {
      Point point = points.pop();
      tempPath.lineTo(point.x, point.y + plotWidth);
    }
    tempPath.lineTo(0, centerY);
    historyPath = path;
    path = tempPath;

    postInvalidate();
  }

  class Point {
    int x;
    float y;

    Point(int x, float y) {
      this.x = x;
      this.y = y;
    }
  }
}
