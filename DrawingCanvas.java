import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;


public class DrawingCanvas extends StackPane {

    // shapes
    public enum ShapeType {
        FREEHAND,
        CURVE,
        LINE,
        RECTANGLE,
        SQUARE,
        CIRCLE,
        ELLIPSE,
        TRIANGLE,
        ERASER,
        EYEDROPPER
    }

    // Dash pattern
    private static final double[] DASH_PATTERN = { 12, 8 };

    // Canvas for the finished drawing
    private final Canvas canvas = new Canvas();
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    // Transparent canvas on top, used only to preview a shape while dragging
    private final Canvas previewCanvas = new Canvas();
    private final GraphicsContext previewGc = previewCanvas.getGraphicsContext2D();

    // Keeps track of whether the drawing was changed
    private final BooleanProperty unsavedChanges = new SimpleBooleanProperty(false);

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

    public DrawingCanvas() {


        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        previewCanvas.widthProperty().bind(widthProperty());
        previewCanvas.heightProperty().bind(heightProperty());


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
        this.currentShape = shapeType;

        // Abandon any in-progress curve if the tool changes mid-curve
        if (shapeType != ShapeType.CURVE) {
            curveStage = 0;
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
