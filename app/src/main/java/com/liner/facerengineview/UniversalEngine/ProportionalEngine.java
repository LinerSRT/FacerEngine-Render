package com.liner.facerengineview.UniversalEngine;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ProportionalEngine extends View {
    private float scaleX = 1f;
    private float scaleY = 1f;

    public ProportionalEngine(Context context) {
        super(context);
    }

    public ProportionalEngine(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public final synchronized float getScaleFactorX() {
        return this.scaleX;
    }

    public final synchronized float getScaleFactorY() {
        return this.scaleY;
    }

    public final synchronized void setScaleFactorX(float scaleFactorX) {
        this.scaleX = scaleFactorX;
    }

    public final synchronized void setScaleFactorY(float scaleFactorY) {
        this.scaleY = scaleFactorY;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int largestDimension;
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width >= height) {
            largestDimension = width;
        } else {
            largestDimension = height;
        }
        int desiredWidth = (int) (((float) largestDimension) * getScaleFactorX());
        int desiredHeight = (int) (((float) largestDimension) * getScaleFactorY());
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            desiredWidth = width;
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                desiredHeight = height;
            } else {
                desiredHeight = (int) (((float) width) * (getScaleFactorY() / getScaleFactorX()));
            }
        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            desiredHeight = height;
            desiredWidth = (int) (((float) height) * (getScaleFactorX() / getScaleFactorY()));
        }
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.getMode(widthMeasureSpec)), MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.getMode(heightMeasureSpec)));
    }
}
