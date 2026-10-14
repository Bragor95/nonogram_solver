package dev.nonogram.solver;

import java.util.Arrays;
import java.util.List;

/**
 * A nonogram: one clue (list of block lengths) per row and per column.
 * A line without blocks is represented by an empty array.
 */
public record Puzzle(List<int[]> rows, List<int[]> cols) {

    public Puzzle {
        rows = rows.stream().map(Puzzle::withoutZeros).toList();
        cols = cols.stream().map(Puzzle::withoutZeros).toList();
        if (rows.isEmpty() || cols.isEmpty()) {
            throw new IllegalArgumentException("Puzzle needs at least one row and one column");
        }
        checkFits(rows, cols.size(), "row");
        checkFits(cols, rows.size(), "column");
        if (sum(rows) != sum(cols)) {
            throw new IllegalArgumentException(
                    "Row clues fill " + sum(rows) + " cells but column clues fill " + sum(cols));
        }
    }

    public int height() {
        return rows.size();
    }

    public int width() {
        return cols.size();
    }

    @Override
    public String toString() {
        return "rows=" + format(rows) + " cols=" + format(cols);
    }

    public static String format(List<int[]> clues) {
        return clues.stream().map(Arrays::toString).toList().toString();
    }

    private static int[] withoutZeros(int[] clue) {
        return Arrays.stream(clue).filter(n -> n != 0).toArray();
    }

    private static void checkFits(List<int[]> clues, int length, String name) {
        for (int[] clue : clues) {
            int needed = Arrays.stream(clue).sum() + Math.max(0, clue.length - 1);
            if (needed > length) {
                throw new IllegalArgumentException(
                        "Clue " + Arrays.toString(clue) + " does not fit in a " + name + " of length " + length);
            }
        }
    }

    private static int sum(List<int[]> clues) {
        return clues.stream().flatMapToInt(Arrays::stream).sum();
    }
}
