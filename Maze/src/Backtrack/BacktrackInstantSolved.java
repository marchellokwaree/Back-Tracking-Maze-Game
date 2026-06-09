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
    
    // PENGGANTI ARRAY 3D & VISITED DFS: Kamus Memori Keputusan (Memoization OOP)
    private Map<String, Integer> decisionMemory;      

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

        rows = rowList.size(); // ngambil jumlah baris.
        if (rows > 0) {
            cols = rowList.get(0).length; // ngambil jumlah kolom.
            costMap = new int[rows][cols]; // Inisialisasi peta biaya
            plateMap = new int[rows][cols]; // Inisialisasi peta pelat, default 0 (tidak ada pelat)
            gateMap = new int[rows][cols]; // Inisialisasi peta gerbang, default 0 (tidak ada gerbang)
            npcMap = new int[rows][cols]; // Inisialisasi peta NPC, default 0 (tidak ada NPC)

            int npcIdCounter = 1000; // ID NPC mulai dari 1000 agar beda dari ID gerbang

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
                            npcMap[r][c] = npcIdCounter++; // ngambil ID NPC secara otomatis, dimulai dari 1000
                            totalNpcCount++;
                        }
                        else if (t.startsWith("P") && t.length() > 1) {
                            try { plateMap[r][c] = Integer.parseInt(t.substring(1)); }  // ngambil ID Pelat dimasukkan ke dalam plateMap
                            catch (NumberFormatException e) {}
                        } 
                        else if (t.startsWith("D") && t.length() > 1) {
                            try { gateMap[r][c] = Integer.parseInt(t.substring(1)); } //ngambil ID Gerbang dimasukkan ke dalam gateMap
                            catch (NumberFormatException e) {} // Gerbang dengan ID tertentu
                        }
                    }
                }
            }
        }
    }

    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>(); // Maze tidak berhasil dimuat, kembalikan jalur kosong

        bestCost = Integer.MAX_VALUE; // Inisialisasi cost terbaik dengan nilai maksimum
        bestPath = new ArrayList<>(); // Inisialisasi jalur terbaik sebagai kosong
        decisionMemory = new HashMap<>(); // Reset memori di setiap pencarian

        Set<Integer> initialInventory = new HashSet<>(); // Tas awal kosong, tapi bisa langsung dapat item di posisi start
        if (plateMap[startX][startY] != 0) initialInventory.add(plateMap[startX][startY]); // Ambil item pelat jika ada di posisi start
        if (npcMap[startX][startY] != 0) initialInventory.add(npcMap[startX][startY]); // Ambil NPC jika ada di posisi start

        List<Point> currentPath = new ArrayList<>(); // Jalur saat ini, akan berubah-ubah selama backtracking
        currentPath.add(new Point(startX, startY)); // Mulai dari titik awal

        backtrack(startX, startY, endX, endY, 0, currentPath, initialInventory);

        if (!bestPath.isEmpty()) {
            System.out.println("Jalur ditemukan dengan cost terbaik: " + bestCost);
        } else {
            System.out.println("Tidak ada jalur yang memungkinkan. Cek rute Maze Anda.");
        }
        
        return bestPath;
    }

    private void backtrack(int x, int y, int endX, int endY, int currentCost, List<Point> currentPath, Set<Integer> inventory) {
        // --- 1. BOUNDING (Mencegah eksplorasi rute yang sudah pasti kalah mahal) ---
        if (currentCost >= bestCost) {
            return;
        }
        
        // --- 2. KAMUS MEMORI KEPUTUSAN (Pengganti SSS Array 3D dan Pengganti 'Visited' DFS) ---
        // Kita jadikan koordinat dan isi tas sebagai kunci riwayat unik
        String memoryKey = x + "," + y + "," + inventory.toString();
        
        // Jika karakter pernah ke sini membawa isi tas yang persis sama, dengan cost lebih murah... Hentikan!
        if (decisionMemory.containsKey(memoryKey) && currentCost >= decisionMemory.get(memoryKey)) {
            return; 
        }
        // Simpan rekor baru
        decisionMemory.put(memoryKey, currentCost);

        // --- 3. CEK KEMENANGAN ---
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

        // --- 4. EKSPLORASI POHON KEPUTUSAN ---
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                
                // CONSTRAINT: Cek Gerbang & Kunci
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && !inventory.contains(requiredGate)) {
                    continue; // Tidak punya kunci di tas, jalan ditolak!
                }

                // PERSIAPAN KONSEKUENSI
                int nextCost = currentCost + costMap[nx][ny];
                
                // Duplikasi tas agar tidak rusak saat membatalkan pilihan (UNDO)
                Set<Integer> nextInventory = new HashSet<>(inventory);
                if (plateMap[nx][ny] != 0) nextInventory.add(plateMap[nx][ny]);
                if (npcMap[nx][ny] != 0) nextInventory.add(npcMap[nx][ny]);

                // DO: Lakukan Keputusan
                currentPath.add(new Point(nx, ny));

                // RECURSE: Eksplorasi Keputusan
                backtrack(nx, ny, endX, endY, nextCost, currentPath, nextInventory);

                // UNDO: Batalkan Keputusan (Kembali ke persimpangan)
                currentPath.remove(currentPath.size() - 1);
            }
        }
    }
}