<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>동영상과 오디오 가공, 멀티트랙 타임라인 편집, 전환 효과와 자막, 화면 조작으로 스크립트 코드 생성</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어 (Languages)

******

현재 README.md 는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Slate Studio는 AutoJs6의 동영상과 오디오 플러그인이며, AutoJs6 없이도 단독으로 쓸 수 있는 편집 앱입니다. 동영상 자르기, 이어 붙이기, 속도 변경, 크기 조정, 회전, 잘라내기, 워터마크, 프레임 추출, GIF 만들기, 트랜스코딩을 하고, 오디오 자르기, 이어 붙이기, 믹스, 페이드, 변환을 합니다. 멀티트랙 타임라인 편집기에서는 클립에 전환 효과, 오버레이, 자막을 함께 구성할 수 있습니다.

처리는 플러그인 안에서 Jetpack Media3 Transformer와 기기의 하드웨어 코덱으로 이루어지므로 AutoJs6 호스트는 가볍게 유지됩니다. AutoJs6 스크립트는 전역 모듈 `video`와 `audio`로 미디어를 처리합니다. 화면 녹화 (`video.record`)와 녹음 (`audio.record`)은 AutoJs6에 내장되어 있어 플러그인 없이도 쓸 수 있습니다. 플러그인의 각 도구 화면은 같은 동작의 스크립트 코드를 보여 주며, 복사하거나 AutoJs6에서 새 스크립트로 열 수 있습니다.

> 현재는 개발 미리보기 버전입니다: AutoJs6 플러그인 센터가 플러그인을 인식할 수 있지만, 처리 기능, Studio 화면, `video` / `audio` 모듈의 처리 메서드는 아직 제공되지 않습니다. 아래 내용은 각 버전의 설계 목표이며, 진행 상황은 로드맵을 참고하세요.

******

### 기능 하이라이트

******

- 동영상 편집: 시간 지정 자르기, 이어 붙이기, 속도 변경 (0.25배에서 8배), 크기 조정, 회전, 잘라내기, 뒤집기, 텍스트나 이미지 워터마크, 오디오 트랙 추출과 제거, 트랜스코딩 (기본 H.264, 기기가 지원하면 H.265).
- 프레임과 GIF: 단일 프레임, 프레임 시퀀스, 썸네일을 이미지 파일이나 `Image` 객체로 얻고, 내장 인코더로 GIF 애니메이션을 만듭니다.
- 오디오: 자르기, 이어 붙이기, 여러 입력 믹스, 페이드 인 / 페이드 아웃을 포함한 볼륨 조정, 속도 변경, M4A, WAV, FLAC, OGG로 변환.
- 멀티트랙 타임라인: 동영상, 오디오, 오버레이 트랙, 7가지 전환 효과 (컷, 페이드, 디졸브, 슬라이드, 와이프, 줌, 블러), SRT 가져오기와 내보내기 및 ASS 기본 하위 집합을 지원하는 자막 (내보낼 때 화면에 입힘).
- 코드 생성: 각 도구 화면과 타임라인 내보내기에서 같은 동작의 `video.*` / `audio.*` 코드를 만들어, 복사하거나 AutoJs6에서 새 스크립트로 열 수 있습니다.
- Studio 녹음기: 플러그인 화면에서 마이크, 기기 내부 소리 (Android 10 이상), 또는 둘 다를 녹음합니다.
- FFmpeg 라이브러리 (1.1.0): 선택적으로 내려받으며 MP3, WebM, MKV 출력, `video.ffmpeg` 명령 직접 실행, 완전한 ASS 렌더링을 추가합니다.

******

### 사용 방법

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases)에서 APK를 내려받아 설치합니다. 모든 ABI에 공통인 패키지 하나만 있습니다.
2. 플러그인을 열고 요청이 나오면 모든 파일 접근 권한을 허용합니다. 그러면 선택한 경로를 읽고 쓸 수 있습니다.
3. 스크립트에서 `video`와 `audio`의 처리 메서드를 쓰려면 AutoJs6를 내부 빌드 6641 (6.8.1) 이상으로 업데이트하고 플러그인 센터에서 플러그인을 활성화하세요.

> 플러그인은 Android 7.0 (API 24) 이상 기기를 지원합니다. 유일한 네이티브 라이브러리는 Jetpack Compose의 것이며 16 KB 메모리 페이지에 맞춰 정렬되어 있으므로, 모든 ABI가 패키지 하나를 함께 씁니다.

******

### 스크립트 API

******

스크립트는 전역 모듈 `video`와 `audio` (`$video`와 `$audio`로도 쓸 수 있음)로 이 기능을 사용합니다 (1.0.0 설계 초안이며, 출시 버전의 문서가 우선합니다):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }], null);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });

let gif = video.toGif(joined.path, { fps: 10, width: 480 });
toastLog(gif.path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }], null);

