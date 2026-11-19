package io.stairpilot;

/** Event prompt trigger: multiple frames + disappearance rearm + cooldown + limit.
 * Pure Java, deterministic and independently testable. */
public final class PromptGate {
    private boolean armed = true;
    private int present = 0, absent = 0, count = 0;
    private long lastTapMs = Long.MIN_VALUE / 2;
    private final int limit;
    public PromptGate(int maxAutoTaps) {
        if (maxAutoTaps < 1) throw new IllegalArgumentException();
        limit = maxAutoTaps;
    }
    /** True only when clearly visible for three frames, rearmed and out of cooldown. */
    public boolean observe(float matchScore, float threshold, long nowMs) {
        if (matchScore >= threshold) {
            absent = 0;
            present = Math.min(3, present + 1);
        } else if (matchScore <= threshold - .12f) {
            present = 0;
            absent = Math.min(3, absent + 1);
            if (absent == 3 && nowMs - lastTapMs >= 1800L) armed = true;
        } else { present = 0; absent = 0; }
        return armed && count < limit && present >= 3 && nowMs - lastTapMs >= 1800L;
    }
    /** Invoke after an acknowledged gesture. */
    public void committed(long nowMs) {
        armed = false; present = 0; absent = 0;
        lastTapMs = nowMs; count++;
    }
    public int count() { return count; }
    public boolean exhausted() { return count >= limit; }
}
