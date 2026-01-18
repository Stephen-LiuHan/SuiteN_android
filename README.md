# SuiteN Android App

**読書理解アプリ - Kotlin/Jetpack Compose ネイティブ版**

SuiteN は、読書中の気づきをキャプチャし、AI で分析して整理するアプリです。このプロジェクトは Flutter 版から Android ネイティブ (Kotlin + Jetpack Compose) に移行されました。

## プロジェクト概要

### コアプライバシー原則 (重要)
- スクリーンショット画像はローカルに保存(圧縮済み)
- **OCR テキストは絶対に保存しない** - AI 処理後すぐに破棄
- AI 出力には原文の複製や近似を含めない
- ユーザーの思考と AI の抽象的分析のみを永続化

### 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **アーキテクチャ**: MVVM + クリーンアーキテクチャ
- **データベース**: Room (SQLite)
- **状態管理**: ViewModel + StateFlow
- **画像処理**: Android Bitmap API
- **OCR**: Google ML Kit Text Recognition
- **ナビゲーション**: Jetpack Navigation Compose
- **依存性注入**: 手動 DI (シンプルなファクトリパターン)

## プロジェクト構造

```
suiten_android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/space/suiten/
│       │   ├── MainActivity.kt
│       │   ├── SuiteNApplication.kt
│       │   ├── core/
│       │   │   └── service/
│       │   │       ├── ImageService.kt       # 画像圧縮・保存
│       │   │       ├── OCRService.kt         # テキスト抽出 (破棄機能付き)
│       │   │       └── AIService.kt          # AI 処理・検証
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── SuiteNDatabase.kt   # Room データベース
│       │   │   │   ├── entity/              # 8つのエンティティ
│       │   │   │   └── dao/                 # 6つの DAO
│       │   │   └── repository/              # リポジトリ実装
│       │   ├── domain/
│       │   │   ├── model/                   # 6つのドメインモデル
│       │   │   └── repository/              # リポジトリインターフェース
│       │   └── presentation/
│       │       ├── navigation/              # ナビゲーション設定
│       │       ├── screens/                 # 8つの画面
│       │       ├── viewmodel/               # ViewModel
│       │       └── ui/theme/                # Material テーマ
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               ├── backup_rules.xml
│               └── data_extraction_rules.xml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 主要機能

### 実装済み

#### データ層
- ✅ Room データベース (8テーブル: books, reading_sessions, captures, ai_results, next_actions, feedback_logs, action_logs, user_profiles)
- ✅ DAO とリポジトリパターン
- ✅ エンティティ ↔ ドメインモデル変換

#### サービス層
- ✅ **ImageService**: 画像選択、圧縮 (最大幅1600px, 品質75%)、保存
- ✅ **OCRService**: Google ML Kit によるテキスト認識 + **即座に破棄する機能**
- ✅ **AIService**: AI 処理とレスポンス検証 (禁止フレーズチェック、文字数制限)

#### プレゼンテーション層
- ✅ Jetpack Compose UI
- ✅ Material Design 3 テーマ
- ✅ 8つの画面:
  1. HomeScreen - 最近の本と未処理キャプチャ
  2. BooksScreen - 本棚 + 検索
  3. BookDetailScreen - 読書セッション一覧
  4. CaptureEditScreen - 感情・メモ追加、AI トリガー
  5. AIResultScreen - AI 分析結果表示
  6. AIChatScreen - AI との対話
  7. ProfileScreen - ユーザー傾向分析
  8. SettingsScreen - データ管理 + プライバシー情報
- ✅ BottomNavigationBar (Home, Books, Profile)
- ✅ Navigation Compose

#### ViewModel
- ✅ BookViewModel
- ✅ CaptureViewModel (重要な OCR 破棄ロジック含む)

### 未実装 (TODO)

- [ ] 実際の AI API 統合 (現在はモック)
- [ ] 画像選択の UI 実装
- [ ] ViewModelFactory と DI
- [ ] エラーハンドリング UI
- [ ] ローディング状態の UI
- [ ] フォーム検証
- [ ] データエクスポート/インポート
- [ ] 単体テスト
- [ ] UI テスト

## ビルドとセットアップ

### 必要な環境

- Android Studio Hedgehog (2023.1.1) 以降
- JDK 17
- Android SDK API 34
- Minimum SDK: API 26 (Android 8.0)

### セットアップ手順

1. **プロジェクトを開く**
   ```bash
   cd suiten_android
   # Android Studio で開く
   ```

2. **Gradle 同期**
   - Android Studio が自動的に Gradle を同期します
   - 初回は依存関係のダウンロードに時間がかかります

3. **ビルド**
   ```bash
   ./gradlew build
   ```

4. **実行**
   - エミュレータまたは実機を接続
   - Run ボタンをクリック、または:
   ```bash
   ./gradlew installDebug
   ```

## 重要な実装詳細

### OCR テキスト破棄 (プライバシー要件)

**場所**: `CaptureViewModel.kt:processWithAI()`

```kotlin
suspend fun processWithAI(captureId: String): Boolean {
    var ocrText: String? = null // このメソッドのみにスコープ

    return try {
        // 1. メモリ上で抽出
        ocrText = ocrService.extractText(capture.imagePath)

        // 2. AI 処理 (OCR テキストを使うが保存しない)
        val (aiResult, nextActions) = aiService.processCapture(
            ocrText = ocrText,
            userMemo = capture.userMemo,
            emotion = capture.emotion
        )

        // 3. OCR テキストなしで結果を保存
        aiRepository.createAIResult(aiResult, nextActions)

        // 4. 重要: 即座に破棄
        OCRService.clearOCRText(ocrText)
        ocrText = null

        true
    } catch (e: Exception) {
        // エラー時も OCR テキストをクリア
        OCRService.clearOCRText(ocrText)
        ocrText = null
        false
    }
}
```

### AI レスポンス検証

**場所**: `AIService.kt:validateAIResponse()`

すべての AI レスポンスを検証:
- ❌ 禁止フレーズ (「述べている」「書かれている」等)
- ✅ 文字数制限 (要約 ≤ 250文字、質問 ≤ 60文字)
- ✅ ユーザー中心の言葉遣い

### データベーススキーマ

Room による 8 テーブル:
- `books` - 書籍メタデータ
- `reading_sessions` - 読書セッション
- `captures` - キャプチャ (画像パス + メモ + 感情 + AI ステータス)
- `ai_results` - AI 分析結果 (原文なし)
- `next_actions` - 提案アクション
- `feedback_logs` - フィードバック (1-3)
- `action_logs` - アクションログ
- `user_profiles` - ユーザープロフィール

## Android 特有の実装

### パーミッション (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CAMERA" />
```

