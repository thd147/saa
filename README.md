# StairPilot Mobile v1.4.0 — 빨간 시간 게이지 대응 고속·안전 패치

> **중요:** Android APK 빌드는 아직 수행하지 못했습니다. Java SE 알고리즘 테스트만 통과했습니다. v1.3.0의 실기기 실행 가능성과 별개로 v1.4의 설치/동작은 검증이 필요합니다. **Google Play 프로텍트의 차단은 사용자 기기를 보호하기 위한 신호**이며 아이콘 변경이나 코드 패치만으로 해결되지 않습니다. 민감한 접근성 권한을 우회·강제 허용하지 마세요.

## v1.4 핵심 변경

- **상단 빨간 시간 게이지 인식(사용자 보정 필수)**: 게임 화면을 정지 캡처한 뒤 게이지 바의 **안쪽 왼쪽 끝→안쪽 오른쪽 끝** 두 번 터치해 범위 지정. 막대 내부의 붉은/주황색 픽셀을 세 줄로 검사하고, 띄엄띄엄한 색상이나 불확실한 결과는 버립니다. 게이지 인식에 실패하면 보조 고층 가속만 작동하며 읽지 못한 게이지를 0%라고 가정하지 않습니다.
- **고층 가변 가속**: 성공 처리된 제스처 입력 횟수를 계단 높이의 *근사치*로 사용해 100·250·500·1,000·2,500회 이후 대기를 단계적으로 줄이고, 게이지가 75%/50%/30%/15% 미만이면 추가 단축. 사용자가 끌 수 있음. 이 횟수는 실제 계단 높이나 점수와 동일하지 않습니다.
- **속도 프로필**: 일반 모드 기본 72ms, PVP 38ms, 이벤트 76ms, 협동전 110ms. 선택 가능한 입력 후 대기 범위 25~350ms이며 최종 하한은 24ms. **안드로이드의 터치 완료 콜백·게임 렌더링·화면 캡처·경로 분석 지연은 이 수치에 포함되지 않습니다.** 24ms마다 반드시 계단이 올라간다는 뜻이 아닙니다.
- **안전한 고속 확정**: 좌우 패턴 차이가 뚜렷할 때만 1프레임 판단, 모호하면 2프레임 재확인. 인식이 흔들리거나 기기 열 상태가 높으면 지연을 추가합니다. 잘못된 연타를 무제한 발송하지 않습니다.
- **장시간 보호**: 8초 프레임 중단, 제스처 2.5초 무응답, 화면 끔, 배터리 15% 미만, 열 상태 3 이상, 게임 밖 전환을 감지하면 중지. 30분마다 **안전 정지**하도록 변경하고 수동 확인 후 다시 시작해야 합니다. 24시간 무중단/발열 없음/계정 안전을 보증하지 않습니다.
- **아이콘 통합**: 기존 StairPilot 계단 모양 아이콘을 Android mdpi~xxxhdpi, 적응형 아이콘용 리소스로 포함. `assets/`에 벡터 원본과 미리보기 포함. **기존 아이콘은 수작업 벡터 제작이며 AI 생성 이미지라고 주장하지 않습니다.**
- **모드 정책**: PVP 계단 조작은 일반 계단 패턴에 한해 적용, 상대 분석·자동 매칭·승리 보장은 아님. 이벤트는 직접 학습시킨 '계속/다시하기' 버튼만 최대 3회 제한 자동 인식. 결제·광고·보상 선택은 자동화 안 함.

## 권한 및 설치 안전

앱은 기기 화면을 읽는 MediaProjection 및 다른 앱을 터치할 수 있는 접근성 서비스 권한을 요청합니다. 매우 민감한 기능이며 Play 프로텍트가 이를 차단할 수 있습니다. **차단 해제 절차나 보안 기능 우회보다, 코드를 검토하고 권한이 꼭 필요한지 확인하는 편이 안전합니다.** 네트워크 전송 권한이 없고 이미지 분석은 기기 안에서만 수행하도록 작성했지만, 코드를 신뢰하기 전에 직접 점검하세요.

## 빨간 시간 게이지 보정 방법

