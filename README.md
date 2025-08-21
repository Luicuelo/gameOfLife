# Game of Life with RGB Channels

A JavaFX implementation of Conway's Game of Life with a colorful twist. This implementation features a three-channel cellular automaton where each RGB color channel interacts with the others in a rock-paper-scissors pattern:
- Red: enhanced by blue, poisoned by green
- Green: enhanced by red, poisoned by blue  
- Blue: enhanced by green, poisoned by red

![Game of Life Demo](demo.gif) <!-- Replace with actual demo GIF if available -->

## Features

- Colorful three-channel cellular automaton
- Interactive grid where you can toggle cells by clicking
- Adjustable grid size through command-line parameters
- Controls for simulation:
  - Restart: Generate a new random pattern
  - Iterate: Start continuous simulation
  - Step: Advance simulation by one generation
  - Clear: Reset the grid

## Getting Started

### Prerequisites

- Java 11 or higher
- JavaFX SDK (if not included in your Java distribution)

### Running the Application

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/gameOfLife.git
   ```

2. Navigate to the project directory:
   ```
   cd gameOfLife
   ```

3. Compile the project:
   ```
   javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -d . src/principal/*.java
   ```

4. Run the application:
   ```
   java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml principal.GameOfLife
   ```

   Optionally, specify width and height:
   ```
   java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml principal.GameOfLife --width=1024 --height=768
   ```

### Controls

- **Mouse Click**: Toggle cells in a 9x9 area around the clicked position
- **Restart Button**: Generate a new random pattern
- **Iterate Button**: Start/pause continuous simulation
- **Step Button**: Advance simulation by one generation
- **Clear Button**: Reset the grid

## Architecture

The application is structured into three main classes:

1. **GameOfLife**: Main JavaFX application class that handles the UI and user interactions
2. **Renderer**: Handles the visualization of the cellular automaton and grid operations
3. **Transition**: Implements the rules for cell state transitions with RGB channel interactions

## How It Works

Each cell in the grid has three values corresponding to RGB channels. The state transitions follow modified Conway's Game of Life rules where:
- A cell's survival or birth is influenced by its neighbors
- Each color channel can enhance or poison other channels
- The rock-paper-scissors interaction creates dynamic, colorful patterns

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Based on John Conway's Game of Life
- Inspired by colorful cellular automata implementations