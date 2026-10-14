package dev.nonogram.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import dev.nonogram.solver.Puzzle;

/** Draws a puzzle with its solution, in the same layout as the source screenshots. */
public final class SolutionRenderer {

    private static final int CELL = 40;
    private static final int PADDING = 16;

    private SolutionRenderer() { }

    public static BufferedImage render(Puzzle puzzle, boolean[][] solution) {
        int clueCols = puzzle.rows().stream().mapToInt(c -> c.length).max().orElse(0);
        int clueRows = puzzle.cols().stream().mapToInt(c -> c.length).max().orElse(0);
        int originX = PADDING + clueCols * CELL;
        int originY = PADDING + clueRows * CELL;
        int width = originX + puzzle.width() * CELL + PADDING;
        int height = originY + puzzle.height() * CELL + PADDING;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        for (int r = 0; r < puzzle.height(); r++) {
            for (int c = 0; c < puzzle.width(); c++) {
                if (solution[r][c]) {
                    g.fillRect(originX + c * CELL, originY + r * CELL, CELL, CELL);
                }
            }
        }

        g.setColor(Color.GRAY);
        for (int r = 0; r <= puzzle.height(); r++) {
            g.drawLine(PADDING, originY + r * CELL, originX + puzzle.width() * CELL, originY + r * CELL);
        }
        for (int c = 0; c <= puzzle.width(); c++) {
            g.drawLine(originX + c * CELL, PADDING, originX + c * CELL, originY + puzzle.height() * CELL);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        FontMetrics metrics = g.getFontMetrics();
        drawColumnClues(g, metrics, puzzle.cols(), originX, originY);
        drawRowClues(g, metrics, puzzle.rows(), originX, originY);
        g.dispose();
        return image;
    }

    /** Column clues are bottom-aligned against the grid. */
    private static void drawColumnClues(Graphics2D g, FontMetrics m, List<int[]> cols, int originX, int originY) {
        for (int c = 0; c < cols.size(); c++) {
            int[] clue = cols.get(c);
            for (int i = 0; i < clue.length; i++) {
                int fromBottom = clue.length - 1 - i;
                drawCentered(g, m, String.valueOf(clue[i]),
                        originX + c * CELL + CELL / 2, originY - fromBottom * CELL - CELL / 2);
            }
        }
    }

    /** Row clues are right-aligned against the grid. */
    private static void drawRowClues(Graphics2D g, FontMetrics m, List<int[]> rows, int originX, int originY) {
        for (int r = 0; r < rows.size(); r++) {
            int[] clue = rows.get(r);
            for (int i = 0; i < clue.length; i++) {
                int fromRight = clue.length - 1 - i;
                drawCentered(g, m, String.valueOf(clue[i]),
                        originX - fromRight * CELL - CELL / 2, originY + r * CELL + CELL / 2);
            }
        }
    }

    private static void drawCentered(Graphics2D g, FontMetrics m, String text, int cx, int cy) {
        g.drawString(text, cx - m.stringWidth(text) / 2, cy + (m.getAscent() - m.getDescent()) / 2);
    }
}
