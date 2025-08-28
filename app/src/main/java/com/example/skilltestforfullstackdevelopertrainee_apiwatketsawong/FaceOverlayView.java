package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FaceOverlayView extends View {
    private Paint backgroundPaint;
    private Paint clearPaint;
    private RectF ovalRect;
    private RectF faceRect;

    public FaceOverlayView(Context context) { super(context); init(); }
    public FaceOverlayView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#B3FFFFFF")); // ขาวโปร่ง
        backgroundPaint.setStyle(Paint.Style.FILL);

        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

//    public void setFaceRect(RectF rect) {
//        this.faceRect = rect;
//        invalidate();
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // วงรีกลางหน้าจอ
        float ovalWidth = width * 0.7f;
        float ovalHeight = height * 0.4f;
        float left = (width - ovalWidth) / 2f;
        float top = (height - ovalHeight) / 2f;
        float right = left + ovalWidth;
        float bottom = top + ovalHeight;

        ovalRect = new RectF(left, top, right, bottom);

        // วาดพื้นหลังขาวโปร่ง
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // เจาะวงรีให้โปร่งใส
        canvas.drawOval(ovalRect, clearPaint);

        // วาดกรอบวงรี
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(Color.parseColor("#FF6200EE"));
        canvas.drawOval(ovalRect, borderPaint);

        // ถ้ามี faceRect จาก detect → วาดสี่เหลี่ยมเพิ่ม (debug)
        if (faceRect != null) {
            Paint facePaint = new Paint();
            facePaint.setStyle(Paint.Style.STROKE);
            facePaint.setStrokeWidth(4f);
            facePaint.setColor(Color.RED);
            canvas.drawRect(faceRect, facePaint);
        }
    }
}