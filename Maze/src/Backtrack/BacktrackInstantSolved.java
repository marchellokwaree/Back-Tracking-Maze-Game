package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BacktrackInstantSolved {

    private int[][] maze;
    private int rows;
    private int cols;
    private boolean[][] visited;
    private List<Point> correctPath;

    // Helper class to store X and Y coordinates
    public static class Point {
        public int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    public BacktrackInstantSolved(String filePath) {
        loadMaze(filePath);
    }

    /**
     * Reads the maze file and converts it into a 2D integer array.
     * 1 = Wall. Everything else (0, P1, I, F, H, etc.) = 0 (Path).
     */


    private void loadMaze(String filePath) {
        List<int[]> rowList = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                // Read the line and remove leading/trailing whitespace
                String line = scanner.nextLine().trim();
                
                // Skip completely empty lines
                if (line.isEmpty()) continue;

                // Split the line by spaces into an array of tokens
                String[] tokens = line.split("\\s+");
                int[] row = new int[tokens.length];
                
                for (int i = 0; i < tokens.length; i++) {
                    // Strictly check for "1" as a wall, everything else becomes 0
                    if (tokens[i].equals("1")) {
                        row[i] = 1;
                    } else {
                        row[i] = 0; 
                    }
                }
                rowList.add(row);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Maze file not found: " + e.getMessage());
            return;
        }

        // Initialize maze dimensions
        rows = rowList.size();
        if (rows > 0) {
            cols = rowList.get(0).length;
            maze = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                maze[i] = rowList.get(i);
            }
        }
    }

    /**
     * Triggers the backtracking algorithm to find the exact path.
     */
    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (maze == null) return new ArrayList<>();

        visited = new boolean[rows][cols];
        correctPath = new ArrayList<>();

        if (backtrack(startX, startY, endX, endY)) {
            System.out.println("Path successfully found!");
            return correctPath;
        } else {
            System.out.println("No valid path exists from start to end.");
            return new ArrayList<>();
        }
    }

    /**
     * The core recursive backtracking algorithm.
     */
    private boolean backtrack(int x, int y, int endX, int endY) {
        // Base Case 1: Out of bounds
        if (x < 0 || y < 0 || x >= rows || y >= cols) {
            return false;
        }
        // Base Case 2: Hit a wall or already visited this cell
        if (maze[x][y] == 1 || visited[x][y]) {
            return false;
        }

        // Mark the current cell as visited and add to our current path
        visited[x][y] = true;
        correctPath.add(new Point(x, y));

        // Base Case 3: Reached the destination
        if (x == endX && y == endY) {
            return true;
        }

        // Try moving in all 4 directions (Down, Up, Right, Left)
        if (backtrack(x + 1, y, endX, endY)) return true; // Down
        if (backtrack(x - 1, y, endX, endY)) return true; // Up
        if (backtrack(x, y + 1, endX, endY)) return true; // Right
        if (backtrack(x, y - 1, endX, endY)) return true; // Left

        // If no direction works, we are at a dead end. 
        // Backtrack: Remove this cell from the correct path and return false.
        correctPath.remove(correctPath.size() - 1);
        return false;
    }

    /**
     * Prints the maze layout alongside the solution path for easy visualization.
     */
    public void printSolutionGrid() {
        if (maze == null || correctPath == null) return;

        char[][] displayGrid = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                displayGrid[r][c] = (maze[r][c] == 1) ? '█' : ' ';
            }
        }

        // Mark the correct path with dots
        for (Point p : correctPath) {
            displayGrid[p.x][p.y] = '.';
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                System.out.print(displayGrid[r][c] + " ");
            }
            System.out.println();
        }
    }

    // --- Main method for testing the logic independently ---
    public static void main(String[] args) {
        // Adjust the path to match where Maze1.txt is located in your project directory
        BacktrackInstantSolved solver = new BacktrackInstantSolved("src/Assets/MAP/Maze1.txt");

        // Example start coordinates (Row 1, Col 1) and end coordinates (Row 14, Col 37)
        // You will need to pass the actual start and end coordinates based on where 'P1' and the exit are.
        List<Point> path = solver.solve(1, 1, 14, 37);

        System.out.println("Steps to finish: " + path.size());
        solver.printSolutionGrid();
    }
}