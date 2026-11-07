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

> 1.0.0 は最初の正式リリースです. スクリプトでメディアを処理するには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. プラグインの画面, タイムラインエディター, Studio レコーダーは AutoJs6 がなくても使えます.

******

### 機能ハイライト

******

- 動画編集: 時間指定のトリミング, 結合, 速度変更 (0.25 倍から 8 倍), 拡大縮小, 回転, 切り抜き, 反転, テキストや画像の透かし, 音声トラックの抽出と削除, トランスコード (既定は H.264, 端末に H.265 エンコーダーがあれば H.265).
- フレームと GIF: 単一フレーム, フレーム列, サムネイルを PNG や JPG のファイル, または `Image` オブジェクトとして取得し, 内蔵エンコーダーで GIF アニメーションを作成します.
- 音声: トリミング, 結合, 複数入力のミックス (合計がクリップする場合は自動で縮小), フェードイン / フェードアウト付きの音量調整, 速度変更, M4A, WAV, FLAC, OGG への変換 (OGG は Android 10 以降).
- マルチトラックタイムラインエディター: 動画, 音声, オーバーレイ, 字幕のトラック, 7 種類のトランジション (カット, フェード, ディゾルブ, スライド, ワイプ, ズーム, ぼかし), リアルタイムプレビュー, 元に戻す / やり直し, `.slate.json` 形式でのプロジェクト保存. 字幕は SRT の読み込みと書き出し, ASS の基本サブセットに対応し, 書き出し時に映像へ焼き込みます.
- ツールボックス: 処理機能ごとにウィザードがあり, フォームには既定値と範囲が表示されます. タスクはキューで実行され, 進行状況と失敗の理由を表示し, 長いタスクにはフォアグラウンド通知が付きます.
- コード生成: 各ウィザード, タイムラインの書き出し, 録音から同等の `video.*` / `audio.*` スクリプトを生成し, コピーするか AutoJs6 で新しいスクリプトとして開けます.
- Studio レコーダー: プラグインの画面からマイク, 端末内部の音声 (Android 10 以降), またはその両方を M4A か WAV で録音します. 一時停止でき, 画面がオフでも録音を続けます.
- FFmpeg ライブラリ (1.1.0 で予定): 任意のダウンロードで, MP3, WebM, MKV などの形式と ASS 字幕の完全な描画を追加します.

******

### スクリーンショット

******

以下は Android 12 と 13 のスマートフォンで撮影したもので, ライトテーマ, 簡体字中国語の画面です.

| メディアライブラリ | ツールボックス | タイムラインエディター | Studio レコーダー |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/library.png?raw=true" alt="library" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/toolbox.png?raw=true" alt="toolbox" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/timeline.png?raw=true" alt="timeline" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/recorder.png?raw=true" alt="recorder" width="200" /> |

******

### 使用方法

******

1. AutoJs6 のプラグインセンターから 3-Slate Studio をインストールするか, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) から APK をダウンロードしてインストールします. パッケージはすべての ABI 共通の 1 つだけです.
2. プラグインを開き, 求められたらすべてのファイルへのアクセスを許可します. これで選んだパスを読み書きできるようになります.
3. スクリプトで `video` と `audio` の処理メソッドを使うには, AutoJs6 6.8.1 (内部バージョン 6641) 以降を使い, プラグインセンターでプラグインを有効にします.

> プラグインは Android 7.0 (API 24) 以降の端末に対応しています. 唯一のネイティブライブラリは Jetpack Compose のもので, 16 KB メモリページに揃えられているため, パッケージはすべての ABI 共通の 1 つです.

******

### スクリプト API

******

