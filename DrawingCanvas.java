import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextInputDialog; // new add: for TEXT tool
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.util.Stack; // new add


public class DrawingCanvas extends StackPane {

    // shapes
    public enum ShapeType {
        FREEHAND,
        CURVE,
        LINE,
        RECTANGLE,
        POLYGON,
        SQUARE,
        CIRCLE,
        ELLIPSE,
        TRIANGLE,
        RIGHT_TRIANGLE,
        TRAPEZIUM,
        ERASER,
        EYEDROPPER,
        SELECT,
        MOVE_SELECTION,
        TEXT,
        MOVE_IMAGE
    }

    // Dash pattern
    private static final double[] DASH_PATTERN = { 12, 8 };

    // Canvas for the finished drawing
    private final Canvas canvas = new Canvas();
    private final GraphicsContext gc = canvas.getGraphicsContext2D();


    private final Canvas previewCanvas = new Canvas();
    private final GraphicsContext previewGc = previewCanvas.getGraphicsContext2D();

    // Keeps track of whether the drawing was changed
    private final BooleanProperty unsavedChanges = new SimpleBooleanProperty(false);

    // new add for 5 Undo/redo history, stored as full-canvas snapshots
    private final Stack<WritableImage> undoStack = new Stack<>();
    private final Stack<WritableImage> redoStack = new Stack<>();

    // Currently selected tool (defaults to freehand drawing)
    private ShapeType currentShape = ShapeType.FREEHAND;

    // Line width used for freehand strokes, shape outlines, and the eraser
    private double brushWidth = 12;

    // Currently selected brush/stroke color
    private Color brushColor = Color.BLACK;

    // Whether shapes/lines are drawn with a dashed outline
    private boolean dashed = false;


    private Consumer<Color> onColorPicked;

    // Mouse position where the current stroke/shape started
    private double startX;
    private double startY;

    // Last point drawn to, used to connect freehand/eraser drag segments
    private double lastX;
    private double lastY;


    private int curveStage = 0;
    private double curveStartX, curveStartY;
    private double curveEndX, curveEndY;


    private boolean backgroundPainted = false;


    private double selX, selY, selW, selH;
    private boolean hasSelection = false;
    private WritableImage clipboard;
    private boolean movingSelection = false;
    private double moveOffsetX;
    private double moveOffsetY;
    private int polygonSides = 5;


    private WritableImage wholeImageClipboard;

