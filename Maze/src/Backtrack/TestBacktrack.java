package Backtrack;

import java.util.List;
import java.util.Scanner;
import java.io.File;

public class TestBacktrack {
    public static void main(String[] args) {
        String mazeFile = "src/Assets/MAP/Maze1.txt";
        
        // Find S and G positions
        int startX = -1, startY = -1, endX = -1, endY = -1;
        try (Scanner scanner = new Scanner(new File(mazeFile))) {
            int row = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                
                String[] tokens = line.split("\\s+");
                for (int col = 0; col < tokens.length; col++) {
                    if (tokens[col].equals("S")) {
                        startX = row;
                        startY = col;
                    }
                    if (tokens[col].equals("G")) {
                        endX = row;
                        endY = col;
                    }
                }
                row++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("=== Testing Pure Backtracking (NO MEMOIZATION) ===");
        System.out.println("Maze size: 18x18 = 324 cells");
        System.out.println("Start: (" + startX + ", " + startY + ")");
        System.out.println("End: (" + endX + ", " + endY + ")");
        
        if (startX < 0 || endX < 0) {
            System.out.println("ERROR: Could not find S or G in maze!");
            return;
        }
        
        BacktrackInstantSolved solver = new BacktrackInstantSolved(mazeFile);
        
        long startTime = System.currentTimeMillis();
        List<BacktrackInstantSolved.Point> path = solver.solve(startX, startY, endX, endY);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nResults:");
        System.out.println("Path found: " + path.size() + " steps");
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
        if (path.size() > 0) {
            System.out.println("First step: " + path.get(0));
            System.out.println("Last step: " + path.get(path.size() - 1));
        }
    }
}
