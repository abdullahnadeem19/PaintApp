import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Appends activity asynchronously; the JavaFX UI never waits on file I/O. */
public final class ActivityLogger {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "paint-activity-log");
        thread.setDaemon(true);
        return thread;
    });
    private static final Path FILE = Paths.get(System.getProperty("user.home"), "PaintApp", "activity.log");
    private static Supplier<String> currentFile = () -> "new tab";
    private ActivityLogger() { }
    public static void setFileSupplier(Supplier<String> supplier) { currentFile = supplier; }
    public static void log(String event) {
        // Capture user-facing context and timestamp before handing off to worker.
        String name;
        try { name = String.valueOf(currentFile.get()); } catch (Exception e) { name = "unknown"; }
        final String safeName = name.replace('\n', ' ').replace('\r', ' ');
        final String message = event.replace('\n', ' ').replace('\r', ' ');
        final String line = LocalDateTime.now().format(FORMAT) + " [" + safeName + "] " + message + System.lineSeparator();
        try {
            WRITER.execute(() -> {
                try {
                    Files.createDirectories(FILE.getParent());
                    Files.write(FILE, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException ex) { System.err.println("Could not write activity log: " + ex.getMessage()); }
            });
        } catch (RejectedExecutionException ignored) { }
    }
    public static void shutdown() {
        WRITER.shutdown();
        try { if (!WRITER.awaitTermination(3, TimeUnit.SECONDS)) WRITER.shutdownNow(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
}
