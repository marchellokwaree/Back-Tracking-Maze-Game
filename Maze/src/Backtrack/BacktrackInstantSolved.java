package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class BacktrackInstantSolved {

    private int[][] costMap;
    private int[][] plateMap;
    private int[][] gateMap;
    private int[][] npcMap;
    private int rows;
    private int cols;
    
    private int totalNpcCount = 0; 

    private int bestCost;
    private List<Point> bestPath;
    
    // PENGGANTI MEMORI: Array visited sederhana khusus untuk jalur yang sedang dilewati saat ini
    private boolean[][] visited;

    public static class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        @Override
        public String toString() { return "(" + x + ", " + y + ")"; }
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
                        costMap[r][c] = -1; // Tembok
                    } else if (t.equals("F")) {
                        costMap[r][c] = 10; // Api
                    } else if (t.equals("I")) {
                        costMap[r][c] = 5;  // Es
                    } else {
                        costMap[r][c] = 1;  // Jalan biasa
                        
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

    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        bestCost = Integer.MAX_VALUE;
        bestPath = new ArrayList<>();
        visited = new boolean[rows][cols]; // Inisialisasi visited array

        Set<Integer> initialInventory = new HashSet<>();
        if (plateMap[startX][startY] != 0) initialInventory.add(plateMap[startX][startY]);
        if (npcMap[startX][startY] != 0) initialInventory.add(npcMap[startX][startY]);

        List<Point> currentPath = new ArrayList<>();
        currentPath.add(new Point(startX, startY));
        
        // Tandai titik awal sebagai sudah dikunjungi
        visited[startX][startY] = true;

        backtrack(startX, startY, endX, endY, 0, currentPath, initialInventory);

        if (!bestPath.isEmpty()) {
            System.out.println("Jalur ditemukan dengan cost terbaik: " + bestCost);
        } else {
            System.out.println("Tidak ada jalur yang memungkinkan. Cek rute Maze Anda.");
        }
        
        return bestPath;
    }

    private void backtrack(int x, int y, int endX, int endY, int currentCost, List<Point> currentPath, Set<Integer> inventory) {
        // --- 1. BOUNDING (Pruning) ---
        // Jika cost saat ini sudah lebih mahal atau sama dengan cost terbaik yang pernah ditemukan, hentikan pencarian di cabang ini
        if (currentCost >= bestCost) {
            return;
        }

        // --- 2. CEK KEMENANGAN ---
        if (x == endX && y == endY) {
            int collectedNpcs = 0;
            for (int item : inventory) {
                if (item >= 1000) collectedNpcs++; 
            }

            if (collectedNpcs == totalNpcCount) {
                bestCost = currentCost;
                bestPath = new ArrayList<>(currentPath); 
            }
            return;
        }

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // --- 3. EKSPLORASI POHON KEPUTUSAN ---
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            // Cek batas map, tembok (-1), dan memastikan kotak belum dikunjungi di rute saat ini
            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1 && !visited[nx][ny]) {
                
                // CONSTRAINT: Cek Gerbang & Kunci
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && !inventory.contains(requiredGate)) {
                    continue; // Tidak punya kunci di tas, lewati arah ini
                }

                // PERSIAPAN STATE BARU
                int nextCost = currentCost + costMap[nx][ny];
                
                Set<Integer> nextInventory = new HashSet<>(inventory);
                if (plateMap[nx][ny] != 0) nextInventory.add(plateMap[nx][ny]);
                if (npcMap[nx][ny] != 0) nextInventory.add(npcMap[nx][ny]);

                // --- DO: Lakukan Keputusan ---
                visited[nx][ny] = true; // Tandai sedang dilewati
                currentPath.add(new Point(nx, ny));

                // --- RECURSE: Eksplorasi Keputusan Selanjutnya ---
                backtrack(nx, ny, endX, endY, nextCost, currentPath, nextInventory);

                // --- UNDO: Batalkan Keputusan (Backtrack murni) ---
                currentPath.remove(currentPath.size() - 1);
                visited[nx][ny] = false; // Hapus tanda agar bisa dilewati oleh cabang rute lain
            }
        }
    }
}