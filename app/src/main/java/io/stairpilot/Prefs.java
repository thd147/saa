package io.stairpilot;

import android.content.Context;
import android.content.SharedPreferences;

/** User calibration and guarded speed settings, expressed as normalized display positions. */
public final class Prefs {
    private final SharedPreferences p;
    public Prefs(Context context) { p = context.getSharedPreferences("stairpilot", Context.MODE_PRIVATE); }
    /** Explicit opt-in; prolonged use is never guaranteed safe or continuous. */
    public boolean extendedSession() { return p.getBoolean("extended_14",false); }
    public void setExtendedSession(boolean enabled) { p.edit().putBoolean("extended_14",enabled).apply(); }
    public boolean adaptiveSpeed() { return p.getBoolean("adaptive_speed_14", true); }
    public void setAdaptiveSpeed(boolean v) { p.edit().putBoolean("adaptive_speed_14",v).apply(); }
    public boolean gaugeCalibrated() { return p.getBoolean("gauge_calibrated_14",false); }
    public void saveGaugeBounds(int x1,int y1,int x2,int y2,int w,int h) {
        if(w<1 || h<1 || Math.abs(x2-x1)<36 || Math.abs(y2-y1)>Math.max(16,h/60))
            throw new IllegalArgumentException("게이지 양 끝을 같은 줄에서 지정하세요");
        p.edit().putFloat("gx1_14",x1/(float)w).putFloat("gx2_14",x2/(float)w)
                .putFloat("gy1_14",y1/(float)h).putFloat("gy2_14",y2/(float)h)
                .putInt("gw_14",w).putInt("gh_14",h)
                .putBoolean("gauge_calibrated_14",true).apply();
    }
    public boolean sameGaugeResolution(int w,int h) {
        return gaugeCalibrated() && Math.abs(w-p.getInt("gw_14",-1))<=2
                && Math.abs(h-p.getInt("gh_14",-1))<=2;
    }
    public int gaugeX1(int w) { return Math.round(p.getFloat("gx1_14",.1f)*w); }
    public int gaugeY1(int h) { return Math.round(p.getFloat("gy1_14",.05f)*h); }
    public int gaugeX2(int w) { return Math.round(p.getFloat("gx2_14",.8f)*w); }
    public int gaugeY2(int h) { return Math.round(p.getFloat("gy2_14",.05f)*h); }
    public ModeProfile mode() {
        try { return ModeProfile.valueOf(p.getString("mode_v13", "NORMAL")); }
        catch (IllegalArgumentException e) { return ModeProfile.NORMAL; }
    }
    public void setMode(ModeProfile m) { p.edit().putString("mode_v13", m.name()).apply(); }
    /** Delay is applied after gesture acknowledgement; OS rendering time is additional. */
    public int delayMs() { return Math.max(25, Math.min(350,
            p.getInt("delay13_" + mode().name(), mode().defaultDelayMs))); }
    public void setDelay(int ms) { p.edit().putInt("delay13_" + mode().name(),
            Math.max(25, Math.min(350, ms))).apply(); }
    public int tapDurationMs() { return mode().tapMs; }
    public boolean eventAutoContinue() { return p.getBoolean("event_auto_continue13", false); }
    public void setEventAutoContinue(boolean enabled) { p.edit().putBoolean("event_auto_continue13", enabled).apply(); }
    public boolean eventPromptCalibrated() { return p.getBoolean("event_prompt13", false); }
    public void markEventPromptCalibrated(boolean v) { p.edit().putBoolean("event_prompt13", v).apply(); }
    public void saveEventPoint(float x, float y, int w, int h) {
        if(w < 1 || h < 1) throw new IllegalArgumentException();
        p.edit().putFloat("event_px13", Math.max(0f, Math.min(1f, x / w)))
                .putFloat("event_py13", Math.max(0f, Math.min(1f, y / h)))
                .putInt("event_w13", w).putInt("event_h13", h).apply();
    }
    public int eventX(int w) { return Math.round(p.getFloat("event_px13", .5f)*w); }
    public int eventY(int h) { return Math.round(p.getFloat("event_py13", .5f)*h); }
    public boolean sameEventResolution(int w, int h) {
        return Math.abs(w-p.getInt("event_w13", -1)) <= 2 &&
                Math.abs(h-p.getInt("event_h13", -1)) <= 2;
    }
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
