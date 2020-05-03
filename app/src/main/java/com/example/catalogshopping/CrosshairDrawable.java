package com.example.catalogshopping;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CrosshairDrawable extends Drawable {

    private static final String TAG = "CrosshairDrawable";

    private final Paint paint = new Paint();
    private boolean enabled;

    @Override
    public void draw(@NonNull Canvas canvas) {
        float cx = canvas.getWidth() / 2;
        float cy = canvas.getHeight() / 2;
        if (enabled) {
            paint.setColor(Color.GREEN);
            canvas.drawCircle(cx, cy, 15, paint);
        } else {
            paint.setColor(Color.RED);
            canvas.drawText("X", cx, cy, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
