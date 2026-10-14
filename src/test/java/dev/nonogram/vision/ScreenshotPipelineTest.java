package dev.nonogram.vision;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import dev.nonogram.solver.Puzzle;
import dev.nonogram.solver.Solver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Needs a local Tesseract installation; skipped when it can't be loaded. */
class ScreenshotPipelineTest {

    private static TesseractRecognizer recognizer;

    @BeforeAll
    static void requireTesseract() {
        try {
            recognizer = new TesseractRecognizer();
            recognizer.recognize(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), 1);
        } catch (IllegalStateException | LinkageError e) {
            assumeTrue(false, "Tesseract not available: " + e.getMessage());
        }
    }

    @Test
    void readsCluesFromExampleScreenshot() throws IOException {
        BufferedImage image = ImageIO.read(getClass().getResource("/example-5x5.png"));
        GridGeometry grid = GridDetector.detect(Bitmap.of(image));

        Puzzle puzzle = new ClueReader(recognizer).read(image, grid);

        assertClues(List.of(new int[] {1, 1, 1}, new int[] {3, 1}, new int[] {1, 1}, new int[] {2}, new int[] {2}),
                puzzle.rows());
        assertClues(List.of(new int[] {2}, new int[] {2}, new int[] {2}, new int[] {3}, new int[] {2, 2}),
                puzzle.cols());
    }

    @Test
    void solvesExampleScreenshotEndToEnd() throws IOException {
        BufferedImage image = ImageIO.read(getClass().getResource("/example-5x5.png"));
        GridGeometry grid = GridDetector.detect(Bitmap.of(image));
        Puzzle puzzle = new ClueReader(recognizer).read(image, grid);

        Solver.Result result = Solver.solve(puzzle);

        assertEquals(Solver.Status.UNIQUE, result.status());
        String[] expected = {"#.#.#", "###.#", ".#.#.", "...##", "...##"};
        for (int r = 0; r < expected.length; r++) {
            StringBuilder row = new StringBuilder();
            for (boolean filled : result.grid()[r]) {
                row.append(filled ? '#' : '.');
            }
            assertEquals(expected[r], row.toString(), "row " + (r + 1));
        }
    }

    private static void assertClues(List<int[]> expected, List<int[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i), "clue " + (i + 1));
        }
    }
}
