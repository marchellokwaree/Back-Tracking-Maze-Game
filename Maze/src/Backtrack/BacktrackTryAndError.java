package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    
    // --- VARIABEL BACKTRACKING MURNI ---
    private int bestCost;
    private boolean[][] activePath; // Pengganti SSS, murni untuk Simple Path Tracker
    
    // BUKU SEJARAH: Merekam jejak visual
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

    public List<BacktrackInstantSolved.Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        bestCost = Integer.MAX_VALUE;
        activePath = new boolean[rows][cols];
        searchHistory = new ArrayList<>(); 

        Set<Integer> initialInventory = new HashSet<>();
        if (plateMap[startX][startY] != 0) initialInventory.add(plateMap[startX][startY]);
        if (npcMap[startX][startY] != 0) initialInventory.add(npcMap[startX][startY]);
        
        // Setup titik awal
        searchHistory.add(new BacktrackInstantSolved.Point(startX, startY));
        activePath[startX][startY] = true;

        backtrack(startX, startY, endX, endY, 0, initialInventory);

        return searchHistory; 
    }

    private void backtrack(int x, int y, int endX, int endY, int currentCost, Set<Integer> inventory) {
        // --- 1. BRANCH AND BOUND MURNI (Tanpa DP Array) ---
        if (currentCost >= bestCost) {
            return; // Rute sudah kalah efisien, potong cabangnya!
        }

        // --- 2. CEK KEMENANGAN ---
        if (x == endX && y == endY) {
            int collectedNpcs = 0;
            for (int item : inventory) {
                if (item >= 1000) collectedNpcs++; 
            }

            if (collectedNpcs == totalNpcCount) {
                bestCost = currentCost; // Update rekor tertinggi
            }
            return;
        }

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // --- 3. POHON KEPUTUSAN BACKTRACKING ---
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                
                // CONSTRAINT 1: Simple Path Rule (Tidak boleh injak jejak sendiri)
                if (activePath[nx][ny]) {
                    continue; 
                }

                // CONSTRAINT 2: Validasi Kunci Gerbang
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && !inventory.contains(requiredGate)) {
                    continue; 
                }

                // Persiapan konsekuensi
                int nextCost = currentCost + costMap[nx][ny];
                Set<Integer> nextInventory = new HashSet<>(inventory);
                if (plateMap[nx][ny] != 0) nextInventory.add(plateMap[nx][ny]);
                if (npcMap[nx][ny] != 0) nextInventory.add(npcMap[nx][ny]);

                // Fitur Look-Ahead Sederhana (Mencegah pencatatan bodoh untuk Branch and Bound)
                if (nextCost >= bestCost) continue;

                // ==========================================
                // ANIMASI & LOGIKA: DO - RECURSE - UNDO
                // ==========================================

                // DO
                activePath[nx][ny] = true;
                searchHistory.add(new BacktrackInstantSolved.Point(nx, ny));

                // RECURSE
                backtrack(nx, ny, endX, endY, nextCost, nextInventory);

                // UNDO (Mundur dan hapus jejak agar bisa dilewati rute lain)
                searchHistory.add(new BacktrackInstantSolved.Point(x, y));
                activePath[nx][ny] = false;
            }
        }
    }
}