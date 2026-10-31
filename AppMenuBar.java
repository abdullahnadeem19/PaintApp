
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Creates the menu bar for the Paint Application.
 * It includes menus for file operations, color selection,
 * line width, and help.
 */
public class AppMenuBar extends MenuBar {

    // The canvas that the color picker / width slider currently control
    private DrawingCanvas activeCanvas;

    private final ColorPicker colorPicker = new ColorPicker(Color.BLACK);
    private final Label colorInfoLabel = new Label();
    private final Slider lineWidthSlider = new Slider(1, 50, 12);

    /**
     * Creates the menu bar and adds all the available menus.
     * Sets the starting drawing color to black.
     *
     * @param fileManager handles opening and saving images
     * @param onClose runs when the application is closed
     * @param onNewTab runs when a new tab is created
     * @param autoSaver the autosave timer the Autosave menu controls
     * @param webServer the image web server the Web Server menu controls
     * @param onOpenBrowser opens the web server's address in a browser
     */
    public AppMenuBar(FileManager fileManager,
                      Runnable onClose,
                      Runnable onNewTab,
                      AutoSaver autoSaver,
                      WebImageServer webServer,
                      Runnable onOpenBrowser) {

        updateColorLabel(Color.BLACK);

        getMenus().addAll(  // Add menu tab
                createFileMenu(fileManager, onClose, onNewTab),
                createColorMenu(),
                createLineWidthMenu(),
                createTransformMenu(),
                createAutosaveMenu(autoSaver),
                createWebServerMenu(webServer, onOpenBrowser),
                createHelpMenu()
        );
    }

    /**
     * Sets the canvas that the menu bar will control.
     * Updates the color picker and line width slider
     * to match the selected canvas.
     *
     * @param canvas the canvas currently selected by the user
     */
    public void setActiveCanvas(DrawingCanvas canvas) {

        this.activeCanvas = canvas;

        colorPicker.setValue(canvas.getBrushColor());
        updateColorLabel(canvas.getBrushColor());

        lineWidthSlider.setValue(canvas.getBrushWidth());
    }

    /**
     * Changes the color shown in the color picker and label.
     * This is used when another tool, such as the eyedropper,
     * selects a new color.
     *
     * @param color the color to display
     */
    public void updateColorDisplay(Color color) {
        colorPicker.setValue(color);
        updateColorLabel(color);
    }

    /**
     * Displays the selected color's hex code, RGB values,
     * and closest English color name in the label.
     *
     * @param color the selected drawing color
     */
    private void updateColorLabel(Color color) {
        colorInfoLabel.setText(
                ColorUtils.toHex(color)
                        + "   " + ColorUtils.toRgbString(color)
                        + "   (" + ColorUtils.closestColorName(color) + ")"
        );
    }

    /**
     * Creates the Color tab menu with a color picker
     * and a label showing information about the selected color.
     * When a color is selected, it updates the active canvas.
     *
     * @return the color selection menu
     */
    private Menu createColorMenu() {

        Menu colorMenu = new Menu("Color tab");

        // Change the drawing color when a color is selected
        colorPicker.setOnAction(event -> {
            Color chosen = colorPicker.getValue();
            if (activeCanvas != null) {
                activeCanvas.setBrushColor(chosen);
            }
            updateColorLabel(chosen);
        });

        // Shows the hex code, rgb values, and closest English color name
        VBox box = new VBox(6, colorPicker, colorInfoLabel);
        box.setPadding(new Insets(6));

        // The color picker (+ label) inside the Color tab
        CustomMenuItem colorItem = new CustomMenuItem(box);

        // Keep the color menu open when selecting a color
        colorItem.setHideOnClick(false);

        colorMenu.getItems().add(colorItem);

        return colorMenu;
    }

