package com.zxyw.sdk.face_sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FaceView extends View { 

    private List<Rect> rect;
    private Paint paint;

    private void initData() {
        paint = new Paint();
        rect = new ArrayList<>();
        paint.setARGB(122, 255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
    }

    public FaceView(Context context) {
        super(context);
        initData();
    }

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public FaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    public void addRect(RectF rect) {
        if (rect == null) return;
        Rect buffer = new Rect();
        buffer.left = (int) rect.left;
        buffer.top = (int) rect.top;
        buffer.right = (int) rect.right;
        buffer.bottom = (int) rect.bottom;
        this.rect.add(buffer);
    }

    public void clear() {
        rect.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < rect.size(); i++) {
            try {
                Rect r = rect.get(i);
                canvas.drawRect(r, paint);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.clear();
    }
}
