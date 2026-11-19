package io.mugye.preview;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** No game automation or permissions. Used only to diagnose installation issues and preview UI. */
public final class PreviewActivity extends Activity {
    private TextView advice;
    private int dp(float x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    private void line(LinearLayout root, String value, int size, boolean strong) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(0xff1c2c47);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(10), 0, dp(10));
        root.addView(t);
    }
    private void choose(LinearLayout root, String label, int issue) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(label);
        root.addView(b, new LinearLayout.LayoutParams(-1, dp(53)));
        b.setOnClickListener(v -> advice.setText(InstallGuide.advice(issue)));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView outer = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(30), dp(20), dp(35));
        root.setBackgroundColor(0xfff3f6fc);
        outer.addView(root);
        setContentView(outer);
        line(root, "무계매크로", 29, true);
        line(root, "v1.5 · 설치 진단 전용 미리보기", 16, true);
        line(root, "이 APK에는 게임 자동 터치, 접근성 서비스, 화면 캡처 기능이 없습니다. 실제 매크로 APK와 다른 앱으로 설치됩니다.", 15, false);
        line(root, "어떤 문제가 발생했나요?", 21, true);
        choose(root, "Play 프로텍트가 설치를 차단함", InstallGuide.PLAY_PROTECT);
        choose(root, "기존 앱과 업데이트 충돌", InstallGuide.SIGNATURE);
        choose(root, "ZIP은 받았지만 APK가 없음", InstallGuide.APK_FILE);
        choose(root, "다른 설치 오류", InstallGuide.OTHER);
        advice = new TextView(this);
        advice.setTextColor(0xff145d76);
        advice.setTextSize(16);
        advice.setPadding(dp(15), dp(18), dp(15), dp(18));
        advice.setBackgroundColor(0xffe4f0f5);
        advice.setText("위에서 증상을 선택하면 설치 안내가 나옵니다.");
        root.addView(advice);
        line(root, "권한 없는 이 미리보기 APK도 차단되는지 확인하면 원인 파악에 도움이 됩니다. 이 결과만으로 실제 매크로 APK의 안전성이나 설치 가능성이 보장되지는 않습니다.", 14, false);
        line(root, "화면 디자인: 계단 아이콘 유지 / 한글 UI / 단계별 안내", 14, false);
    }
}
