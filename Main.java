import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private TabPane tabPane;
    private FileManager fileManager;
    private AppMenuBar menuBar;
    private DrawingToolBar toolBar;

    private int tabCount = 0;

    @Override
    public void start(Stage stage) {

        //  main layout
        BorderPane root = new BorderPane();


        tabPane = new TabPane();

        fileManager = new FileManager(stage);

        // toolbar
        toolBar = new DrawingToolBar();

        //  menu bar
        menuBar = new AppMenuBar(
                fileManager,
                () -> closeApplication(stage),
                this::createNewTab
        );

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab instanceof ImageTab) {
                ImageTab imageTab = (ImageTab) newTab;
                menuBar.setActiveCanvas(imageTab.getCanvas());
                toolBar.setActiveCanvas(imageTab.getCanvas());
                fileManager.setActiveTab(imageTab);
            }
        });

        // Start with one blank tab
        createNewTab();


        VBox topBar = new VBox(menuBar, toolBar);

        // Add top bar and tabbed drawing area
        root.setTop(topBar);
        root.setCenter(tabPane);

        // Create scene
        Scene scene = new Scene(root, 900, 650);

        // Set title
        stage.setTitle("Paint Application");

        // Set scene
        stage.setScene(scene);


        stage.setOnCloseRequest(event -> {

            if (!confirmCloseAllTabs()) {
                event.consume();
            }
        });

        // Show window
        stage.show();
    }


    private void createNewTab() {

        tabCount++;

        DrawingCanvas canvas = new DrawingCanvas();

        // eyedropper
        canvas.setOnColorPicked(menuBar::updateColorDisplay);

        ImageTab tab = new ImageTab("Untitled " + tabCount, canvas);


        tab.setOnCloseRequest(event -> {
            if (!confirmCloseCanvas(canvas)) {
                event.consume();
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    // CLOSE APPLICATION
    private void closeApplication(Stage stage) {


        if (confirmCloseAllTabs()) {
            stage.close();
        }
    }


    private boolean confirmCloseAllTabs() {

        boolean anyUnsaved = false;

        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof ImageTab && ((ImageTab) tab).getCanvas().hasUnsavedChanges()) {
                anyUnsaved = true;
                break;
            }
        }

        if (!anyUnsaved) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You are about to close without saving.");
        alert.setContentText("One or more tabs have unsaved changes. Close anyway?");

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

        return result == ButtonType.OK;
    }


    private boolean confirmCloseCanvas(DrawingCanvas canvas) {

        if (!canvas.hasUnsavedChanges()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You are about to close this tab without saving.");
        alert.setContentText("Do you want to close it anyway?");

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

        return result == ButtonType.OK;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
