package io.stairpilot;

/**
 * Game decision, independent of Android and image capture.
 * 1.0: original single-cell UP/TURN state machine.
 * 1.1: two-frame confirmation and safe no-evidence handling.
 * 1.2: 3-cell lookahead on both sides; reject ambiguous crossings.
 */
public final class DecisionEngine {
    public enum Action { NONE, UP, TURN, STOP }
    private boolean facingLeft;
    private Action candidate = Action.NONE;
    private int consecutive = 0;
    private int missing = 0;

    public DecisionEngine(boolean initialLeft) { facingLeft = initialLeft; }
    public boolean facingLeft() { return facingLeft; }

    /**
     * left/right: three scores for the successive visible steps on each side.
     * A TURN tap also goes up one step: do not dispatch UP after it.
     * Lookahead only resolves ambiguity; never blindly dispatch a 3-click burst.
     */
    public Action observe(float[] left, float[] right, float threshold) {
        if (left == null || right == null || left.length < 3 || right.length < 3) {
            throw new IllegalArgumentException("Three scores on each side are required");
        }
        boolean l1 = left[0] >= threshold;
        boolean r1 = right[0] >= threshold;
        Action desired;
        if (l1 && r1) {
            // A branching image can be a visual effect or a bad calibration.
            // Only continue if exactly one side has a clear 2-or-3-tile run.
            int lr = run(left, threshold);
            int rr = run(right, threshold);
            if (lr == rr || (lr < 2 && rr < 2)) desired = Action.NONE;
            else desired = (lr > rr) == facingLeft ? Action.UP : Action.TURN;
        } else if (l1 || r1) {
            desired = (l1 == facingLeft) ? Action.UP : Action.TURN;
        } else {
            // A weak first tile can be recovered only with two strong following tiles
            // and no competing path; this is conservative temporal pattern recovery.
            boolean lWeak = left[0] >= threshold - .035f
                    && left[1] >= threshold && left[2] >= threshold;
            boolean rWeak = right[0] >= threshold - .035f
                    && right[1] >= threshold && right[2] >= threshold;
            if (lWeak == rWeak) desired = Action.NONE;
            else desired = (lWeak == facingLeft) ? Action.UP : Action.TURN;
        }
        if (desired == Action.NONE) {
            missing++;
            candidate = Action.NONE;
            consecutive = 0;
            return missing >= 14 ? Action.STOP : Action.NONE;
        }
        missing = 0;
        if (desired != candidate) {
            candidate = desired;
            consecutive = 1;
            return Action.NONE;
        }
        consecutive++;
        return consecutive >= 2 ? desired : Action.NONE;
    }
    private static int run(float[] scores, float t) {
        int len=0;
        while(len<3 && scores[len] >= t) len++;
        return len;
    }
    /** Invoke only after the operating system confirms the gesture. */
    public void committed(Action action) {
        if (action == Action.TURN) facingLeft = !facingLeft;
        candidate = Action.NONE;
        consecutive = 0;
        missing = 0;
    }
}
