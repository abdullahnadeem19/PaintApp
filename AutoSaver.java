import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Periodically saves tabs that have unsaved changes.
 *
 * Timing: a one-second tick counts down to the next autosave. The remaining
 * time is exposed as a property so the UI can show a live countdown, and the
 * countdown can be restarted (for example when the user saves by hand).
 *
 * Threading: the timer and the canvas snapshot run on the JavaFX thread
 * (JavaFX requires that), but the slow part, encoding the image and writing
 * it to disk, is handed to a single background thread so the UI never stalls.
 *
 * What gets saved:
 *  - A tab that already has a file is saved to that file, just like File > Save.
 *    Its "*" unsaved marker clears.
 *  - An untitled tab is NOT forced through a Save As dialog. It is written to a
 *    recovery folder instead, and stays marked as unsaved.
 */
public class AutoSaver {

    public static final int DEFAULT_INTERVAL_SECONDS = 60;

    // Where untitled tabs are backed up
    private static final File RECOVERY_DIR =
            new File(System.getProperty("user.home"), ".paint-autosave");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm:ss a");

    private final ObservableList<Tab> tabs;
    private final Consumer<String> onStatus;


    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "autosave-writer");
        t.setDaemon(true);
        return t;
    });

    // For untitled tabs: the change count already backed up, so an idle tab
    // isn't rewritten every tick.
    private final Map<DrawingCanvas, Long> lastBackedUp = new WeakHashMap<>();

    // Countdown shown to the user (optional)
    private final IntegerProperty secondsRemaining = new SimpleIntegerProperty(0);
    private final StringProperty countdownText = new SimpleStringProperty("Autosave off");
    private final BooleanProperty countdownVisible = new SimpleBooleanProperty(true);

    private Timeline ticker;
    private boolean enabled = false;
    private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;

    /**
     * @param tabs     the live list of tabs to watch
     * @param onStatus called on the JavaFX thread with a short message
     *                 ("Autosaved at ...", "Autosave failed: ...")
     */
    public AutoSaver(ObservableList<Tab> tabs, Consumer<String> onStatus) {
        this.tabs = tabs;
        this.onStatus = onStatus;
    }

    //  Settings

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        restartTimer();
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int seconds) {
        this.intervalSeconds = Math.max(1, seconds);
        restartTimer();
    }

    //  Countdown (for the UI)

    /** Seconds until the next autosave (0 when autosave is off). */
    public IntegerProperty secondsRemainingProperty() {
        return secondsRemaining;
    }

    /** Ready-made label text, e.g. "Next autosave in 0:42" or "Autosave off". */
    public StringProperty countdownTextProperty() {
        return countdownText;
    }

    /** Whether the user wants the countdown shown. */
    public BooleanProperty countdownVisibleProperty() {
        return countdownVisible;
    }

    /** Restarts the countdown from the full interval (no-op while off). */
    public void resetTimer() {
        if (enabled) {
            setRemaining(intervalSeconds);
        }
    }

    private void restartTimer() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
        if (enabled) {
            setRemaining(intervalSeconds);
            ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
            ticker.setCycleCount(Timeline.INDEFINITE);
            ticker.play();
        } else {
            setRemaining(0);
        }
    }

    private void tick() {
        int left = secondsRemaining.get() - 1;
        if (left <= 0) {
            boolean deferred = saveAllTabs();
            // If a tab was mid-drag, try again in a second instead of waiting
            // a whole interval.
            left = deferred ? 1 : intervalSeconds;
        }
        setRemaining(left);
    }

    private void setRemaining(int seconds) {
        secondsRemaining.set(seconds);
        if (enabled) {
            countdownText.set(String.format("Next autosave in %d:%02d",
                    seconds / 60, seconds % 60));
        } else {
            countdownText.set("Autosave off");
        }
    }

    //  Saving

    /** Saves every tab that needs it, now. Must be called on the JavaFX thread. */
    public void autosaveNow() {
        saveAllTabs();
        resetTimer();
    }

    /** @return true if some tab had to be skipped because it was mid-drag */
    private boolean saveAllTabs() {
        boolean deferred = false;
        for (Tab tab : tabs) {
            if (tab instanceof ImageTab) {
                deferred |= queueSave((ImageTab) tab);
            }
        }
        return deferred;
    }

    /** @return true if the tab was skipped only because the mouse is down */
    private boolean queueSave(ImageTab tab) {

        DrawingCanvas canvas = tab.getCanvas();

        if (!canvas.hasUnsavedChanges()) {
            return false;
        }
        if (canvas.isGestureInProgress()) {
            return true; // try again shortly
        }
        if (canvas.getCanvas().getWidth() <= 0
                || canvas.getCanvas().getHeight() <= 0) {
            return false;
        }

        final boolean hasRealFile = tab.getFile() != null;
        final File target;

        if (hasRealFile) {
            target = tab.getFile();
        } else {
            long changes = canvas.getChangeCount();
            Long already = lastBackedUp.get(canvas);
            if (already != null && already == changes) {
                return false; // nothing new since the last backup
            }
            lastBackedUp.put(canvas, changes);
            target = new File(RECOVERY_DIR, safeName(tab.getBaseName()) + ".png");
        }

        // Snapshot now, on the FX thread, so the saved image is a consistent
        // picture of this moment even if the user keeps drawing.
        final BufferedImage image = FileManager.snapshotToBufferedImage(canvas);

        // Clearing the flag *now* (not after the write) means any stroke the
        // user makes while the write is running correctly re-marks the tab.
        if (hasRealFile) {
            canvas.setUnsavedChanges(false);
        }

        writer.submit(() -> {
            try {
                if (!hasRealFile) {
                    RECOVERY_DIR.mkdirs();
                }
                FileManager.writeImage(image, target);

                String when = LocalTime.now().format(TIME_FORMAT);
                Platform.runLater(() -> onStatus.accept("Autosaved at " + when));

            } catch (IOException | RuntimeException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (hasRealFile) {
                        canvas.setUnsavedChanges(true); // it really isn't saved
                    } else {
                        lastBackedUp.remove(canvas);    // retry next tick
                    }
                    onStatus.accept("Autosave failed: " + ex.getMessage());
                });
            }
        });
        return false;
    }

    private static String safeName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Stops the timer and lets any in-flight write finish. Call on app exit. */
    public void shutdown() {
        if (ticker != null) {
            ticker.stop();
        }
        writer.shutdown();
        try {
            writer.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
