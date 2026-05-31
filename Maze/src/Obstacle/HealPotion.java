
package Obstacle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.IOException;
import Main.GamePanel;
public class HealPotion extends Obstacle {
    private BufferedImage spriteSheet;
    private BufferedImage[] animationFrames = new BufferedImage[8];
    private int currentFrame = 0;
    private int animationCounter = 0;
    private final int animationDelay = 6;
    public HealPotion(int x, int y, int width, int height) {
        super(x, y, width, height);
        try {
            this.spriteSheet = loadBufferedImage("/Assets/ASSET/Traps/heal_potion.png");
            int frameWidth = 16;
            int frameHeight = 16;
            if (this.spriteSheet != null) {
                int index = 0;
                for (int row = 0; row < 3 && index < animationFrames.length; row++) {
                    for (int col = 0; col < 3 && index < animationFrames.length; col++) {
                        animationFrames[index++] = spriteSheet.getSubimage(col * frameWidth, row * frameHeight, 16, 16);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading heal potion image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void update() {
        animationCounter++;
        if (animationCounter >= animationDelay) {
            animationCounter = 0;
            currentFrame++;
            if (currentFrame >= animationFrames.length) {
                currentFrame = 0;
            }
            active = true;
        }
    }

    public BufferedImage getCurrentFrame() {
        if (animationFrames == null || animationFrames.length == 0) {
            return spriteSheet;
        }
        if (currentFrame < 0 || currentFrame >= animationFrames.length) {
            currentFrame = currentFrame % animationFrames.length;
            if (currentFrame < 0) {
                currentFrame += animationFrames.length;
            }
        }
        if (animationFrames[currentFrame] != null) {
            return animationFrames[currentFrame];
        }
        return spriteSheet;
    }

    @Override
    public void drawCamera(Graphics2D g2, GamePanel gp, BufferedImage img) {
        if (img != null && gp.getPlayer() != null) {
            int camX = gp.getCameraXInt();
            int camY = gp.getCameraYInt();
            int screenX = x - camX + 6;
            int screenY = y - camY + 6;

            // Optimasi: Hanya gambar jika objek masuk dalam area layar
            if (x + width > camX &&
                x - width < camX + gp.screenWidth &&
                y + height > camY &&
                y - height < camY + gp.screenHeight) {

                g2.drawImage(img, screenX, screenY, 20, 20, null);
            }
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        BufferedImage frame = getCurrentFrame();
        drawCamera(g2, gp , frame);
    }

     
}
