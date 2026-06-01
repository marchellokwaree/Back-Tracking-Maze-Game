package Obstacle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import Main.GamePanel;

public class Gate extends Obstacle {
    public boolean open = false;
    public boolean alrOpen = false;
    private BufferedImage spriteSheet;
    private BufferedImage[] animationFrames = new BufferedImage[6];
    private int currentFrame = 0;
    private int animationCounter = 0;
    private final int animationDelay = 20;
    public boolean active = false;
    private PressurePlate requiredPressurePlate = null; // Trapdoor requirement
    public String ID;

    public Gate(int x, int y, int width, int height, boolean right, String ID) {
        super(x, y, width, height);
        this.ID = ID;
        try {
            if (right) {
                this.spriteSheet = loadBufferedImage("/Assets/ASSET/Traps/Push_Trap_Front.png");
            } else {
                this.spriteSheet = loadBufferedImage("/Assets/ASSET/Traps/Push_Trap_Right.png");
            }
            int frameWidth = 32;
            int frameHeight = 32;
            int startX = 32 * 5;
            for (int i = 0; i < animationFrames.length; i++) {
                int colx = startX + (i * frameWidth);
                if (i == 0) {
                    animationFrames[i] = spriteSheet.getSubimage(colx, 0, frameWidth, frameHeight);

                } else {
                    animationFrames[i] = spriteSheet.getSubimage(colx, 0, frameWidth, frameHeight);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading pressure plate image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setRequiredPressurePlate(PressurePlate plate) {
        this.requiredPressurePlate = plate;
    }

    public boolean canOpen() {
        // Jika ada trapdoor yang diperlukan, cek apakah sudah diinjak
        if (requiredPressurePlate != null) {
            return requiredPressurePlate.activated;
        }
        // Jika tidak ada trapdoor yang diperlukan, gate bisa dibuka
        return true;
    }

    public void openGate() {
        if (alrOpen) {
            return; // Jika sudah terbuka, tidak perlu membuka lagi
        }
        // Hanya buka jika persyaratan trapdoor terpenuhi
        if (!canOpen()) {
            return; // Tidak bisa membuka jika trapdoor belum diinjak
        }
        open = true;
        this.currentFrame = 0;
        this.animationCounter = 0;
    }

    public void update() {
        animationCounter++;
        if (animationCounter >= animationDelay) {
            animationCounter = 0;
            currentFrame++;
            if (currentFrame >= animationFrames.length) {
                currentFrame = animationFrames.length - 1;
            }

        }
    }

    public BufferedImage getCurrentFrame() {
        if (animationFrames == null || animationFrames.length == 0)
            return null;
        if (open) {
            return animationFrames[currentFrame];
        } else {
            return animationFrames[0]; // Gambar default saat tidak aktif
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        BufferedImage frame = getCurrentFrame();
        super.drawCamera(g2, gp , frame);
    }
}
