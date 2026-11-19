package io.stairpilot;

/** Pure-Java policy for long-running sessions. This does NOT guarantee 24h uptime. */
public final class RunGuard {
    public enum State {
        OK, COOLDOWN, STOP_SCREEN_OFF, STOP_LOW_BATTERY, STOP_HOT,
        STOP_NO_FRAMES, STOP_GESTURE_TIMEOUT, STOP_SESSION_LIMIT
    }
    public static final long FRAME_TIMEOUT_MS = 8000;
    public static final long GESTURE_TIMEOUT_MS = 2500;
    public static final long REGULAR_MAX_MS = 90L * 60_000L;
    public static final long EXTENDED_MAX_MS = 24L * 60L * 60_000L;
    public static final long COOLDOWN_INTERVAL_MS = 30L * 60_000L;
    public static final long COOLDOWN_DURATION_MS = 90_000L;
    public static final int MIN_BATTERY_PERCENT = 15;
    public static final int THERMAL_SEVERE = 3;
    private final long startedAt;
    private long nextRestAt;
    private long restUntil;
    private final boolean extended;

    public RunGuard(long startedAt, boolean extended) {
        this.startedAt = startedAt;
        this.extended = extended;
        this.nextRestAt = startedAt + COOLDOWN_INTERVAL_MS;
    }
    public State evaluate(long now, long latestFrame, long gestureSince,
            int batteryPercent, int thermalStatus, boolean interactive) {
        if (!interactive) return State.STOP_SCREEN_OFF;
        if (batteryPercent >= 0 && batteryPercent < MIN_BATTERY_PERCENT) return State.STOP_LOW_BATTERY;
        if (thermalStatus >= THERMAL_SEVERE) return State.STOP_HOT;
        if (now - startedAt >= (extended ? EXTENDED_MAX_MS : REGULAR_MAX_MS)) return State.STOP_SESSION_LIMIT;
        if (gestureSince > 0 && now - gestureSince >= GESTURE_TIMEOUT_MS) return State.STOP_GESTURE_TIMEOUT;
        if (latestFrame > 0 && now - latestFrame >= FRAME_TIMEOUT_MS) return State.STOP_NO_FRAMES;
        if (extended && (restUntil > 0 || now >= nextRestAt)) {
            if (restUntil == 0) restUntil = now + COOLDOWN_DURATION_MS;
            if (now < restUntil) return State.COOLDOWN;
            restUntil = 0;
            nextRestAt = now + COOLDOWN_INTERVAL_MS;
        }
        return State.OK;
    }
    public boolean extended() { return extended; }
    public long uptimeMs(long now) { return Math.max(0, now - startedAt); }
}
