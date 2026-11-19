package io.stairpilot;

import android.content.Context;
import android.content.SharedPreferences;

/** User calibration and guarded speed settings, expressed as normalized display positions. */
public final class Prefs {
    private final SharedPreferences p;
    public Prefs(Context context) { p = context.getSharedPreferences("stairpilot", Context.MODE_PRIVATE); }
    public int delayMs() { return Math.max(90, Math.min(790, p.getInt("delay", 160))); }
    public void setDelay(int ms) { p.edit().putInt("delay", Math.max(90, Math.min(790, ms))).apply(); }
    public float threshold() { return Math.max(.50f, Math.min(.95f, p.getFloat("threshold", .78f))); }
    public void setThreshold(float f) { p.edit().putFloat("threshold", Math.max(.50f, Math.min(.95f, f))).apply(); }
    public boolean initialLeft() { return p.getBoolean("initialLeft", true); }
    public void setInitialLeft(boolean left) { p.edit().putBoolean("initialLeft", left).apply(); }
    public boolean calibrated() { return p.getBoolean("calibrated_v2", false); }
    public void markCalibrated(boolean value) { p.edit().putBoolean("calibrated_v2", value).apply(); }
    public void savePoint(int step, float x, float y, int width, int height) {
        if (width < 1 || height < 1 || step < 0 || step > 7) throw new IllegalArgumentException();
        p.edit().putFloat("px" + step, Math.max(0, Math.min(1, x / width)))
                .putFloat("py" + step, Math.max(0, Math.min(1, y / height)))
                .putInt("calW", width).putInt("calH", height).apply();
    }
    public int x(int step, int width) { return Math.round(p.getFloat("px" + step, .5f) * width); }
    public int y(int step, int height) { return Math.round(p.getFloat("py" + step, .5f) * height); }
    public boolean sameResolution(int w, int h) {
        return Math.abs(w-p.getInt("calW", -1000)) <= 2 &&
               Math.abs(h-p.getInt("calH", -1000)) <= 2;
    }
}
