package io.stairpilot;

import android.media.Image;
import android.graphics.Bitmap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Matches a user-taught stair patch in a small ROI. No third-party native libraries needed. */
public final class VisualMatcher {
    public static final int TEMPLATE_W = 22, TEMPLATE_H = 14;
    private byte[] pattern;
    private int pw, ph;
    private double sampledSum, sampledSquares;
    private int sampledCount;
    private void prepareSampleStats() {
        sampledSum = 0d; sampledSquares = 0d; sampledCount = 0;
        if(!ready()) return;
        for(int j=0;j<ph;j+=2) for(int i=0;i<pw;i+=2) {
            int value = pattern[j*pw+i] & 255;
            sampledSum += value; sampledSquares += value*value; sampledCount++;
        }
    }

    private static int gray(Image.Plane plane, int x, int y) {
        return gray(plane.getBuffer(), plane.getRowStride(), plane.getPixelStride(), x, y);
    }
    private static int gray(ByteBuffer data, int rowStride, int pixelStride, int x, int y) {
        int pos = y * rowStride + x * pixelStride;
        int r = data.get(pos) & 255;
        int g = data.get(pos + 1) & 255;
        int b = data.get(pos + 2) & 255;
        return (77*r + 150*g + 29*b) >> 8;
    }
    public boolean ready() { return pattern != null && pattern.length == pw * ph; }
    public void learn(Image image, int cx, int cy, File file) throws IOException {
        if (cx < TEMPLATE_W || cy < TEMPLATE_H || cx + TEMPLATE_W >= image.getWidth() || cy + TEMPLATE_H >= image.getHeight()) {
            throw new IOException("블록 좌표가 화면 가장자리에 너무 가깝습니다.");
        }
        byte[] values = new byte[TEMPLATE_W * TEMPLATE_H];
        Image.Plane plane = image.getPlanes()[0];
        int i = 0;
        for (int y = cy - TEMPLATE_H / 2; y < cy + TEMPLATE_H / 2; y++) {
            for (int x = cx - TEMPLATE_W / 2; x < cx + TEMPLATE_W / 2; x++) {
                values[i++] = (byte) gray(plane, x, y);
            }
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(TEMPLATE_W); out.writeInt(TEMPLATE_H); out.write(values);
        }
        pattern = values; pw = TEMPLATE_W; ph = TEMPLATE_H;
        prepareSampleStats();
    }
    /** User taught texture is acquired from the frozen game screen, not a moving scene. */
    public void learn(Bitmap bitmap, int cx, int cy, File file) throws IOException {
        if (cx < TEMPLATE_W/2 || cy < TEMPLATE_H/2 ||
                cx + TEMPLATE_W/2 >= bitmap.getWidth() || cy + TEMPLATE_H/2 >= bitmap.getHeight()) {
            throw new IOException("학습할 위치가 화면 가장자리에 가깝습니다");
        }
        byte[] values = new byte[TEMPLATE_W*TEMPLATE_H];
        int idx=0;
        double sum=0,squares=0;
        for(int y=cy-TEMPLATE_H/2; y<cy+TEMPLATE_H/2; y++) {
            for(int x=cx-TEMPLATE_W/2; x<cx+TEMPLATE_W/2; x++) {
                int c=bitmap.getPixel(x,y);
                int g=(77*((c>>>16)&255) + 150*((c>>>8)&255) + 29*(c&255))>>8;
                values[idx++]=(byte)g;
                sum+=g;squares+=g*g;
            }
        }
        double variance=squares/idx - (sum/idx)*(sum/idx);
        if(variance<24d) throw new IOException("너무 단색입니다. 계단의 모서리/무늬를 선택하세요");
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(TEMPLATE_W);out.writeInt(TEMPLATE_H);out.write(values);
        }
        pattern=values;pw=TEMPLATE_W;ph=TEMPLATE_H;
        prepareSampleStats();
    }
    public boolean load(File file) {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int w = in.readInt(), h = in.readInt();
            if (w < 8 || w > 48 || h < 8 || h > 32) return false;
            byte[] bytes = new byte[w * h];
            in.readFully(bytes);
            pattern = bytes; pw = w; ph = h;
            prepareSampleStats();
            return true;
        } catch (IOException e) { return false; }
    }
    public float match(Image image, int cx, int cy, int rw, int rh) {
        if (!ready()) return 0f;
        int left = Math.max(0, cx-rw/2), top = Math.max(0, cy-rh/2);
        int right = Math.min(image.getWidth(), cx+rw/2);
        int bottom = Math.min(image.getHeight(), cy+rh/2);
        if (right-left < pw || bottom-top < ph) return 0f;
        Image.Plane plane = image.getPlanes()[0];
        // Keep one native ByteBuffer view per search rather than per pixel.
        ByteBuffer data = plane.getBuffer();
        int rowStride = plane.getRowStride(), pixelStride = plane.getPixelStride();
        final double ta = sampledSum, ta2 = sampledSquares;
        final int n = sampledCount;
        final double varT = ta2 - ta * ta / n;
        float best = 0f;
        for (int y = top; y <= bottom-ph; y += 2) {
            for (int x = left; x <= right-pw; x += 2) {
                double ia=0, ia2=0, cross=0;
                int absDiff=0;
                for (int j=0; j<ph; j+=2) {
                    for (int i=0; i<pw; i+=2) {
                        int t = pattern[j*pw+i] & 255;
                        int v = gray(data,rowStride,pixelStride,x+i,y+j);
                        ia+=v; ia2+=v*v; cross+=t*v;
                        absDiff += Math.abs(t-v);
                    }
                }
                double varI = ia2 - ia*ia/n;
                double s = varT < 70 || varI < 70
                        ? Math.max(0d, 1d - (double)absDiff/n/64d)
                        : (cross - ta*ia/n) / Math.sqrt(varT*varI);
                if (s > best) {
                    best = (float) s;
                    if(best > .978f) return best;
                }
            }
        }
        return best;
    }
}
