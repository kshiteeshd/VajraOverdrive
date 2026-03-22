package fleet;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FormationGenerator {

    private static final int MIN_TARGET_Y = 20;

    public static List<Point> generate(
            FleetDefinition.Formation type,
            int count, int spacing,
            int centerX, int startY) {

        List<Point> points = new ArrayList<>();

        switch (type) {

            case LINE -> {
                // Centered horizontal line
                int totalW = (count - 1) * spacing;
                int startX = centerX - totalW / 2;
                for (int i = 0; i < count; i++)
                    points.add(new Point(startX + i * spacing, startY));
            }

            case V -> {
                if (count % 2 == 0) count++;
                int mid = count / 2;
                for (int i = 0; i < count; i++) {
                    int offset = i - mid;
                    int x = centerX + offset * spacing;
                    int y = startY  + Math.abs(offset) * (spacing / 2);
                    points.add(new Point(x, Math.max(y, MIN_TARGET_Y)));
                }
            }

            case GRID -> {
                // FIXED: proper centering for any count
                int cols   = (int) Math.ceil(Math.sqrt(count));
                int rows   = (int) Math.ceil((double) count / cols);
                int totalW = (cols - 1) * spacing;
                int startX = centerX - totalW / 2;

                int placed = 0;
                for (int row = 0; row < rows && placed < count; row++) {
                    // How many in this row
                    int inRow   = Math.min(cols, count - placed);
                    int rowW    = (inRow - 1) * spacing;
                    int rowStartX = centerX - rowW / 2;   // center each row

                    for (int col = 0; col < inRow; col++) {
                        int x = rowStartX + col * spacing;
                        int y = startY    + row * spacing;
                        points.add(new Point(x, Math.max(y, MIN_TARGET_Y)));
                        placed++;
                    }
                }
            }

            case CIRCLE -> {
                double radius    = spacing * (count / (2.0 * Math.PI));
                radius           = Math.max(radius, spacing * 1.5);
                int circleCenterY = (int)(startY + radius);

                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count - Math.PI / 2;
                    int x = (int)(centerX      + Math.cos(angle) * radius);
                    int y = (int)(circleCenterY + Math.sin(angle) * radius);
                    points.add(new Point(x, Math.max(y, MIN_TARGET_Y)));
                }
            }

            case STAR -> {
                double outer     = spacing * 2.0;
                double inner     = spacing * 1.0;
                int starCenterY  = (int)(startY + outer);

                for (int i = 0; i < count; i++) {
                    double angle  = Math.PI * i / (count / 2.0);
                    double radius = (i % 2 == 0) ? outer : inner;
                    int x = (int)(centerX    + Math.cos(angle) * radius);
                    int y = (int)(starCenterY + Math.sin(angle) * radius);
                    points.add(new Point(x, Math.max(y, MIN_TARGET_Y)));
                }
            }

            case DIAMOND -> {
                // Rotated square — 4 sides
                // count determines how many per side (min 4)
                int perSide = Math.max(2, count / 4);
                int r       = perSide * spacing / 2;
                int dcx     = centerX;
                int dcy     = startY + r;

                // Top, Right, Bottom, Left points traced as diamond
                double[] angles = { -Math.PI/2, 0, Math.PI/2, Math.PI };
                Point[]  corners = new Point[4];
                for (int i = 0; i < 4; i++) {
                    corners[i] = new Point(
                            (int)(dcx + r * Math.cos(angles[i])),
                            (int)(dcy + r * Math.sin(angles[i]))
                    );
                }

                // Interpolate along each side
                int placed = 0;
                for (int side = 0; side < 4 && placed < count; side++) {
                    Point a = corners[side];
                    Point b = corners[(side + 1) % 4];
                    for (int j = 0; j < perSide && placed < count; j++) {
                        float t = j / (float) perSide;
                        int   x = (int)(a.x + (b.x - a.x) * t);
                        int   y = (int)(a.y + (b.y - a.y) * t);
                        points.add(new Point(x, Math.max(y, MIN_TARGET_Y)));
                        placed++;
                    }
                }
            }
        }

        return points;
    }
}