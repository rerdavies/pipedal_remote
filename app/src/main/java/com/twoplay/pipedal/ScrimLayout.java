package com.twoplay.pipedal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

public class ScrimLayout extends FrameLayout   {
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

    PaintDrawable statusBarPaint = new PaintDrawable();
    PaintDrawable navBarPaint = new PaintDrawable();
    public void setStatusBarColor(int color) {

        statusBarPaint.getPaint().setColor(color);
        this.invalidate();
    }
    public void setNavigationBarColorXX(int color) {

        navBarPaint.getPaint().setColor(color);
        this.invalidate();
    }

    Insets statusBarInsets;
    Insets navBarInsets;
    public void setScrims(@NonNull WindowInsetsCompat insets) {
        this.statusBarInsets  = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        this.navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
        this.invalidate();


    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        if (statusBarInsets != null && navBarInsets != null)
        {
            canvas.drawRect(0,0, width, statusBarInsets.top, statusBarPaint.getPaint());

            if (navBarInsets.bottom > 0) {
                canvas.drawRect(
                        0,
                        height - navBarInsets.bottom,
                        width,
                        navBarInsets.bottom,
                        navBarPaint.getPaint()
                );
            }
            if (navBarInsets.left > 0) {
                canvas.drawRect(
                        0,
                        0,
                        navBarInsets.left,
                        height,
                        navBarPaint.getPaint()
                );
            }
            if (navBarInsets.right > 0) {
                canvas.drawRect(
                        width-navBarInsets.right,
                        0,
                        width,
                        height,
                        navBarPaint.getPaint()
                );
            }
        }



    }
}
