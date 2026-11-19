package io.stairpilot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
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
    public static volatile float recentStepsPerSecond = 0f;

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
    private final VisualMatcher eventMatcher = new VisualMatcher();
    private PromptGate promptGate = new PromptGate(3);
    private long missingEventSince = 0L;
    private long lastFrameTimestamp = 0L;
    private long lastTelemetryMs = 0L;
    private long speedWindowStart = 0L;
    private int speedWindowCount = 0;
    private volatile DecisionEngine engine;
    private volatile int sessionToken = 0;
    private Prefs prefs;
    private volatile long lastActionMs = 0;
    private int captureW, captureH;
    private RunGuard guard;
    private volatile long latestFrameMs = 0L;
    private volatile long gestureSinceMs = 0L;
    private volatile boolean restActive = false;
    private long lastHealthMs = 0L;
    private int batteryPercent = 100;
    private int thermalStatus = 0;
    public static volatile String health = "기기 보호 대기";
    public static volatile String gaugeStatus = "시간 게이지 보정 전";
    public static volatile int currentDelayMs = 0;
    private float smoothedGauge = -1f;
    private boolean reliableGauge = false;
    private long lastGaugeScanMs = 0L;



    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = new Prefs(this);
        ui = new Handler(getMainLooper());
        thread = new HandlerThread("stairpilot-capture");
        thread.start();
        work = new Handler(thread.getLooper());
        work.postDelayed(new Runnable() {
            @Override public void run() {
                try { checkHealth(); }
                finally { if (thread != null && thread.isAlive()) work.postDelayed(this, 1700); }
            }
        }, 1700);

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("capture", "화면 인식 중", NotificationManager.IMPORTANCE_LOW));
    }

    private Notification notification() {
        Intent stop = new Intent(this, ProjectionService.class).setAction(ACTION_STOP);
        PendingIntent pi = PendingIntent.getService(this, 41, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "capture")
                .setContentTitle("무계매크로 · 화면 분석 중")
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
            display = projection.createVirtualDisplay("무계매크로", captureW, captureH,
                    getResources().getDisplayMetrics().densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, work);
            matcher.load(templateFile());
            eventMatcher.load(eventTemplateFile());
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

    /** Periodic safety watchdog runs even when Android sends no more video frames. */
    private void checkHealth() {
        if (!running || guard == null) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastHealthMs >= 4500L) {
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (level >= 0 && scale > 0) batteryPercent=100*level/scale;
            }
            PowerManager pm=getSystemService(PowerManager.class);
            thermalStatus = pm == null ? 0 : pm.getCurrentThermalStatus();
            lastHealthMs = now;
        }
        PowerManager power=getSystemService(PowerManager.class);
        boolean interactive=power == null || power.isInteractive();
        RunGuard.State result=guard.evaluate(now,latestFrameMs,gestureSinceMs,batteryPercent,thermalStatus,interactive);
        health="배터리 "+batteryPercent+"% · 열 상태 "+thermalStatus+" · 보호 "+result.name();
        if(result == RunGuard.State.COOLDOWN) {
            Diagnostics.record(now,"장시간 제한 도달 · 안전 휴식 필요");
            pause("30분 실행 종료 · 기기 식힌 뒤 수동 재시작");
            return;
        }
        if(result != RunGuard.State.OK) {
            Diagnostics.record(now,"보호 정지: "+result.name());
            pause("안전 정지: "+result.name()+" · 다시 시작은 수동");
        }
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
            if (running) latestFrameMs=SystemClock.uptimeMillis();
            if (!running || waitingForGesture.get() || engine == null || restActive) return;
            StairAccessibilityService serviceCheck = StairAccessibilityService.get();
            if (serviceCheck == null) { pause("접근성 서비스 연결 안 됨"); return; }
            if (!serviceCheck.isGameForeground()) { pause("게임 외 화면 감지 · 안전 정지"); return; }
            long frameTimestamp = image.getTimestamp();
            // Never evaluate the same captured frame twice.
            if (frameTimestamp > 0 && frameTimestamp <= lastFrameTimestamp) return;
            if (frameTimestamp > 0) lastFrameTimestamp = frameTimestamp;
            int w = image.getWidth(), h = image.getHeight();
            if (!prefs.sameResolution(w,h)) { pause("화면 크기 불일치 · 다시 보정"); return; }
            long now = SystemClock.uptimeMillis();
            if (prefs.adaptiveSpeed() && prefs.sameGaugeResolution(w,h)
                    && now-lastGaugeScanMs>=90L) {
                lastGaugeScanMs=now;
                Image.Plane plane=image.getPlanes()[0];
                final ByteBuffer bytes=plane.getBuffer();
                final int rowStride=plane.getRowStride();
                final int pxStride=plane.getPixelStride();
                GaugeDetector.Reading reading=GaugeDetector.read((xx,yy)->{
                    int pos=yy*rowStride+xx*pxStride;
                    int red=bytes.get(pos)&255;
                    int green=bytes.get(pos+1)&255;
                    int blue=bytes.get(pos+2)&255;
                    return (red<<16)|(green<<8)|blue;
                },w,h,prefs.gaugeX1(w),prefs.gaugeY1(h),prefs.gaugeX2(w),prefs.gaugeY2(h));
                if(reading.valid) {
                    // Refilled bar may jump upward; quickly return to safe pacing.
                    smoothedGauge = smoothedGauge < 0f ? reading.fill
                            : .35f*smoothedGauge + .65f*reading.fill;
                    reliableGauge=true;
                    gaugeStatus="시간 게이지 약 "+Math.round(smoothedGauge*100f)+"%";
                } else {
                    reliableGauge=false;
                    gaugeStatus="시간 게이지 미인식 · 입력 횟수 기반 보조 가속";
                }
            }
            int targetDelay = prefs.adaptiveSpeed()
                    ? GaugePacer.delayMs(prefs.delayMs(),successfulSteps,
                        smoothedGauge,reliableGauge,0)
                    : prefs.delayMs();
            currentDelayMs=targetDelay;
            if (now-lastActionMs < targetDelay) return;
            // First probe the two nearest cells; scanning distant cells is expensive.
            // Only inspect the 3-step path when the first pair is ambiguous/weak.
            int rw = Math.max(42, Math.round(64f*w/720f));
            int rh = Math.max(26, Math.round(34f*h/1280f));
            float[] left = new float[3], right = new float[3];
            float threshold = prefs.threshold();
            left[0] = matcher.match(image,prefs.x(2,w),prefs.y(2,h),rw,rh);
            right[0] = matcher.match(image,prefs.x(5,w),prefs.y(5,h),rw,rh);
            boolean l = left[0] >= threshold, r = right[0] >= threshold;
            boolean needLookahead = l == r || Math.abs(left[0]-threshold) < .055f
                    || Math.abs(right[0]-threshold) < .055f;
            if (needLookahead) {
                for(int i=1;i<3;i++) {
                    left[i] = matcher.match(image,prefs.x(2+i,w),prefs.y(2+i,h),rw,rh);
                    right[i] = matcher.match(image,prefs.x(5+i,w),prefs.y(5+i,h),rw,rh);
                }
            }
            if (now - lastTelemetryMs >= 300L) {
                lastScores=String.format(java.util.Locale.US,"L %.2f %.2f %.2f / R %.2f %.2f %.2f",
                        left[0],left[1],left[2],right[0],right[1],right[2]);
                lastTelemetryMs = now;
            }
            ModeProfile mode = prefs.mode();
            // Event screens may require a learned continue/retry button instead of stairs.
            // Only tap a user-chosen template, with a 3-frame gate and max 3 prompts/session.
            boolean recoverableWeakTile =
                    (left[0] >= threshold - .035f && left[1] >= threshold && left[2] >= threshold)
                    || (right[0] >= threshold - .035f && right[1] >= threshold && right[2] >= threshold);
            if (mode == ModeProfile.EVENT && !l && !r && !recoverableWeakTile) {
                if (missingEventSince == 0L) missingEventSince = now;
                if (prefs.eventAutoContinue() && prefs.eventPromptCalibrated()
                        && prefs.sameEventResolution(w,h) && eventMatcher.ready()) {
                    float score = eventMatcher.match(image,prefs.eventX(w),prefs.eventY(h),rw,rh);
                    if (promptGate.observe(score, .94f, now)) {
                        dispatchEventTap(w,h);
                        return;
                    }
                }
                if (now-missingEventSince < 3500L) {
                    status = "이벤트 화면 분석 중 (최대 3.5초)";
                    return;
                }
                pause("이벤트 화면 미인식 · 안전 정지 (수동 진행/재보정 필요)");
                return;
            } else missingEventSince = 0L;

            boolean turbo = mode.fastConfirmation || (prefs.adaptiveSpeed()
                    && reliableGauge && smoothedGauge < .30f);
            DecisionEngine.Action action = engine.observe(left,right,threshold,turbo);
            if (action == DecisionEngine.Action.STOP) {
                pause("계단 인식 실패 · 자동 정지"); return;
            }
            if (action == DecisionEngine.Action.NONE) return;
            final int cautiousDelay = AdaptivePacer.extraDelayMs(left[0],right[0],threshold,thermalStatus);
            final DecisionEngine.Action committedAction = action;
            final int actionSession = sessionToken;
            final int targetX = prefs.x(action == DecisionEngine.Action.UP ? 0 : 1, w);
            final int targetY = prefs.y(action == DecisionEngine.Action.UP ? 0 : 1, h);
            if (!waitingForGesture.compareAndSet(false, true)) return;
            gestureSinceMs=SystemClock.uptimeMillis();
            StairAccessibilityService touch = StairAccessibilityService.get();
            if (touch == null) { waitingForGesture.set(false); pause("접근성 서비스 연결 해제됨"); return; }
            ui.post(() -> touch.tap(targetX, targetY, prefs.tapDurationMs(), success -> work.post(() -> {
                if (actionSession == sessionToken && running) {
                    if (success) {
                        if (engine != null) engine.committed(committedAction);
                        lastActionMs = SystemClock.uptimeMillis() + cautiousDelay;
                        successfulSteps++;
                        speedWindowCount++;
                        long elapsed = lastActionMs - speedWindowStart;
                        if (elapsed >= 1200L) {
                            recentStepsPerSecond = speedWindowCount * 1000f / Math.max(1L,elapsed);
                            speedWindowCount = 0;
                            speedWindowStart = lastActionMs;
                        }
                        status = mode.korean + " · " + (committedAction == DecisionEngine.Action.UP ? "등산" : "방향 전환");
                    } else pause("터치 실패 · 자동 정지");
                }
                gestureSinceMs=0L;
                waitingForGesture.set(false);
            })));
        } catch (Exception e) {
            pause("화면 인식 오류: " + e.getClass().getSimpleName());
        } finally { if (image != null) image.close(); }
    }

    private void dispatchEventTap(int w, int h) {
        if (!waitingForGesture.compareAndSet(false,true)) return;
        gestureSinceMs=SystemClock.uptimeMillis();
        StairAccessibilityService touch = StairAccessibilityService.get();
        if (touch == null) { waitingForGesture.set(false); pause("접근성 서비스 중단"); return; }
        final int token = sessionToken;
        ui.post(() -> touch.tap(prefs.eventX(w), prefs.eventY(h), 55, success -> work.post(() -> {
            if (token == sessionToken && running) {
                if (success) {
                    long now = SystemClock.uptimeMillis();
                    promptGate.committed(now);
                    engine = new DecisionEngine(prefs.initialLeft());
                    lastActionMs = now + 650L; // transition animation delay
                    missingEventSince = 0L;
                    status = "학습한 이벤트 버튼 자동 클릭 (" + promptGate.count() + "/3)";
                    if(promptGate.exhausted()) status += " · 다음부터 수동";
                } else pause("이벤트 버튼 입력 취소됨");
            }
            gestureSinceMs=0L;
            waitingForGesture.set(false);
        })));
    }

    private File eventTemplateFile() { return new File(getFilesDir(), "event_prompt_v13.bin"); }
    /** Learn a single visibly-labelled event continue/retry button from a frozen image. */
    public boolean learnEventPrompt(Bitmap image, int x, int y) {
        if (image == null || !prefs.sameResolution(image.getWidth(),image.getHeight())) {
            status="이벤트 버튼 학습 실패: 해상도 불일치"; return false;
        }
        try {
            eventMatcher.learn(image,x,y,eventTemplateFile());
            prefs.saveEventPoint(x,y,image.getWidth(),image.getHeight());
            prefs.markEventPromptCalibrated(true);
            status = "이벤트 버튼 템플릿 학습 완료 · 설정에서 자동 누르기 활성화";
            return true;
        } catch(Exception ex) {
            prefs.markEventPromptCalibrated(false);
            status = "이벤트 버튼 학습 실패: " + ex.getMessage();
            return false;
        }
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
        if (waitingForGesture.get()) { toast("이전 터치 종료를 확인한 뒤 다시 시작하세요"); return false; }
        running = false;
        sessionToken++;
        engine = new DecisionEngine(prefs.initialLeft());
        promptGate = new PromptGate(3);
        guard = new RunGuard(SystemClock.uptimeMillis(), prefs.extendedSession());
        latestFrameMs=SystemClock.uptimeMillis();
        gestureSinceMs=0L;
        restActive=false;
        lastHealthMs=0L;
        smoothedGauge=-1f;
        reliableGauge=false;
        lastGaugeScanMs=0L;
        gaugeStatus=prefs.gaugeCalibrated()?"시간 게이지 인식 준비":"시간 게이지 보정 전";
        Diagnostics.record(SystemClock.uptimeMillis(),"시작 · "+prefs.mode().name()+" · 장시간="+guard.extended());
        missingEventSince = 0L;
        lastFrameTimestamp = 0L;
        lastTelemetryMs = 0L;
        recentStepsPerSecond = 0f;
        speedWindowStart = SystemClock.uptimeMillis();
        speedWindowCount = 0;
        successfulSteps = 0;
        lastActionMs = SystemClock.uptimeMillis();
        running = true;
        status = prefs.mode().korean + " · 자동 플레이 중";
        return true;
    }
    public void pause(String reason) {
        if (running) Diagnostics.record(SystemClock.uptimeMillis(),"중지: "+reason);
        running = false;
        restActive=false;
        sessionToken++;
        status = reason;
    }
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
