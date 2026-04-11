## StellarStep

스텔라이브 팬게임 – 4키 리듬게임 (Windows PC AMD64)

---

## 어떻게 플레이하나요? / How to Play

### 빌드 & 실행 / Build & Run

**필요 사항 (Requirements):**
- Java 21 JDK (e.g. [Eclipse Temurin](https://adoptium.net/))
- Windows 10/11 x64

**실행:**
```bash
git clone https://github.com/jwyoon1220/StellarStep
cd StellarStep
./gradlew run
```
> Windows에서: `gradlew.bat run`

**패키징 (fat JAR):**
```bash
./gradlew jar
java -jar build/libs/StellarStep-1.0-SNAPSHOT.jar
```

### 조작키 / Controls (Default)

| 기능 | 키 |
|------|----|
| Lane 1 (leftmost) | **D** |
| Lane 2 | **F** |
| Lane 3 | **J** |
| Lane 4 (rightmost) | **K** |
| Menu navigate | **↑ / ↓** |
| Select / Confirm | **Enter** |
| Back / Quit game | **Escape** |

### 게임 화면 / Screens

1. **Main Menu** – Play, Calibration, Quit
2. **Song Select** – Choose from `assets/songs/<folder>/chart.json`
3. **Gameplay** – 4-key lane game; judgments: PERFECT / GREAT / GOOD / BAD / MISS
4. **Result** – Score, accuracy, judgment breakdown
5. **Calibration** – Adjust global offset (ms); tap SPACE to the metronome beat

### 곡 추가 / Adding Songs

1. `assets/songs/<song_name>/` 폴더 생성
2. 폴더에 `chart.json` 작성 (포맷은 `ASSET_SPEC.md` 참고)
3. 폴더에 오디오 파일 (`audio.ogg` 또는 `audio.wav`) 추가
4. 게임 실행 후 Song Select에서 선택

### 스킨 교체 / Replacing Skin Assets

`assets/skins/default/` 안의 PNG 파일들을 교체하면 됩니다.  
각 파일의 용도, 필요 크기, 포맷은 **`ASSET_SPEC.md`** 를 참고하세요.

---

## 라이선스 / License
APACHE LICENSE, VERSION 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

## 면책조항
StellarStep 측은 음원 파일, 뮤직비디오(MV) 동영상 파일 (이하 "음원 및 비디오") 등을 제공하지 않으며, 오직 '채보 파일'만을 제공합니다.  
따라서 음원 및 비디오는 사용자가 적법한 방법으로 확보해야 합니다.  
StellarStep 측은 음원 및 비디오의 저작권 문제에 대해 책임을 지지 않습니다.
