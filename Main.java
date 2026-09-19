import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    private DrawingCanvas drawing;

    @Override
    public void start(Stage stage) {

        // Create the main layout
        BorderPane root = new BorderPane();

        // Create the drawing area (canvas + brush + unsaved-changes flag)
        drawing = new DrawingCanvas();

        // Create the file manager (open / save / save as)
        FileManager fileManager = new FileManager(stage, drawing);

        // Create the menu bar
        AppMenuBar menuBar = new AppMenuBar(
                drawing,
                fileManager,
                () -> closeApplication(stage)
        );

        // Add menu bar and drawing area
        root.setTop(menuBar);
        root.setCenter(drawing);

        // Create scene
        Scene scene = new Scene(root, 800, 600);

        // Set title
        stage.setTitle("Paint Application");

        // Set scene
        stage.setScene(scene);

        // WARN WHEN WINDOW IS CLOSED WITH UNSAVED CHANGES
        stage.setOnCloseRequest(event -> {

            // Cancel closing if user does not choose OK
            if (!confirmClose()) {
                event.consume();
            }
        });

        // Show window
        stage.show();
    }

    // CLOSE APPLICATION
    private void closeApplication(Stage stage) {

        // Close only if there are no unsaved changes or the user confirms
        if (confirmClose()) {
            stage.close();
        }
    }

    // Returns true if it is OK to close (no changes, or user confirmed)
    private boolean confirmClose() {

        if (!drawing.hasUnsavedChanges()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You are about to close without saving.");
        alert.setContentText("Do you want to close anyway?");

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

        return result == ButtonType.OK;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
