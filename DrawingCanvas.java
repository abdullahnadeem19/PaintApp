import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;


public class DrawingCanvas extends StackPane {

    // Canvas for drawing
    private final Canvas canvas = new Canvas();

    // Used to draw on the canvas
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    // Keeps track of whether the drawing was changed
    private boolean unsavedChanges = false;

    public DrawingCanvas() {

        // Make the canvas resize with the window
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // Add canvas to drawing area
        getChildren().add(canvas);

        // Set brush size
        gc.setLineWidth(12);

        // Set default drawing color
        gc.setStroke(Color.BLACK);

        // Set white background
        gc.setFill(Color.WHITE);

        // Draw when mouse is dragged
        canvas.setOnMouseDragged(event -> {

            gc.strokeLine(
                    event.getX(),
                    event.getY(),
                    event.getX(),
                    event.getY()
            );

            // The drawing has been changed
            unsavedChanges = true;
        });

        // Print message when mouse is pressed
        canvas.setOnMousePressed(event ->
                System.out.println("Mouse is pressed.")
        );
    }

    // ---------- Brush settings ----------

    public void setBrushColor(Color color) {
        gc.setStroke(color);
    }

    public void setBrushWidth(double width) {
        gc.setLineWidth(width);
    }

    // ---------- Canvas access ----------

    public Canvas getCanvas() {
        return canvas;
    }

    // Clears the canvas and draws an image stretched to fit it
    public void showImage(Image image) {

        gc.clearRect(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );

        gc.drawImage(
                image,
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );
    }

    // ---------- Unsaved changes ----------

    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    public void setUnsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;
    }
}
