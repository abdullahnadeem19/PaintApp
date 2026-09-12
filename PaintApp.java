package org.example;

import javafx.application.Application;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class PaintApp extends Application {

    // Stores the current file
    private File currentFile;

    // Canvas for drawing
    private Canvas canvas;

    // Used to draw on the canvas
    private GraphicsContext gc;

    @Override
    public void start(Stage stage) {

        // Create the canvas
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();


        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Set brush size
        gc.setLineWidth(5);

        // Draw when the mouse is dragged
        canvas.setOnMouseDragged(event -> {
            gc.strokeLine(event.getX(), event.getY(),
                    event.getX(), event.getY());
        });

        // Print a message when the mouse is pressed
        canvas.setOnMousePressed(event -> {
            System.out.println("Mouse is pressed.");
        });

        // Add canvas to a pane
        Pane pane = new Pane();
        pane.getChildren().add(canvas);

        // Create the menu bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");

        // Open menu item
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> openImage(stage));

        // Save menu item
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> save(stage));

        // Save As menu item
        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setOnAction(e -> saveAs(stage));

        // Close menu item
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> stage.close());

        // Add items to the File menu
        fileMenu.getItems().addAll(
                openItem,
                saveItem,
                saveAsItem,
                new SeparatorMenuItem(),
                closeItem
        );

        // Add File menu to the menu bar
        menuBar.getMenus().add(fileMenu);


        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(pane);

        Scene scene = new Scene(root, 800, 630);

        // Set the window title
        stage.setTitle("Paint Application");

        // Add the scene to the window
        stage.setScene(scene);


        stage.show();
    }

    // Opens an image
    private void openImage(Stage stage) {

        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");

        // Allow image files
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // Let the user choose a file
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {


            Image image = new Image(file.toURI().toString());


            gc.clearRect(0, 0,
                    canvas.getWidth(), canvas.getHeight());


            gc.drawImage(image, 0, 0,
                    canvas.getWidth(), canvas.getHeight());


            currentFile = file;


            stage.setTitle("Paint Application - " + file.getName());
        }
    }

    // Saves the current image
    private void save(Stage stage) {

        // If there is no file, use Save As
        if (currentFile == null) {
            saveAs(stage);
        } else {
            writeCanvasToFile(currentFile);
        }
    }

    // Saves the image to a new file
    private void saveAs(Stage stage) {

        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image As");

        // Allow PNG and JPEG files
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg")
        );

        // Ask the user where to save
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


            writeCanvasToFile(file);


            currentFile = file;


            stage.setTitle(
                    "Paint Application - " + file.getName()
            );
        }
    }


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


        PixelReader pixelReader = fxImage.getPixelReader();


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int argb = pixelReader.getArgb(x, y);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        return bufferedImage;
    }


    private void writeCanvasToFile(File file) {

        try {


            WritableImage snapshot =
                    canvas.snapshot(
                            new SnapshotParameters(),
                            null
                    );
            
            BufferedImage bufferedImage =
                    fxImageToBufferedImage(snapshot);

            String name = file.getName().toLowerCase();

            boolean isJpeg =
                    name.endsWith(".jpg")
                            || name.endsWith(".jpeg");


            String format = isJpeg ? "jpg" : "png";

            if (isJpeg) {

                BufferedImage rgbImage =
                        new BufferedImage(
                                bufferedImage.getWidth(),
                                bufferedImage.getHeight(),
                                BufferedImage.TYPE_INT_RGB
                        );


                rgbImage.createGraphics().drawImage(
                        bufferedImage,
                        0,
                        0,
                        java.awt.Color.WHITE,
                        null
                );

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


    public static void main(String[] args) {
        launch();
    }
}