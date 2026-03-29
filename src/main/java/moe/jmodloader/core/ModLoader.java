package moe.jmodloader.core;

import com.google.gson.Gson;
import moe.jmodloader.api.ModSpec;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModLoader {
    private final Map<String, ModSpec> modSpecs = new LinkedHashMap<>();
    private final Map<String, ModClassLoader> modLoaders = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private final File logFile;

    public ModLoader() {
        this.logFile = new File(System.getProperty("user.dir"), "lastrun.log");
    }

    public void loadMods(File modsDir) {
        if (!modsDir.exists() || !modsDir.isDirectory()) return;

        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".jmod"));
        if (files == null) return;

        for (File file : files) {
            try (JarFile jarFile = new JarFile(file)) {
                JarEntry entry = jarFile.getJarEntry("mod.json");
                if (entry == null) continue;

                try (InputStreamReader reader = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    ModSpec spec = gson.fromJson(reader, ModSpec.class);
                    if (spec.getId() == null || spec.getMainClass() == null) continue;
                    modSpecs.put(spec.getId(), spec);

                    URL[] urls = new URL[]{file.toURI().toURL()};
                    ModClassLoader loader = new ModClassLoader(spec.getId(), urls, getClass().getClassLoader());
                    modLoaders.put(spec.getId(), loader);
                    log("[Loader] Loaded mod: " + spec.getId() + " v" + spec.getVersion() + " from " + file.getName());
                }
            } catch (Exception e) {
                log("[Loader] Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }

        resolveDependencies();
        initializeMods();
    }

    private void resolveDependencies() {
        for (ModSpec spec : modSpecs.values()) {
            ModClassLoader loader = modLoaders.get(spec.getId());
            if (spec.getDependencies() != null) {
                for (String depId : spec.getDependencies()) {
                    ModClassLoader depLoader = modLoaders.get(depId);
                    if (depLoader != null) {
                        loader.addDependencyLoader(depId, depLoader);
                        log("[Loader] Resolved dependency: " + spec.getId() + " -> " + depId);
                    }
                }
            }
        }
    }

    private void initializeMods() {
        for (ModSpec spec : modSpecs.values()) {
            try {
                ModClassLoader loader = modLoaders.get(spec.getId());
                Class<?> mainClass = loader.loadClass(spec.getMainClass());

                // Capture mod output to log file
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;

                PipedOutputStream pipedOut = new PipedOutputStream();
                PrintStream capturingOut = new PrintStream(pipedOut, true, StandardCharsets.UTF_8.name());

                PipedOutputStream pipedErr = new PipedOutputStream();
                PrintStream capturingErr = new PrintStream(pipedErr, true, StandardCharsets.UTF_8.name());

                System.setOut(new PrefixPrintStream(capturingOut, "[" + spec.getId() + "] "));
                System.setErr(new PrefixPrintStream(capturingErr, "[" + spec.getId() + "] [ERR] "));

                StringBuilder captured = new StringBuilder();

                try {
                    // Start reader threads
                    Thread outReader = new Thread(() -> {
                        try { readPipe(new PipedInputStream(pipedOut), originalOut, captured); } catch (IOException ignored) {}
                    });
                    Thread errReader = new Thread(() -> {
                        try { readPipe(new PipedInputStream(pipedErr), originalErr, captured); } catch (IOException ignored) {}
                    });
                    outReader.start();
                    errReader.start();

                    mainClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
                } catch (NoSuchMethodException ignored) {} finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                    if (captured.length() > 0) {
                        log("[Mod:" + spec.getId() + "]\n" + captured);
                    }
                }

            } catch (Exception e) {
                log("[Loader] Failed to initialize mod " + spec.getId() + ": " + e.getMessage());
            }
        }
    }

    private void log(String msg) {
        try {
            String line = "[" + new Date() + "] " + msg + System.lineSeparator();
            Files.write(logFile.toPath(), line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private void readPipe(PipedInputStream in, PrintStream original, StringBuilder dest) throws IOException {
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            String s = new String(buf, 0, n, StandardCharsets.UTF_8);
            original.print(s);
            dest.append(s);
        }
    }

    public Map<String, ModSpec> getModSpecs() {
        return modSpecs;
    }

    private static class PrefixPrintStream extends PrintStream {
        private final String prefix;
        private boolean newline = true;

        public PrefixPrintStream(PrintStream out, String prefix) {
            super(out, true);
            this.prefix = prefix;
        }

        @Override
        public void write(int b) {
            if (newline) {
                super.print(prefix);
                newline = false;
            }
            super.write(b);
            if (b == '\n') newline = true;
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            for (int i = 0; i < len; i++) {
                write(buf[off + i]);
            }
        }
    }
}
