package Entitiy;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import Main.GamePanel;

public class RedHood extends Entity {
    private BufferedImage bufferedImage;
    private BufferedImage[] animationFrames = new BufferedImage[4];
    private BufferedImage[] disapearAnimation = new BufferedImage[4];
    private int currentFrame = 0;
    private int animationCounter = 0;
    private int disappearFrame = 0;
    private int disappearCounter = 0;
    private final int animationDelay = 20;
    private final int disappearDelay = 10;
    private int width = 32;
    private int height = 32;
    GamePanel gp;
    public boolean active = false;
    public boolean removed = false;
    public Rectangle hitbox;

    public RedHood(int x, int y, int speed, GamePanel gp) {
        super(x, y, speed);
        this.gp = gp;
        try {
            this.bufferedImage = loadBufferedImage("/Assets/ASSET/ASSETKARAKTER/AnimationSheet_Character.png");
            int spriteWidth = 32; // Lebar setiap sprite
            int spriteHeight = 32; // Tinggi setiap sprite
            int row2Y = (0 * spriteHeight) + 1; // Y untuk baris kedua
            if (this.bufferedImage != null) {
                for (int i = 0; i < 4; i++) {
                    int colx = i * spriteWidth; // X untuk setiap kolom
                    if (i > 1) {
                        row2Y = (1 * spriteHeight) + 1; // Y untuk baris kedua
                        colx = (i - 2) * spriteWidth; // X untuk setiap kolom
                    }

                    animationFrames[i] = bufferedImage.getSubimage(colx, row2Y, spriteWidth, spriteHeight);
                }
                for (int i = 0; i < 4; i++) {
                    int colx = i * spriteWidth; // X untuk setiap kolom
                    int rowY = (6 * spriteHeight) + 1; // Y untuk baris ketiga
                    disapearAnimation[i] = bufferedImage.getSubimage(colx, rowY, spriteWidth, spriteHeight);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading animation sheet: " + e.getMessage());
            e.printStackTrace();
        }

        hitbox = new Rectangle();
        hitbox.x = 4; // offset dari kiri sprite
        hitbox.y = 4; // offset dari atas sprite
        hitbox.width = gp.getTileSize() -16; // lebar hitbox
        hitbox.height = gp.getTileSize() - 8; // tinggi hitbox
    }

    public void startDisappear() {
        active = true;
        disappearFrame = 0;
        disappearCounter = 0;
    }

    public boolean shouldRemove() {
        return removed;
    }

    public void update() {
        if (active) {
            disappearCounter++;
            if (disappearCounter >= disappearDelay) {
                disappearCounter = 0;
                disappearFrame++;
                if (disappearFrame >= disapearAnimation.length) {
                    removed = true;
                }
            }
            return;
        }

        animationCounter++;
        if (animationCounter >= animationDelay) {
            animationCounter = 0;
            currentFrame++;
            if (currentFrame >= animationFrames.length) {
                currentFrame = 0;
            }
        }
    }

    public BufferedImage getCurrentFrame() {
        if (animationFrames == null || animationFrames.length == 0) {
            return bufferedImage;
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
        return bufferedImage;
    }

    public BufferedImage getDisappearFrame() {
        if (disapearAnimation == null || disapearAnimation.length == 0) {
            return bufferedImage;
        }
        if (disappearFrame < 0 || disappearFrame >= disapearAnimation.length) {
            return bufferedImage;
        }
        return disapearAnimation[disappearFrame];
    }

    public void drawCamera(Graphics2D g2, GamePanel gp, BufferedImage img) {
        if (img != null && gp.getPlayer() != null) {
            // Rumus posisi layar = Posisi dunia - Posisi Player + Titik tengah layar
            int camX = gp.getCameraXInt();
            int camY = gp.getCameraYInt();
            int screenX = x - camX;
            int screenY = y - camY;

            // Optimasi: Hanya gambar jika objek masuk dalam area layar
            if (x + width > camX &&
                x - width < camX + gp.screenWidth &&
                y + height > camY &&
                y - height < camY + gp.screenHeight) {

                g2.drawImage(img, screenX, screenY, 32, 32, null);
            }
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        if (removed) {
            return;
        }

        BufferedImage frame = active ? getDisappearFrame() : getCurrentFrame();
        drawCamera(g2, gp, frame);
    }
}
