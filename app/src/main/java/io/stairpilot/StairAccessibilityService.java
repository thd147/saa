package io.stairpilot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.function.Consumer;

/** Accessibility overlay: no SYSTEM_ALERT_WINDOW permission or root required. */
public final class StairAccessibilityService extends AccessibilityService {
    private static volatile StairAccessibilityService instance;
    public static StairAccessibilityService get() { return instance; }
    private WindowManager windowManager;
    private View toolbar;
    private FrameLayout wizard;
    private TextView hint;
    private Bitmap frozenImage;
    private int step = 0;
    private boolean eventWizard = false;
    private boolean gaugeWizard = false;
    private int gaugeFirstX, gaugeFirstY;
    private Handler ui;
    private final String[] prompts = {
        "1/9: 올라가기 버튼의 중앙을 터치",
        "2/9: 방향 전환 버튼의 중앙을 터치",
        "3/9: 캐릭터 기준 왼쪽 1칸 계단 중앙",
        "4/9: 왼쪽 2칸째 계단 중앙",
        "5/9: 왼쪽 3칸째 계단 중앙",
        "6/9: 캐릭터 기준 오른쪽 1칸 계단 중앙",
        "7/9: 오른쪽 2칸째 계단 중앙",
        "8/9: 오른쪽 3칸째 계단 중앙",
        "9/9: 현재 보이는 계단의 테두리/무늬 부분을 터치 (이미지 학습)"
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = getSystemService(WindowManager.class);
        ui = new Handler(getMainLooper());
        showToolbar();
    }
    private volatile String lastForeground = "";
    public boolean isGameForeground() {
        return lastForeground.isEmpty() || "com.nflystudio.InfiniteStaircase".equals(lastForeground);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg=event.getPackageName().toString();
        // Own overlay transitions must not invalidate the current game window.
        if (getPackageName().equals(pkg)) return;
        lastForeground=pkg;
        ProjectionService service=ProjectionService.get();
        if(service != null && service.isRunning() && !isGameForeground())
            service.pause("게임 밖 앱 전환 감지 · 자동 조작 정지");
    }
    @Override public void onInterrupt() { }

