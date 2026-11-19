package io.stairpilot;

/** Adaptive post-gesture delay. Estimates stair count from ACKed touch inputs. */
public final class GaugePacer {
    private GaugePacer() { }
    public static int delayMs(int baseMs, int acceptedInputs,
            float gaugeFill, boolean reliableGauge, int cautiousExtraMs) {
        int baseline = Math.max(25, Math.min(350, baseMs));
        int steps = Math.max(0, acceptedInputs);
        int ramp = steps >= 2500 ? 38 : steps >= 1000 ? 32
                : steps >= 500 ? 25 : steps >= 250 ? 17 : steps >= 100 ? 8 : 0;
        int gaugeBoost = 0;
        if (reliableGauge && gaugeFill >= 0f && gaugeFill <= 1f) {
            if (gaugeFill < .15f) gaugeBoost = 25;
            else if (gaugeFill < .30f) gaugeBoost = 19;
            else if (gaugeFill < .50f) gaugeBoost = 12;
            else if (gaugeFill < .75f) gaugeBoost = 5;
        }
        // The gesture's OS acknowledgement, screen frames and recognition still
        // add latency. Never fire an unbounded series of taps.
        return Math.max(24, Math.min(350,baseline-ramp-gaugeBoost+Math.max(0,cautiousExtraMs)));
    }
}
