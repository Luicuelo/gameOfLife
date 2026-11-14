package principal;

import java.util.Arrays;

/**
 * State and rules of the three channel automaton; this is the engine, with no
 * JavaFX in it.
 * <p>
 * The grid is the same for the three colours: cell {@code (x, y)} holds three
 * intensities (0 = empty, 255 = full). {@link Transition} decides the next value
 * of each channel using the same position of the two other channels, so the
 * only thing that differs between channels is which one enhances them and which
 * one poisons them.
 */
final class Simulation {

    /** Channel indexes, used by the arrays below and by the renderer. */
    static final int RED = 0;
    static final int GREEN = 1;
    static final int BLUE = 2;
    static final int CHANNELS = 3;

    /**
     * Rock-paper-scissors cycle: {@code ENHANCED_BY[c]} brightens channel
     * {@code c} and {@code POISONED_BY[c]} darkens it.
     */
    private static final int[] ENHANCED_BY = {BLUE, RED, GREEN};
    private static final int[] POISONED_BY = {GREEN, BLUE, RED};

    /** A random cell stays empty when the draw is above this value: 25% are born. */
    private static final double EMPTY_THRESHOLD = 0.75;
    /** Intensity range of the cells created by {@link #randomize()}. */
    private static final int RANDOM_BIRTH_BASE = 100;
    private static final int RANDOM_BIRTH_SPREAD = 155;
    /** Intensity range of the cells painted by {@link #toggle(int, int)}. */
    private static final int CLICK_BIRTH_BASE = 150;
    private static final int CLICK_BIRTH_SPREAD = 105;
    /** {@link #toggle(int, int)} works on a 9x9 square: radius 4 in cell units. */
    private static final int CLICK_RADIUS = 4;

    private final int width;
    private final int height;
    private final Transition transition;

    /** Read buffers, one grid per channel. */
    private final int[][][] current;
    /** Write buffers; swapped with {@link #current} at the end of every tick. */
    private final int[][][] next;

    private long generation;

    /**
     * Creates an empty simulation.
     *
     * @param width  grid width, in cells
     * @param height grid height, in cells
     * @throws IllegalArgumentException if any dimension is not positive
     */
    Simulation(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid must be at least 1x1");
        }
        this.width = width;
        this.height = height;
        this.current = new int[CHANNELS][height][width];
        this.next = new int[CHANNELS][height][width];
        this.transition = new Transition(width, height);
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    /** Ticks advanced since the last {@link #clear()} or {@link #randomize()}. */
    long generation() {
        return generation;
    }

    /**
     * Returns one channel of the current generation, as {@code [y][x]}.
     * <p>
     * The array is owned by the simulation and is replaced by {@link #advance()};
     * read it and forget it, never keep a reference.
     *
     * @param channel {@link #RED}, {@link #GREEN} or {@link #BLUE}
     */
    int[][] channel(int channel) {
        return current[channel];
    }

    /**
     * Advances the automaton by one generation: every channel is calculated from
     * the current buffers into the next ones, and the two sets are swapped.
     */
    void advance() {
        for (int channel = 0; channel < CHANNELS; channel++) {
            int[][] main = current[channel];
            int[][] enhancer = current[ENHANCED_BY[channel]];
            int[][] poison = current[POISONED_BY[channel]];
            int[][] result = next[channel];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result[y][x] = transition.calculateIteration(x, y, main, enhancer, poison);
                }
            }
        }

        for (int channel = 0; channel < CHANNELS; channel++) {
            int[][] swap = current[channel];
            current[channel] = next[channel];
            next[channel] = swap;
        }
        generation++;
    }

    /** Empties every channel and resets the generation counter. */
    void clear() {
        for (int channel = 0; channel < CHANNELS; channel++) {
            for (int[] row : current[channel]) {
                Arrays.fill(row, 0);
            }
        }
        generation = 0;
    }

    /** Empties every channel and fills ~25% of each one with random intensities. */
    void randomize() {
        clear();
        for (int channel = 0; channel < CHANNELS; channel++) {
            int[][] data = current[channel];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (Math.random() > EMPTY_THRESHOLD) {
                        data[y][x] = randomIntensity(RANDOM_BIRTH_BASE, RANDOM_BIRTH_SPREAD);
                    }
                }
            }
        }
    }

    /**
     * Paints a 9x9 square of cells around the given cell, in a randomly chosen
     * channel: empty cells are born, living ones are killed.
     *
     * @param cellX column of the clicked cell, in cell units
     * @param cellY row of the clicked cell, in cell units
     */
    void toggle(int cellX, int cellY) {
        if (cellX < 0 || cellX >= width || cellY < 0 || cellY >= height) {
            return;
        }

        int[][] data = current[(int) (Math.random() * CHANNELS)];
        int fromX = Math.max(0, cellX - CLICK_RADIUS);
        int toX = Math.min(width - 1, cellX + CLICK_RADIUS);
        int fromY = Math.max(0, cellY - CLICK_RADIUS);
        int toY = Math.min(height - 1, cellY + CLICK_RADIUS);

        for (int y = fromY; y <= toY; y++) {
            for (int x = fromX; x <= toX; x++) {
                data[y][x] = data[y][x] != 0
                        ? 0
                        : randomIntensity(CLICK_BIRTH_BASE, CLICK_BIRTH_SPREAD);
            }
        }
    }

    /** Random intensity in {@code [base, base + spread)}, always below 255. */
    private static int randomIntensity(int base, int spread) {
        return base + (int) (Math.random() * spread);
    }
}
