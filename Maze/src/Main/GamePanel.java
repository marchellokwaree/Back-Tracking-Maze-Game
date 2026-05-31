package Main;

import Obstacle.*;
import Entitiy.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;

public class GamePanel extends JPanel implements Runnable {

    String map1[][] = loadMapFromFileOrDefault();
    private static final String MAP_FILE_PATH = "src/Assets/MAP/Maze1.txt";

    final int tileSize = 32;
    public final int maxScreenCol = 24;
    public final int maxScreenRow = 18;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    public final int maxWorldCol = map1[0].length;
    public final int maxWorldRow = map1.length;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;
    protected int Key;
    public KeyHandler keyH = new KeyHandler();
    public Thread gameThread;
    public Player player;
    public Timer timer;
    public ArrayList<Obstacle> obstacles = new ArrayList<>();
    public ArrayList<Entity> entities = new ArrayList<>();
    public Image floorTile, wallCenter, playerimg, ExitDoor;
    public BufferedImage bufferedImage;

    // Smooth camera position (world coordinates of top-left of screen)
    private double cameraX = 0.0;
    private double cameraY = 0.0;
    private final double cameraSmoothFactor = 0.15; // between 0 (no move) and 1 (instant)
    Image wallCornerTopRight, wallCornerBottomRight, wallCornerTopLeft, wallCornerBottomLeft;
    Image wallVertical, wallHorizontal;
    Image wallEndLeft, wallEndRight, wallEndTop, wallEndBottom;
    Image wallTUp, wallTDown, wallTLeft, wallTRight, wallTIntersection;

    public GamePanel() {

        this.addKeyListener(keyH);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        loadAssets();

        int startX = 0, startY = 0;
        for (int i = 0; i < maxWorldRow; i++) {
            for (int j = 0; j < maxWorldCol; j++) {
                if ("S".equals(map1[i][j])) {
                    startX = j * tileSize;
                    startY = i * tileSize;
                }
            }
        }

        
        this.timer = new Timer(100000); // Timer 100 detik (100.000 ms)
        // Simpan referensi untuk linking gates dengan pressure plates
        ArrayList<PressurePlate> pressurePlates = new ArrayList<>();
        ArrayList<Gate> gates = new ArrayList<>();

        for (int i = 0; i < maxWorldRow; i++) {
            for (int j = 0; j < maxWorldCol; j++) {
                if ("F".equals(map1[i][j])) {
                    obstacles.add(new FireTrap(j * tileSize, i * tileSize, tileSize, tileSize));
                }
                if ("I".equals(map1[i][j])) {
                    obstacles.add(new IceTrap(j * tileSize, i * tileSize, tileSize, tileSize));
                }
            
                if ("G".equals(map1[i][j])) {
                    obstacles.add(new Finish(j * tileSize, i * tileSize, tileSize, tileSize));
                }
                if ("H".equals(map1[i][j])) {
                    obstacles.add(new HealPotion(j * tileSize, i * tileSize, tileSize, tileSize));
                }
                if (map1[i][j].length() > 1) {
                    if (map1[i][j].charAt(0) == 'P') {
                        String id = map1[i][j].substring(1); // Ambil ID setelah 'P'
                        PressurePlate plate = new PressurePlate(j * tileSize, i * tileSize, tileSize, tileSize, id);
                        obstacles.add(plate);
                        pressurePlates.add(plate);
                    } else if (map1[i][j].charAt(0) == 'D') {
                        String id = map1[i][j].substring(1); // Ambil ID setelah 'G'
                        boolean aboveIsWall = (i - 1 >= 0 && "1".equals(map1[i - 1][j]));
                        Gate gate = new Gate(j * tileSize, i * tileSize, tileSize, tileSize, aboveIsWall, id);
                        obstacles.add(gate);
                        gates.add(gate);
                    }
                }

                if ("N".equals(map1[i][j])) {
                    entities.add(new RedHood(j * tileSize, i * tileSize, 0, this));
                    Key++;
                }
            }
        }

        // CONTOH: Link gate ke pressure plate
        // Uncomment dan sesuaikan index untuk mengatur gate mana yang memerlukan
        // trapdoor
        // Jika ada pressure plate di index 0 dan gate di index 0:
        // if (pressurePlates.size() > 0 && gates.size() > 0) {
        // gates.get(0).setRequiredPressurePlate(pressurePlates.get(0));
        // }

        // Pastikan parameter Player sesuai dengan constructor baru di Player.java
        player = new Player(this, keyH, playerimg, startX, startY);

        // Initialize camera at player-centered position (clamped)
        cameraX = player.x - player.screenX;
        cameraY = player.y - player.screenY;
        clampCamera();
    }

