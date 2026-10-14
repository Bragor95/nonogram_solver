package dev.nonogram;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import dev.nonogram.render.SolutionRenderer;
import dev.nonogram.solver.Puzzle;
import dev.nonogram.solver.Solver;
import dev.nonogram.vision.Bitmap;
import dev.nonogram.vision.ClueReader;
import dev.nonogram.vision.GridDetector;
import dev.nonogram.vision.GridGeometry;
import dev.nonogram.vision.GridNotFoundException;
import dev.nonogram.vision.TesseractRecognizer;

public final class Main {

    private Main() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -jar nonogram.jar <screenshot.png> [solution.png]");
            System.exit(2);
        }
        Path input = Path.of(args[0]);
        try {
            BufferedImage screenshot = ImageIO.read(input.toFile());
            if (screenshot == null) {
                throw new IOException("Not a readable image: " + input);
            }
            GridGeometry grid = GridDetector.detect(Bitmap.of(screenshot));
            Puzzle puzzle = new ClueReader(new TesseractRecognizer()).read(screenshot, grid);

            System.out.println("Grid:    " + puzzle.width() + "x" + puzzle.height());
            System.out.println("Rows:    " + Puzzle.format(puzzle.rows()));
            System.out.println("Columns: " + Puzzle.format(puzzle.cols()));

            Solver.Result result = Solver.solve(puzzle);
            switch (result.status()) {
                case NO_SOLUTION -> fail("The clues have no solution; a number was probably misread.");
                case MULTIPLE -> System.out.println("Warning: the puzzle has more than one solution; showing the first.");
                case UNIQUE -> { }
            }

            System.out.println();
            print(result.grid());

            Path output = args.length == 2 ? Path.of(args[1]) : defaultOutput(input);
            ImageIO.write(SolutionRenderer.render(puzzle, result.grid()), "png", output.toFile());
            System.out.println();
            System.out.println("Wrote " + output);
        } catch (IOException | GridNotFoundException | IllegalStateException | IllegalArgumentException e) {
            fail(e.getMessage());
        }
    }

    private static void print(boolean[][] grid) {
        for (boolean[] row : grid) {
            StringBuilder line = new StringBuilder();
            for (boolean filled : row) {
                line.append(filled ? "##" : "..");
            }
            System.out.println(line);
        }
    }

    private static Path defaultOutput(Path input) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return input.resolveSibling(base + "-solved.png");
    }

    private static void fail(String message) {
        System.err.println("Error: " + message);
        System.exit(1);
    }
}
