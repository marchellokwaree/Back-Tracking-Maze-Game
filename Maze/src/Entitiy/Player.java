package Entitiy;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import Main.Darah;
import javax.imageio.ImageIO;

import Main.GamePanel;
import Main.KeyHandler;

public class Player extends Entity {
    GamePanel gp;
    KeyHandler keyH;
    Image currentImage;
    BufferedImage bufferedImage;
    public Darah darah;
    // Variabel Animasi
    int spriteCounter = 0;
    int spriteNum = 1;
    boolean hadapKiri = false;
    public int HP = 100;
    public int maxHP = 100;
    public int damageCooldown = 0;
    public int normalSpeed = 2;
    public int slowSpeed = 1;
    public int slowEffectCounter = 0; // Counter untuk efek slow
    public int screenX;
    public int screenY;
    public final int defaultScreenX;
    public final int defaultScreenY;
    BufferedImage[] walkImages = new BufferedImage[8]; // Array untuk menyimpan gambar berjalan
    Rectangle hitbox;



    // Constructor disesuaikan dengan GamePanel kamu (5 parameter)
    public Player(GamePanel gp, KeyHandler keyH, Image playerImg, int x, int y) {
        super(x, y, 3); // speed 2
        this.gp = gp;
        this.keyH = keyH;
        this.currentImage = playerImg;
        try {
            this.bufferedImage = loadBufferedImage("/Assets/ASSET/ASSETKARAKTER/AnimationSheet.png");
            int spriteWidth = 24; // Lebar setiap sprite
            int spriteHeight = 24; // Tinggi setiap sprite
            int row2Y = (1 * spriteHeight) + 1; // Y untuk baris kedua
            if (this.bufferedImage != null) {
                for (int i = 0; i < 8; i++) {
                    int colx = i * spriteWidth; // X untuk setiap kolom
                    walkImages[i] = bufferedImage.getSubimage(colx, row2Y, spriteWidth, spriteHeight);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading animation sheet: " + e.getMessage());
            e.printStackTrace();
        }

        // Sesuaikan hitbox dengan ukuran tile 24x24.
        // Hitbox lebih kecil agar tidak menabrak dinding di bawah/sekitar sprite.
        hitbox = new Rectangle();
        hitbox.x = 8;       // offset dari kiri sprite
        hitbox.y = 4;       // offset dari atas sprite
        hitbox.width = gp.getTileSize() - 16;  // lebar hitbox
        hitbox.height = gp.getTileSize() - 4; // tinggi hitbox
        // Mengunci posisi default pemain tepat di tengah jendela game
        this.defaultScreenX = (gp.screenWidth / 2) - (gp.getTileSize() / 2);
        this.defaultScreenY = (gp.screenHeight / 2) - (gp.getTileSize() / 2);

        // Posisi layar saat ini
        this.screenX = defaultScreenX;
        this.screenY = defaultScreenY;

        this.darah = new Darah();
    }

    
    public Rectangle getHitbox() {
        return new Rectangle(x + hitbox.x, y + hitbox.y, hitbox.width, hitbox.height);
    }

    public void update() {
        updateEffects();
        int nextX = x;
        int nextY = y;
           // --- KAMERA CLAMPING (Mencegah kamera keluar map) ---
        screenX = defaultScreenX;
        screenY = defaultScreenY;

        // Batas Kiri
        if (x < defaultScreenX) {
            screenX = x;
        }
        // Batas Kanan
        else if (x > gp.worldWidth - (gp.screenWidth - defaultScreenX)) {
            screenX = gp.screenWidth - (gp.worldWidth - x);
        }

            // Batas Atas
        if (y < defaultScreenY) {
            screenY = y;
        }
        // Batas Bawah
        else if (y > gp.worldHeight - (gp.screenHeight - defaultScreenY)) {
            screenY = gp.screenHeight - (gp.worldHeight - y);
        }
        boolean moving = false;

        // Cek Input dan tentukan gambar (Bisa dikembangkan per arah jika ada asetnya)
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            moving = true;
            if (keyH.upPressed)
                nextY -= speed;
            if (keyH.downPressed)
                nextY += speed;
            if (keyH.leftPressed) {

                nextX -= speed;
                hadapKiri = true;
            }
            if (keyH.rightPressed) {
                nextX += speed;
                hadapKiri = false;
            }
                

                
        }

        // LOGIKA ANIMASI: Berganti antara spriteNum 1 dan 2 saat bergerak
        if (moving) {
            spriteCounter++;
            if (spriteCounter > 12) { // Kecepatan ganti kaki
                if (spriteNum == 1) {
                    spriteNum = 2;
                } else if (spriteNum == 2) {
                    spriteNum = 3;
                } else if (spriteNum == 3) {
                    spriteNum = 4;
                } else if (spriteNum == 4) {
                    spriteNum = 5;
                } else if (spriteNum == 5) {
                    spriteNum = 6;
                } else if (spriteNum == 6) {
                    spriteNum = 7;
                } else if (spriteNum == 7) {
                    spriteNum = 8;
                } else {
                    spriteNum = 1; // Kembali ke posisi diam setelah langkah ke-8
                }
                spriteCounter = 0;
            }

            if (spriteNum >= 1 && spriteNum <= 8) {
                currentImage = walkImages[spriteNum - 1]; // Ganti gambar sesuai dengan spriteNum
            }
        } else {
            spriteNum = 1; // Kembali ke posisi diam jika berhenti
        }

        // Collision Check
        if (!gp.collidesWithWall(nextX, y , this.hitbox) && !gp.collidesWithClosedGate(nextX, y , this.hitbox)) {
            x = nextX;
        }
        if (!gp.collidesWithWall(x, nextY , this.hitbox) && !gp.collidesWithClosedGate(x, nextY , this.hitbox)) {
            y = nextY;
        }
    }

    public void updateEffects() {
        if (slowEffectCounter > 0) {
            slowEffectCounter--;
            if (slowEffectCounter == 0) {
                speed = normalSpeed;
            }
        }
        if (damageCooldown > 0) {
            damageCooldown--;
        }
    }

    public void applySlow(int durationFrames) {
        slowEffectCounter = durationFrames;
        speed = slowSpeed;
    }

    

    

    public void draw(Graphics2D g2) {
        int camX = gp.getCameraXInt();
        int camY = gp.getCameraYInt();
        int drawY = y - camY;
        Image walkingImage = null;

        // EFEK VISUAL JALAN:
        // Jika sedang melangkah (spriteNum 2), gambar naik sedikit agar terlihat
        // seperti melangkah
        // Jika kamu sudah punya 2 gambar berbeda, kamu bisa mengganti gambarnya di
        // sini.
        if (spriteNum == 2) {
            drawY -= 4;
            // Ganti ke gambar berjalan
            walkingImage = walkImages[1]; // Contoh: potongan gambar untuk langkah kedua
        } else {
            walkingImage = currentImage; // Gambar diam
        }



        java.awt.geom.AffineTransform originalTransform = g2.getTransform();

        int width = gp.getTileSize();
        int height = gp.getTileSize();
        int drawX = x - camX;

        // gambar menghadap ke arah kiri
        if (hadapKiri) {
            g2.translate(drawX + width, drawY); // Sesuaikan posisi setelah flip
            g2.scale(-1, 1); // Flip horizontal
            if (walkingImage != null) {
                g2.drawImage(walkingImage, 0, 0, width, height, null);
            }

        } if (!hadapKiri) {
            if (walkingImage != null) {
                g2.drawImage(walkingImage, drawX, drawY, gp.getTileSize(), gp.getTileSize(), null);
            }

        }
        g2.setTransform(originalTransform);
    }
}