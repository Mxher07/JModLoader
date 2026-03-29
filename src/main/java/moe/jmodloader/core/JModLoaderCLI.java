package moe.jmodloader.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import moe.jmodloader.api.ModSpec;
import moe.jmodloader.utils.I18n;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "jml", mixinStandardHelpOptions = true, version = "JModLoader 1.1.2",
        description = "JModLoader - A powerful Java Mod Loader and Build Tool")
public class JModLoaderCLI implements Callable<Integer> {

    private static final String VERSION = "1.1.2";
    private static final String UPDATE_URL = "https://api.github.com/repos/Mxher07/JModLoader/releases/latest";

    private String jmlHome;

    public JModLoaderCLI() {
        this.jmlHome = resolveJmlHome();
        loadConfig();
    }

    // ── JML_HOME 解析 ──────────────────────────────
    private String resolveJmlHome() {
        String envHome = System.getenv("JML_HOME");
        if (envHome != null && !envHome.isEmpty()) return envHome;

        File config = getConfigFile();
        if (config.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(config)) {
                props.load(fis);
                String cfgHome = props.getProperty("jml.home");
                if (cfgHome != null && !cfgHome.isEmpty()) return cfgHome;
            } catch (IOException ignored) {}
        }
        return isWindows() ? "C:\\JModLoader" : System.getProperty("user.home") + "/.jmodloader";
    }

    private File getJmlHomeDir() {
        File dir = new File(jmlHome);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File getModsDir() {
        File dir = new File(jmlHome, "Mods");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File getConfigFile() {
        return new File(jmlHome, "config.properties");
    }

    // ── 跨平台工具 ─────────────────────────────────
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String findJava() {
        // 1. 尝试从 JML_HOME 找
        File jmlJava = new File(jmlHome, "java");
        if (jmlJava.exists() && jmlJava.isDirectory()) {
            File javaBin = new File(jmlJava, isWindows() ? "java.exe" : "java");
            if (javaBin.exists()) return javaBin.getAbsolutePath();
        }

        // 2. 扫描系统上的 JDK（优先找较新的版本）
        File javaHome = findBestJavaHome();
        if (javaHome != null) {
            File javaBin = new File(javaHome,
                isWindows() ? "bin\\java.exe" : "bin/java");
            if (javaBin.exists()) return javaBin.getAbsolutePath();
        }

        // 3. PATH 中的 java
        String javaCmd = isWindows() ? "java.exe" : "java";
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(isWindows() ? ";" : ":")) {
                File f = new File(dir.trim(), javaCmd);
                if (f.exists()) return f.getAbsolutePath();
            }
        }
        return javaCmd;
    }

    private File findBestJavaHome() {
        List<File> candidates = new ArrayList<>();
        File progFiles = new File("C:\\Program Files\\Java");
        if (progFiles.exists()) {
            File[] jdks = progFiles.listFiles((d, n) ->
                n.startsWith("jdk-") || n.startsWith("jdk"));
            if (jdks != null) {
                for (File jdk : jdks) candidates.add(jdk);
            }
        }
        // 按版本号排序，取最高的
        candidates.sort((a, b) -> compareVersion(b.getName(), a.getName()));
        for (File cand : candidates) {
            File javac = new File(cand, isWindows() ? "bin\\javac.exe" : "bin/javac");
            if (javac.exists()) {
                // 验证版本 >= 17
                int v = getJavaMajorVersion(cand);
                if (v >= 17) return cand;
            }
        }
        return null;
    }

    private int getJavaMajorVersion(File javaHome) {
        File release = new File(javaHome, "release");
        if (release.exists()) {
            try {
                List<String> lines = Files.readAllLines(release.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("JAVA_VERSION")) {
                        String ver = line.split("\"")[1]; // "24.0.2"
                        return Integer.parseInt(ver.split("\\.")[0]);
                    }
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private int compareVersion(String a, String b) {
        String na = a.replaceAll("[^0-9.]", "");
        String nb = b.replaceAll("[^0-9.]", "");
        String[] pa = na.split("\\."), pb = nb.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int ia = i < pa.length && !pa[i].isEmpty() ? Integer.parseInt(pa[i]) : 0;
            int ib = i < pb.length && !pb[i].isEmpty() ? Integer.parseInt(pb[i]) : 0;
            if (ia != ib) return Integer.compare(ia, ib);
        }
        return 0;
    }

    private String getJavaVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(findJava(), "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            return line != null ? line.trim() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ── 配置读写 ───────────────────────────────────
    private void loadConfig() {
        File cfg = getConfigFile();
        if (cfg.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cfg)) {
                props.load(fis);
                String lang = props.getProperty("lang", "en_US");
                if (I18n.isSupported(lang)) I18n.setLang(lang);
                String home = props.getProperty("jml.home");
                if (home != null && !home.isEmpty()) this.jmlHome = home;
            } catch (IOException ignored) {}
        }
    }

    private void saveConfig() {
        Properties props = new Properties();
        props.setProperty("lang", I18n.getLang());
        props.setProperty("jml.home", jmlHome);
        try (FileOutputStream fos = new FileOutputStream(getConfigFile())) {
            props.store(fos, "JModLoader Config");
        } catch (IOException ignored) {}
    }

    private void logToFile(String msg) {
        try {
            File logFile = new File(System.getProperty("user.dir"), "lastrun.log");
            String content = "=== JModLoader Run Log ===" + System.lineSeparator()
                + "Time: " + new Date() + System.lineSeparator()
                + msg + System.lineSeparator()
                + "===========================" + System.lineSeparator();
            Files.write(logFile.toPath(), content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    // ── lang ──────────────────────────────────────
    @Command(name = "lang", description = "Set language (en_US, zh_CN)")
    public void setLang(@Parameters(index = "0", arity = "0..1") String lang) {
        if (lang == null) {
            System.out.println("Current language: " + I18n.getLang());
            System.out.println("Available: en_US, zh_CN");
            return;
        }
        if (!I18n.isSupported(lang)) {
            System.err.println("Unsupported language: " + lang);
            return;
        }
        I18n.setLang(lang);
        saveConfig();
        System.out.println("Language set to: " + lang);
    }

    // ── home ───────────────────────────────────────
    @Command(name = "home", description = "Get or set JML_HOME")
    public void home(@Parameters(index = "0", arity = "0..1") String path) {
        if (path == null) {
            System.out.println(I18n.get("msg.home_current") + jmlHome);
            System.out.println(I18n.get("msg.home_cmd_info"));
            return;
        }
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println(I18n.get("msg.home_invalid"));
            return;
        }
        this.jmlHome = dir.getAbsolutePath();
        saveConfig();
        System.out.println(I18n.get("msg.home_changed") + jmlHome);
    }

    // ── init ───────────────────────────────────────
    // jml init jar|jmod [pkg] [mainClass]
    @Command(name = "init", description = "Initialize a new project (jar or jmod)")
    public void init(
            @Parameters(index = "0") String type,
            @Parameters(index = "1", arity = "0..1") String pkg,
            @Parameters(index = "2", arity = "0..1") String mainClassName) throws IOException {

        type = type.toLowerCase();
        if (!type.equals("jar") && !type.equals("jmod")) {
            System.err.println(I18n.get("err.invalid_init"));
            return;
        }

        File currentDir = new File(System.getProperty("user.dir"));
        String packageName = (pkg != null && !pkg.isEmpty()) ? pkg : "com.example";
        String mainClass = (mainClassName != null && !mainClassName.isEmpty())
            ? mainClassName : "Main";
        String packagePath = packageName.replace('.', '/');

        // 创建目录
        String[] dirs = {
            "src/main/java/" + packagePath,
            "src/main/resources",
            "libs",
            "build/libs"
        };
        for (String dir : dirs) new File(currentDir, dir).mkdirs();

        // 生成 mod.json
        File modJson = new File(currentDir, "mod.json");
        if (!modJson.exists()) {
            ModSpec spec = new ModSpec();
            spec.setId(currentDir.getName().toLowerCase().replaceAll("[^a-z0-9_-]", ""));
            spec.setName(currentDir.getName());
            spec.setVersion("1.0.0");
            spec.setMainClass(packageName + "." + mainClass);
            spec.setApiVersion("1.0");
            spec.setDescription("");
            spec.setAuthor("");
            spec.setLicense("MIT");
            spec.setHomepage("");
            spec.setDependencies(new ArrayList<>());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(modJson)) {
                gson.toJson(spec, writer);
            }
        }

        // 生成 Main 类
        File mainJava = new File(currentDir,
            "src/main/java/" + packagePath + "/" + mainClass + ".java");
        if (!mainJava.exists()) {
            String content = "package " + packageName + ";\n\n"
                + "public class " + mainClass + " {\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Hello from JModLoader!\");\n"
                + "    }\n"
                + "}\n";
            Files.write(mainJava.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }

        // 生成 jml.config（统一格式，jar 和 jmod 都用它）
        File jmlConfig = new File(currentDir, "jml.config");
        if (!jmlConfig.exists()) {
            StringBuilder sb = new StringBuilder();
            sb.append("# JModLoader Project Config\n");
            sb.append("project.type=").append(type).append("\n");
            sb.append("project.version=1.0.0\n");
            sb.append("java.source=24\n");
            sb.append("java.target=24\n");
            sb.append("main.class=").append(packageName).append(".").append(mainClass).append("\n");
            sb.append("# Dependencies (jars in libs/ are auto-added to classpath)\n");
            sb.append("# deps=gson-2.10.1.jar,another.jar\n");
            Files.write(jmlConfig.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        }

        if ("jar".equals(type)) {
            System.out.println(I18n.get("msg.init_jar_success"));
        } else {
            System.out.println(I18n.get("msg.init_jmod_success"));
        }
        System.out.println("Project: " + currentDir.getName());
        System.out.println("Type: " + type);
        System.out.println("Package: " + packageName);
        System.out.println("Main: " + packageName + "." + mainClass);
        System.out.println("Location: " + currentDir.getAbsolutePath());
    }

    // ── build ─────────────────────────────────────
    @Command(name = "build", description = "Build the current project (auto-detects jar/jmod)")
    public void build() throws Exception {
        printBanner();
        System.out.println(I18n.get("msg.starting"));
        File currentDir = new File(System.getProperty("user.dir"));

        // 读取项目类型
        String projectType = detectProjectType(currentDir);
        System.out.println("Project type: " + projectType);

        ModBuilder builder = new ModBuilder(currentDir, projectType);
        builder.build();
    }

    private String detectProjectType(File dir) {
        File jmlConfig = new File(dir, "jml.config");
        if (jmlConfig.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(jmlConfig)) {
                props.load(fis);
                String type = props.getProperty("project.type");
                if (type != null) return type.toLowerCase();
            } catch (IOException ignored) {}
        }
        // 兼容旧项目
        return "jmod";
    }

    // ── run ────────────────────────────────────────
    @Command(name = "run", description = "Build and run the current project")
    public void run() throws Exception {
        printBanner();
        System.out.println(I18n.get("msg.starting"));
        File currentDir = new File(System.getProperty("user.dir"));

        String projectType = detectProjectType(currentDir);
        ModBuilder builder = new ModBuilder(currentDir, projectType);
        builder.build();

        StringBuilder logMsg = new StringBuilder();
        File buildOutput = null;
        String mainClass = null;

        // 读取 jml.config 获取主类
        File jmlConfig = new File(currentDir, "jml.config");
        if (jmlConfig.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(jmlConfig)) {
                props.load(fis);
                mainClass = props.getProperty("main.class");
            } catch (IOException ignored) {}
        }

        // 读取 mod.json
        File modJson = new File(currentDir, "mod.json");
        if (modJson.exists() && mainClass == null) {
            try (FileReader reader = new FileReader(modJson)) {
                ModSpec spec = new Gson().fromJson(reader, ModSpec.class);
                mainClass = spec.getMainClass();
            }
        }

        // 查找构建产物
        File buildLibs = new File(currentDir, "build/libs");
        if (buildLibs.exists()) {
            String ext = "jar".equals(projectType) ? ".jar" : ".jmod";
            File[] outputs = buildLibs.listFiles((d, n) -> n.endsWith(ext));
            if (outputs != null && outputs.length > 0) buildOutput = outputs[0];
        }

        if (buildOutput == null) {
            System.err.println(I18n.get("msg.run_not_found"));
            logToFile("ERROR: No build output found\n");
            return;
        }

        logMsg.append("Project type: ").append(projectType).append("\n");
        logMsg.append("Build output: ").append(buildOutput.getAbsolutePath()).append("\n");
        logMsg.append("Main class: ").append(mainClass != null ? mainClass : "(null)").append("\n");

        String javaBin = findJava();

        // jar 项目 → java -jar 或 java -cp mainClass
        if ("jar".equals(projectType) && mainClass != null) {
            String cp = buildOutput.getAbsolutePath()
                + (isWindows() ? ";" : ":")
                + buildClasspath(currentDir);
            String[] cmd = new String[]{javaBin, "-cp", cp, mainClass};
            System.out.println(I18n.get("msg.run_launching") + String.join(" ", cmd));
            logMsg.append("Running: ").append(String.join(" ", cmd)).append("\n");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(currentDir);
            pb.redirectErrorStream(true);
            captureAndLog(pb.start(), logMsg);
        } else if (buildOutput.isFile()) {
            // jmod/未知 → java -cp
            String cp = buildOutput.getAbsolutePath()
                + (isWindows() ? ";" : ":")
                + buildClasspath(currentDir);
            String[] cmd;
            if (mainClass != null) {
                cmd = new String[]{javaBin, "-cp", cp, mainClass};
            } else {
                cmd = new String[]{javaBin, "-jar", buildOutput.getAbsolutePath()};
            }
            System.out.println(I18n.get("msg.run_launching") + String.join(" ", cmd));
            logMsg.append("Running: ").append(String.join(" ", cmd)).append("\n");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(currentDir);
            pb.redirectErrorStream(true);
            captureAndLog(pb.start(), logMsg);
        } else {
            // 目录（classes）
            String cp = buildClasspath(currentDir);
            String[] cmd = mainClass != null
                ? new String[]{javaBin, "-cp", cp, mainClass}
                : new String[]{javaBin, "-cp", cp};
            System.out.println(I18n.get("msg.run_launching") + String.join(" ", cmd));
            logMsg.append("Running: ").append(String.join(" ", cmd)).append("\n");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(currentDir);
            pb.redirectErrorStream(true);
            captureAndLog(pb.start(), logMsg);
        }

        System.out.println(I18n.get("msg.run_complete"));
        logToFile(logMsg.toString());
    }

    private String buildClasspath(File currentDir) {
        StringBuilder cp = new StringBuilder();
        File classes = new File(currentDir, "build/classes/java/main");
        if (classes.exists()) cp.append(classes.getAbsolutePath());
        File libs = new File(currentDir, "libs");
        if (libs.exists()) {
            File[] jars = libs.listFiles((d, n) -> n.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    if (cp.length() > 0) cp.append(isWindows() ? ";" : ":");
                    cp.append(jar.getAbsolutePath());
                }
            }
        }
        return cp.length() > 0 ? cp.toString() : ".";
    }

    private void captureAndLog(Process p, StringBuilder logMsg) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            logMsg.append(line).append(System.lineSeparator());
        }
        p.waitFor();
    }

    // ── reginsys ───────────────────────────────────
    @Command(name = "reginsys", description = "Register JModLoader to system PATH")
    public void reginsys() throws IOException {
        System.out.println("Registering JModLoader to system...");
        System.out.println("Platform: " + (isWindows() ? "Windows" : "Unix-like"));

        File homeDir = getJmlHomeDir();
        getModsDir();

        String jarPath = JModLoaderCLI.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();
        try {
            jarPath = java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {}
        if (jarPath.startsWith("/") && isWindows()) jarPath = jarPath.substring(1);

        File sourceJar = new File(jarPath);
        File targetJar = new File(homeDir, "jmodloader-" + VERSION + ".jar");
        File latestJar = new File(homeDir, "jmodloader.jar");

        if (sourceJar.exists() && sourceJar.isFile()) {
            Files.copy(sourceJar.toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(sourceJar.toPath(), latestJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        if (isWindows()) {
            registerOnWindows(homeDir, latestJar);
        } else {
            registerOnUnix(homeDir, latestJar);
        }

        System.out.println(I18n.get("msg.reg_success"));
        System.out.println("JML_HOME: " + jmlHome);
        saveConfig();
    }

    private void registerOnWindows(File homeDir, File jarFile) throws IOException {
        File binDir = new File(System.getenv("LOCALAPPDATA"), "JModLoader\\bin");
        if (!binDir.exists()) binDir.mkdirs();

        File batFile = new File(binDir, "jml.bat");
        String batContent = "@echo off\n"
            + "set JML_HOME=" + homeDir.getAbsolutePath() + "\n"
            + "java -jar \"" + jarFile.getAbsolutePath() + "\" %*\n";
        try (OutputStream os = new FileOutputStream(batFile)) {
            os.write(batContent.getBytes("GBK"));
        }

        String userPath = System.getenv("PATH");
        String newPath = binDir.getAbsolutePath();
        if (userPath != null && !userPath.contains(newPath)) {
            try {
                ProcessBuilder pb = new ProcessBuilder("setx", "PATH",
                    userPath + ";" + newPath);
                pb.inheritIO().start().waitFor();
                System.out.println(I18n.get("msg.reg_added_path"));
            } catch (Exception ignored) {}
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("setx", "JML_HOME",
                homeDir.getAbsolutePath());
            pb.inheritIO().start().waitFor();
            System.out.println(I18n.get("msg.reg_added_env"));
        } catch (Exception ignored) {}

        System.out.println("BAT: " + batFile.getAbsolutePath());
        System.out.println("Please restart your terminal.");
    }

    private void registerOnUnix(File homeDir, File jarFile) throws IOException {
        File binDir = new File(System.getProperty("user.home"), ".local/bin");
        if (!binDir.exists()) binDir.mkdirs();

        File script = new File(binDir, "jml");
        String content = "#!/bin/sh\n"
            + "JML_HOME=\"" + homeDir.getAbsolutePath() + "\"\n"
            + "exec java -jar \"" + jarFile.getAbsolutePath() + "\" \"$@\"\n";
        Files.write(script.toPath(), content.getBytes(StandardCharsets.UTF_8));
        script.setExecutable(true);

        File bashrc = new File(System.getProperty("user.home"), ".bashrc");
        if (bashrc.exists()) {
            String rc = new String(Files.readAllBytes(bashrc.toPath()), StandardCharsets.UTF_8);
            if (!rc.contains("JML_HOME")) {
                String add = "\nexport JML_HOME=\"" + homeDir.getAbsolutePath() + "\"\n"
                    + "export PATH=\"$PATH:" + binDir.getAbsolutePath() + "\"\n";
                Files.write(bashrc.toPath(), add.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
            }
        }
        System.out.println("Script: " + script.getAbsolutePath());
        System.out.println("Please run: source ~/.bashrc");
    }

    // ── update ─────────────────────────────────────
    @Command(name = "update", description = "Check for and apply updates")
    public void update() {
        System.out.println(I18n.get("msg.update_info"));
        try {
            URL url = new URL(UPDATE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                Pattern pat = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = pat.matcher(sb.toString());
                if (m.find()) {
                    String latest = m.group(1).replace("v", "").replace(".", "");
                    String current = VERSION.replace(".", "");
                    if (Integer.parseInt(latest) > Integer.parseInt(current)) {
                        System.out.println(I18n.get("msg.update_available"));
                        System.out.println("Current: " + VERSION + " | Latest: " + m.group(1));
                    } else {
                        System.out.println(I18n.get("msg.update_not_available"));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(I18n.get("msg.update_check_failed") + e.getMessage());
        }
    }

    // ── install ────────────────────────────────────
    @Command(name = "install", description = "Install a mod file")
    public void install(@Parameters(index = "0") String modPath) throws IOException {
        File modFile = new File(modPath);
        if (!modFile.exists()) {
            System.err.println(I18n.get("err.mod_not_found") + modPath);
            return;
        }

        try (JarFile jarFile = new JarFile(modFile)) {
            JarEntry entry = jarFile.getJarEntry("mod.json");
            if (entry != null) {
                try (InputStreamReader reader =
                        new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    ModSpec spec = new Gson().fromJson(reader, ModSpec.class);
                    System.out.println("Mod Info:");
                    System.out.println("  ID: " + spec.getId());
                    System.out.println("  Name: " + spec.getName());
                    System.out.println("  Version: " + spec.getVersion());
                    System.out.println("  Main: " + spec.getMainClass());
                    if (spec.getDescription() != null)
                        System.out.println("  Desc: " + spec.getDescription());
                    if (spec.getAuthor() != null)
                        System.out.println("  Author: " + spec.getAuthor());

                    System.out.print(I18n.get("msg.install_confirm"));
                    Scanner scanner = new Scanner(System.in);
                    if ("y".equalsIgnoreCase(scanner.nextLine())) {
                        File target = new File(getModsDir(), modFile.getName());
                        Files.copy(modFile.toPath(), target.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Installed to: " + target.getAbsolutePath());
                    } else {
                        System.out.println("Cancelled.");
                    }
                }
            } else {
                System.err.println("Invalid mod: mod.json not found inside.");
            }
        }
    }

    // ── mods ───────────────────────────────────────
    @Command(name = "mods", description = "List installed mods")
    public void mods() {
        printBanner();
        File modsDir = getModsDir();
        if (!modsDir.exists()) modsDir.mkdirs();

        ModLoader loader = new ModLoader();
        loader.loadMods(modsDir);

        System.out.println("\n" + I18n.get("msg.installed_mods"));
        System.out.printf("%-20s %-12s %-20s%n", "Name", "Version", "ID");
        System.out.println(String.join("", Collections.nCopies(55, "-")));
        loader.getModSpecs().values().forEach(spec ->
            System.out.printf("%-20s %-12s %-20s%n",
                spec.getName(), spec.getVersion(), spec.getId())
        );
        System.out.println("Total: " + loader.getModSpecs().size() + " mod(s)");
    }

    // ── info ────────────────────────────────────────
    @Command(name = "info", description = "Show JModLoader info")
    public void info() {
        printBanner();
        System.out.println(I18n.get("msg.home_env") + jmlHome);
        System.out.println(I18n.get("msg.java_version") + getJavaVersion());
        System.out.println("Platform: " + (isWindows() ? "Windows" : "Unix-like"));
        System.out.println("Java path: " + findJava());
        File cfg = getConfigFile();
        System.out.println("Config: " + (cfg.exists() ? cfg.getAbsolutePath()
            : "(not created yet)"));
    }

    // ── Banner & entry ──────────────────────────────
    private void printBanner() {
        System.out.println("========================================");
        System.out.println("   JModLoader v" + VERSION + " - Java Mod Loader");
        System.out.println("========================================");
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        checkAndSyncVersion();
        int exitCode = new CommandLine(new JModLoaderCLI()).execute(args);
        System.exit(exitCode);
    }

    private static void checkAndSyncVersion() {
        try {
            String home = System.getenv("JML_HOME");
            if (home == null) home = System.getProperty("user.home") + "/.jmodloader";
            File homeDir = new File(home);
            File homeJar = new File(homeDir, "jmodloader.jar");
            String currentJarPath = JModLoaderCLI.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
            try {
                currentJarPath = java.net.URLDecoder.decode(currentJarPath,
                    StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {}
            if (currentJarPath.startsWith("/")
                    && System.getProperty("os.name").toLowerCase().contains("win")) {
                currentJarPath = currentJarPath.substring(1);
            }
            File currentJar = new File(currentJarPath);

            if (currentJar.exists() && homeJar.exists()
                    && !currentJar.getAbsolutePath().equals(homeJar.getAbsolutePath())) {
                if (currentJar.lastModified() > homeJar.lastModified()) {
                    Files.copy(currentJar.toPath(), homeJar.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception ignored) {}
    }
}
