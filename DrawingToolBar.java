import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;


/**
 * Creates the toolbar for the Paint Application.
 * It contains drawing tool buttons and a dashed outline toggle,
 * and buttons for selecting different drawing tools.
 * All controls work with the currently active canvas.
 */
public class DrawingToolBar extends HBox {

    // The canvas that this toolbar's controls currently act on
    private DrawingCanvas activeCanvas;

    private final ToggleButton dashedBtn = new ToggleButton("Dashed");



    private final ToggleGroup shapeGroup = new ToggleGroup();
    private final ToggleButton freehandBtn = new ToggleButton("Pencil");
    private final ToggleButton curveBtn = new ToggleButton("Curve");
    private final ToggleButton polybtn  =  new ToggleButton("Polygon");
    private final ToggleButton lineBtn = new ToggleButton("Line");
    private final ToggleButton rectBtn = new ToggleButton("Rectangle");
    private final ToggleButton squareBtn = new ToggleButton("Square");
    private final ToggleButton circleBtn = new ToggleButton("Circle");
    private final ToggleButton ellipseBtn = new ToggleButton("Ellipse");
    private final ToggleButton triangleBtn = new ToggleButton("Triangle");
    private final ToggleButton rightTriangleBtn = new ToggleButton("Right Triangle"); // new add
    private final ToggleButton trapeziumBtn = new ToggleButton("Trapezium"); // new add
    private final ToggleButton eraserBtn = new ToggleButton("Eraser");
    private final ToggleButton eyedropperBtn = new ToggleButton("Eyedropper");
    private final ToggleButton selectBtn = new ToggleButton("Select"); // new add
    private final ToggleButton moveSelectionBtn = new ToggleButton("Move Selection"); // new add
    private final ToggleButton textBtn = new ToggleButton("Text"); // new add
    private final ToggleButton moveImageBtn = new ToggleButton("Move Picture"); // new add

    // new add: Clear Canvas button (wipes the canvas, with a confirmation check)
    private final Button clearBtn = new Button("Clear Canvas");


    /**
     * Initializes the toolbar and sets up its controls.
     * Adds the drawing tool buttons and dashed option to the toolbar.
     */
    public DrawingToolBar() {

        setSpacing(12);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER_LEFT);

        //  Dashed outline toggle
        dashedBtn.setOnAction(event -> {
            if (activeCanvas != null) {
                activeCanvas.setDashed(dashedBtn.isSelected());
            }
        });

        // Tool selection

        setupToolButton(freehandBtn, DrawingCanvas.ShapeType.FREEHAND);
        setupToolButton(curveBtn, DrawingCanvas.ShapeType.CURVE);
        setupToolButton (polybtn,DrawingCanvas.ShapeType.POLYGON);
        setupToolButton(lineBtn, DrawingCanvas.ShapeType.LINE);
        setupToolButton(rectBtn, DrawingCanvas.ShapeType.RECTANGLE);
        setupToolButton(squareBtn, DrawingCanvas.ShapeType.SQUARE);
        setupToolButton(circleBtn, DrawingCanvas.ShapeType.CIRCLE);
        setupToolButton(ellipseBtn, DrawingCanvas.ShapeType.ELLIPSE);
        setupToolButton(triangleBtn, DrawingCanvas.ShapeType.TRIANGLE);
        setupToolButton(rightTriangleBtn, DrawingCanvas.ShapeType.RIGHT_TRIANGLE); // new add
        setupToolButton(trapeziumBtn, DrawingCanvas.ShapeType.TRAPEZIUM); // new add
        setupToolButton(eraserBtn, DrawingCanvas.ShapeType.ERASER);
        setupToolButton(eyedropperBtn, DrawingCanvas.ShapeType.EYEDROPPER);
        setupToolButton(selectBtn, DrawingCanvas.ShapeType.SELECT); // new add
        setupToolButton(moveSelectionBtn, DrawingCanvas.ShapeType.MOVE_SELECTION); // new add
        setupToolButton(textBtn, DrawingCanvas.ShapeType.TEXT); // new add
        setupToolButton(moveImageBtn, DrawingCanvas.ShapeType.MOVE_IMAGE); // new add

        // new add: Clear Canvas button — confirms before wiping the drawing
        clearBtn.setOnAction(event -> {

            if (activeCanvas == null) {
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear Canvas");
            alert.setHeaderText("You are about to clear the entire canvas.");
            alert.setContentText("Are you sure you want to clear it?");

            ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

            if (result == ButtonType.OK) {
                activeCanvas.clearCanvas();
            }
        });

        // Select the button matching the canvas's current tool (defaults to pencil)
        freehandBtn.setSelected(true);

        getChildren().addAll(
                new Label("|"),
                freehandBtn, curveBtn, polybtn,lineBtn, rectBtn, squareBtn, circleBtn, ellipseBtn, triangleBtn, rightTriangleBtn, trapeziumBtn,
                new Label("|"),
                eraserBtn, eyedropperBtn,
                new Label("|"),
                selectBtn, moveSelectionBtn, textBtn, moveImageBtn,
                new Label("|"),
                dashedBtn,
                new Label("|"),
                clearBtn
        );
    }

    /**
     * Sets the active canvas for the toolbar.
     * Updates the dashed option and selected tool
     * to match the current settings of the canvas.
     */
    public void setActiveCanvas(DrawingCanvas canvas) {

        this.activeCanvas = canvas;

        dashedBtn.setSelected(canvas.isDashed());

        selectButtonForShape(canvas.getShapeType());
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
            case POLYGON:     target=  polybtn;break;
            case RECTANGLE:   target = rectBtn; break;
            case SQUARE:      target = squareBtn; break;
            case CIRCLE:      target = circleBtn; break;
            case ELLIPSE:     target = ellipseBtn; break;
            case TRIANGLE:    target = triangleBtn; break;
            case RIGHT_TRIANGLE: target = rightTriangleBtn; break; // new add
            case TRAPEZIUM:   target = trapeziumBtn; break; // new add
            case ERASER:      target = eraserBtn; break;
            case EYEDROPPER:  target = eyedropperBtn; break;
            case SELECT:      target = selectBtn; break; // new add
            case MOVE_SELECTION: target = moveSelectionBtn; break; // new add
            case TEXT:        target = textBtn; break; // new add
            case MOVE_IMAGE:  target = moveImageBtn; break; // new add
            default:          target = freehandBtn; break;
        }

        target.setSelected(true);
    }
}