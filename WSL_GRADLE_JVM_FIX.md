# WSL環境でのGradle JVMエラー修正ガイド

## ❌ エラー内容

```text
Gradle JVM option is incorrect: 'C:\Program Files\Android\Android Studio\jbr'.
The project is located on WSL(Ubuntu-22.04 - '\\wsl.localhost\Ubuntu-22.04\').
Use the JDK installed on the same WSL distribution.
```

このエラーは、Windows側のAndroid StudioがWSLプロジェクトに対してWindows用のJDKを使おうとしているために発生します。

---

## ✅ 解決方法

### 方法1: gradle.propertiesで設定（推奨・自動適用）

すでに `gradle.properties` に以下の設定を追加済みです：

```properties
# WSL JDK path
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
```

**次のステップ：**

1. Android Studioで **File** → **Invalidate Caches...** → **Invalidate and Restart**
2. プロジェクトが再起動したら **File** → **Sync Project with Gradle Files**

---

### 方法2: Android StudioのGradle設定で手動設定

Android Studioのエラーダイアログで **"Open Gradle Settings"** をクリック、または：

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**
2. **Gradle JVM** ドロップダウンをクリック
3. 以下のいずれかを選択：

   **オプションA: "Add JDK" で追加**
   - WSLのJDKパスを入力：
     ```text
     \\wsl.localhost\Ubuntu-22.04\usr\lib\jvm\java-17-openjdk-amd64
     ```

   **オプションB: 既存のWSL JDKを選択**
   - リストに "openjdk-17 (WSL)" があれば選択

4. **Apply** → **OK**

---

### 方法3: local.propertiesで設定

`suiten_android/local.properties` を編集：

```properties
# WSL SDK Path (既存)
sdk.dir=/mnt/c/Users/liuha/AppData/Local/Android/Sdk

# WSL JDK Path (追加)
java.home=/usr/lib/jvm/java-17-openjdk-amd64
```

---

## 🔍 JDKパスの確認方法

WSL内で以下のコマンドを実行：

```bash
# 実際のJavaバイナリのパスを確認
readlink -f /usr/bin/java
# 出力例: /usr/lib/jvm/java-17-openjdk-amd64/bin/java

# インストール済みJVMの一覧
ls -la /usr/lib/jvm/

# Javaバージョン確認
java -version
```

---

## 🛠️ トラブルシューティング

### 問題1: "Gradle sync failed" が続く

**解決策：**

```bash
# WSLターミナルで
cd /home/liuhan/suiten/suiten_android
./gradlew clean
rm -rf .gradle app/.gradle
./gradlew build --refresh-dependencies
```

その後、Android Studioで再度sync。

---

### 問題2: JDKが見つからない

**WSL内にJDK 17をインストール：**

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y

# インストール確認
java -version
# 出力: openjdk version "17.0.x"
```

**環境変数を設定（永続化）：**

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

---

### 問題3: WSLパスが認識されない

**Windows側でWSLパスを確認：**

1. Windowsエクスプローラーを開く
2. アドレスバーに入力：
   ```text
   \\wsl.localhost\Ubuntu-22.04\usr\lib\jvm\java-17-openjdk-amd64
   ```

3. フォルダが開けば正しいパス
4. 開けない場合は以下を試す：
   ```text
   \\wsl$\Ubuntu-22.04\usr\lib\jvm\java-17-openjdk-amd64
   ```

---

### 問題4: Gradleビルドが遅い

**gradle.propertiesを最適化：**

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

---

## 📋 確認チェックリスト

設定後、以下を確認：

- [ ] `gradle.properties` に `org.gradle.java.home` が設定されている
- [ ] WSLターミナルで `./gradlew --version` が成功する
- [ ] Android StudioのGradle設定でWSL JDKが選択されている
- [ ] Gradle syncがエラーなく完了する
- [ ] ビルドが成功する（`./gradlew assembleDebug`）

---

## 🚀 次のステップ

設定完了後：

1. **Gradle同期を実行**
   ```text
   File → Sync Project with Gradle Files
   ```

2. **ビルドを実行**
   ```text
   Build → Make Project (Ctrl+F9)
   ```

3. **アプリを実行**
   ```text
   Run → Run 'app' (Shift+F10)
   ```

---

## 📚 参考情報

### 現在の設定

- **WSL Distribution**: Ubuntu-22.04
- **JDK Path**: `/usr/lib/jvm/java-17-openjdk-amd64`
- **JDK Version**: OpenJDK 17.0.17
- **Gradle Version**: 8.3
- **Android Gradle Plugin**: 8.2.0

### 設定ファイルの場所

```text
suiten_android/
├── gradle.properties          ← WSL JDK設定済み
├── local.properties           ← Android SDK設定
├── gradlew                    ← Gradle Wrapper
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties
```

---

## 💡 推奨設定

### 最適なgradle.properties

```properties
# Project-wide Gradle settings
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
android.nonTransitiveRClass=false

# WSL JDK path
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64

# Performance optimizations
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Android build optimizations
android.enableBuildCache=true
android.enableR8.fullMode=true
```

---

## ✅ 解決済み

このガイドの手順に従うことで、WSL環境でのGradle JVMエラーが解決され、Android Studioで正常にビルド・実行できるようになります。

**作成日**: 2026-01-04
**環境**: WSL2 (Ubuntu 22.04) + Windows 11
**ステータス**: 設定完了
