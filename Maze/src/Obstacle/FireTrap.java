package Obstacle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import Main.GamePanel;

public class FireTrap extends Obstacle {
    private BufferedImage spriteSheet;
    private BufferedImage[] animationFrames = new BufferedImage[14];
    private int currentFrame = 0;
    private int animationCounter = 0;
    private final int animationDelay = 6;
    public boolean active = false;
    public FireTrap(int x, int y, int width, int height) {
        super(x, y, width, height);

        try {
            this.spriteSheet = loadBufferedImage("/Assets/ASSET/Traps/Fire_Trap.png");
            int frameWidth = 32;
            int frameHeight = 32;
            for (int i = 0; i < animationFrames.length; i++) {
                int colx = i * frameWidth;
                animationFrames[i] = spriteSheet.getSubimage(colx, 9, frameWidth, frameHeight);
            }
        } catch (Exception e) {
            System.out.println("Error loading fire trap image: " + e.getMessage());
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
            if (currentFrame >= 7 && currentFrame <= 9) {
                active = true;
            } else {
                active = false;
            }
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

    public void draw(Graphics2D g2, GamePanel gp) {
        BufferedImage frame = getCurrentFrame();
        super.drawCamera(g2, gp , frame);
    }



}
