package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BacktrackInstantSolved {

    private int[][] costMap;
    private int[][] plateMap; // Stores ID of Pressure Plates
    private int[][] gateMap;  // Stores ID of Gates
    private int[][] npcMap;   // Stores unique bits for each RedHood (NPC)
    private int rows;
    private int cols;
    
    private int totalNpcMask = 0; // The final "Victory" mask requiring all NPCs
    private int maxStates = 1;    // How many possible states exist in this maze

    // Backtracking global trackers
    private int bestCost;
    private List<Point> bestPath;
    private int[][][] minCostVisited; // Memoization to prune expensive paths

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

    private void loadMaze(String filePath) {
        List<String[]> rowList = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                rowList.add(line.split("\\s+"));
            }
        } catch (FileNotFoundException e) {
            System.err.println("Maze file not found: " + e.getMessage());
            return;
        }

        rows = rowList.size();
        if (rows > 0) {
            cols = rowList.get(0).length;
            costMap = new int[rows][cols];
            plateMap = new int[rows][cols];
            gateMap = new int[rows][cols];
            npcMap = new int[rows][cols];

            int currentBit = 0; 
            Map<Integer, Integer> plateBits = new HashMap<>();

            for (int r = 0; r < rows; r++) {
                String[] tokens = rowList.get(r);
                for (int c = 0; c < cols; c++) {
                    String t = tokens[c];
                    
                    if (t.equals("1")) {
                        costMap[r][c] = -1; // Wall
                    } else if (t.equals("F")) {
                        costMap[r][c] = 10; // High penalty
                    } else if (t.equals("I")) {
                        costMap[r][c] = 5;  // Medium penalty
                    } else {
                        costMap[r][c] = 1;  // Normal safe floor
                        
                        if (t.equals("N")) {
                            int bit = (1 << currentBit++);
                            npcMap[r][c] = bit;
                            totalNpcMask |= bit;
                        }
                        else if (t.startsWith("P") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                if (!plateBits.containsKey(id)) {
                                    plateBits.put(id, 1 << currentBit++);
                                }
                                plateMap[r][c] = plateBits.get(id);
                            } catch (NumberFormatException e) {}
                        } 
                        else if (t.startsWith("D") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                if (!plateBits.containsKey(id)) {
                                    plateBits.put(id, 1 << currentBit++);
                                }
                                gateMap[r][c] = plateBits.get(id);
                            } catch (NumberFormatException e) {}
                        }
                    }
                }
            }
            maxStates = 1 << currentBit; 
        }
    }

    /**
     * Initializes the tracking arrays and kicks off the recursive search.
     */
    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        // Reset state for new solve
        bestCost = Integer.MAX_VALUE;
        bestPath = new ArrayList<>();
        minCostVisited = new int[rows][cols][maxStates];
        
        // Initialize memoization array with Max Value
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int s = 0; s < maxStates; s++) {
                    minCostVisited[r][c][s] = Integer.MAX_VALUE;
                }
            }
        }

        List<Point> currentPath = new ArrayList<>();
        currentPath.add(new Point(startX, startY));
        
        // Check if the starting position instantly gives us an NPC or Plate
        int startState = plateMap[startX][startY] | npcMap[startX][startY];

        // Begin recursive backtracking
        backtrack(startX, startY, endX, endY, 0, startState, currentPath);

        if (!bestPath.isEmpty()) {
            System.out.println("Optimal Stateful Path successfully found! All NPCs Collected.");
        } else {
            System.out.println("No valid path exists to collect all keys and reach the exit.");
        }
        
        return bestPath;
    }

    /**
     * The core recursive backtracking function.
     */
    private void backtrack(int x, int y, int endX, int endY, int currentCost, int stateMask, List<Point> currentPath) {
        // PRUNING 1: If current path is already more expensive than the best found, abort.
        if (currentCost >= bestCost) {
            return;
        }

        // PRUNING 2: If we've visited this exact tile with the exact same items/keys, 
        // but with a cheaper or equal cost, exploring further is redundant. Abort.
        if (currentCost >= minCostVisited[x][y][stateMask]) {
            return;
        }
        
        // Record our current best cost for this specific state at this location
        minCostVisited[x][y][stateMask] = currentCost;

        // VICTORY CHECK
        if (x == endX && y == endY) {
            if ((stateMask & totalNpcMask) == totalNpcMask) {
                // We reached the end with all NPCs at a lower cost than before
                bestCost = currentCost;
                bestPath = new ArrayList<>(currentPath); 
            }
            return; // Exit this branch
        }

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // Explore all 4 directions
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                
                // 1. Check if Gate is locked
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && (stateMask & requiredGate) == 0) {
                    continue; 
                }

                // 2. Prepare next states
                int nextStateMask = stateMask | plateMap[nx][ny] | npcMap[nx][ny];
                int nextCost = currentCost + costMap[nx][ny];

                // 3. DO: Add to path
                currentPath.add(new Point(nx, ny));
                
                // 4. RECURSE
                backtrack(nx, ny, endX, endY, nextCost, nextStateMask, currentPath);
                
                // 5. UNDO: Backtrack by removing the last point added
                currentPath.remove(currentPath.size() - 1);
            }
        }
    }
}