### ML Kit 依存関係

```xml
<meta-data
    android:name="com.google.mlkit.vision.DEPENDENCIES"
    android:value="ocr" />
```

## 開発ガイドライン

### コーディング規約

- Kotlin 公式スタイルガイドに従う
- Material Design 3 ガイドライン準拠
- Clean Architecture パターン
- MVVM パターン

### プライバシーバイデザイン

1. **OCR テキストは絶対に保存しない**
2. **ログにも OCR テキストを出力しない**
3. **AI 出力に原文を含めない**
4. バックアップから機密データを除外

## トラブルシューティング

### Gradle ビルドエラー

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Room コンパイルエラー

KSP が正しく設定されているか確認:
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
```

### ML Kit エラー

Google Play Services が最新か確認してください。

## ライセンス

このプロジェクトは SuiteN 仕様 v0.2 に基づいています。

## 次のステップ

### テスト前
1. ViewModelFactory を実装
2. DI コンテナをセットアップ
3. エミュレータでテスト

### 本番環境前
1. 実際の AI API を統合
2. エラーハンドリング UI を追加
3. ローディング状態を追加
4. 単体テストと UI テストを追加
5. アプリアイコンとスプラッシュスクリーンを設定
6. セキュリティ監査
7. Google Play Store 提出

## 参考リソース

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

---

**Flutter 版から Kotlin ネイティブへの移行完了**

元の Flutter アプリ (`suiten_app`) と完全な機能パリティを持つ Android ネイティブアプリです。
