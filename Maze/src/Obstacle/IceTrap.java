package Obstacle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import Main.GamePanel;
public class IceTrap extends Obstacle{
    private BufferedImage spriteSheet;
    private BufferedImage[] animationFrames = new BufferedImage[4];
    private int currentFrame = 0;
    private int animationCounter = 0;
    private final int animationDelay = 100;
    public boolean active = true;

    public IceTrap(int x, int y, int width, int height) {
        super(x, y, width, height);

        try {
            this.spriteSheet = loadBufferedImage("/Assets/ASSET/Traps/Ice_Trap_2.jpeg");
            int frameWidth = 120;
            int frameHeight = 120;
            int xstart = 67+270;
            int ystart = 605;
            for (int i = 0; i < animationFrames.length; i++) {
                int colx = xstart + (i * frameWidth);
                if (i == 1) {
                    colx = 67+135; // Tambahkan 1 pixel untuk frame kedua
                }
                if (i == 2) {
                    colx = 67+270; // Tambahkan 1 pixel untuk frame kedua
                }
                if (i == 3) {
                    colx = 67+405; // Tambahkan 1 pixel untuk frame kedua
                }
                animationFrames[i] = spriteSheet.getSubimage(colx, ystart, frameWidth, frameHeight);
            }
            System.out.println("berhasil memuat 4 item");
        } catch (Exception e) {
            System.out.println("Error loading ice trap image: " + e.getMessage());
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
