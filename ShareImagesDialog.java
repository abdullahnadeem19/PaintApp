import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * "Choose Images to Share" dialog: one checkbox per open tab. Ticked tabs are
 * presented by the web server.
 */
public class ShareImagesDialog {

    private ShareImagesDialog() { }

    /**
     * @param tabs      the open tabs
     * @param onChanged runs after the user confirms (to refresh the status text)
     */
    public static void show(ObservableList<Tab> tabs, Runnable onChanged) {

        Map<ImageTab, CheckBox> boxes = new LinkedHashMap<>();
        List<ImageTab> imageTabs = new ArrayList<>();

        for (Tab tab : tabs) {
            if (tab instanceof ImageTab) {
                imageTabs.add((ImageTab) tab);
            }
        }

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));

        if (imageTabs.isEmpty()) {
            content.getChildren().add(new Label("There are no open images."));
        }

        for (ImageTab tab : imageTabs) {
            CheckBox box = new CheckBox(tab.getBaseName());
            box.setSelected(tab.isShared());
            boxes.put(tab, box);
            content.getChildren().add(box);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Choose Images to Share");
        dialog.setHeaderText("\n"
                + "If none are ticked, the page shows a \"no images\" message.");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            for (Map.Entry<ImageTab, CheckBox> entry : boxes.entrySet()) {
                entry.getKey().setShared(entry.getValue().isSelected());
            }
            if (onChanged != null) {
                onChanged.run();
            }
        }
    }
}
