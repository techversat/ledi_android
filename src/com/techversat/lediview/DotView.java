package com.techversat.lediview;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.techversat.lediview.Dot;
import com.techversat.lediview.DotMatrix;

public class DotView extends View {

    private volatile DotMatrix dots;
    // private int width, height;

    /**
     * @param context the rest of the application
     */
    public DotView(Context context) {
        super(context);
        setFocusableInTouchMode(true);
    }

    /**
     * @param context
     * @param attrs
     */
    public DotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusableInTouchMode(true);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public DotView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusableInTouchMode(true);
    }

    /**
     * @param dots
     */
    public void setDots(DotMatrix dots) { this.dots = dots; }

    /**
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override protected void onDraw(Canvas canvas) {
        //canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setStyle(Style.STROKE);
        paint.setShadowLayer(5, 5, 5, Color.DKGRAY);
        paint.setColor(hasFocus() ? Color.BLACK : Color.GRAY);
        // RectF rect = new RectF(10, 10, getWidth()-10, getHeight()-10);
        RectF rect = new RectF(10, 10, getWidth()-10, getHeight()-10);
        // canvas.drawRect(0, 0, getWidth() - 1, getHeight() -1, paint);
        canvas.drawRoundRect(rect, 15, 15, paint);
        
        if (null == dots) { return; }
        
        // if the dimension changed, it will reinitialize the dot objects
        dots.setDimension(getWidth()-10, getHeight()-10);
        
        Paint dotpaint = new Paint();
        dotpaint.setStyle(Style.FILL);

       
        for (Dot dot : dots.getDots()) {
            dotpaint.setColor(dot.getColor());
            canvas.drawCircle(
                dot.getX(),
                dot.getY(),
                dot.getDiameter(),
                dotpaint);
        }
    }
    
    /*
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
    	width = w;
    	height = h;
    }
    */
    
}
