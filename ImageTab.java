import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Tab;

import java.io.File;



public class ImageTab extends Tab {

    // Gives every tab a stable id (used in the web server's image URLs)
    private static int nextId = 1;

    private final int id = nextId++;
    private final DrawingCanvas canvas;
    private File file;
    private String baseName;

    // Whether this tab's image is presented by the web server
    private final BooleanProperty shared = new SimpleBooleanProperty(false);

    public ImageTab(String baseName, DrawingCanvas canvas) {

        super(baseName);

        this.baseName = baseName;
        this.canvas = canvas;

        setContent(canvas);
        setClosable(true);


        canvas.unsavedChangesProperty().addListener((obs, was, isNow) -> updateTitle());
        shared.addListener((obs, was, isNow) -> updateTitle());
    }

    public int getImageId() {
        return id;
    }

    public DrawingCanvas getCanvas() {
        return canvas;
    }

    public String getBaseName() {
        return baseName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.baseName = file.getName();
        updateTitle();
    }

    public boolean isShared() {
        return shared.get();
    }

    public void setShared(boolean value) {
        shared.set(value);
    }

    public BooleanProperty sharedProperty() {
        return shared;
    }

    private void updateTitle() {
        setText((canvas.hasUnsavedChanges() ? "*" : "")
                + baseName
                + (shared.get() ? "  [web]" : ""));
    }
}
