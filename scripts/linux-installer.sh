#!/bin/bash
#
# JModLoader Linux Installer (CLI, non-GUI)
# Usage: sudo ./install.sh [--path DIR] [--no-register]
#

set -e

APP_NAME="JModLoader"
INSTALL_DIR="/opt/jmodloader"
BIN_DIR="/usr/local/bin"
CONFIG_DIR="/etc/jmodloader"
DESKTOP_DIR="/usr/share/applications"
ARCH=$(uname -m)
INSTALL_VERSION="@VERSION@"

# ── i18n ──────────────────────────────────────────
en_US() {
    TITLE="JModLoader Installer"
    SELECT_LANG="Select language (en_US / zh_CN):"
    ENTER_PATH="Installation path [$INSTALL_DIR]:"
    REGISTER_PATH="Register to system PATH? (y/n) [y]:"
    ALREADY_INSTALLED="JModLoader is already installed at:"
    OVERWRITE="Overwrite existing installation? (y/n) [y]:"
    EXTRACTING="Extracting files..."
    CREATING_LINKS="Creating symbolic links..."
    REGISTERING="Registering to PATH..."
    SUCCESS="Installation completed successfully!"
    FAILED="Installation failed:"
    CONTINUE="Press Enter to continue..."
    CANCEL="Cancel"
    CHOICE="Enter choice:"
    JAVA_NOT_FOUND="Java not found. Please install OpenJDK 17+ first."
    INSTALL_OK="Installation OK"
    JAVA_VERSION_OK="Java 17+ found:"
    JAVA_VERSION_BAD="Warning: Java version is below 17."
}

zh_CN() {
    TITLE="JModLoader 安装程序"
    SELECT_LANG="选择语言 (en_US / zh_CN):"
    ENTER_PATH="安装路径 [$INSTALL_DIR]:"
    REGISTER_PATH="是否注册到系统 PATH？(y/n) [y]:"
    ALREADY_INSTALLED="JModLoader 已安装在："
    OVERWRITE="是否覆盖已有安装？(y/n) [y]:"
    EXTRACTING="正在解压文件..."
    CREATING_LINKS="正在创建符号链接..."
    REGISTERING="正在注册 PATH..."
    SUCCESS="安装成功！"
    FAILED="安装失败："
    CONTINUE="按回车键继续..."
    CANCEL="取消"
    CHOICE="请输入选项："
    JAVA_NOT_FOUND="未找到 Java，请先安装 OpenJDK 17+。"
    INSTALL_OK="安装成功"
    JAVA_VERSION_OK="Java 17+ 已找到："
    JAVA_VERSION_BAD="警告：Java 版本低于 17。"
}

# ── Detect Java ───────────────────────────────────
detect_java() {
    JAVA_CMD=""
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
    elif command -v java &>/dev/null; then
        JAVA_CMD="java"
    fi

    if [ -z "$JAVA_CMD" ]; then
        return 1
    fi

    # Get version string
    JAVA_VER=$("$JAVA_CMD" -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/')
    JAVA_MAJOR=$(echo "$JAVA_VER" | cut -d. -f1)
    if [ "$JAVA_MAJOR" = "1" ]; then
        JAVA_MAJOR=$(echo "$JAVA_VER" | cut -d. -f2)
    fi
    return 0
}

# ── Detect Maven ──────────────────────────────────
detect_maven() {
    if [ -n "$MAVEN_HOME" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
        MVN_CMD="$MAVEN_HOME/bin/mvn"
    elif command -v mvn &>/dev/null; then
        MVN_CMD="mvn"
    else
        MVN_CMD=""
    fi
}

# ── Install ───────────────────────────────────────
do_install() {
    INSTALL_DIR="$1"
    REGISTER="$2"

    echo "[$INSTALL_OK] $INSTALL_DIR"

    mkdir -p "$INSTALL_DIR"
    mkdir -p "$(dirname "$INSTALL_DIR")"

    echo "$EXTRACTING"
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    if [ -f "$SCRIPT_DIR/jmodloader.jar" ]; then
        cp "$SCRIPT_DIR/jmodloader.jar" "$INSTALL_DIR/jmodloader.jar"
    elif [ -f "$SCRIPT_DIR/lib/jmodloader.jar" ]; then
        cp "$SCRIPT_DIR/lib/jmodloader.jar" "$INSTALL_DIR/jmodloader.jar"
    fi

    mkdir -p "$INSTALL_DIR/Mods"
    mkdir -p "$INSTALL_DIR/libs"

    if [ "$REGISTER" = "yes" ]; then
        echo "$REGISTERING"
        if [ -L "$BIN_DIR/jml" ]; then
            rm "$BIN_DIR/jml"
        fi
        ln -sf "$INSTALL_DIR/jmodloader.jar" "$BIN_DIR/jml"
        echo 'JML_HOME="'"$INSTALL_DIR"'"' > /etc/profile.d/jmodloader.sh
        chmod +x /etc/profile.d/jmodloader.sh
    fi

    # Config
    mkdir -p "$CONFIG_DIR"
    echo "# JModLoader Config" > "$CONFIG_DIR/config.properties"
    echo "jml.home=$INSTALL_DIR" >> "$CONFIG_DIR/config.properties"

    echo "$SUCCESS"
}

# ── Ask Maven ─────────────────────────────────────
ask_maven() {
    echo "$SELECT_LANG"
    read -r lang

    if [ "$lang" = "zh_CN" ] || [ "$lang" = "zh" ]; then
        zh_CN
    else
        en_US
    fi

    echo "$TITLE"
    echo ""

    # Java check
    if detect_java; then
        if [ "$JAVA_MAJOR" -ge 17 ]; then
            echo "$JAVA_VERSION_OK $JAVA_VER"
        else
            echo "$JAVA_VERSION_BAD $JAVA_VER"
        fi
    else
        echo "$JAVA_NOT_FOUND"
    fi

    # Maven check
    detect_maven
    if [ -n "$MVN_CMD" ]; then
        echo "$JAVA_VERSION_OK Maven: $("$MVN_CMD" -version | head -1)"
    else
        echo "Maven: not found"
        echo "Maven is required for building projects. Install with:"
        echo "  Ubuntu/Debian: sudo apt install maven"
        echo "  Fedora/RHEL:   sudo dnf install maven"
        echo "  Arch:          sudo pacman -S maven"
    fi
    echo ""

    # Path
    echo -n "$ENTER_PATH "
    read -r USER_PATH
    if [ -n "$USER_PATH" ]; then
        INSTALL_DIR="$USER_PATH"
    fi

    # Register
    echo -n "$REGISTER_PATH "
    read -r reg
    reg=${reg:-y}
    REGISTER="no"
    if [ "$reg" = "y" ] || [ "$reg" = "Y" ] || [ "$reg" = "" ]; then
        REGISTER="yes"
    fi

    # Already installed?
    if [ -d "$INSTALL_DIR" ]; then
        echo ""
        echo "$ALREADY_INSTALLED $INSTALL_DIR"
        echo -n "$OVERWRITE "
        read -r ow
        ow=${ow:-y}
        if [ "$ow" != "y" ] && [ "$ow" != "Y" ]; then
            echo "$CANCEL"
            exit 0
        fi
    fi

    echo ""
    do_install "$INSTALL_DIR" "$REGISTER"
    echo ""
    echo "$CONTINUE"
    read -r
}

ask_maven