    public Player getPlayer() {
        return player; // agar player bisa di akses di obstacle
    }

    public void loadAssets() {
        try {
            // Loading Sprite Sheet
            this.bufferedImage = loadBufferedImage("/Assets/ASSET/ASSETKARAKTER/AnimationSheet.png");
            if (this.bufferedImage != null) {
                this.playerimg = bufferedImage.getSubimage(0, 0, 25, 25);
            }

            // Loading Tiles
            this.floorTile = loadImage("/Assets/lab_tileset_LITE/seperated/tile031.png");

            this.wallCenter = loadImage("/Assets/lab_tileset_LITE/seperated/tile066.png");
            this.wallCornerBottomLeft = loadImage("/Assets/lab_tileset_LITE/seperated/tile067.png");
            this.wallCornerTopLeft = loadImage("/Assets/lab_tileset_LITE/seperated/tile039.png");
            this.wallCornerBottomRight = loadImage("/Assets/lab_tileset_LITE/seperated/tile071.png");
            this.wallCornerTopRight = loadImage("/Assets/lab_tileset_LITE/seperated/tile043.png");
            this.wallVertical = loadImage("/Assets/lab_tileset_LITE/seperated/tile074.png");
            this.wallHorizontal = loadImage("/Assets/lab_tileset_LITE/seperated/tile033.png");
            this.wallEndLeft = loadImage("/Assets/lab_tileset_LITE/seperated/tile052.png");
            this.wallEndRight = loadImage("/Assets/lab_tileset_LITE/seperated/tile047.png");
            this.wallEndTop = loadImage("/Assets/lab_tileset_LITE/seperated/tile058.png");
            this.wallEndBottom = loadImage("/Assets/lab_tileset_LITE/seperated/tile066.png");
            this.wallTUp = loadImage("/Assets/lab_tileset_LITE/seperated/tile042.png");
            this.wallTDown = loadImage("/Assets/lab_tileset_LITE/seperated/tile041.png");
            this.wallTLeft = loadImage("/Assets/lab_tileset_LITE/seperated/tile054.png");
            this.wallTRight = loadImage("/Assets/lab_tileset_LITE/seperated/tile055.png");

        } catch (Exception e) {
            System.err.println("Error loading assets!");
            e.printStackTrace();
        }
    }

