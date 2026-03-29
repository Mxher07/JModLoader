package moe.jmodloader.utils;

import java.util.HashMap;
import java.util.Map;

public class I18n {
    private static String currentLang = "en_US";
    private static final Map<String, Map<String, String>> translations = new HashMap<>();

    static {
        // en_US
        Map<String, String> en = new HashMap<>();
        en.put("msg.starting", "Starting...");
        en.put("msg.ml_version", "ML Version: ");
        en.put("msg.installed_mods", "Installed Mods:");
        en.put("msg.init_success", "Project initialized successfully!");
        en.put("msg.build_success", "Build successful! Created: ");
        en.put("msg.install_confirm", "Do you want to install this mod? (y/n): ");
        en.put("msg.reg_success", "JModLoader registered to system successfully!");
        en.put("msg.reg_success_win", "JModLoader registered to system successfully!");
        en.put("msg.reg_added_path", "Added to system PATH.");
        en.put("msg.reg_added_env", "Set JML_HOME to system environment.");
        en.put("msg.home_set", "JML_HOME set to: ");
        en.put("msg.home_env", "JML_HOME is currently: ");
        en.put("msg.update_available", "A new version is available!");
        en.put("msg.update_not_available", "You are running the latest version.");
        en.put("msg.update_downloading", "Downloading update...");
        en.put("msg.update_complete", "Update downloaded successfully!");
        en.put("msg.update_failed", "Update failed: ");
        en.put("msg.init_jar_success", "JAR project initialized successfully!");
        en.put("msg.init_jmod_success", "JMod project initialized successfully!");
        en.put("msg.no_java", "Java not found. Please install Java 17+.");
        en.put("msg.no_java_home", "JAVA_HOME is not set.");
        en.put("msg.java_version", "Java version: ");
        en.put("msg.run_not_found", "No build output found. Run 'jml build' first.");
        en.put("msg.run_launching", "Launching: ");
        en.put("msg.run_complete", "Application exited.");
        en.put("msg.home_cmd_info", "Usage: jml home [path]");
        en.put("msg.home_current", "Current JML_HOME: ");
        en.put("msg.home_changed", "JML_HOME changed to: ");
        en.put("msg.home_invalid", "Invalid path.");
        en.put("msg.update_info", "Checking for updates...");
        en.put("msg.update_check_failed", "Update check failed: ");
        en.put("err.invalid_init", "Invalid init type. Use 'jar' or 'jmod'.");
        en.put("err.mod_not_found", "Mod file not found: ");
        en.put("err.build_failed", "Build failed.");
        en.put("err.init_failed", "Initialization failed.");
        en.put("err.unsupported_platform", "Unsupported platform.");
        translations.put("en_US", en);

        // zh_CN
        Map<String, String> zh = new HashMap<>();
        zh.put("msg.starting", "正在启动...");
        zh.put("msg.ml_version", "加载器版本: ");
        zh.put("msg.installed_mods", "已安装的模组:");
        zh.put("msg.init_success", "项目初始化成功！");
        zh.put("msg.build_success", "构建成功！已创建: ");
        zh.put("msg.install_confirm", "是否安装此模组？(y/n): ");
        zh.put("msg.reg_success", "JModLoader 已成功注册到系统！");
        zh.put("msg.reg_success_win", "JModLoader 已成功注册到系统！");
        zh.put("msg.reg_added_path", "已添加到系统 PATH。");
        zh.put("msg.reg_added_env", "已将 JML_HOME 设置到系统环境变量。");
        zh.put("msg.home_set", "JML_HOME 已设置为: ");
        zh.put("msg.home_env", "当前 JML_HOME: ");
        zh.put("msg.update_available", "发现新版本！");
        zh.put("msg.update_not_available", "当前已是最新版本。");
        zh.put("msg.update_downloading", "正在下载更新...");
        zh.put("msg.update_complete", "更新下载完成！");
        zh.put("msg.update_failed", "更新失败: ");
        zh.put("msg.init_jar_success", "JAR 项目初始化成功！");
        zh.put("msg.init_jmod_success", "JMod 项目初始化成功！");
        zh.put("msg.no_java", "未找到 Java，请安装 Java 24+。");
        zh.put("msg.no_java_home", "JAVA_HOME 未设置。");
        zh.put("msg.java_version", "Java 版本: ");
        zh.put("msg.run_not_found", "未找到构建产物，请先运行 'jml build'。");
        zh.put("msg.run_launching", "正在启动: ");
        zh.put("msg.run_complete", "应用程序已退出。");
        zh.put("msg.home_cmd_info", "用法: jml home [路径]");
        zh.put("msg.home_current", "当前 JML_HOME: ");
        zh.put("msg.home_changed", "JML_HOME 已更改为: ");
        zh.put("msg.home_invalid", "无效的路径。");
        zh.put("msg.update_info", "正在检查更新...");
        zh.put("msg.update_check_failed", "检查更新失败: ");
        zh.put("err.invalid_init", "无效的初始化类型，请使用 'jar' 或 'jmod'。");
        zh.put("err.mod_not_found", "未找到模组文件: ");
        zh.put("err.build_failed", "构建失败。");
        zh.put("err.init_failed", "初始化失败。");
        zh.put("err.unsupported_platform", "不支持的平台。");
        translations.put("zh_CN", zh);
    }

    public static void setLang(String lang) {
        if (translations.containsKey(lang)) {
            currentLang = lang;
        }
    }

    public static String get(String key) {
        return translations.getOrDefault(currentLang, translations.get("en_US")).getOrDefault(key, key);
    }

    public static String getLang() {
        return currentLang;
    }

    public static boolean isSupported(String lang) {
        return translations.containsKey(lang);
    }
}
