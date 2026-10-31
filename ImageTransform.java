import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/** Lossless pixel coordinate transforms; all methods return a new image. */
public final class ImageTransform {
    private ImageTransform() { }
    public static WritableImage rotate(Image source, int degrees) {
        if (degrees != 90 && degrees != 180 && degrees != 270)
            throw new IllegalArgumentException("Only 90, 180 and 270 degrees are supported");
        int w = (int) source.getWidth(), h = (int) source.getHeight();
        WritableImage result = new WritableImage(degrees == 180 ? w : h, degrees == 180 ? h : w);
        PixelReader from = source.getPixelReader();
        PixelWriter to = result.getPixelWriter();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int tx = degrees == 90 ? h - 1 - y : degrees == 180 ? w - 1 - x : y;
            int ty = degrees == 90 ? x : degrees == 180 ? h - 1 - y : w - 1 - x;
            to.setArgb(tx, ty, from.getArgb(x, y));
        }
        return result;
    }
    public static WritableImage flip(Image source, boolean horizontal) {
        int w = (int) source.getWidth(), h = (int) source.getHeight();
        WritableImage result = new WritableImage(w, h);
        PixelReader from = source.getPixelReader();
        PixelWriter to = result.getPixelWriter();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            to.setArgb(horizontal ? w - x - 1 : x, horizontal ? y : h - y - 1, from.getArgb(x, y));
        return result;
    }
}
