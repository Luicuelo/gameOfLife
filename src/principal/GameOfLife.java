package principal;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * JavaFX front end of the three channel Game of Life.
 * <p>
 * This class owns the window, the buttons and the clock: it turns the pulses of
 * an {@link AnimationTimer} into fixed rate ticks (see {@link FrameTimer}), so
 * the simulation runs at 60 ticks per second on any machine and any display.
 * The speed selector multiplies the generations simulated in each tick, from one
 * (60 per second) up to {@value #FASTEST_SPEED} times that, without changing the
 * rate of the clock itself. The state lives in {@link Simulation}, the rules in
 * {@link Transition} and the drawing in {@link Renderer}.
 * <p>
 * Command line options: {@code --width=<pixels>} and {@code --height=<pixels>}.
 * Both are rounded down to a multiple of {@value Renderer#CELL_SIZE} and clamped
 * to a minimum, see {@link #parseSize(Application.Parameters)}.
 */
public class GameOfLife extends Application {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int BUTTON_HEIGHT = 25;
    /** Below these sizes the button bar and the status label would not fit. */
    private static final int MIN_CANVAS_WIDTH = 720;
    private static final int MIN_CANVAS_HEIGHT = 80;
    /** Multipliers offered by the speed selector, in generations per tick. */
    private static final Integer[] SPEEDS = {1, 2, 3, 4, 6, 8, 12, 16};
    /** Largest multiplier of the speed selector. */
    private static final int FASTEST_SPEED = 16;

    /** Passes the pulses of the animation loop through a fixed rate clock. */
    private final FrameTimer frameTimer = new FrameTimer();

    private int canvasWidth;
    private int canvasHeight;
    private Canvas canvas;
    private Simulation simulation;
    private Renderer renderer;
    private AnimationTimer animationTimer;
    private Label status;

    /** True while the simulation advances on its own. */
    private boolean running;
    /** Set by the Step button, consumed by the next pulse. */
    private boolean stepRequested;
    /** Generations simulated per tick; 1 keeps the 60 per second of the clock. */
    private int generationsPerTick = 1;
    /** Pulses seen since launch; never reset, used only for diagnostics. */
    private long frameCounter;

    /**
     * Launches the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Builds the window and starts the clock.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        parseSize(getParameters());
        initialize();

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setBottom(createButtonBar());

        Scene scene = new Scene(root, renderer.width(), renderer.height() + BUTTON_HEIGHT);
        primaryStage.setTitle("Game of Life - RGB channels");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        repaint();
        startAnimation();
    }

    /**
     * Reads the size arguments, in pixels, and snaps them to the cell grid so the
     * canvas is always covered exactly.
     * <p>
     * A value that is not a number or not positive falls back to the default for
     * that dimension, and a value below the minimum is raised to it; the canvas may
     * end up a few pixels smaller than asked, never larger.
     *
     * @param params the parameters to parse
     */
    private void parseSize(Parameters params) {
        canvasWidth = snap(readSize(params, "width", DEFAULT_WIDTH), MIN_CANVAS_WIDTH);
        canvasHeight = snap(readSize(params, "height", DEFAULT_HEIGHT), MIN_CANVAS_HEIGHT);
    }

    /**
     * Reads one named pixel size.
     *
     * @param params   the parameters to parse
     * @param name     name of the option, without the dashes
     * @param fallback value used when the option is missing or invalid
     * @return the requested size, or the fallback
     */
    private static int readSize(Parameters params, String name, int fallback) {
        String value = params.getNamed().get(name);
        if (value == null) {
            return fallback;
        }
        try {
            int pixels = Integer.parseInt(value);
            if (pixels > 0) {
                return pixels;
            }
            System.err.println("--" + name + " must be positive, using " + fallback);
        } catch (NumberFormatException e) {
            System.err.println("--" + name + " is not a whole number, using " + fallback);
        }
        return fallback;
    }

    /**
     * Rounds a pixel size down to a whole number of cells and raises it to the
     * minimum.
     *
     * @param pixels  requested size, in pixels
     * @param minimum smallest accepted size, itself a multiple of the cell size
     * @return a multiple of {@value Renderer#CELL_SIZE}, at least {@code minimum}
     */
    private static int snap(int pixels, int minimum) {
        int cells = pixels / Renderer.CELL_SIZE;
        return Math.max(minimum, cells * Renderer.CELL_SIZE);
    }

    /** Creates the simulation, the canvas that shows it and the mouse handling. */
    private void initialize() {
        simulation = new Simulation(canvasWidth / Renderer.CELL_SIZE, canvasHeight / Renderer.CELL_SIZE);
        canvas = new Canvas(canvasWidth, canvasHeight);
        renderer = new Renderer(canvas.getGraphicsContext2D(), simulation);
        canvas.setOnMousePressed(this::onMousePressed);
    }

    /**
     * Handles a click on the canvas: paints or erases a 9x9 square of cells.
     *
     * @param event the mouse event, in canvas coordinates
     */
    private void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        simulation.toggle((int) (event.getX() / Renderer.CELL_SIZE),
                (int) (event.getY() / Renderer.CELL_SIZE));
        repaint();
    }

    /**
     * Builds the bottom bar: the five commands, the smoothing switch, the speed
     * selector and the status label.
     *
     * @return the bar, ready to be placed at the bottom of the window
     */
    private HBox createButtonBar() {
        status = new Label();

        ToggleButton smooth = new ToggleButton("Smooth");
        smooth.setSelected(true);
        smooth.setPrefHeight(BUTTON_HEIGHT);
        smooth.setOnAction(e -> {
            renderer.setSmoothing(smooth.isSelected());
            repaint();
        });

        ChoiceBox<Integer> speed = new ChoiceBox<>(FXCollections.observableArrayList(SPEEDS));
        speed.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer generations) {
                return "x" + generations;
            }

            @Override
            public Integer fromString(String text) {
                return Integer.valueOf(text.substring(1));
            }
        });
        speed.setValue(generationsPerTick);
        speed.setPrefHeight(BUTTON_HEIGHT);
        speed.valueProperty().addListener((observable, previous, chosen) -> {
            if (chosen != null) {
                generationsPerTick = chosen;
            }
        });

        HBox bar = new HBox(10,
                createButton("Restart", this::restart),
                createButton("Iterate", this::iterate),
                createButton("Stop", this::stopIteration),
                createButton("Step", this::step),
                createButton("Clear", this::clear),
                smooth,
                speed,
                status);
        bar.setPadding(new Insets(0, 10, 0, 10));
        return bar;
    }

    /**
     * Creates a button of the standard height.
     *
     * @param text   label of the button
     * @param action what the button does
     * @return the configured button
     */
    private Button createButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefHeight(BUTTON_HEIGHT);
        button.setOnAction(e -> action.run());
        return button;
    }

    /**
     * Starts the animation loop, the only place where the simulation advances.
     * <p>
     * Each pulse asks the clock how many ticks are due and simulates one generation
     * per tick, times the speed selected in the bar; the canvas is repainted once, at
     * the end. When the game is paused only an explicit Step request advances it, and
     * always exactly one generation, whatever the speed. Any runtime error pauses the
     * game and reports it in the status label, so the window stays usable.
     */
    private void startAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                frameCounter++;
                int ticks = frameTimer.ticksFor(now);
                int generations = running ? ticks * generationsPerTick : (stepRequested ? 1 : 0);
                stepRequested = false;

                if (generations == 0) {
                    return;
                }
                try {
                    for (int i = 0; i < generations; i++) {
                        simulation.advance();
                    }
                    repaint();
                } catch (RuntimeException e) {
                    pause("Error at pulse " + frameCounter + ": " + e.getMessage());
                }
            }
        };
        animationTimer.start();
    }

    /** Draws the current generation and refreshes the status label. */
    private void repaint() {
        renderer.render();
        updateStatus();
    }

    /** Shows the generation reached and whether the game is running. */
    private void updateStatus() {
        status.setText("Generation " + simulation.generation()
                + (running ? " (running)" : " (paused)"));
    }

    /** Creates a new random pattern and pauses, so the user can inspect it. */
    private void restart() {
        simulation.randomize();
        running = false;
        stepRequested = false;
        repaint();
    }

    /** Starts continuous iteration. */
    private void iterate() {
        running = true;
        stepRequested = false;
        frameTimer.reset();
        updateStatus();
    }

    /** Stops continuous iteration; a pending Step is discarded too. */
    private void stopIteration() {
        running = false;
        stepRequested = false;
        updateStatus();
    }

    /** Requests exactly one generation on the next pulse. */
    private void step() {
        stepRequested = true;
    }

    /** Empties the grid and pauses. */
    private void clear() {
        simulation.clear();
        running = false;
        stepRequested = false;
        repaint();
    }

    /**
     * Pauses the game after an unexpected failure and shows the reason in the
     * status label instead of killing the animation loop for good.
     *
     * @param message what went wrong
     */
    private void pause(String message) {
        running = false;
        stepRequested = false;
        System.err.println(message);
        if (status != null) {
            status.setText(message);
        }
    }

    /** Stops the animation loop before the window closes. */
    @Override
    public void stop() {
        running = false;
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
}
