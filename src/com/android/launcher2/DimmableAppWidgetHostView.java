/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher2;

import com.android.launcher.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.view.View;

public class DimmableAppWidgetHostView extends LauncherAppWidgetHostView {
    public DimmableAppWidgetHostView(Context context) {
        super(context);
        mPaint.setFilterBitmap(true);
    }

    private final Paint mPaint = new Paint();
    private int mAlpha;
    private int mDimmedAlpha;
    private Bitmap mDimmedView;
    private Canvas mDimmedViewCanvas;
    private boolean isDimmedViewUpdatePass;

    private static float cubic(float r) {
        return (float) (Math.pow(r-1, 3) + 1);
    }

    /**
     * Returns the interpolated holographic highlight alpha for the effect we want when scrolling
     * pages.
     */
    public static float highlightAlphaInterpolator(float r) {
        final float pivot = 0.3f;
        if (r < pivot) {
            return Math.max(0.5f, 0.65f*cubic(r/pivot));
        } else {
            return Math.min(1.0f, 0.65f*cubic(1 - (r-pivot)/(1-pivot)));
        }
    }

    /**
     * Returns the interpolated view alpha for the effect we want when scrolling pages.
     */
    public static float viewAlphaInterpolator(float r) {
        final float pivot = 0.6f;
        if (r < pivot) {
            return r/pivot;
        } else {
            return 1.0f;
        }
    }

    private void setChildAlpha(float alpha) {
        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            if (child.getAlpha() != alpha) {
                getChildAt(0).setAlpha(alpha);
            }
        }
    }

    private void updateChildAlpha() {
        // hacky, but sometimes widgets get their alpha set back to 1.0f, so we call
        // this to force them back
        setChildAlpha(getAlpha());
    }

    @Override
    public boolean onSetAlpha(int alpha) {
        super.onSetAlpha(alpha);
        return true;
    }

    @Override
    public void setAlpha(float alpha) {
        final float viewAlpha = viewAlphaInterpolator(alpha);
        final float dimmedAlpha = highlightAlphaInterpolator(alpha);
        mAlpha = (int) (viewAlpha * 255);
        mDimmedAlpha = (int) (dimmedAlpha * 255);
        super.setAlpha(viewAlpha);
        setChildAlpha(viewAlpha);
    }

    private void updateDimmedView() {
        if (mDimmedView == null) {
            mDimmedView = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888);
            mDimmedViewCanvas = new Canvas(mDimmedView);
        }
        mDimmedViewCanvas.drawColor(0x00000000);
        mDimmedViewCanvas.concat(getMatrix());
        isDimmedViewUpdatePass = true;
        draw(mDimmedViewCanvas);
        // make the bitmap look "dimmed"
        int dimmedColor = getContext().getResources().getColor(R.color.dimmed_view_color);
        mDimmedViewCanvas.drawColor(dimmedColor, PorterDuff.Mode.SRC_IN);
        isDimmedViewUpdatePass = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mDimmedView == null && mDimmedAlpha > 0.0f) {
            updateDimmedView();
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if (isDimmedViewUpdatePass) {
            final float alpha = getAlpha();
            canvas.save();
            setAlpha(1.0f);
            super.dispatchDraw(canvas);
            canvas.restore();
            setAlpha(alpha);
        } else {
            if (mDimmedView != null && mDimmedAlpha > 0) {
                // draw the dimmed version of this widget
                mPaint.setAlpha(mDimmedAlpha);
                canvas.drawBitmap(mDimmedView, 0, 0, mPaint);
            }

            updateChildAlpha();
            super.dispatchDraw(canvas);
        }
    }
}
