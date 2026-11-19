package io.stairpilot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.nio.ByteBuffer;

/** On-device frame capture; captures locally, doesn't send or save full screenshots. */
public final class ProjectionService extends Service {
    public static final String ACTION_START = "io.stairpilot.START_CAPTURE";
    public static final String ACTION_STOP = "io.stairpilot.STOP_CAPTURE";
    public static final String EXTRA_RESULT_CODE = "code";
    public static final String EXTRA_RESULT_DATA = "data";
    private static volatile ProjectionService instance;
    public static ProjectionService get() { return instance; }
    public static volatile String status = "화면 공유 전";
    public static volatile int successfulSteps = 0;
    public static volatile String lastScores = "계단 신뢰도 대기";

    private HandlerThread thread;
    private Handler work, ui;
    private MediaProjection projection;
    private MediaProjection.Callback projectionCallback;
    private VirtualDisplay display;
    private ImageReader reader;
    private volatile boolean running = false;
    private final AtomicBoolean waitingForGesture = new AtomicBoolean(false);
    private volatile Consumer<Bitmap> snapshotRequest;
    private final VisualMatcher matcher = new VisualMatcher();
    private volatile DecisionEngine engine;
    private volatile int sessionToken = 0;
    private Prefs prefs;
    private volatile long lastActionMs = 0;
    private int captureW, captureH;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = new Prefs(this);
        ui = new Handler(getMainLooper());
        thread = new HandlerThread("stairpilot-capture");
        thread.start();
        work = new Handler(thread.getLooper());
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("capture", "화면 인식 중", NotificationManager.IMPORTANCE_LOW));
    }

    private Notification notification() {
        Intent stop = new Intent(this, ProjectionService.class).setAction(ACTION_STOP);
        PendingIntent pi = PendingIntent.getService(this, 41, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "capture")
                .setContentTitle("StairPilot · 모바일 계단 인식")
                .setContentText("게임 화면 분석 중 · 중지하려면 누르세요")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "화면 공유 종료", pi)
                .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!ACTION_START.equals(intent.getAction())) return START_NOT_STICKY;
        if (projection != null) return START_NOT_STICKY;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(11, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else { startForeground(11, notification()); }
            int code = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent consentData = Build.VERSION.SDK_INT >= 33
                    ? intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class)
                    : intent.getParcelableExtra(EXTRA_RESULT_DATA);
            if (code != android.app.Activity.RESULT_OK || consentData == null) { stopSelf(); return START_NOT_STICKY; }
            MediaProjectionManager mgr = getSystemService(MediaProjectionManager.class);
            projection = mgr.getMediaProjection(code, consentData);
            if (projection == null) { stopSelf(); return START_NOT_STICKY; }
            projectionCallback = new MediaProjection.Callback() {
                @Override public void onStop() { status = "화면 공유가 종료됨"; stopSelf(); }
                @Override public void onCapturedContentResize(int width, int height) {
                    work.post(() -> resizeCapture(width, height));
                }
            };
            projection.registerCallback(projectionCallback, work);
            Point dimensions = screenDimensions();
            captureW = dimensions.x; captureH = dimensions.y;
            reader = ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 2);
            reader.setOnImageAvailableListener(this::onFrame, work);
            display = projection.createVirtualDisplay("StairPilot", captureW, captureH,
                    getResources().getDisplayMetrics().densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, work);
            matcher.load(templateFile());
            status = "화면 공유 준비 완료 · 게임에서 보정하세요";
        } catch (Exception e) {
            status = "화면 공유 실패: " + e.getClass().getSimpleName();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private Point screenDimensions() {
        WindowManager wm = getSystemService(WindowManager.class);
        if (Build.VERSION.SDK_INT >= 30) {
            Rect r = wm.getMaximumWindowMetrics().getBounds();
            return new Point(r.width(), r.height());
        }
        Point p = new Point();
        wm.getDefaultDisplay().getRealSize(p);
        return p;
    }

    private void resizeCapture(int newW, int newH) {
        if (newW < 1 || newH < 1 || display == null || (newW == captureW && newH == captureH)) return;
        pause("화면 크기 변경됨 · 다시 보정해야 합니다");
        if (reader != null) { reader.setOnImageAvailableListener(null, null); reader.close(); }
        reader = ImageReader.newInstance(newW, newH, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(this::onFrame, work);
        display.resize(newW, newH, getResources().getDisplayMetrics().densityDpi);
        display.setSurface(reader.getSurface());
        captureW = newW; captureH = newH;
    }

    private void onFrame(ImageReader source) {
        Image image = null;
        try {
            image = source.acquireLatestImage();
            if (image == null) return;
            Consumer<Bitmap> snapshot = snapshotRequest;
            if (snapshot != null) {
                snapshotRequest = null;
                Bitmap frozen = renderFrame(image);
                ui.post(() -> snapshot.accept(frozen));
                return;
            }
            if (!running || waitingForGesture.get() || engine == null) return;
            int w = image.getWidth(), h = image.getHeight();
            if (!prefs.sameResolution(w,h)) { pause("화면 크기 불일치 · 다시 보정"); return; }
            long now = SystemClock.uptimeMillis();
            if (now-lastActionMs < prefs.delayMs()) return;
            int rw = Math.max(50, Math.round(75f*w/720f));
            int rh = Math.max(28, Math.round(40f*h/1280f));
            // Patch 2: scan up to three squares on both the left and right.
            // Position 0/1: action buttons; 2..4: left steps; 5..7: right steps.
            float[] left = new float[3], right = new float[3];
            for (int i=0; i<3; i++) {
                left[i] = matcher.match(image, prefs.x(2+i,w), prefs.y(2+i,h),rw,rh);
                right[i] = matcher.match(image, prefs.x(5+i,w), prefs.y(5+i,h),rw,rh);
            }
            lastScores=String.format(java.util.Locale.US,"L %.2f %.2f %.2f / R %.2f %.2f %.2f",
                    left[0],left[1],left[2],right[0],right[1],right[2]);
            DecisionEngine.Action action = engine.observe(left,right,prefs.threshold());
            if (action == DecisionEngine.Action.STOP) {
                pause("블록 인식 실패 14회 · 자동 정지"); return;
            }
            if (action == DecisionEngine.Action.NONE) return;
            final DecisionEngine.Action committedAction = action;
            final int actionSession = sessionToken;
            final int targetX = prefs.x(action == DecisionEngine.Action.UP ? 0 : 1, w);
            final int targetY = prefs.y(action == DecisionEngine.Action.UP ? 0 : 1, h);
            if (!waitingForGesture.compareAndSet(false, true)) return;
            StairAccessibilityService touch = StairAccessibilityService.get();
            if (touch == null) { waitingForGesture.set(false); pause("접근성 서비스 연결 해제됨"); return; }
            // v1.2: avoid firing a new gesture before the previous one has completed.
            ui.post(() -> touch.tap(targetX, targetY, success -> work.post(() -> {
                // Patch 1: serialized acknowledgements; a stopped session cannot resume
                // through a delayed callback from its previous gesture.
                if (actionSession == sessionToken && running) {
                    if (success) {
                        if (engine != null) engine.committed(committedAction);
                        lastActionMs = SystemClock.uptimeMillis();
                        successfulSteps++;
                        status = (committedAction == DecisionEngine.Action.UP ? "등산" : "방향 전환") + " 완료";
                    } else pause("터치 실패 · 자동 정지");
                }
                waitingForGesture.set(false);
            })));
        } catch (Exception e) {
            pause("화면 인식 오류: " + e.getClass().getSimpleName());
        } finally { if (image != null) image.close(); }
    }

    private File templateFile() { return new File(getFilesDir(), "learned_stair.bin"); }
    /** Capture exactly one frozen frame for 9-point offline calibration. */
    public void requestSnapshot(Consumer<Bitmap> onReady) {
        if (reader == null) { toast("화면 공유가 먼저 필요합니다"); return; }
        pause("보정용 화면 고정 중");
        snapshotRequest = onReady;
    }
    private Bitmap renderFrame(Image image) {
        int w=image.getWidth(), h=image.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer b = plane.getBuffer();
        int stride = plane.getRowStride(), pixelStride = plane.getPixelStride();
        int[] pixels = new int[w];
        for (int y=0; y<h; y++) {
            int off=y*stride;
            for (int x=0; x<w; x++) {
                int pos=off+x*pixelStride;
                int r=b.get(pos)&255,g=b.get(pos+1)&255,bl=b.get(pos+2)&255;
                pixels[x]=Color.rgb(r,g,bl);
            }
            bitmap.setPixels(pixels,0,w,0,y,w,1);
        }
        return bitmap;
    }
    public boolean learnTemplate(Bitmap image, int x, int y) {
        if (image == null || !prefs.sameResolution(image.getWidth(),image.getHeight())) {
            status="보정 실패: 화면 크기가 다릅니다"; return false;
        }
        try {
            matcher.learn(image,x,y,templateFile());
            prefs.markCalibrated(true);
            status="9개 위치 + 계단 이미지 학습 완료";
            return true;
        } catch (Exception ex) {
            prefs.markCalibrated(false);
            status="보정 실패: " + ex.getMessage();
            return false;
        }
    }
    public boolean startRunning() {
        if (projection == null || display == null) { toast("화면 공유 권한부터 허용하세요"); return false; }
        if (StairAccessibilityService.get() == null) { toast("접근성 서비스를 켜주세요"); return false; }
        if (!prefs.calibrated() || !matcher.ready()) { toast("먼저 게임에서 보정하세요"); return false; }
        if (!prefs.sameResolution(captureW, captureH)) { toast("화면 크기가 달라졌습니다. 다시 보정하세요"); return false; }
        running = false;
        sessionToken++;
        engine = new DecisionEngine(prefs.initialLeft());
        successfulSteps = 0;
        lastActionMs = SystemClock.uptimeMillis();
        waitingForGesture.set(false);
        running = true;
        status = "자동 플레이 중";
        return true;
    }
    public void pause(String reason) { running = false; sessionToken++; status = reason; }
    public boolean isRunning() { return running; }
    private void toast(String message) { ui.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show()); }
    @Override public void onDestroy() {
        running = false;
        sessionToken++;
        snapshotRequest = null;
        if (reader != null) { reader.setOnImageAvailableListener(null, null); reader.close(); reader = null; }
        if (display != null) { display.release(); display = null; }
        if (projection != null) {
            if (projectionCallback != null) projection.unregisterCallback(projectionCallback);
            projection.stop(); projection = null;
        }
        if (thread != null) thread.quitSafely();
        instance = null;
        status = "화면 공유 중지됨";
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
