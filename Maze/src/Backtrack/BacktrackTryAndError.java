package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class BacktrackTryAndError {

    private int[][] costMap;
    private int[][] plateMap;
    private int[][] gateMap;
    private int[][] npcMap;
    private int rows;
    private int cols;
    
    private int totalNpcCount = 0; 
    private int bestCost;
    
    // Kamus Memori Keputusan (OOP Memoization)
    private Map<String, Integer> decisionMemory;  
    
    // Untuk Merekam seluruh jejak eksplorasi (Maju & Mundur)
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

            int npcIdCounter = 1000; 

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
                            npcMap[r][c] = npcIdCounter++;
                            totalNpcCount++;
                        }
                        else if (t.startsWith("P") && t.length() > 1) {
                            try { plateMap[r][c] = Integer.parseInt(t.substring(1)); } 
                            catch (NumberFormatException e) {}
                        } 
                        else if (t.startsWith("D") && t.length() > 1) {
                            try { gateMap[r][c] = Integer.parseInt(t.substring(1)); } 
                            catch (NumberFormatException e) {}
                        }
                    }
                }
            }
        }
    }

    // Perhatikan tipe datanya mereturn Point milik BacktrackInstantSolved 
    // agar kompatibel dengan Player.java Anda
    public List<BacktrackInstantSolved.Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        bestCost = Integer.MAX_VALUE;
        decisionMemory = new HashMap<>(); 
        searchHistory = new ArrayList<>(); 

        Set<Integer> initialInventory = new HashSet<>();
        if (plateMap[startX][startY] != 0) initialInventory.add(plateMap[startX][startY]);
        if (npcMap[startX][startY] != 0) initialInventory.add(npcMap[startX][startY]);
        
        // Catat langkah pertama
        searchHistory.add(new BacktrackInstantSolved.Point(startX, startY));

        backtrack(startX, startY, endX, endY, 0, initialInventory);

        return searchHistory; // Yang di-return adalah sejarah perjalanan, bukan rute tercepat!
    }

    private void backtrack(int x, int y, int endX, int endY, int currentCost, Set<Integer> inventory) {
        // --- 1. BOUNDING ---
        if (currentCost >= bestCost) { // Jika cost saat ini sudah lebih buruk dari bestCost, tidak perlu lanjut di cabang ini
            return;
        }
        
        // --- 2. MEMORI KEPUTUSAN (Anti Loop) ---
        String memoryKey = x + "," + y + "," + inventory.toString();
        
        if (decisionMemory.containsKey(memoryKey) && currentCost > decisionMemory.get(memoryKey)) { // Jika sudah pernah ke titik ini dengan inventory yang sama atau lebih baik, jangan ulangi!
            return; 
        }
        decisionMemory.put(memoryKey, currentCost);

        // --- 3. CEK KEMENANGAN ---
        if (x == endX && y == endY) {
            int collectedNpcs = 0;
            for (int item : inventory) {
                if (item >= 1000) collectedNpcs++; 
            }

            if (collectedNpcs == totalNpcCount) {
                bestCost = currentCost; // Rekor diperbarui, algoritma akan mencari jalan yang lebih cepat lagi
            }
            return;
        }

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // --- 4. EKSPLORASI POHON KEPUTUSAN ---
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                
                // Cek Gerbang & Kunci
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && !inventory.contains(requiredGate)) {
                    continue; 
                }

                // Persiapan konsekuensi langkah
                int nextCost = currentCost + costMap[nx][ny];
                
                // Fotokopi tas bawaan
                Set<Integer> nextInventory = new HashSet<>(inventory);
                if (plateMap[nx][ny] != 0) nextInventory.add(plateMap[nx][ny]);
                if (npcMap[nx][ny] != 0) nextInventory.add(npcMap[nx][ny]);

                // --- FITUR LOOK AHEAD (PENCEGAH STUTTER/PING-PONG) ---
                if (nextCost >= bestCost) continue; 
                
                String nextMemoryKey = nx + "," + ny + "," + nextInventory.toString();
                if (decisionMemory.containsKey(nextMemoryKey) && nextCost >= decisionMemory.get(nextMemoryKey)) {
                    continue; // Jika kotak di depan sudah pernah diinjak dengan cost lebih murah, jangan divisualisasikan!
                }

                // DO: Catat langkah masuk ke ruangan baru
                searchHistory.add(new BacktrackInstantSolved.Point(nx, ny));

                // RECURSE: Eksplorasi cabang ini
                backtrack(nx, ny, endX, endY, nextCost, nextInventory);

                // UNDO: Saat cabang ini selesai/buntu, catat langkah MUNDUR ke titik (x,y) saat ini
                searchHistory.add(new BacktrackInstantSolved.Point(x, y));
            }
        }
    }
}