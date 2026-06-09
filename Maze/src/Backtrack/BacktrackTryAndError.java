package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BacktrackTryAndError {

    private int[][] costMap;
    private int[][] plateMap;
    private int[][] gateMap;
    private int[][] npcMap;
    private int rows;
    private int cols;
    
    private int totalNpcMask = 0; 
    private int bestCost;
    
    private Map<String, Integer> dpMemo;      
    private List<BacktrackInstantSolved.Point> searchHistory;

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
            System.err.println("File Maze tidak ditemukan: " + e.getMessage());
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
                        costMap[r][c] = -1;
                    } else if (t.equals("F")) {
                        costMap[r][c] = 10;
                    } else if (t.equals("I")) {
                        costMap[r][c] = 5;
                    } else {
                        costMap[r][c] = 1;
                        if (t.equals("N")) {
                            int bit = (1 << currentBit++);
                            npcMap[r][c] = bit;
                            totalNpcMask |= bit;
                        }
                        else if (t.startsWith("P") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                if (!plateBits.containsKey(id)) plateBits.put(id, 1 << currentBit++);
                                plateMap[r][c] = plateBits.get(id);
                            } catch (NumberFormatException e) {}
                        } 
                        else if (t.startsWith("D") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                if (!plateBits.containsKey(id)) plateBits.put(id, 1 << currentBit++);
                                gateMap[r][c] = plateBits.get(id);
                            } catch (NumberFormatException e) {}
                        }
                    }
                }
            }
        }
    }

    public List<BacktrackInstantSolved.Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        bestCost = Integer.MAX_VALUE;
        dpMemo = new HashMap<>(); 
        searchHistory = new ArrayList<>(); 

        int startState = plateMap[startX][startY] | npcMap[startX][startY];
        
        searchHistory.add(new BacktrackInstantSolved.Point(startX, startY));
        backtrack(startX, startY, endX, endY, 0, startState);

        return searchHistory; 
    }

    private void backtrack(int x, int y, int endX, int endY, int currentCost, int stateMask) {
        if (currentCost >= bestCost) {
            return;
        }
        
        String stateKey = x + "," + y + "," + stateMask;
        // Pengecekan aman di awal
        if (dpMemo.containsKey(stateKey) && currentCost > dpMemo.get(stateKey)) {
            return; 
        }
        dpMemo.put(stateKey, currentCost);

        if (x == endX && y == endY) {
            if ((stateMask & totalNpcMask) == totalNpcMask) {
                bestCost = currentCost;
            }
            return;
        }

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && (stateMask & requiredGate) == 0) {
                    continue; 
                }

                int nextStateMask = stateMask | plateMap[nx][ny] | npcMap[nx][ny];
                int nextCost = currentCost + costMap[nx][ny];

                // --- FITUR LOOK AHEAD (PENCEGAH PING-PONG / STUTTER) ---
                if (nextCost >= bestCost) {
                    continue; // Rute ini sudah pasti kalah, jangan divisualisasikan
                }
                
                String nextStateKey = nx + "," + ny + "," + nextStateMask;
                if (dpMemo.containsKey(nextStateKey) && nextCost >= dpMemo.get(nextStateKey)) {
                    continue; // Kotak ini sudah pernah dikunjungi dengan rute lebih baik, atau ini kotak asal kita. LEWATI!
                }
                // -------------------------------------------------------

                // DO: Catat langkah masuk
                searchHistory.add(new BacktrackInstantSolved.Point(nx, ny));

                // RECURSE: Cek lebih dalam
                backtrack(nx, ny, endX, endY, nextCost, nextStateMask);

                // UNDO: Catat langkah keluar (Mundur dari jalan buntu)
                searchHistory.add(new BacktrackInstantSolved.Point(x, y));
            }
        }
    }
}