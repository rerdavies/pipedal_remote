package com.twoplay.pipedal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;


public class ScrimLayout extends FrameLayout   {

    public enum ScrimStyle {
        None,
        PiPedalRemote,
        WebView
    }

    private ScrimStyle scrimStyle;

    public void setScrimStyle(ScrimStyle scrimStyle) {
        if (this.scrimStyle != scrimStyle) {
            this.scrimStyle = scrimStyle;
        }
    }


    public ScrimLayout(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public ScrimLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public ScrimLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

//    @Override
//    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
//        return insets;
//    }

    public ScrimLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWillNotDraw(false);
    }


    Paint  statusBarPaint = new Paint();
    Paint navBarPaint = new Paint();
    Paint testPaint = new Paint();
    public void setStatusBarColor(int color) {

        statusBarPaint.setColor(color);
        statusBarPaint.setStyle(Paint.Style.FILL);
        testPaint.setColor(0xFFFF8080);
        testPaint.setStyle(Paint.Style.FILL);
        this.invalidate();
    }
    public void setNavigationBarColorXX(int color) {

        navBarPaint.setColor(color);
        navBarPaint.setStyle(Paint.Style.FILL);
        this.invalidate();
    }

    Insets statusBarInsets;
    Insets navBarInsets;
    Insets displayCutoutInsets;
    public void setScrims(@NonNull WindowInsetsCompat insets) {
        this.statusBarInsets  = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        this.navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
        this.displayCutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
        this.invalidate();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        //super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();


        // prevents occasional flashes during screen rotation
        canvas.drawRect(0,0,width,height,statusBarPaint);
        if (statusBarInsets != null && navBarInsets != null)
        {

            canvas.drawRect(0,0, width, statusBarInsets.top, statusBarPaint);

            if (navBarInsets.bottom > 0) {
                canvas.drawRect(
                        0,
                        height - navBarInsets.bottom,
                        width,
                        height,
                        navBarPaint
                );
            }
            if (navBarInsets.left > 0) {
                canvas.drawRect(
                        0,
                        0,
                        navBarInsets.left,
                        height,
                        navBarPaint
                );
            }
            if (navBarInsets.right > 0) {
                canvas.drawRect(
                        width-navBarInsets.right,
                        0,
                        width,
                        height,
                        navBarPaint
                );
            }


            if (displayCutoutInsets.left > 0)
            {
                canvas.drawRect(
                        0,
                        0,
                        displayCutoutInsets.left,
                        height,
                        navBarPaint
                );

            }
            if (displayCutoutInsets.right > 0)
            {
                canvas.drawRect(
                        width-displayCutoutInsets.right,
                        0,
                        width,
                        height,
                        navBarPaint
                );

            }

        }



    }
}
