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
    private Map<String, BacktrackInstantSolved.Point> discoveredGates;
    private Set<String> pendingGatesToVisit; // Gates opened but not yet explored
    private BacktrackInstantSolved.Point rememberedExit = null;
    private BacktrackInstantSolved.Point activeGoal = null;

    // --- MINECRAFT COBBLESTONE LOGIC ---
    private boolean[][] sealed; 
    private boolean[][] leadsToValuable; 

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

        List<BacktrackInstantSolved.Point> journey = new ArrayList<>();
        List<BacktrackInstantSolved.Point> pathStack = new ArrayList<>();
        boolean[][] visitedInEpoch = new boolean[rows][cols];
        
        discoveredGates = new HashMap<>();
        pendingGatesToVisit = new HashSet<>();
        rememberedExit = null;
        activeGoal = null;
        
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
        
        // Treat permanently sealed dead-ends as solid walls!
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
                    map[r][c] = "0"; // Erase the gate from the map logic so we can walk through
                    opened = true;
                }
            }
        }
        return opened;
    }
}