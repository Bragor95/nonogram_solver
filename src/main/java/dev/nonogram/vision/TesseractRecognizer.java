package dev.nonogram.vision;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.OptionalInt;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/** {@link NumberRecognizer} backed by a locally installed Tesseract. */
public final class TesseractRecognizer implements NumberRecognizer {

    private static final int SCALE = 4;
    private static final int MARGIN = 12;
    private static final int PSM_SINGLE_LINE = 7;
    private static final int PSM_SINGLE_CHAR = 10;

    private final Tesseract tesseract = new Tesseract();

    public TesseractRecognizer() {
        configureNativeLookup();
        tesseract.setDatapath(tessdataPath());
        tesseract.setLanguage("eng");
        tesseract.setVariable("tessedit_char_whitelist", "0123456789");
    }

    @Override
    public OptionalInt recognize(BufferedImage image, int digits) {
        BufferedImage prepared = prepare(image);
        int[] modes = digits == 1
                ? new int[] {PSM_SINGLE_CHAR, PSM_SINGLE_LINE}
                : new int[] {PSM_SINGLE_LINE, PSM_SINGLE_CHAR};
        for (int mode : modes) {
            tesseract.setPageSegMode(mode);
            try {
                String text = tesseract.doOCR(prepared).strip();
                if (text.matches("\\d{1,4}")) {
                    return OptionalInt.of(Integer.parseInt(text));
                }
            } catch (TesseractException e) {
                throw new IllegalStateException("Tesseract failed: " + e.getMessage(), e);
            } catch (UnsatisfiedLinkError e) {
                throw new IllegalStateException(
                        "Could not load the Tesseract library. Install it (brew install tesseract) "
                                + "or point -Djna.library.path to its directory.", e);
            }
        }
        return OptionalInt.empty();
    }

    /** Upscales and pads the glyphs: Tesseract does poorly on tiny, edge-hugging text. */
    private static BufferedImage prepare(BufferedImage image) {
        int width = image.getWidth() * SCALE + 2 * MARGIN;
        int height = image.getHeight() * SCALE + 2 * MARGIN;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = result.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, MARGIN, MARGIN, image.getWidth() * SCALE, image.getHeight() * SCALE, null);
        g.dispose();
        return result;
    }

    private static void configureNativeLookup() {
        if (System.getProperty("jna.library.path") == null && new File("/opt/homebrew/lib").isDirectory()) {
            System.setProperty("jna.library.path", "/opt/homebrew/lib");
        }
    }

    private static String tessdataPath() {
        String fromEnv = System.getenv("TESSDATA_PREFIX");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        for (String candidate : new String[] {
                "/opt/homebrew/share/tessdata", "/usr/local/share/tessdata", "/usr/share/tesseract-ocr/5/tessdata"}) {
            if (new File(candidate).isDirectory()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Tesseract language data not found. Set TESSDATA_PREFIX.");
    }
}
