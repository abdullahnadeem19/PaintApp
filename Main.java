import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class Main extends Application {

    private TabPane tabPane;
    private FileManager fileManager;
    private AppMenuBar menuBar;
    private DrawingToolBar toolBar;
    private AutoSaver autoSaver;
    private WebImageServer webServer;

    private int tabCount = 0;

    @Override
    public void start(Stage stage) {

        //  main layout
        BorderPane root = new BorderPane();


        tabPane = new TabPane();

        fileManager = new FileManager(stage);

        // Status bar (bottom): autosave message, web server status, countdown
        Label autosaveStatusLabel = new Label("");
        Label webStatusLabel = new Label();
        Label countdownLabel = new Label();

        autoSaver = new AutoSaver(
                tabPane.getTabs(),
                autosaveStatusLabel::setText
        );

        // Saving by hand restarts the autosave countdown
        fileManager.setOnUserSave(autoSaver::resetTimer);

        webServer = new WebImageServer(tabPane.getTabs());
        webStatusLabel.textProperty().bind(webServer.statusProperty());

        // The countdown only takes up room when the user has it switched on
        countdownLabel.textProperty().bind(autoSaver.countdownTextProperty());
        countdownLabel.visibleProperty().bind(autoSaver.countdownVisibleProperty());
        countdownLabel.managedProperty().bind(autoSaver.countdownVisibleProperty());

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        HBox statusBar = new HBox(16, autosaveStatusLabel, statusSpacer, webStatusLabel, countdownLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));

        // toolbar
        toolBar = new DrawingToolBar();

        //  menu bar
        menuBar = new AppMenuBar(
                fileManager,
                () -> closeApplication(stage),
                this::createNewTab,
                autoSaver,
                webServer,
                () -> getHostServices().showDocument(webServer.getUrl())
        );

        // Autosave is on by default; the Autosave menu can turn it off
        autoSaver.setEnabled(true);

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


        // The toolbar has many buttons; let it scroll sideways instead of
        // squeezing every button down to "..." in a narrow window
        ScrollPane toolScroll = new ScrollPane(toolBar);
        toolScroll.setFitToHeight(true);
        toolScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        toolScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        toolScroll.setStyle("-fx-background-insets: 0; -fx-padding: 0; -fx-background-color: transparent;");

        VBox topBar = new VBox(menuBar, toolScroll);

        // Add top bar and tabbed drawing area
        root.setTop(topBar);
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        // Create scene
        Scene scene = new Scene(root, 900, 650);



        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                () -> {
                    if (tabPane.getSelectionModel().getSelectedItem() instanceof ImageTab) {
                        ((ImageTab) tabPane.getSelectionModel().getSelectedItem()).getCanvas().undo();
                    }
                }
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN),
                () -> {
                    if (tabPane.getSelectionModel().getSelectedItem() instanceof ImageTab) {
                        ((ImageTab) tabPane.getSelectionModel().getSelectedItem()).getCanvas().redo();
                    }
                }
        );
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


    @Override
    public void stop() {
        // Stop the web server, the timer, and let any in-flight autosave finish
        autoSaver.shutdown();
        webServer.stop();
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