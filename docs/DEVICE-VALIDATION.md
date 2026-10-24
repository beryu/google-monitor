# Xiaomi実機の検証 — 2026-10-24

- 実機: Xiaomi TV S Mini LED 55 2025（ADBモデル名 `MiTV-MOEU0`）、Android 14。
- アプリ: `com.beryu.tvhome` / Quiet TV 1.0.3。compileSdk / targetSdk 36、minSdk 26。
- インストール成功。APK: `artifacts/quiet-tv-1.0.3.apk`（24,148 bytes）。
- SHA-256: `2d9815dc7179f4c5b9b5450a0b847414a5869102786788e6d70ff9f37ca1dcfb`。

## 検証済み

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

## Homeキーへの割り当て完了

この実機では、HOMEロールをQuiet TVに設定しても、優先度2の純正HomeActivityが優先されます。Quiet TVは優先度0、Google TVのRecoveryActivityは優先度1です。

ユーザーから純正ホームの置き換えを明示的に承認いただいています。コンポーネントだけの無効化はAndroidのADB権限制限で拒否されました（変更は適用されていません）。通常のADBで可能な方法は、次の2パッケージをユーザー0で無効化することです。削除はしません。

```text
com.google.android.apps.tv.launcherx
com.google.android.tungsten.setupwraith
```

保存Wi-Fiの作成元が当初 `com.google.android.tungsten.setupwraith` だったため、ユーザーにテレビ設定からWi-Fiを削除・再接続していただきました。その後、作成元が `android.uid.system` に変わったことを確認して実施しました。置き換え後もADB接続が維持され、Wi-Fiは使えています。

実施した変更内容:

```sh
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell cmd package set-home-activity --user 0 com.beryu.tvhome/.MainActivity
adb shell input keyevent KEYCODE_HOME
```

Homeキーで `com.beryu.tvhome/.MainActivity` が開くこと、アプリ一覧から `com.android.tv.settings/.MainSettings` を開いてHomeキーで戻れること、入力カードからHDMI 3（HW4）とHDMI 1（HW2）を選べることを変更後に確認しました。

復元は `scripts/restore-google-tv.sh`。元のパッケージの有効状態（default）と既定ホームを復元します。実際に純正ホームへ復元できることを一度確認し、その後Quiet TVを再び既定ホームにしています。Quiet TVをアンインストールする前に復元してください。設定アプリ、TV入力サービス、AndroidのFallbackHome、リカバリーパーティションは変更しません。

## 工場出荷状態への初期化

今回の変更はユーザーごとのパッケージ有効状態と既定ホームの設定で、システムファイルの削除・書き換えは行いません。この仕組みから、通常の工場出荷状態への初期化で純正ホームに戻ると考えられます。ただし、この実機で初期化しての復元は未検証です。

初期化はアプリ、アカウント、Wi-Fi設定なども消すため、通常はADBでの復元を優先します。Google TVの `RecoveryActivity` はアプリの画面であり、端末のリカバリーモードとは別です。Xiaomi TV S Mini LED 55 2025での物理ボタンによるリカバリー起動手順は未確認です。

参考: [Google TVの初期化](https://support.google.com/googletv/answer/10070482?hl=ja)、[Xiaomiの初期化案内（別機種のTV A 55 2025）](https://www.mi.com/global/support/faq/details/KA-481125/)、[AndroidのADBによるパッケージ有効状態の変更制限](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-dev/services/core/java/com/android/server/pm/PackageManagerService.java)。

再起動、電源復帰、ファームウェア更新後のHomeキー動作は未検証です。
