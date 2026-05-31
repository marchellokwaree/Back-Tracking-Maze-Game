package Obstacle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import Main.GamePanel;

public class Obstacle {
    public int x, y, width, height;
    boolean active = false;
    
    public Obstacle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean collidesWith(int playerX, int playerY, int playerSize) {
        return playerX < x + width &&
               playerX + playerSize > x &&
               playerY < y + height &&
               playerY + playerSize > y;
    }

    protected BufferedImage loadBufferedImage(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                return ImageIO.read(url);
            }

            File file = resolveImageFile(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }

            throw new IOException("Resource not found: " + path);
        } catch (IOException e) {
            System.out.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private File resolveImageFile(String path) {
        String normalizedPath = path.replace('/', File.separatorChar);
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

    public void drawCamera(Graphics2D g2, GamePanel gp, BufferedImage img) {
        if (img != null && gp.getPlayer() != null) {
            int camX = gp.getCameraXInt();
            int camY = gp.getCameraYInt();
            int screenX = x - camX;
            int screenY = y - camY;

            // Optimasi: Hanya gambar jika objek masuk dalam area layar
            if (x + width > camX &&
                x - width < camX + gp.screenWidth &&
                y + height > camY &&
                y - height < camY + gp.screenHeight) {

                g2.drawImage(img, screenX, screenY, width, height, null);
            }
        }
    }


}
