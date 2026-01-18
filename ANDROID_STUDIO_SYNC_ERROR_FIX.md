# Android Studio Gradle Sync エラー修正ガイド

## ❌ エラー症状

Android StudioでGradle同期時に以下のエラーが発生：

```text
Gradle sync failed: Operation result has not been received.
```

**ログファイル場所**: `C:\Users\liuha\AppData\Local\Google\AndroidStudio2025.2.2\log\idea.log`

---

## 🔍 エラー原因

ログ分析結果：

1. **WSL内のGradleは正常動作** ✅
   - コマンドライン: `./gradlew tasks` → 成功

2. **Android StudioとWSL間の通信タイムアウト** ❌
   - Gradle Daemon との接続が7-9秒でタイムアウト
   - `Operation result has not been received.`

3. **JDKパスは正しく認識** ✅
   - `\\wsl.localhost\Ubuntu-22.04\usr\lib\jvm\java-17-openjdk-amd64`

---

## ✅ 解決方法（実施済み）

### 1. Gradleデーモンをリセット

```bash
cd /home/liuhan/suiten/suiten_android

# Gradleデーモンを停止
./gradlew --stop

# Gradleキャッシュをクリア
rm -rf ~/.gradle/caches/ ~/.gradle/daemon/

# プロジェクトのキャッシュをクリア
rm -rf .gradle/ app/.gradle/ app/build/ build/
```

### 2. gradle.properties を最適化（完了）

以下の設定を追加済み：

```properties
# メモリを増加（2GB → 4GB）
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8

# WSL JDK path
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64

# パフォーマンス最適化
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=false

# タイムアウト設定（1時間）
org.gradle.daemon.idletimeout=3600000
```

---

## 🚀 Android Studioでの次のステップ

### ステップ1: キャッシュを無効化して再起動

1. Android Studioで **File** → **Invalidate Caches...**
2. ✅ **Invalidate and Restart** を選択
3. Android Studioが再起動するのを待つ

### ステップ2: Gradle設定を確認

1. **File** → **Settings** (または **Ctrl+Alt+S**)
2. **Build, Execution, Deployment** → **Build Tools** → **Gradle**
3. 以下を確認：
   - **Gradle JVM**: `openjdk-17 (WSL)` または手動で設定
   - **Build and run using**: Gradle
   - **Run tests using**: Gradle

### ステップ3: Gradle同期を手動実行

1. **File** → **Sync Project with Gradle Files**
2. または、ツールバーの 🐘 アイコンをクリック
3. 同期完了まで待機（初回は3-5分かかる場合あります）

---

## 🛠️ 追加のトラブルシューティング

### オプションA: Gradle Wrapperを再生成

WSLターミナルで：

```bash
cd /home/liuhan/suiten/suiten_android

# 既存のwrapperを削除
rm -rf gradle/ gradlew gradlew.bat

# Flutter appから再コピー
cp -r /home/liuhan/suiten/suiten_app/android/gradle ./
cp /home/liuhan/suiten/suiten_app/android/gradlew ./
chmod +x gradlew

# 動作確認
./gradlew --version
```

### オプションB: local.properties を確認

`suiten_android/local.properties` に以下が設定されているか確認：

```properties
# Android SDK path (Windows側)
sdk.dir=C\:\\Users\\liuha\\AppData\\Local\\Android\\Sdk

# JDK path (WSL側)
java.home=/usr/lib/jvm/java-17-openjdk-amd64
```

**注意**: `sdk.dir` はバックスラッシュをエスケープ (`\\`) する必要があります。

### オプションC: WSL2の最適化

#### Windows側でWSL2メモリを増やす

`C:\Users\liuha\.wslconfig` を作成/編集：

```ini
[wsl2]
memory=8GB
processors=4
swap=2GB
```

保存後、PowerShellで：

```powershell
wsl --shutdown
wsl
```

---

## 🔍 ログの確認方法

### Android Studioログ

**場所**: `C:\Users\liuha\AppData\Local\Google\AndroidStudio2025.2.2\log\idea.log`

**確認コマンド (WSL)**:

```bash
tail -100 /mnt/c/Users/liuha/AppData/Local/Google/AndroidStudio2025.2.2/log/idea.log
```

### Gradle Build Output

Android Studioで：

1. **View** → **Tool Windows** → **Build**
2. エラーメッセージとスタックトレースを確認

---

## ✅ 確認チェックリスト

同期が成功したかを確認：

- [ ] Android Studioで「Gradle sync finished」と表示される
- [ ] プロジェクト構造が左側に表示される
- [ ] `app/src/main/kotlin` フォルダが認識される
- [ ] エラーが表示されない
- [ ] Build Output に成功メッセージ

---

## 🎯 ビルドテスト（WSL）

Android Studioでの同期が成功したら、WSLでビルドテスト：

```bash
cd /home/liuhan/suiten/suiten_android

# クリーンビルド
./gradlew clean

# デバッグAPKをビルド
./gradlew assembleDebug

# 成功したら APK が生成される
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 期待される結果

### 同期成功時

```text
BUILD SUCCESSFUL in 45s
```

### APKサイズ

```text
app-debug.apk: 約 5-10 MB
```

---

## ⚠️ 既知の制限事項

### WSL環境での制約

1. **ビルド速度**
   - WSL環境はネイティブより20-30%遅い可能性
   - Gradle daemonを有効にして緩和

2. **ファイルウォッチャー**
   - WSLのファイル変更検出が遅延する場合がある
   - 手動で再同期が必要な場合あり

3. **メモリ使用量**
   - Android Studio + WSL で 8GB 以上推奨
   - `.wslconfig` でメモリ制限を調整

---

## 🔄 代替アプローチ

### プロジェクトをWindows側にコピー

WSL環境で問題が続く場合：

```powershell
# PowerShellで
Copy-Item -Recurse "\\wsl.localhost\Ubuntu-22.04\home\liuhan\suiten\suiten_android" "C:\Projects\suiten_android"
```

その後、Windows側のプロジェクトをAndroid Studioで開く。

**注意**: この場合、WSL固有の設定（`org.gradle.java.home`）を削除する必要があります。

---

## 📚 参考情報

### 実行環境

- **OS**: Windows 11 + WSL2 (Ubuntu 22.04)
- **Android Studio**: 2025.2.2
- **Gradle**: 8.3
- **JDK**: OpenJDK 17.0.17
- **Android Gradle Plugin**: 8.2.0

### ドキュメント

- [Gradle Performance Guide](https://docs.gradle.org/current/userguide/performance.html)
- [WSL Best Practices](https://docs.microsoft.com/en-us/windows/wsl/setup/environment)
- [Android Studio Sync Troubleshooting](https://developer.android.com/studio/troubleshoot)

---

## 🆘 サポート

それでもエラーが解決しない場合：

1. **完全なエラーログを収集**:
   ```bash
   tail -500 /mnt/c/Users/liuha/AppData/Local/Google/AndroidStudio2025.2.2/log/idea.log > error_log.txt
   ```

2. **Gradle詳細ログを取得**:
   ```bash
   ./gradlew assembleDebug --stacktrace --debug > gradle_debug.log 2>&1
   ```

3. **システム情報を確認**:
   ```bash
   # WSL
   uname -a
   java -version
   ./gradlew --version

   # Windows (PowerShell)
   wsl --list --verbose
   systeminfo | findstr /B /C:"Total Physical Memory"
   ```

---

**ステータス**: 設定完了
**最終更新**: 2026-01-04
**次のアクション**: Android Studioで再同期を試す