スクリプトはグローバルモジュール `video` と `audio` (`$video` と `$audio` とも書けます) からこれらの機能を使います (すべてのメソッドとオプションは AutoJs6 のドキュメントの [video](https://docs.autojs6.com/#/video) と [audio](https://docs.autojs6.com/#/audio) にあります):

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

- 各処理メソッドには, 完了まで待つ形式と Promise を返す `xxxAsync` 形式があります. `options.onProgress(progress)` は `percent`, `stage`, `etaMillis` を報告し, スクリプトを停止するとそのタスクは取り消されます.
- `output` を省略すると, 動画は `Movies/3-Slate Studio/` に, 音声は `Music/3-Slate Studio/` に, フレーム, サムネイル, GIF は `Pictures/3-Slate Studio/` に, 元ファイル名, 操作, 時刻からなる名前で保存されます. 結果には実際のパスとメディア情報が含まれ, メディアライブラリにも登録されます.
- `video.compose(timeline)` は, オブジェクトで記述したマルチトラックのタイムライン (動画トラック最大 8 本と音声トラック最大 16 本, クリップ, トランジション, オーバーレイ, 字幕) を描画します. 形式はタイムラインエディターの書き出しと同じです.
- `video.record` と `audio.record` は AutoJs6 が録画, 録音し, パスを省略すると `Movies/AutoJs6/` と `Music/AutoJs6/` に保存します. 画面録画の前に `images.requestScreenCapture` を呼び出す必要があります.
- `video.tasks()` / `audio.tasks()` はスクリプトの実行中タスクを一覧し, 取り消すことができます. 失敗すると `MediaStudioError` が投げられ, その `code` は `INVALID_OPTIONS` や `STORAGE_PERMISSION_DENIED` のような安定したエラーコードです.

******

### サンプルスクリプト

******

このリポジトリの [samples](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/tree/master/samples) フォルダーには, そのまま実行できる 4 つのサンプルがあります. AutoJs6 に付属するサンプルにも同じスクリプトがあります (3-Slate Studio フォルダー):

- 画面を 10 秒録画して GIF に: `video.record` で音声なしの画面を録画し, `video.toGif` で毎秒 10 フレーム, 幅 480 ピクセルの GIF に変換します (初回は画面キャプチャの確認が必要です).
- 録音して FLAC に: `audio.record` でマイクを録音し, `audio.convert` でロスレスの FLAC に変換します (初回は録音の権限が必要です).
- トリミング, 透かし, 結合: `video.probe` で素材の情報を読み取り, `video.trim` で最初の 5 秒を切り出し, `video.overlay` でテキストの透かしを入れ, `video.concat` で結合して進行状況を報告します.
- タイムライン合成: `video.compose` で 2 本の動画 (ディゾルブ付き), BGM (フェード付き), テキストのオーバーレイを 1 本の縦長動画にまとめます.

後の 2 つのサンプルは既定で `/sdcard/Movies/a.mp4`, `/sdcard/Movies/b.mp4`, `/sdcard/Music/bgm.m4a` を読み込みます. 素材を置くか, パスを自分のものに変更してください.

******

### 互換性

******

プラグインは Android 7.0 (API 24) 以降に対応し, AutoJs6 がなくても単独で使えます. プラグインセンターで有効にし, スクリプトで `video` と `audio` の処理メソッドを使うには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です (F10). 両モジュールは Rhino, Node.js, Bun の各エンジンで使えます.

括弧内の番号は [開発ロードマップの付録 F](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md) の証拠項目を指します. "未テスト" は 1.0.0 のリリース前にそのバージョンや形式で確認していないという意味で, 使えないという意味ではありません.

**Android バージョン別**

| Android | 確認した端末 | 処理 | プラグイン画面 | Studio レコーダー | サンプルスクリプト |
| --- | --- | --- | --- | --- | --- |
| 7.0 (API 24) | Android エミュレーター | 合格 (F5) | 合格 (F4, F6) | 未テスト | 未テスト |
| 7.1 - 8.1 (API 25 - 27) | - | 未テスト | 未テスト | 未テスト | 未テスト |
| 9 (API 28) | Sony G8441 | 合格 (F3) | 録音画面は合格 (F8) | 合格, マイクのみ (F8) | 未テスト |
| 10 - 11 (API 29 - 30) | - | 未テスト | 未テスト | 未テスト | 未テスト |
| 12 (API 31) | Sony XQ-AT72 | 合格 (F3, F5) | 合格 (F4, F5, F7) | 合格, 内部音声を含む (F8) | 未テスト |
| 12L (API 32) | - | 未テスト | 未テスト | 未テスト | 未テスト |
| 13 (API 33) | Sony XQ-DQ72, Redmi 22120RN86C | 合格 (F1, F3) | 合格 (F6, F8) | 合格, 内部音声を含む (F8) | 合格 (F9) |
| 14 (API 34) | - | 未テスト | 未テスト | 未テスト | 未テスト |
| 15 (API 35) | Xiaomi Pad 23046RP50C, OnePlus OPD2413, Android エミュレーター | 合格 (F1, F2, F3, F5) | 合格 (F4, F5, F6) | 合格, 内部音声を含む (F8) | 未テスト |

処理: 25 のスクリプト操作または 20 のツールボックスウィザード. プラグイン画面: メディアライブラリ, ツールボックス, タスク, 設定, タイムラインエディター. サンプルスクリプト: 前述の 4 つのサンプル.

**形式別**

| 形式 | 入力 | 出力 | 備考 |
| --- | --- | --- | --- |
| MP4: H.264 動画 + AAC 音声 | 合格 (F2, F3) | 合格 (F2, F3) | 動画の既定の出力形式 |
| MP4: H.265 / HEVC 動画 | 未テスト | 合格 (F1, F2, F3) | 端末に H.265 エンコーダーがある場合のみ選択可能. 確認した 6 台のスマートフォンとタブレットはすべて対応 |
| M4A (AAC) | 合格 (F2, F3) | 合格 (F2, F3, F8) | 音声の既定の出力形式で, Studio レコーダーの既定の形式でもあります |
| WAV (16 ビット PCM) | 合格 (F2) | 合格 (F2, F3) | ロスレス, ファイルが大きい |
| FLAC | 未テスト | 合格 (F2, F3) | ロスレス圧縮 |
| OGG (Opus) | 未テスト | 合格 (F2, F3) | Android 10 以降が必要 (システムが OGG を書き出せるのは Android 10 から). 確認したのは Android 12 - 15 |
| GIF | 未テスト | 合格 (F2, F3) | 内蔵エンコーダー, 毎秒 1 - 50 フレーム |
| PNG, JPG 画像 | 合格 (F2, F3) | 合格 (F2, F3) | 入力はオーバーレイ用の画像, 出力はフレームとサムネイル |
| SRT 字幕 | 合格 (F2, F3, F6) | 合格 (F2) | タイムラインエディターへの読み込みと映像への焼き込み |
| ASS 字幕 (基本サブセット) | 合格 (F2) | 合格 (F2, F3) | 未対応のタグは警告とともに無視されます. ASS の完全な描画は 1.1.0 で予定 |
| MP3, AMR などその他の音声 | 未テスト | 1.0.0 では非対応 | 読み込めるかどうかは端末のシステムデコーダー次第. MP3 の出力は 1.1.0 の FFmpeg ライブラリで予定 |
| WebM, MKV | 未テスト | 1.0.0 では非対応 | 1.1.0 の FFmpeg ライブラリで予定 |

******

### よくある質問

******

#### AutoJs6 がなくても使えますか?

使えます. メディアライブラリ, ツールボックス, タイムラインエディター, Studio レコーダーは単独で動作します. AutoJs6 が必要なのは, スクリプトでメディアを処理するときだけです.

#### プラグインがすべてのファイルへのアクセスを求めるのはなぜですか?

スクリプトは `/sdcard/DCIM/...` のような通常のファイルパスを渡し, プラグインは 3-Maple Image と同じようにそのパスを直接読み書きします. 許可がないと処理は `STORAGE_PERMISSION_DENIED` を報告し, プラグインの画面に許可の方法が表示されます. `video.frame` は `Image` オブジェクトを返しますが, 画像は `Movies/3-Slate Studio/` の一時ファイルを経由して AutoJs6 に渡されるため, 同じ権限が必要です.

#### 画面録画にプラグインは必要ですか?

必要ありません. `video.record` と `audio.record` は AutoJs6 (6.8.1 以降) に組み込まれています. プラグインは処理メソッドと独自の Studio レコーダーを追加します.

#### 古い AutoJs6 でも使えますか?

スクリプトからプラグインを使うには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要で, それより古い AutoJs6 には `video` と `audio` モジュールがまだありません. プラグイン自体は単独の編集アプリとして使えます.

#### タイムラインの書き出しが `CODEC_ERROR` になり, トラックを減らすよう表示されます. どうすればよいですか?

重なるトランジション (ディゾルブ, スライド, ワイプ, ズーム, ぼかし) の間, 動画トラックは 2 つのクリップを同時にデコードしますが, 端末のコーデックの数には限りがあります. 確認した 3 台の Sony のスマートフォンでは, トランジション付きの動画トラック 6 本と音声トラック 16 本は書き出せ, 8 + 16 は `CODEC_ERROR` で終わりました. 動画トラックか重なるトランジションを減らしてください.

******

### 権限とセキュリティ

******

プラグインが要求する権限とその用途:

- すべてのファイルへのアクセス (`MANAGE_EXTERNAL_STORAGE`, Android 10 以前はストレージの読み書き権限): スクリプトと画面が指定したファイルパスを読み書きし, パスを指定しない出力を `Movies`, `Music`, `Pictures` の下の `3-Slate Studio` フォルダーに書き込みます.
- メディアの読み取り (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, Android 13 以降): ホーム画面のメディアライブラリに端末の動画と音声を一覧表示します.
- マイク (`RECORD_AUDIO`): Studio レコーダーだけが使います. スクリプトの `audio.record` と `video.record` は, AutoJs6 が自身の録音権限で録音します.
- 画面キャプチャ (システムの画面共有の同意で, マニフェストの権限ではありません): Studio レコーダーが端末内部の音声 (内部音声またはミックス) を取り込むときに必要で, 録音の開始時にシステムが確認します.
- 通知 (`POST_NOTIFICATIONS`, Android 13 以降): 処理の進行状況と, 一時停止と停止のボタンが付いた録音の通知を表示します. 許可しない場合, これらの通知は表示されません.
- フォアグラウンドサービスとウェイクロック (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`): 長い処理をバックグラウンドで続け, 画面がオフでも録音を続けます.
- ネットワーク (`INTERNET`, `ACCESS_NETWORK_STATE`): 1.0.0 では更新の確認にだけ使います. ホーム画面を開いたときに最大 12 時間に 1 回, 公式プラグインインデックスを読み込みます. 設定でオフにできます.
- AutoJs6 プラグイン権限 (`org.autojs.permission.PLUGIN`): AutoJs6 との通信に使います.

プラグインは必要な権限をできるだけ少なくしています:

- 保護された入口: 機能サービスと有効化の入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護されており, 他のアプリから直接呼び出すことはできません.
- ストレージ: すべてのファイルへのアクセスは, スクリプトと Studio の画面が指定したパスにだけ使われます. パスを指定しない出力は `Movies`, `Music`, `Pictures` の下の `3-Slate Studio` フォルダーにだけ書き込まれます.
- 録音: マイクと端末内部の音声は Studio レコーダーの動作中にだけ, フォアグラウンド通知を表示したうえで取り込まれます. 端末内部の音声の取り込みには, システムの画面キャプチャの同意も必要です.
- ネットワーク: 1.0.0 では更新の確認にだけ使います. 予定している FFmpeg ライブラリ (1.1.0) は要求したときにだけダウンロードされ, 読み込む前に SHA-256 で検証されます.

******

### FFmpeg ライブラリ

******

FFmpeg ライブラリは 1.1.0 で予定しており, LGPL-2.1-or-later でビルドします. ライブラリは APK に含まれず, 要求したときに端末の ABI 用だけがダウンロードされて動的に読み込まれます. 独自のライセンス表示と, 対応するソースコードの入手先が付属します. ライブラリがなくても 1.0.0 の機能はすべて使えます. 対応する形式とスクリプトのインターフェースは 1.1.0 のリリース時に説明します.

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

_2026/11/08_

- `ヒント` スクリプトで video / audio の処理メソッドを使うには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. プラグインの画面, タイムラインエディター, Studio レコーダーは AutoJs6 に依存しません
- `ヒント` スクリプト API は AutoJs6 のドキュメントの [video](https://docs.autojs6.com/#/video) と [audio](https://docs.autojs6.com/#/audio) の章を参照してください
- `機能` 動画の処理: トリミング, 結合, 速度変更 (0.25 - 8 倍), 拡大縮小, 回転, 切り抜き, 反転, テキストや画像の透かし, 音声トラックの抽出と削除, トランスコード (H.264, 端末が対応していれば H.265)
- `機能` フレーム, サムネイル, GIF: PNG / JPG ファイルまたは Image オブジェクトとして取得, 内蔵 GIF エンコーダー (パレット, ディザリング, ループ)
- `機能` 音声の処理: トリミング, 結合, ミックス (クリップする場合は自動で縮小), 音量とフェード, 速度変更, M4A / WAV / FLAC / OGG への変換 (OGG は Android 10 以降)
- `機能` video.compose によるマルチトラック合成: 動画トラック最大 8 本と音声トラック最大 16 本, 7 種類のトランジション, テキスト / 画像のオーバーレイと字幕
- `機能` 字幕: SRT の読み書きと ASS の基本サブセット, video.burnSubtitles による映像への焼き込み, 字幕ファイルを読み書きする video.subtitles.parse / write
- `機能` AutoJs6 スクリプトモジュール video / audio (AutoJs6 6.8.1, Rhino, Node.js, Bun): 各処理メソッドに xxxAsync 形式と進行状況のコールバックがあり, スクリプトを停止するとそのタスクは取り消されます
- `機能` Studio の画面: メディアライブラリ, ツールボックス, タスクキュー, 設定. 外観は AutoJs6 に合わせるか, 言語, ナイトモード, テーマカラーを個別に選べます. 設定からすべてのファイルへのアクセスとバッテリーの制限なしを案内します
- `機能` ツールボックスのウィザード: 処理機能ごとのウィザード, 既定値と範囲を示すフォーム, 出力, メディア情報, 同等のスクリプトを表示する結果ページ
- `機能` タイムラインエディター: マルチトラック編集, トランジション, オーバーレイと字幕の編集, リアルタイムプレビュー, 元に戻す / やり直し, .slate.json 形式のプロジェクト保存, タスクキューへの書き出し
- `機能` コード生成: ウィザード, タイムライン, レコーダーが同等の完全なスクリプトを表示し, コピーするか AutoJs6 で新しいスクリプトとして開けます
- `機能` Studio レコーダー: マイク, 端末内部の音声 (Android 10 以降), またはその両方を M4A / WAV で録音. 一時停止 / 再開, 画面オフでの録音, 通知からの操作に対応
- `機能` 長いタスクは進行状況の通知付きのフォアグラウンドサービスで実行されます. 設定でハードウェアエンコーダーの優先と同時に実行するタスク数 (1 - 2) を選べます
- `機能` パスを指定しない出力は Movies, Music, Pictures の下の 3-Slate Studio フォルダーに保存され, メディアライブラリに登録されます
- `機能` 更新の確認: ホーム画面を開いたときに最大 12 時間に 1 回, 公式プラグインインデックスを読み込みます. 特定のバージョンを無視できます
- `機能` すべての ABI 共通の APK が 1 つ: 唯一のネイティブライブラリは Jetpack Compose のもので, 16 KB メモリページに揃えられています
- `機能` 10 言語の画面, README, プラグインセンターの説明, 更新履歴
- `依存関係` Jetpack Media3 1.10.1 を追加 (Transformer, Effect, Muxer, ExoPlayer, UI. 処理とプレビュー用)
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加 (プラグインの画面)
- `依存関係` AndroidX Activity Compose 1.13.0 を追加
- `依存関係` Navigation Compose 2.10.2 を追加
- `依存関係` AndroidX Core KTX 1.18.0 を追加
- `依存関係` Material Color Utilities (MaterialKolor) 4.1.1 を追加 (プラグインの画面のテーマカラー)

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
