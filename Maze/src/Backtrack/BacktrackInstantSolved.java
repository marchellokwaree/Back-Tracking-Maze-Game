package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

public class BacktrackInstantSolved {

    private int[][] costMap;
    private int[][] plateMap; // Stores ID of Pressure Plates
    private int[][] gateMap;  // Stores ID of Gates
    private int rows;
    private int cols;

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
        int stateMask; // Bitmask storing which pressure plates we have activated
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
     * Reads the maze file and assigns movement costs, gates, and pressure plates.
     */
    private void loadMaze(String filePath) {
        List<String[]> rowList = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                rowList.add(tokens);
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

            for (int r = 0; r < rows; r++) {
                String[] tokens = rowList.get(r);
                for (int c = 0; c < cols; c++) {
                    String t = tokens[c];
                    
                    if (t.equals("1")) {
                        costMap[r][c] = -1; // Wall
                    } else if (t.equals("F")) {
                        costMap[r][c] = 10; // High penalty (Avoid)
                    } else if (t.equals("I")) {
                        costMap[r][c] = 5;  // Medium penalty
                    } else {
                        costMap[r][c] = 1;  // Normal safe floor
                        
                        // Check if it's a Pressure Plate (e.g., P1, P2)
                        if (t.startsWith("P") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                // Shift bits to create a unique signature (Key)
                                plateMap[r][c] = (1 << id);
                            } catch (NumberFormatException e) {}
                        } 
                        // Check if it's a Gate (e.g., D1, D2)
                        else if (t.startsWith("D") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                // Shift bits to require a unique signature (Lock)
                                gateMap[r][c] = (1 << id);
                            } catch (NumberFormatException e) {}
                        }
                    }
                }
            }
        }
    }

    /**
     * Uses Dijkstra's Algorithm with Bitmasking to find the shortest path 
     * while collecting required keys (stepping on plates) to open doors.
     */
    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        PriorityQueue<Node> pq = new PriorityQueue<>();
        
        // Visited array now has 3 dimensions: [Row][Col][StateMask]
        // This allows the AI to visit the same tile multiple times (e.g., walking 
        // to a plate, and walking back out) because its "State" has changed!
        boolean[][][] visited = new boolean[rows][cols][1024]; 

        // Start Node (State 0 means no plates stepped on yet)
        pq.offer(new Node(startX, startY, 0, 0, null));

        // Directions: Right, Left, Down, Up
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        Node endNode = null;

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            // Skip if this exact state has been visited
            if (visited[current.x][current.y][current.stateMask]) continue;
            visited[current.x][current.y][current.stateMask] = true;

            // Target reached
            if (current.x == endX && current.y == endY) {
                endNode = current;
                break;
            }

            // Check all 4 neighbors
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                // If within bounds and not a wall
                if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                    
                    int nextStateMask = current.stateMask;
                    
                    // 1. If it's a Gate, do we have the required Plate activated?
                    int requiredGate = gateMap[nx][ny];
                    if (requiredGate != 0 && (current.stateMask & requiredGate) == 0) {
                        continue; // We don't have the key, block path!
                    }

                    // 2. If it's a Plate, add its ID to our "Keys Collected" mask
                    int plateHere = plateMap[nx][ny];
                    if (plateHere != 0) {
                        nextStateMask = current.stateMask | plateHere; 
                    }

                    // 3. Queue the next move if it hasn't been visited in this state
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
            // Reverse it so it goes from Start to End
            Collections.reverse(correctPath);
            System.out.println("Optimal Stateful Path successfully found!");
        } else {
            System.out.println("No valid path exists from start to end.");
        }
        
        return correctPath;
    }
}