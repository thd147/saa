import io.mugye.preview.InstallGuide;

public final class InstallGuideTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        int[] codes = {InstallGuide.PLAY_PROTECT, InstallGuide.SIGNATURE, InstallGuide.APK_FILE, InstallGuide.OTHER};
        for (int code : codes) {
            String advice = InstallGuide.advice(code);
            check(advice != null && advice.length() > 30, "missing guidance " + code);
            check(!advice.contains("비활성화하세요"), "unsafe guidance " + code);
            check(!advice.contains("우회하세요"), "unsafe guidance " + code);
        }
        check(InstallGuide.advice(InstallGuide.PLAY_PROTECT).contains("접근성"), "explain accessibility");
        check(InstallGuide.advice(InstallGuide.SIGNATURE).contains("서명"), "explain signature");
        check(InstallGuide.advice(InstallGuide.APK_FILE).contains("압축"), "explain ZIP");
        System.out.println("InstallGuideTest: PASS (4 cases)");
    }
}
