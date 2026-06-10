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
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import Backtrack.BacktrackInstantSolved;
import Backtrack.BacktrackTryAndError;

import java.awt.Rectangle;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;

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

    // --- AUTOSOLVE ADDITIONS ---
    public BacktrackInstantSolved solver1;
    public BacktrackTryAndError solver2;
    public List<BacktrackInstantSolved.Point> autoPath;
    public int currentPathIndex = 0;
    public boolean autoSolveActive;// Set to true to instantly auto-walk to the finish line
    public int backtrackMode = 0;
    private final JFrame parentFrame;
    private boolean endGameShown = false;
    // ---------------------------

    public GamePanel(JFrame parentFrame, int backtrackMode) {
        this.parentFrame = parentFrame;
        this.backtrackMode = backtrackMode;
        if (backtrackMode > 0) {
            this.autoSolveActive = true; // Aktifkan autosolve jika mode 1 atau 2
        }

        this.addKeyListener(keyH);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.addKeyListener(keyH);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        loadAssets();

        int startX = 0, startY = 0;
        int startGridRow = 0, startGridCol = 0;
        int endGridRow = 0, endGridCol = 0;

        for (int i = 0; i < maxWorldRow; i++) {
            for (int j = 0; j < maxWorldCol; j++) {
                if ("S".equals(map1[i][j])) {
                    startX = j * tileSize;
                    startY = i * tileSize;
                    startGridRow = i;
                    startGridCol = j;
                }
                if ("G".equals(map1[i][j])) {
                    endGridRow = i;
                    endGridCol = j;
                }
            }
        }

        this.timer = new Timer(1000000); // Timer 100 detik (100.000 ms)

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

                    } else if (map1[i][j].charAt(0) == 'D') {
                        String id = map1[i][j].substring(1); // Ambil ID setelah 'G'
                        boolean aboveIsWall = (i - 1 >= 0 && "1".equals(map1[i - 1][j]));
                        Gate gate = new Gate(j * tileSize, i * tileSize, tileSize, tileSize, aboveIsWall, id);
                        obstacles.add(gate);

                    }
                }

                if ("N".equals(map1[i][j])) {
                    entities.add(new RedHood(j * tileSize, i * tileSize, 0, this));
                    Key++;
                }
            }
        }

        // Pastikan parameter Player sesuai dengan constructor baru di Player.java
        player = new Player(this, keyH, playerimg, startX, startY);

        // --- AUTOSOLVE INITIALIZATION ---
        if (autoSolveActive) {
            String solvedPathFile = resolveMapFilePath(MAP_FILE_PATH);

            // Cek mode mana yang dipilih
            if (backtrackMode == 1) {
                solver1 = new BacktrackInstantSolved(solvedPathFile);
                autoPath = solver1.solve(startGridRow, startGridCol, endGridRow, endGridCol);
            } else if (backtrackMode == 2) {
                solver2 = new BacktrackTryAndError(solvedPathFile);
                autoPath = solver2.solve(startGridRow, startGridCol, endGridRow, endGridCol);
            }
        }
        // --------------------------------

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
        int hitboxLeftX = nextX + hitbox.x;
        int hitboxRightX = nextX + hitbox.x + hitbox.width - 1;
        int hitboxTopY = nextY + hitbox.y;
        int hitboxBottomY = nextY + hitbox.y + hitbox.height - 1;
        int left = hitboxLeftX / tileSize;
        int right = hitboxRightX / tileSize;
        int top = hitboxTopY / tileSize;
        int bottom = hitboxBottomY / tileSize;

        if (left < 0 || right >= maxWorldCol || top < 0 || bottom >= maxWorldRow) {
            return true;
        }

        return "1".equals(map1[top][left]) || "1".equals(map1[top][right]) || "1".equals(map1[bottom][left])
                || "1".equals(map1[bottom][right]);
    }

    public boolean collidesWithClosedGate(int nextX, int nextY, Rectangle hitbox) {
        Rectangle playerFutureBounds = new Rectangle(
                nextX + hitbox.x,
                nextY + hitbox.y,
                hitbox.width,
                hitbox.height);

        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof Gate) {
                Gate gate = (Gate) obstacle;
                if (!gate.open) {
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
        long lastTime = System.nanoTime();

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
                gameThread = null;
                LoseGame();
            }
            try {
                Thread.sleep(1000 / 60);
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

        if (timer != null) {
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
        if (player == null)
            return;

        double targetX = player.x - player.screenX;
        double targetY = player.y - player.screenY;

        cameraX += (targetX - cameraX) * cameraSmoothFactor;
        cameraY += (targetY - cameraY) * cameraSmoothFactor;

        clampCamera();
    }

    private void clampCamera() {
        if (cameraX < 0)
            cameraX = 0;
        double maxCamX = Math.max(0, worldWidth - screenWidth);
        if (cameraX > maxCamX)
            cameraX = maxCamX;

        if (cameraY < 0)
            cameraY = 0;
        double maxCamY = Math.max(0, worldHeight - screenHeight);
        if (cameraY > maxCamY)
            cameraY = maxCamY;
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

        if (top && bottom && left && right) {
            if (wallTIntersection != null) {
                return wallTIntersection;
            } else if (wallVertical != null) {
                return wallVertical;
            } else {
                return wallCenter;
            }
        }

        if (bottom && left && right && !top) {
            if (wallTUp != null) {
                return wallTUp;
            } else {
                return wallHorizontal;
            }
        }

        if (top && left && right && !bottom) {
            if (wallTDown != null) {
                return wallTDown;
            } else {
                return wallHorizontal;
            }
        }

        if (top && bottom && right && !left) {
            if (wallTLeft != null) {
                return wallTLeft;
            } else {
                return wallVertical;
            }
        }

        if (top && bottom && left && !right) {
            if (wallTRight != null) {
                return wallTRight;
            } else {
                return wallVertical;
            }
        }

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

        if (top && right && !bottom && !left) {
            if (wallCornerBottomLeft != null) {
                return wallCornerBottomLeft;
            } else {
                return wallCenter;
            }
        }
        if (right && bottom && !top && !left) {
            if (wallCornerTopLeft != null) {
                return wallCornerTopLeft;
            } else {
                return wallCenter;
            }
        }
        if (bottom && left && !top && !right) {
            if (wallCornerTopRight != null) {
                return wallCornerTopRight;
            } else {
                return wallCenter;
            }
        }
        if (left && top && !bottom && !right) {
            if (wallCornerBottomRight != null) {
                return wallCornerBottomRight;
            } else {
                return wallCenter;
            }
        }

        if (top && !bottom && !left && !right) {
            if (wallEndBottom != null) {
                return wallEndBottom;
            } else {
                return wallCenter;
            }
        }
        if (bottom && !top && !left && !right) {
            if (wallEndTop != null) {
                return wallEndTop;
            } else {
                return wallCenter;
            }
        }
        if (left && !right && !top && !bottom) {
            if (wallEndRight != null) {
                return wallEndRight;
            } else {
                return wallCenter;
            }
        }
        if (right && !left && !top && !bottom) {
            if (wallEndLeft != null) {
                return wallEndLeft;
            } else {
                return wallCenter;
            }
        }

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

        if (player != null) {
            player.draw(g2);
            player.darah.draw(g2);
        }

        if (timer != null) {
            timer.draw(g2, this);
        }

        g2.dispose();
    }

    protected void LoseGame() {
        if (endGameShown) {
            return;
        }

        endGameShown = true;
        if (gameThread != null) {
            gameThread = null;
        }

        int timeSpent = timer != null ? timer.getMaxTimeInSeconds() - timer.getCurrentTime() : 0;
        System.out.println("Game Over! Displaying end game panel.");

        SwingUtilities.invokeLater(() -> {
            parentFrame.setContentPane(new EndGamePanel(parentFrame, false, timeSpent));
            parentFrame.revalidate();
            parentFrame.pack();
            parentFrame.setLocationRelativeTo(null);
        });
    }

    protected void WinGame() {
        if (endGameShown) {
            return;
        }

        endGameShown = true;
        if (gameThread != null) {
            gameThread = null;
        }

        int timeSpent = timer.getMaxTimeInSeconds() - timer.getCurrentTime();
        System.out.println("Congratulations! You've reached the exit!");
        System.out.println("Time spent: " + timeSpent + " seconds.");

        SwingUtilities.invokeLater(() -> {
            parentFrame.setContentPane(new EndGamePanel(parentFrame, true, timeSpent));
            parentFrame.revalidate();
            parentFrame.pack();
            parentFrame.setLocationRelativeTo(null);
        });
    }

    protected void checkDamage() {
        java.util.Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle obstacle = it.next();
            if (obstacle instanceof Finish) {
                Finish finish = (Finish) obstacle;
                if (finish.collidesWith(player.x, player.y, tileSize)) {
                    // --- AUTOSOLVE WIN LOGIC OVERRIDE ---
                    if (Key <= 0) {
                        WinGame();
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this,
                                    "You need to find all Red Hood to unlock the exit!",
                                    "Exit Locked",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                }
            }
            if (obstacle instanceof FireTrap) {

                FireTrap fireTrap = (FireTrap) obstacle;
                Rectangle fireHitbox = new Rectangle(fireTrap.x, fireTrap.y, 30, 30);

                // --- IMMUNITY REMOVED: Auto-solve can now take damage! ---
                if (fireTrap.active && fireHitbox.intersects(player.getHitbox())
                        && player.damageCooldown == 0) {

                    player.darah.takeDamage(0);

                    if (player.darah.getCurrentHP() < 0) {
                        player.darah.update(0);
                    }
                    player.damageCooldown = 60;

                    System.out.println("Player hit by fire trap! HP: " + player.darah.getCurrentHP());
                    if (player.darah.getCurrentHP() <= 0) {
                        System.out.println("Game Over! Player has been defeated.");
                        LoseGame();
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
                            if (gate.ID.equals(pressurePlate.ID) && pressurePlate.activated) {
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
                if (iceTrap.active && iceTrap.canStun() && iceHitbox.intersects(player.getHitbox())) {
                    player.applyStun(120); // 2 detik stun = 120 frames (60 FPS)
                    iceTrap.setStunCooldown(); // Set cooldown 1 menit
                }
            }
            if (obstacle instanceof HealPotion) {
                HealPotion healPotion = (HealPotion) obstacle;
                Rectangle healHitbox = new Rectangle(healPotion.x, healPotion.y, tileSize, tileSize);
                if (healHitbox.intersects(player.getHitbox())) {
                    player.darah.heal(50);
                    if (player.darah.getCurrentHP() > 100) {
                        player.darah.update(100);
                    }
                    it.remove();
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

                Rectangle enemyHitbox = new Rectangle(
                        redHood.x + redHood.hitbox.x,
                        redHood.y + redHood.hitbox.y,
                        redHood.hitbox.width,
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