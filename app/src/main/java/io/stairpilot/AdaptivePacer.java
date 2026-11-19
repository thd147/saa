package io.stairpilot;

/** Add cautious delay for uncertain recognition / elevated device temperature. */
public final class AdaptivePacer {
    private AdaptivePacer() { }
    public static int extraDelayMs(float left, float right, float threshold, int thermal) {
        int extra = 0;
        float difference = Math.abs(left - right);
        // The 1-frame PVP optimization remains opt-in through ModeProfile.
        if (difference < .08f || Math.abs(Math.max(left, right) - threshold) < .045f) extra += 35;
        if (thermal >= 2) extra += 70; // moderate throttling
        return Math.min(120, extra);
    }
}
