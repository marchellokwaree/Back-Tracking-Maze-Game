package Main;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import Main.GamePanel;
import java.io.InputStream;

public class Timer {
    private long maxTime;
    private long endTime;
    private int currentTime;
    private Font customFont;
    private boolean isRunning = false;
    private boolean timeUp = false;


    public Timer(long maxTime) {
        this.maxTime = maxTime; // jika maxtime 100000, maka timer akan aktif selama 100 detik
        this.currentTime = (int) (maxTime / 1000); // Convert milliseconds to seconds
        try {
            InputStream is = getClass().getResourceAsStream("/Assets/Pixuf.ttf"); 
            customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(28f); 
        } catch (Exception e) {
            System.out.println("Gagal load font untuk timer, kembali ke Arial.");
            e.printStackTrace();
            customFont = new Font("Arial", Font.BOLD, 28); // Font cadangan jika error
        }


    }

    public void start() {
        this.endTime = System.currentTimeMillis() + maxTime;
        this.isRunning = true;
    }

    public void update() {
        if (!isRunning) return;

        long remainingTime = endTime - System.currentTimeMillis();
        if (remainingTime < 0) {
            currentTime = 0;
            isRunning = false;
            timeUp = true;
        } else {
            currentTime = (int) (remainingTime / 1000); // Convert milliseconds to seconds
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        String timeText = "Time: " + currentTime + "s";
        g2.setFont(customFont);
        
        // --- RUMUS POSISI bawah kanan---
        // Menghitung panjang teks dalam pixel agar bisa membaginya ke tengah layar
        FontMetrics metrics = g2.getFontMetrics(customFont);
        int textWidth = metrics.stringWidth(timeText);
        int x = (gp.screenWidth / 2) + (gp.screenWidth / 2 - textWidth) - (textWidth / 2) + 50;
        int y = gp.screenHeight - 25; // Posisi Y dengan offset dari bawah layar

        // (Opsional) Efek bayangan teks agar selalu terlihat meski backgroundnya putih
        g2.setColor(new java.awt.Color(0, 0, 0, 150));
        g2.drawString(timeText, x + 2, y + 2);

        // Teks utama berwarna merah
        g2.setColor(new java.awt.Color(255, 225, 225));
        g2.drawString(timeText, x, y); 
    }

    public boolean isTimeUp() {
        return timeUp;
    }
}
        
