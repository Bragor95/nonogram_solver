package dev.nonogram.solver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SolverTest {

    static Puzzle examplePuzzle() {
        return new Puzzle(
                List.of(new int[] {1, 1, 1}, new int[] {3, 1}, new int[] {1, 1}, new int[] {2}, new int[] {2}),
                List.of(new int[] {2}, new int[] {2}, new int[] {2}, new int[] {3}, new int[] {2, 2}));
    }

    static boolean[][] exampleSolution() {
        return parse("#.#.#", "###.#", ".#.#.", "...##", "...##");
    }

    @Test
    void solvesExampleScreenshotPuzzle() {
        Solver.Result result = Solver.solve(examplePuzzle());

        assertEquals(Solver.Status.UNIQUE, result.status());
        assertGridEquals(exampleSolution(), result.grid());
    }

    @Test
    void handlesEmptyAndFullLines() {
        Puzzle puzzle = new Puzzle(
                List.of(new int[] {3}, new int[] {0}, new int[] {}),
                List.of(new int[] {1}, new int[] {1}, new int[] {1}));

        Solver.Result result = Solver.solve(puzzle);

        assertEquals(Solver.Status.UNIQUE, result.status());
        assertGridEquals(parse("###", "...", "..."), result.grid());
    }

    @Test
    void reportsPuzzleWithoutSolution() {
        // Row clue {1,1} in a row of 3 forces the outer cells, but column 0 then needs two filled cells.
        Puzzle puzzle = new Puzzle(
                List.of(new int[] {1, 1}, new int[] {}, new int[] {}),
                List.of(new int[] {2}, new int[] {}, new int[] {}));

        assertEquals(Solver.Status.NO_SOLUTION, Solver.solve(puzzle).status());
    }

    @Test
    void reportsAmbiguousPuzzle() {
        Puzzle puzzle = new Puzzle(
                List.of(new int[] {1}, new int[] {1}),
                List.of(new int[] {1}, new int[] {1}));

        assertEquals(Solver.Status.MULTIPLE, Solver.solve(puzzle).status());
    }

    @Test
    void agreesWithBruteForceOnRandom4x4Puzzles() {
        Random random = new Random(42);
        for (int i = 0; i < 30; i++) {
            boolean[][] grid = randomGrid(random, 4);
            Puzzle puzzle = cluesOf(grid);

            Solver.Result result = Solver.solve(puzzle);

            long expected = countSolutions(puzzle);
            assertEquals(expected == 1 ? Solver.Status.UNIQUE : Solver.Status.MULTIPLE, result.status(),
                    "puzzle " + puzzle);
            assertEquals(puzzle.toString(), cluesOf(result.grid()).toString());
        }
    }

    @Test
    void solvesRandom10x10PuzzlesConsistently() {
        Random random = new Random(7);
        for (int i = 0; i < 20; i++) {
            Puzzle puzzle = cluesOf(randomGrid(random, 10));

            Solver.Result result = Solver.solve(puzzle);

            assertNotEquals(Solver.Status.NO_SOLUTION, result.status(), "puzzle " + puzzle);
            assertEquals(puzzle.toString(), cluesOf(result.grid()).toString());
        }
    }

    @Test
    void rejectsInconsistentClueTotals() {
        assertThrows(IllegalArgumentException.class, () -> new Puzzle(
                List.of(new int[] {2}, new int[] {}),
                List.of(new int[] {1}, new int[] {})));
    }

    @Test
    void rejectsClueThatDoesNotFit() {
        assertThrows(IllegalArgumentException.class, () -> new Puzzle(
                List.of(new int[] {2, 2}, new int[] {}),
                List.of(new int[] {1}, new int[] {1}, new int[] {1}, new int[] {1})));
    }

    private static boolean[][] randomGrid(Random random, int size) {
        boolean[][] grid = new boolean[size][size];
        for (boolean[] row : grid) {
            for (int c = 0; c < size; c++) {
                row[c] = random.nextBoolean();
            }
        }
        return grid;
    }

    private static Puzzle cluesOf(boolean[][] grid) {
        List<int[]> rows = new ArrayList<>();
        List<int[]> cols = new ArrayList<>();
        for (int r = 0; r < grid.length; r++) {
            boolean[] line = new boolean[grid[r].length];
            for (int c = 0; c < line.length; c++) {
                line[c] = grid[r][c];
            }
            rows.add(blocks(line));
        }
        for (int c = 0; c < grid[0].length; c++) {
            boolean[] line = new boolean[grid.length];
            for (int r = 0; r < line.length; r++) {
                line[r] = grid[r][c];
            }
            cols.add(blocks(line));
        }
        return new Puzzle(rows, cols);
    }

    private static int[] blocks(boolean[] line) {
        List<Integer> blocks = new ArrayList<>();
        int run = 0;
        for (boolean filled : line) {
            if (filled) {
                run++;
            } else if (run > 0) {
                blocks.add(run);
                run = 0;
            }
        }
        if (run > 0) {
            blocks.add(run);
        }
        return blocks.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Tries every grid; only usable for tiny sizes. */
    private static long countSolutions(Puzzle puzzle) {
        int size = puzzle.height();
        String wanted = puzzle.toString();
        long count = 0;
        for (int bits = 0; bits < 1 << (size * size); bits++) {
            boolean[][] grid = new boolean[size][size];
            for (int i = 0; i < size * size; i++) {
                grid[i / size][i % size] = (bits >> i & 1) == 1;
            }
            if (cluesOf(grid).toString().equals(wanted)) {
                count++;
            }
        }
        return count;
    }

    private static boolean[][] parse(String... rows) {
        boolean[][] grid = new boolean[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            grid[r] = new boolean[rows[r].length()];
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c] = rows[r].charAt(c) == '#';
            }
        }
        return grid;
    }

    private static void assertGridEquals(boolean[][] expected, boolean[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int r = 0; r < expected.length; r++) {
            assertArrayEquals(expected[r], actual[r], "row " + (r + 1));
        }
    }
}