1. 스마트폰에서 기존 계단 9점 보정을 완료하고, 게임에 빨간 시간 게이지가 표시된 장면을 띄웁니다.
2. 화면 공유를 시작한 상태에서 우측 플로팅 메뉴의 **빨간 게이지 보정**을 누릅니다.
3. 고정된 화면에서 막대의 **색이 채워질 수 있는 영역의 맨 왼쪽 안쪽**과 **맨 오른쪽 안쪽**을 차례로 선택합니다. 수평 위치를 비슷하게 맞추고, 테두리·점수 숫자·일시정지 버튼은 피하세요.
4. 제어판의 **가변 가속: 켜짐** 상태를 확인합니다. 게임을 다시 열어 보정된 방향에서 ▶ 시작합니다.
5. 표시된 게이지가 이상하거나 방향이 틀리면 바로 ■ 정지하고 다시 보정합니다. 특정 모드에 시간 게이지가 없거나 디자인이 달라지면 가변 속도를 끄세요.

## 장거리 검증 — 게임 플레이가 아닌 알고리즘 시뮬레이션

명령어: `javac ... && java -cp tests/build LongClimbSimulationTest`

| 테스트 | 가상 계단 패턴 | 예상/실제 분기 오류 | 결과 |
|---|---:|---:|---|
| 1차 | 50,000 | 0 | PASS |
| 2차 | 100,000 | 0 | PASS |
| 3차 | 200,000 | 0 | PASS |

세 번의 테스트는 결정적 의사난수 패턴으로 `DecisionEngine`, 1프레임/2프레임 판정, `GaugePacer` 범위를 **총 350,000회** 호출하는 알고리즘 검증입니다. 실제 게임에서 5만·10만·20만 계단에 도달하거나 24시간 정상 플레이를 검증한 것이 **아닙니다**. 실제 속도와 연동되는 게이지 검출도 합성 픽셀 테스트만 수행했습니다.

## GitHub `thd147/saa` 업데이트 (휴대폰 Codespaces)

1. `StairPilotMobile-v1.4.0-android-source.zip`을 GitHub `saa` 저장소의 `main` 브랜치에 업로드하고 **Commit changes** 합니다. v1.2/v1.3 ZIP은 삭제하지 않습니다.
2. Codespaces `verbose pancake`의 터미널에서:

```bash
git pull --ff-only origin main
unzip -oq StairPilotMobile-v1.4.0-android-source.zip
cp -a StairPilotMobile/. .
rm -rf StairPilotMobile
git add -A
git commit -m "StairPilot Mobile v1.4 gauge adaptive speed"
git push origin main
```

3. GitHub Actions의 **Build Android debug APK** 작업에서 빌드 성공 여부를 확인합니다. 성공 시 `StairPilot-debug-apk` 아티팩트의 `app-debug.apk`를 받습니다. GitHub Actions 디버그 APK는 빌드 환경별 서명이 달라 설치 업데이트가 안 될 수 있으며, 새 설치 시 보정 정보가 사라질 수 있습니다.

## 테스트 실행 (Android SDK 없이)

```bash
mkdir -p tests/build
javac -encoding UTF-8 -d tests/build \
  app/src/main/java/io/stairpilot/{DecisionEngine,PromptGate,ModeProfile,AdaptivePacer,RunGuard,GaugeDetector,GaugePacer}.java \
  tests/*.java
for test in DecisionEngineTest PromptGateTest AdaptivePacerTest RunGuardTest GaugeDetectorTest GaugePacerTest LongClimbSimulationTest; do
  java -cp tests/build "$test"
done
```

## 참고

- 원본 프로젝트: https://github.com/hykeegj/auto-play-game
- 무한의 계단 게이지 공략: https://gamejay.net/667
- 고득점 팁: https://www.inven.co.kr/board/mogong/4222/612?vtype=pc
- Reddit 플레이 경험: https://www.reddit.com/r/AndroidGaming/comments/c37ub3
- Android MediaProjection: https://developer.android.com/media/grow/media-projection
- Android 접근성: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

---

# StairPilot Mobile v1.3.1 (앱 아이콘 패치)

> v1.3.1 변경: 기본 안드로이드 앱 아이콘을 계단 테마의 자체 제작 아이콘으로 교체했습니다. 런처용 mdpi~xxxhdpi 및 Android 8+ adaptive icon 포함. **AI 이미지 생성 도구 오류로 아이콘은 직접 제작한 벡터 디자인이며, AI 생성 결과가 아닙니다.** 이번 패치는 아이콘만 변경하며 기존 v1.3.0의 속도·PVP·이벤트 기능은 유지합니다. Google Play 프로텍트의 차단 경고는 아이콘을 바꿔도 해결되지 않습니다. 미심쩍은 권한이나 출처를 우회하지 말고 먼저 검증하세요.

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
