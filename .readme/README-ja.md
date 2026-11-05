<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>動画と音声の加工, マルチトラックのタイムライン編集, トランジションと字幕, 画面の操作からスクリプトコードを生成</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### 概要

******

3-Slate Studio は AutoJs6 の動画と音声のプラグインで, AutoJs6 がなくても単独で使える編集アプリでもあります. 動画のトリミング, 結合, 速度変更, 拡大縮小, 回転, 切り抜き, 透かし, フレーム抽出, GIF 作成, トランスコードを行い, 音声のトリミング, 結合, ミックス, フェード, 変換を行います. マルチトラックのタイムラインエディターでは, クリップにトランジション, オーバーレイ, 字幕を組み合わせられます.

処理はプラグイン内で Jetpack Media3 Transformer と端末のハードウェアコーデックによって行われるため, AutoJs6 ホストは軽量なままです. AutoJs6 のスクリプトはグローバルモジュール `video` と `audio` でメディアを処理します. 画面録画 (`video.record`) と録音 (`audio.record`) は AutoJs6 に組み込まれており, プラグインがなくても使えます. プラグインの各ツール画面には同等のスクリプトコードが表示され, コピーしたり AutoJs6 で新しいスクリプトとして開いたりできます.

> 現在は開発プレビュー版です: AutoJs6 のプラグインセンターはプラグインを検出できますが, 処理機能, Studio の画面, `video` / `audio` モジュールの処理メソッドはまだ提供されていません. 以下は各バージョンの設計目標です. 進捗はロードマップを参照してください.

******

### 機能ハイライト

******

- 動画編集: 時間指定のトリミング, 結合, 速度変更 (0.25 倍から 8 倍), 拡大縮小, 回転, 切り抜き, 反転, テキストや画像の透かし, 音声トラックの抽出と削除, トランスコード (既定は H.264, 端末が対応していれば H.265).
- フレームと GIF: 単一フレーム, フレーム列, サムネイルを画像ファイルまたは `Image` オブジェクトとして取得し, 内蔵エンコーダーで GIF アニメーションを作成します.
- 音声: トリミング, 結合, 複数入力のミックス, フェードイン / フェードアウト付きの音量調整, 速度変更, M4A, WAV, FLAC, OGG への変換.
- マルチトラックタイムライン: 動画, 音声, オーバーレイのトラック, 7 種類のトランジション (カット, フェード, ディゾルブ, スライド, ワイプ, ズーム, ぼかし), SRT の読み込みと書き出しおよび ASS の基本サブセットに対応した字幕 (書き出し時に映像へ焼き込み).
- コード生成: 各ツール画面とタイムラインの書き出しから同等の `video.*` / `audio.*` コードを生成し, コピーするか AutoJs6 で新しいスクリプトとして開けます.
- Studio レコーダー: プラグインの画面からマイク, 端末内部の音声 (Android 10 以降), またはその両方を録音します.
- FFmpeg ライブラリ (1.1.0): 任意のダウンロードで, MP3, WebM, MKV の出力, `video.ffmpeg` によるコマンドの直接実行, ASS の完全な描画を追加します.

******

### 使用方法

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) から APK をダウンロードしてインストールします. パッケージはすべての ABI 共通の 1 つだけです.
2. プラグインを開き, 求められたらすべてのファイルへのアクセスを許可します. これで選んだパスを読み書きできるようになります.
3. スクリプトで `video` と `audio` の処理メソッドを使うには, AutoJs6 を内部バージョン 6641 (6.8.1) 以降に更新し, プラグインセンターでプラグインを有効にします.

> プラグインは Android 7.0 (API 24) 以降の端末に対応しています. 唯一のネイティブライブラリは Jetpack Compose のもので, 16 KB メモリページに揃えられているため, パッケージはすべての ABI 共通の 1 つです.

******

### スクリプト API

******

スクリプトはグローバルモジュール `video` と `audio` (`$video` と `$audio` とも書けます) からこれらの機能を使います (1.0.0 の設計草案です. リリース版のドキュメントが優先されます):

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

- 各処理メソッドには, 完了まで待つ形式と Promise を返す `xxxAsync` 形式があります. `options.onProgress(progress)` は `percent`, `stage`, `etaMillis` を報告し, スクリプトを停止するとそのタスクは取り消されます.
- `output` を省略すると, 動画は `Movies/3-Slate Studio/` に, 音声は `Music/3-Slate Studio/` に, 元ファイル名, 操作, 時刻からなる名前で保存されます. 結果には実際のパスとメディア情報が含まれ, メディアライブラリにも登録されます.
- `video.compose(timeline)` は, オブジェクトで記述したマルチトラックのタイムライン (トラック, クリップ, トランジション, オーバーレイ, 字幕) を描画します. 形式はタイムラインエディターの書き出しと同じです.
- `video.tasks()` / `audio.tasks()` はスクリプトの実行中タスクを一覧し, 取り消すことができます. 失敗すると `MediaStudioError` が投げられ, その `code` は `INVALID_OPTIONS` や `STORAGE_PERMISSION_DENIED` のような安定したエラーコードです.

