package io.stairpilot;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
    private TextView status, delayLabel, thresholdLabel, orientationLabel, modeLabel;
    private Prefs prefs;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (status != null) {
                status.setText(ProjectionService.status + "\n성공 입력: " + ProjectionService.successfulSteps
                        + "회 · 최근 약 " + String.format(java.util.Locale.US, "%.1f", ProjectionService.recentStepsPerSecond)
                        + "회/초 (실측 추정)\n" + ProjectionService.health
                        + "\n" + ProjectionService.gaugeStatus + " · 목표 대기 "
                        + ProjectionService.currentDelayMs + "ms");
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
        text("v1.4.0 · 장시간 보호 · 속도 적응 · 진단 · 새 계단 아이콘", 14, false, root);
        text("권한 안내: 이 앱은 게임 화면을 읽고 접근성 제스처를 보냅니다. 화면 픽셀은 기기에서만 분석하고 네트워크로 보내지 않습니다. 보안 경고를 무시하거나 우회하지 마세요.", 13, false, root);
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
        text("모드 선택 · 실행 중 변경하면 안전 정지됩니다", 18, true, root).setPadding(0, dp(22), 0, dp(5));
        modeLabel = text("현재: " + prefs.mode().korean, 15, true, root);
        text("PVP는 강한 단일 계단 신호에서만 1프레임 확정. 이벤트 특수 규칙과 협동전 몬스터는 별도 인식하지 못합니다.", 13, false, root);
        delayLabel = text("입력 대기", 15, false, root);
        SeekBar delay = new SeekBar(this);
        delay.setMax(325);
        delay.setProgress(prefs.delayMs()-25);
        root.addView(delay);
        delayLabel.setText("터치 완료 후 대기: " + prefs.delayMs() + "ms (25~350ms)");
        delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser) {
                if(fromUser) { prefs.setDelay(25 + p); delayLabel.setText("터치 완료 후 대기: " + prefs.delayMs() + "ms"); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        button(root,"일반 · 균형",v -> selectMode(ModeProfile.NORMAL,delay));
        button(root,"PVP · 빠름 (고속 확정)",v -> selectMode(ModeProfile.PVP,delay));
        button(root,"이벤트 · 안정 (일반 계단만)",v -> selectMode(ModeProfile.EVENT,delay));
        button(root,"4인 협동 · 정밀 (계단만)",v -> selectMode(ModeProfile.COOP,delay));
        text("상단 빨간 시간 게이지 · 가변 가속",18,true,root).setPadding(0,dp(18),0,dp(4));
        text("게임 안에서 플로팅 메뉴 → 빨간 게이지 보정: 시간 바의 안쪽 왼쪽 끝과 안쪽 오른쪽 끝을 선택하세요. 미인식 시에는 입력 횟수 기준으로 보조 가속합니다. 모든 입력은 Android 제스처 완료를 확인하며, 불확실한 계단은 감속합니다.",13,false,root);
        Button adaptiveToggle=new Button(this);
        adaptiveToggle.setAllCaps(false);
        adaptiveToggle.setText(prefs.adaptiveSpeed()?"가변 가속: 켜짐":"가변 가속: 꺼짐");
        root.addView(adaptiveToggle);
        adaptiveToggle.setOnClickListener(v->{
            ProjectionService runningService=ProjectionService.get();
            if(runningService!=null && runningService.isRunning()) runningService.pause("가속 설정 변경 · 재시작");
            prefs.setAdaptiveSpeed(!prefs.adaptiveSpeed());
            adaptiveToggle.setText(prefs.adaptiveSpeed()?"가변 가속: 켜짐":"가변 가속: 꺼짐");
        });
        text("장시간 안정성 · 24시간 무오류 보장 아님", 18, true, root).setPadding(0, dp(18), 0, dp(4));
        text("기본 세션은 최대 90분. 장시간 옵션을 켜도 30분마다 안전 정지하고 수동 확인 후 재시작해야 합니다. 화면 꺼짐·배터리 15% 미만·심한 발열·캡처 정지·앱 전환·터치 응답없음이면 자동 정지합니다. 충전/기기 보호 상태를 직접 확인하세요.", 13, false, root);
        Button longMode = new Button(this);
        longMode.setAllCaps(false);
        longMode.setText(prefs.extendedSession() ? "장시간 모드: 켜짐 (수동 시작 필요)" : "장시간 모드: 꺼짐 (권장)");
        root.addView(longMode);
        longMode.setOnClickListener(v -> {
            ProjectionService current=ProjectionService.get();
            if(current != null && current.isRunning()) current.pause("장시간 옵션 변경 · 수동 재시작");
            if (prefs.extendedSession()) {
                prefs.setExtendedSession(false);
                longMode.setText("장시간 모드: 꺼짐 (권장)");
            } else new AlertDialog.Builder(this).setTitle("장시간 모드 주의")
                    .setMessage("24시간 정상 동작은 보장되지 않습니다. 화면을 켠 상태의 장시간 조작은 배터리 수명, 발열, 게임 운영 정책에 영향을 줄 수 있습니다. 위험 신호가 생기면 앱이 자동 정지합니다. 활성화할까요?")
                    .setNegativeButton("취소",(d,w)->{})
                    .setPositiveButton("활성화",(d,w)->{
                        prefs.setExtendedSession(true);
                        longMode.setText("장시간 모드: 켜짐 (수동 시작 필요)");
                    }).show();
        });
        button(root,"진단 기록 복사 (화면 이미지 비포함)",v -> {
            ClipboardManager clip=getSystemService(ClipboardManager.class);
            if(clip != null) clip.setPrimaryClip(ClipData.newPlainText("StairPilot diagnostic",Diagnostics.snapshot()));
            toast("진단 기록을 복사했습니다. 필요할 때만 공유하세요.");
        });
        text("이벤트 버튼 자동화: 게임 이벤트 화면에 보이는 안전한 '계속/다시하기' 버튼을 플로팅 메뉴에서 직접 학습해야 합니다. 이벤트 참가, 구매, 광고, 보상 선택은 자동으로 실행하지 않습니다.", 13, false, root);
        Button eventToggle = new Button(this);
        eventToggle.setAllCaps(false);
        eventToggle.setText(prefs.eventAutoContinue() ? "이벤트 버튼 자동 누르기: 켜짐" : "이벤트 버튼 자동 누르기: 꺼짐");
        root.addView(eventToggle);
        eventToggle.setOnClickListener(v -> {
            ProjectionService service = ProjectionService.get();
            if(service != null && service.isRunning()) service.pause("이벤트 설정 변경 · 안전 정지");
            prefs.setEventAutoContinue(!prefs.eventAutoContinue());
            eventToggle.setText(prefs.eventAutoContinue() ? "이벤트 버튼 자동 누르기: 켜짐" : "이벤트 버튼 자동 누르기: 꺼짐");
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
        text("v1.4 참고: 이벤트/대결 로비는 직접 선택하세요. 자동 터치는 화면 모양이 맞는 게임 구간에만 적용되며 실시간 PVP 및 일부 이벤트 사용은 게임 운영 정책에 영향을 줄 수 있습니다.\n주의: 게임을 실제로 시작한 뒤 ▶ 시작을 누르세요. 첫 방향은 화면 속 캐릭터가 바라보는 방향과 같게 맞추세요. 실패 시 ■ 정지하고, 화면을 회전했거나 스킨·테마가 달라졌다면 다시 보정하세요. Android 14 이상에서는 화면 공유할 때 '전체 화면'을 선택해야 앱 전환에도 프레임이 유지됩니다. 휴대폰에 따라 게임의 화면 캡처가 차단될 수 있습니다.", 13, false, root).setPadding(0, dp(15), 0, 0);
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
    private void selectMode(ModeProfile m, SeekBar delay) {
        ProjectionService running = ProjectionService.get();
        if(running != null && running.isRunning()) running.pause("모드 변경 · 다시 ▶ 시작");
        prefs.setMode(m);
        modeLabel.setText("현재: " + prefs.mode().korean);
        delay.setProgress(prefs.delayMs() - 25);
        delayLabel.setText("터치 완료 후 대기: " + prefs.delayMs() + "ms");
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
