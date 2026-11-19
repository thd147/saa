package io.stairpilot;

/** Only changes the pacing/confirmation policy. Never assumes special event rules. */
public enum ModeProfile {
    NORMAL("일반 · 가속", 72, 22, true),
    PVP("PVP · 빠름", 38, 17, true),
    EVENT("이벤트 · 안정", 76, 25, false),
    COOP("협동 · 정밀", 110, 30, false);

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
