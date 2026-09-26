import javafx.scene.control.Tab;

import java.io.File;



public class ImageTab extends Tab {

    private final DrawingCanvas canvas;
    private File file;
    private String baseName;

    public ImageTab(String baseName, DrawingCanvas canvas) {

        super(baseName);

        this.baseName = baseName;
        this.canvas = canvas;

        setContent(canvas);
        setClosable(true);


        canvas.unsavedChangesProperty().addListener((obs, was, isNow) -> updateTitle());
    }

    public DrawingCanvas getCanvas() {
        return canvas;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.baseName = file.getName();
        updateTitle();
    }

    private void updateTitle() {
        setText((canvas.hasUnsavedChanges() ? "*" : "") + baseName);
    }
}
