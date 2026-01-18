# WSL環境でのセットアップガイド

## 問題

```
Gradle JVM option is incorrect: 'C:\Program Files\Android\Android Studio\jbr'.
The project is located on WSL(Ubuntu-22.04).
Use the JDK installed on the same WSL distribution.
```

Windows側のJDKではなく、WSL内のJDKを使用する必要があります。

## 解決方法

### ステップ1: WSL内にJDK 17をインストール

```bash
# パッケージリストを更新
sudo apt update

# OpenJDK 17をインストール
sudo apt install openjdk-17-jdk -y

# インストール確認
java -version
javac -version
```

期待される出力:
```
openjdk version "17.0.x" ...
```

### ステップ2: JAVA_HOMEを設定

`~/.bashrc`に追加:

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

確認:
```bash
echo $JAVA_HOME
# 出力: /usr/lib/jvm/java-17-openjdk-amd64
```

### ステップ3: Gradleラッパーのパーミッション設定

```bash
cd /home/liuhan/suiten/suiten_android
chmod +x gradlew
```

### ステップ4: ビルド実行

```bash
# Gradleビルド
./gradlew build

# または、依存関係のダウンロードのみ
./gradlew tasks
```

## 代替方法: Gradleラッパーを使わずにビルド

### 方法1: Android Studioをインストール (推奨)

WSL内にAndroid Studioをインストールする場合:

1. **WSLgを有効化** (Windows 11推奨)
   ```bash
   # Windowsターミナルで
   wsl --update
   ```

2. **Android Studio for Linuxをダウンロード**
   ```bash
   cd ~
   wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2023.1.1.28/android-studio-2023.1.1.28-linux.tar.gz
   tar -xzf android-studio-2023.1.1.28-linux.tar.gz
   ```

3. **Android Studioを起動**
   ```bash
   ~/android-studio/bin/studio.sh
   ```

### 方法2: Gradle Daemonを使う

`gradle.properties`に以下を追加 (既に存在):

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
```

### 方法3: コマンドラインビルド

```bash
cd /home/liuhan/suiten/suiten_android

# クリーンビルド
./gradlew clean build

# デバッグAPKのビルド
./gradlew assembleDebug

# リリースAPKのビルド
./gradlew assembleRelease
```

ビルド後のAPKの場所:
- デバッグ: `app/build/outputs/apk/debug/app-debug.apk`
- リリース: `app/build/outputs/apk/release/app-release-unsigned.apk`

## トラブルシューティング

### エラー: Permission denied

```bash
chmod +x gradlew
```

### エラー: JAVA_HOME is not set

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### エラー: SDK location not found

`local.properties`を作成:

```bash
cat > local.properties <<EOF
sdk.dir=/home/$(whoami)/Android/Sdk
EOF
```

Android SDKがない場合は、コマンドラインツールをインストール:

```bash
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk

# コマンドラインツールをダウンロード
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/

# SDKをインストール
cmdline-tools/latest/bin/sdkmanager --update
cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

`local.properties`を更新:
```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

## Windows側のAndroid Studioから接続する場合

**推奨しません**。WSLプロジェクトはWSL内のツールでビルドすることを強く推奨します。

どうしても必要な場合:

1. プロジェクトをWindows側にコピー
   ```powershell
   # PowerShellで
   cp -r \\wsl.localhost\Ubuntu-22.04\home\liuhan\suiten\suiten_android C:\Projects\
   ```

2. Windows側のAndroid Studioで開く

## クイックスタート (推奨手順)

```bash
# 1. JDKインストール
sudo apt update && sudo apt install openjdk-17-jdk -y

# 2. JAVA_HOME設定
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 3. プロジェクトディレクトリへ移動
cd /home/liuhan/suiten/suiten_android

# 4. Gradleラッパーに実行権限を付与
chmod +x gradlew

# 5. ビルド実行
./gradlew build

# 6. APKのインストール (実機接続時)
# adbがWSL内にあれば
./gradlew installDebug
```

## 参考

- [OpenJDK 17 Documentation](https://openjdk.org/projects/jdk/17/)
- [Gradle on WSL](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Android Studio on Linux](https://developer.android.com/studio/install#linux)
