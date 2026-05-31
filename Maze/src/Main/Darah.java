package Main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.InputStream;
import java.util.Random;
public class Darah {
    private int currentHP;
    private final int maxHP;
    private Font customFont;

    public Darah() {
        this.maxHP = 100;
        this.currentHP = maxHP;

        try {
            // Ganti path ini sesuai dengan lokasi file .ttf Anda!
            InputStream is = getClass().getResourceAsStream("/Assets/Pixuf.ttf"); 
            
            // Buat font dari file, lalu atur ukurannya (misal: ukuran 14)
            customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(14f); 
        } catch (Exception e) {
            System.out.println("Gagal load font, kembali ke Arial biasa.");
            e.printStackTrace();
            customFont = new Font("Arial", Font.BOLD, 14); // Font cadangan jika error
        }
    }

    public void update(int hp) {
        if (hp < 0) {
            hp = 0;
        }
        if (hp > maxHP) {
            hp = maxHP;
        }
        this.currentHP = hp;
    }

    

    public void draw(Graphics2D g2) {
        int x = 14 ; // Posisi X dengan offset getaran
        int y = 10 ; // Posisi Y dengan offset getaran
        int width = 212;
        int height = 28;    
        int radius = 12;

        // Background panel with transparansi agar tidak terlalu menutupi
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(x, y, width, height, radius, radius);

        // Border panel
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, width, height, radius, radius);

        int innerX = x + 4;
        int innerY = y + 4;
        int innerWidth = width - 8;
        int innerHeight = height - 8;

        // Health bar background
        g2.setColor(new Color(60, 60, 60, 200));
        g2.fillRoundRect(innerX, innerY, innerWidth, innerHeight, radius, radius);

        // Health fill
        int barWidth = (int) ((currentHP / (double) maxHP) * innerWidth);
        Color hpColor = currentHP > 70 ? new Color(0, 191, 0) : currentHP > 50 ? new Color(235, 150, 40) : currentHP > 20 ? new Color(200, 60, 60) : new Color(150, 0, 0);
        g2.setColor(hpColor);
        g2.fillRoundRect(innerX, innerY, Math.max(barWidth, 2), innerHeight, radius, radius);

        // Inner border
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawRoundRect(innerX, innerY, innerWidth, innerHeight, radius, radius);

        // Text dan ikon
        g2.setFont(customFont);
        g2.setColor(Color.WHITE);
        g2.drawString("HP", innerX + 8, innerY + innerHeight - 6);
        g2.drawString(currentHP + " / " + maxHP, innerX + innerWidth - 60, innerY + innerHeight - 6);
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public void heal(int amount) {
        update(currentHP + amount);
    }

    public void takeDamage(int amount) {
        update(currentHP - amount);
    }
}
