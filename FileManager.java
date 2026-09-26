import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
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

    private final Stage stage;

    // The tab that File > Open/Save/Save As currently act on
    private ImageTab activeTab;

    public FileManager(Stage stage) {
        this.stage = stage;
    }

    public void setActiveTab(ImageTab tab) {
        this.activeTab = tab;
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
                        "*.jpeg"
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

            writeCanvasToFile(activeTab.getFile(), activeTab.getCanvas());

            // The current drawing has been saved
            activeTab.getCanvas().setUnsavedChanges(false);
        }
    }

    // Saves the active tab's image to a new file
    public void saveAs() {

        if (activeTab == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image As");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "PNG Files",
                        "*.png"
                ),
                new FileChooser.ExtensionFilter(
                        "JPEG Files",
                        "*.jpg"
                )
        );

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {

            String name = file.getName().toLowerCase();

            if (!name.endsWith(".png")
                    && !name.endsWith(".jpg")
                    && !name.endsWith(".jpeg")) {

                file = new File(
                        file.getParentFile(),
                        file.getName() + ".png"
                );
            }

            writeCanvasToFile(file, activeTab.getCanvas());

            // The drawing has been saved
            activeTab.getCanvas().setUnsavedChanges(false);

            activeTab.setFile(file);
        }
    }

    // Converts image to BufferedImage
    private BufferedImage fxImageToBufferedImage(
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

    // Writes the given canvas to a file
    private void writeCanvasToFile(File file, DrawingCanvas drawing) {

        try {

            WritableImage snapshot =
                    drawing.getCanvas().snapshot(
                            new SnapshotParameters(),
                            null
                    );

            BufferedImage bufferedImage =
                    fxImageToBufferedImage(snapshot);

            String name =
                    file.getName().toLowerCase();

            boolean isJpeg =
                    name.endsWith(".jpg")
                            || name.endsWith(".jpeg");

            String format =
                    isJpeg ? "jpg" : "png";

            if (isJpeg) {

                // JPEG
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

            ImageIO.write(
                    bufferedImage,
                    format,
                    file
            );

        } catch (IOException e) {

            e.printStackTrace();

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Could not save the image:\n"
                            + e.getMessage()
            );

            alert.showAndWait();
        }
    }
}
