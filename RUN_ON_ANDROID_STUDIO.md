# Android Studioで suiten_android を実行する方法

## 📋 前提条件

- Windows 11 + WSL2
- Android Studio インストール済み（Windows側）
- JDK 17インストール済み（WSL側）

---

## 🎯 方法1: Windows側のAndroid Studio使用（推奨）

### ステップ1: Android Studioを起動

1. Windowsで **Android Studio** を起動

### ステップ2: WSLプロジェクトを開く

1. **File** → **Open**
2. 以下のパスを入力またはナビゲート：
   ```
   \\wsl.localhost\Ubuntu-22.04\home\liuhan\suiten\suiten_android
   ```

   または（Windows 10の場合）：
   ```
   \\wsl$\Ubuntu-22.04\home\liuhan\suiten\suiten_android
   ```

3. **OK** をクリック

### ステップ3: Gradle同期を待つ

- Android Studioが自動的にプロジェクトを認識
- 右上に「Sync Now」が表示されたらクリック
- 初回は依存関係のダウンロードで3-5分かかります

### ステップ4: SDK設定を確認

1. **File** → **Settings** → **Appearance & Behavior** → **System Settings** → **Android SDK**
2. SDK Locationを確認：
   ```
   C:\Users\liuha\AppData\Local\Android\Sdk
   ```
3. 必要なSDKコンポーネントをインストール：
   - ✅ Android SDK Platform 33 (または最新)
   - ✅ Android SDK Build-Tools
   - ✅ Android Emulator
   - ✅ Android SDK Platform-Tools

### ステップ5: エミュレータまたは実機を準備

#### オプションA: エミュレータ
1. **Tools** → **Device Manager**
2. **Create Device**
3. ハードウェア選択（例: Pixel 5）
4. システムイメージ選択：
   - **API Level 33** (Android 13) 推奨
   - または API 26以上
5. **Download** → **Next** → **Finish**
6. 作成したデバイスの **▶ 起動**

#### オプションB: 実機
1. Android端末で **設定** → **デバイス情報** → **ビルド番号** を7回タップ
2. **開発者オプション** → **USBデバッグ** をON
3. USBケーブルでPCに接続
4. 端末に表示される許可ダイアログで「許可」を選択
5. Android StudioのDevice Managerに端末が表示されることを確認

### ステップ6: アプリを実行

1. 上部ツールバーでターゲットデバイスを選択
2. **Run 'app'** ボタン（緑の▶）をクリック
   - ショートカット: `Shift + F10`
3. ビルドが開始され、完了後に自動的にインストール・起動

---

## 🛠️ 方法2: WSLでビルド → Windowsでインストール

WSL側でAPKをビルドし、Windows側のADBでインストールする方法。

### WSL側の作業

```bash
cd /home/liuhan/suiten/suiten_android

# デバッグAPKをビルド
./gradlew assembleDebug

# APKの場所を確認
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### Windows側の作業（PowerShell）

```powershell
# ADBでデバイスを確認
adb devices

# WSL内のAPKをインストール
adb install "\\wsl.localhost\Ubuntu-22.04\home\liuhan\suiten\suiten_android\app\build\outputs\apk\debug\app-debug.apk"
```

---

## 🔧 トラブルシューティング

### 問題1: Gradle Sync Failed

**症状**: "Gradle sync failed: Could not resolve..."

**解決策**:
```bash
# WSLで実行
cd /home/liuhan/suiten/suiten_android
./gradlew clean
./gradlew build --refresh-dependencies
```

その後、Android Studioで再度 **File** → **Sync Project with Gradle Files**

### 問題2: SDK Not Found

**症状**: "SDK location not found"

**解決策**:
1. Android Studioで **File** → **Project Structure**
2. **SDK Location** タブ
3. Android SDK locationを設定：
   ```
   C:\Users\liuha\AppData\Local\Android\Sdk
   ```

### 問題3: デバイスが認識されない

**実機の場合**:
- USBデバッグが有効か確認
- USBケーブルを変更してみる
- デバイスマネージャーでADBドライバを確認

**エミュレータの場合**:
- Intel HAXM または Hyper-Vが有効か確認
- BIOSで仮想化（VT-x/AMD-V）が有効か確認

### 問題4: ビルドが遅い

**解決策**:
Android Studioの設定を最適化：

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler**
2. "Build process heap size" を `4096` MB に設定

`gradle.properties` に以下を追加：
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### 問題5: WSLパスが見つからない

**解決策**:
1. WSLが起動しているか確認
2. Windowsエクスプローラーで以下を試す：
   ```
   \\wsl.localhost\Ubuntu-22.04
   ```
   または
   ```
   \\wsl$\Ubuntu-22.04
   ```

---

## 📦 プロジェクト構成

```
suiten_android/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/space/suiten/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SuiteNApplication.kt
│   │   │   ├── core/service/
│   │   │   ├── data/
│   │   │   ├── domain/
│   │   │   └── presentation/
│   │   └── res/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradle/
```

---

## 🚀 次のステップ

### 開発環境の設定

1. **Kotlinプラグイン有効化**
   - Android Studioに標準搭載済み

2. **コードスタイル設定**
   - **File** → **Settings** → **Editor** → **Code Style** → **Kotlin**
   - "Set from..." → "Kotlin style guide"

3. **Logcat設定**
   - 下部の **Logcat** タブを開く
   - フィルタを設定：`package:space.suiten`

### デバッグ方法

1. ブレークポイントの設定
   - コード行番号の左をクリック
2. デバッグ実行
   - **Run** → **Debug 'app'** または `Shift + F9`

### プロファイリング

1. **View** → **Tool Windows** → **Profiler**
2. アプリを起動してパフォーマンスを監視

---

## 📚 参考リソース

- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [Gradle Build Guide](https://developer.android.com/build)
- [Debugging Guide](https://developer.android.com/studio/debug)
- [WSL Integration](https://docs.microsoft.com/en-us/windows/wsl/)

---

## ✅ チェックリスト

開発を始める前に確認：

- [ ] Android Studio起動済み
- [ ] プロジェクトがWSLパスから開けた
- [ ] Gradle同期が成功
- [ ] エミュレータまたは実機が認識されている
- [ ] アプリがビルド・実行できた
- [ ] Logcatにログが表示される

---

**作成日**: 2026-01-04
**WSL環境**: Ubuntu 22.04
**Android Studio**: 推奨バージョン 2023.1以降
