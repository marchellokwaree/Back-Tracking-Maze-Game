package Backtrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BacktrackInstantSolved {

    private int[][] costMap;
    private int[][] plateMap;
    private int[][] gateMap;
    private int[][] npcMap;
    private int rows;
    private int cols;
    
    private int totalNpcMask = 0; 

    private int bestCost;
    private List<Point> bestPath;
    
    // SOLUSI ELEGAN DP: Menggunakan HashMap (Bukan Array 3D State Space!)
    private Map<String, Integer> dpMemo;      

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

            int currentBit = 0; 
            Map<Integer, Integer> plateBits = new HashMap<>();

            for (int r = 0; r < rows; r++) {
                String[] tokens = rowList.get(r);
                for (int c = 0; c < cols; c++) {
                    String t = tokens[c];
                    
                    if (t.equals("1")) {
                        costMap[r][c] = -1; // Tembok
                    } else if (t.equals("F")) {
                        costMap[r][c] = 10; // Api (Mahal)
                    } else if (t.equals("I")) {
                        costMap[r][c] = 5;  // Es (Sedang)
                    } else {
                        costMap[r][c] = 1;  // Jalan biasa
                        
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

    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        bestCost = Integer.MAX_VALUE;
        bestPath = new ArrayList<>();
        dpMemo = new HashMap<>(); // Bersihkan DP sebelum mencari jalan

        int startState = plateMap[startX][startY] | npcMap[startX][startY];
        List<Point> currentPath = new ArrayList<>();
        
        currentPath.add(new Point(startX, startY));

        // Mulai mesin pencari
        backtrack(startX, startY, endX, endY, 0, startState, currentPath);

        if (!bestPath.isEmpty()) {
            System.out.println("Jalur ditemukan dengan cost terbaik: " + bestCost);
        } else {
            System.out.println("Tidak ada jalur yang memungkinkan. Cek desain map (Maze1.txt) Anda.");
        }
        
        return bestPath;
    }

    private void backtrack(int x, int y, int endX, int endY, int currentCost, int stateMask, List<Point> currentPath) {
        // 1. BOUNDING: Jika sudah lebih mahal dari rekor pemenang, batalkan.
        if (currentCost >= bestCost) {
            return;
        }
        
        // 2. DP MEMOIZATION DENGAN HASHMAP
        // Kita buat "Kunci Memori" dari koordinat + barang bawaan saat ini
        String stateKey = x + "," + y + "," + stateMask;
        
        // Jika status ini sudah pernah dicapai dengan rute yang lebih murah, batalkan (Pruning)
        if (dpMemo.containsKey(stateKey) && currentCost >= dpMemo.get(stateKey)) {
            return; 
        }
        // Simpan rekor termurah yang baru
        dpMemo.put(stateKey, currentCost);

        // 3. CEK KEMENANGAN
        if (x == endX && y == endY) {
            if ((stateMask & totalNpcMask) == totalNpcMask) {
                bestCost = currentCost;
                bestPath = new ArrayList<>(currentPath); 
            }
            return;
        }

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // 4. EKSPLORASI BACKTRACKING (DO - RECURSE - UNDO)
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                
                // Cek apakah punya kunci untuk lewat gerbang
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && (stateMask & requiredGate) == 0) {
                    continue; // Jalan ditutup, tidak punya kunci!
                }

                int nextStateMask = stateMask | plateMap[nx][ny] | npcMap[nx][ny];
                int nextCost = currentCost + costMap[nx][ny];

                // DO: Melangkah
                currentPath.add(new Point(nx, ny));

                // RECURSE: Cari jalan lebih dalam
                backtrack(nx, ny, endX, endY, nextCost, nextStateMask, currentPath);

                // UNDO: Mundur untuk coba arah lain
                currentPath.remove(currentPath.size() - 1);
            }
        }
    }
}