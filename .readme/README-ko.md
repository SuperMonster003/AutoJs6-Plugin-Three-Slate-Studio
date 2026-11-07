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

> 1.0.0은 첫 정식 버전입니다. 스크립트로 미디어를 처리하려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요합니다. 플러그인 화면, 타임라인 편집기, Studio 녹음기는 AutoJs6 없이도 쓸 수 있습니다.

******

### 기능 하이라이트

******

- 동영상 편집: 시간 지정 자르기, 이어 붙이기, 속도 변경 (0.25배에서 8배), 크기 조정, 회전, 잘라내기, 뒤집기, 텍스트나 이미지 워터마크, 오디오 트랙 추출과 제거, 트랜스코딩 (기본 H.264, 기기에 H.265 인코더가 있으면 H.265).
- 프레임과 GIF: 단일 프레임, 프레임 시퀀스, 썸네일을 PNG나 JPG 파일 또는 `Image` 객체로 얻고, 내장 인코더로 GIF 애니메이션을 만듭니다.
- 오디오: 자르기, 이어 붙이기, 여러 입력 믹스 (합치면 클리핑될 때 자동으로 줄임), 페이드 인 / 페이드 아웃을 포함한 볼륨 조정, 속도 변경, M4A, WAV, FLAC, OGG로 변환 (OGG는 Android 10 이상).
- 멀티트랙 타임라인 편집기: 동영상, 오디오, 오버레이, 자막 트랙, 7가지 전환 효과 (컷, 페이드, 디졸브, 슬라이드, 와이프, 줌, 블러), 실시간 미리보기, 실행 취소와 다시 실행, `.slate.json` 프로젝트 저장. 자막은 SRT 가져오기와 내보내기 및 ASS 기본 하위 집합을 지원하며 내보낼 때 화면에 입힙니다.
- 도구 상자: 처리 기능마다 마법사가 있고 양식에 기본값과 범위가 표시됩니다. 작업은 대기열에서 실행되며 진행 상황과 실패 이유를 보여 주고, 긴 작업에는 포그라운드 알림이 표시됩니다.
- 코드 생성: 각 마법사, 타임라인 내보내기, 녹음에서 같은 동작의 `video.*` / `audio.*` 스크립트를 만들어, 복사하거나 AutoJs6에서 새 스크립트로 열 수 있습니다.
- Studio 녹음기: 플러그인 화면에서 마이크, 기기 내부 소리 (Android 10 이상), 또는 둘 다를 M4A나 WAV로 녹음합니다. 일시 중지할 수 있고 화면이 꺼져도 계속 녹음합니다.
- FFmpeg 라이브러리 (1.1.0 예정): 선택적으로 내려받으며 MP3, WebM, MKV 등의 형식과 완전한 ASS 자막 렌더링을 추가합니다.

******

### 스크린샷

******

아래 스크린샷은 Android 12와 13 휴대폰에서 밝은 테마와 중국어 간체 화면으로 찍었습니다.

| 미디어 라이브러리 | 도구 상자 | 타임라인 편집기 | Studio 녹음기 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/library.png?raw=true" alt="library" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/toolbox.png?raw=true" alt="toolbox" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/timeline.png?raw=true" alt="timeline" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/recorder.png?raw=true" alt="recorder" width="200" /> |

******

### 사용 방법

******

1. AutoJs6의 플러그인 센터에서 3-Slate Studio를 설치하거나, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases)에서 APK를 내려받아 설치합니다. 모든 ABI에 공통인 패키지 하나만 있습니다.
2. 플러그인을 열고 요청이 나오면 모든 파일 접근 권한을 허용합니다. 그러면 선택한 경로를 읽고 쓸 수 있습니다.
3. 스크립트에서 `video`와 `audio`의 처리 메서드를 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상을 사용하고 플러그인 센터에서 플러그인을 활성화하세요.

