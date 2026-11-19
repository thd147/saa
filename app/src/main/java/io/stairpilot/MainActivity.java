package io.stairpilot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** Control panel. All recognition and gestures run on the user's phone. */
public final class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 501;
    private final Handler ui = new Handler();
    private TextView status, delayLabel, thresholdLabel, orientationLabel;
    private Prefs prefs;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (status != null) {
                status.setText(ProjectionService.status + "\n성공 입력: " + ProjectionService.successfulSteps + "회");
            }
            ui.postDelayed(this, 700);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        ScrollView outer = new ScrollView(this);
        outer.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(19), dp(24), dp(19), dp(30));
        root.setBackgroundColor(0xfff5f7fa);
        outer.addView(root);
        setContentView(outer);
        TextView heading = text("StairPilot · 무한의 계단", 23, true, root);
        heading.setTextColor(0xff17233b);
        text("로컬 화면 분석 · 접근성 터치 · 계단 3칸 탐색", 14, false, root);
        status = text("준비 중", 16, true, root);
        status.setTextColor(0xff12656b);
        status.setPadding(0, dp(16), 0, dp(18));
        button(root, "① 접근성 서비스 켜기", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(root, "② 화면 공유 시작 (전체 화면 선택)", v -> requestCapture());
        button(root, "③ 무한의 계단 열기", v -> openGame());
        button(root, "④ 플로팅 메뉴 표시", v -> {
            StairAccessibilityService s = StairAccessibilityService.get();
            if (s == null) toast("설정에서 접근성 서비스를 먼저 켜세요");
            else s.showToolbar();
        });
        button(root, "■ 화면 공유 / 자동 실행 완전 종료", v -> {
            Intent i = new Intent(this, ProjectionService.class).setAction(ProjectionService.ACTION_STOP);
            startService(i);
            ProjectionService.status = "중지 명령 전송됨";
        });
        text("보정 방법", 18, true, root).setPadding(0, dp(22), 0, dp(5));
        text("게임 화면이 보이는 상태에서 오른쪽 위 '보정'을 누르세요. 올라가기·회전 버튼을 먼저 지정하고, 왼쪽 1~3칸과 오른쪽 1~3칸의 계단 중심을 차례로 터치합니다. 마지막으로 현재 보이는 계단 블록 안의 테두리/무늬가 들어간 곳을 학습시킵니다. 총 9번 터치입니다.", 14, false, root);
        text("설정", 18, true, root).setPadding(0, dp(22), 0, dp(5));
        delayLabel = text("입력 대기", 15, false, root);
        SeekBar delay = new SeekBar(this);
        delay.setMax(70);
        delay.setProgress((prefs.delayMs()-90)/10);
        root.addView(delay);
        delayLabel.setText("터치 후 대기: " + prefs.delayMs() + "ms (90~790ms)");
        delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser) {
                if(fromUser) { prefs.setDelay(90 + p*10); delayLabel.setText("터치 후 대기: " + prefs.delayMs() + "ms"); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        thresholdLabel = text("인식 임계값", 15, false, root);
        SeekBar threshold = new SeekBar(this);
        threshold.setMax(45);
        threshold.setProgress(Math.round((prefs.threshold()-0.50f)*100));
        root.addView(threshold);
        thresholdLabel.setText("계단 인식 임계값: " + Math.round(prefs.threshold()*100) + "%");
        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser) {
                if(fromUser) { prefs.setThreshold((50+p)/100f); thresholdLabel.setText("계단 인식 임계값: " + (50+p) + "%"); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        orientationLabel = text("", 15, false, root);
        updateFacingLabel();
        button(root,"첫 시작 방향 바꾸기",v -> {
            prefs.setInitialLeft(!prefs.initialLeft());
            updateFacingLabel();
        });
        text("주의: 게임을 실제로 시작한 뒤 ▶ 시작을 누르세요. 첫 방향은 화면 속 캐릭터가 바라보는 방향과 같게 맞추세요. 실패 시 ■ 정지하고, 화면을 회전했거나 스킨·테마가 달라졌다면 다시 보정하세요. Android 14 이상에서는 화면 공유할 때 '전체 화면'을 선택해야 앱 전환에도 프레임이 유지됩니다. 휴대폰에 따라 게임의 화면 캡처가 차단될 수 있습니다.", 13, false, root).setPadding(0, dp(15), 0, 0);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }
    private int dp(float d) {return Math.round(d * getResources().getDisplayMetrics().density);}
    private TextView text(String value, int size, boolean bold, LinearLayout parent) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextColor(0xff354052); v.setTextSize(size);
        if(bold) v.setTypeface(null, 1);
        parent.addView(v, new LinearLayout.LayoutParams(-1, -2));
        return v;
    }
    private void button(LinearLayout parent,String name, View.OnClickListener onClick) {
        Button b = new Button(this); b.setText(name); b.setAllCaps(false);
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(51));
        p.topMargin = dp(5); parent.addView(b,p);
    }
    private void updateFacingLabel() { orientationLabel.setText("시작 시 캐릭터 방향: " + (prefs.initialLeft()?"왼쪽":"오른쪽")); }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    private void requestCapture() {
        MediaProjectionManager m = getSystemService(MediaProjectionManager.class);
        startActivityForResult(m.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }
    private void openGame() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.nflystudio.InfiniteStaircase");
        if (launch != null) startActivity(launch);
        else toast("게임 설치가 확인되지 않았습니다. 게임을 직접 열어도 됩니다.");
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==REQUEST_CAPTURE) {
            if(result!=RESULT_OK || data==null){toast("화면 공유 권한이 취소되었습니다."); return;}
            Intent service = new Intent(this,ProjectionService.class);
            service.setAction(ProjectionService.ACTION_START);
            service.putExtra(ProjectionService.EXTRA_RESULT_CODE,result);
            service.putExtra(ProjectionService.EXTRA_RESULT_DATA,data);
            startForegroundService(service);
            toast("화면 공유 허용됨. 이제 게임을 열고 플로팅 메뉴에서 보정하세요.");
        }
    }
    @Override protected void onStart(){super.onStart(); ui.removeCallbacks(refresh);ui.post(refresh);}
    @Override protected void onStop(){ui.removeCallbacks(refresh);super.onStop();}
}
