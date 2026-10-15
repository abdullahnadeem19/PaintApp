import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


public class FileManager {

    private static final String[] KNOWN_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".bmp", ".gif" };

    private final Stage stage;

    // The tab that File > Open/Save/Save As currently act on
    private ImageTab activeTab;

    // Runs after the user saves by hand (used to restart the autosave countdown)
    private Runnable onUserSave;

    public FileManager(Stage stage) {
        this.stage = stage;
    }

    public void setActiveTab(ImageTab tab) {
        this.activeTab = tab;
    }

    public void setOnUserSave(Runnable onUserSave) {
        this.onUserSave = onUserSave;
    }

    private void notifyUserSave() {
        if (onUserSave != null) {
            onUserSave.run();
        }
    }

    // Opens an image into the active tab
    public void openImage() {

        if (activeTab == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.bmp",
                        "*.gif"
                )
        );

        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {

            Image image = new Image(file.toURI().toString());

            activeTab.getCanvas().showImage(image);

            // Opening a file means there are no new changes
            activeTab.getCanvas().setUnsavedChanges(false);

            activeTab.setFile(file);
        }
    }

    // Saves the active tab's image
    public void save() {

        if (activeTab == null) {
            return;
        }

        if (activeTab.getFile() == null) {

            saveAs();

        } else {

            if (writeCanvasToFile(activeTab.getFile(), activeTab.getCanvas())) {

                // The current drawing has been saved
                activeTab.getCanvas().setUnsavedChanges(false);
                notifyUserSave();
            }
        }
    }

    // Saves the active tab's image to a new file. The tab now belongs to that
    // file (and its format), so later Save / autosave write to it.
    public void saveAs() {

        if (activeTab == null) {
            return;
        }

        File file = chooseSaveFile("Save Image As", false);

        if (file != null && writeCanvasToFile(file, activeTab.getCanvas())) {

            // The drawing has been saved
            activeTab.getCanvas().setUnsavedChanges(false);

            activeTab.setFile(file);
            notifyUserSave();
        }
    }

    // Saves a COPY in any supported format. The tab keeps its current file,
    // format and unsaved state, so you can convert without changing what
    // Save / autosave write to.
    public void exportCopy() {

        if (activeTab == null) {
            return;
        }

        File file = chooseSaveFile("Export Copy As", true);

        if (file != null) {
            writeCanvasToFile(file, activeTab.getCanvas());
        }
    }

    // File format helpers

    // "png", "jpg", "bmp" or "gif", based on a file name's extension
    // (unknown extensions are treated as png, matching the old behaviour)
    public static String formatOf(String fileName) {

        String name = fileName.toLowerCase();

        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpg";
        }
        if (name.endsWith(".bmp")) {
            return "bmp";
        }
        if (name.endsWith(".gif")) {
            return "gif";
        }
        return "png";
    }

    private static String formatLabel(String format) {
        switch (format) {
            case "jpg": return "JPEG";
            case "bmp": return "BMP";
            case "gif": return "GIF";
            default:    return "PNG";
        }
    }

    // What can be lost by saving into this format, or null if nothing.
    // PNG is lossless, so it never warns.
    public static String lossNotes(String targetFormat) {
        switch (targetFormat) {
            case "jpg":
                return "- JPEG is lossy: fine detail, sharp edges and exact colors are "
                     + "permanently degraded, and quality drops a little more each time "
                     + "it is re-saved.\n"
                     + "- JPEG has no transparency: transparent areas become white.";
            case "gif":
                return "- GIF holds only 256 colors: gradients and subtle shades are "
                     + "flattened.\n"
                     + "- GIF output here has no transparency: transparent areas become white.";
            case "bmp":
                return "- BMP has no transparency: transparent areas become white.\n"
                     + "- BMP is uncompressed, so the file will be much larger.";
            default:
                return null;
        }
    }

    // Shows a file chooser (PNG / JPEG / BMP / GIF), makes sure the name has
    // a real extension, and warns about data loss if the format is changing.
    // Returns null if the user cancels.
    private File chooseSaveFile(String title, boolean isExport) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        FileChooser.ExtensionFilter png = new FileChooser.ExtensionFilter(
                "PNG - lossless (*.png)", "*.png");
        FileChooser.ExtensionFilter jpeg = new FileChooser.ExtensionFilter(
                "JPEG - lossy (*.jpg, *.jpeg)", "*.jpg", "*.jpeg");
        FileChooser.ExtensionFilter bmp = new FileChooser.ExtensionFilter(
                "BMP (*.bmp)", "*.bmp");
        FileChooser.ExtensionFilter gif = new FileChooser.ExtensionFilter(
                "GIF - 256 colors (*.gif)", "*.gif");

        fileChooser.getExtensionFilters().addAll(png, jpeg, bmp, gif);

        // Start on the image's current format and folder
        File current = activeTab.getFile();
        String currentFormat = (current == null) ? null : formatOf(current.getName());

        if ("jpg".equals(currentFormat)) {
            fileChooser.setSelectedExtensionFilter(jpeg);
        } else if ("bmp".equals(currentFormat)) {
            fileChooser.setSelectedExtensionFilter(bmp);
        } else if ("gif".equals(currentFormat)) {
            fileChooser.setSelectedExtensionFilter(gif);
        } else {
            fileChooser.setSelectedExtensionFilter(png);
        }

        if (current != null && current.getParentFile() != null
                && current.getParentFile().isDirectory()) {
            fileChooser.setInitialDirectory(current.getParentFile());
        }

        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return null;
        }

        // No recognised extension typed: use the one from the chosen filter
        String lower = file.getName().toLowerCase();
        boolean hasExtension = false;
        for (String ext : KNOWN_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                hasExtension = true;
            }
        }

        if (!hasExtension) {
            FileChooser.ExtensionFilter chosen = fileChooser.getSelectedExtensionFilter();
            String ext = (chosen == null) ? ".png"
                    : chosen.getExtensions().get(0).substring(1);   // "*.jpg" -> ".jpg"

            file = new File(file.getParentFile(), file.getName() + ext);
        }

        if (!confirmFormatChange(currentFormat, formatOf(file.getName()), isExport)) {
            return null;
        }

        return file;
    }

    // Warns when saving into a different format that can lose information.
    // Returns true to go ahead, false if the user backs out.
    private boolean confirmFormatChange(String fromFormat, String toFormat, boolean isExport) {

        // Same format as the image already has: no conversion happening
        if (toFormat.equals(fromFormat)) {
            return true;
        }

        String notes = lossNotes(toFormat);

        if (notes == null) {
            return true;
        }

        StringBuilder text = new StringBuilder();

        if (fromFormat != null) {
            text.append("This image is currently ")
                .append(formatLabel(fromFormat)).append(".\n\n");
        }

        text.append("Converting to ").append(formatLabel(toFormat))
            .append(" can lose image features or data:\n\n")
            .append(notes);

        if (!isExport) {
            text.append("\n\nTip: File > Export Copy As saves a copy in this format and "
                      + "leaves your current file untouched.");
        }

        ButtonType saveAnyway = new ButtonType("Save Anyway", ButtonBar.ButtonData.OK_DONE);

        Alert alert = new Alert(Alert.AlertType.WARNING, text.toString(),
                saveAnyway, ButtonType.CANCEL);
        alert.setTitle("Image Format Warning");
        alert.setHeaderText("Saving as " + formatLabel(toFormat) + " may lose data");

        return alert.showAndWait().orElse(ButtonType.CANCEL) == saveAnyway;
    }

    // Takes a snapshot of the canvas as a BufferedImage.
    // Must be called on the JavaFX thread.
    public static BufferedImage snapshotToBufferedImage(DrawingCanvas drawing) {

        WritableImage snapshot =
                drawing.getCanvas().snapshot(
                        new SnapshotParameters(),
                        null
                );

        return fxImageToBufferedImage(snapshot);
    }

    // Converts image to BufferedImage
    private static BufferedImage fxImageToBufferedImage(
            WritableImage fxImage) {

        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();

        BufferedImage bufferedImage =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_ARGB
                );

        PixelReader pixelReader =
                fxImage.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int argb = pixelReader.getArgb(x, y);

                bufferedImage.setRGB(
                        x,
                        y,
                        argb
                );
            }
        }

        return bufferedImage;
    }

    // Encodes an image to disk. The format comes from the file extension
    // (PNG, JPEG, BMP or GIF). Touches no JavaFX objects, so it is safe to
    // call from any thread.
    public static void writeImage(BufferedImage bufferedImage, File file)
            throws IOException {

        String format = formatOf(file.getName());

        if (!format.equals("png")) {

            // JPEG, BMP and GIF are written without transparency,
            // so flatten onto white first
            BufferedImage rgbImage =
                    new BufferedImage(
                            bufferedImage.getWidth(),
                            bufferedImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB
                    );

            Graphics2D g2d = rgbImage.createGraphics();

            g2d.drawImage(
                    bufferedImage,
                    0,
                    0,
                    java.awt.Color.WHITE,
                    null
            );

            g2d.dispose();

            bufferedImage = rgbImage;
        }

        if (!ImageIO.write(bufferedImage, format, file)) {
            throw new IOException("No image writer available for " + format);
        }
    }

    // Writes the given canvas to a file (with an error dialog on failure).
    // Returns true if the file was written.
    private boolean writeCanvasToFile(File file, DrawingCanvas drawing) {

        try {

            writeImage(snapshotToBufferedImage(drawing), file);
            return true;

        } catch (IOException e) {

            e.printStackTrace();

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Could not save the image:\n"
                            + e.getMessage()
            );

            alert.showAndWait();
            return false;
        }
    }
}
