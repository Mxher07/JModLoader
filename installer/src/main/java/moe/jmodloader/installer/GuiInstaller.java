package moe.jmodloader.installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class GuiInstaller extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String GITHUB_API = "https://api.github.com/repos/Mxher07/JModLoader/releases/latest";

    private JComboBox<String> langCombo;
    private JTextField pathField;
    private JCheckBox pathCheckBox;
    private JCheckBox overwriteCheckBox;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private String installPath;
    private boolean registerPath = true;
    private boolean overwrite = false;
    private String latestVersion;
    private String currentVersion = "unknown";

    public GuiInstaller() {
        super("JModLoader Installer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
        setVisible(true);
        new Thread(this::checkLatestVersion).start();
    }

    private void initUI() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(welcomePanel(), "welcome");
        cardPanel.add(langPanel(), "lang");
        cardPanel.add(installPathPanel(), "path");
        cardPanel.add(installingPanel(), "installing");
        cardPanel.add(successPanel(), "success");
        cardPanel.add(failedPanel(), "failed");
        add(cardPanel);
    }

    private JPanel welcomePanel() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel("\u2699", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 64));
        JLabel title = new JLabel(I18n.get("welcome"), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JLabel ver = new JLabel(I18n.get("version") + " " + getVersion(), SwingConstants.CENTER);
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ver.setForeground(Color.GRAY);
        JButton next = btn(I18n.get("next") + " >", e -> {
            I18n.setLang((String) langCombo.getSelectedItem());
            refreshTexts();
            cardLayout.show(cardPanel, "path");
        }, 120, 36);
        JButton cancel = btn(I18n.get("cancel"), e -> System.exit(0), 120, 36);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.add(cancel);
        btnPanel.add(next);
        center.add(icon); center.add(Box.createVerticalStrut(15));
        center.add(title); center.add(Box.createVerticalStrut(8));
        center.add(ver); center.add(Box.createVerticalStrut(30));
        center.add(btnPanel);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel langPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JLabel lbl = new JLabel(I18n.get("select_lang"));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        langCombo = new JComboBox<>(new String[]{"English (en_US)", "\u7b80\u4f53\u4e2d\u6587 (zh_CN)"});
        langCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        langCombo.setMaximumSize(new Dimension(250, 32));
        JButton next = btn(I18n.get("next") + " >", e -> {
            String sel = (String) langCombo.getSelectedItem();
            I18n.setLang(sel.contains("\u7b80") ? "zh_CN" : "en_US");
            refreshTexts();
            cardLayout.show(cardPanel, "path");
        }, 120, 36);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        btnPanel.add(next);
        p.add(lbl); p.add(Box.createVerticalStrut(10));
        p.add(langCombo); p.add(Box.createVerticalStrut(20));
        p.add(btnPanel);
        return p;
    }

    private JPanel installPathPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
        JLabel header = new JLabel(I18n.get("install_path"));
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JPanel pathPanel = new JPanel(new BorderLayout(5, 5));
        pathField = new JTextField(getDefaultPath());
        pathField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JButton browse = btn(I18n.get("browse"), e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(pathField.getText()));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }, -1, 28);
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browse, BorderLayout.EAST);
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        pathCheckBox = new JCheckBox(I18n.get("register_path"), true);
        pathCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pathCheckBox.addActionListener(e -> registerPath = pathCheckBox.isSelected());
        overwriteCheckBox = new JCheckBox(I18n.get("overwrite"), false);
        overwriteCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        overwriteCheckBox.addActionListener(e -> overwrite = overwriteCheckBox.isSelected());
        options.add(pathCheckBox);
        options.add(overwriteCheckBox);
        checkExistingInstall();
        JButton installBtn = btn(I18n.get("installing"), e -> doInstall(), 140, 38);
        installBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JButton backBtn = btn("< " + I18n.get("back"), e -> cardLayout.show(cardPanel, "welcome"), 120, 38);
        JButton cancelBtn = btn(I18n.get("cancel"), e -> System.exit(0), 120, 38);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.add(cancelBtn);
        btnPanel.add(backBtn);
        btnPanel.add(installBtn);
        JPanel south = new JPanel(new BorderLayout());
        south.add(options, BorderLayout.CENTER);
        south.add(btnPanel, BorderLayout.SOUTH);
        p.add(header, BorderLayout.NORTH);
        p.add(pathPanel, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel installingPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        statusLabel = new JLabel(I18n.get("extracting"), SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logArea = new JTextArea(12, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        p.add(statusLabel, BorderLayout.NORTH);
        p.add(progressBar, BorderLayout.CENTER);
        p.add(scroll, BorderLayout.SOUTH);
        return p;
    }

    private JPanel successPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel("\u2705", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        JLabel title = new JLabel(I18n.get("success"), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JLabel pathLbl = new JLabel(I18n.get("install_path") + " " + installPath, SwingConstants.CENTER);
        pathLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pathLbl.setForeground(Color.GRAY);
        JButton finish = btn(I18n.get("finish"), e -> {
            if (registerPath) showPathHint();
            System.exit(0);
        }, 140, 38);
        finish.setFont(new Font("Segoe UI", Font.BOLD, 14));
        center.add(icon); center.add(Box.createVerticalStrut(15));
        center.add(title); center.add(Box.createVerticalStrut(8));
        center.add(pathLbl); center.add(Box.createVerticalStrut(30));
        center.add(finish);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel failedPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JLabel icon = new JLabel("\u274c", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        JLabel title = new JLabel(I18n.get("failed"), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        JButton retry = btn(I18n.get("back"), e -> cardLayout.show(cardPanel, "path"), 140, 38);
        JButton exit = btn(I18n.get("cancel"), e -> System.exit(1), 140, 38);
        JPanel btnPanel = new JPanel();
        btnPanel.add(exit);
        btnPanel.add(retry);
        p.add(icon, BorderLayout.NORTH);
        p.add(title, BorderLayout.CENTER);
        p.add(scroll, BorderLayout.CENTER);
        p.add(btnPanel, BorderLayout.SOUTH);
        return p;
    }

    private JButton btn(String text, ActionListener l, int w, int h) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (w > 0 && h > 0) b.setPreferredSize(new Dimension(w, h));
        if (l != null) b.addActionListener(l);
        return b;
    }

    private String getDefaultPath() {
        return System.getProperty("os.name").toLowerCase().contains("win")
            ? "C:\\JModLoader"
            : System.getProperty("user.home") + "/.jmodloader";
    }

    private String getVersion() {
        return "1.1.2";
    }

    private void checkExistingInstall() {
        String jmlHome = System.getenv("JML_HOME");
        if (jmlHome == null) jmlHome = getDefaultPath();
        File cfg = new File(jmlHome, "config.properties");
        if (cfg.exists()) {
            try {
                Properties p = new Properties();
                p.load(new FileInputStream(cfg));
                String v = p.getProperty("version");
                if (v != null) currentVersion = v;
            } catch (IOException ignored) {}
        }
    }

    private void checkLatestVersion() {
        try {
            URL url = new URL(GITHUB_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String body = sb.toString();
                int idx = body.indexOf("\"tag_name\"");
                if (idx >= 0) {
                    int start = body.indexOf("\"", idx + 12) + 1;
                    int end = body.indexOf("\"", start);
                    latestVersion = body.substring(start, end);
                }
            }
        } catch (Exception ignored) {}
    }

    private void refreshTexts() {
        setTitle(I18n.get("title"));
    }

    private void doInstall() {
        installPath = pathField.getText().trim();
        if (installPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an installation directory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File target = new File(installPath);
        if (!target.exists()) target.mkdirs();
        cardLayout.show(cardPanel, "installing");
        new Thread(() -> install(target)).start();
    }

    private void install(File target) {
        StringBuilder errors = new StringBuilder();
        try {
            setStatus(I18n.get("extracting"), 10);
            log("Install to: " + target.getAbsolutePath());
            File mainDir = new File(target, "jmodloader");
            mainDir.mkdirs();
            File jmlJar = new File(mainDir, "jmodloader.jar");
            String selfPath = GuiInstaller.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
            try { selfPath = URLDecoder.decode(selfPath, StandardCharsets.UTF_8.name()); } catch (Exception ignored) {}
            if (selfPath.endsWith(".jar")) {
                Files.copy(Paths.get(selfPath), jmlJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                log("Note: Running from IDE, JAR not bundled.");
            }
            setStatus(I18n.get("creating_shortcuts"), 40);
            log("Creating launcher...");
            createLauncher(mainDir, target);
            setStatus(I18n.get("registering_path"), 60);
            if (registerPath) registerToPath(target);
            setStatus(I18n.get("installing"), 80);
            File cfg = new File(target, "config.properties");
            Properties props = new Properties();
            props.setProperty("version", getVersion());
            props.setProperty("installed.path", target.getAbsolutePath());
            props.store(new FileOutputStream(cfg), "JModLoader Config");
            log("Done!");
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                cardLayout.show(cardPanel, "success");
            });
        } catch (Exception e) {
            errors.append(e.getMessage());
            log("ERROR: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                logArea.setText(errors.toString());
                cardLayout.show(cardPanel, "failed");
            });
        }
    }

    private void createLauncher(File mainDir, File target) throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            File binDir = new File(System.getenv("LOCALAPPDATA"), "JModLoader\\bin");
            binDir.mkdirs();
            File batFile = new File(binDir, "jml.bat");
            String content = "@echo off\n"
                + "set JML_HOME=" + mainDir.getAbsolutePath() + "\n"
                + "java -jar \"" + new File(mainDir, "jmodloader.jar").getAbsolutePath() + "\" %*\n";
            try (OutputStream os = new FileOutputStream(batFile)) {
                os.write(content.getBytes("GBK"));
            }
            log("Created: " + batFile.getAbsolutePath());
        } else {
            File binDir = new File("/usr/local/bin");
            File script = new File(binDir, "jml");
            String content = "#!/bin/sh\n"
                + "JML_HOME=\"" + mainDir.getAbsolutePath() + "\"\n"
                + "exec java -jar \"" + new File(mainDir, "jmodloader.jar").getAbsolutePath() + "\" \"$@\"\n";
            Files.write(script.toPath(), content.getBytes(StandardCharsets.UTF_8));
            script.setExecutable(true);
            log("Created: " + script.getAbsolutePath());
        }
    }

    private void registerToPath(File target) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                String userPath = System.getenv("PATH");
                String binPath = new File(System.getenv("LOCALAPPDATA"), "JModLoader\\bin").getAbsolutePath();
                if (userPath != null && !userPath.contains(binPath)) {
                    ProcessBuilder pb = new ProcessBuilder("setx", "PATH", userPath + ";" + binPath);
                    pb.inheritIO().start().waitFor();
                }
                ProcessBuilder pb2 = new ProcessBuilder("setx", "JML_HOME", target.getAbsolutePath());
                pb2.inheritIO().start().waitFor();
                log("PATH and JML_HOME registered.");
            } else {
                File profileD = new File("/etc/profile.d/jmodloader.sh");
                String content = "export JML_HOME=\"" + target.getAbsolutePath() + "\"\n";
                Files.write(profileD.toPath(), content.getBytes(StandardCharsets.UTF_8));
                log("JML_HOME set in /etc/profile.d/jmodloader.sh");
            }
        } catch (Exception e) {
            log("Warning: Could not fully register PATH: " + e.getMessage());
        }
    }

    private void setStatus(String text, int progress) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            progressBar.setValue(progress);
        });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void showPathHint() {
        JOptionPane.showMessageDialog(this,
            "JModLoader has been registered to PATH.\n"
            + "Please restart your terminal or run: refreshenv",
            "PATH Updated", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new GuiInstaller();
        });
    }
}
