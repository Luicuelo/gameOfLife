# Game of Life with RGB Channels

A JavaFX implementation of Conway's Game of Life with a colourful twist: instead of
one grid of dead and alive cells there are three grids, one per RGB channel, and each
channel interacts with the other two in a rock-paper-scissors cycle.

- **red** is enhanced by **blue** and poisoned by **green**
- **green** is enhanced by **red** and poisoned by **blue**
- **blue** is enhanced by **green** and poisoned by **red**

The result is a simulation where coloured regions grow, starve and chase each other
around the screen.

## Requirements

| What | Version |
| --- | --- |
| JDK | 17 or newer (the code uses switch expressions) |
| JavaFX SDK | 17 or newer, tested with 21.0.8 |

You only need the `javafx.controls` module (`javafx.fxml` is not used). Download the
SDK from <https://gluonhq.com/products/javafx/> and unzip it anywhere; the examples
below use `C:\Program Files\Java\javafx-sdk-21.0.8\lib`, **but the path is yours to
choose**: the project does not bundle JavaFX, so every developer points the build at
their own copy. Nothing else is needed: there is no Maven or Gradle build, just
`javac` and `java`.

## Build and run

All commands are run from the root of the repository.

### Windows, cmd

```bat
set FX_LIB=C:\Program Files\Java\javafx-sdk-21.0.8\lib
javac --module-path "%FX_LIB%" --add-modules javafx.controls -d out src\principal\*.java
java  --module-path "%FX_LIB%" --add-modules javafx.controls -cp out principal.GameOfLife
```

### Windows, PowerShell

```powershell
$FX_LIB = "C:\Program Files\Java\javafx-sdk-21.0.8\lib"
javac --module-path "$FX_LIB" --add-modules javafx.controls -d out (Get-ChildItem src\principal\*.java).FullName
java  --module-path "$FX_LIB" --add-modules javafx.controls -cp out principal.GameOfLife
```

### Linux, macOS

```bash
export FX_LIB=/opt/javafx-sdk-21.0.8/lib
javac --module-path "$FX_LIB" --add-modules javafx.controls -d out src/principal/*.java
java  --module-path "$FX_LIB" --add-modules javafx.controls -cp out principal.GameOfLife
```

Classes are written to `out/`, which is already covered by `.gitignore` (`*.class`).

### Running from an IDE

`javafx` is not on the default classpath, so the run configuration has to pass the
`--module-path` above. The files in `.vscode/` and the IntelliJ module file contain an
absolute path to a JavaFX SDK as an example; replace it with yours. `/out` is the
project output directory in both IDEs.

## Command line options

```
principal.GameOfLife [--width=<pixels>] [--height=<pixels>]
```

- Defaults are `800x600`.
- Sizes are rounded **down** to a multiple of 8, the size of one cell, so the grid
  always covers the canvas exactly (`--width=999` becomes 992).
- Sizes are clamped to a minimum of `720x80`; smaller values are raised, because the
  button bar and the status label need the room.
- A value that is not a whole number or not positive is ignored with a message on
  `stderr` and replaced by the default.

## Controls

| Control | Effect |
| --- | --- |
| **Mouse click** (left button) | Paints or erases a 9x9 square of cells in a random channel |
| **Restart** | Creates a new random pattern and pauses the simulation |
| **Iterate** | Starts the continuous simulation |
| **Stop** | Pauses the continuous simulation |
| **Step** | Advances exactly one generation |
| **Clear** | Empties the grid and pauses |
| **Smooth** | Toggles the smoothing of the cells; it applies immediately, even while the simulation runs |
| **x1 ... x16** | How many generations are simulated per tick of the clock: `x1` is 60 per second, `x16` is 960. It can be changed while the simulation runs, and **Step** still advances exactly one generation |

The label on the right shows the generation reached and whether the simulation is
running. The application starts with an empty grid: press **Restart** for a random
pattern or click on the canvas to draw your own.

## Rules

Each cell holds three intensities, one per channel, from 0 (empty) to 255 (full). Only
the *main* channel of a cell is used to decide whether the cell is alive; the other two
channels change its value at the same position:

```
value = min(255, main + enhancer / 20 - poison / 20)
```

The neighbour term is not the number of living neighbours, but their **total intensity
divided by 255**, the *neighbour density*. Eight neighbours at intensity 100 count as
`3`, which is enough to give birth; a lone neighbour at full intensity counts as `1`.

