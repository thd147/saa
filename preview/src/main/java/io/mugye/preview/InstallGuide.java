package io.mugye.preview;

/** Human-readable diagnosis, never attempts to alter Android security settings. */
public final class InstallGuide {
    private InstallGuide() { }
    public static final int PLAY_PROTECT = 0;
    public static final int SIGNATURE = 1;
    public static final int APK_FILE = 2;
    public static final int OTHER = 3;
    public static final int APK_FORMAT = 4;
    public static final int ARCHITECTURE = 5;
    public static String advice(int problem) {
        switch (problem) {
            case PLAY_PROTECT:
                return "Play 프로텍트가 기기를 보호하기 위해 설치를 차단한 상태입니다. 실제 매크로는 접근성 제스처 권한을 선언합니다. 앱 출처, 코드 및 권한을 검토하고 Google의 경고 사유를 확인하세요. 보안 보호 기능을 끄지 마세요.";
            case SIGNATURE:
                return "이전 버전과 새 버전의 APK 서명이 다르면 업데이트 설치가 실패할 수 있습니다. 앱 데이터 백업 여부와 이전 APK 서명을 확인하고 동일한 릴리스 키를 사용하는 방식으로 빌드해야 합니다.";
            case APK_FILE:
                return "GitHub Actions 결과의 Artifacts를 받아 ZIP 압축을 푼 뒤 APK를 선택해야 합니다. 소스 ZIP이나 GitHub에서 다운로드한 아티팩트 ZIP은 설치 파일이 아닙니다.";
            case APK_FORMAT:
                return "무계매크로는 .apkm/.xapk가 아닌 단일 .apk입니다. ZIP은 다운로드용 아카이브이므로 먼저 압축을 풀어야 합니다. SAI나 APKMirror Installer는 이 APK에 필요하지 않습니다.";
            case ARCHITECTURE:
                return "무계매크로는 Java 전용 앱이며 현재 프로젝트에 .so 네이티브 라이브러리가 없습니다. 따라서 arm64 전용 기기에서도 CPU 아키텍처 문제는 예상되지 않습니다. Play 프로텍트 차단은 별도 문제입니다.";
            default:
                return "설치 실패 문구와 Android 버전을 확인하세요. 이 진단용 미리보기 앱 자체는 접근성·화면 캡처 권한을 선언하지 않고, 게임 자동 조작 기능도 없습니다.";
        }
    }
}
