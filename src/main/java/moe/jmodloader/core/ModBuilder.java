package moe.jmodloader.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import moe.jmodloader.api.ModSpec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;

/**
 * ModBuilder - Cross-platform mod compilation and packaging
 * Supports both jar and jmod output types.
 */
public class ModBuilder {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File projectDir;
    private final File buildDir;
    private final File classesDir;
    private final File libsDir;
    private final File outputDir;
    private final String projectType;
    private final boolean isWindows;

    public ModBuilder(File projectDir, String projectType) {
        this.projectDir = projectDir;
        this.buildDir = new File(projectDir, "build");
        this.classesDir = new File(buildDir, "classes");
        this.libsDir = new File(buildDir, "libs");
        this.outputDir = new File(projectDir, "build/libs");
        this.projectType = (projectType == null) ? "jmod" : projectType.toLowerCase();
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    }

    public void build() throws Exception {
        System.out.println("Building JModLoader project (" + projectType + ")...");

        cleanDirectory(buildDir);
        classesDir.mkdirs();
        outputDir.mkdirs();

        // Find Java sources
        Path srcPath = projectDir.toPath().resolve("src/main/java");
        if (!Files.exists(srcPath)) {
            throw new IOException("Source directory not found: " + srcPath);
        }

        List<String> javaFiles = Files.walk(srcPath)
                .filter(p -> p.toString().endsWith(".java"))
                .map(Path::toString)
                .collect(Collectors.toList());

        if (javaFiles.isEmpty()) {
            System.out.println("No Java files found.");
            return;
        }

        // Compile
        compile(javaFiles);

        // Load or create spec
        ModSpec spec = loadOrCreateSpec();

        // Write mod.json to classes
        File modJsonFile = new File(classesDir, "mod.json");
        try (FileWriter writer = new FileWriter(modJsonFile)) {
            gson.toJson(spec, writer);
        }

        // Copy resources
        Path resPath = projectDir.toPath().resolve("src/main/resources");
        if (Files.exists(resPath)) {
            copyResources(resPath, classesDir.toPath());
        }

        // Package based on project type
        String version = spec.getVersion() != null ? spec.getVersion() : "1.0.0";
        String ext = "jar".equals(projectType) ? ".jar" : ".jmod";
        String jarName = spec.getId() + "-" + version + ext;
        File outputFile = new File(outputDir, jarName);

        createJar(classesDir, outputFile);

        System.out.println("Build successful! Created: " + outputFile.getAbsolutePath());
        logToFile("BUILD SUCCESS (" + projectType + "): " + outputFile.getAbsolutePath());
    }

    private void compile(List<String> javaFiles) throws Exception {
        String javaVersion = detectJavaVersion();
        System.out.println("Detected Java: " + javaVersion);

        List<String> cmd = new ArrayList<>();
        cmd.add(isWindows ? "javac.exe" : "javac");
        cmd.add("-d");
        cmd.add(classesDir.getAbsolutePath());
        cmd.add("-encoding");
        cmd.add("UTF-8");
        cmd.add("--release");
        cmd.add("24");

        // Classpath: . + libs/*.jar
        StringBuilder cp = new StringBuilder(".");
        File projectLibs = new File(projectDir, "libs");
        if (projectLibs.exists() && projectLibs.isDirectory()) {
            File[] jars = projectLibs.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                String sep = isWindows ? ";" : ":";
                for (File jar : jars) {
                    cp.append(sep).append(jar.getAbsolutePath());
                }
            }
        }
        cmd.add("-cp");
        cmd.add(cp.toString());
        cmd.addAll(javaFiles);

        System.out.println("Compiling with: javac --release 24");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        int code = pb.start().waitFor();
        if (code != 0) {
            throw new Exception("Compilation failed with exit code: " + code);
        }
    }

    private String detectJavaVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(isWindows ? "java.exe" : "java", "-version");
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

    private ModSpec loadOrCreateSpec() throws IOException {
        File specFile = new File(projectDir, "mod.json");
        if (specFile.exists()) {
            try (FileReader reader = new FileReader(specFile)) {
                return gson.fromJson(reader, ModSpec.class);
            }
        }

        ModSpec spec = new ModSpec();
        spec.setId(projectDir.getName().toLowerCase().replaceAll("[^a-z0-9_-]", ""));
        spec.setName(projectDir.getName());
        spec.setVersion("1.0.0");
        spec.setMainClass("com.example.Main");
        spec.setApiVersion("1.0");
        spec.setDescription("");
        spec.setAuthor("");
        spec.setLicense("MIT");
        spec.setHomepage("");
        spec.setDependencies(new ArrayList<>());
        return spec;
    }

    private void copyResources(Path source, Path target) {
        try {
            Files.walk(source).forEach(p -> {
                try {
                    Path dest = target.resolve(source.relativize(p));
                    if (Files.isDirectory(p)) {
                        if (!Files.exists(dest)) Files.createDirectories(dest);
                    } else {
                        Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a JAR file. Entry names always use "/" (JAR standard),
     * regardless of OS.
     */
    private void createJar(File sourceDir, File outputFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile), manifest)) {
            Path src = sourceDir.toPath();
            Files.walk(src)
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(p -> {
                        // Always use "/" for JAR entries
                        String entryName = src.relativize(p).toString().replace("\\", "/");
                        try {
                            jos.putNextEntry(new JarEntry(entryName));
                            Files.copy(p, jos);
                            jos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private void cleanDirectory(File dir) throws IOException {
        if (dir.exists()) {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private void logToFile(String msg) {
        try {
            File logFile = new File(projectDir, "lastrun.log");
            String content = "=== JModLoader Build Log ===" + System.lineSeparator()
                + "Time: " + new Date() + System.lineSeparator()
                + msg + System.lineSeparator()
                + "============================" + System.lineSeparator();
            Files.write(logFile.toPath(), content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}
