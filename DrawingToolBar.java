
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/**
 * Creates the toolbar for the Paint Application.
 * It contains a brush-width slider, a dashed outline toggle,
 * and buttons for selecting different drawing tools.
 * All controls work with the currently active canvas.
 */
public class DrawingToolBar extends HBox {

    // The canvas that this toolbar's controls currently act on
    private DrawingCanvas activeCanvas;

    private final Slider widthSlider = new Slider(1, 50, 12);
    private final Label widthValue = new Label();
    private final ToggleButton dashedBtn = new ToggleButton("Dashed");

    private final ToggleGroup shapeGroup = new ToggleGroup();
    private final ToggleButton freehandBtn = new ToggleButton("Pencil");
    private final ToggleButton curveBtn = new ToggleButton("Curve");
    private final ToggleButton lineBtn = new ToggleButton("Line");
    private final ToggleButton rectBtn = new ToggleButton("Rectangle");
    private final ToggleButton squareBtn = new ToggleButton("Square");
    private final ToggleButton circleBtn = new ToggleButton("Circle");
    private final ToggleButton ellipseBtn = new ToggleButton("Ellipse");
    private final ToggleButton triangleBtn = new ToggleButton("Triangle");
    private final ToggleButton eraserBtn = new ToggleButton("Eraser");
    private final ToggleButton eyedropperBtn = new ToggleButton("Eyedropper");

    /**
     * Initializes the toolbar and sets up its controls.
     * Adds the brush-width slider, dashed option, and
     * drawing tool buttons to the toolbar.
     */
    public DrawingToolBar() {

        setSpacing(12);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER_LEFT);

        // Width control

        Label widthLabel = new Label("Width:");

        widthSlider.setShowTickMarks(true);
        widthSlider.setShowTickLabels(false);
        widthSlider.setPrefWidth(140);

        widthValue.setPrefWidth(44);
        updateWidthLabel(widthSlider.getValue());

        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (activeCanvas != null) {
                activeCanvas.setBrushWidth(newVal.doubleValue());
            }
            updateWidthLabel(newVal.doubleValue());
        });

        //  Dashed outline toggle
        dashedBtn.setOnAction(event -> {
            if (activeCanvas != null) {
                activeCanvas.setDashed(dashedBtn.isSelected());
            }
        });

        // Tool selection

        setupToolButton(freehandBtn, DrawingCanvas.ShapeType.FREEHAND);
        setupToolButton(curveBtn, DrawingCanvas.ShapeType.CURVE);
        setupToolButton(lineBtn, DrawingCanvas.ShapeType.LINE);
        setupToolButton(rectBtn, DrawingCanvas.ShapeType.RECTANGLE);
        setupToolButton(squareBtn, DrawingCanvas.ShapeType.SQUARE);
        setupToolButton(circleBtn, DrawingCanvas.ShapeType.CIRCLE);
        setupToolButton(ellipseBtn, DrawingCanvas.ShapeType.ELLIPSE);
        setupToolButton(triangleBtn, DrawingCanvas.ShapeType.TRIANGLE);
        setupToolButton(eraserBtn, DrawingCanvas.ShapeType.ERASER);
        setupToolButton(eyedropperBtn, DrawingCanvas.ShapeType.EYEDROPPER);

        // Select the button matching the canvas's current tool (defaults to pencil)
        freehandBtn.setSelected(true);

        getChildren().addAll(
                widthLabel, widthSlider, widthValue,
                new Label("|"),
                freehandBtn, curveBtn, lineBtn, rectBtn, squareBtn, circleBtn, ellipseBtn, triangleBtn,
                new Label("|"),
                eraserBtn, eyedropperBtn,
                new Label("|"),
                dashedBtn
        );
    }

    /**
     * Sets the active canvas for the toolbar.
     * Updates the slider, dashed option, and selected tool
     * to match the current settings of the canvas.
     */
    public void setActiveCanvas(DrawingCanvas canvas) {

        this.activeCanvas = canvas;

        widthSlider.setValue(canvas.getBrushWidth());
        updateWidthLabel(canvas.getBrushWidth());

        dashedBtn.setSelected(canvas.isDashed());

        selectButtonForShape(canvas.getShapeType());
    }

    /**
     * Updates the width label to show the current brush size.
     * Displays the width as a number followed by pixels.
     *
     * @param width the current brush width
     */
    private void updateWidthLabel(double width) {
        widthValue.setText(String.format("%.0f px", width));
    }

    /**
     * Connects a tool button to its drawing shape.
     * When the user selects a button, the active canvas
     * changes to use that drawing tool.
     *
     * @param button the button for the drawing tool
     * @param shape the shape type linked to the button
     */
    private void setupToolButton(ToggleButton button, DrawingCanvas.ShapeType shape) {

        button.setToggleGroup(shapeGroup);

        button.setOnAction(event -> {
            if (button.isSelected()) {
                if (activeCanvas != null) {
                    activeCanvas.setShapeType(shape);
                }
            } else {
                // Prevent deselecting the active tool with no replacement selected
                button.setSelected(true);
            }
        });
    }

    /**
     * Selects the button that matches the current drawing tool.
     * This keeps the toolbar selection in sync when the user
     * switches between different canvases.
     *
     * @param shape the drawing tool currently selected
     */
    private void selectButtonForShape(DrawingCanvas.ShapeType shape) {

        ToggleButton target;

        switch (shape) {
            case CURVE:       target = curveBtn; break;
            case LINE:        target = lineBtn; break;
            case RECTANGLE:   target = rectBtn; break;
            case SQUARE:      target = squareBtn; break;
            case CIRCLE:      target = circleBtn; break;
            case ELLIPSE:     target = ellipseBtn; break;
            case TRIANGLE:    target = triangleBtn; break;
            case ERASER:      target = eraserBtn; break;
            case EYEDROPPER:  target = eyedropperBtn; break;
            default:          target = freehandBtn; break;
        }

        target.setSelected(true);
    }
}