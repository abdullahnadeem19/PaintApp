import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/** Best-effort native system-tray alerts; does not fail when unavailable. */
public final class DesktopNotifications {
    private static TrayIcon icon;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private DesktopNotifications() { }
    public static void notify(String heading, String detail) {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) return;
        try {
            if (initialized.compareAndSet(false, true)) {
                BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.setColor(java.awt.Color.BLUE);
                g.fillOval(1, 1, 14, 14);
                g.dispose();
                icon = new TrayIcon(image, "Paint App");
                icon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(icon);
            }
            if (icon != null) icon.displayMessage(heading, detail, TrayIcon.MessageType.INFO);
        } catch (AWTException | SecurityException ignored) { /* OS may prohibit tray messages. */ }
    }
    public static void shutdown() {
        if (icon != null && SystemTray.isSupported()) SystemTray.getSystemTray().remove(icon);
    }
}
