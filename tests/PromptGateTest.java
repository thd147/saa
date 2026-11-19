import io.stairpilot.PromptGate;
public class PromptGateTest {
    private static void eq(boolean v,boolean got){if(v!=got)throw new AssertionError(v+" != "+got);}
    public static void main(String[] args) {
        PromptGate g = new PromptGate(2);
        eq(false,g.observe(.97f,.93f,100));
        eq(false,g.observe(.97f,.93f,120));
        eq(true,g.observe(.97f,.93f,140));
        g.committed(140);
        eq(false,g.observe(.98f,.93f,200));
        eq(false,g.observe(.98f,.93f,2020));
        for(int i=0;i<3;i++)eq(false,g.observe(.1f,.93f,2100+i));
        eq(false,g.observe(.97f,.93f,2300));
        eq(false,g.observe(.97f,.93f,2310));
        eq(true,g.observe(.97f,.93f,2320));
        g.committed(2320);
        eq(true,g.exhausted());
        for(int i=0;i<3;i++)eq(false,g.observe(.1f,.93f,4500+i));
        for(int i=0;i<5;i++)eq(false,g.observe(.97f,.93f,4600+i));
        System.out.println("PASS prompt gating rearm, debounce, cooldown, cap");
    }
}
