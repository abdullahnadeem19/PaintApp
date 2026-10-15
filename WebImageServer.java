import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * A small web server (the JDK's built-in com.sun.net.httpserver) that shows
 * the images of the tabs the user has chosen to share.
 *
 *   /            a page listing every shared image (refreshes itself)
 *   /image/{id}  the live picture of one shared tab, as a PNG
 *
 * If no tab is shared, the page says so instead of showing images, and the
 * status bar text says the same. Images are read-only and always current: each
 * request takes a fresh snapshot of the canvas.
 *
 * Threading: HTTP requests arrive on the server's own threads, but tabs and
 * canvases may only be touched on the JavaFX thread, so every request hops
 * over to it (see onFx) for the moment it needs to look at them.
 *
 * By default the server only answers to this computer (localhost). The
 * "allow network" option opens it to other devices on the same network.
 */
public class WebImageServer {

    public static final int PREFERRED_PORT = 8080;

    private final ObservableList<Tab> tabs;

    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final StringProperty status = new SimpleStringProperty("Web server: off");

    private HttpServer server;
    private ExecutorService pool;
    private boolean allowNetwork = false;
    private int port;
    private String lastError = "";

    public WebImageServer(ObservableList<Tab> tabs) {
        this.tabs = tabs;

        // Keep the status text right as tabs open/close
        tabs.addListener((javafx.collections.ListChangeListener<Tab>) change -> refreshStatus());
    }

    //  State for the UI

    public ObservableList<Tab> getTabs() {
        return tabs;
    }

    public ReadOnlyBooleanProperty runningProperty() {
        return running;
    }

    public boolean isRunning() {
        return running.get();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isAllowNetwork() {
        return allowNetwork;
    }

    /** Opens/closes the server to other devices on the network. */
    public void setAllowNetwork(boolean allow) {
        if (this.allowNetwork == allow) {
            return;
        }
        this.allowNetwork = allow;
        if (isRunning()) {
            stop();
            start();
        }
    }

    /** Address to type into a browser. */
    public String getUrl() {
        String host = allowNetwork ? lanAddress() : "localhost";
        return "http://" + host + ":" + port + "/";
    }

    private int sharedCount() {
        int count = 0;
        for (Tab tab : tabs) {
            if (tab instanceof ImageTab && ((ImageTab) tab).isShared()) {
                count++;
            }
        }
        return count;
    }

    /** Recomputes the status-bar text. Call on the JavaFX thread. */
    public void refreshStatus() {
        if (!isRunning()) {
            status.set("Web server: off");
            return;
        }
        int count = sharedCount();
        if (count == 0) {
            status.set("Web server: " + getUrl() + "  (no images selected to share)");
        } else {
            status.set("Web server: " + getUrl() + "  (" + count
                    + (count == 1 ? " image" : " images") + " shared)");
        }
    }

    //  Start / stop

    /** @return true if the server is now running */
    public boolean start() {

        if (isRunning()) {
            return true;
        }

        try {
            InetSocketAddress address = bindAddress(PREFERRED_PORT);

            try {
                server = HttpServer.create(address, 0);
            } catch (BindException busy) {
                // 8080 is taken: let the system pick a free port
                server = HttpServer.create(bindAddress(0), 0);
            }

            port = server.getAddress().getPort();

            pool = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "web-image-server");
                t.setDaemon(true);
                return t;
            });

            server.createContext("/", this::handle);
            server.setExecutor(pool);
            server.start();

            lastError = "";
            running.set(true);
            refreshStatus();
            return true;

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            lastError = String.valueOf(e.getMessage());
            server = null;
            running.set(false);
            status.set("Web server: could not start (" + lastError + ")");
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        running.set(false);
        refreshStatus();
    }

    private InetSocketAddress bindAddress(int portNumber) {
        return allowNetwork
                ? new InetSocketAddress(portNumber)                              // all interfaces
                : new InetSocketAddress(InetAddress.getLoopbackAddress(), portNumber);
    }

    // First private IPv4 address of this machine
    private static String lanAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
            // fall through
        }
        return "localhost";
    }

    //  Handling requests (runs on the server's threads)

    private void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();

            if (!method.equals("GET") && !method.equals("HEAD")) {
                sendText(ex, 405, "Method not allowed");
                return;
            }

            String path = ex.getRequestURI().getPath();

            if (path.equals("/") || path.equals("/index.html")) {
                sendIndex(ex);
            } else if (path.startsWith("/image/")) {
                sendImage(ex, path.substring("/image/".length()));
            } else {
                sendText(ex, 404, "Not found");
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendText(ex, 500, "Server error");
            } catch (IOException ignored) {
                // client went away
            }
        } finally {
            ex.close();
        }
    }

    private void sendIndex(HttpExchange ex) throws Exception {

        // Ids and names of the shared tabs, read on the FX thread
        List<String[]> shared = onFx(() -> {
            List<String[]> list = new ArrayList<>();
            for (Tab tab : tabs) {
                if (tab instanceof ImageTab && ((ImageTab) tab).isShared()) {
                    ImageTab it = (ImageTab) tab;
                    list.add(new String[] { String.valueOf(it.getImageId()), it.getBaseName() });
                }
            }
            return list;
        });

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            .append("<meta http-equiv=\"refresh\" content=\"5\">")
            .append("<title>Paint Application - Shared Images</title>")
            .append("<style>")
            .append("body{font-family:sans-serif;margin:2rem;background:#f4f4f4;color:#222}")
            .append("figure{display:inline-block;margin:0 1rem 1rem 0;padding:.5rem;")
            .append("background:#fff;border:1px solid #ccc;vertical-align:top}")
            .append("img{max-width:100%;height:auto;display:block}")
            .append("figcaption{margin-top:.4rem;font-weight:bold}")
            .append(".empty{padding:1.5rem;background:#fff;border:1px dashed #999;max-width:36rem}")
            .append("</style></head><body><h1>Shared images</h1>");

        if (shared.isEmpty()) {
            html.append("<div class=\"empty\"><p><b>No images are being shared right now.</b></p>")
                .append("<p>In the Paint Application, choose <i>Web Server &rarr; ")
                .append("Choose Images to Share...</i> and tick one or more tabs. ")
                .append("This page refreshes by itself.</p></div>");
        } else {
            long stamp = System.currentTimeMillis(); // defeats browser caching
            for (String[] item : shared) {
                String name = escape(item[1]);
                html.append("<figure><a href=\"/image/").append(item[0]).append("\">")
                    .append("<img src=\"/image/").append(item[0]).append("?t=").append(stamp)
                    .append("\" alt=\"").append(name).append("\"></a>")
                    .append("<figcaption>").append(name).append("</figcaption></figure>");
            }
            html.append("<p><small></small></p>");
        }

        html.append("</body></html>");

        send(ex, 200, "text/html; charset=utf-8", html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void sendImage(HttpExchange ex, String idText) throws Exception {

        final int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            sendText(ex, 404, "Not found");
            return;
        }

        // Look the tab up and snapshot its canvas on the FX thread.
        // Returns null if there is no such tab or it isn't shared.
        BufferedImage image = onFx(() -> {
            for (Tab tab : tabs) {
                if (tab instanceof ImageTab) {
                    ImageTab it = (ImageTab) tab;
                    if (it.getImageId() == id && it.isShared()
                            && it.getCanvas().getCanvas().getWidth() > 0
                            && it.getCanvas().getCanvas().getHeight() > 0) {
                        return FileManager.snapshotToBufferedImage(it.getCanvas());
                    }
                }
            }
            return null;
        });

        if (image == null) {
            sendText(ex, 404, "That image is not being shared.");
            return;
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);

        send(ex, 200, "image/png", png.toByteArray());
    }

    private void sendText(HttpExchange ex, int code, String text) throws IOException {
        send(ex, code, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");

        if (ex.getRequestMethod().equals("HEAD")) {
            ex.sendResponseHeaders(code, -1);
            return;
        }

        ex.sendResponseHeaders(code, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    // Runs a job on the JavaFX thread and waits (up to 5 s) for its result
    private static <T> T onFx(Callable<T> job) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return job.call();
        }
        FutureTask<T> task = new FutureTask<>(job);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                   .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
