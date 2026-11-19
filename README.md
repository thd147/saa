# StairPilot Mobile v1.2.0 — 무한의 계단 자동 플레이 안드로이드 앱 (소스코드)

> 상태: **개발용 소스코드 + 순수 Java 로직 테스트 통과**. 이 실행 환경에는 Android SDK/Gradle이 없어 APK 빌드 및 실기기 연동 테스트는 **미완료**입니다. 실제 플레이 성공률, 10만 계단 달성 등을 보증하지 않습니다.

PC/에뮬레이터와 루팅 없이 안드로이드 기기 안에서 화면을 분석하고 버튼을 터치하도록 설계했습니다. 네트워크 서버나 게임 프로세스 변조를 사용하지 않습니다.

## 참고 및 구현 범위

- [원본 소스](https://github.com/hykeegj/auto-play-game) `main.py`: 720×1280 LDPlayer에서 PyAutoGUI/OpenCV로 계단(좌·우 영역)을 찾아 `올라가기` 또는 `회전` 버튼을 클릭합니다.
- 사용자가 제공한 [오토의 세계 YouTube 영상](https://www.youtube.com/watch?v=uNmjkLuwqag) 요약: 좌/우 최대 3칸 확인, 모호한 배치와 방향 전환 처리, 반복 재인식·속도 개선.
- [steve1316/android-cv-bot-template](https://github.com/steve1316/android-cv-bot-template)처럼 Android의 `MediaProjection` 화면 캡처와 `AccessibilityService.dispatchGesture`를 조합했습니다. OpenCV 설치 없이 동작하도록 간단한 Java 그레이스케일 템플릿 매칭을 직접 구현했습니다.
- Reddit의 Android 기기별 접근성 서비스 종료 및 화면 공유 이슈를 고려해 포그라운드 서비스·명시적 사용자 시작/종료·오류 시 정지를 추가했습니다. 기기별 중단을 완전히 방지하지는 못합니다.

## v1 → 패치 1 → 패치 2

| 버전 | 변화 |
| --- | --- |
| 기본형 v1.0 | 1칸 기준 계단 존재 여부로 올라가기 / 방향 바꾸기 선택 |
| 패치 1 v1.1 | 2프레임 연속 동의, 터치 완료 응답 확인, 중복 터치 차단, 14프레임 인식 실패 시 자동 정지 |
| 패치 2 v1.2 | 양쪽 최대 3칸씩 이미지 확인, 비슷한 분기 제거, 확실한 연속 패턴에 한해 약한 1칸 복구, 고정 스크린샷을 이용한 9곳 보정, 해상도 검사 및 지연 조정 |

**중요:** 영상의 모든 분기 규칙을 1:1로 재구성한 것은 아닙니다. 영상 요약에 없는 좌표/디테일을 임의로 추측해 반복 입력하지 않고, 확실한 첫 걸음만 실행 후 새 프레임을 분석합니다. 이 버전에서 3칸 미리 보기는 **후보 검증용**이며 3연속 터치를 하지 않습니다.

## 요구사항

- Android **10(API 29)** 이상 권장, 개발 설정 `minSdk 29`, `targetSdk 35`.
- 기기에서 화면 공유/녹화 권한 및 접근성 서비스 허용.
- 순정 `무한의 계단` 설치(`com.nflystudio.InfiniteStaircase`). 게임의 화면 캡처/자동화 정책 및 운영 약관을 확인하세요.
- APK를 직접 만들 경우 **Android Studio + Android SDK API 35 + JDK 17** 또는 GitHub Actions 빌드.
- 루팅, ADB 케이블, PC와의 플레이 중 실시간 연결은 필요하지 않습니다. **소스코드를 APK로 빌드하는 과정에는 개발환경이 필요**합니다.

## 설치: APK 빌드

### 쉬운 방법: GitHub Actions

1. 이 폴더의 파일을 본인의 **GitHub 저장소 루트**에 올립니다. `.github/workflows/build-apk.yml`도 함께 올립니다.
2. GitHub 저장소 **Actions** → **Build Android debug APK** → **Run workflow**.
3. 성공하면 해당 실행 화면 하단 **Artifacts**의 `StairPilot-debug-apk`을 받아 압축 해제한 후 `app-debug.apk`을 휴대폰에 설치합니다.
4. 이 파일은 GitHub 빌드가 실제로 성공해야 생성됩니다. 여기에는 APK가 포함되어 있지 않습니다.

### Android Studio

1. Android Studio에서 이 프로젝트 폴더를 엽니다. Android Gradle Plugin 8.7.3, Gradle 8.9, JDK 17, Android SDK 35를 사용합니다.
2. `gradle`(8.9)이 설치된 환경에서 프로젝트 루트의 `gradle assembleDebug`를 실행하거나 IDE의 Gradle 작업 `assembleDebug`를 실행합니다. 이 소스 ZIP에는 Gradle wrapper JAR가 포함되지 않습니다.
3. 성공하면 `app/build/outputs/apk/debug/app-debug.apk` 파일이 생성됩니다.

## 사용하기

1. 설치 후 **StairPilot**을 열고 `① 접근성 서비스 켜기`에서 **StairPilot**을 허용합니다. 이 권한은 자동 터치에만 사용됩니다.
2. `② 화면 공유 시작`을 눌러 공유 허용 팝업에서 **전체 화면**을 선택합니다. Android 14+에서는 앱 하나만 공유하도록 선택하면 게임 전환 시 캡처가 달라질 수 있습니다.
3. `③ 무한의 계단 열기`를 눌러 게임을 띄웁니다. 상단 오른쪽에 **보정 / ▶ 시작 / ■ 정지 / 숨김** 메뉴가 나옵니다.
4. 게임 플레이 화면이 잘 보일 때 `보정`을 눌러 한 장의 프레임을 고정합니다. 그 프레임에서 **9곳을 순서대로 터치**합니다. 위치를 잘못 지정했으면 `취소` 후 처음부터 다시 보정합니다.
   - 1: **올라가기 버튼** 중심
   - 2: **회전 버튼** 중심
   - 3~5: **캐릭터 기준 화면 왼쪽**의 다음 1·2·3번째 계단 중심
   - 6~8: **화면 오른쪽**의 다음 1·2·3번째 계단 중심
   - 9: 식별하기 쉬운 **계단의 모서리/무늬** 중심 (색이 단색이면 학습 실패)
5. 학습 완료 안내를 확인하고, **현재 캐릭터 방향**과 앱에 저장된 시작 방향(기본 왼쪽)을 일치시킵니다. 게임을 다시 시작한 뒤 플로팅 `▶ 시작`을 누릅니다. 방향전환 버튼 한 번은 **방향 회전 + 한 계단 이동**으로 계산합니다.
6. 오동작, 게임오버 또는 게임 화면 밖으로 나갈 때는 즉시 `■ 정지`를 누르세요. 앱의 `화면 공유/자동 실행 완전 종료`나 알림의 종료 버튼은 화면 공유 서비스 자체를 중지합니다.

**실전 보정 팁:** 캐릭터와 계단 3칸을 동시에 볼 수 있는 순간 고정하는 것이 가장 좋습니다. 화면에 3칸이 보이지 않는 게임 스킨/장면에서는 3칸 모드 인식이 신뢰할 수 없으므로 사용하지 마세요. 이미 캘리브레이션된 화면과 해상도 또는 테마가 바뀌면 재보정해야 합니다.

## 조절값

- `터치 후 대기`: 90~790ms (기본 160ms). 너무 빠르면 같은 프레임을 다시 읽고, 너무 느리면 타이머가 소진될 수 있습니다.
- `계단 인식 임계값`: 50~95% (기본 78%). 무작정 낮추면 배경이나 UI를 계단으로 잘못 인식합니다.
- `첫 시작 방향`: 왼쪽 / 오른쪽.

## 기본 알고리즘

1. 지정된 6개 ROI(좌1~3, 우1~3) 각각에서 학습된 22×14 픽셀 템플릿을 그레이스케일 정규화 상관계수(NCC)로 스캔.
2. 좌1과 우1 중 오직 한 곳에 계단이 보이면, 현재 바라보는 방향과 비교해 `UP` 또는 `TURN` 선택.
3. 양쪽에 동시에 보이면 **2~3칸까지 이어진 쪽**이 유일하게 우세할 때만 결정. 한쪽 1칸만 약하게 보이는 경우 2·3칸이 강하게 일치하는 경우만 제한적으로 인정.
4. 동일 판단 2프레임 연속 확인 후 터치를 한 번 전송. 실제 완료 콜백을 받은 뒤 진행 방향 및 입력 간격 갱신.
5. 이미지가 없거나 애매한 상태 14프레임 연속 → 자동 정지. 회전 한 번도 **한 계단**으로 계산하고 다음 프레임을 분석.

## 제한 / 보안 / 주의

- **스마트폰에서 실행해 검증하지 못했습니다.** 휴대폰 종류, 계단 스킨, 화면 효과, 휴대폰의 게임 화면 보호로 인해 인식이 안 되거나 잘못 눌릴 수 있습니다.
- 무한의 계단 게임 내 *특정 이벤트, PvP 모드, 특수 스킨*에 대한 별도 인식은 구현되지 않았습니다.
- 화면 공유 권한은 Android가 직접 묻고, 접근성 권한도 사용자 수동 설정이 필요합니다. 백그라운드에서 몰래 시작하지 않습니다.
- 계단의 **22×14 회색조 학습 패치만 앱 내부 파일에 저장**하고 전체 화면 이미지나 사용자 데이터를 서버로 전송하지 않습니다.
- 실행 중 앱 전환 등으로 게임 상태가 변했을 때 오작동할 수 있으니 필요시 정지해야 합니다. 게임사의 이용약관에 따라 사용이 제한될 수 있습니다.

## 로직 테스트

Android SDK 없이 `DecisionEngine`의 판단 로직은 아래 명령으로 테스트할 수 있습니다.

```sh
mkdir -p tests/build
javac -d tests/build app/src/main/java/io/stairpilot/DecisionEngine.java tests/DecisionEngineTest.java
java -cp tests/build DecisionEngineTest
```

## 외부 API 참고

- Android 공식 [MediaProjection](https://developer.android.com/media/grow/media-projection) (최신 OS는 포그라운드 서비스 유형 필수)
- Android 공식 [AccessibilityService.dispatchGesture](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#dispatchGesture(android.accessibilityservice.GestureDescription,android.accessibilityservice.AccessibilityService.GestureResultCallback,android.os.Handler))
- 개발자 커뮤니티: [Android 실기기 접근성 서비스 문제](https://www.reddit.com/r/androiddev/comments/1msraxp/), [기기별 전원 관리 이슈](https://www.reddit.com/r/androiddev/comments/yopu8u/)
