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

public class BacktrackTryAndError {

    private String[][] map;
    private int rows;
    private int cols;

    private int totalNPCs = 0;
    private int collectedNPCs = 0;

    // --- AI MEMORY ---
<<<<<<< HEAD
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
=======
    private Map<String, BacktrackInstantSolved.Point> discoveredGates;
    private Set<String> pendingGatesToVisit; // Gates opened but not yet explored
    private BacktrackInstantSolved.Point rememberedExit = null;
    private BacktrackInstantSolved.Point activeGoal = null;

    // --- MINECRAFT COBBLESTONE LOGIC ---
    private boolean[][] sealed; 
    private boolean[][] leadsToValuable; 
>>>>>>> parent of 7b11ac9 (fix work 5)

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
            map = new String[rows][cols];

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    map[r][c] = rowList.get(r)[c];
                    // Count total keys/NPCs dynamically
                    if (map[r][c].equals("N")) {
                        totalNPCs++;
                    }
                }
            }
        }
    }

    public List<BacktrackInstantSolved.Point> solve(int startX, int startY, int endX, int endY) {
        if (map == null) return new ArrayList<>();

<<<<<<< HEAD
        List<Point> journey = new ArrayList<>();
        List<Point> currentStack = new ArrayList<>();

=======
        List<BacktrackInstantSolved.Point> journey = new ArrayList<>();
        List<BacktrackInstantSolved.Point> pathStack = new ArrayList<>();
        boolean[][] visitedInEpoch = new boolean[rows][cols];
        
>>>>>>> parent of 7b11ac9 (fix work 5)
        discoveredGates = new HashMap<>();
        pendingGatesToVisit = new HashSet<>();
        rememberedExit = null;
        activeGoal = null;
<<<<<<< HEAD
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
=======
        
        sealed = new boolean[rows][cols];
        leadsToValuable = new boolean[rows][cols];
        
        collectedNPCs = 0;

        BacktrackInstantSolved.Point start = new BacktrackInstantSolved.Point(startX, startY);
        pathStack.add(start);
        journey.add(start);
        visitedInEpoch[start.x][start.y] = true;

        int maxSteps = 50000; 
        int steps = 0;

        System.out.println("Starting Simulation: Human-like Trial & Error...");

        while (!pathStack.isEmpty() && steps < maxSteps) {
            steps++;
            BacktrackInstantSolved.Point curr = pathStack.get(pathStack.size() - 1);
            
            // 1. Goal & Gate Arrival Check
            if (activeGoal != null && curr.x == activeGoal.x && curr.y == activeGoal.y) {
                System.out.println("AI: Reached my target location! Resuming local exploration.");
                activeGoal = null;
            }

            // Check if we arrived at a gate we were planning to visit
            List<String> gatesReached = new ArrayList<>();
            for (Map.Entry<String, BacktrackInstantSolved.Point> entry : discoveredGates.entrySet()) {
                if (entry.getValue().x == curr.x && entry.getValue().y == curr.y && pendingGatesToVisit.contains(entry.getKey())) {
                    gatesReached.add(entry.getKey());
                }
            }
            for (String gateId : gatesReached) {
                pendingGatesToVisit.remove(gateId);
                System.out.println("AI: I arrived at the opened Gate " + gateId + "! Time to see what's hidden inside.");
            }

            String tile = map[curr.x][curr.y];
            boolean stateChanged = false;

            // --- WORLD INTERACTIONS ---
            if (tile.equals("N")) {
                collectedNPCs++;
                map[curr.x][curr.y] = "0"; // Consumed
                stateChanged = true;
                System.out.println("AI: Rescued a Red Hood / Key! (" + collectedNPCs + "/" + totalNPCs + ")");
                
                // If this was the final key, and we remember where the exit is, sprint to it!
                if (collectedNPCs == totalNPCs && rememberedExit != null) {
                    activeGoal = rememberedExit;
                    System.out.println("AI: That's all 3 keys! I remember the portal. Making a run for it!");
                }
                
            } else if (tile.startsWith("P") && tile.length() > 1) {
                String id = tile.substring(1);
                map[curr.x][curr.y] = "0"; // Consumed
                boolean opened = openGate(id);
                
                if (opened) {
                    stateChanged = true;
                    if (discoveredGates.containsKey(id)) {
                        // HUMAN LOGIC: Remember the gate, but DO NOT go there yet. 
                        // Finish exploring the current hallway first.
                        pendingGatesToVisit.add(id);
                        System.out.println("AI: Pressed plate " + id + ". Gate " + id + " opened! I'll remember this, but let me check this area first.");
                    }
                }
            } else if (tile.equals("G")) {
                if (collectedNPCs == totalNPCs) {
                    System.out.println("AI: Escaped the maze!");
                    break; // Successfully escaped!
                }
            }

            // Memory wipe (Allows revisiting intersections, but NOT sealed dead-ends)
            if (stateChanged) {
                visitedInEpoch = new boolean[rows][cols];
                for (BacktrackInstantSolved.Point p : pathStack) {
                    visitedInEpoch[p.x][p.y] = true;
                }
            }

            // --- SCAN SURROUNDINGS (LOOK AHEAD) ---
            for (int i = 0; i < 4; i++) {
                int nr = curr.x + dr[i];
                int nc = curr.y + dc[i];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    String lookTile = map[nr][nc];
                    
                    // If we see a Gate, Potion, or the Exit, we MUST protect this path from being sealed.
                    if ((lookTile.startsWith("D") && lookTile.length() > 1) || lookTile.equals("G") || lookTile.equals("H")) {
                        for (BacktrackInstantSolved.Point p : pathStack) {
                            leadsToValuable[p.x][p.y] = true;
                        }
                        
                        if (lookTile.startsWith("D")) {
                            String id = lookTile.substring(1);
                            if (!discoveredGates.containsKey(id)) {
                                discoveredGates.put(id, new BacktrackInstantSolved.Point(nr, nc));
                                System.out.println("AI: Found locked gate " + id + ". I will memorize this location.");
                            }
                        } else if (lookTile.equals("G")) {
                            if (rememberedExit == null) {
                                rememberedExit = new BacktrackInstantSolved.Point(nr, nc);
                                System.out.println("AI: Found the exit portal! But I need " + (totalNPCs - collectedNPCs) + " more keys. Memorizing location.");
                            }
                        }
                    }
                }
            }

            // --- EVALUATE MOVEMENTS ---
            List<Integer> safeNeighbors = new ArrayList<>();
            List<Integer> trapNeighbors = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                int nr = curr.x + dr[i];
                int nc = curr.y + dc[i];

                if (isValid(nr, nc) && !visitedInEpoch[nr][nc]) {
                    String nTile = map[nr][nc];
                    
                    // DO NOT step on the exit portal if we don't have all the keys yet!
                    if (nTile.equals("G") && collectedNPCs < totalNPCs) {
                        continue; 
                    }

                    if (nTile.equals("F") || nTile.equals("I")) {
                        trapNeighbors.add(i);
                    } else {
                        safeNeighbors.add(i);
                    }
                }
            }

            // Route towards goal if we have one
            if (activeGoal != null) {
                sortDirectionsByGoal(safeNeighbors, curr);
                sortDirectionsByGoal(trapNeighbors, curr);
            } else {
                Collections.shuffle(safeNeighbors);
                Collections.shuffle(trapNeighbors);
            }

            // --- EXECUTE STEP OR BACKTRACK ---
            if (!safeNeighbors.isEmpty()) {
                stepForward(safeNeighbors.get(0), curr, pathStack, journey, visitedInEpoch);
            } else if (!trapNeighbors.isEmpty()) {
                stepForward(trapNeighbors.get(0), curr, pathStack, journey, visitedInEpoch);
            } else {
                // -----------------------------------------------------------
                // DEAD END DETECTED: Step backward and SEAL the path!
                // -----------------------------------------------------------
                BacktrackInstantSolved.Point popped = pathStack.remove(pathStack.size() - 1);
                
                String t = map[popped.x][popped.y];
                boolean isSelfValuable = t.equals("H") || t.equals("G") || (t.startsWith("D") && t.length() > 1);
                
                // Seal it permanently so we never return to this useless corridor!
                if (!leadsToValuable[popped.x][popped.y] && !isSelfValuable) {
                    sealed[popped.x][popped.y] = true;
                }

                // HUMAN LOGIC: Now that I've cleared this dead end, is there an open gate I was saving for later?
                if (activeGoal == null && !pendingGatesToVisit.isEmpty()) {
                    String targetGateId = pendingGatesToVisit.iterator().next(); // Pick any pending gate
                    activeGoal = discoveredGates.get(targetGateId);
                    System.out.println("AI: Dead end! Okay, the area is clear. Now routing back to the open Gate " + targetGateId + "!");
                } else if (activeGoal == null && collectedNPCs == totalNPCs && rememberedExit != null) {
                    activeGoal = rememberedExit;
                    System.out.println("AI: Dead end, but I have all 3 keys! Making a run to the remembered portal!");
                }
                
                if (!pathStack.isEmpty()) {
                    BacktrackInstantSolved.Point prev = pathStack.get(pathStack.size() - 1);
                    journey.add(prev);
                }
            }
        }

        return journey;
    }

    private void stepForward(int dir, BacktrackInstantSolved.Point curr, List<BacktrackInstantSolved.Point> pathStack, List<BacktrackInstantSolved.Point> journey, boolean[][] visitedInEpoch) {
        BacktrackInstantSolved.Point next = new BacktrackInstantSolved.Point(curr.x + dr[dir], curr.y + dc[dir]);
        pathStack.add(next);
        journey.add(next);
        visitedInEpoch[next.x][next.y] = true;
>>>>>>> parent of 7b11ac9 (fix work 5)
    }

    private void sortDirectionsByGoal(List<Integer> dirs, BacktrackInstantSolved.Point curr) {
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
<<<<<<< HEAD
        if (sealed[r][c]) return false;

=======
        
        // Treat permanently sealed dead-ends as solid walls!
        if (sealed[r][c]) return false; 
        
>>>>>>> parent of 7b11ac9 (fix work 5)
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
<<<<<<< HEAD
                    map[r][c] = "0";
=======
                    map[r][c] = "0"; // Erase the gate from the map logic so we can walk through
>>>>>>> parent of 7b11ac9 (fix work 5)
                    opened = true;
                }
            }
        }
        return opened;
    }
}