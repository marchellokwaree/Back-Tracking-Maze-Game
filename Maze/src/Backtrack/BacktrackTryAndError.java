package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import Backtrack.BacktrackInstantSolved.Point; // <--- ADD THIS LINE

public class BacktrackTryAndError {

    private String[][] map;
    private int rows;
    private int cols;

    private int totalNPCs = 0;
    private int collectedNPCs = 0;

    // --- AI MEMORY ---
    private Map<String, Point> discoveredGates;
    private Set<String> pendingGatesToVisit;
    private Point rememberedExit = null;
    private Point activeGoal = null;

    // --- MINECRAFT COBBLESTONE LOGIC ---
    private boolean[][] sealed;
    private boolean[][] leadsToValuable;

    // Visited cells tracked as a persistent Set of "r,c" strings.
    // No epoch resets — cells simply remain visited or are re-evaluated
    // based on whether the path stack contains them (cycle prevention via currentStack).
    private Set<String> visitedCells;

    // Execution limits to prevent stack overflow
    private int steps = 0;
    private final int MAX_STEPS = 50000;

    // Directions: Up, Down, Left, Right
    private final int[] dr = {-1, 1, 0, 0};
    private final int[] dc = {0, 0, -1, 1};


    public BacktrackTryAndError(String filePath) {
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
            map = new String[rows][cols];

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    map[r][c] = rowList.get(r)[c];
                    if (map[r][c].equals("N")) {
                        totalNPCs++;
                    }
                }
            }
        }
    }

    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (map == null) return new ArrayList<>();

        List<Point> journey = new ArrayList<>();
        List<Point> currentStack = new ArrayList<>();

        discoveredGates = new HashMap<>();
        pendingGatesToVisit = new HashSet<>();
        rememberedExit = null;
        activeGoal = null;
        collectedNPCs = 0;
        steps = 0;

        sealed = new boolean[rows][cols];
        leadsToValuable = new boolean[rows][cols];

        // Persistent visited set: tracks cells visited across the entire journey.
        // Unlike the previous epoch-based boolean[][] array, this Set is never wiped
        // — cells are removed from it only when we physically backtrack off them,
        // preventing revisits within the current active path (cycle prevention).
        visitedCells = new HashSet<>();

        System.out.println("Starting Simulation: Human-like Trial & Error (Recursive Version)...");

        explore(startX, startY, journey, currentStack);

        return journey;
    }

    /**
     * The recursive method that simulates human wandering.
     * Returns true if the exit was successfully found to unwind the recursion.
     */
    private boolean explore(int r, int c, List<Point> journey, List<Point> currentStack) {
        if (steps >= MAX_STEPS) return false;
        steps++;

        String cellKey = r + "," + c;
        Point curr = new Point(r, c);
        journey.add(curr);
        currentStack.add(curr);
        visitedCells.add(cellKey);

        // 1. Goal & Gate Arrival Check
        if (activeGoal != null && r == activeGoal.x && c == activeGoal.y) {
            activeGoal = null;
        }

        List<String> gatesReached = new ArrayList<>();
        for (Map.Entry<String, Point> entry : discoveredGates.entrySet()) {
            if (entry.getValue().x == r && entry.getValue().y == c && pendingGatesToVisit.contains(entry.getKey())) {
                gatesReached.add(entry.getKey());
            }
        }
        for (String gateId : gatesReached) {
            pendingGatesToVisit.remove(gateId);
        }

        // --- WORLD INTERACTIONS ---
        String tile = map[r][c];
        boolean stateChanged = false;

        if (tile.equals("N")) {
            collectedNPCs++;
            map[r][c] = "0";
            stateChanged = true;

            if (collectedNPCs == totalNPCs && rememberedExit != null) {
                activeGoal = rememberedExit;
            }
        } else if (tile.startsWith("P") && tile.length() > 1) {
            String id = tile.substring(1);
            map[r][c] = "0";
            if (openGate(id)) {
                stateChanged = true;
                if (discoveredGates.containsKey(id)) {
                    pendingGatesToVisit.add(id);
                }
            }
        } else if (tile.equals("G")) {
            if (collectedNPCs == totalNPCs) {
                return true;
            }
        }

        // When the world state changes (NPC collected, gate opened), allow revisiting
        // cells that were previously skipped. We achieve this by removing from visitedCells
        // all cells NOT on the current active path — making them explorable again.
        // This replaces the old epoch-reset approach (wiping the entire boolean[][] and
        // re-marking the current stack) with a targeted Set operation.
        if (stateChanged) {
            Set<String> activePathKeys = new HashSet<>();
            for (Point p : currentStack) {
                activePathKeys.add(p.x + "," + p.y);
            }
            visitedCells.retainAll(activePathKeys);
        }

        // --- SCAN SURROUNDINGS (LOOK AHEAD) ---
        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                String lookTile = map[nr][nc];
                if ((lookTile.startsWith("D") && lookTile.length() > 1) || lookTile.equals("G") || lookTile.equals("H")) {
                    for (Point p : currentStack) {
                        leadsToValuable[p.x][p.y] = true;
                    }
                    if (lookTile.startsWith("D")) {
                        String id = lookTile.substring(1);
                        if (!discoveredGates.containsKey(id)) {
                            discoveredGates.put(id, new Point(nr, nc));
                        }
                    } else if (lookTile.equals("G") && rememberedExit == null) {
                        rememberedExit = new Point(nr, nc);
                    }
                }
            }
        }

        // --- EVALUATE MOVEMENTS ---
        List<Integer> safeNeighbors = new ArrayList<>();
        List<Integer> trapNeighbors = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (isValid(nr, nc) && !visitedCells.contains(nr + "," + nc)) {
                String nTile = map[nr][nc];
                if (nTile.equals("G") && collectedNPCs < totalNPCs) continue;

                if (nTile.equals("F") || nTile.equals("I")) trapNeighbors.add(i);
                else safeNeighbors.add(i);
            }
        }

        if (activeGoal != null) {
            sortDirectionsByGoal(safeNeighbors, curr);
            sortDirectionsByGoal(trapNeighbors, curr);
        } else {
            Collections.shuffle(safeNeighbors);
            Collections.shuffle(trapNeighbors);
        }

        List<Integer> allMoves = new ArrayList<>(safeNeighbors);
        allMoves.addAll(trapNeighbors);

        // --- EXECUTE STEPS RECURSIVELY ---
        for (int dir : allMoves) {
            int nr = r + dr[dir];
            int nc = c + dc[dir];

            if (isValid(nr, nc) && !visitedCells.contains(nr + "," + nc)) {
                boolean escaped = explore(nr, nc, journey, currentStack);
                if (escaped) return true;

                // If we didn't escape, we physically stepped back to this cell
                journey.add(curr);
            }
        }

        // --- DEAD END COBBLESTONE LOGIC ---
        boolean isSelfValuable = tile.equals("H") || tile.equals("G") || (tile.startsWith("D") && tile.length() > 1);
        if (!leadsToValuable[r][c] && !isSelfValuable) {
            sealed[r][c] = true;
        }

        // Since we are backtracking, does this trigger a new goal?
        if (activeGoal == null && !pendingGatesToVisit.isEmpty()) {
            String targetGateId = pendingGatesToVisit.iterator().next();
            activeGoal = discoveredGates.get(targetGateId);
        } else if (activeGoal == null && collectedNPCs == totalNPCs && rememberedExit != null) {
            activeGoal = rememberedExit;
        }

        // Ascending back up the recursive stack — remove this cell from visited
        // so that sibling branches (other paths) can enter it if needed.
        visitedCells.remove(cellKey);
        currentStack.remove(currentStack.size() - 1);
        return false;
    }

    private void sortDirectionsByGoal(List<Integer> dirs, Point curr) {
        dirs.sort((d1, d2) -> {
            int r1 = curr.x + dr[d1], c1 = curr.y + dc[d1];
            int r2 = curr.x + dr[d2], c2 = curr.y + dc[d2];
            int dist1 = Math.abs(r1 - activeGoal.x) + Math.abs(c1 - activeGoal.y);
            int dist2 = Math.abs(r2 - activeGoal.x) + Math.abs(c2 - activeGoal.y);
            return Integer.compare(dist1, dist2);
        });
    }

    private boolean isValid(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        if (sealed[r][c]) return false;

        String t = map[r][c];
        if (t.equals("1")) return false;
        if (t.startsWith("D") && t.length() > 1) return false;
        return true;
    }

    private boolean openGate(String id) {
        String targetGate = "D" + id;
        boolean opened = false;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (map[r][c].equals(targetGate)) {
                    map[r][c] = "0";
                    opened = true;
                }
            }
        }
        return opened;
    }
}