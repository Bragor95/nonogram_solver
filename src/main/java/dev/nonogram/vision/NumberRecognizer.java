package dev.nonogram.vision;

import java.awt.image.BufferedImage;
import java.util.OptionalInt;

/** Reads a number from a small image that contains nothing but its digits. */
public interface NumberRecognizer {

    /**
     * @param image  dark digits on a white background
     * @param digits how many digit glyphs the image contains
     */
    OptionalInt recognize(BufferedImage image, int digits);
}
