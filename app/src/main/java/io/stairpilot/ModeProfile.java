package io.stairpilot;

/** Only changes the pacing/confirmation policy. Never assumes special event rules. */
public enum ModeProfile {
    NORMAL("일반 · 균형", 105, 27, false),
    PVP("PVP · 빠름", 55, 19, true),
    EVENT("이벤트 · 안정", 105, 28, false),
    COOP("협동 · 정밀", 135, 31, false);

    public final String korean;
    public final int defaultDelayMs;
    public final int tapMs;
    public final boolean fastConfirmation;

    ModeProfile(String korean, int delay, int tap, boolean fast) {
        this.korean = korean;
        this.defaultDelayMs = delay;
        this.tapMs = tap;
        this.fastConfirmation = fast;
    }
}
