# Quiet TV

Xiaomi TV S Mini LED 55 2025 向けの、HDMI入力とアプリ一覧だけを置くAndroid TV / Google TVランチャー。

## 表示と操作

- Google TVのHDMI入力設定で付けた名前を、そのままカード名に使います。
- 名前に `Switch` が含まれるHDMI入力を表示します。
- 名前に `PlayStation` が含まれるHDMI入力を表示します。入力カードは設定名のテキストだけを中央に表示し、メーカーロゴは使いません。
- Switchの文字色は `#E60012`、PlayStationの文字色は `#0070CC`（RGB 0, 112, 204）です。背景は通常時 `#F7F7F4`、フォーカス時 `#FFFFFF`。選択中は濃い枠線 `#566B7A` と太字・影で示します。
- 大文字・小文字は区別しません。`Nintendo Switch 2`、`PlayStation 5` も対象です。`PS5` だけでは一致しません。両方を含む場合はSwitchを優先します。
- 対象外のHDMI入力はホームに表示しません。対象が2入力なら「入力2枚＋すべてのアプリ」の3枚です。
- 切断中もカードを残し、CEC子入力と物理HDMIの重複を防ぎます。Google TVで非表示の入力は表示しません。
- 「すべてのアプリ」は起動可能なTVアプリと通常アプリをパッケージ単位でまとめて表示します。起動画面のないシステムサービスは対象外です。上部に「Google TVの設定」ボタンがあり、十字キーの上で移動できます。設定アプリも一覧に含めます。
- 十字キーで移動、決定で開き、戻るで一覧からホームへ戻ります。ホーム上の戻るでは終了しません。
- 入力名は画面に戻るたびに読み直します。HDMI入力の追加・削除・更新も監視します。

インターネット権限、外部API、広告SDK、WebView、アクセシビリティサービス、常駐ポーリングはありません。OSや既存Google TVアプリ自体は変更しません。添付ポストにあるRAM削減率や高速化倍率は本アプリでは未測定です。

## ビルド

Android SDK 36、Build Tools 35.0.0、JDK 17以降（Gradle 8.13が対応するバージョン）を用意し、`local.properties` に `sdk.dir=/path/to/Android/sdk` を記入します。対象SDKも36です。

```sh
./gradlew assembleDebug lintDebug assembleDebugAndroidTest
sh scripts/test-rules.sh
```

APK: `app/build/outputs/apk/debug/app-debug.apk`。個人用のデバッグ署名APKです。同じ署名鍵を使って更新してください。Gradleは初回のみビルド用依存を取得します。アプリ本体に追加ライブラリはありません。

## テレビにインストール

1. テレビとMacを同じネットワークに接続します。
2. テレビの「設定 → システム → デバイス情報」からOSビルドを繰り返し選んで開発者向けオプションを有効にします。メニュー名はファームウェアにより異なります。
3. 開発者向けオプションでデバッグを有効にします。ワイヤレスデバッグがある場合は「ペア設定コードでデバイスをペア設定」を開きます。
4. 次を実行します。ペア設定ポートと接続ポートは異なります。テレビ側でこのPCからの接続を許可してください。

```sh
adb pair TV_IP:PAIRING_PORT
# 画面のコードを対話入力
adb connect TV_IP:CONNECT_PORT
adb devices -l
adb -s TV_IP:CONNECT_PORT install -r app/build/outputs/apk/debug/app-debug.apk
adb -s TV_IP:CONNECT_PORT shell am start -n com.beryu.tvhome/.MainActivity
```

古いADBでペア設定に失敗する場合はAndroid SDK Platform-Toolsを更新するか、テレビが提供する従来のネットワークデバッグ接続と、テレビ画面上の認証を使います。`unauthorized` の間はインストールできません。

5. Google TVの入力設定で、使用するHDMI入力を `Nintendo Switch` / `PlayStation` などに変更します。アプリにはGoogle TV側の実際の名前が表示されます。
6. 両方のカードから正しいHDMI映像へ切り替わることと、アプリ一覧の起動を確認します。

