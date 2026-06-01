package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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

    // Node class used for Dijkstra's State-Space Algorithm
    private static class Node implements Comparable<Node> {
        int x, y, totalCost;
        int stateMask; // Bitmask storing activated plates AND collected NPCs
        Node parent;

        public Node(int x, int y, int totalCost, int stateMask, Node parent) {
            this.x = x;
            this.y = y;
            this.totalCost = totalCost;
            this.stateMask = stateMask;
            this.parent = parent;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.totalCost, other.totalCost);
        }
    }

    public BacktrackInstantSolved(String filePath) {
        loadMaze(filePath);
    }

    /**
     * Reads the maze file and dynamically assigns unique bits to plates, gates, and NPCs.
     */
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

            // Dynamic bit assignment ensures we only use as much memory as needed
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
                        
                        // If it's a RedHood (NPC)
                        if (t.equals("N")) {
                            int bit = (1 << currentBit++);
                            npcMap[r][c] = bit;
                            totalNpcMask |= bit; // Add this NPC to the victory requirement
                        }
                        // Check if it's a Pressure Plate (e.g., P1)
                        else if (t.startsWith("P") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                if (!plateBits.containsKey(id)) {
                                    plateBits.put(id, 1 << currentBit++);
                                }
                                plateMap[r][c] = plateBits.get(id);
                            } catch (NumberFormatException e) {}
                        } 
                        // Check if it's a Gate (e.g., D1)
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
            
            // Calculate total possible states based on the number of interactive items
            maxStates = 1 << currentBit; 
        }
    }

    /**
     * Uses Dijkstra's Algorithm with Bitmasking to find the shortest path 
     * while collecting ALL NPCs and triggering required plates for gates.
     */
    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        PriorityQueue<Node> pq = new PriorityQueue<>();
        
        // 3D array tracks X, Y, and the current "Backpack State" (items/plates collected)
        boolean[][][] visited = new boolean[rows][cols][maxStates]; 

        // Start Node (State 0 means no plates stepped on, no NPCs collected yet)
        pq.offer(new Node(startX, startY, 0, 0, null));

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        Node endNode = null;

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            if (visited[current.x][current.y][current.stateMask]) continue;
            visited[current.x][current.y][current.stateMask] = true;

            // VICTORY CHECK: Reached the goal AND collected all NPCs
            if (current.x == endX && current.y == endY) {
                if ((current.stateMask & totalNpcMask) == totalNpcMask) {
                    endNode = current;
                    break;
                }
                // If they haven't collected all NPCs, the Goal acts like a normal floor tile
            }

            // Check all 4 neighbors
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                    
                    int nextStateMask = current.stateMask;
                    
                    // 1. Is this a locked Gate? Check if we stepped on the required Plate.
                    int requiredGate = gateMap[nx][ny];
                    if (requiredGate != 0 && (current.stateMask & requiredGate) == 0) {
                        continue; // Blocked! We don't have the key for this gate yet.
                    }

                    // 2. Is there a Plate or NPC here? Add it to our State Mask (Backpack).
                    nextStateMask |= plateMap[nx][ny];
                    nextStateMask |= npcMap[nx][ny];

                    // 3. Queue the move if we haven't been here in this specific state yet.
                    if (!visited[nx][ny][nextStateMask]) {
                        pq.offer(new Node(nx, ny, current.totalCost + costMap[nx][ny], nextStateMask, current));
                    }
                }
            }
        }

        // Trace back the optimal path
        List<Point> correctPath = new ArrayList<>();
        if (endNode != null) {
            Node curr = endNode;
            while (curr != null) {
                correctPath.add(new Point(curr.x, curr.y));
                curr = curr.parent;
            }
            Collections.reverse(correctPath);
            System.out.println("Optimal Stateful Path successfully found! All NPCs Collected.");
        } else {
            System.out.println("No valid path exists to collect all keys and reach the exit.");
        }
        
        return correctPath;
    }
}