******

### よくある質問

******

#### AutoJs6 がなくても使えますか?

使えます. ツール画面, タイムラインエディター, Studio レコーダーは単独で動作します. AutoJs6 が必要なのは, スクリプトでメディアを処理するときだけです.

#### プラグインがすべてのファイルへのアクセスを求めるのはなぜですか?

スクリプトは `/sdcard/DCIM/...` のような通常のファイルパスを渡し, プラグインは 3-Maple Image と同じようにそのパスを直接読み書きします. 許可がないと処理は `STORAGE_PERMISSION_DENIED` を報告し, プラグインの画面に許可の方法が表示されます.

#### 画面録画にプラグインは必要ですか?

必要ありません. `video.record` と `audio.record` は AutoJs6 (内部バージョン 6641 以降) に組み込まれています. プラグインは処理メソッドと独自の Studio レコーダーを追加します.

******

### 権限とセキュリティ

******

プラグインは必要な権限をできるだけ少なくしています:

- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません.
- ストレージ: すべてのファイルへのアクセスは, スクリプトと Studio の画面が指定したパスにだけ使われます. パスを指定しない出力は `Movies` と `Music` の下のプラグインフォルダーにだけ書き込まれます.
- 録音: マイクと端末内部の音声は Studio レコーダーの動作中にだけ, フォアグラウンド通知を表示したうえで取り込まれます. 端末内部の音声の取り込みには, システムの画面キャプチャの同意も必要です.
- ネットワーク: 1.0.0 では更新の確認にだけ使います. 1.1.0 以降, FFmpeg ライブラリは要求したときにダウンロードされ, 読み込む前に SHA-256 で検証されます.

******

### FFmpeg ライブラリ

******

1.1.0 以降, プラグインは LGPL-2.1-or-later でビルドした任意の FFmpeg ライブラリをダウンロードできます. ライブラリは APK に含まれず, 要求したときに端末の ABI 用だけがダウンロードされて動的に読み込まれます. 独自のライセンス表示と, 対応するソースコードの入手先が付属します. ライブラリがなくても 1.0.0 の機能はすべて使えます.

> FFmpeg が対応するコーデックの一部 (H.265 / HEVC, AAC など) は, 国や地域によって特許の対象となる場合があります. ライブラリは利便性のためだけに提供され, 特許ライセンスは付きません. 使う前に, ご自身に適用される規則を確認してください.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストとプラグイン開発者向けです. ホストはこれらの識別子でプラグインを検出し, 機能ネゴシエーションを行います:

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

### 開発ロードマップ

******

計画, 設計上の決定, 進捗は ROADMAP.md のチェックリストで管理しています. 未チェックの項目は計画であり, 現在のバージョンの機能ではありません.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/11/05_

- `ヒント` 開発プレビュー: AutoJs6 のプラグインセンターはプラグインを検出できますが, 処理機能, Studio の画面, video / audio スクリプトモジュールの処理メソッドはまだ提供されていません
- `ヒント` AutoJs6 6.8.1 (6641) 以降が必要です (正確な最低バージョンはホスト側の変更が入り次第記入します)
- `機能` プラグインの骨格: Wake 起動プロトコル, プラグインセンターの識別サービス, メディア処理の機能サービス (契約バージョン 1)
- `機能` ホーム画面からすべてのファイルへのアクセスを許可できます
- `機能` すべての ABI 共通の APK が 1 つ: 唯一のネイティブライブラリは Jetpack Compose のもので, 16 KB メモリページに揃えられています
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴を JSON ソースから生成
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加 (プラグインの画面)
- `依存関係` Navigation Compose 2.10.2 を追加
- `依存関係` Jetpack Media3 1.10.1 を追加 (処理とプレビュー用の Transformer, Effect, Muxer, ExoPlayer, UI)
- `依存関係` AndroidX Core KTX 1.18.0 を追加

##### その他のリリース履歴は以下を参照

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

この節はソースからプラグインをビルドしたい開発者向けです.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE) で公開されています. サードパーティのコンポーネントとデータについてはサードパーティ通知を参照してください.

- [サードパーティの表示 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
