package principal;

/**
 * Rules of the three channel automaton.
 * <p>
 * All three channels share the same grid, so the interaction between colours
 * does not happen through neighbours: it happens through the value of the very
 * same cell. Each channel reads the cell in an <em>enhancer</em> channel (which
 * brightens it) and the cell in a <em>poison</em> channel (which darkens it),
 * following a rock-paper-scissors cycle:
 *
 * <pre>
 * channel   enhanced by   poisoned by
 * red       blue          green
 * green     red           blue
 * blue      green         red
 * </pre>
 *
 * <p>
 * The neighbour term is the <em>neighbour density</em>: the intensity of the
 * eight neighbours added up and divided by 255 (see {@link #neighborDensity}).
 * Eight neighbours of intensity 100 count as "3", enough to give birth. With
 * {@code value = min(255, main + enhancer / 20 - poison / 20)}, the rules are:
 *
 * <pre>
 * density  | living cell          | dead cell
 * ---------+----------------------+----------------------------
 *     0    | max(0, value - 30)   | 0                (fades out)
 *     1    | max(0, value - 20)   | 0
 *     2    | max(0, value)        | enhancer / 20    (0-12)
 *     3    | max(0, value)        | 255              (full birth)
 *     4    | max(0, value - 40)   | enhancer / 20    (0-12)
 *     5    | max(0, value - 50)   | enhancer / 20    (0-12)
 *     6    | max(0, value - 60)   | enhancer / 20    (0-12)
 *   7, 8   | 0 (dies)             | 0
 * </pre>
 *
 * <p>
 * Three details of that table look odd and are the reason the simulation works:
 * <ul>
 *   <li>Only density 3 gives birth at full intensity. The births at densities 2,
 *       4, 5 and 6 arrive almost black (12 at most), in front of a patch where the
 *       cell already has some activity of its own colour. Those faint cells are the
 *       expansion engine: they germinate at the front and then, at density 2, the
 *       enhancer channel pushes them up by 12 per generation until they are bright.
 *       A single neighbour is not enough to start a new patch.</li>
 *   <li>A dead cell at density 0 never appears, so nothing germinates where the
 *       channel is completely dark: growth stays on its own fronts instead of
 *       filling the grid.</li>
 *   <li>A living cell never dies of isolation, it fades: 30 per generation when it
 *       is completely alone, 20 with a single neighbour.</li>
 * </ul>
 * <p>
 * All these numbers are tuned, not derived, and the automaton sits at a critical
 * point. The divisor of the influences is the knob that moves that point:
 * {@link #INFLUENCE_DIVISOR} softens both the enhancer and the poison, the fronts
 * advance with less energy and the grid is much harder to push into white noise,
 * at the price of a slower spread. Pushing the other way is what fails: counting
 * neighbours instead of weighing their intensity, or rounding the density up,
 * turns the grid into white noise. Taking the faint births or the free survival
 * at density 2 away makes it die out. Change one number at a time.
 */
final class Transition {

    private static final int MAX_INTENSITY = 255;
    /**
     * Divisor of the enhancer and the poison, so one influence is worth 12 at most.
     * Raising it softens both and moves the automaton away from white noise;
     * lowering it gives the fronts more energy and the grid saturates.
     */
    private static final int INFLUENCE_DIVISOR = 20;

    private final int width;
    private final int height;

    /**
     * Creates a rule calculator for a grid of the given size.
     *
     * @param width  grid width, in cells
     * @param height grid height, in cells
     */
    Transition(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Calculates the next intensity of a cell.
     *
     * @param cellX        column of the cell, in cells
     * @param cellY        row of the cell, in cells
     * @param mainData     channel that owns the cell
     * @param enhancerData channel that brightens it
     * @param poisonData   channel that darkens it
     * @return new intensity (0-255)
     */
    int calculateIteration(int cellX, int cellY, int[][] mainData, int[][] enhancerData, int[][] poisonData) {
        boolean alive = mainData[cellY][cellX] != 0;

        int density = neighborDensity(cellX, cellY, mainData);
        int enhancerInfluence = enhancerData[cellY][cellX] / INFLUENCE_DIVISOR;
        int poisonInfluence = poisonData[cellY][cellX] / INFLUENCE_DIVISOR;

        int value = mainData[cellY][cellX] + enhancerInfluence - poisonInfluence;
        if (value > MAX_INTENSITY) {
            value = MAX_INTENSITY;
        }

        // Tuned by eye: the faint births (enhancerInfluence) and the free survival at
        // density 2 are what let a patch spread, see the class documentation.
        return switch (density) {
            case 0 -> alive ? Math.max(0, value - 30) : 0;                 // isolation
            case 1 -> alive ? Math.max(0, value - 20) : 0;                 // isolation
            case 2 -> alive ? Math.max(0, value) : enhancerInfluence;      // survival
            case 3 -> alive ? Math.max(0, value) : MAX_INTENSITY;          // survival / birth
            case 4 -> alive ? Math.max(0, value - 40) : enhancerInfluence; // crowding
            case 5 -> alive ? Math.max(0, value - 50) : enhancerInfluence; // crowding
            case 6 -> alive ? Math.max(0, value - 60) : enhancerInfluence; // crowding
            default -> 0;                                                  // suffocation
        };
    }

    /**
     * Measures how much activity surrounds a cell: the intensity of the eight
     * neighbours, expressed in units of a full cell (255) and truncated, so the
     * result lands in the 0-8 range of the rules.
     * <p>
     * It adds the whole 3x3 window, clipped at the borders, and then takes the cell
     * itself out, so no guard has to live inside the loop.
     *
     * @param cellX    column of the cell, in cells
     * @param cellY    row of the cell, in cells
     * @param cellData channel to measure
     * @return neighbour density, 0-8
     */
    private int neighborDensity(int cellX, int cellY, int[][] cellData) {
        int firstRow = Math.max(0, cellY - 1);
        int lastRow = Math.min(height - 1, cellY + 1);
        int firstColumn = Math.max(0, cellX - 1);
        int lastColumn = Math.min(width - 1, cellX + 1);

        int sum = 0;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                sum += cellData[row][column];
            }
        }
        sum -= cellData[cellY][cellX]; // the cell itself is not a neighbour

        return sum / MAX_INTENSITY;
    }
}
