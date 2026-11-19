# StairPilot Mobile v1.3.0 — 무한의 계단 모바일 화면 매크로 (실험용)

Android 10 이상 / Java + Android SDK / 루팅·PC 연결 없이 화면 공유(MediaProjection)와 접근성 서비스 제스처를 사용하는 오프라인 앱. **게임 화면에 대한 사용자별 보정과 실제 기기 테스트가 필요합니다.** 게임 프로그램 자체를 변경하지 않습니다. 게임 이용약관이나 대전 공정성을 확인하세요.

## 변경 내용 — v1.3.0 (속도 + 모드 + 이벤트)

| 개선 | 구현된 동작 | 제한 |
|---|---|---|
| 화면 탐색 비용 절감 | 먼저 좌/우 첫 계단만 비교; 모호한 패턴에서만 2·3번째 조사 | 일부 특수 계단에서 재보정 필요 |
| 템플릿 매칭 최적화 | 샘플 템플릿 통계를 미리 계산; 프레임 ByteBuffer 재사용; 0.978 넘으면 조기 종료 | 입력 FPS는 기기·게임 렌더링에 따라 달라짐 |
| 빠른 입력 | 모드별 터치 시간 19~31ms, 완료 후 지연 45~350ms 직접 조절 | 모든 기기에서 동일한 실제 입력속도를 보장하지 않음 |
| PVP 빠름 | 자신이 선택한 경로가 다른 쪽과 명확히 구분될 때만 단일 프레임 판단; 애매하면 2프레임 | 상대 플레이어/대전 게이지·데스매치 전용 탐지 없음 |
| 이벤트 안전 보조 | 사용자가 눈으로 확인한 **'다시하기/계속' 버튼** 하나를 학습시켜 이벤트 모드에서만 자동으로 한 번 누름 | 이벤트 입장/미션/결제/광고/보상 선택 자동화 없음 |
| 버튼 안전장치 | 학습한 UI가 3프레임 연속 보일 때만 작동, 사라져야 재무장, 1.8초 재클릭 제한, 세션당 3번 제한 | 특수 스킨/이벤트 화면에 따라 인식 실패 가능 |
| 추가 모드 | 일반 / PVP / 이벤트 / 4인 협동 속도 프로필 | 협동전 몬스터 전투, 특수 임무 미지원 |
| 표시 | 성공적으로 접수된 터치 횟수와 최근 추정 입력/초 표시 | 실제 캐릭터가 올라간 칸 수는 별도 검증되지 않음 |
| 보호 | 프레임 타임스탬프 중복 무시, 제스처 직렬화, 인식불능 자동 정지, 모드 전환 시 정지 | 터치가 게임에 실제 반영되었는지 보장하지 않음 |

## 현재 게임 모드 조사와 적용 범위

게임 개발자 공식 Google Play 소개에는 **PVP 1vs1·2vs2·1vs4**, **협동전(4P)** 및 **시즌별 테마/특수 규칙 이벤트**가 소개되어 있습니다. 커뮤니티 위키에서는 월드/친구 PVP, 1:1 데스매치에서 빠른 게이지, 협동전과 이벤트별 추가 규칙을 설명합니다. 이는 계단 두 버튼만 누르는 기본 알고리즘과 다를 수 있습니다.

- **일반 모드**: 동일한 계단 이미지가 보이는 구간에서 기본 이동 자동화. 대기 기본값 105ms.
- **PVP**: 사용자가 게임 안에서 직접 매칭하고 게임이 시작된 뒤 활성화합니다. 첫 계단 신뢰도가 기준보다 0.12 이상 높고 반대쪽이 0.12 이상 낮을 때에만 즉시 반응. 기본값 55ms. 계단 위치/크기가 일반 모드와 다르면 다시 보정해야 합니다. 상대 정보 분석, 데스매치 게이지 탐지 및 자동 매칭 없음.
- **이벤트**: 기본 계단 인식 + 보조 버튼 자동 탭(사전 학습 시). 기본값 105ms. 이벤트 전용 장애물·수집·다른 조작 규칙은 지원하지 않으며, 게임 화면에 계단이 3.5초간 인식되지 않으면 자동 정지합니다.
- **협동전**: 빠른 실수보다 계단 터치의 안정성을 우선한 135ms 프로필. 몬스터 전투나 팀원 행동에 반응하지 않습니다.

## 설치 및 설정

