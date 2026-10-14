# Nonogram solver

Reads a nonogram screenshot, extracts the row/column clues with Tesseract OCR, solves the puzzle and
writes a PNG of the solution. Proof of concept, validated on the 5x5 screenshot in `example/`.

## Requirements
- JDK 21, Maven 3.9+
- Tesseract with English data (`brew install tesseract`). The libraries are looked up in
  `/opt/homebrew/lib`; override with `-Djna.library.path=...` and `TESSDATA_PREFIX`.

## Usage
```
mvn package
java --enable-native-access=ALL-UNNAMED -jar target/nonogram.jar <screenshot.png> [solution.png]
```
Prints the detected clues and the solved grid; the solution image defaults to `<screenshot>-solved.png`.

## How it works
- `vision/GridDetector`: finds grid lines (long dark pixel runs) and the evenly spaced cell grid.
- `vision/ClueReader` + `TesseractRecognizer`: segments digits per clue cell and OCRs them.
- `solver/Solver`: line-wise constraint propagation with DFS fallback; reports no / unique / multiple solutions.
- `render/SolutionRenderer`: draws the result.

## Limits
- Only the screenshot style in `example/` is tested (black clues on white, grid in the lower right).
- If every clue of a puzzle is a single number of one cell, the clue box and the grid can't be told apart.
- `mvn test` OCR tests are skipped when Tesseract can't be loaded.
