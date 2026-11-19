import io.stairpilot.DecisionEngine;
import io.stairpilot.GaugePacer;
import java.util.Random;
import static io.stairpilot.DecisionEngine.Action.*;

/** Three deterministic synthetic long-run tests, NOT actual Infinite Stairs gameplay. */
public final class LongClimbSimulationTest {
    private static final float THRESHOLD=.78f;
    static float[] evidence(boolean exists, float jitter) {
        if(exists) return new float[]{.96f-jitter,.92f-jitter,.88f-jitter};
        return new float[]{.09f+jitter,.08f+jitter,.10f+jitter};
    }
    public static void run(int target, long seed, float twistChance) {
        Random rng=new Random(seed);
        DecisionEngine engine=new DecisionEngine(true);
        boolean left=true;
        int wrong=0, turns=0, minimum=999,maximum=0,slow=0,fast=0,lowGauge=0;
        long begin=System.nanoTime();
        for(int stair=0;stair<target;stair++) {
            boolean targetSide=rng.nextFloat()<twistChance?!left:left;
            float jit=rng.nextFloat()*.03f;
            float[] l=evidence(targetSide,jit),r=evidence(!targetSide,jit);
            // 4% of frames carry a weak signal that needs a safe confirmation.
            boolean weak=rng.nextInt(25)==0;
            if(weak) (targetSide?l:r)[0]=.81f;
            DecisionEngine.Action expected=targetSide==left?UP:TURN;
            DecisionEngine.Action got=engine.observe(l,r,THRESHOLD,true);
            if(weak) {
                if(got!=NONE) throw new AssertionError("weak event acted on first frame at "+stair);
                got=engine.observe(l,r,THRESHOLD,true);
                slow++;
            } else fast++;
            if(got!=expected) {wrong++;throw new AssertionError("mismatch step "+stair+" want "+expected+" got "+got);}
            engine.committed(got);
            if(got==TURN) {left=!left;turns++;}
            if(engine.facingLeft()!=left) throw new AssertionError("direction desynchronized at "+stair);
            float gauge=(float)((stair*31+seed)%113)/112f; // synthetic 0..1
            if(gauge<.15f) lowGauge++;
            int delay=GaugePacer.delayMs(72,stair,gauge,true,weak?35:0);
            if(delay<24 || delay>350) throw new AssertionError("unsafe pacing");
            minimum=Math.min(minimum,delay);maximum=Math.max(maximum,delay);
        }
        System.out.printf("SIMULATED %,d steps: PASS; wrong=%d, turns=%,d, extra-confirm=%,d, fast-confirm=%,d, low-gauge=%d, delay-range=%d..%dms, compute=%dms%n",
                target,wrong,turns,slow,fast,lowGauge,minimum,maximum,
                (System.nanoTime()-begin)/1_000_000L);
    }
    public static void main(String[] args) {
        run(50_000,147L,.15f);
        run(100_000,9247L,.30f);
        run(200_000,938822L,.45f);
    }
}
