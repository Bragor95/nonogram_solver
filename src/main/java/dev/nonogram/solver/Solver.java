package dev.nonogram.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * Solves nonograms by line-wise constraint propagation, falling back to depth-first search
 * for puzzles that propagation alone cannot finish.
 */
public final class Solver {

    public enum Status { NO_SOLUTION, UNIQUE, MULTIPLE }

    /** {@code grid} is the (first) solution, or {@code null} for {@link Status#NO_SOLUTION}. */
    public record Result(Status status, boolean[][] grid) { }

    private static final byte UNKNOWN = 0;
    private static final byte FILLED = 1;
    private static final byte EMPTY = 2;
    private static final int CONTRADICTION = -1;

    private Solver() { }

    public static Result solve(Puzzle puzzle) {
        byte[][] start = new byte[puzzle.height()][puzzle.width()];
        List<byte[][]> solutions = new ArrayList<>();
        search(puzzle, start, solutions);
        if (solutions.isEmpty()) {
            return new Result(Status.NO_SOLUTION, null);
        }
        Status status = solutions.size() == 1 ? Status.UNIQUE : Status.MULTIPLE;
        return new Result(status, toBooleans(solutions.get(0)));
    }

    /** Collects at most two solutions, which is enough to tell unique from ambiguous. */
    private static void search(Puzzle puzzle, byte[][] grid, List<byte[][]> solutions) {
        if (!propagate(puzzle, grid)) {
            return;
        }
        int[] unknown = firstUnknown(grid);
        if (unknown == null) {
            solutions.add(grid);
            return;
        }
        for (byte guess : new byte[] {FILLED, EMPTY}) {
            byte[][] copy = copy(grid);
            copy[unknown[0]][unknown[1]] = guess;
            search(puzzle, copy, solutions);
            if (solutions.size() >= 2) {
                return;
            }
        }
    }

    /** Applies line solving to all rows and columns until nothing changes. False on contradiction. */
    private static boolean propagate(Puzzle puzzle, byte[][] grid) {
        int height = puzzle.height();
        int width = puzzle.width();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < height; r++) {
                int progress = solveLine(grid[r], puzzle.rows().get(r));
                if (progress == CONTRADICTION) {
                    return false;
                }
                changed |= progress > 0;
            }
            byte[] column = new byte[height];
            for (int c = 0; c < width; c++) {
                for (int r = 0; r < height; r++) {
                    column[r] = grid[r][c];
                }
                int progress = solveLine(column, puzzle.cols().get(c));
                if (progress == CONTRADICTION) {
                    return false;
                }
                if (progress > 0) {
                    changed = true;
                    for (int r = 0; r < height; r++) {
                        grid[r][c] = column[r];
                    }
                }
            }
        }
        return true;
    }

    /**
     * Fixes every cell of {@code line} that has the same value in all arrangements of {@code clue}
     * consistent with the cells known so far.
     *
     * @return number of newly determined cells, or {@link #CONTRADICTION} if no arrangement fits
     */
    private static int solveLine(byte[] line, int[] clue) {
        int n = line.length;
        int k = clue.length;
        boolean[][] prefix = reachable(line, clue);
        if (!prefix[n][k]) {
            return CONTRADICTION;
        }
        boolean[][] suffix = reachableFromEnd(line, clue);

        boolean[] canBeFilled = new boolean[n];
        boolean[] canBeEmpty = new boolean[n];
        for (int c = 0; c < n; c++) {
            if (line[c] == FILLED) {
                continue;
            }
            for (int j = 0; j <= k && !canBeEmpty[c]; j++) {
                canBeEmpty[c] = prefix[c][j] && suffix[c + 1][j];
            }
        }
        for (int j = 0; j < k; j++) {
            for (int start = 0; start + clue[j] <= n; start++) {
                int end = start + clue[j];
                if (!blockFits(line, start, end)) {
                    continue;
                }
                boolean leftOk = start == 0 ? j == 0 : line[start - 1] != FILLED && prefix[start - 1][j];
                boolean rightOk = end == n ? j == k - 1 : line[end] != FILLED && suffix[end + 1][j + 1];
                if (leftOk && rightOk) {
                    for (int c = start; c < end; c++) {
                        canBeFilled[c] = true;
                    }
                }
            }
        }

        int determined = 0;
        for (int c = 0; c < n; c++) {
            if (line[c] != UNKNOWN) {
                continue;
            }
            if (canBeFilled[c] && !canBeEmpty[c]) {
                line[c] = FILLED;
                determined++;
            } else if (canBeEmpty[c] && !canBeFilled[c]) {
                line[c] = EMPTY;
                determined++;
            }
        }
        return determined;
    }

    /**
     * {@code result[i][j]}: the first {@code i} cells can hold the first {@code j} blocks, ignoring
     * what follows (a block ending at cell {@code i - 1} is allowed).
     */
    private static boolean[][] reachable(byte[] line, int[] clue) {
        int n = line.length;
        int k = clue.length;
        boolean[][] f = new boolean[n + 1][k + 1];
        f[0][0] = true;
        for (int i = 1; i <= n; i++) {
            for (int j = 0; j <= k; j++) {
                if (line[i - 1] != FILLED && f[i - 1][j]) {
                    f[i][j] = true;
                    continue;
                }
                if (j == 0) {
                    continue;
                }
                int start = i - clue[j - 1];
                if (start < 0 || !blockFits(line, start, i)) {
                    continue;
                }
                f[i][j] = start == 0 ? j == 1 : line[start - 1] != FILLED && f[start - 1][j - 1];
            }
        }
        return f;
    }

    /** {@code result[i][j]}: cells {@code i..n-1} can hold blocks {@code j..k-1}. */
    private static boolean[][] reachableFromEnd(byte[] line, int[] clue) {
        int n = line.length;
        int k = clue.length;
        byte[] reversedLine = new byte[n];
        for (int i = 0; i < n; i++) {
            reversedLine[i] = line[n - 1 - i];
        }
        int[] reversedClue = new int[k];
        for (int j = 0; j < k; j++) {
            reversedClue[j] = clue[k - 1 - j];
        }
        boolean[][] fr = reachable(reversedLine, reversedClue);
        boolean[][] g = new boolean[n + 1][k + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= k; j++) {
                g[i][j] = fr[n - i][k - j];
            }
        }
        return g;
    }

    private static boolean blockFits(byte[] line, int start, int end) {
        for (int c = start; c < end; c++) {
            if (line[c] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    private static int[] firstUnknown(byte[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == UNKNOWN) {
                    return new int[] {r, c};
                }
            }
        }
        return null;
    }

    private static byte[][] copy(byte[][] grid) {
        byte[][] copy = new byte[grid.length][];
        for (int r = 0; r < grid.length; r++) {
            copy[r] = grid[r].clone();
        }
        return copy;
    }

    private static boolean[][] toBooleans(byte[][] grid) {
        boolean[][] result = new boolean[grid.length][grid[0].length];
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                result[r][c] = grid[r][c] == FILLED;
            }
        }
        return result;
    }
}