    public DrawingCanvas() {


        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        previewCanvas.widthProperty().bind(widthProperty());
        previewCanvas.heightProperty().bind(heightProperty());


        canvas.widthProperty().addListener((obs, oldV, newV) -> paintBlankBackgroundOnce());
        canvas.heightProperty().addListener((obs, oldV, newV) -> paintBlankBackgroundOnce());

        getChildren().addAll(canvas, previewCanvas);

        // Set brush size
        gc.setLineWidth(brushWidth);
        previewGc.setLineWidth(brushWidth);

        // Set default drawing color
        gc.setStroke(Color.BLACK);
        previewGc.setStroke(Color.BLACK);

        // Set white background
        gc.setFill(Color.WHITE);

        // Record starting point
        previewCanvas.setOnMousePressed(event -> {


            undoStack.push(canvas.snapshot(new SnapshotParameters(), null));
            redoStack.clear();

            startX = event.getX();
            startY = event.getY();
            lastX = startX;
            lastY = startY;

            switch (currentShape) {

                case FREEHAND:
                    gc.strokeLine(startX, startY, startX, startY);
                    setUnsavedChanges(true);
                    break;

                case ERASER:
                    eraseSegment(startX, startY, startX, startY);
                    setUnsavedChanges(true);
                    break;

                case EYEDROPPER:
                    pickColor(startX, startY);
                    break;

                case CURVE:
                    if (curveStage == 0) {
                        curveStartX = startX;
                        curveStartY = startY;
                        curveStage = 1;
                    }

                    break;

                case SELECT:
                    // Stage 0 (no captured piece yet): nothing to do on press itself —
                    // the drag handler below live-draws the selection rectangle.
                    // Stage 1 (piece already captured): nothing to do on press either —
                    // the drag handler live-draws the piece following the cursor.
                    break;

                case MOVE_SELECTION:
                    if (hasSelection && clipboard != null) {
                        movingSelection = true;
                        moveOffsetX = event.getX() - selX;
                        moveOffsetY = event.getY() - selY;
                        gc.clearRect(selX, selY, selW, selH);
                        gc.setFill(Color.WHITE);
                        gc.fillRect(selX, selY, selW, selH);
                    }
                    break;

                case MOVE_IMAGE:
                    // Capture the whole picture, then clear it from its current spot —
                    // the drag handler below live-draws it following the cursor
                    wholeImageClipboard = canvas.snapshot(new SnapshotParameters(), null);
                    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    gc.setFill(Color.WHITE);
                    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    setUnsavedChanges(true);
                    break;

                case TEXT: {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Add Text");
                    dialog.setHeaderText("Enter text to place on the canvas:");
                    dialog.showAndWait().ifPresent(text -> {
                        gc.setFont(javafx.scene.text.Font.font(Math.max(brushWidth * 2, 12)));
                        gc.setFill(brushColor);
                        gc.fillText(text, startX, startY);
                        setUnsavedChanges(true);
                    });
                    break;
                }

                default:

                    break;
            }
        });

        // Draw a live preview while dragging (or draw freehand/erase directly)
        previewCanvas.setOnMouseDragged(event -> {

            double x = event.getX();
            double y = event.getY();

            switch (currentShape) {

                case FREEHAND:
                    gc.strokeLine(lastX, lastY, x, y);
                    lastX = x;
                    lastY = y;
                    setUnsavedChanges(true);
                    break;

                case ERASER:
                    eraseSegment(lastX, lastY, x, y);
                    lastX = x;
                    lastY = y;
                    setUnsavedChanges(true);
                    break;

                case EYEDROPPER:
                    // Only sample on click, not while dragging
                    break;

                case CURVE:
                    if (curveStage == 1) {
                        curveEndX = x;
                        curveEndY = y;
                        clearPreview();
                        previewGc.strokeLine(curveStartX, curveStartY, curveEndX, curveEndY);
                    } else if (curveStage == 2) {
                        clearPreview();
                        drawQuadCurve(previewGc, curveStartX, curveStartY, x, y, curveEndX, curveEndY);
                    }
                    break;

                case POLYGON:
                    clearPreview();
                    drawShape(previewGc, currentShape, startX, startY, event.getX(), event.getY());
                    break;

                case SELECT:
                    if (!hasSelection) {
                        // Live-draw the selection rectangle being dragged out ("marching box")
                        clearPreview();
                        double rx = Math.min(startX, x);
                        double ry = Math.min(startY, y);
                        double rw = Math.abs(x - startX);
                        double rh = Math.abs(y - startY);
                        previewGc.strokeRect(rx, ry, rw, rh);
                    } else if (clipboard != null) {
                        // Live-draw the captured piece following the cursor before it's dropped
                        clearPreview();
                        previewGc.drawImage(clipboard, x, y);
                    }
                    break;

                case MOVE_SELECTION:
                    if (movingSelection && clipboard != null) {
                        clearPreview();
                        previewGc.drawImage(clipboard, x - moveOffsetX, y - moveOffsetY);
                    }
                    break;

                case TEXT:
                    // Text is placed immediately on click; nothing to preview while dragging
                    break;

                case MOVE_IMAGE:
                    // Live-draw the whole picture following the cursor, offset by where it was grabbed
                    if (wholeImageClipboard != null) {
                        clearPreview();
                        previewGc.drawImage(wholeImageClipboard, x - startX, y - startY);
                    }
                    break;

                default:
                    clearPreview();
                    drawShape(previewGc, currentShape, startX, startY, x, y);
                    setUnsavedChanges(true);
                    break;
            }
        });

        // On release, commit the shape to the real canvas and clear the preview
        previewCanvas.setOnMouseReleased(event -> {

            switch (currentShape) {

                case FREEHAND:
                case ERASER:
                case EYEDROPPER:
                    // Already handled live during press/drag
                    break;

                case CURVE:
                    if (curveStage == 1) {

                        curveEndX = event.getX();
                        curveEndY = event.getY();
                        curveStage = 2;
                    } else if (curveStage == 2) {
                        drawQuadCurve(gc, curveStartX, curveStartY,
                                event.getX(), event.getY(), curveEndX, curveEndY);
                        clearPreview();
                        setUnsavedChanges(true);
                        curveStage = 0;
                    }
                    break;

                case SELECT:
                    if (!hasSelection) {
                        // Finish drawing the selection rectangle and capture its pixels
                        double x = event.getX();
                        double y = event.getY();

                        selX = Math.min(startX, x);
                        selY = Math.min(startY, y);
                        selW = Math.abs(x - startX);
                        selH = Math.abs(y - startY);
                        clearPreview();

                        if (selW > 0 && selH > 0) {
                            SnapshotParameters params = new SnapshotParameters();
                            params.setViewport(new Rectangle2D(selX, selY, selW, selH));
                            clipboard = canvas.snapshot(params, null);
                            hasSelection = true;
                        }

                    } else if (clipboard != null) {

                        gc.drawImage(clipboard, event.getX(), event.getY());
                        clearPreview();
                        setUnsavedChanges(true);
                    }
                    break;

                case MOVE_SELECTION:
                    if (movingSelection && clipboard != null) {
                        double newX = event.getX() - moveOffsetX;
                        double newY = event.getY() - moveOffsetY;
                        gc.drawImage(clipboard, newX, newY);
                        clearPreview();
                        selX = newX;
                        selY = newY;
                        movingSelection = false;
                        setUnsavedChanges(true);
                    }
                    break;

                case TEXT:

                    break;

                case MOVE_IMAGE:

                    if (wholeImageClipboard != null) {
                        double dropX = event.getX() - startX;
                        double dropY = event.getY() - startY;
                        gc.drawImage(wholeImageClipboard, dropX, dropY);
                        clearPreview();
                        setUnsavedChanges(true);
                        wholeImageClipboard = null;
                    }
                    break;

                default:
                    drawShape(gc, currentShape, startX, startY, event.getX(), event.getY());
                    clearPreview();
                    setUnsavedChanges(true);
                    break;
            }
        });
    }

