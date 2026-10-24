# システム機能との共存

## 純正アプリを有効に保つ

Xiaomi MiTV-MOEU0（Android 14）では、Google TV純正ホームが優先度2を持ち、通常アプリのHOMEロール指定だけではHomeキーの行き先を変えられません。一方、純正ホームパッケージ `com.google.android.apps.tv.launcherx` は、右側設定パネル `dashboard.DashboardHandler` と複数のグローバルキー受信機能も提供しています。

そのため、1.0.4以降は次の両パッケージを有効に戻し、無効化による置き換えを使いません。

- `com.google.android.apps.tv.launcherx`
- `com.google.android.tungsten.setupwraith`

初期設定アプリの無効化中は、端末ログにそのContentProviderへの取得失敗も繰り返し出ていました。設定画面を明示的に起動できるだけでは、リモコンの右側パネルが正常に動くことの検証にはなりません。

## ホーム切替サービス

ユーザーが有効にした `HomeRedirectService` は、純正ランチャーの `TYPE_WINDOW_STATE_CHANGED` だけを受け取ります。パッケージとクラスが以下に一致するときだけQuiet TVを開きます。

- `com.google.android.apps.tv.launcherx.home.HomeActivity`
- `com.google.android.apps.tv.launcherx.home.VanillaModeHomeActivity`

`dashboard.DashboardHandler`、その他の設定、HDMI映像アプリ、Quiet TV自身の画面は切替対象外です。設定パネルはQuiet TVの上にそのまま表示できます。

`canRetrieveWindowContent` はfalse。キーイベントのフィルタも要求しません。画面内容、文字入力、リモコン入力の監視やポーリングを追加しません。サービスの停止時や起動に失敗した場合は純正ホームが使える状態を保ちます。切替時に純正ホームが一瞬表示される場合があります。

## 戻す方法

「設定 → ユーザー補助」で「Quiet TV ホーム切替」を無効にしてください。ADBで戻す場合は次を使います。

```sh
ADB=/path/to/adb sh scripts/restore-google-tv.sh TV_IP:PORT
```

既存のButton Mapperなどのユーザー補助サービスは一覧に残します。純正2パッケージの状態と既定ホームも戻すため、旧版からの復旧にも使えます。

## 起動時のサウンドバーのポップ音

接続はPolk Signa S4のHDMI。ユーザー申告では、待機状態からリモコンで電源を入れ、画面が点灯したタイミングで発生します。

調査時の端末ログにはeARCの再初期化、ARC終了処理の失敗、音声デバイスの切断・再接続がありました。ただし、これはポップ音との因果関係を証明するものではありません。Quiet TVは音声出力・音量・HDMI-CEC設定を操作していません。

純正システム機能を有効に戻し、ホーム切替サービスを有効にした状態で、2026-10-24にユーザーが待機からの電源オンを確認し、「ポップ音は鳴りませんでした」と回答しました。これは今回の実機確認の結果であり、eARC内部の原因を特定したという意味ではありません。

調査・修正では電源操作の自動反復、音量・eARC・音声形式の変更を行っていません。今後の確認でも、ログだけで音の解消を判定せず実際の症状を確認します。
