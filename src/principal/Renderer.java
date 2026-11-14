package principal;

import java.nio.ByteBuffer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Draws the simulation on a JavaFX canvas; the view, with no rules and no state
 * of its own.
 * <p>
 * Every cell becomes a {@value #CELL_SIZE}x{@value #CELL_SIZE} block of pixels. The
 * colours can be smoothed with a small weighted average around each cell, or drawn
 * as flat blocks, and the whole canvas is composed in one {@code byte[]} that is
 * pushed to the image with a single {@link PixelWriter#setPixels} call per frame.
 */
final class Renderer {

    /** Size of one cell on screen, in pixels. The canvas is a multiple of it. */
    static final int CELL_SIZE = 8;

    private static final int BYTES_PER_PIXEL = 4;
    private static final PixelFormat<ByteBuffer> BGRA = PixelFormat.getByteBgraInstance();
    private static final byte OPAQUE = (byte) 0xFF;
    /**
     * Weight the cell adds on top of its single pass of the loop, so the cell counts
     * five times and each neighbour once.
     */
    private static final int CENTER_EXTRA_WEIGHT = 4;

    private final GraphicsContext gc;
    private final Simulation simulation;
    private final int width;
    private final int height;
    private final int cellWidth;
    private final int cellHeight;
    private final WritableImage image;
    private final PixelWriter pixelWriter;
    /** One frame of BGRA pixels, {@code width * height * 4} bytes, reused. */
    private final byte[] frame;
    /** True to average each cell with its neighbourhood, false to draw flat blocks. */
    private boolean smoothing = true;

    /**
     * Creates a renderer for the given simulation.
     *
     * @param gc         graphics context of the canvas (must not be null)
     * @param simulation simulation to draw (must not be null)
     * @throws IllegalArgumentException if either argument is null
     */
    Renderer(GraphicsContext gc, Simulation simulation) {
        if (gc == null) {
            throw new IllegalArgumentException("GraphicsContext cannot be null");
        }
        if (simulation == null) {
            throw new IllegalArgumentException("Simulation cannot be null");
        }

        this.gc = gc;
        this.simulation = simulation;
        this.cellWidth = simulation.width();
        this.cellHeight = simulation.height();
        this.width = cellWidth * CELL_SIZE;
        this.height = cellHeight * CELL_SIZE;
        this.image = new WritableImage(width, height);
        this.pixelWriter = image.getPixelWriter();
        this.frame = new byte[width * height * BYTES_PER_PIXEL];
    }

    /** Canvas width in pixels, always a multiple of {@link #CELL_SIZE}. */
    int width() {
        return width;
    }

    /** Canvas height in pixels, always a multiple of {@link #CELL_SIZE}. */
    int height() {
        return height;
    }

    /** Composes the current generation and paints it on the canvas. */
    void render() {
        fillFrame();
        pixelWriter.setPixels(0, 0, width, height, BGRA, frame, 0, width * BYTES_PER_PIXEL);
        gc.drawImage(image, 0, 0);
    }

    /**
     * Switches between the smoothed look and flat blocks. It can be called at any
     * time, even in the middle of a run; the change shows on the next {@link #render()}.
     *
     * @param smoothing true to smooth the cells, false to draw them as flat blocks
     */
    void setSmoothing(boolean smoothing) {
        this.smoothing = smoothing;
    }

    /** Writes the whole frame buffer; the screen is only touched once, in {@link #render()}. */
    private void fillFrame() {
        int[][] red = simulation.channel(Simulation.RED);
        int[][] green = simulation.channel(Simulation.GREEN);
        int[][] blue = simulation.channel(Simulation.BLUE);

        for (int cy = 0; cy < cellHeight; cy++) {
            for (int cx = 0; cx < cellWidth; cx++) {
                if (smoothing) {
                    fillBlock(cx, cy, (byte) blurredValue(cx, cy, blue),
                            (byte) blurredValue(cx, cy, green),
                            (byte) blurredValue(cx, cy, red));
                } else {
                    fillBlock(cx, cy, (byte) blue[cy][cx], (byte) green[cy][cx], (byte) red[cy][cx]);
                }
            }
        }
    }

    /** Paints one cell as a solid {@value #CELL_SIZE}x{@value #CELL_SIZE} block. */
    private void fillBlock(int cellX, int cellY, byte b, byte g, byte r) {
        int blockStart = cellY * CELL_SIZE * width + cellX * CELL_SIZE; // in pixels
        for (int dy = 0; dy < CELL_SIZE; dy++) {
            int position = (blockStart + dy * width) * BYTES_PER_PIXEL;
            for (int dx = 0; dx < CELL_SIZE; dx++) {
                frame[position] = b;
                frame[position + 1] = g;
                frame[position + 2] = r;
                frame[position + 3] = OPAQUE;
                position += BYTES_PER_PIXEL;
            }
        }
    }

    /**
     * Averages a cell with its 3x3 neighbourhood, so the three colours blend instead
     * of meeting in hard edges.
     * <p>
     * The cell keeps most of its own colour: it counts five times, every neighbour
     * once. At the borders the window is clipped and the average uses only the cells
     * that exist, which softens the edge of the screen.
     *
     * @param cellX    column of the cell, in cells
     * @param cellY    row of the cell, in cells
     * @param cellData channel to sample, as {@code [row][column]}
     * @return smoothed intensity, 0-255
     */
    private int blurredValue(int cellX, int cellY, int[][] cellData) {
        int firstRow = Math.max(0, cellY - 1);
        int lastRow = Math.min(cellHeight - 1, cellY + 1);
        int firstColumn = Math.max(0, cellX - 1);
        int lastColumn = Math.min(cellWidth - 1, cellX + 1);

        int sum = cellData[cellY][cellX] * CENTER_EXTRA_WEIGHT;
        int samples = CENTER_EXTRA_WEIGHT;

        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                sum += cellData[row][column];
                samples++;
            }
        }
        return sum / samples;
    }
}
