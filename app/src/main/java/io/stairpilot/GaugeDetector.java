package io.stairpilot;

/**
 * Pure Java estimator for a user-selected horizontal red/orange timer bar.
 * It never accesses personal data: the caller supplies only pixels within the
 * two chosen screen coordinates. Detection is conservative by design.
 */
public final class GaugeDetector {
    public interface PixelSource { int colorAt(int x, int y); }
    public static final class Reading {
        public final float fill;
        public final boolean valid;
        public final float consistency;
        public Reading(float fill, boolean valid, float consistency) {
            this.fill = fill;
            this.valid = valid;
            this.consistency = consistency;
        }
    }
    private GaugeDetector() { }
    private static boolean isFilled(int argb) {
        int r = (argb >>> 16) & 255;
        int g = (argb >>> 8) & 255;
        int b = argb & 255;
        return r >= 115 && r >= g + 38 && r >= b + 45 && g <= 205;
    }
    public static Reading read(PixelSource pixels, int width, int height,
            int x1, int y1, int x2, int y2) {
        if (pixels == null || width <= 0 || height <= 0) return new Reading(-1f, false, 0f);
        int start = Math.min(x1,x2) + 2;
        int end = Math.max(x1,x2) - 2;
        int cy = (y1+y2)/2;
        if (start < 0 || end >= width || end-start < 34 || cy < 3 || cy >= height-3
                || Math.abs(y1-y2) > Math.max(16,height/60)) {
            return new Reading(-1f,false,0f);
        }
        int samples = Math.min(240, end-start+1);
        boolean[] marks = new boolean[samples];
        int filled = 0;
        for (int i=0; i<samples; ++i) {
            int x = start + (int)((long)i * (end-start) / Math.max(1,samples-1));
            int matches=0;
            if (isFilled(pixels.colorAt(x,cy-2))) matches++;
            if (isFilled(pixels.colorAt(x,cy))) matches++;
            if (isFilled(pixels.colorAt(x,cy+2))) matches++;
            marks[i] = matches >= 2;
            if (marks[i]) filled++;
        }
        if (filled < 4) return new Reading(-1f,false,0f);
        // Filled pixels should form one left-anchored block. If colors are
        // scattered (e.g. red lettering or an event UI), reject rather than
        // accelerate based on a false low timer estimate.
        int first = -1, last = -1;
        for(int i=0;i<samples;i++) if(marks[i]) { if(first==-1) first=i; last=i; }
        if(first > samples / 8) return new Reading(-1f,false,0f);
        int span = last-first+1;
        float consistency = filled/(float)Math.max(1,span);
        if(consistency < .75f) return new Reading(-1f,false,consistency);
        float fill = Math.min(1f, Math.max(0f, (last+1)/(float)samples));
        return new Reading(fill,true,consistency);
    }
}