let recording = video.record(null, { audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- 각 처리 메서드에는 완료될 때까지 기다리는 형태와 Promise를 반환하는 `xxxAsync` 형태가 있습니다. `options.onProgress(progress)`는 `percent`, `stage`, `etaMillis`를 알려 주며, 스크립트를 멈추면 그 작업은 취소됩니다.
- `output`을 생략하면 동영상은 `Movies/3-Slate Studio/`에, 오디오는 `Music/3-Slate Studio/`에 원본 파일 이름, 작업, 시각으로 된 이름으로 저장됩니다. 결과에는 실제 경로와 미디어 정보가 들어 있으며 미디어 라이브러리에도 등록됩니다.
- `video.compose(timeline)`는 객체로 기술한 멀티트랙 타임라인 (트랙, 클립, 전환 효과, 오버레이, 자막)을 렌더링합니다. 형식은 타임라인 편집기가 내보내는 것과 같습니다.
- `video.tasks()` / `audio.tasks()`는 스크립트의 진행 중인 작업을 나열하고 취소할 수 있습니다. 실패하면 `MediaStudioError`가 발생하며, 그 `code`는 `INVALID_OPTIONS`나 `STORAGE_PERMISSION_DENIED` 같은 안정적인 오류 코드입니다.

******

### 자주 묻는 질문

******

#### AutoJs6 없이 쓸 수 있나요?

쓸 수 있습니다. 도구 화면, 타임라인 편집기, Studio 녹음기는 단독으로 동작합니다. AutoJs6는 스크립트로 미디어를 처리할 때만 필요합니다.

#### 플러그인이 모든 파일 접근 권한을 요청하는 이유는 무엇인가요?

스크립트는 `/sdcard/DCIM/...` 같은 일반 파일 경로를 넘기고, 플러그인은 3-Maple Image처럼 그 경로를 직접 읽고 씁니다. 권한이 없으면 처리는 `STORAGE_PERMISSION_DENIED`를 알리고, 플러그인 화면에서 권한을 허용하는 방법을 안내합니다.

#### 화면 녹화에 플러그인이 필요한가요?

필요하지 않습니다. `video.record`와 `audio.record`는 AutoJs6 (내부 빌드 6641 이상)에 내장되어 있습니다. 플러그인은 처리 메서드와 자체 Studio 녹음기를 더합니다.

******

### 권한과 보안

******

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다.
- 저장소: 모든 파일 접근 권한은 스크립트와 Studio 화면이 지정한 경로에만 쓰입니다. 경로를 지정하지 않은 출력은 `Movies`와 `Music` 아래의 플러그인 폴더에만 기록됩니다.
- 녹음: 마이크와 기기 내부 소리는 Studio 녹음기가 동작하는 동안에만, 포그라운드 알림을 표시한 채로 수집됩니다. 기기 내부 소리를 수집하려면 시스템의 화면 캡처 동의도 필요합니다.
- 네트워크: 1.0.0에서는 업데이트 확인에만 씁니다. 1.1.0부터 FFmpeg 라이브러리는 요청할 때 내려받으며, 불러오기 전에 SHA-256으로 검증합니다.

******

### FFmpeg 라이브러리

******

1.1.0부터 플러그인은 LGPL-2.1-or-later로 빌드한 선택적 FFmpeg 라이브러리를 내려받을 수 있습니다. 라이브러리는 APK에 들어 있지 않으며, 요청할 때 기기의 ABI용만 내려받아 동적으로 불러옵니다. 자체 라이선스 고지와 해당 소스 코드를 받을 수 있는 위치가 함께 제공됩니다. 라이브러리가 없어도 1.0.0의 모든 기능은 그대로 쓸 수 있습니다.

> FFmpeg가 지원하는 일부 코덱 (예: H.265 / HEVC, AAC)은 국가나 지역에 따라 특허 대상일 수 있습니다. 라이브러리는 편의를 위해서만 제공되며 특허 라이선스를 포함하지 않습니다. 사용하기 전에 적용되는 규정을 확인하세요.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것으로, 호스트는 이 식별자들로 플러그인을 발견하고 기능을 협상합니다:

```text
application id: io.github.supermonster003.autojs6.plugin.three.slate.studio
plugin id: three-slate-studio
engine: media-studio
variant: default
info service: org.autojs.plugin.INFO (category three-slate-studio)
capability service: org.autojs.plugin.MEDIA_STUDIO (category media-studio)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so of Jetpack Compose only (arm64-v8a, armeabi-v7a, x86_64, x86; 16 KB aligned); a single APK for every ABI
```

******

### 개발 로드맵

******

계획, 설계 결정, 진행 상황은 ROADMAP.md의 체크리스트로 관리합니다. 체크되지 않은 항목은 계획이며 현재 버전의 기능이 아닙니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/11/05_

- `힌트` 개발 미리보기: AutoJs6 플러그인 센터가 플러그인을 인식할 수 있지만, 처리 기능, Studio 화면, video / audio 스크립트 모듈의 처리 메서드는 아직 제공되지 않습니다
- `힌트` AutoJs6 6.8.1 (6641) 이상이 필요합니다 (정확한 최소 버전은 호스트 쪽 변경이 반영된 뒤 채워 넣습니다)
- `기능` 플러그인 골격: Wake 활성화 프로토콜, 플러그인 센터 식별 서비스, 미디어 처리 기능 서비스 (계약 버전 2)
- `기능` 홈 화면에서 모든 파일 접근 권한을 허용할 수 있습니다
- `기능` 모든 ABI에 공통인 APK 하나: 유일한 네이티브 라이브러리는 Jetpack Compose의 것이며 16 KB 메모리 페이지에 맞춰 정렬되어 있습니다
- `기능` 10개 언어의 README, 플러그인 센터 설명, 변경 기록을 JSON 소스에서 생성
- `의존성` Jetpack Compose BOM 2026.09.00 추가 (플러그인 화면)
- `의존성` Navigation Compose 2.10.2 추가
- `의존성` Jetpack Media3 1.10.1 추가 (처리와 미리보기용 Transformer, Effect, Muxer, ExoPlayer, UI)
- `의존성` AndroidX Core KTX 1.18.0 추가

##### 더 많은 릴리스 기록은 다음을 참고

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

이 절은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE)으로 배포됩니다. 서드파티 구성 요소와 데이터는 서드파티 고지에서 확인할 수 있습니다.

- [제3자 고지 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
