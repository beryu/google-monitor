# Xiaomi実機の検証 — 2026-10-24

- 実機: Xiaomi TV S Mini LED 55 2025（ADBモデル名 `MiTV-MOEU0`）、Android 14。
- アプリ: `com.beryu.tvhome` / Quiet TV 1.0.4。compileSdk / targetSdk 36、minSdk 26。
- インストール済みAPK: `artifacts/quiet-tv-1.0.4.apk`（26,199 bytes）。
- SHA-256: `ecac80a6b77f9c66e9ce9a96c63c7933889c69bf8c082294caff9f55d0a9128c`。

## 1.0.3での初期検証

- ビルド成功、Lintエラー0件。TVの横向き指定や固定したビルドツールに関する警告は残っています。
- 大文字小文字・部分一致・設定名優先・未一致などのJavaテスト11件成功。
- 実機上の自動操作テスト成功。初期フォーカス、カード間の右移動、アプリ一覧の起動と移動、設定ボタンへの上移動、`ACTION_SETTINGS` の送信、戻る時のフォーカス復帰を確認。
- 実際のGoogle TV設定画面 `com.android.tv.settings/.MainSettings` をボタンから起動できることを確認。
- HDMI 3の設定名 `Switch` をそのままテキストで表示（ロゴは削除）。
- HDMI 1の設定名 `Playstation` をそのままテキストで表示（ロゴは削除）。
- 実際のカード操作後、TV Inputセッションがそれぞれ `com.mediatek.tis/.HdmiInputService/HW4`（HDMI 3）、`.../HW2`（HDMI 1）を選択することを確認。ゲーム機の映像・音声は未確認。
- Switchの文字色は `#E60012`、PlayStationの文字色は `#0070CC`。背景は通常時 `#F7F7F4`、フォーカス時 `#FFFFFF` に戻し、濃い枠線 `#566B7A` と太字・影で選択を示します。ロゴなしのテキスト表示です。
- ホームとアプリ一覧の実機スクリーンショットを `artifacts/tv-home.png` / `artifacts/tv-apps.png` に保存。
- 検証用APK `com.beryu.tvhome.test` は実機から削除済み。

## システム機能との互換性修正（1.0.4）

1.0.3では純正2パッケージを無効化していましたが、リモコンの右側設定パネルが開かない不具合が報告されました。調査の結果、右側パネルは純正ランチャーの `dashboard.DashboardHandler` が提供していることを確認しました。純正2パッケージをdefault状態へ復元すると、ユーザーの物理リモコンとADBの設定パネル用キー（KEYCODE_NOTIFICATION）の両方で右側パネルが復帰しました。

1.0.4は純正パッケージを有効のまま、純正ホーム画面だけをQuiet TVへ切り替える補助サービスを追加しています。画面内容の取得・キー入力の取得は要求しません。詳細は `docs/SYSTEM-INTEGRATION.md` を参照してください。

- 修正版のビルド・Lintエラー0件。
- 入力ルール11件・ホーム切り替えの分離ルール10件成功。
- 復元スクリプトの5ケースを端末に接続しないテストで確認。既存サービスを保持することを検証。
- 修正版APKは実機へインストール済み。
- 明示的なユーザー承認後に補助サービスを有効化済み。純正ホームと初期設定アプリを有効に戻した状態を維持しています。
- 実機でHomeキーからQuiet TVへの切り替え、Quiet TV上への純正右側設定パネルの表示（DashboardHandler）、戻るキーでの復帰を確認。画面は `artifacts/tv-settings-panel.png` に保存しました。
- `dumpsys accessibility` でホーム切替サービスのcapabilities=0、イベントはTYPE_WINDOW_STATE_CHANGEDのみであることを確認。既存のButton Mapperも有効のままです。
- 純正機能を復元し補助サービスを有効にした状態で、待機からの電源オン時にポップ音が鳴らなかったことをユーザーが確認（2026-10-24）。eARC内部処理との因果関係は断定していません。電源の自動反復・音量や音声形式の変更は行っていません。
- ユーザーの待機復帰確認後、最前面がQuiet TVであること、ホーム切替サービスとButton Mapperが有効であること、純正2パッケージが無効化されていないことをADBで確認しました。

旧版の初期検証結果は上記のとおりです。ゲーム機の映像・音声、再起動・ファームウェア更新後の動作、初期化による復元は未検証です。

## 工場出荷状態への初期化

旧版の変更はユーザーごとのパッケージ有効状態と既定ホームの設定で、システムファイルの削除・書き換えは行いません。この仕組みから、通常の工場出荷状態への初期化で純正ホームに戻ると考えられます。ただし、この実機で初期化しての復元は未検証です。

初期化はアプリ、アカウント、Wi-Fi設定なども消すため、通常はADBでの復元を優先します。Google TVの `RecoveryActivity` はアプリの画面であり、端末のリカバリーモードとは別です。Xiaomi TV S Mini LED 55 2025での物理ボタンによるリカバリー起動手順は未確認です。

参考: [Google TVの初期化](https://support.google.com/googletv/answer/10070482?hl=ja)、[Xiaomiの初期化案内（別機種のTV A 55 2025）](https://www.mi.com/global/support/faq/details/KA-481125/)、[AndroidのADBによるパッケージ有効状態の変更制限](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-dev/services/core/java/com/android/server/pm/PackageManagerService.java)。

待機復帰後のQuiet TV表示は確認済みです。再起動・ファームウェア更新後のHomeキー動作は未検証です。
