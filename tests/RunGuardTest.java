import io.stairpilot.RunGuard;

public final class RunGuardTest {
    private static void eq(RunGuard.State got, RunGuard.State want) {
        if(got != want) throw new AssertionError(got+" != "+want);
    }
    public static void main(String[] args) {
        long start=100_000L;
        RunGuard g = new RunGuard(start,false);
        eq(g.evaluate(start+1000,start+900,0,90,0,true),RunGuard.State.OK);
        eq(g.evaluate(start+1000,start+900,0,5,0,true),RunGuard.State.STOP_LOW_BATTERY);
        eq(g.evaluate(start+1000,start+900,0,90,3,true),RunGuard.State.STOP_HOT);
        eq(g.evaluate(start+1000,start+900,0,90,0,false),RunGuard.State.STOP_SCREEN_OFF);
        eq(g.evaluate(start+10_000,start+1000,0,90,0,true),RunGuard.State.STOP_NO_FRAMES);
        eq(g.evaluate(start+10_000,start+9999,start+4000,90,0,true),RunGuard.State.STOP_GESTURE_TIMEOUT);
        eq(g.evaluate(start+RunGuard.REGULAR_MAX_MS,start+RunGuard.REGULAR_MAX_MS,0,90,0,true),RunGuard.State.STOP_SESSION_LIMIT);
        RunGuard longRun = new RunGuard(start,true);
        long atRest=start+RunGuard.COOLDOWN_INTERVAL_MS+100;
        eq(longRun.evaluate(atRest,atRest-100,0,95,0,true),RunGuard.State.COOLDOWN);
        eq(longRun.evaluate(atRest+RunGuard.COOLDOWN_DURATION_MS-1,atRest+RunGuard.COOLDOWN_DURATION_MS-2,0,95,0,true),RunGuard.State.COOLDOWN);
        eq(longRun.evaluate(atRest+RunGuard.COOLDOWN_DURATION_MS,atRest+RunGuard.COOLDOWN_DURATION_MS-100,0,95,0,true),RunGuard.State.OK);
        eq(longRun.evaluate(start+RunGuard.EXTENDED_MAX_MS,start+RunGuard.EXTENDED_MAX_MS,0,95,0,true),RunGuard.State.STOP_SESSION_LIMIT);
        System.out.println("RunGuardTest OK");
    }
}
