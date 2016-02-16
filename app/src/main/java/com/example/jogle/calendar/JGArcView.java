package com.example.jogle.calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by jogle on 15/7/21.
 */

public class JGArcView extends ImageView {
    private int percent;

    public JGArcView(Context context) {
        super(context);
    }

    public JGArcView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public JGArcView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public void setPercent(int percent) {
        this.percent = percent;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 这里 get 回来的宽度和高度是当前控件相对应的宽度和高度（在 xml 设置）
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Paint backCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backCirclePaint.setColor(0xff438ac8);
        backCirclePaint.setAntiAlias(true);
        backCirclePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle((float)(getWidth() * 0.5), (float) (getHeight() * 0.5), (float) (getHeight() * 0.5), backCirclePaint);

        // 画圆弧
        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(0xffffce3a);
        arcPaint.setAntiAlias(true);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(getHeight() / 24);

        RectF rect = new RectF();
        rect.left = (float) (getHeight() * 0.05);
        rect.top = (float) (getHeight() * 0.05);
        rect.bottom = (float) (getHeight() * 0.95);
        rect.right = (float) (getWidth() * 0.95);
        canvas.drawArc(rect, -90, (float) (- percent * 3.6), false, arcPaint);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(0xffffce3a);
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle((float)(getWidth() * 0.5), (float) (getHeight() * 0.05), getHeight() / 24, circlePaint);
    }

}