## ホームに設定・元に戻す

まず元のホームを記録しておきます。

```sh
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

テレビの既定ホーム選択画面でQuiet TVを選ぶか、対応機種では次のADBコマンドを使えます。

```sh
adb shell cmd package set-home-activity com.beryu.tvhome/.MainActivity
adb shell input keyevent KEYCODE_HOME
```

元に戻す場合は、記録した元のホームのコンポーネントを指定します。例のGoogle TVパッケージ名を決め打ちせず、実機の値を使ってください。

```sh
adb shell cmd package set-home-activity ORIGINAL_PACKAGE/ORIGINAL_ACTIVITY
# またはQuiet TVをアンインストールして元のホームを選択
adb uninstall com.beryu.tvhome
```

アプリとして起動する場合、純正ホームの変更は不要です。ただし今回のXiaomi実機では純正ホームが高い優先度を持つため、上記の既定ホーム指定だけではHomeキーを置き換えられません。ユーザーからホーム置き換えの承認を得ています。ADBはシステムアプリのコンポーネント単位の停止を許可しないため、純正ホームとGoogle TV初期設定アプリの2パッケージをユーザー0で無効化する必要があります。Wi-Fi再接続後、保存元が `android.uid.system` へ変わったことを確認して2パッケージを無効化し、Quiet TVへの置き換えを完了しました。Homeキーでの起動、Google TV設定への導線、両HDMI入力、Wi-Fi維持を実機で確認済みです。詳細とリスクは `docs/DEVICE-VALIDATION.md` を参照してください。

この追加変更を行った後に元に戻すには、**Quiet TVを削除する前に**以下を実行します。

```sh
ADB=/path/to/adb sh scripts/restore-google-tv.sh TV_IP:PORT
```

追加変更後はQuiet TVのアンインストールだけでは元のホームへ戻りません。先に2つのパッケージの有効状態を復元してください。

## 互換性と実機確認

Android 8.0 / API 26以降。`TvInputManager` / `TvInputInfo.loadCustomLabel()` で入力を読み、`TvContract.buildChannelUriForPassthroughInput()` の `ACTION_VIEW` をテレビのTVアプリへ渡します。

メーカーが入力情報や起動先を標準APIに公開していない場合は、実機での調査・調整が必要です。権限エラーはクラッシュさせず画面に案内します。標準APIの存在だけでは、Xiaomiの全ファームウェアでの切り替え成功は保証できません。

参考: [入力名API](https://developer.android.com/reference/android/media/tv/TvInputInfo#loadCustomLabel(android.content.Context)) / [HDMI用URI](https://developer.android.com/reference/android/media/tv/TvContract#buildChannelUriForPassthroughInput(java.lang.String)) / [TVアプリ設定](https://developer.android.com/training/tv/get-started/create)

Nintendo Switch、PlayStationの名称・商標は各権利者に帰属します。

## 画面操作のスモークテスト

テスト用APKを併せてインストールした端末で明示的に実行します。テストの途中だけ仮のHDMIカードを描画し、通常起動には影響しません。入力切り替えの成功はこのテストの対象外で、実機のHDMI機器で確認します。

```sh
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.beryu.tvhome.test/com.beryu.tvhome.SmokeInstrumentation
```

初期フォーカス、十字キー移動、アプリ一覧、一覧内移動、設定ボタンへの移動と設定Intent、戻るを確認し、ホームとアプリ一覧のスクリーンショットをアプリの内部ファイル領域に保存します。デバッグAPKでは `adb exec-out run-as com.beryu.tvhome cat files/apps-preview.png > apps.png` で取り出せます。

先行研究: [simbasjungles/free-my-tv](https://github.com/simbasjungles/free-my-tv)。`HomeApps.java` の物理HDMIとCEC子入力の整理、設定名の優先、HDMI URI、`ANDROID_SETTINGS` の `Settings.ACTION_SETTINGS` 導線を確認して参考にしています。本プロジェクトはコードや素材をコピーせず独立して実装しています。
