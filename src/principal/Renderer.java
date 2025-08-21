package principal;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import java.nio.ByteBuffer;

/**
 * Cellular automaton renderer with three-channel RGB interactions.
 * Each color channel acts as main/enhancer/poison for the others in a rock-paper-scissors pattern.
 * Red: enhanced by blue, poisoned by green
 * Green: enhanced by red, poisoned by blue  
 * Blue: enhanced by green, poisoned by red
 */
class Renderer {
    private static final double INITIAL_PROBABILITY = 0.75;
	private static final int PIXEL_SIZE = 8; // Pixels per cell
 

    private final GraphicsContext gc;
    private final int width;
    private final int height;
    private final int cellWidth;
    private final int cellHeight;
    private final WritableImage estadosImage;
    private final PixelWriter pixelWriter;
    private final Transition transition;
    
    private int[][] cellDataR; // Red channel current generation (0=dead/white, 255=alive/black)
    private int[][] cellDataG; // Green channel current generation
    private int[][] cellDataB; // Blue channel current generation

    private int[][] nextCellDataR; // Red channel next generation buffer
    private int[][] nextCellDataG; // Green channel next generation buffer
    private int[][] nextCellDataB; // Blue channel next generation buffer

    private boolean runningIteration = false;
    private boolean runningStep = false;
    private final byte[] blockBuffer = new byte[PIXEL_SIZE * PIXEL_SIZE * 4]; // Reusable buffer
    
    /**
     * Constructs a new cellular automaton renderer with the specified graphics context and dimensions.
     * Initializes all necessary components including the cell data arrays and transition object.
     * 
     * @param gc the graphics context to draw on (must not be null)
     * @param w  canvas width in pixels (must be positive)
     * @param h  canvas height in pixels (must be positive)
     * @throws IllegalArgumentException if gc is null or dimensions are invalid
     */
    public Renderer(GraphicsContext gc, int w, int h) {
        if (gc == null) {
            throw new IllegalArgumentException("GraphicsContext cannot be null");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }

        this.gc = gc;
        this.width = w;
        this.height = h;
        this.cellWidth = width / PIXEL_SIZE;
        this.cellHeight = height / PIXEL_SIZE;

        // Create writable image and pixel writer for efficient drawing
        estadosImage = new WritableImage(width, height);
        pixelWriter = estadosImage.getPixelWriter();
        
        cellDataR = new int[cellHeight][cellWidth];
        cellDataG = new int[cellHeight][cellWidth];
        cellDataB = new int[cellHeight][cellWidth];
        nextCellDataR = new int[cellHeight][cellWidth];
        nextCellDataG = new int[cellHeight][cellWidth];
        nextCellDataB = new int[cellHeight][cellWidth];
        
        transition = new Transition(this, cellWidth, cellHeight);
    }

    /**
     * Handles user interaction by toggling cells in a 9x9 area around the clicked position.
     * Randomly selects one of the RGB channels and toggles cells between dead/alive states.
     * Dead cells become alive with random intensity, and alive cells become dead.
     * 
     * @param x screen X coordinate where the user clicked
     * @param y screen Y coordinate where the user clicked
     */
    public void toggleCell(int x, int y) {
        int cx = x / PIXEL_SIZE;
        int cy = y / PIXEL_SIZE;
        
        // Randomly select RGB channel
		int channel = (int) (Math.random() * 3);
		
		int[][] cellData = switch(channel) {
			case 0 -> cellDataR;
			case 1 -> cellDataG;
			case 2 -> cellDataB;
			default -> null;
		};
		
		// Toggle cells in 9x9 area around click point
		for (int a = Math.max(0, cx-4); a < Math.min(cellWidth, cx+4); a++) {
			for (int b = Math.max(0, cy-4); b < Math.min(cellHeight, cy+4); b++) {
			    if (cellData[b][a] != 0) 
			    	cellData[b][a] = 0;  // Kill cell
		        else 
		        	cellData[b][a] = (int) (150 + Math.random() * 105); // Birth cell with random intensity
			}
		}
        
        bufferToScreen();
        gc.drawImage(estadosImage, 0, 0);
    }
    
    /**
     * Hook method for additional drawing operations during initialization.
     * Currently just yields the thread and serves as a placeholder for subclasses
     * to add custom drawing operations before the first render.
     */
    public void initialDrawing() {
        System.out.println("Initial Drawing");
        Thread.yield();
    }

    /**
     * Initializes the cellular automaton grid.
     * 
     * @param clear if true, creates empty grid; if false, adds random living cells
     */
    private void initialize(boolean clear) {
        // Initialize all cells as dead (0)
        for (int y = 0; y < cellHeight; y++) {
            for (int x = 0; x < cellWidth; x++) {
                cellDataR[y][x] = 0;
                cellDataG[y][x] = 0;      
                cellDataB[y][x] = 0;      
                if (!clear) {
                	// 25% chance to create living cell in each channel
                	if (Math.random() > INITIAL_PROBABILITY) cellDataR[y][x] = (int) (100 + Math.random() * 155);
                	if (Math.random() > INITIAL_PROBABILITY) cellDataG[y][x] = (int) (100 + Math.random() * 155);
                	if (Math.random() > INITIAL_PROBABILITY) cellDataB[y][x] = (int) (100 + Math.random() * 155);
                }
            }
        }
        
        initialDrawing();
        bufferToScreen();
        gc.drawImage(estadosImage, 0, 0);
        System.out.println("Image Drawn");
    }

