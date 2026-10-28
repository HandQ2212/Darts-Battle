import btl_ltm_n3.Main;
import controllers.SocketHandler;
import javafx.application.Platform;
import javafx.stage.Stage;
import protocol.Message;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Exercises navigation on a real native window, including maximize and fullscreen. */
public class WindowNavigationTest {
    private static Stage stage;
    private static class FakeSocket extends SocketHandler {
        @Override public void ensureConnected(Runnable ready) {}
        @Override public void sendData(Message message) {}
    }
    private record Bounds(double x, double y, double width, double height) {}
    private static <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try { result.complete(action.call()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
    private static Bounds bounds() {
        return new Bounds(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static boolean close(double a, double b) { return Math.abs(a - b) <= 2; }
    private static void navigate(String mode) throws Exception {
        Bounds before = fx(() -> bounds());
        for (String page : new String[]{"home", "ranking", "match_history", "startgame", "home", "register", "login"}) {
            fx(() -> { Main.setRoot(page); return null; });
            // Allow window-manager events and JavaFX layout pulses to settle.
            Thread.sleep(200);
            fx(() -> {
                Bounds after = bounds();
                check(close(before.x(), after.x()) && close(before.y(), after.y()), mode + " position changed on " + page);
                check(close(before.width(), after.width()) && close(before.height(), after.height()), mode + " size changed on " + page);
                check(!mode.equals("maximized") || stage.isMaximized(), "Maximized state lost on " + page);
                check(!mode.equals("fullscreen") || stage.isFullScreen(), "Fullscreen state lost on " + page);
                check(stage.getMinWidth() == 1040 && stage.getMinHeight() == 780, "Minimum size changed on " + page);
                return null;
            });
        }
        System.out.println("PASS navigation preserves " + mode + " window geometry and state");
    }
    public static void main(String[] args) throws Exception {
        // Isolate the test from the running game server and saved player session.
        System.setProperty("darts.host", "127.0.0.1");
        System.setProperty("darts.port", "1");
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(() -> started.complete(null));
        started.get(10, TimeUnit.SECONDS);
        try {
            fx(() -> {
                stage = new Stage();
                new Main().start(stage);
                Main.socketHandler.shutdown();
                Main.stopBackgroundMusic();
                Main.socketHandler = new FakeSocket();
                Main.socketHandler.loginUser = "WindowTest";
                Main.socketHandler.competitor = "Opponent";
                stage.setX(60); stage.setY(40);
                stage.setWidth(1120); stage.setHeight(840);
                return null;
            });
            Thread.sleep(400);
            navigate("normal");
            fx(() -> { stage.setMaximized(true); return null; });
            Thread.sleep(500);
            check(fx(() -> stage.isMaximized()), "Window did not maximize");
            navigate("maximized");
            fx(() -> { stage.setMaximized(false); return null; });
            Thread.sleep(300);
            fx(() -> { stage.setFullScreen(true); return null; });
            Thread.sleep(500);
            check(fx(() -> stage.isFullScreen()), "Window did not enter fullscreen");
            navigate("fullscreen");
            System.out.println("ALL WINDOW NAVIGATION CHECKS PASSED");
        } finally {
            fx(() -> {
                if (Main.startGameController != null) Main.startGameController.dispose();
                if (Main.socketHandler != null) Main.socketHandler.shutdown();
                if (stage != null) stage.hide();
                return null;
            });
            Platform.exit();
        }
    }
}
