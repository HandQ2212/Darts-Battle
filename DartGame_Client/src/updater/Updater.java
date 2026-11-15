package updater;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import btl_ltm_n3.Main;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Signed release metadata and platform app-image updates. */
public final class Updater {
    private static final String FEED = "https://github.com/HandQ2212/Darts-Battle/releases/latest/download/darts-update.txt";
    private static final long MAX_ARCHIVE = 600L * 1024 * 1024;
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private Updater() {}

    public static void check(boolean manual) {
        if (!BUSY.compareAndSet(false, true)) return;
        Thread worker = new Thread(() -> {
            boolean delegated = false;
            try {
                Path app = appRoot();
                String platform = platform();
                if (app == null || platform == null) throw new IOException("Bản chạy này không hỗ trợ tự cập nhật.");
                Properties current = new Properties();
                try (InputStream in = Updater.class.getResourceAsStream("/updater/build.properties")) {
                    if (in == null) throw new IOException("Thiếu phiên bản ứng dụng.");
                    current.load(in);
                }
                byte[] signed = readLimited(open(FEED), 65536);
                byte[] pub;
                try (InputStream in = Updater.class.getResourceAsStream("/updater/update-public.der")) { pub = readLimited(in, 4096); }
                Properties release = verify(signed, pub);
                String version = release.getProperty("version", "");
                if (compare(version, current.getProperty("version", "")) <= 0) {
                    if (manual) ui(() -> Main.notifications().info("Đã cập nhật", "Bạn đang dùng phiên bản mới nhất."));
                    return;
                }
                String url = release.getProperty(platform + ".url", "");
                String hash = release.getProperty(platform + ".sha256", "");
                if (!hash.matches("[0-9a-f]{64}")) throw new IOException("Thiếu mã kiểm tra bản cập nhật.");
                uri(url);
                delegated = true;
                ui(() -> { BUSY.set(false); if (manual || Main.isLoginPage()) Main.notifications().confirm("Có phiên bản " + version, "Tải và cài bản mới? Game sẽ khởi động lại sau khi tải xong.", "Cập nhật", () -> downloadAndInstall(app, platform, url, hash)); });
            } catch (Exception e) {
                if (manual) ui(() -> Main.notifications().info("Chưa thể kiểm tra cập nhật", "Vui lòng thử lại sau. " + safeMessage(e)));
            } finally { if (!delegated) BUSY.set(false); }
        }, "update-check");
        worker.setDaemon(true); worker.start();
    }
    private static void downloadAndInstall(Path app, String platform, String url, String hash) {
        if (!BUSY.compareAndSet(false, true)) return;
        AtomicBoolean cancelled = new AtomicBoolean();
        Dialog<Void> progress = new Dialog<>();
        progress.setTitle("Cập nhật Darts Battle");
        progress.initOwner(Main.notifications().root().getScene().getWindow());
        progress.initModality(Modality.WINDOW_MODAL);
        progress.getDialogPane().setContent(new HBox(12, new ProgressIndicator(), new Label("Đang tải và kiểm tra bản cập nhật...")));
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        progress.getDialogPane().getButtonTypes().add(cancel);
        progress.setOnCloseRequest(event -> cancelled.set(true));
        progress.setResultConverter(button -> { if (button == cancel) cancelled.set(true); return null; });
        progress.show();
        Thread worker = new Thread(() -> {
            Path temp = null;
            boolean scheduled = false;
            try {
                temp = Files.createTempDirectory("darts-update-");
                Path archive = temp.resolve("update.zip");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream in = open(url); OutputStream out = Files.newOutputStream(archive)) {
                    byte[] buffer = new byte[32768]; long total = 0; int n;
                    while ((n = in.read(buffer)) != -1) { if (cancelled.get()) return; total += n; if (total > MAX_ARCHIVE) throw new IOException("Bản cập nhật quá lớn."); digest.update(buffer, 0, n); out.write(buffer, 0, n); }
                }
                if (cancelled.get()) return;
                if (!HexFormat.of().formatHex(digest.digest()).equals(hash)) throw new IOException("Bản cập nhật không vượt qua kiểm tra toàn vẹn.");
                String helper = platform.equals("mac") ? "install-mac.sh" : "install-windows.ps1";
                Path script = temp.resolve(helper);
                try (InputStream in = Updater.class.getResourceAsStream("/updater/" + helper)) { Files.copy(in, script); }
                ProcessBuilder process = platform.equals("mac")
                    ? new ProcessBuilder("/bin/sh", script.toString(), app.toString(), archive.toString(), Long.toString(ProcessHandle.current().pid()))
                    : new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString(), app.toString(), archive.toString(), Long.toString(ProcessHandle.current().pid()));
                process.directory(temp.toFile());
                process.redirectErrorStream(true).redirectOutput(temp.resolve("install.log").toFile());
                if (cancelled.get()) return;
                Path working = temp;
                scheduled = true;
                ui(() -> {
                    if (cancelled.get()) { removeTemp(working); return; }
                    try {
                        process.start();
                        if (Main.socketHandler != null) Main.socketHandler.shutdown();
                        Main.stopBackgroundMusic(); Platform.exit(); System.exit(0);
                    } catch (Exception e) {
                        progress.close(); removeTemp(working);
                        Main.notifications().info("Không thể cập nhật", safeMessage(e));
                    }
                });
            } catch (Exception e) {
                if (!cancelled.get()) ui(() -> { progress.close(); Main.notifications().info("Không thể cập nhật", safeMessage(e)); });
            } finally { if (!scheduled && temp != null) removeTemp(temp); BUSY.set(false); }
        }, "update-download");
        worker.setDaemon(true); worker.start();
    }
    private static String safeMessage(Exception e) { return e instanceof IOException ? Objects.toString(e.getMessage(), "") : "Vui lòng thử lại sau."; }
    private static void removeTemp(Path directory) {
        try (var files = Files.walk(directory)) { files.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) {} }); }
        catch (IOException ignored) {}
    }
    private static void ui(Runnable r) { Platform.runLater(r); }
    static Properties verify(byte[] signed, byte[] pub) throws Exception {
        int split = -1;
        for (int i = 0; i < signed.length; i++) if (signed[i] == '\n') { split = i; break; }
        if (split < 1) throw new IOException("Thông tin cập nhật không hợp lệ.");
        byte[] signature = Base64.getDecoder().decode(new String(signed, 0, split, StandardCharsets.US_ASCII).trim());
        byte[] payload = Arrays.copyOfRange(signed, split + 1, signed.length);
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(pub)));
        verifier.update(payload);
        if (!verifier.verify(signature)) throw new IOException("Chữ ký bản cập nhật không hợp lệ.");
        Properties release = new Properties(); release.load(new ByteArrayInputStream(payload)); return release;
    }
    static int compare(String a, String b) throws IOException {
        if (!a.matches("[0-9]{1,6}(\\.[0-9]{1,6}){1,3}") || !b.matches("[0-9]{1,6}(\\.[0-9]{1,6}){1,3}")) throw new IOException("Phiên bản không hợp lệ.");
        String[] x=a.split("\\."), y=b.split("\\.");
        for(int i=0;i<Math.max(x.length,y.length);i++){int p=i<x.length?Integer.parseInt(x[i]):0, q=i<y.length?Integer.parseInt(y[i]):0;if(p!=q)return Integer.compare(p,q);} return 0;
    }
    private static String platform() {
        String os=System.getProperty("os.name").toLowerCase(Locale.ROOT), arch=System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if(os.contains("mac") && (arch.equals("aarch64") || arch.equals("arm64"))) return "mac";
        if(os.contains("windows") && (arch.equals("amd64") || arch.equals("x86_64"))) return "windows";
        return null;
    }
    private static Path appRoot() throws Exception {
        Path jar=Paths.get(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        if(!Files.isRegularFile(jar) || !jar.getFileName().toString().equals("darts-client.jar")) return null;
        Path parent=jar.getParent();
        if(parent.getFileName().toString().equals("app") && parent.getParent()!=null) {
            Path base=parent.getParent();
            if(base.getFileName().toString().equals("Contents") && base.getParent()!=null && base.getParent().getFileName().toString().endsWith(".app") && Files.isRegularFile(base.resolve("MacOS/DartsBattle"))) return base.getParent();
            if(Files.isRegularFile(base.resolve("DartsBattle.exe"))) return base;
        }
        return null;
    }
    private static URI uri(String value) throws IOException { try { URI u=URI.create(value); if(!"https".equalsIgnoreCase(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null) throw new IOException("Địa chỉ cập nhật không hợp lệ."); return u; } catch(IllegalArgumentException e){throw new IOException("Địa chỉ cập nhật không hợp lệ.");} }
    private static InputStream open(String value) throws IOException {
        URI u=uri(value);
        for(int i=0;i<6;i++) {
            HttpURLConnection c=(HttpURLConnection)u.toURL().openConnection(); c.setInstanceFollowRedirects(false); c.setConnectTimeout(10000); c.setReadTimeout(30000); c.setRequestProperty("User-Agent", "DartsBattle-Updater");
            int status=c.getResponseCode();
            if(status>=300 && status<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Chuyển hướng không hợp lệ.");u=uri(u.resolve(location).toString());continue;}
            if(status!=200){c.disconnect();throw new IOException("Máy chủ cập nhật chưa sẵn sàng (HTTP " + status + ").");} return c.getInputStream();
        } throw new IOException("Quá nhiều lần chuyển hướng.");
    }
    private static byte[] readLimited(InputStream source, int limit) throws IOException {
        if(source==null)throw new IOException("Thiếu dữ liệu cập nhật.");
        try(InputStream in=source;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Dữ liệu cập nhật quá lớn.");out.write(b,0,n);}return out.toByteArray();}
    }
}