    private int dp(float val) { return Math.round(val * getResources().getDisplayMetrics().density); }
    private Button makeButton(String label, LinearLayout column, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setPadding(dp(4), 0, dp(4), 0);
        column.addView(b, new LinearLayout.LayoutParams(dp(100), dp(45)));
        b.setOnClickListener(click);
        return b;
    }
    public void showToolbar() {
        if (toolbar != null || windowManager == null) return;
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(3),dp(3),dp(3),dp(3));
        bar.setBackgroundColor(0xB9222930);
        makeButton("계단 보정", bar, v -> startWizard());
        makeButton("이벤트 버튼 학습", bar, v -> startEventWizard());
        makeButton("빨간 게이지 보정", bar, v -> startGaugeWizard());
        makeButton("모드 변경", bar, v -> {
            Prefs prefs = new Prefs(this);
            ModeProfile[] all = ModeProfile.values();
            int i = (prefs.mode().ordinal()+1) % all.length;
            ProjectionService p = ProjectionService.get();
            if(p != null && p.isRunning()) p.pause("모드 변경 · 재시작 필요");
            prefs.setMode(all[i]);
            say("현재 모드: " + all[i].korean);
        });
        makeButton("▶ 시작", bar, v -> {
            ProjectionService p = ProjectionService.get();
            if (p == null) { say("먼저 앱에서 화면 공유를 켜세요"); return; }
            if (p.startRunning()) say("자동 플레이 시작 · ■ 로 중지");
        });
        makeButton("■ 정지", bar, v -> {
            ProjectionService p = ProjectionService.get();
            if (p != null) p.pause("사용자 중지");
            say("중지됨");
        });
        makeButton("메뉴 숨기기", bar, v -> hideToolbar());
        TextView liveStatus=new TextView(this);
        liveStatus.setTextSize(10); liveStatus.setTextColor(Color.WHITE);
        liveStatus.setPadding(dp(3),dp(3),dp(3),dp(3));
        bar.addView(liveStatus);
        Runnable[] refresher=new Runnable[1];
        refresher[0]=() -> {
            if (toolbar == bar) {
                liveStatus.setText(new Prefs(this).mode().korean + "\n" + ProjectionService.status
                        + "\n" + ProjectionService.lastScores);
                ui.postDelayed(refresher[0],600);
            }
        };
        ui.postDelayed(refresher[0],600);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(dp(106), -2,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.x = 0; lp.y = dp(100);
        windowManager.addView(bar, lp);
        toolbar = bar;
    }
    private void hideToolbar() {
        if (toolbar != null) {
            windowManager.removeView(toolbar);
            toolbar = null;
        }
    }
    private Point screenSize() {
        if (Build.VERSION.SDK_INT >= 30) {
            Rect r = windowManager.getMaximumWindowMetrics().getBounds();
            return new Point(r.width(), r.height());
        }
        Point p = new Point();
        windowManager.getDefaultDisplay().getRealSize(p);
        return p;
    }
    private void startWizard() {
        eventWizard = false;
        gaugeWizard = false;
        new Prefs(this).markCalibrated(false);
        startFrozenWizard();
    }
    private void startEventWizard() {
        eventWizard = true;
        gaugeWizard = false;
        new Prefs(this).markEventPromptCalibrated(false);
        startFrozenWizard();
    }
    private void startGaugeWizard() {
        eventWizard=false;
        gaugeWizard=true;
        startFrozenWizard();
    }
    private void startFrozenWizard() {
        ProjectionService service = ProjectionService.get();
        if (service == null) { say("앱에서 화면 공유를 먼저 시작하세요"); return; }
        service.pause("보정 준비 중");
        hideToolbar();
        step=0;
        // Capture with the control overlay removed to avoid training on our own UI.
        ui.postDelayed(() -> {
            ProjectionService p=ProjectionService.get();
            if(p != null) p.requestSnapshot(this::showFrozenWizard);
        },300);
        ui.postDelayed(() -> {
            if (wizard==null) { showToolbar(); say("화면을 가져오지 못했습니다. 화면 공유 범위를 확인하세요"); }
        },3500);
    }
    private void showFrozenWizard(Bitmap screenshot) {
        if (windowManager == null || wizard != null) return;
        hideToolbar();
        frozenImage=screenshot;
        wizard=new FrameLayout(this);
        ImageView backdrop=new ImageView(this);
        backdrop.setImageBitmap(screenshot);
        backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        wizard.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        hint=new TextView(this);
        hint.setText(gaugeWizard ? "1/2: 상단 빨간 시간 게이지 바의 안쪽 왼쪽 끝을 터치하세요.\n테두리와 숫자는 피하세요."
                : eventWizard ? "이벤트 화면에서 '다시하기/계속' 등 안전한 버튼 중앙 1곳을 누르세요.\n결제·광고·보상 선택은 지정하지 마세요."
                : prompts[0] + "\n고정된 화면에서 9곳을 지정하세요 (게임이 뒤에서 진행돼도 괜찮습니다)");
        hint.setTextSize(16);
        hint.setTextColor(Color.WHITE);
        hint.setBackgroundColor(0xDD151515);
        hint.setPadding(dp(20),dp(12),dp(20),dp(12));
        wizard.addView(hint,new FrameLayout.LayoutParams(-1,dp(128),gaugeWizard ? Gravity.BOTTOM : Gravity.TOP));
        Button cancel = new Button(this);
        cancel.setText("취소"); cancel.setAllCaps(false);
        cancel.setOnClickListener(v -> closeWizard());
        FrameLayout.LayoutParams cancelPosition = new FrameLayout.LayoutParams(dp(76),dp(49),Gravity.TOP|Gravity.RIGHT);
        wizard.addView(cancel,cancelPosition);
        wizard.setOnTouchListener((v,event) -> {
            if(event.getAction()==MotionEvent.ACTION_UP && frozenImage!=null && wizard!=null) {
                int sx = Math.round(event.getX() * frozenImage.getWidth() / Math.max(1,wizard.getWidth()));
                int sy = Math.round(event.getY() * frozenImage.getHeight() / Math.max(1,wizard.getHeight()));
                onWizardTap(sx,sy);
            }
            return true;
        });
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.LEFT;
        windowManager.addView(wizard,lp);
    }
    private void onWizardTap(int x, int y) {
        if (frozenImage == null) return;
        if (gaugeWizard) {
            if(step==0) {
                gaugeFirstX=x; gaugeFirstY=y; step=1;
                hint.setText("2/2: 같은 게이지 바의 안쪽 오른쪽 끝을 터치하세요.");
                return;
            }
            boolean saved;
            try {
                new Prefs(this).saveGaugeBounds(gaugeFirstX,gaugeFirstY,x,y,
                        frozenImage.getWidth(),frozenImage.getHeight());
                saved=true;
            } catch (IllegalArgumentException ex) { saved=false; }
            if(wizard!=null) {windowManager.removeView(wizard); wizard=null;}
            frozenImage=null; gaugeWizard=false; showToolbar();
            say(saved?"빨간 시간 게이지 보정 완료":"보정 실패 · 게이지 양 끝을 같은 높이에서 선택하세요");
            return;
        }
        if (eventWizard) {
            ProjectionService p = ProjectionService.get();
            boolean learned = p != null && p.learnEventPrompt(frozenImage,x,y);
            if(wizard != null) { windowManager.removeView(wizard); wizard = null; }
            frozenImage = null;
            showToolbar();
            say(learned ? "버튼 학습 완료! 앱에서 이벤트 자동 누르기 켜세요"
                    : "버튼 학습 실패 · 무늬나 글자가 보이는 버튼을 선택하세요");
            return;
        }
        if (step < 8) {
            new Prefs(this).savePoint(step, x, y, frozenImage.getWidth(), frozenImage.getHeight());
            step++;
            hint.setText(prompts[step]);
        } else {
            ProjectionService p=ProjectionService.get();
            boolean learned=p != null && p.learnTemplate(frozenImage,x,y);
            if (wizard != null) { windowManager.removeView(wizard); wizard=null; }
            frozenImage=null;
            showToolbar();
            say(learned ? "계단 이미지 학습 완료! 게임에서 ▶ 시작하세요" :
                    "학습 실패: 앱 상태를 확인하고 다시 보정하세요");
        }
    }
    private void closeWizard() {
        if (wizard != null) { windowManager.removeView(wizard); wizard = null; }
        frozenImage=null;
        showToolbar();
    }
    private void say(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    /** Send a single gesture, report success after system acknowledgement. */
    public void tap(int x, int y, Consumer<Boolean> done) {
        tap(x, y, 35, done);
    }
    public void tap(int x, int y, int durationMs, Consumer<Boolean> done) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0,
                        Math.max(15, Math.min(95, durationMs)))).build();
        boolean sent = dispatchGesture(gesture, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription g) { done.accept(true); }
            @Override public void onCancelled(GestureDescription g) { done.accept(false); }
        }, ui);
        if (!sent) done.accept(false);
    }
    @Override public void onDestroy() {
        if (wizard != null) { windowManager.removeView(wizard); wizard = null; }
        hideToolbar();
        frozenImage=null;
        instance = null;
        ProjectionService p = ProjectionService.get();
        if (p != null) p.pause("접근성 서비스 종료됨");
        super.onDestroy();
    }
}
