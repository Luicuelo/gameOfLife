package principal;

/**
 * Defines transition rules for three-channel cellular automaton.
 * Implements Game of Life with cross-channel interactions where each channel
 * can enhance or poison the others in a rock-paper-scissors pattern.
 */
class Transition {

    private final int width;
    private final int height;
    
    /**
     * Creates a new transition rule calculator for the cellular automaton.
     * 
     * @param renderer the renderer this transition is associated with
     * @param width the width of the grid
     * @param height the height of the grid
     */
    public Transition(Renderer renderer, int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Calculates the next state for a cell based on Game of Life rules with channel interactions.
     * 
     * @param x cell X coordinate
     * @param y cell Y coordinate  
     * @param mainData the primary channel data for this cell
     * @param enhancerData channel that enhances this cell's survival/birth
     * @param poisonData channel that hinders this cell's survival/birth
     * @return new cell value (0-255)
     */
    public int calculateIteration(int x, int y, int[][] mainData, int[][] enhancerData, int[][] poisonData) {
    	
    	boolean currentCellAlive = mainData[y][x] != 0;
    	 
        int neighborCount = getAvgNeighbors(x, y, mainData);
        int enhancerInfluence = enhancerData[y][x] / 10;  // Enhancer effect from same position
        int poisonInfluence = poisonData[y][x] / 10;      // Poison effect from same position
        
        // Calculate modified cell value with channel influences
        int originalValue = (int) (mainData[y][x] + (enhancerInfluence) - poisonInfluence);
        if (originalValue > 255) originalValue = 255;

        // Game of Life rules with channel influence modifiers
        return switch(neighborCount) {
            case 0 -> currentCellAlive ? Math.max(0, originalValue - 10) : 0;  // Underpopulation
            case 1 -> currentCellAlive ? Math.max(0, originalValue - 20) : enhancerInfluence;  // Underpopulation
            case 2 -> currentCellAlive ? Math.max(0, originalValue) : enhancerInfluence;       // Survival
            case 3 -> currentCellAlive ? Math.max(0, originalValue) : 255; // Survival/Birth
            case 4 -> currentCellAlive ? Math.max(0, originalValue - 40) : enhancerInfluence;  // Overpopulation
            case 5 -> currentCellAlive ? Math.max(0, originalValue - 50) : enhancerInfluence;  // Overpopulation
            case 6 -> currentCellAlive ? Math.max(0, originalValue - 60) : enhancerInfluence;  // Overpopulation
            default -> 0;  // Death
        };
    }

    /**
     * Calculates average neighbor intensity and converts to neighbor count scale (0-8).
     * 
     * @param x cell X coordinate
     * @param y cell Y coordinate
     * @param cellData channel data to analyze
     * @return neighbor count equivalent (0-8 scale)
     */
    private int getAvgNeighbors(int x, int y, int[][] cellData) {
        int sum = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip center cell
                
                int nx = x + dx;
                int ny = y + dy;
                
                // Validate bounds
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {                
                    sum += cellData[ny][nx];
                }
            }
        }
        return (int) (sum/255); // Convert sum to 0-8 neighbor count scale
    }
}