    private BufferedImage loadBufferedImage(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream != null) {
                return ImageIO.read(stream);
            }
            File file = resolveImageFile(path);
            return ImageIO.read(file);
        } catch (Exception e) {
            System.err.println("Failed to load buffered image: " + path + " -> " + e.getMessage());
            return null;
        }
    }

    public Image loadImage(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                return new ImageIcon(url).getImage();
            }
            File file = resolveImageFile(path);
            return new ImageIcon(file.getAbsolutePath()).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path + " -> " + e.getMessage());
            return null;
        }
    }

    private File resolveImageFile(String path) {
        String normalizedPath = path.replace("/", File.separator);
        String userDir = System.getProperty("user.dir");

        File candidate = new File(userDir + File.separator + "src" + normalizedPath);
        if (candidate.exists()) {
            return candidate;
        }

        candidate = new File(userDir + normalizedPath);
        if (candidate.exists()) {
            return candidate;
        }

        candidate = new File(userDir + File.separator + "Maze" + File.separator + "src" + normalizedPath);
        if (candidate.exists()) {
            return candidate;
        }

        return new File(userDir + File.separator + "src" + normalizedPath);
    }

    private String resolveMapFilePath(String path) {
        String normalizedPath = path.replace("/", File.separator);
        String userDir = System.getProperty("user.dir");

        File candidate = new File(userDir + File.separator + normalizedPath);
        if (candidate.exists()) {
            return candidate.getAbsolutePath();
        }

        candidate = new File(userDir + File.separator + "Maze" + File.separator + normalizedPath);
        if (candidate.exists()) {
            return candidate.getAbsolutePath();
        }

        return userDir + File.separator + normalizedPath;
    }

    private String[][] loadMapFromFileOrDefault() {
        String filePath = resolveMapFilePath(MAP_FILE_PATH);
        String[][] loaded = new MapLoader().loadMapFromFile(filePath);
        return loaded;
    }

    public int getTileSize() {
        return tileSize;
    }

    public boolean collidesWithWall(int nextX, int nextY, Rectangle hitbox) {
        // Hitung posisi absolut hitbox di koordinat world untuk posisi selanjutnya
        int hitboxLeftX = nextX + hitbox.x;
        int hitboxRightX = nextX + hitbox.x + hitbox.width - 1;
        int hitboxTopY = nextY + hitbox.y;
        int hitboxBottomY = nextY + hitbox.y + hitbox.height - 1;
        // Konversi koordinat pixel absolut ke indeks baris/kolom matriks map
        int left = hitboxLeftX / tileSize;
        int right = hitboxRightX / tileSize;
        int top = hitboxTopY / tileSize;
        int bottom = hitboxBottomY / tileSize;

        // Batasan luar map (Out of Bounds)
        if (left < 0 || right >= maxWorldCol || top < 0 || bottom >= maxWorldRow) {
            return true;
        }

        // Cek apakah 4 sudut hitbox menubruk tembok ("1")
        return "1".equals(map1[top][left]) || "1".equals(map1[top][right]) || "1".equals(map1[bottom][left])
                || "1".equals(map1[bottom][right]);
    }

    public boolean collidesWithClosedGate(int nextX, int nextY, Rectangle hitbox) {
        // 1. Buat objek Rectangle bayangan untuk posisi player selanjutnya
        Rectangle playerFutureBounds = new Rectangle(
                nextX + hitbox.x,
                nextY + hitbox.y,
                hitbox.width,
                hitbox.height);

        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof Gate) {
                Gate gate = (Gate) obstacle;

                // 2. Jika gate tertutup, cek tabrakan menggunakan .intersects()
                if (!gate.open) {
                    // Asumsi: Class Gate memiliki koordinat x, y, dan ukuran sendiri.
                    // Jika Gate Anda sudah punya objek Rectangle sendiri, gunakan itu (misal:
                    // gate.hitbox).
                    Rectangle gateBounds = new Rectangle(gate.x, gate.y, tileSize, tileSize);

                    if (playerFutureBounds.intersects(gateBounds)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
        timer.start(); // Mulai timer saat game thread dimulai
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / 60;
        double delta = 0;
        long lastTime = System.nanoTime(); // Mendapatkan waktu saat ini dalam nanodetik

        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

            checkDamage();
            if (timer.isTimeUp()) {
                // Tampilkan pesan "Game Over" menggunakan JOptionPane
                gameThread = null; // Hentikan game loop
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Time's Up! Game Over!", "Game Over",
                            JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0); // Keluar dari aplikasi setelah menutup dialog
                });
            }
            try {
                Thread.sleep(1000 / 60); // Tidur sebentar untuk mengurangi beban CPU, target sekitar 60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



    }

    public void update() {
        if (player != null) {
            player.update();
        }
        for (Entity entity : entities) {
            if (entity instanceof RedHood) {
                ((RedHood) entity).update();
            }
        }

        if  (timer != null) {
            timer.update();
        }
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof FireTrap) {
                ((FireTrap) obstacle).update();
            }
            if (obstacle instanceof IceTrap) {
                ((IceTrap) obstacle).update();
            }
            if (obstacle instanceof Gate) {
                ((Gate) obstacle).update();
            }
            if (obstacle instanceof PressurePlate) {

                ((PressurePlate) obstacle).update();
            }
            if (obstacle instanceof Finish) {
                ((Finish) obstacle).update();
            }
            if (obstacle instanceof HealPotion) {
                ((HealPotion) obstacle).update();
            }
        }
        // Update smooth camera after updating entities
        updateCamera();
    }

    private void updateCamera() {
        if (player == null) return;

        double targetX = player.x - player.screenX;
        double targetY = player.y - player.screenY;

        cameraX += (targetX - cameraX) * cameraSmoothFactor;
        cameraY += (targetY - cameraY) * cameraSmoothFactor;

        clampCamera();
    }

    private void clampCamera() {
        if (cameraX < 0) cameraX = 0;
        double maxCamX = Math.max(0, worldWidth - screenWidth);
        if (cameraX > maxCamX) cameraX = maxCamX;

        if (cameraY < 0) cameraY = 0;
        double maxCamY = Math.max(0, worldHeight - screenHeight);
        if (cameraY > maxCamY) cameraY = maxCamY;
    }

    public int getCameraXInt() {
        return (int) Math.round(cameraX);
    }

    public int getCameraYInt() {
        return (int) Math.round(cameraY);
    }

    private boolean hasWallAt(int row, int col) {
        if (row < 0 || row >= maxWorldRow || col < 0 || col >= maxWorldCol) {
            return false;
        }
        return "1".equals(map1[row][col]);
    }

    private Image getWallImageForTile(int row, int col) {
        boolean top = hasWallAt(row - 1, col);
        boolean bottom = hasWallAt(row + 1, col);
        boolean left = hasWallAt(row, col - 1);
        boolean right = hasWallAt(row, col + 1);

        // Intersection 4-arah
        if (top && bottom && left && right) {
            if (wallTIntersection != null) {
                return wallTIntersection;
            } else if (wallVertical != null) {
                return wallVertical;
            } else {
                return wallCenter;
            }
        }

        // Pertigaan terbuka ke ATAS ┴ (bawah + kiri + kanan)
        if (bottom && left && right && !top) {
            if (wallTUp != null) {
                return wallTUp;
            } else {
                return wallHorizontal;
            }
        }

        // Pertigaan terbuka ke BAWAH ┬ (atas + kiri + kanan)
        if (top && left && right && !bottom) {
            if (wallTDown != null) {
                return wallTDown;
            } else {
                return wallHorizontal;
            }
        }

        // Pertigaan terbuka ke KIRI ┤ (atas + bawah + kanan)
        if (top && bottom && right && !left) {
            if (wallTLeft != null) {
                return wallTLeft;
            } else {
                return wallVertical;
            }
        }

        // Pertigaan terbuka ke KANAN ├ (atas + bawah + kiri)
        if (top && bottom && left && !right) {
            if (wallTRight != null) {
                return wallTRight;
            } else {
                return wallVertical;
            }
        }

        // --- Straight walls --- (Tidak berubah, sudah benar)
        if (top && bottom && !left && !right) {
            if (wallVertical != null) {
                return wallVertical;
            } else {
                return wallCenter;
            }
        }
        if (left && right && !top && !bottom) {
            if (wallHorizontal != null) {
                return wallHorizontal;
            } else {
                return wallCenter;
            }
        }

        // --- Corners --- (DITUKAR untuk visual yang benar)
        // Jika ada tembok di ATAS dan KANAN, kita butuh pojokan yang visualnya
        // menghadap ke bawah-kiri.
        if (top && right && !bottom && !left) {
            if (wallCornerBottomLeft != null) {
                return wallCornerBottomLeft;
            } else {
                return wallCenter;
            }
        }
        // Jika ada tembok di KANAN dan BAWAH, kita butuh pojokan yang visualnya
        // menghadap ke atas-kiri.
        if (right && bottom && !top && !left) {
            if (wallCornerTopLeft != null) {
                return wallCornerTopLeft;
            } else {
                return wallCenter;
            }
        }
        // Jika ada tembok di BAWAH dan KIRI, kita butuh pojokan yang visualnya
        // menghadap ke atas-kanan.
        if (bottom && left && !top && !right) {
            if (wallCornerTopRight != null) {
                return wallCornerTopRight;
            } else {
                return wallCenter;
            }
        }
        // Jika ada tembok di KIRI dan ATAS, kita butuh pojokan yang visualnya menghadap
        // ke bawah-kanan.
        if (left && top && !bottom && !right) {
            if (wallCornerBottomRight != null) {
                return wallCornerBottomRight;
            } else {
                return wallCenter;
            }
        }

        // --- End pieces --- (DITUKAR untuk visual yang benar)
        // Ujung yang menyambung ke ATAS, butuh gambar ujung yang TERTUTUP (misal:
        // wallEndBottom)
        if (top && !bottom && !left && !right) {
            if (wallEndBottom != null) {
                return wallEndBottom;
            } else {
                return wallCenter;
            }
        }
        // Ujung yang menyambung ke BAWAH, butuh gambar ujung yang TERTUTUP (misal:
        // wallEndTop)
        if (bottom && !top && !left && !right) {
            if (wallEndTop != null) {
                return wallEndTop;
            } else {
                return wallCenter;
            }
        }
        // Ujung yang menyambung ke KIRI, butuh gambar ujung yang TERTUTUP (misal:
        // wallEndRight)
        if (left && !right && !top && !bottom) {
            if (wallEndRight != null) {
                return wallEndRight;
            } else {
                return wallCenter;
            }
        }
        // Ujung yang menyambung ke KANAN, butuh gambar ujung yang TERTUTUP (misal:
        // wallEndLeft)
        if (right && !left && !top && !bottom) {
            if (wallEndLeft != null) {
                return wallEndLeft;
            } else {
                return wallCenter;
            }
        }

        // --- Fallback cases --- (Juga disesuaikan agar pojokan tertutup lebih rapi)
        if ((top && left && right) || (bottom && left && right)) {
            if (wallHorizontal != null) {
                return wallHorizontal;
            } else {
                return wallCenter;
            }
        }
        if ((left && top && bottom) || (right && top && bottom)) {
            if (wallVertical != null) {
                return wallVertical;
            } else {
                return wallCenter;
            }
        }
        if (top || bottom || left || right) {
            if (wallCenter != null) {
                return wallCenter;
            } else {
                return wallHorizontal;
            }
        }

        // --- Default Fallback ---
        if (wallCenter != null) {
            return wallCenter;
        } else {
            return wallHorizontal;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int camX = getCameraXInt();
        int camY = getCameraYInt();

        for (int i = 0; i < maxWorldRow; i++) {
            for (int j = 0; j < maxWorldCol; j++) {

                int worldX = j * tileSize;
                int worldY = i * tileSize;

                int screenX = worldX - camX;
                int screenY = worldY - camY;

                // Hanya gambar tile jika masuk ke dalam pandangan layar monitor
                if (worldX + tileSize > camX &&
                        worldX - tileSize < camX + screenWidth &&
                        worldY + tileSize > camY &&
                        worldY - tileSize < camY + screenHeight) {

                    if (floorTile != null) {
                        g2.drawImage(floorTile, screenX, screenY, tileSize, tileSize, null);
                    }

                    if ("1".equals(map1[i][j])) {
                        Image wallImage = getWallImageForTile(i, j);
                        if (wallImage != null) {
                            g2.drawImage(wallImage, screenX, screenY, tileSize, tileSize, null);
                        }
                    }
                }
            }
        }

        // Gambar semua obstacles, termasuk fire trap animasi
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof FireTrap) {
                ((FireTrap) obstacle).draw(g2, this);
            }
            if (obstacle instanceof IceTrap) {
                ((IceTrap) obstacle).draw(g2, this);
            }
            if (obstacle instanceof PressurePlate) {
                ((PressurePlate) obstacle).draw(g2, this);
            }
            if (obstacle instanceof Gate) {
                ((Gate) obstacle).draw(g2, this);
            }
            if (obstacle instanceof Finish) {
                ((Finish) obstacle).draw(g2, this);
            }
            if (obstacle instanceof HealPotion) {
                ((HealPotion) obstacle).draw(g2, this);
            }
        }

        for (Entity entity : entities) {
            if (entity instanceof RedHood) {
                ((RedHood) entity).draw(g2, this);
            }
        }

        if (player != null){
                player.draw(g2);
                player.darah.draw(g2);

        }

        if (timer != null) {
            timer.draw(g2, this);
        }

        g2.dispose();
    }

    protected void WinGame() {
        System.out.println("Congratulations! You've reached the exit!");
        System.exit(0); // Keluar dari game
    }

    protected void checkDamage() {
        java.util.Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle obstacle = it.next();
            if (obstacle instanceof Finish) {
                Finish finish = (Finish) obstacle;
                if (finish.collidesWith(player.x, player.y, tileSize)) {
                    if (Key == 0) {
                        WinGame();
                    } else {
                        System.out.println("You need to find all Red Hood to unlock the exit!");
                    }
                }
            }
            if (obstacle instanceof FireTrap) {

                FireTrap fireTrap = (FireTrap) obstacle;
                Rectangle fireHitbox = new Rectangle(fireTrap.x, fireTrap.y, 30, 30);

                if (fireTrap.active && fireHitbox.intersects(player.getHitbox())
                        && player.damageCooldown == 0) {
                    player.darah.takeDamage(30);
                    
                    if (player.darah.getCurrentHP() < 0) {
                        player.darah.update(0);
                    }
                    player.damageCooldown = 60; // Jangan terkena damage lagi selama sekitar 1 detik

                    System.out.println("Player hit by fire trap! HP: " + player.darah.getCurrentHP());
                    if (player.darah.getCurrentHP() <= 0) {
                        System.out.println("Game Over! Player has been defeated.");
                        System.exit(0);
                    }
                }
            }
            if (obstacle instanceof PressurePlate) {
                PressurePlate pressurePlate = (PressurePlate) obstacle;
                Rectangle pressureHitbox = new Rectangle(pressurePlate.x, pressurePlate.y, tileSize, tileSize);
                if (pressureHitbox.intersects(player.getHitbox())) {
                    pressurePlate.activate();
                    for (Obstacle o : obstacles) {
                        if (o instanceof Gate) {
                            Gate gate = (Gate) o;
                            if (gate.ID.equals(pressurePlate.ID) && pressurePlate.activated) { // Cek apakah ID gate
                                                                                               // cocok dengan ID
                                                                                               // pressure plate
                                gate.alrOpen = true;
                                gate.open = true;
                                gate.openGate();
                            }
                        }
                    }
                } else {
                    pressurePlate.deactivate();
                }
            }
            if (obstacle instanceof IceTrap) {
                IceTrap iceTrap = (IceTrap) obstacle;
                Rectangle iceHitbox = new Rectangle(iceTrap.x, iceTrap.y, tileSize, tileSize);
                if (iceTrap.active && iceHitbox.intersects(player.getHitbox())) {
                    player.applySlow(5); // Contoh: efek es berlangsung selama 2 detik (120 frame)
                }
            }
            if (obstacle instanceof HealPotion) {
                HealPotion healPotion = (HealPotion) obstacle;
                Rectangle healHitbox = new Rectangle(healPotion.x, healPotion.y, tileSize, tileSize);
                if (healHitbox.intersects(player.getHitbox())) {
                    player.darah.heal(50); // Contoh: menyembuhkan 50 HP
                    if (player.darah.getCurrentHP() > 100) {
                        player.darah.update(100); // Batas maksimal HP
                    }
                    it.remove(); // Hapus potion setelah digunakan dengan aman
                    System.out.println("Player consumed a heal potion! HP: " + player.darah.getCurrentHP());
                }
            }
        }

        java.util.Iterator<Entity> entityIt = entities.iterator();
        while (entityIt.hasNext()) {
            Entity entity = entityIt.next();
            if (entity instanceof RedHood) {
                RedHood redHood = (RedHood) entity;
                if (redHood.shouldRemove()) {
                    entityIt.remove();
                    continue;
                }

                Rectangle enemyHitbox = new Rectangle(redHood.x, redHood.y, redHood.hitbox.width,
                        redHood.hitbox.height);
                if (enemyHitbox.intersects(player.getHitbox()) && !redHood.active) {
                    redHood.startDisappear();
                    System.out.println("you got a key ");
                    Key--;
                    
                }
            }
        }
    }

}