    /**
     * Updates the display for one animation frame.
     * Processes a single iteration if the simulation is running continuously
     * or a single step has been requested, then redraws the canvas.
     */
    public void updateRegion() {
    	if (!runningIteration && !runningStep) return;
    	iteration();
    	runningStep = false;
        gc.drawImage(estadosImage, 0, 0);
    }

    /**
     * Executes one complete iteration of the cellular automaton.
     * Calculates the next generation for all three RGB channels based on the current state,
     * applying the rock-paper-scissors interaction rules between color channels.
     * After calculation, swaps the buffers and updates the display.
     */
    public void iteration() {
        for (int y = 0; y < cellHeight; y++) {
            for (int x = 0; x < cellWidth; x++) {
                // Red channel: enhanced by blue, poisoned by green
                nextCellDataR[y][x] = transition.calculateIteration(x, y, cellDataR, cellDataB, cellDataG);                
                // Green channel: enhanced by red, poisoned by blue
                nextCellDataG[y][x] = transition.calculateIteration(x, y, cellDataG, cellDataR, cellDataB);
                // Blue channel: enhanced by green, poisoned by red
                nextCellDataB[y][x] = transition.calculateIteration(x, y, cellDataB, cellDataG, cellDataR);
            }
        }
        
        // Swap current and next generation buffers
        int[][] temp = cellDataR; cellDataR = nextCellDataR; nextCellDataR = temp;
        temp = cellDataG; cellDataG = nextCellDataG; nextCellDataG = temp;
        temp = cellDataB; cellDataB = nextCellDataB; nextCellDataB = temp;
        
        bufferToScreen();
    }
    
    /**
     * Calculates blurred/smoothed value for a cell by averaging with neighbors.
     * 
     * @param x cell X coordinate
     * @param y cell Y coordinate
     * @param cellData the channel data to blur
     * @return blurred intensity value
     */
    private int getBlurredValue(int x, int y, int[][] cellData) {
    	int sum = cellData[y][x]*4;
    	int count = 4;
    	    	
        // Add all 9 cells (including center)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < cellWidth && ny >= 0 && ny < cellHeight) {
                    sum += cellData[ny][nx];
                    count++;
                }
            }
        }
            	
        return sum / count;
    }
    
    /**
     * Renders the cellular automaton data to screen with blur effect.
     * Converts cell data to BGRA pixel format.
     * Each cell is rendered as a PIXEL_SIZE x PIXEL_SIZE block.
     */
    private void bufferToScreen() {
        WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraInstance();
        
        for (int cy = 0; cy < cellHeight; cy++) {
            for (int cx = 0; cx < cellWidth; cx++) {
                for (int i = 0; i < PIXEL_SIZE * PIXEL_SIZE; i++) {
                    int pos = i * 4;
                    // Direct mapping: 0=black, 255=white
                    blockBuffer[pos] =     (byte) getBlurredValue(cx, cy, cellDataB); // Blue
                    blockBuffer[pos + 1] = (byte) getBlurredValue(cx, cy, cellDataG); // Green
                    blockBuffer[pos + 2] = (byte) getBlurredValue(cx, cy, cellDataR); // Red
                    blockBuffer[pos + 3] = (byte) 255;  // Alpha (fully opaque)
                }
                
                // Write pixel block to screen
                int screenX = cx * PIXEL_SIZE;
                int screenY = cy * PIXEL_SIZE;
                if (screenX + PIXEL_SIZE <= width && screenY + PIXEL_SIZE <= height) {
                    pixelWriter.setPixels(screenX, screenY, PIXEL_SIZE, PIXEL_SIZE, 
                                        pixelFormat, blockBuffer, 0, PIXEL_SIZE * 4);
                }
            }
        }
    }
        
    /**
     * Restarts the simulation with a new random initial pattern.
     * Stops any ongoing simulation and initializes the grid with random living cells.
     */
    public void restart() {
    	runningIteration = false;
        initialize(false);
    }
    
    /**
     * Clears the simulation grid by resetting all cells to the dead state.
     * Stops any ongoing simulation and creates an empty grid.
     */
    public void clear() {
    	runningIteration = false;
        initialize(true);
    }

    /**
     * Activates continuous iteration mode.
     * When active, the simulation will continuously run iterations and update the display.
     */
    public void iterate() {
    	runningIteration = true;
    }

    /**
     * Stops the continuous iteration mode.
     * When stopped, the simulation will no longer run iterations automatically.
     */
    public void stopIteration() {
        runningIteration = false;
    }
    /**
     * Performs a single iteration step.
     * Useful for manual control of the simulation progression, allowing users to observe each state change individually.
     */
    public void step() {
    	runningStep = true;
    }
}