    //  Brush settings

    public void setBrushColor(Color color) {
        brushColor = color;
        gc.setStroke(color);
        previewGc.setStroke(color);
    }

    public Color getBrushColor() {
        return brushColor;
    }

    public void setBrushWidth(double width) {
        brushWidth = width;
        gc.setLineWidth(width);
        previewGc.setLineWidth(width);
    }

    public double getBrushWidth() {
        return brushWidth;
    }

    //  Dashed outlines

    public void setDashed(boolean dashed) {
        this.dashed = dashed;
        if (dashed) {
            gc.setLineDashes(DASH_PATTERN);
            previewGc.setLineDashes(DASH_PATTERN);
        } else {
            gc.setLineDashes((double[]) null);
            previewGc.setLineDashes((double[]) null);
        }
    }

    public boolean isDashed() {
        return dashed;
    }

    //  Shape/tool selection

    public void setShapeType(ShapeType shapeType) {
        // Ask for the polygon size when the Polygon tool is selected,
        // not while the mouse is already pressed on the canvas.
        // This keeps the normal press-drag-release drawing sequence working.
        if (shapeType == ShapeType.POLYGON) {
            TextInputDialog dialog = new TextInputDialog(String.valueOf(polygonSides));
            dialog.setTitle("Polygon");
            dialog.setHeaderText("Enter the number of sides for the regular polygon:");
            dialog.setContentText("Number of sides:");

            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    int sides = Integer.parseInt(result.get().trim());
                    if (sides >= 3) {
                        polygonSides = sides;
                    } else {
                        new Alert(Alert.AlertType.ERROR,
                                "A polygon must have at least 3 sides.", ButtonType.OK).showAndWait();
                        return;
                    }
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR,
                            "Please enter a whole number (3 or more).", ButtonType.OK).showAndWait();
                    return;
                }
            } else {
                return;
            }
        }

        this.currentShape = shapeType;

        // Abandon any in-progress curve if the tool changes mid-curve
        if (shapeType != ShapeType.CURVE) {
            curveStage = 0;
            clearPreview();
        }

        // new add: abandon any in-progress selection if the tool changes away from SELECT
        if (shapeType != ShapeType.SELECT && shapeType != ShapeType.MOVE_SELECTION) {
            hasSelection = false;
            clipboard = null;
            movingSelection = false;
            clearPreview();
        }
    }

    public ShapeType getShapeType() {
        return currentShape;
    }

    //  Eyedropper (color grabber)

    public void setOnColorPicked(Consumer<Color> onColorPicked) {
        this.onColorPicked = onColorPicked;
    }


    private void pickColor(double x, double y) {

        if (x < 0 || y < 0 || x >= canvas.getWidth() || y >= canvas.getHeight()) {
            return;
        }

        WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
        PixelReader reader = snapshot.getPixelReader();

        Color sampled = reader.getColor((int) x, (int) y);

        setBrushColor(sampled);

        if (onColorPicked != null) {
            onColorPicked.accept(sampled);
        }
    }

    //  Eraser

    // Clears square patches (sized to the brush width) along a segment so
    // dragging quickly still erases a continuous stroke.
    private void eraseSegment(double x1, double y1, double x2, double y2) {

        double size = Math.max(brushWidth, 4);
        double dist = Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(1, (int) (dist / (size / 2)));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            gc.clearRect(x - size / 2, y - size / 2, size, size);
        }
    }

    //  Shape drawing

    private void clearPreview() {
        previewGc.clearRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
    }

    // new add: fills the canvas white once it has a real size, so new tabs start as a true blank image
    private void paintBlankBackgroundOnce() {
        if (!backgroundPainted && canvas.getWidth() > 0 && canvas.getHeight() > 0) {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            backgroundPainted = true;
        }
    }


    private void drawQuadCurve(GraphicsContext g,
                               double x1, double y1,
                               double cx, double cy,
                               double x2, double y2) {

        g.beginPath();
        g.moveTo(x1, y1);
        g.quadraticCurveTo(cx, cy, x2, y2);
        g.stroke();
    }

    private void drawShape(GraphicsContext g, ShapeType shape,
                           double x1, double y1, double x2, double y2) {

        double x = Math.min(x1, x2);
        double y = Math.min(y1, y2);
        double w = Math.abs(x2 - x1);
        double h = Math.abs(y2 - y1);

        switch (shape) {

            case LINE:
                g.strokeLine(x1, y1, x2, y2);
                break;

            case RECTANGLE:
                g.strokeRect(x, y, w, h);
                break;


            case POLYGON: {
                int sides = Math.max(3, polygonSides);
                double radius = Math.hypot(x2 - x1, y2 - y1);

                double[] xPoints = new double[sides];
                double[] yPoints = new double[sides];

                for (int i = 0; i < sides; i++) {
                    double angle = 2 * Math.PI * i / sides;

                    xPoints[i] = x1 + radius * Math.cos(angle);
                    yPoints[i] = y1 + radius * Math.sin(angle);
                }

                g.strokePolygon(xPoints, yPoints, sides);
                break;
            }
            case SQUARE: {
                double side = Math.max(w, h);
                double sx = (x2 >= x1) ? x1 : x1 - side;
                double sy = (y2 >= y1) ? y1 : y1 - side;
                g.strokeRect(sx, sy, side, side);
                break;
            }

            case ELLIPSE:
                g.strokeOval(x, y, w, h);
                break;

            case CIRCLE: {
                double diameter = Math.max(w, h);
                double cx = (x2 >= x1) ? x1 : x1 - diameter;
                double cy = (y2 >= y1) ? y1 : y1 - diameter;
                g.strokeOval(cx, cy, diameter, diameter);
                break;
            }

            case TRIANGLE: {
                double[] xs = { x + w / 2, x, x + w };
                double[] ys = { y, y + h, y + h };
                g.strokePolygon(xs, ys, 3);
                break;
            }

            case RIGHT_TRIANGLE: {
                double[] xs = { x, x, x + w };
                double[] ys = { y, y + h, y + h };
                g.strokePolygon(xs, ys, 3);
                break;
            }

            case TRAPEZIUM: {
                // Isosceles trapezoid: shorter parallel side on top, full width on the bottom
                double[] xs = { x + w * 0.25, x + w * 0.75, x + w, x };
                double[] ys = { y, y, y + h, y + h };
                g.strokePolygon(xs, ys, 4);
                break;
            }

            default:
                break;
        }
    }

    //  Canvas

    public Canvas getCanvas() {
        return canvas;
    }

    // Clears the canvas and draws an image stretched to fit it
    public void showImage(Image image) {

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());
    }

    // new add: wipes the canvas back to a blank white image.
    // Pushes onto the same undo stack as every other drawing action,
    // so clearing the canvas can also be undone.
    public void clearCanvas() {

        undoStack.push(canvas.snapshot(new SnapshotParameters(), null));
        redoStack.clear();

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        setUnsavedChanges(true);
    }

    // new add for 5 Undo / Redo
    // Restores the canvas to its state before the most recent change.
    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(canvas.snapshot(new SnapshotParameters(), null));
            WritableImage previous = undoStack.pop();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(previous, 0, 0);
            setUnsavedChanges(true);
        }
    }


    // Re-applies a change that was just undone.
    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(canvas.snapshot(new SnapshotParameters(), null));
            WritableImage next = redoStack.pop();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(next, 0, 0);
            setUnsavedChanges(true);
        }
    }




    // Unsaved

    public boolean hasUnsavedChanges() {
        return unsavedChanges.get();
    }

    public void setUnsavedChanges(boolean value) {
        unsavedChanges.set(value);
    }

    public BooleanProperty unsavedChangesProperty() {
        return unsavedChanges;
    }
}