| neighbour density (0-8) | living cell | dead cell |
| --- | --- | --- |
| 0 | `max(0, value - 30)` (fades out) | 0 |
| 1 | `max(0, value - 20)` | 0 |
| 2 | `max(0, value)` | `enhancer / 20` (0-12) |
| 3 | `max(0, value)` | `255` (full birth) |
| 4 | `max(0, value - 40)` | `enhancer / 20` (0-12) |
| 5 | `max(0, value - 50)` | `enhancer / 20` (0-12) |
| 6 | `max(0, value - 60)` | `enhancer / 20` (0-12) |
| 7, 8 | `0` (dies) | 0 |

### Why the numbers look odd

Three details of the table are what make the simulation work:

- **The faint births are the engine.** Only density 3 is born at full intensity. The
  births at densities 2, 4, 5 and 6 arrive almost black (12 at most), at the front of
  a patch where the cell already has some activity of its own colour. At density 2
  they are not penalised, so the enhancer channel pushes them up by 12 per generation
  until they are bright. That is how a patch spreads: germinating at the front,
  maturing, and opening room for the next row. A single neighbour is never enough to
  start a new patch.
- **A dead cell at density 0 never appears**, so nothing germinates where the channel
  is completely dark: the growth stays on its own fronts instead of filling the grid.
- **A living cell never dies of isolation, it fades**: 30 per generation when it is
  completely alone, 20 with a single neighbour. That is what keeps loose debris from
  hanging around.

All of this is tuned by eye, not derived, and the automaton sits at a critical point.
The divisor of the influences is the knob that moves that point: raising it softens
both the enhancer and the poison, so the fronts advance with less energy and the grid
is much harder to push into white noise, at the price of a slower spread (which is why
the speed selector exists). Pushing the other way is what fails: count neighbours
instead of weighing their intensity, or round the density up, and the entire grid
turns into white noise; take the faint births or the free survival at density 2 away
and the simulation dies out. Change one number at a time.

## Architecture

| Class | Responsibility |
| --- | --- |
| `GameOfLife` | JavaFX window, buttons, status label and the animation loop. Owns no rules. |
| `FrameTimer` | Turns the pulses of the animation loop into a fixed rate of 60 ticks per second. |
| `Simulation` | The engine: the six buffers, the channel cycle, `advance()`, `clear()`, `randomize()`, `toggle()`. No JavaFX. |
| `Transition` | The rules: given a cell and its neighbours, returns the next intensity. |
| `Renderer` | The view: composes the frame in a `byte[]` and paints it on the canvas. |

A pulse of the animation loop goes through `FrameTimer` (how many generations are
due?) to `Simulation.advance()` (calculate them) and finally to `Renderer.render()`
(draw the last one). Everything runs on the JavaFX thread, so the buffers never need
synchronisation.

### Implementation notes

- **Speed does not depend on the machine or the monitor.** `AnimationTimer` calls back
  once per frame, which is 60 times per second on one display and 144 on another.
  `FrameTimer` accumulates the elapsed nanoseconds and returns the whole number of
  16.6 ms ticks that are due, capping a single burst at 4 ticks so a long pause does
  not fast-forward the game. The speed selector multiplies those ticks (`x1` is 60
  generations per second, `x16` is 960), so it changes how much the game advances,
  never how often it is asked to.
- **One repaint per pulse, whatever the speed.** The generations of a pulse are
  simulated in a loop and the canvas is painted once at the end, so a high multiplier
  means bigger jumps between frames, not more work for the drawing code.
- **One `setPixels` per frame.** The whole canvas is written into a reusable
  `byte[]` in BGRA order and pushed to the `WritableImage` in a single call, instead
  of one call per cell.
- **Smoothing is optional.** The **Smooth** button switches between the two looks on
  the fly. When it is on, each cell is drawn as an 8x8 block whose colour is a 3x3
  weighted average: the cell counts 5 times and each neighbour once, and cells outside
  the grid are left out of the average, which slightly softens the borders. When it is
  off, every block is drawn with the raw intensity of its cell.
- **Errors do not kill the window.** If a frame fails, the simulation pauses and the
  reason appears in the status label; the buttons keep working.

## License

This project is licensed under the MIT License, see the [LICENSE](LICENSE) file.

## Acknowledgments

- Based on John Conway's Game of Life.
- Inspired by colourful cellular automata implementations.
