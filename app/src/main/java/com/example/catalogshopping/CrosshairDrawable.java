package com.example.catalogshopping;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class to create a crosshair for the user indicating on whether a model could be placed or not.
 * IS NOT WORKING REPORTEDLY DUE TO UNKNOWN SCENEFORM GLITCH
 */
public class CrosshairDrawable extends Drawable {

    private static final String TAG = "CrosshairDrawable";

    private Paint paint = new Paint();
    private boolean enabled;

    @Override
    public void draw(@NonNull Canvas canvas) {
        float x = canvas.getWidth() / 2;
        float y = canvas.getHeight() / 2;
        if (enabled) {
            paint.setColor(Color.GREEN);
            canvas.drawCircle(x, y, 15, paint);
        } else {
            paint.setColor(Color.RED);
            canvas.drawCircle(x, y, 15, paint);
//            canvas.drawText("X", x, y, paint);
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
