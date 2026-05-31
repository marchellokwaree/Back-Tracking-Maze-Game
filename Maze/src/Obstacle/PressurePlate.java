
package Obstacle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import Main.GamePanel;
public class PressurePlate extends Obstacle {
    public boolean activated = false;
    private BufferedImage spriteSheet;
    private BufferedImage[] animationFrames = new BufferedImage[2];
    private int currentFrame = 0;
    private int animationCounter = 0;
    public String ID;
    public PressurePlate(int x, int y, int width, int height, String ID) {
        super(x, y, width, height);
        this.ID = ID;
        try {
            this.spriteSheet = loadBufferedImage("/Assets/ASSET/free-pixel-art-dungeon-objects-asset-pack/PNG/Pedestals.png");
            int frameWidth = 48;
            int frameHeight = 48;
            for (int i = 0; i < animationFrames.length; i++) {
                int colx = 0 * frameWidth;
                if (i == 0) {
                    animationFrames[i] = spriteSheet.getSubimage(colx, 2, frameWidth, frameHeight);

                } else {
                    animationFrames[i] = spriteSheet.getSubimage(colx, 0, frameWidth, frameHeight);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading pressure plate image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    

    public void activate() {
        activated = true;
    }

    public void deactivate() {
        activated = false;
    }

    public void update() {
        animationCounter++;
        
            currentFrame++;
            active = true;
        
    }

    public BufferedImage getCurrentFrame() {
        if (activated) {
            return animationFrames[1];
        } else {
            return animationFrames[0]; // Gambar default saat tidak aktif
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        BufferedImage frame = getCurrentFrame();
        super.drawCamera(g2, gp , frame);
    }

}
