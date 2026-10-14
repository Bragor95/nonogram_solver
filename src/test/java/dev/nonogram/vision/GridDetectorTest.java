package dev.nonogram.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class GridDetectorTest {

    @Test
    void detectsFiveByFiveGridInExampleScreenshot() throws IOException {
        BufferedImage image = ImageIO.read(getClass().getResource("/example-5x5.png"));

        GridGeometry grid = GridDetector.detect(Bitmap.of(image));

        assertEquals(5, grid.width());
        assertEquals(5, grid.height());
        // Cells are ~37px wide and the grid sits in the lower right of the 368x357 screenshot.
        for (int i = 0; i < 5; i++) {
            assertEquals(37, grid.xs()[i + 1] - grid.xs()[i], 3);
            assertEquals(37, grid.ys()[i + 1] - grid.ys()[i], 3);
        }
        assertTrue(grid.clueLeft() < grid.xs()[0]);
        assertTrue(grid.clueTop() < grid.ys()[0]);
    }

    @Test
    void rejectsImageWithoutGrid() {
        BufferedImage blank = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        assertThrows(GridNotFoundException.class, () -> GridDetector.detect(Bitmap.of(blank)));
    }
}
