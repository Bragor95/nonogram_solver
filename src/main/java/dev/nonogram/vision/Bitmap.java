package dev.nonogram.vision;

import java.awt.image.BufferedImage;

/** Black-and-white view of a screenshot: {@code true} marks dark (ink) pixels. */
public final class Bitmap {

    private static final int DARK_THRESHOLD = 140;

    private final boolean[][] dark;
    private final int width;
    private final int height;

    private Bitmap(boolean[][] dark, int width, int height) {
        this.dark = dark;
        this.width = width;
        this.height = height;
    }

    public static Bitmap of(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] dark = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dark[y][x] = luminance(image.getRGB(x, y)) < DARK_THRESHOLD;
            }
        }
        return new Bitmap(dark, width, height);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean isDark(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height && dark[y][x];
    }

    /** Luminance of an ARGB pixel composited onto a white background. */
    private static int luminance(int argb) {
        int alpha = argb >>> 24;
        int r = blend((argb >> 16) & 0xFF, alpha);
        int g = blend((argb >> 8) & 0xFF, alpha);
        int b = blend(argb & 0xFF, alpha);
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }

    private static int blend(int channel, int alpha) {
        return (channel * alpha + 255 * (255 - alpha)) / 255;
    }
}
