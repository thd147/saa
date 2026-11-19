import io.stairpilot.DecisionEngine;
import static io.stairpilot.DecisionEngine.Action.*;

/** Java SE tests: javac ... && java ... */
public class DecisionEngineTest {
    static float[] vals(float a,float b,float c){return new float[]{a,b,c};}
    static void eq(Object e,Object a){if(!e.equals(a)) throw new AssertionError("expected="+e+" actual="+a);}
    static void confirm(DecisionEngine e,float[] l,float[] r,DecisionEngine.Action a){
        eq(NONE,e.observe(l,r,.77f));
        eq(a,e.observe(l,r,.77f));
        e.committed(a);
    }
    public static void main(String[] args){
        DecisionEngine e = new DecisionEngine(true);
        float[] yes = vals(.91f,.88f,.88f), no = vals(.10f,.12f,.08f);
        confirm(e,yes,no,UP);
        eq(true,e.facingLeft());
        confirm(e,no,yes,TURN);
        eq(false,e.facingLeft());
        confirm(e,no,yes,UP);
        // First frame alone is never sufficient.
        eq(NONE,e.observe(no,yes,.77f));
        eq(NONE,e.observe(yes,no,.77f));
        // Both immediate cells with equal path strength = ambiguous.
        for(int i=0;i<13;i++) eq(NONE,e.observe(yes,yes,.77f));
        eq(STOP,e.observe(yes,yes,.77f));
        // 3-step path favors the longer coherent branch, not a random direction.
        e = new DecisionEngine(true);
        confirm(e,vals(.89f,.82f,.15f),vals(.9f,.86f,.82f),TURN);
        eq(false,e.facingLeft());
        // Patch 2: weak near tile, strong next two tiles => recognized only when unique.
        e = new DecisionEngine(false);
        confirm(e,no,vals(.76f,.83f,.9f),UP);
        e = new DecisionEngine(true);
        for(int i=0;i<13;i++) eq(NONE,e.observe(no,no,.77f));
        eq(STOP,e.observe(no,no,.77f));
        // New turbo mode: only a decisive, isolated high score gets one-frame response.
        e = new DecisionEngine(true);
        eq(UP,e.observe(vals(.98f,.1f,.1f),no,.77f,true));
        e.committed(UP);
        eq(NONE,e.observe(vals(.80f,.1f,.1f),no,.77f,true));
        eq(UP,e.observe(vals(.80f,.1f,.1f),no,.77f,true));
        e.committed(UP);
        eq(NONE,e.observe(yes,yes,.77f,true));
        System.out.println("PASS: basic control, 2-frame smoothing, 3-step lookahead, ambiguity, no-evidence stop");
    }
}
