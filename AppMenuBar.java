
import javafx.scene.control.Alert;
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
     */
    public AppMenuBar(FileManager fileManager,
                      Runnable onClose,
                      Runnable onNewTab) {

        updateColorLabel(Color.BLACK);

        getMenus().addAll(  // Add menu tab
                createFileMenu(fileManager, onClose, onNewTab),
                createColorMenu(),
                createLineWidthMenu(),
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

        // Close
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> onClose.run());

        fileMenu.getItems().addAll(
                newTabItem,
                openItem,
                saveItem,
                saveAsItem,
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