import io.stairpilot.GaugeDetector;

public class GaugeDetectorTest {
    private static final int RED = 0xfff14b19;
    private static final int DARK = 0xff281c19;
    static GaugeDetector.Reading bar(final float fill) {
        return GaugeDetector.read((x,y)->x < 10 + (int)(400*fill) ? RED : DARK,
                450, 80, 10, 20, 410, 20);
    }
    static void close(float a, float b, float tolerance) {
        if(Math.abs(a-b)>tolerance) throw new AssertionError(a + " != " + b);
    }
    public static void main(String[] args) {
        for(float amount:new float[] {.08f,.17f,.40f,.75f,.99f}) {
            GaugeDetector.Reading result=bar(amount);
            if(!result.valid) throw new AssertionError("valid gauge rejected at " + amount);
            close(result.fill, amount, .035f);
        }
        GaugeDetector.Reading fake=GaugeDetector.read((x,y)->x%23 < 8 ? RED : DARK,
                450,80,10,20,410,20);
        if(fake.valid) throw new AssertionError("red letters mistaken for gauge");
        GaugeDetector.Reading missing=GaugeDetector.read((x,y)->DARK,
                450,80,10,20,410,20);
        if(missing.valid) throw new AssertionError("unreadable timer must remain unknown");
        GaugeDetector.Reading invalidBounds=GaugeDetector.read((x,y)->RED,
                450,80,10,2,410,35);
        if(invalidBounds.valid) throw new AssertionError("incorrect calibration accepted");
        System.out.println("GaugeDetectorTest OK: 5 synthetic fills + 3 rejection cases");
    }
}
