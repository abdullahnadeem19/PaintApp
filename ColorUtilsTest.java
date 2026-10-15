import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**  tests for ColorUtils */
public class ColorUtilsTest {

    @Test
    void convertsRedToHex() {
        assertEquals("#FF0000", ColorUtils.toHex(Color.RED));
    }

    @Test
    void convertsGreenToRgbString() {
        assertEquals("rgb(0, 128, 0)", ColorUtils.toRgbString(Color.GREEN));
    }

    @Test
    void findsClosestNameForBlue() {
        assertEquals("Blue", ColorUtils.closestColorName(Color.BLUE));
    }
}
