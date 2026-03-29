package moe.jmodloader.installer;

import java.util.*;

public class I18n {
    private static String currentLang = "en_US";
    private static final Map<String, Map<String, String>> translations = new HashMap<>();

    static {
        Map<String, String> en = new HashMap<>();
        en.put("title", "JModLoader Installer");
        en.put("welcome", "Welcome to JModLoader Installer");
        en.put("select_lang", "Select Language:");
        en.put("install_path", "Installation Directory:");
        en.put("browse", "Browse...");
        en.put("register_path", "Register JModLoader to system PATH?");
        en.put("overwrite", "An older version is installed. Overwrite?");
        en.put("yes", "Yes");
        en.put("no", "No");
        en.put("cancel", "Cancel");
        en.put("installing", "Installing...");
        en.put("success", "Installation completed successfully!");
        en.put("failed", "Installation failed:");
        en.put("finish", "Finish");
        en.put("back", "Back");
        en.put("next", "Next");
        en.put("maven_required", "Maven is required to build projects. Install now?");
        en.put("maven_found", "Maven found:");
        en.put("maven_not_found", "Maven not found. Please install Maven first.");
        en.put("java_required", "Java 17+ is required. Install now?");
        en.put("java_found", "Java found:");
        en.put("java_not_found", "Java not found. Please install Java 17+ first.");
        en.put("extracting", "Extracting files...");
        en.put("creating_shortcuts", "Creating shortcuts...");
        en.put("registering_path", "Registering to PATH...");
        en.put("version", "Version:");
        en.put("already_installed", "JModLoader is already installed:");
        en.put("uninstall_first", "Please uninstall the previous version first.");
        en.put("dir_not_empty", "Directory is not empty. Continue?");
        en.put("agree_license", "I agree to the MIT License terms.");
        en.put("license", "MIT License\n\nCopyright 2024 JModLoader Team\n\nPermission is hereby granted, free of charge...");
        en.put("java_check", "Checking Java...");
        en.put("maven_check", "Checking Maven...");
        en.put("checking_version", "Checking for updates...");
        en.put("new_version", "A newer version is available:");
        en.put("current_version", "Current version:");
        en.put("latest_version", "You are on the latest version.");
        translations.put("en_US", en);

        Map<String, String> zh = new HashMap<>();
        zh.put("title", "JModLoader 安装程序");
        zh.put("welcome", "欢迎使用 JModLoader 安装程序");
        zh.put("select_lang", "选择语言：");
        zh.put("install_path", "安装目录：");
        zh.put("browse", "浏览...");
        zh.put("register_path", "是否将 JModLoader 注册到系统 PATH？");
        zh.put("overwrite", "检测到旧版本，是否覆盖安装？");
        zh.put("yes", "是");
        zh.put("no", "否");
        zh.put("cancel", "取消");
        zh.put("installing", "正在安装...");
        zh.put("success", "安装成功！");
        zh.put("failed", "安装失败：");
        zh.put("finish", "完成");
        zh.put("back", "上一步");
        zh.put("next", "下一步");
        zh.put("maven_required", "需要 Maven 来构建项目。是否立即安装？");
        zh.put("maven_found", "已找到 Maven：");
        zh.put("maven_not_found", "未找到 Maven，请先安装 Maven。");
        zh.put("java_required", "需要 Java 17+。是否立即安装？");
        zh.put("java_found", "已找到 Java：");
        zh.put("java_not_found", "未找到 Java，请先安装 Java 17+。");
        zh.put("extracting", "正在解压文件...");
        zh.put("creating_shortcuts", "正在创建快捷方式...");
        zh.put("registering_path", "正在注册 PATH...");
        zh.put("version", "版本：");
        zh.put("already_installed", "JModLoader 已安装：");
        zh.put("uninstall_first", "请先卸载旧版本。");
        zh.put("dir_not_empty", "目录非空，是否继续？");
        zh.put("agree_license", "我同意 MIT License 协议。");
        zh.put("license", "MIT License\n\nCopyright 2024 JModLoader Team\n\n本软件依据 MIT 协议授权...");
        zh.put("java_check", "正在检查 Java...");
        zh.put("maven_check", "正在检查 Maven...");
        zh.put("checking_version", "正在检查更新...");
        zh.put("new_version", "发现新版本：");
        zh.put("current_version", "当前版本：");
        zh.put("latest_version", "当前已是最新版本。");
        translations.put("zh_CN", zh);
    }

    public static void setLang(String lang) {
        if (translations.containsKey(lang)) currentLang = lang;
    }

    public static String get(String key) {
        return translations.getOrDefault(currentLang, translations.get("en_US")).getOrDefault(key, key);
    }

    public static String[] getSupportedLangs() {
        return translations.keySet().toArray(new String[0]);
    }

    public static String getCurrentLang() { return currentLang; }
}
