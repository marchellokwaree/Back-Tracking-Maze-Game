package Obstacle;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import Main.GamePanel;

public class Finish extends Obstacle {
    private BufferedImage spriteSheet;
    private BufferedImage[] animationFrames = new BufferedImage[8];
    private int currentFrame = 0;
    private int animationCounter = 0;
    private final int animationDelay = 6;
    public Finish(int x, int y, int width, int height) {
        super(x, y, width, height);

        try {
            this.spriteSheet = loadBufferedImage("/Assets/ASSET/Traps/Portal-Blue-Sheet.png");
            int frameWidth = 64;
            int frameHeight = 64;
            for (int i = 0; i < animationFrames.length; i++) {
                int colx = i * frameWidth;
                animationFrames[i] = spriteSheet.getSubimage(colx, 0, frameWidth, frameHeight);
            }
        } catch (Exception e) {
            System.out.println("Error loading finish image: " + e.getMessage());
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

    public void draw(Graphics2D g2, GamePanel gp) {
        BufferedImage frame = getCurrentFrame();
        super.drawCamera(g2, gp , frame);
    }

    
}
