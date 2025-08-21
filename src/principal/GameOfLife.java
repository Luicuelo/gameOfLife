package principal;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Main JavaFX application.
 * <p>
 * Features:
 * 
 * <p>
 */
public class GameOfLife extends Application {
	private static final int DEFAULT_WIDTH = 800;
	private static final int DEFAULT_HEIGHT = 600;
	private static final int BUTTON_HEIGHT = 25;

	private int canvasWidth;
	private int canvasHeight;
	private Canvas canvas;
	private Renderer renderer;
	private AnimationTimer animationTimer;
	private boolean finished = false;

	private double frameCounter = 0;
	private static final int FRAME_SKIP = 1; // Update every FRAME_SKIP

	/**
	 * Main method to launch the JavaFX application.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Starts the JavaFX application and initializes the user interface.
	 * 
	 * @param primaryStage the primary stage for this application
	 */
	@Override
	public void start(Stage primaryStage) {
		// Parse command line arguments for size
		Parameters params = getParameters();
		parseSize(params);

		primaryStage.setTitle("EstadosFX");

		// Create and initialize
		initialize();

		// Setup mouse listeners
		setupMouseListeners();

		// Create the restart button
		Button restartButton = new Button("Restart");
		restartButton.setOnAction(e -> restart());
		restartButton.setPrefHeight(BUTTON_HEIGHT);

		// Create the Iterate
		Button iterateButton = new Button("Iterate");
		iterateButton.setOnAction(e -> iterate());
		iterateButton.setPrefHeight(BUTTON_HEIGHT);

        // Create the Stop
        Button stopButton  = new Button("Stop");
        stopButton.setOnAction(e -> stopIteration());
        stopButton.setPrefHeight(BUTTON_HEIGHT);

		// Create the step
		Button stepButton = new Button("Step");
		stepButton.setOnAction(e -> step());
		stepButton.setPrefHeight(BUTTON_HEIGHT);
		
		// Create the clear
		Button clearButton = new Button("Clear");
		clearButton.setOnAction(e -> clear());
		clearButton.setPrefHeight(BUTTON_HEIGHT);

		// Layout
		BorderPane root = new BorderPane();
		root.setCenter(canvas);

		HBox buttons = new HBox();
		root.setBottom(buttons);
		buttons.setPadding(new Insets(0, 10, 0, 0));
		buttons.setSpacing(10);
		buttons.getChildren().addAll(restartButton, iterateButton,stopButton,stepButton,clearButton);

		// Create scene and show
		Scene scene = new Scene(root, canvasWidth, canvasHeight + BUTTON_HEIGHT);
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	/**
	 * Parses command line arguments for canvas size.
	 * 
	 * @param params the parameters to parse
	 */
	private void parseSize(Parameters params) {
		try {
			if (params.getNamed().containsKey("width")) {
				canvasWidth = Integer.parseInt(params.getNamed().get("width"));
			} else {
				canvasWidth = DEFAULT_WIDTH;
			}

			if (params.getNamed().containsKey("height")) {
				canvasHeight = Integer.parseInt(params.getNamed().get("height"));
			} else {
				canvasHeight = DEFAULT_HEIGHT;
			}
		} catch (NumberFormatException e) {
			System.err.println("Invalid size parameters, using defaults");
			canvasWidth = DEFAULT_WIDTH;
			canvasHeight = DEFAULT_HEIGHT;
		}
	}

	/**
	 * Initializes the canvas and starts the animation loop. Sets up the graphics
	 * context and begins 60 FPS rendering.
	 */
	private void initialize() {
		canvas = new Canvas(canvasWidth, canvasHeight);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		finished = false;

		//gc.setFill(Color.RED);
		//gc.fillText("Generating .....", 100, 100);
		renderer = new Renderer(gc, canvasWidth, canvasHeight);
		animationTimer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (!finished && renderer != null) {
					frameCounter++;
					if (frameCounter>=Double.MAX_VALUE)frameCounter=0;
					if (frameCounter % FRAME_SKIP == 0) {
						try {
							renderer.updateRegion();
						} catch (Exception e) {
							System.err.println("Error updating : " + e.getMessage());
							stopAnimation();
						}
					}
				}
			}
		};

		renderer.clear();
		animationTimer.start();
	}

	/**
	 * Configures mouse event handlers for interactive effects. Tracks mouse
	 * movement, entry, and exit events on the canvas.
	 */
	private void setupMouseListeners() {

		canvas.setOnMousePressed(e -> {
			if (renderer != null&& e.getButton()==MouseButton.PRIMARY) {
				
			renderer.toggleCell((int)e.getX(),(int) e.getY());
			}
		});

	}

	/**
	 * Restarts the simulation with a new random pattern.
	 */
	private void restart() {
		System.out.println("Restarting ...");
		if (renderer != null) {
			renderer.restart();
		}
	}

	/**
	 * Starts continuous iteration mode for the simulation.
	 */
	private void iterate() {
		System.out.println("Iterating ...");
		if (renderer != null) {
			renderer.iterate();
		}
	}

    /**
     * Stops the simulation.
     */
    private void stopIteration() {
        System.out.println("stop ...");
        if (renderer != null) {
            renderer.stopIteration();
        }
    }


	/**
	 * Performs a single iteration step of the simulation.
	 */
	private void step() {
		System.out.println("step ...");
		if (renderer != null) {
			renderer.step();
		}
	}
	
	/**
	 * Clears the grid, setting all cells to dead.
	 */
	private void clear() {
		System.out.println("clear ...");
		if (renderer != null) {
			renderer.clear();
		}
	}




	/**
	 * Stops the animation timer and cleans up resources.
	 */
	private void stopAnimation() {
		finished = true;
		if (animationTimer != null) {
			animationTimer.stop();
		}
	}

	/**
	 * Stops the application and cleans up resources before exiting.
	 */
	@Override
	public void stop() {
		stopAnimation();
		renderer = null;
		System.out.println(" destroyed");
		Platform.exit();
	}
}