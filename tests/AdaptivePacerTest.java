import io.stairpilot.AdaptivePacer;

public final class AdaptivePacerTest {
    public static void main(String[] args) {
        if (AdaptivePacer.extraDelayMs(.95f,.20f,.78f,0)!=0) throw new AssertionError("strong signal slowed");
        if (AdaptivePacer.extraDelayMs(.80f,.79f,.78f,0)!=35) throw new AssertionError("weak signal not slowed");
        if (AdaptivePacer.extraDelayMs(.95f,.20f,.78f,2)!=70) throw new AssertionError("moderate not slowed");
        if (AdaptivePacer.extraDelayMs(.78f,.77f,.78f,2)!=105) throw new AssertionError("combined delay");
        System.out.println("AdaptivePacerTest OK");
    }
}
