package dev.nonogram.vision;

/**
 * Pixel layout of a nonogram screenshot.
 *
 * @param xs       x positions of the vertical lines of the cell grid (width + 1 entries)
 * @param ys       y positions of the horizontal lines of the cell grid (height + 1 entries)
 * @param clueTop  y where the column-clue area starts
 * @param clueLeft x where the row-clue area starts
 */
public record GridGeometry(int[] xs, int[] ys, int clueTop, int clueLeft) {

    public int width() {
        return xs.length - 1;
    }

    public int height() {
        return ys.length - 1;
    }
}
