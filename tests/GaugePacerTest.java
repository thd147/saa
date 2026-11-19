import io.stairpilot.GaugePacer;
public class GaugePacerTest {
    private static void eq(int actual,int expected) {
        if(actual!=expected) throw new AssertionError("expected="+expected+" actual="+actual);
    }
    public static void main(String[] args) {
        eq(GaugePacer.delayMs(72,0,1f,true,0),72);
        eq(GaugePacer.delayMs(72,100,1f,true,0),64);
        eq(GaugePacer.delayMs(72,250,.49f,true,0),43);
        eq(GaugePacer.delayMs(72,500,.20f,true,0),28);
        eq(GaugePacer.delayMs(72,1000,.10f,true,0),24);
        eq(GaugePacer.delayMs(72,1000,.10f,false,0),40);
        eq(GaugePacer.delayMs(72,1000,.10f,true,70),85);
        eq(GaugePacer.delayMs(35,10000,.10f,true,0),24);
        System.out.println("GaugePacerTest OK");
    }
}
