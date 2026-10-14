package dev.nonogram.vision;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

import dev.nonogram.solver.Puzzle;

/** Reads the row and column clues of a screenshot, given where its grid is. */
public final class ClueReader {

    /** Blobs shorter than this fraction of the median blob height are treated as specks. */
    private static final double MIN_HEIGHT_FRACTION = 0.4;
    /** Blobs closer than this fraction of their height belong to the same multi-digit number. */
    private static final double DIGIT_GAP_FRACTION = 0.5;

    private final NumberRecognizer recognizer;

    public ClueReader(NumberRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    public Puzzle read(BufferedImage image, GridGeometry grid) {
        Bitmap bitmap = Bitmap.of(image);
        int[] xs = grid.xs();
        int[] ys = grid.ys();

        List<List<Blob>> columnBlobs = new ArrayList<>();
        for (int c = 0; c < grid.width(); c++) {
            columnBlobs.add(findBlobs(bitmap, inset(xs[c], true), inset(grid.clueTop(), true),
                    inset(xs[c + 1], false), inset(ys[0], false)));
        }
        List<List<Blob>> rowBlobs = new ArrayList<>();
        for (int r = 0; r < grid.height(); r++) {
            rowBlobs.add(findBlobs(bitmap, inset(grid.clueLeft(), true), inset(ys[r], true),
                    inset(xs[0], false), inset(ys[r + 1], false)));
        }
        double minHeight = MIN_HEIGHT_FRACTION * medianHeight(columnBlobs, rowBlobs);

        List<int[]> cols = new ArrayList<>();
        for (int c = 0; c < columnBlobs.size(); c++) {
            cols.add(readClue(image, columnBlobs.get(c), minHeight, true, "column " + (c + 1)));
        }
        List<int[]> rows = new ArrayList<>();
        for (int r = 0; r < rowBlobs.size(); r++) {
            rows.add(readClue(image, rowBlobs.get(r), minHeight, false, "row " + (r + 1)));
        }
        return new Puzzle(rows, cols);
    }

    private int[] readClue(BufferedImage image, List<Blob> blobs, double minHeight, boolean vertical, String where) {
        List<Blob> glyphs = blobs.stream().filter(b -> b.height() >= minHeight).toList();
        List<List<Blob>> numbers = groupIntoNumbers(glyphs, vertical);
        int[] clue = new int[numbers.size()];
        for (int i = 0; i < clue.length; i++) {
            List<Blob> digits = numbers.get(i);
            Blob box = Blob.union(digits);
            BufferedImage crop = image.getSubimage(box.minX, box.minY, box.width(), box.height());
            OptionalInt value = recognizer.recognize(crop, digits.size());
            if (value.isEmpty()) {
                throw new IllegalStateException(
                        "Could not read clue number " + (i + 1) + " of " + where + " at " + box);
            }
            clue[i] = value.getAsInt();
        }
        return clue;
    }

    /**
     * Groups glyphs into numbers. Glyphs on the same text line that are close together form one
     * multi-digit number; numbers are ordered top-to-bottom (columns) or left-to-right (rows).
     */
    private static List<List<Blob>> groupIntoNumbers(List<Blob> glyphs, boolean vertical) {
        List<Blob> byX = glyphs.stream().sorted(Comparator.comparingInt(b -> b.minX)).toList();
        List<List<Blob>> numbers = new ArrayList<>();
        for (Blob glyph : byX) {
            List<Blob> target = null;
            for (List<Blob> number : numbers) {
                Blob last = number.get(number.size() - 1);
                boolean sameLine = glyph.centerY() >= last.minY && glyph.centerY() <= last.maxY;
                boolean close = glyph.minX - last.maxX <= DIGIT_GAP_FRACTION * last.height();
                if (sameLine && close) {
                    target = number;
                    break;
                }
            }
            if (target == null) {
                target = new ArrayList<>();
                numbers.add(target);
            }
            target.add(glyph);
        }
        Comparator<List<Blob>> order = vertical
                ? Comparator.comparingDouble(n -> Blob.union(n).centerY())
                : Comparator.comparingDouble(n -> Blob.union(n).centerX());
        return numbers.stream().sorted(order).toList();
    }

    private static double medianHeight(List<List<Blob>> columns, List<List<Blob>> rows) {
        List<Integer> heights = new ArrayList<>();
        columns.forEach(blobs -> blobs.forEach(b -> heights.add(b.height())));
        rows.forEach(blobs -> blobs.forEach(b -> heights.add(b.height())));
        if (heights.isEmpty()) {
            throw new IllegalStateException("No clue numbers found in the image");
        }
        heights.sort(Comparator.naturalOrder());
        return heights.get(heights.size() / 2);
    }

    /** Moves a grid line position just inside the cell it borders, clear of the line's stroke. */
    private static int inset(int linePosition, boolean startEdge) {
        int margin = 3;
        return startEdge ? linePosition + margin : linePosition - margin;
    }

    /** Connected components of dark pixels inside the inclusive-exclusive region. */
    private static List<Blob> findBlobs(Bitmap bitmap, int x0, int y0, int x1, int y1) {
        boolean[][] seen = new boolean[Math.max(0, y1 - y0)][Math.max(0, x1 - x0)];
        List<Blob> blobs = new ArrayList<>();
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (seen[y - y0][x - x0] || !bitmap.isDark(x, y)) {
                    continue;
                }
                Blob blob = new Blob(x, y);
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[] {x, y});
                seen[y - y0][x - x0] = true;
                while (!queue.isEmpty()) {
                    int[] p = queue.poll();
                    blob.add(p[0], p[1]);
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = p[0] + dx;
                            int ny = p[1] + dy;
                            if (nx >= x0 && nx < x1 && ny >= y0 && ny < y1
                                    && !seen[ny - y0][nx - x0] && bitmap.isDark(nx, ny)) {
                                seen[ny - y0][nx - x0] = true;
                                queue.add(new int[] {nx, ny});
                            }
                        }
                    }
                }
                blobs.add(blob);
            }
        }
        return blobs;
    }

    /** Bounding box of a connected group of dark pixels. */
    private static final class Blob {
        int minX;
        int minY;
        int maxX;
        int maxY;

        Blob(int x, int y) {
            minX = maxX = x;
            minY = maxY = y;
        }

        void add(int x, int y) {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }

        double centerX() {
            return (minX + maxX) / 2.0;
        }

        double centerY() {
            return (minY + maxY) / 2.0;
        }

        static Blob union(List<Blob> blobs) {
            Blob result = new Blob(blobs.get(0).minX, blobs.get(0).minY);
            for (Blob b : blobs) {
                result.add(b.minX, b.minY);
                result.add(b.maxX, b.maxY);
            }
            return result;
        }

        @Override
        public String toString() {
            return "(" + minX + "," + minY + ")-(" + maxX + "," + maxY + ")";
        }
    }
}