> 플러그인은 Android 7.0 (API 24) 이상 기기를 지원합니다. 유일한 네이티브 라이브러리는 Jetpack Compose의 것이며 16 KB 메모리 페이지에 맞춰 정렬되어 있으므로, 모든 ABI가 패키지 하나를 함께 씁니다.

******

### 스크립트 API

******

스크립트는 전역 모듈 `video`와 `audio` (`$video`와 `$audio`로도 쓸 수 있음)로 이 기능을 사용합니다 (모든 메서드와 옵션은 AutoJs6 문서의 [video](https://docs.autojs6.com/#/video)와 [audio](https://docs.autojs6.com/#/audio)에 있습니다):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }]);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });
toastLog(video.toGif(joined.path, { fps: 10, width: 480 }).path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }]);

images.requestScreenCapture();
let recording = video.record({ audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.compose({
    video: [{ clips: [
        { src: "/sdcard/Movies/a.mp4", end: 8000 },
        { src: "/sdcard/Movies/b.mp4", start: 2000, end: 10000, transition: { type: "dissolve", duration: 500 } },
    ] }],
    audio: [{ clips: [{ src: "/sdcard/Music/bgm.m4a", end: 15500, volume: 0.4, fadeIn: 1000, fadeOut: 2000 }] }],
    overlays: [{ type: "text", text: "AutoJs6", from: 1000, to: 4000, x: "50%", y: "10%", fontSize: 48 }],
});

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- 각 처리 메서드에는 완료될 때까지 기다리는 형태와 Promise를 반환하는 `xxxAsync` 형태가 있습니다. `options.onProgress(progress)`는 `percent`, `stage`, `etaMillis`를 알려 주며, 스크립트를 멈추면 그 작업은 취소됩니다.
- `output`을 생략하면 동영상은 `Movies/3-Slate Studio/`에, 오디오는 `Music/3-Slate Studio/`에, 프레임, 썸네일, GIF는 `Pictures/3-Slate Studio/`에 원본 파일 이름, 작업, 시각으로 된 이름으로 저장됩니다. 결과에는 실제 경로와 미디어 정보가 들어 있으며 미디어 라이브러리에도 등록됩니다.
- `video.compose(timeline)`는 객체로 기술한 멀티트랙 타임라인 (동영상 트랙 최대 8개와 오디오 트랙 최대 16개, 클립, 전환 효과, 오버레이, 자막)을 렌더링합니다. 형식은 타임라인 편집기가 내보내는 것과 같습니다.
- `video.record`와 `audio.record`는 AutoJs6가 녹화, 녹음하며, 경로를 생략하면 `Movies/AutoJs6/`와 `Music/AutoJs6/`에 저장합니다. 화면을 녹화하기 전에 `images.requestScreenCapture`를 먼저 호출해야 합니다.
- `video.tasks()` / `audio.tasks()`는 스크립트의 진행 중인 작업을 나열하고 취소할 수 있습니다. 실패하면 `MediaStudioError`가 발생하며, 그 `code`는 `INVALID_OPTIONS`나 `STORAGE_PERMISSION_DENIED` 같은 안정적인 오류 코드입니다.

******

### 예제 스크립트

******

이 저장소의 [samples](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/tree/master/samples) 폴더에는 그대로 실행할 수 있는 예제 4개가 있으며, AutoJs6에 포함된 예제에도 같은 스크립트가 있습니다 (3-Slate Studio 폴더):

- 화면을 10초 녹화해 GIF로: `video.record`로 소리 없는 화면을 녹화한 뒤 `video.toGif`로 초당 10프레임, 너비 480픽셀의 GIF로 변환합니다 (처음 실행할 때 화면 캡처를 확인해야 합니다).
- 녹음해 FLAC으로: `audio.record`로 마이크를 녹음한 뒤 `audio.convert`로 무손실 FLAC으로 변환합니다 (처음 실행할 때 녹음 권한을 허용해야 합니다).
- 자르기, 워터마크, 이어 붙이기: `video.probe`로 소재 정보를 읽고, `video.trim`으로 처음 5초를 잘라 내고, `video.overlay`로 텍스트 워터마크를 넣은 뒤, `video.concat`으로 이어 붙이며 진행 상황을 알립니다.
- 타임라인 합성: `video.compose`로 동영상 2개 (디졸브 전환), 배경 음악 (페이드), 텍스트 오버레이를 세로 동영상 하나로 합칩니다.

뒤의 두 예제는 기본적으로 `/sdcard/Movies/a.mp4`, `/sdcard/Movies/b.mp4`, `/sdcard/Music/bgm.m4a`를 읽습니다. 소재를 넣어 두거나 경로를 직접 바꾸세요.

******

### 호환성

******

플러그인은 Android 7.0 (API 24) 이상을 지원하며 AutoJs6 없이도 단독으로 쓸 수 있습니다. 플러그인 센터에서 활성화하고 스크립트에서 `video`와 `audio`의 처리 메서드를 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요합니다 (F10). 두 모듈은 Rhino, Node.js, Bun 엔진에서 쓸 수 있습니다.

괄호 안의 번호는 [개발 로드맵 부록 F](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)의 증거 항목을 가리킵니다. "테스트 안 함"은 1.0.0 출시 전에 해당 버전이나 형식에서 확인하지 않았다는 뜻이며, 쓸 수 없다는 뜻은 아닙니다.

**Android 버전별**

| Android | 확인한 기기 | 처리 | 플러그인 화면 | Studio 녹음기 | 예제 스크립트 |
| --- | --- | --- | --- | --- | --- |
| 7.0 (API 24) | Android 에뮬레이터 | 통과 (F5) | 통과 (F4, F6) | 테스트 안 함 | 테스트 안 함 |
| 7.1 - 8.1 (API 25 - 27) | - | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 |
| 9 (API 28) | Sony G8441 | 통과 (F3) | 녹음 화면 통과 (F8) | 통과, 마이크만 (F8) | 테스트 안 함 |
| 10 - 11 (API 29 - 30) | - | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 |
| 12 (API 31) | Sony XQ-AT72 | 통과 (F3, F5) | 통과 (F4, F5, F7) | 통과, 내부 소리 포함 (F8) | 테스트 안 함 |
| 12L (API 32) | - | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 |
| 13 (API 33) | Sony XQ-DQ72, Redmi 22120RN86C | 통과 (F1, F3) | 통과 (F6, F8) | 통과, 내부 소리 포함 (F8) | 통과 (F9) |
| 14 (API 34) | - | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 | 테스트 안 함 |
| 15 (API 35) | Xiaomi Pad 23046RP50C, OnePlus OPD2413, Android 에뮬레이터 | 통과 (F1, F2, F3, F5) | 통과 (F4, F5, F6) | 통과, 내부 소리 포함 (F8) | 테스트 안 함 |

처리: 스크립트 작업 25개 또는 도구 상자 마법사 20개; 플러그인 화면: 미디어 라이브러리, 도구 상자, 작업, 설정, 타임라인 편집기; 예제 스크립트: 위의 예제 4개.

**형식별**

| 형식 | 입력 | 출력 | 참고 |
| --- | --- | --- | --- |
| MP4: H.264 동영상 + AAC 오디오 | 통과 (F2, F3) | 통과 (F2, F3) | 동영상의 기본 출력 형식 |
| MP4: H.265 / HEVC 동영상 | 테스트 안 함 | 통과 (F1, F2, F3) | 기기에 H.265 인코더가 있을 때만 선택할 수 있으며, 확인한 휴대폰과 태블릿 6대 모두 있음 |
| M4A (AAC) | 통과 (F2, F3) | 통과 (F2, F3, F8) | 오디오의 기본 출력 형식이자 Studio 녹음기의 기본 형식 |
| WAV (16비트 PCM) | 통과 (F2) | 통과 (F2, F3) | 무손실, 파일이 큼 |
| FLAC | 테스트 안 함 | 통과 (F2, F3) | 무손실 압축 |
| OGG (Opus) | 테스트 안 함 | 통과 (F2, F3) | Android 10 이상 필요 (시스템이 Android 10부터 OGG를 쓸 수 있음), 확인한 버전은 Android 12 - 15 |
| GIF | 테스트 안 함 | 통과 (F2, F3) | 내장 인코더, 초당 1 - 50프레임 |
| PNG, JPG 이미지 | 통과 (F2, F3) | 통과 (F2, F3) | 입력은 오버레이용 이미지, 출력은 프레임과 썸네일 |
| SRT 자막 | 통과 (F2, F3, F6) | 통과 (F2) | 타임라인 편집기로 가져오거나 화면에 입힘 |
| ASS 자막 (기본 하위 집합) | 통과 (F2) | 통과 (F2, F3) | 지원하지 않는 태그는 경고와 함께 무시됨. 완전한 ASS 렌더링은 1.1.0 예정 |
| MP3, AMR 등 기타 오디오 | 테스트 안 함 | 1.0.0에서는 제공 안 함 | 읽을 수 있는지는 기기의 시스템 디코더에 따라 다름. MP3 출력은 1.1.0의 FFmpeg 라이브러리와 함께 예정 |
| WebM, MKV | 테스트 안 함 | 1.0.0에서는 제공 안 함 | 1.1.0의 FFmpeg 라이브러리와 함께 예정 |

******

### 자주 묻는 질문

******

#### AutoJs6 없이 쓸 수 있나요?

쓸 수 있습니다. 미디어 라이브러리, 도구 상자, 타임라인 편집기, Studio 녹음기는 단독으로 동작합니다. AutoJs6는 스크립트로 미디어를 처리할 때만 필요합니다.

#### 플러그인이 모든 파일 접근 권한을 요청하는 이유는 무엇인가요?

스크립트는 `/sdcard/DCIM/...` 같은 일반 파일 경로를 넘기고, 플러그인은 3-Maple Image처럼 그 경로를 직접 읽고 씁니다. 권한이 없으면 처리는 `STORAGE_PERMISSION_DENIED`를 알리고, 플러그인 화면에서 권한을 허용하는 방법을 안내합니다. `video.frame`은 `Image` 객체를 반환하지만, 그림을 `Movies/3-Slate Studio/`의 임시 이미지를 거쳐 AutoJs6에 넘기므로 같은 권한이 필요합니다.

#### 화면 녹화에 플러그인이 필요한가요?

필요하지 않습니다. `video.record`와 `audio.record`는 AutoJs6 (6.8.1 이상)에 내장되어 있습니다. 플러그인은 처리 메서드와 자체 Studio 녹음기를 더합니다.

#### 오래된 AutoJs6에서도 쓸 수 있나요?

스크립트에서 플러그인을 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요하며, 그보다 오래된 AutoJs6에는 아직 `video`와 `audio` 모듈이 없습니다. 플러그인 자체는 독립된 편집 앱으로 계속 쓸 수 있습니다.

#### 타임라인 내보내기가 `CODEC_ERROR`를 알리고 트랙을 줄이라고 합니다. 어떻게 해야 하나요?

겹치는 전환 효과 (디졸브, 슬라이드, 와이프, 줌, 블러) 동안 동영상 트랙은 클립 두 개를 동시에 디코딩하는데, 기기의 코덱 수에는 한계가 있습니다. 확인한 Sony 휴대폰 3대에서는 전환 효과가 있는 동영상 트랙 6개와 오디오 트랙 16개를 내보낼 수 있었고, 8 + 16은 `CODEC_ERROR`로 끝났습니다. 동영상 트랙이나 겹치는 전환 효과를 줄이세요.

******

### 권한과 보안

******

플러그인이 요청하는 권한과 용도:

- 모든 파일 접근 (`MANAGE_EXTERNAL_STORAGE`, Android 10 이하는 저장소 읽기 / 쓰기 권한): 스크립트와 화면이 지정한 파일 경로를 읽고 쓰며, 경로를 지정하지 않은 출력을 `Movies`, `Music`, `Pictures` 아래의 `3-Slate Studio` 폴더에 씁니다.
- 미디어 읽기 (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, Android 13 이상): 홈 화면의 미디어 라이브러리에 기기의 동영상과 오디오를 나열합니다.
- 마이크 (`RECORD_AUDIO`): Studio 녹음기만 사용합니다. 스크립트의 `audio.record`와 `video.record`는 AutoJs6가 자체 녹음 권한으로 녹음합니다.
- 화면 캡처 (시스템의 화면 공유 동의이며 매니페스트 권한이 아님): Studio 녹음기가 기기 내부 소리 (내부 소리 또는 혼합)를 수집할 때 필요하며, 녹음을 시작할 때 시스템이 묻습니다.
- 알림 (`POST_NOTIFICATIONS`, Android 13 이상): 처리 진행 상황과 일시 중지 / 중지 버튼이 있는 녹음 알림을 표시합니다. 허용하지 않으면 이 알림은 표시되지 않습니다.
- 포그라운드 서비스와 절전 해제 잠금 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`): 긴 처리를 백그라운드에서 계속하고 화면이 꺼져도 녹음을 이어 갑니다.
- 네트워크 (`INTERNET`, `ACCESS_NETWORK_STATE`): 1.0.0에서는 업데이트 확인에만 씁니다. 홈 화면을 열 때 최대 12시간에 한 번 공식 플러그인 색인을 읽으며, 설정에서 끌 수 있습니다.
- AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`): AutoJs6와 통신합니다.

플러그인은 권한 범위를 최대한 좁게 유지합니다:

- 보호된 진입점: 기능 서비스와 활성화 진입점은 AutoJs6 플러그인 권한 (`org.autojs.permission.PLUGIN`)으로 보호되어 다른 앱이 직접 호출할 수 없습니다.
- 저장소: 모든 파일 접근 권한은 스크립트와 Studio 화면이 지정한 경로에만 쓰입니다. 경로를 지정하지 않은 출력은 `Movies`, `Music`, `Pictures` 아래의 `3-Slate Studio` 폴더에만 기록됩니다.
- 녹음: 마이크와 기기 내부 소리는 Studio 녹음기가 동작하는 동안에만, 포그라운드 알림을 표시한 채로 수집됩니다. 기기 내부 소리를 수집하려면 시스템의 화면 캡처 동의도 필요합니다.
- 네트워크: 1.0.0에서는 업데이트 확인에만 씁니다. 예정된 FFmpeg 라이브러리 (1.1.0)는 요청할 때만 내려받으며, 불러오기 전에 SHA-256으로 검증합니다.

******

### FFmpeg 라이브러리

******

FFmpeg 라이브러리는 1.1.0에서 제공할 예정이며 LGPL-2.1-or-later로 빌드합니다. 라이브러리는 APK에 들어 있지 않으며, 요청할 때 기기의 ABI용만 내려받아 동적으로 불러옵니다. 자체 라이선스 고지와 해당 소스 코드를 받을 수 있는 위치가 함께 제공됩니다. 라이브러리가 없어도 1.0.0의 모든 기능은 그대로 쓸 수 있습니다. 지원 형식과 스크립트 인터페이스는 1.1.0 출시 때 안내합니다.

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

_2026/11/08_

- `힌트` 스크립트에서 video / audio의 처리 메서드를 쓰려면 AutoJs6 6.8.1 (내부 빌드 6641) 이상이 필요합니다. 플러그인 화면, 타임라인 편집기, Studio 녹음기는 AutoJs6에 의존하지 않습니다
- `힌트` 스크립트 API는 AutoJs6 문서의 [video](https://docs.autojs6.com/#/video)와 [audio](https://docs.autojs6.com/#/audio) 장을 참고하세요
- `기능` 동영상 처리: 자르기, 이어 붙이기, 속도 변경 (0.25 - 8배), 크기 조정, 회전, 잘라내기, 뒤집기, 텍스트나 이미지 워터마크, 오디오 트랙 추출과 제거, 트랜스코딩 (H.264, 기기가 지원하면 H.265)
- `기능` 프레임, 썸네일, GIF: PNG / JPG 파일 또는 Image 객체로 얻기, 내장 GIF 인코더 (팔레트, 디더링, 반복)
- `기능` 오디오 처리: 자르기, 이어 붙이기, 믹스 (클리핑될 때 자동으로 줄임), 볼륨과 페이드, 속도 변경, M4A / WAV / FLAC / OGG로 변환 (OGG는 Android 10 이상)
- `기능` video.compose 멀티트랙 합성: 동영상 트랙 최대 8개와 오디오 트랙 최대 16개, 7가지 전환 효과, 텍스트 / 이미지 오버레이와 자막
- `기능` 자막: SRT 읽기와 쓰기 및 ASS 기본 하위 집합, video.burnSubtitles로 화면에 입히기, 자막 파일을 읽고 쓰는 video.subtitles.parse / write
- `기능` AutoJs6 스크립트 모듈 video / audio (AutoJs6 6.8.1, Rhino, Node.js, Bun): 모든 처리 메서드에 xxxAsync 형태와 진행 콜백이 있으며, 스크립트를 멈추면 그 작업은 취소됩니다
- `기능` Studio 화면: 미디어 라이브러리, 도구 상자, 작업 대기열, 설정. 외관은 AutoJs6를 따르거나 언어, 야간 모드, 테마 색을 따로 고를 수 있으며, 설정에서 모든 파일 접근과 배터리 제한 없음을 안내합니다
- `기능` 도구 상자 마법사: 처리 기능마다 마법사 하나, 기본값과 범위를 보여 주는 양식, 출력, 미디어 정보, 같은 동작의 스크립트를 보여 주는 결과 페이지
- `기능` 타임라인 편집기: 멀티트랙 편집, 전환 효과, 오버레이와 자막 편집, 실시간 미리보기, 실행 취소 / 다시 실행, .slate.json 프로젝트 저장, 작업 대기열로 내보내기
- `기능` 코드 생성: 마법사, 타임라인, 녹음기가 같은 동작의 완전한 스크립트를 보여 주며, 복사하거나 AutoJs6에서 새 스크립트로 열 수 있습니다
- `기능` Studio 녹음기: 마이크, 기기 내부 소리 (Android 10 이상), 또는 둘 다를 M4A / WAV로 녹음, 일시 중지 / 다시 시작, 화면이 꺼진 상태의 녹음, 알림에서 제어
- `기능` 긴 작업은 진행 알림이 있는 포그라운드 서비스에서 실행됩니다. 설정에서 하드웨어 인코더 선호와 동시에 실행할 작업 수 (1 - 2)를 고를 수 있습니다
- `기능` 경로를 지정하지 않은 출력은 Movies, Music, Pictures 아래의 3-Slate Studio 폴더에 저장되고 미디어 라이브러리에 등록됩니다
- `기능` 업데이트 확인: 홈 화면을 열 때 최대 12시간에 한 번 공식 플러그인 색인을 읽으며, 특정 버전을 무시할 수 있습니다
- `기능` 모든 ABI에 공통인 APK 하나: 유일한 네이티브 라이브러리는 Jetpack Compose의 것이며 16 KB 메모리 페이지에 맞춰 정렬되어 있습니다
- `기능` 10개 언어의 화면, README, 플러그인 센터 설명, 변경 기록
- `의존성` Jetpack Media3 1.10.1 추가 (Transformer, Effect, Muxer, ExoPlayer, UI, 처리와 미리보기용)
- `의존성` Jetpack Compose BOM 2026.09.00 추가 (플러그인 화면)
- `의존성` AndroidX Activity Compose 1.13.0 추가
- `의존성` Navigation Compose 2.10.2 추가
- `의존성` AndroidX Core KTX 1.18.0 추가
- `의존성` Material Color Utilities (MaterialKolor) 4.1.1 추가 (플러그인 화면의 테마 색)

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
