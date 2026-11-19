# 무계매크로 v1.6.0 — 설치 호환성 및 APK 검증 패치

> **소스 프로젝트입니다.** GitHub Actions 빌드 후에만 APK가 생성됩니다. 이 패치는 **Google Play 프로텍트 우회 기능을 제공하지 않으며**, 차단된 앱을 설치할 수 있다고 주장하지 않습니다.

## anyapk 저장소 참고 결과

- 참고: https://github.com/sam1am/anyapk (Kotlin, 무선 ADB 기반 APK 설치기).
- `anyapk`는 일반 `.apk` 및 `.apkm`/`.xapk` 등 분할 패키지를 처리하지만, **무계매크로는 단일 APK**입니다. APKMirror Installer나 SAI가 필요한 형식이 아닙니다.
- 무계매크로는 네이티브 `.so` 파일이 없는 Java 앱으로, 32비트 전용 ABI 호환성 문제일 가능성이 낮습니다.
- `anyapk`의 ADB 설치 우회 경로는 포함하지 않습니다. 민감한 접근성 서비스가 선언된 앱의 Play 프로텍트 경고는 별도로 해결해야 합니다.

## v1.6 변경 사항

1. **한글 앱 UI의 호환성·설치 진단**: 현재 기기 Android API/CPU ABI 정보 표시, 단일 APK와 분할 APK 차이 안내, Play 프로텍트 오류와 ABI/서명 오류 구분.
2. **실제 매크로는 기존 접근성 서비스를 그대로 명시적으로 선언**하고 화면 공유 권한은 시스템에서 개별 승인받습니다. Play 프로텍트를 회피하려고 권한을 숨기지 않습니다.
3. **빌드 후 APK 검사** `scripts/audit_apk.py`: APK 형식·ZIP 손상·DEX·Manifest·네이티브 ABI 확인, `aapt`로 앱 ID·버전·권한 검증, `apksigner`로 APK 서명과 인증서 SHA-256 표시.
4. **권한 없는 미리보기 APK도 동시 생성**: 미리보기는 게임 조작 기능이 없고 설치 오류만 진단합니다.
5. **정적·Java·합성 APK 검사 3회 반복**: 5만·10만·20만 가상 계단의 경로 판단이 매 회 검증됩니다. 실제 게임 등반과 Play 프로텍트 통과는 **테스트하지 않았습니다**.
6. 기존 속도 가속/게이지 보정/안전 정지/계단 아이콘 유지. 실제 앱 패키지명 `io.stairpilot.mobile`을 유지하고 버전만 v1.6.0으로 갱신.

## 설치 차단과 관련한 사실

- 공식 안내: https://developers.google.com/android/play-protect/warning-dev-guidance?hl=ko
- 공식 64비트: https://developer.android.com/google/play/requirements/64-bit?hl=ko
- Google은 인터넷 출처 APK에 접근성 등 민감한 기능이 선언되면 설치를 자동 차단할 수 있습니다. **서명·형식 검사가 모두 통과해도** 이 조건은 바뀌지 않습니다.
- Play 프로텍트가 차단하면 보호 기능을 끄지 말고, 원인 확인 및 필요 시 공식 오탐 신고·이의신청 절차를 이용하세요.
- 기존 설치 앱과 APK 서명이 다르면 업데이트가 실패할 수 있습니다. 지속적인 업데이트에는 안전하게 관리되는 같은 서명 키가 필요합니다. GitHub Actions의 임시 디버그 키는 빌드마다 달라질 수 있습니다.

## GitHub 업로드 (스마트폰 Codespaces)

1. `MugyeMacro-v1.6.0-android-source.zip`을 https://github.com/thd147/saa/upload/main 에 업로드하고 Commit changes.
2. Codespaces `verbose pancake`에서 아래 명령어 실행:

```bash
git pull --ff-only origin main
unzip -oq MugyeMacro-v1.6.0-android-source.zip
cp -a StairPilotMobile/. .
rm -rf StairPilotMobile
git add -A
git commit -m "MugyeMacro v1.6 APK diagnostics and compatibility"
git push origin main
```

3. https://github.com/thd147/saa/actions 에서 `Build Android debug APK` 상태를 확인하세요.
4. 빌드 성공 시 **3개의 아티팩트**가 생성됩니다: `MugyeMacro-v1.6-debug-apk` (실제 매크로), `MugyePreview-v1.6-safe-apk` (권한 없는 미리보기), `Mugye-v1.6-apk-audit-report` (APK 서명/ABI/권한 보고서).
5. 실제 매크로 설치가 Play 프로텍트에 차단된다면 **억지로 설치하지 마세요**. 보고서와 Google 개발자 문서로 원인을 검토하세요.

## 로컬 소스 테스트 (Android SDK 불필요)

```bash
for cycle in 1 2 3; do
  python3 scripts/check_project.py
  python3 tests/test_apk_audit.py
  mkdir -p tests/build
  javac -encoding UTF-8 -d tests/build \
    app/src/main/java/io/stairpilot/{DecisionEngine,PromptGate,ModeProfile,AdaptivePacer,RunGuard,GaugeDetector,GaugePacer}.java \
    preview/src/main/java/io/mugye/preview/InstallGuide.java tests/*.java
  for test in DecisionEngineTest PromptGateTest AdaptivePacerTest RunGuardTest GaugeDetectorTest GaugePacerTest LongClimbSimulationTest InstallGuideTest; do
    java -cp tests/build "$test"
  done
done
```

**검증의 범위:** 위 테스트는 Java 로직과 APK 분석 코드의 *시뮬레이션/정적 검사*입니다. Android SDK가 없어 실제 APK 빌드, 휴대폰 설치, Play 프로텍트 판정, 연속 24시간 게임 실행은 확인하지 않았습니다.

이전 변경사항은 `docs/README-v1.5.md`를 참조하세요.