1. Android Studio에서 이 프로젝트 폴더를 열어 디버그 APK를 빌드하거나, GitHub Actions `Build Android debug APK` 워크플로를 실행하세요.
2. APK를 설치한 후 **접근성 서비스 켜기**, **화면 공유 시작**을 순서대로 허용합니다. Android 14 이상에서는 가능하면 **전체 화면** 공유를 선택하세요.
3. 게임을 열고 화면 우측 플로팅 메뉴 **계단 보정**: 올라가기 버튼, 방향 전환 버튼, 왼쪽 1~3계단, 오른쪽 1~3계단, 이미지 학습 지점을 순서대로 터치(9회). UI 스킨/테마 변경이나 화면 회전 후 다시 보정합니다.
4. 시작 방향(왼쪽/오른쪽)을 맞추고 모드 선택 후 **▶ 시작**. 탭이 너무 빠르거나 틀리면 **■ 정지** 후 대기시간을 높이고 재보정하세요.
5. 이벤트 버튼 자동화가 필요하면 **이벤트 종료/다시하기 화면을 직접 띄운 후** 플로팅 메뉴 **이벤트 버튼 학습**을 눌러 캡처한 고정 화면에서 글자/무늬가 있는 안전한 '다시하기' 또는 '계속' 버튼 하나의 중심을 선택하세요. 앱의 `이벤트 버튼 자동 누르기: 켜짐`으로 변경하고 `이벤트` 모드에서만 실행합니다. 오인식 위험이 있으므로 결제, 광고, 스킵 등은 절대 학습하지 마세요.
6. 앱이 갑자기 멈추면 `StairPilot` 화면의 상태 메시지와 플로팅 메뉴의 L/R 신뢰도 값을 확인하세요. `Threshold`를 무작정 낮추기보다 먼저 위치와 이미지 패턴을 다시 보정하세요.

## GitHub Codespaces를 이용한 기존 thd147/saa 저장소 업데이트

**중요**: 이 ZIP에는 `StairPilotMobile/` 디렉터리로 소스가 들어 있습니다. 기존 v1.2 ZIP, GitHub 기록, 다른 파일은 강제로 지우지 않습니다.

1. `StairPilotMobile-v1.3.0-android-source.zip`을 저장소의 `main` 브랜치에 업로드하고 `Commit changes`를 완료합니다. 별도 PR 생성 시 병합합니다.
2. Codespaces 터미널에서:

```bash
unzip -oq StairPilotMobile-v1.3.0-android-source.zip
cp -a StairPilotMobile/. .
rm -rf StairPilotMobile
git add -A
git commit -m "StairPilot v1.3 speed + PVP/event profiles"
git push origin main
```

3. `https://github.com/thd147/saa/actions`에서 새 `Build Android debug APK` 작업의 성공 여부를 확인하고, 성공 시 `Artifacts`의 `StairPilot-debug-apk`를 받아 ZIP 압축을 풀어 APK 설치.
4. 기존 설치 앱은 같은 applicationId `io.stairpilot.mobile`로 유지하지만, 디버그 APK 서명 키가 이전 빌드와 다르면 업데이트 설치가 실패할 수 있습니다. 그 경우 기존 앱 설정/학습 정보가 지워질 수 있으니 미리 기록하고, 기존 앱을 제거한 뒤 새 APK를 설치해야 합니다.

### 기본 권장 속도

- 처음 실행 시 **일반 105ms** 또는 **협동 135ms**에서 정확도를 확인하세요.
- 정확도가 충분하면 **PVP 55ms**로 낮출 수 있습니다. 고속 입력 한계는 화면 인식과 Android 제스처 완료시간에 제한됩니다. 55ms는 *제스처 완료 후 대기 설정*이지 한 계단에 55ms가 걸린다는 뜻이 아닙니다.
- 잦은 오류: 대기 시간을 90~160ms로 늘리고 시작 방향/보정/임계값을 점검하세요.

## 검증

`javac` 및 Java SE 테스트:

```bash
mkdir -p tests/build
javac -d tests/build app/src/main/java/io/stairpilot/DecisionEngine.java app/src/main/java/io/stairpilot/ModeProfile.java app/src/main/java/io/stairpilot/PromptGate.java tests/DecisionEngineTest.java tests/PromptGateTest.java
java -cp tests/build DecisionEngineTest
java -cp tests/build PromptGateTest
```

실제 Android APK **v1.3.0의 컴파일 및 휴대폰 실전 플레이는 아직 검증되지 않았습니다**. GitHub Actions 결과와 실제 게임 화면에 따라 추가 수정이 필요할 수 있습니다.

## 참고 출처

- 원본 매크로: https://github.com/hykeegj/auto-play-game (LDPlayer, OpenCV, 화면 좌표 기반)
- 사용자가 제공한 영상 설명: https://www.youtube.com/watch?v=uNmjkLuwqag (1~3칸 경로 판단 아이디어)
- 공식 Google Play 소개: https://play.google.com/store/apps/details?id=com.nflystudio.InfiniteStaircase
- K리그 2026 이벤트: https://www.kleague.com/news_view.do?orderBy=seq&seq=94550&viewOption=album
- Android 접근성 제스처 공식 문서: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Android 매크로 구현 비교: https://github.com/justin-reid/screen-clicker 및 https://github.com/steve1316/android-cv-bot-template
- 커뮤니티(공식 개발자 문서는 아님): https://d.namu.moe/w/%EB%AC%B4%ED%95%9C%EC%9D%98%20%EA%B3%84%EB%8B%A8/%EC%BD%98%ED%85%90%EC%B8%A0

Reddit의 관련 일반 Android 자동화 논의를 살펴봤으나, **무한의 계단 최신 PVP/이벤트를 직접 검증할 만한 자료는 찾지 못했습니다**. 구체적인 모드 지원 범위를 Reddit 정보로 추측하지 않았습니다.
