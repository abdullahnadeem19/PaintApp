import javafx.scene.control.Alert;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;

//The menu bar: Color tab, Line Width, File, and Help menus.

public class AppMenuBar extends MenuBar {


    public AppMenuBar(DrawingCanvas drawing,
                      FileManager fileManager,
                      Runnable onClose) {

        getMenus().addAll(
                createColorMenu(drawing),
                createLineWidthMenu(drawing),
                createFileMenu(fileManager, onClose),
                createHelpMenu()
        );
    }

    // COLOR CHOOSER
    private Menu createColorMenu(DrawingCanvas drawing) {

        Menu colorMenu = new Menu("Color tab");

        // Creating color picker
        ColorPicker colorPicker = new ColorPicker();

        // Change the drawing color when a color is selected
        colorPicker.setOnAction(event ->
                drawing.setBrushColor(colorPicker.getValue())
        );

        // The color picker inside the Color tab
        CustomMenuItem colorItem = new CustomMenuItem(colorPicker);

        // Keep the color menu open when selecting a color
        colorItem.setHideOnClick(false);

        colorMenu.getItems().add(colorItem);

        return colorMenu;
    }

    // LINE WIDTH
    private Menu createLineWidthMenu(DrawingCanvas drawing) {

        // Create line width slider
        Slider lineWidthSlider = new Slider(1, 50, 12);

        // Show numbers/ticks on the slider
        lineWidthSlider.setShowTickLabels(true);
        lineWidthSlider.setShowTickMarks(true);

        // Change brush size when slider moves
        lineWidthSlider.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        drawing.setBrushWidth(newValue.doubleValue())
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

    // FILE MENU
    private Menu createFileMenu(FileManager fileManager, Runnable onClose) {

        Menu fileMenu = new Menu("File");

        // Open
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> fileManager.openImage());

        // Save
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> fileManager.save());

        // Save As
        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setOnAction(e -> fileManager.saveAs());

        // Close
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> onClose.run());

        fileMenu.getItems().addAll(
                openItem,
                saveItem,
                saveAsItem,
                new SeparatorMenuItem(),
                closeItem
        );

        return fileMenu;
    }

    // HELP MENU
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
            );

            helpAlert.showAndWait();
        });

        // About (shown in a separate dialog)
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
