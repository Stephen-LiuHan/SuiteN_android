#!/bin/bash

# Shiorio Android - WSL Setup Script
# このスクリプトはWSL環境でAndroid開発環境をセットアップします

set -e

echo "======================================"
echo "Shiorio Android - WSL環境セットアップ"
echo "======================================"
echo ""

# 色の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Javaがインストールされているか確認
echo -e "${YELLOW}[1/5] Javaの確認中...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓ Javaがインストールされています: $JAVA_VERSION${NC}"

    # Java 17以上か確認
    if java -version 2>&1 | grep -q "version \"1[7-9]"; then
        echo -e "${GREEN}✓ Java 17以上が検出されました${NC}"
    else
        echo -e "${YELLOW}⚠ Java 17以上が必要です${NC}"
        echo "以下のコマンドでインストールしてください:"
        echo "  sudo apt update && sudo apt install openjdk-17-jdk -y"
        exit 1
    fi
else
    echo -e "${RED}✗ Javaがインストールされていません${NC}"
    echo ""
    echo "以下のコマンドでJDK 17をインストールしてください:"
    echo ""
    echo "  sudo apt update"
    echo "  sudo apt install openjdk-17-jdk -y"
    echo ""
    echo "インストール後、このスクリプトを再実行してください。"
    exit 1
fi

# JAVA_HOMEの設定確認
echo ""
echo -e "${YELLOW}[2/5] JAVA_HOMEの確認中...${NC}"
if [ -z "$JAVA_HOME" ]; then
    echo -e "${YELLOW}⚠ JAVA_HOMEが設定されていません${NC}"

    # Java 17のパスを探す
    if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        JAVA_PATH="/usr/lib/jvm/java-17-openjdk-amd64"
    elif [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
        JAVA_PATH="/usr/lib/jvm/java-17-openjdk"
    else
        echo -e "${RED}✗ Java 17のパスが見つかりません${NC}"
        exit 1
    fi

    echo "JAVA_HOMEを $JAVA_PATH に設定します"
    export JAVA_HOME=$JAVA_PATH
    export PATH=$JAVA_HOME/bin:$PATH

    # .bashrcに追加
    if ! grep -q "JAVA_HOME" ~/.bashrc; then
        echo "" >> ~/.bashrc
        echo "# Java 17" >> ~/.bashrc
        echo "export JAVA_HOME=$JAVA_PATH" >> ~/.bashrc
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
        echo -e "${GREEN}✓ JAVA_HOMEを ~/.bashrc に追加しました${NC}"
    fi
else
    echo -e "${GREEN}✓ JAVA_HOME: $JAVA_HOME${NC}"
fi

# Gradleラッパーのパーミッション設定
echo ""
echo -e "${YELLOW}[3/5] Gradleラッパーのパーミッション設定中...${NC}"
if [ -f "./gradlew" ]; then
    chmod +x gradlew
    echo -e "${GREEN}✓ gradlewに実行権限を付与しました${NC}"
else
    echo -e "${RED}✗ gradlewが見つかりません${NC}"
    exit 1
fi

# local.propertiesの確認
echo ""
echo -e "${YELLOW}[4/5] Android SDK設定の確認中...${NC}"
if [ ! -f "local.properties" ]; then
    echo -e "${YELLOW}⚠ local.propertiesが存在しません${NC}"

    # Android SDKのパスを探す
    if [ -d "$HOME/Android/Sdk" ]; then
        SDK_PATH="$HOME/Android/Sdk"
    elif [ -d "/usr/lib/android-sdk" ]; then
        SDK_PATH="/usr/lib/android-sdk"
    else
        echo -e "${YELLOW}Android SDKが見つかりません${NC}"
        echo "local.propertiesをスキップします（ビルド時に必要になります）"
        SDK_PATH=""
    fi

    if [ -n "$SDK_PATH" ]; then
        echo "sdk.dir=$SDK_PATH" > local.properties
        echo -e "${GREEN}✓ local.propertiesを作成しました: $SDK_PATH${NC}"
    fi
else
    SDK_DIR=$(grep "sdk.dir" local.properties | cut -d'=' -f2)
    echo -e "${GREEN}✓ local.properties: $SDK_DIR${NC}"
fi

# Gradleキャッシュのクリア（オプション）
echo ""
echo -e "${YELLOW}[5/5] 環境確認完了${NC}"
echo ""
echo "======================================"
echo -e "${GREEN}セットアップが完了しました！${NC}"
echo "======================================"
echo ""
echo "次のステップ:"
echo ""
echo "1. ビルドを実行:"
echo "   ./gradlew build"
echo ""
echo "2. APKをビルド:"
echo "   ./gradlew assembleDebug"
echo ""
echo "3. アプリをインストール（実機接続時）:"
echo "   ./gradlew installDebug"
echo ""
echo "4. 利用可能なタスクを確認:"
echo "   ./gradlew tasks"
echo ""

# セッション用の環境変数エクスポート
if [ -n "$JAVA_PATH" ]; then
    echo -e "${YELLOW}注意: 現在のセッションでJAVA_HOMEを設定しました${NC}"
    echo "新しいターミナルを開く場合は、以下を実行してください:"
    echo "  source ~/.bashrc"
    echo ""
fi
