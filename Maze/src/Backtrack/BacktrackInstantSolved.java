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

    // Node class used for Dijkstra's Algorithm pathfinding
    private static class Node implements Comparable<Node> {
        int x, y, totalCost;
        Node parent;

        public Node(int x, int y, int totalCost, Node parent) {
            this.x = x;
            this.y = y;
            this.totalCost = totalCost;
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
     * Reads the maze file and assigns movement "costs".
     * 1 = Wall (-1, impassable)
     * F = Fire Trap (Cost 10, strongly avoid)
     * I = Ice Trap (Cost 5, somewhat avoid)
     * Everything else = Normal Path (Cost 1)
     */
    private void loadMaze(String filePath) {
        List<int[]> rowList = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] tokens = line.split("\\s+");
                int[] row = new int[tokens.length];
                
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].equals("1")) {
                        row[i] = -1; // Wall
                    } else if (tokens[i].equals("F")) {
                        row[i] = 10; // High penalty (Avoid)
                    } else if (tokens[i].equals("I")) {
                        row[i] = 5;  // Medium penalty (Avoid if possible)
                    } else {
                        row[i] = 1;  // Normal safe floor
                    }
                }
                rowList.add(row);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Maze file not found: " + e.getMessage());
            return;
        }

        rows = rowList.size();
        if (rows > 0) {
            cols = rowList.get(0).length;
            costMap = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                costMap[i] = rowList.get(i);
            }
        }
    }

    /**
     * Uses Dijkstra's Algorithm (Priority Queue) to find the shortest and safest path.
     */
    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        PriorityQueue<Node> pq = new PriorityQueue<>();
        boolean[][] visited = new boolean[rows][cols];

        // Start Node
        pq.offer(new Node(startX, startY, 0, null));

        // Directions: Right, Left, Down, Up
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        Node endNode = null;

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            // Skip if already visited
            if (visited[current.x][current.y]) continue;
            visited[current.x][current.y] = true;

            // Target reached
            if (current.x == endX && current.y == endY) {
                endNode = current;
                break;
            }

            // Check all 4 neighbors
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                // If within bounds, not a wall (-1), and not visited yet
                if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && !visited[nx][ny] && costMap[nx][ny] != -1) {
                    pq.offer(new Node(nx, ny, current.totalCost + costMap[nx][ny], current));
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
            System.out.println("Optimal Path successfully found!");
        } else {
            System.out.println("No valid path exists from start to end.");
        }
        
        return correctPath;
    }
}