    /**
     * Creates the Line Width menu with a slider.
     * The slider lets the user change the brush size
     * from 1 to 50 and applies the new width to the active canvas.
     * @return the line width menu
     */
    private Menu createLineWidthMenu() {

        // Show numbers/ticks on the slider
        lineWidthSlider.setShowTickLabels(true);
        lineWidthSlider.setShowTickMarks(true);

        // Change brush size when slider moves
        lineWidthSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (activeCanvas != null) {
                        activeCanvas.setBrushWidth(newValue.doubleValue());
                    }
                }
        );

        // Put slider inside a menu item
        CustomMenuItem lineWidthItem = new CustomMenuItem(lineWidthSlider);

        // Keep the menu open while changing the size
        lineWidthItem.setHideOnClick(false);

        // Create line width menu
        Menu lineMenu = new Menu("Line Width");
        lineMenu.getItems().add(lineWidthItem);

        return lineMenu;
    }

    /**
     * Creates the Autosave menu: an on/off checkbox, a choice of
     * interval, and an "Autosave Now" command.
     */
    private Menu createAutosaveMenu(AutoSaver autoSaver) {

        Menu autosaveMenu = new Menu("Autosave");

        CheckMenuItem enabledItem = new CheckMenuItem("Enable autosave");
        enabledItem.setSelected(autoSaver.isEnabled());
        enabledItem.setOnAction(e -> { autoSaver.setEnabled(enabledItem.isSelected()); ActivityLogger.log("Autosave " + (enabledItem.isSelected() ? "enabled" : "disabled")); DesktopNotifications.notify("Paint autosave", enabledItem.isSelected() ? "Autosave enabled" : "Autosave disabled"); });

        // Show / hide the "Next autosave in 0:42" countdown in the status bar
        CheckMenuItem countdownItem = new CheckMenuItem("Show countdown");
        countdownItem.selectedProperty().bindBidirectional(autoSaver.countdownVisibleProperty());

        autosaveMenu.getItems().addAll(enabledItem, countdownItem, new SeparatorMenuItem());

        ToggleGroup intervalGroup = new ToggleGroup();
        int[] seconds = { 15, 30, 60, 120, 300 };
        String[] labels = { "Every 15 seconds", "Every 30 seconds", "Every minute",
                            "Every 2 minutes", "Every 5 minutes" };

        for (int i = 0; i < seconds.length; i++) {
            final int secs = seconds[i];
            RadioMenuItem item = new RadioMenuItem(labels[i]);
            item.setToggleGroup(intervalGroup);
            item.setSelected(secs == autoSaver.getIntervalSeconds());
            item.setOnAction(e -> autoSaver.setIntervalSeconds(secs));
            autosaveMenu.getItems().add(item);
        }

        MenuItem nowItem = new MenuItem("Autosave Now");
        nowItem.setOnAction(e -> autoSaver.autosaveNow());

        autosaveMenu.getItems().addAll(new SeparatorMenuItem(), nowItem);

        return autosaveMenu;
    }

    /**
     * Creates the Web Server menu: start/stop, who may connect, which images
     * are shared, and a shortcut to open the page in a browser.
     */
    private Menu createWebServerMenu(WebImageServer webServer, Runnable onOpenBrowser) {

        Menu webMenu = new Menu("Web Server");

        CheckMenuItem runItem = new CheckMenuItem("Run web server");
        runItem.setOnAction(e -> {
            if (runItem.isSelected()) {
                if (!webServer.start()) {
                    runItem.setSelected(false);
                    new Alert(Alert.AlertType.ERROR,
                            "Could not start the web server:\n" + webServer.getLastError()).showAndWait();
                }
            } else {
                webServer.stop();
            }
        });
        webServer.runningProperty().addListener((obs, was, now) -> runItem.setSelected(now));

        CheckMenuItem networkItem = new CheckMenuItem("Allow other devices on my network");
        networkItem.setOnAction(e -> webServer.setAllowNetwork(networkItem.isSelected()));

        MenuItem chooseItem = new MenuItem("Choose Images to Share...");
        chooseItem.setOnAction(e ->
                ShareImagesDialog.show(webServer.getTabs(), webServer::refreshStatus));

        MenuItem openItem = new MenuItem("Open in Browser");
        openItem.disableProperty().bind(webServer.runningProperty().not());
        openItem.setOnAction(e -> onOpenBrowser.run());

        webMenu.getItems().addAll(
                runItem,
                networkItem,
                new SeparatorMenuItem(),
                chooseItem,
                openItem
        );

        return webMenu;
    }

    /**
     * Creates the File menu with options to create a new tab,
     * open an image, save, save as, and close the application.
     * Each menu item also has a keyboard shortcut.
     */
    private Menu createFileMenu(FileManager fileManager,
                                Runnable onClose,
                                Runnable onNewTab) {

        Menu fileMenu = new Menu("File");

        // New Tab
        MenuItem newTabItem = new MenuItem("New Tab");
        newTabItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        newTabItem.setOnAction(e -> onNewTab.run());

        // Open
        MenuItem openItem = new MenuItem("Open");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> fileManager.openImage());

        // Save
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        saveItem.setOnAction(e -> fileManager.save());

        // Save As
        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setAccelerator(
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> fileManager.saveAs());

        // Export Copy As (another format, current file untouched)
        MenuItem exportItem = new MenuItem("Export Copy As...");
        exportItem.setOnAction(e -> fileManager.exportCopy());

        // Close
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> onClose.run());

        fileMenu.getItems().addAll(
                newTabItem,
                openItem,
                saveItem,
                saveAsItem,
                exportItem,
                new SeparatorMenuItem(),
                closeItem
        );

        return fileMenu;
    }

    /**
     * Creates the Help menu with Help and About options.
     * The Help option shows instructions for using the
     * drawing tools and keyboard shortcuts.
     */
    private Menu createTransformMenu() {
        Menu transform = new Menu("Transform");
        Menu entire = new Menu("Whole Canvas");
        Menu selected = new Menu("Selection");
        addTransformActions(entire, false);
        addTransformActions(selected, true);
        transform.getItems().addAll(entire, selected);
        return transform;
    }

    private void addTransformActions(Menu menu, boolean selectionOnly) {
        for (int degrees : new int[]{90, 180, 270}) {
            MenuItem item = new MenuItem("Rotate " + degrees + "° clockwise");
            item.setOnAction(e -> { if (activeCanvas != null)
                activeCanvas.transformImage(degrees, null, selectionOnly); });
            menu.getItems().add(item);
        }
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem horizontal = new MenuItem("Flip Horizontal (left / right)");
        horizontal.setOnAction(e -> { if (activeCanvas != null)
            activeCanvas.transformImage(0, true, selectionOnly); });
        MenuItem vertical = new MenuItem("Flip Vertical (top / bottom)");
        vertical.setOnAction(e -> { if (activeCanvas != null)
            activeCanvas.transformImage(0, false, selectionOnly); });
        menu.getItems().addAll(horizontal, vertical);
    }

    private Menu createHelpMenu() {

        Menu helpMenu = new Menu("Help");

        // Help
        MenuItem helpItem = new MenuItem("Help");

        helpItem.setOnAction(event -> {

            Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);

            helpAlert.setTitle("Help");
            helpAlert.setHeaderText("Paint Application");
            helpAlert.setContentText(
                    "Use the mouse to draw on the canvas.\n\n"
                            + "Tools: pencil, curve, line, rectangle, square, circle, "
                            + "ellipse, triangle, eraser, and eyedropper (color grabber).\n\n"
                            + "Clear Canvas (toolbar): wipes the page to blank white after "
                            + "asking you to confirm. Ctrl+Z brings it back.\n\n"
                            + "Autosave (menu): periodically saves tabs that have unsaved \n"
                            + "changes. Tabs with a file are saved to that file; untitled tabs are \n"
                            + "backed up to the .paint-autosave folder in your home directory. \n"
                            + "'Show countdown' displays the time until the next autosave in the \n"
                            + "status bar; saving by hand restarts the countdown.\n\n"
                            + "Web Server (menu): presents the images you choose (Choose Images \n"
                            + "to Share) as a web page. Tick 'Run web server', then 'Open in \n"
                            + "Browser'. If no image is chosen, the page says so.\n\n"
                            + "File formats: Save As and Export Copy As can write PNG, JPEG, BMP \n"
                            + "or GIF. You get a warning when the new format can lose data. \n"
                            + "Export Copy As leaves your current file untouched.\n\n"
                            + "Curve tool: drag out a baseline, release, then drag again "
                            + "to bend the curve and release to commit it.\n\n"
                            + "Shortcuts:\n"
                            + "  Ctrl+n   New tab\n"
                            + "  Ctrl+o  Open\n"
                            + "  Ctrl+S   Save\n"
                            + "  Ctrl+Shift+S   Save As\n"
                            + "  Ctrl+Q   Close\n"
            );

            helpAlert.showAndWait();
        });

        // About  in a separate dialog
        MenuItem aboutItem = new MenuItem("About");

        aboutItem.setOnAction(event -> {

            Alert aboutAlert = new Alert(Alert.AlertType.INFORMATION);

            aboutAlert.setTitle("About");
            aboutAlert.setHeaderText("Paint Application");
            aboutAlert.setContentText("");

            aboutAlert.showAndWait();
        });

        helpMenu.getItems().addAll(helpItem, aboutItem);

        return helpMenu;
    }
}