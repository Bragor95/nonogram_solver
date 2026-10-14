package dev.nonogram.vision;

import java.util.ArrayList;
import java.util.List;

/**
 * Locates the cell grid in a nonogram screenshot.
 *
 * <p>Grid lines are the pixel columns/rows that contain a long unbroken dark run. The cell grid
 * is the bottom-right block of those lines whose spacing is uniform; wider gaps further out belong
 * to the clue areas.
 */
public final class GridDetector {

    /** A line must be at least this fraction of the longest run to count as a grid line. */
    private static final double LINE_FRACTION = 0.5;
    private static final int MIN_LINE_LENGTH = 30;
    private static final double SPACING_TOLERANCE = 0.2;

    private GridDetector() { }

    public static GridGeometry detect(Bitmap bitmap) {
        int[] verticalRuns = new int[bitmap.width()];
        for (int x = 0; x < bitmap.width(); x++) {
            int run = 0;
            for (int y = 0; y < bitmap.height(); y++) {
                run = bitmap.isDark(x, y) ? run + 1 : 0;
                verticalRuns[x] = Math.max(verticalRuns[x], run);
            }
        }
        int[] horizontalRuns = new int[bitmap.height()];
        for (int y = 0; y < bitmap.height(); y++) {
            int run = 0;
            for (int x = 0; x < bitmap.width(); x++) {
                run = bitmap.isDark(x, y) ? run + 1 : 0;
                horizontalRuns[y] = Math.max(horizontalRuns[y], run);
            }
        }

        List<Integer> xLines = lineCenters(verticalRuns);
        List<Integer> yLines = lineCenters(horizontalRuns);
        List<Integer> xGrid = uniformTail(xLines);
        List<Integer> yGrid = uniformTail(yLines);
        if (xGrid.size() < 2 || yGrid.size() < 2) {
            throw new GridNotFoundException("Could not find a grid of evenly spaced lines in the image");
        }

        int clueLeft = xLines.get(0) < xGrid.get(0) ? xLines.get(0) : 0;
        int clueTop = yLines.get(0) < yGrid.get(0) ? yLines.get(0) : 0;
        return new GridGeometry(toArray(xGrid), toArray(yGrid), clueTop, clueLeft);
    }

    /** Centers of groups of adjacent indices whose run length qualifies as a grid line. */
    private static List<Integer> lineCenters(int[] runs) {
        int longest = 0;
        for (int run : runs) {
            longest = Math.max(longest, run);
        }
        if (longest < MIN_LINE_LENGTH) {
            throw new GridNotFoundException("No grid lines found in the image");
        }
        double threshold = LINE_FRACTION * longest;
        List<Integer> centers = new ArrayList<>();
        int i = 0;
        while (i < runs.length) {
            if (runs[i] < threshold) {
                i++;
                continue;
            }
            int first = i;
            while (i < runs.length && runs[i] >= threshold) {
                i++;
            }
            centers.add((first + i - 1) / 2);
        }
        return centers;
    }

    /** Longest run of lines at the end of the list that are spaced like the last gap. */
    private static List<Integer> uniformTail(List<Integer> lines) {
        int last = lines.size() - 1;
        if (last < 1) {
            return lines;
        }
        double spacing = lines.get(last) - lines.get(last - 1);
        int first = last - 1;
        while (first > 0) {
            double gap = lines.get(first) - lines.get(first - 1);
            if (Math.abs(gap - spacing) > SPACING_TOLERANCE * spacing) {
                break;
            }
            first--;
        }
        return lines.subList(first, lines.size());
    }

    private static int[] toArray(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
}
