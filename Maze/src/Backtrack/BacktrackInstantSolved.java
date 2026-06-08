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
    private int maxStates = 1;    

    // --- STRUKTUR DATA KHUSUS DYNAMIC PROGRAMMING ---
    private int[][][] memo;             // Menyimpan hasil sub-masalah (Memoization)
    private boolean[][][] inPath;       // Mencegah siklus tak berujung (Cycle prevention)
    
    // Array untuk merekonstruksi jalur (karena DP murni tidak mencatat List saat rekursi)
    private int[][][] nextMoveX;
    private int[][][] nextMoveY;
    private int[][][] nextMoveState;

    // Nilai representasi tak terhingga (agar tidak overflow saat dijumlahkan)
    private final int INF = Integer.MAX_VALUE / 2;

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
            maxStates = 1 << currentBit; 
        }
    }

    public List<Point> solve(int startX, int startY, int endX, int endY) {
        if (costMap == null) return new ArrayList<>();

        // Inisialisasi memori DP
        memo = new int[rows][cols][maxStates];
        inPath = new boolean[rows][cols][maxStates];
        nextMoveX = new int[rows][cols][maxStates];
        nextMoveY = new int[rows][cols][maxStates];
        nextMoveState = new int[rows][cols][maxStates];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int s = 0; s < maxStates; s++) {
                    memo[r][c][s] = -1; // -1 berarti belum dihitung
                }
            }
        }

        int startState = plateMap[startX][startY] | npcMap[startX][startY];

        // Eksekusi fungsi DP
        int optimalCost = dpGetMinCost(startX, startY, endX, endY, startState);

        List<Point> optimalPath = new ArrayList<>();

        // Jika hasilnya INF, berarti tidak ada jalan valid
        if (optimalCost >= INF) {
            System.out.println("Tidak ada jalur valid untuk mencapai tujuan.");
            return optimalPath;
        }

        System.out.println("Jalur DP optimal ditemukan dengan total biaya: " + optimalCost);

        // Rekonstruksi jalur dari memori DP (Forward tracking)
        int currX = startX, currY = startY, currState = startState;
        optimalPath.add(new Point(currX, currY));

        while (currX != endX || currY != endY) {
            int nx = nextMoveX[currX][currY][currState];
            int ny = nextMoveY[currX][currY][currState];
            int nState = nextMoveState[currX][currY][currState];

            optimalPath.add(new Point(nx, ny));
            currX = nx;
            currY = ny;
            currState = nState;
        }

        return optimalPath;
    }

    /**
     * FUNGSI DYNAMIC PROGRAMMING MURNI (Top-Down / Memoization)
     * Mengembalikan "Total Biaya Minimal" dari titik (x,y) menuju titik akhir.
     */
    private int dpGetMinCost(int x, int y, int endX, int endY, int stateMask) {
        // 1. BASE CASE (Kondisi Berhenti)
        if (x == endX && y == endY) {
            if ((stateMask & totalNpcMask) == totalNpcMask) {
                return 0; // Sampai di tujuan dengan syarat lengkap, biaya sisa = 0
            } else {
                return INF; // Sampai di tujuan tapi NPC kurang = Jalur tidak valid
            }
        }

        // 2. MEMOIZATION CHECK (Jika sub-masalah ini sudah pernah dihitung, langsung return)
        if (memo[x][y][stateMask] != -1) {
            return memo[x][y][stateMask];
        }

        // Tandai kotak ini sedang dikunjungi dalam rekursi saat ini untuk mencegah siklus (loop)
        inPath[x][y][stateMask] = true;

        int minSubCost = INF;
        int bestNx = -1, bestNy = -1, bestNState = -1;

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        // 3. PERSAMAAN REKURSIF (Hitung semua cabang, pilih yang termurah)
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && costMap[nx][ny] != -1) {
                int requiredGate = gateMap[nx][ny];
                if (requiredGate != 0 && (stateMask & requiredGate) == 0) continue;

                int nextStateMask = stateMask | plateMap[nx][ny] | npcMap[nx][ny];

                // Cegah kembali ke kotak yang sedang diproses di rantai rekursi ini
                if (!inPath[nx][ny][nextStateMask]) {
                    
                    // Rekursi untuk mencari biaya dari kotak selanjutnya ke tujuan akhir
                    int costFromNextNode = dpGetMinCost(nx, ny, endX, endY, nextStateMask);
                    
                    if (costFromNextNode != INF) {
                        // Persamaan DP: Biaya Total = Biaya melangkah ke (nx,ny) + Biaya dari (nx,ny) ke akhir
                        int totalCost = costMap[nx][ny] + costFromNextNode;
                        
                        if (totalCost < minSubCost) {
                            minSubCost = totalCost;
                            bestNx = nx; bestNy = ny; bestNState = nextStateMask;
                        }
                    }
                }
            }
        }

        // 4. UNDO STATE & SIMPAN HASIL KE MEMORI
        inPath[x][y][stateMask] = false;
        memo[x][y][stateMask] = minSubCost;

        // 5. SIMPAN ARAH JALUR TERBAIK (Untuk rekonstruksi nanti)
        nextMoveX[x][y][stateMask] = bestNx;
        nextMoveY[x][y][stateMask] = bestNy;
        nextMoveState[x][y][stateMask] = bestNState;

        return minSubCost;
    }
}