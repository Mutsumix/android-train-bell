# andbell Android (Kotlin + Compose)

## 現在の実装内容
- Jetpack Compose + シンプルMVVM
- ON/OFFスイッチで音声再生
- 発車ベル/戸締り放送の選択
- 設定ダイアログでユーザーMP3追加/削除
- `DataStore` で選択状態とユーザー音源の保存

## 音源ポリシー
- URL音源は使わない
- `sample` フレーバー: アプリ内サンプル音（ToneGenerator）を表示
- `useronly` フレーバー: 同梱サンプルなし（ユーザー追加音源のみ）

## ビルドメモ
Gradle Wrapper は作成済みです。Android Studio でこの `android/` ディレクトリを開くか、  
CLI では JDK 17+ を指定して実行してください（この環境では JDK 25 で確認）。

```bash
JAVA_HOME="/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home" ./gradlew :app:assembleSampleDebug
JAVA_HOME="/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home" ./gradlew :app:assembleUseronlyRelease
```

- `sampleDebug` で起動
- `useronlyRelease` でユーザー音源のみ運用を検証
