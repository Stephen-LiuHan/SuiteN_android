# SuiteN Android アプリ - ビルドガイド

## WSL環境でのセットアップ (重要!)

このプロジェクトはWSL (Windows Subsystem for Linux) 上にあります。
ビルドする前に、WSL内にJDKをインストールする必要があります。

### 自動セットアップ（推奨）

```bash
cd /home/liuhan/suiten/suiten_android
./setup-wsl.sh
```

このスクリプトが以下を確認・設定します:
- Java 17のインストール確認
- JAVA_HOME環境変数の設定
- Gradleラッパーのパーミッション設定
- Android SDK設定の確認

### 手動セットアップ

#### 1. JDK 17をインストール

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

#### 2. JAVA_HOMEを設定

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# .bashrcに追加して永続化
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

#### 3. 確認

```bash
java -version
# 出力: openjdk version "17.0.x" ...
```

詳細は `SETUP_WSL.md` を参照してください。

---

## クイックスタート

### 1. プロジェクトディレクトリへ移動

```bash
cd /home/liuhan/suiten/suiten_android
```

### 2. ビルド実行

**方法1: コマンドラインでビルド（推奨）**

```bash
# Gradleラッパーに実行権限を付与
chmod +x gradlew

# ビルド実行
./gradlew build

# または、デバッグAPKのみビルド
./gradlew assembleDebug
```

**方法2: Android Studioを使う場合**

WSL内のGUIツールを使うには、WSLg（Windows 11）が必要です。
または、プロジェクトをWindows側にコピーしてください。

### 3. エミュレータまたは実機の準備

#### エミュレータを使う場合:
1. Android Studio の Device Manager を開く
2. 新しい仮想デバイスを作成 (API 26 以上推奨)
3. デバイスを起動

#### 実機を使う場合:
1. USB デバッグを有効化
2. デバイスを PC に接続
3. Android Studio でデバイスが認識されることを確認

### 4. APKファイルの場所

ビルド完了後、APKファイルは以下に生成されます:

```bash
# デバッグAPK
app/build/outputs/apk/debug/app-debug.apk

# リリースAPK（署名なし）
app/build/outputs/apk/release/app-release-unsigned.apk
```

### 5. 実機へのインストール

**WSL内にadbがある場合:**

```bash
# adbのインストール（必要な場合）
sudo apt install android-tools-adb

# デバイスを確認
adb devices

# APKをインストール
./gradlew installDebug
# または
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Windows側のadbを使う場合:**

```powershell
# PowerShellで
adb install \\wsl.localhost\Ubuntu-22.04\home\liuhan\suiten\suiten_android\app\build\outputs\apk\debug\app-debug.apk
```

## プロジェクト構成

```
suiten_android/
├── app/
│   ├── src/main/
│   │   ├── kotlin/space/suiten/
│   │   │   ├── MainActivity.kt              # メインアクティビティ
│   │   │   ├── SuiteNApplication.kt        # Application クラス
│   │   │   ├── core/service/                # サービス層
│   │   │   │   ├── ImageService.kt          # 画像処理
│   │   │   │   ├── OCRService.kt            # テキスト認識
│   │   │   │   └── AIService.kt             # AI 処理
│   │   │   ├── data/
│   │   │   │   ├── local/                   # データベース
│   │   │   │   │   ├── SuiteNDatabase.kt
│   │   │   │   │   ├── entity/              # 8 エンティティ
│   │   │   │   │   └── dao/                 # 6 DAO
│   │   │   │   └── repository/              # リポジトリ実装
│   │   │   ├── domain/
│   │   │   │   ├── model/                   # ドメインモデル
│   │   │   │   └── repository/              # リポジトリIF
│   │   │   └── presentation/
│   │   │       ├── navigation/              # ナビゲーション
│   │   │       ├── screens/                 # 8 画面
│   │   │       ├── viewmodel/               # ViewModel
│   │   │       └── ui/theme/                # テーマ
│   │   └── res/
│   │       ├── values/strings.xml
│   │       └── xml/
│   └── build.gradle.kts
└── README_ja.md
```

## 主要な依存関係

- **Jetpack Compose**: Modern UI toolkit
- **Material Design 3**: UI コンポーネント
- **Room Database**: SQLite ORM
- **ML Kit**: テキスト認識 (OCR)
- **Navigation Compose**: 画面遷移
- **Kotlin Coroutines**: 非同期処理
- **Coil**: 画像読み込み

## 重要な実装

### プライバシー保護

OCR で抽出したテキストは**絶対に保存されません**:

```kotlin
// CaptureViewModel.kt
suspend fun processWithAI(captureId: String): Boolean {
    var ocrText: String? = null  // ローカル変数のみ

    try {
        ocrText = ocrService.extractText(imagePath)  // メモリ上で抽出
        val result = aiService.processCapture(ocrText, ...)  // AI 処理
        aiRepository.createAIResult(result)  // OCR テキストなしで保存

        // 即座に破棄
        OCRService.clearOCRText(ocrText)
        ocrText = null
    } catch (e: Exception) {
        // エラー時も必ず破棄
        OCRService.clearOCRText(ocrText)
        ocrText = null
    }
}
```

### データベース

8 つのテーブルを Room で管理:
- `books` - 書籍
- `reading_sessions` - 読書セッション
- `captures` - キャプチャ
- `ai_results` - AI 結果
- `next_actions` - 次のアクション
- `feedback_logs` - フィードバック
- `action_logs` - アクションログ
- `user_profiles` - ユーザープロフィール

## トラブルシューティング

### ビルドエラー

```bash
# キャッシュをクリア
./gradlew clean

# 依存関係を再取得
./gradlew build --refresh-dependencies
```

### ML Kit のエラー

Google Play Services が最新版かを確認してください。

### Room のコンパイルエラー

KSP プラグインが正しく設定されているか確認:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
```

## 次のステップ

### 開発を続ける場合:

1. **ViewModelFactory の実装**
   - 依存性注入をセットアップ
   - ViewModel のインスタンス化を改善

2. **画像選択 UI の実装**
   - ActivityResultContracts を使用
   - ギャラリーとカメラの選択

3. **AI API の統合**
   - 現在はモック実装
   - 実際の API に接続

4. **テストの追加**
   - 単体テスト (JUnit)
   - UI テスト (Espresso/Compose Test)

### リリース準備:

1. **アプリアイコンの設定**
   - `res/mipmap-*/ic_launcher.png`

2. **署名設定**
   - キーストアを作成
   - `build.gradle.kts` に署名設定を追加

3. **ProGuard/R8 最適化**
   - `proguard-rules.pro` を調整

4. **Play Store 提出**
   - アプリバンドル (.aab) をビルド
   - Google Play Console にアップロード

## 参考

- [Jetpack Compose ドキュメント](https://developer.android.com/jetpack/compose)
- [Room 永続化ライブラリ](https://developer.android.com/training/data-storage/room)
- [ML Kit テキスト認識](https://developers.google.com/ml-kit/vision/text-recognition/android)

---

**Flutter から Kotlin ネイティブへの完全移行**
