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

    // Item registries
    private List<Integer> allNpcIds;

    // Nilai representasi tak terhingga
    private final int INF = Integer.MAX_VALUE / 2;

    // --- STRUKTUR DATA DP ---
    // Key = "x,y|npc1,npc2,...|plate1,plate2,..." (snapshot lengkap, bukan bitmask)
    // Value = biaya minimum dari titik tersebut ke tujuan
    private Map<String, Integer> memo;

    // Untuk rekonstruksi jalur: key yang sama, value = "nx,ny" langkah terbaik
    private Map<String, String> nextMove;

    // Set untuk mencegah siklus dalam SATU rantai rekursi
    // Key = "x,y|collectedNpcs|collectedPlates" — unik per konteks
    private Set<String> inPath;

    // Koleksi item pada jalur rekursi saat ini (dimodifikasi + di-undo)
    private Set<Integer> collectedNpcs;
    private Set<Integer> collectedPlates;
    private Set<Integer> openedGates;

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
            costMap  = new int[rows][cols];
            plateMap = new int[rows][cols];
            gateMap  = new int[rows][cols];
            npcMap   = new int[rows][cols];

            allNpcIds   = new ArrayList<>();
            Map<Integer, Boolean> seenPlate = new HashMap<>();
            int npcCounter = 1;

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
                            int id = npcCounter++;
                            npcMap[r][c] = id;
                            allNpcIds.add(id);
                        } else if (t.startsWith("P") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                plateMap[r][c] = id;
                                if (!seenPlate.containsKey(id)) seenPlate.put(id, true);
                            } catch (NumberFormatException e) {}
                        } else if (t.startsWith("D") && t.length() > 1) {
                            try {
                                int id = Integer.parseInt(t.substring(1));
                                gateMap[r][c] = id;
                            } catch (NumberFormatException e) {}
                        }
                    }
                }
            }
        }
    }

    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        memo     = new HashMap<>();
        nextMove = new HashMap<>();
        inPath   = new HashSet<>();

        collectedNpcs   = new HashSet<>();
        collectedPlates = new HashSet<>();
        openedGates     = new HashSet<>();

        collectItemAt(startX, startY);

        int optimalCost = dpGetMinCost(startX, startY, endX, endY);

        List<Point> optimalPath = new ArrayList<>();

        if (optimalCost >= INF) {
            System.out.println("Tidak ada jalur valid untuk mencapai tujuan.");
            return optimalPath;
        }

        System.out.println("Jalur DP optimal ditemukan dengan total biaya: " + optimalCost);

        // Rekonstruksi jalur dari memori DP (Forward tracking)
        // Kita perlu replay koleksi item saat rekonstruksi agar kunci memo cocok
        Set<Integer> replayNpcs   = new HashSet<>(collectedNpcs);
        Set<Integer> replayPlates = new HashSet<>(collectedPlates);
        Set<Integer> replayGates  = new HashSet<>(openedGates);

        int currX = startX, currY = startY;
        optimalPath.add(new Point(currX, currY));

        while (currX != endX || currY != endY) {
            String key = makeKey(currX, currY, replayNpcs, replayPlates);
            String best = nextMove.get(key);
            if (best == null) break;

            String[] parts = best.split(",");
            int nx = Integer.parseInt(parts[0]);
            int ny = Integer.parseInt(parts[1]);

            // Replay item collection untuk maju ke langkah berikutnya
            if (npcMap[nx][ny] != 0)   replayNpcs.add(npcMap[nx][ny]);
            if (plateMap[nx][ny] != 0) { replayPlates.add(plateMap[nx][ny]); replayGates.add(plateMap[nx][ny]); }

            optimalPath.add(new Point(nx, ny));
            currX = nx;
            currY = ny;
        }

        return optimalPath;
    }

    /**
     * FUNGSI DYNAMIC PROGRAMMING MURNI (Top-Down / Memoization)
     *
     * Kunci memo = (x, y, set NPC terkumpul, set plate terkumpul).
     * Ini menggantikan bitmask state mask — setiap kombinasi unik item yang dikumpulkan
     * diperlakukan sebagai submasalah yang berbeda, persis seperti sebelumnya,
     * tapi tanpa enkoding bitmask.
     */
    private int dpGetMinCost(int x, int y, int endX, int endY) {
        // 1. BASE CASE
        if (x == endX && y == endY) {
            return collectedNpcs.containsAll(allNpcIds) ? 0 : INF;
        }

        // 2. MEMOIZATION CHECK — kunci menyertakan snapshot item yang sudah dikumpulkan
        String key = makeKey(x, y, collectedNpcs, collectedPlates);
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        // Tandai (x,y,konteks) ini sedang dalam rantai rekursi aktif
        inPath.add(key);

        int minSubCost = INF;
        String bestNext = null;

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // 3. PERSAMAAN REKURSIF
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx < 0 || nx >= rows || ny < 0 || ny >= cols) continue;
            if (costMap[nx][ny] == -1) continue;

            // Cek gate
            int requiredGate = gateMap[nx][ny];
            if (requiredGate != 0 && !openedGates.contains(requiredGate)) continue;

            // Kumpulkan item di (nx,ny) sementara
            boolean newNpc   = collectNpcAt(nx, ny);
            boolean newPlate = collectPlateAt(nx, ny);
            boolean newGate  = openGateIfPlate(nx, ny);

            // Cek siklus dengan kunci konteks BARU (setelah item dikumpulkan)
            String nextKey = makeKey(nx, ny, collectedNpcs, collectedPlates);
            if (!inPath.contains(nextKey)) {
                int costFromNext = dpGetMinCost(nx, ny, endX, endY);

                if (costFromNext != INF) {
                    int totalCost = costMap[nx][ny] + costFromNext;
                    if (totalCost < minSubCost) {
                        minSubCost = totalCost;
                        bestNext = nx + "," + ny;
                    }
                }
            }

            // UNDO item collection
            if (newNpc)   undoNpcAt(nx, ny);
            if (newPlate) undoPlateAt(nx, ny);
            if (newGate)  undoGate(nx, ny);
        }

        // 4. Hapus dari inPath, simpan ke memo
        inPath.remove(key);
        memo.put(key, minSubCost);
        if (bestNext != null) nextMove.put(key, bestNext);

        return minSubCost;
    }

    /**
     * Membuat kunci unik untuk submasalah (x, y, npc dikumpulkan, plate dikumpulkan).
     * Menggantikan integer bitmask dengan representasi string dari Set.
     * Sorted agar urutan insert tidak mempengaruhi kesamaan kunci.
     */
    private String makeKey(int x, int y, Set<Integer> npcs, Set<Integer> plates) {
        List<Integer> sortedNpcs   = new ArrayList<>(npcs);
        List<Integer> sortedPlates = new ArrayList<>(plates);
        java.util.Collections.sort(sortedNpcs);
        java.util.Collections.sort(sortedPlates);
        return x + "," + y + "|" + sortedNpcs + "|" + sortedPlates;
    }

    private void collectItemAt(int r, int c) {
        collectNpcAt(r, c);
        collectPlateAt(r, c);
        openGateIfPlate(r, c);
    }

    private boolean collectNpcAt(int r, int c) {
        int id = npcMap[r][c];
        if (id != 0 && !collectedNpcs.contains(id)) { collectedNpcs.add(id); return true; }
        return false;
    }

    private boolean collectPlateAt(int r, int c) {
        int id = plateMap[r][c];
        if (id != 0 && !collectedPlates.contains(id)) { collectedPlates.add(id); return true; }
        return false;
    }

    private boolean openGateIfPlate(int r, int c) {
        int id = plateMap[r][c];
        if (id != 0 && collectedPlates.contains(id) && !openedGates.contains(id)) { openedGates.add(id); return true; }
        return false;
    }

    private void undoNpcAt(int r, int c)   { collectedNpcs.remove(npcMap[r][c]); }
    private void undoPlateAt(int r, int c) { collectedPlates.remove(plateMap[r][c]); }
    private void undoGate(int r, int c)    { openedGates.remove(plateMap[r][c]); }
}