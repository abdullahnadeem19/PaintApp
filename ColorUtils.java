import javafx.scene.paint.Color;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small helper for describing a Color as hex, rgb, and the closest named
 */
public class ColorUtils {

    // Built once, lazily, from every predefined JavaFX web color constant
    private static Map<String, Color> namedColors;

    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    public static String toRgbString(Color c) {
        return String.format("rgb(%d, %d, %d)",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    // Finds the closest predefined JavaFX color name by simple RGB distance
    public static String closestColorName(Color target) {

        Map<String, Color> named = namedColors();

        String bestName = "Unknown";
        double bestDist = Double.MAX_VALUE;

        for (Map.Entry<String, Color> entry : named.entrySet()) {

            Color c = entry.getValue();

            double dr = c.getRed() - target.getRed();
            double dg = c.getGreen() - target.getGreen();
            double db = c.getBlue() - target.getBlue();

            double dist = dr * dr + dg * dg + db * db;

            if (dist < bestDist) {
                bestDist = dist;
                bestName = entry.getKey();
            }
        }

        return bestName;
    }


    private static synchronized Map<String, Color> namedColors() {

        if (namedColors != null) {
            return namedColors;
        }

        Map<String, Color> map = new LinkedHashMap<>();

        for (Field field : Color.class.getFields()) {

            boolean isNamedColorConstant =
                    Modifier.isStatic(field.getModifiers())
                            && Modifier.isPublic(field.getModifiers())
                            && field.getType() == Color.class;

            if (!isNamedColorConstant) {
                continue;
            }

            try {

                Color value = (Color) field.get(null);


                if (value.getOpacity() < 0.99) {
                    continue;
                }

                map.put(prettify(field.getName()), value);

            } catch (IllegalAccessException ignored) {
                // Skip fields we can't read
            }
        }

        namedColors = map;

        return map;
    }

    // "LIGHTSTEELBLUE" -> "Lightsteelblue"
    private static String prettify(String constantName) {
        String lower = constantName.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
