package Main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;

public class EndGamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JFrame parentFrame;
    private BufferedImage backgroundImage;
    private Font customFont;

    public EndGamePanel(JFrame frame, boolean success, int timeSpent) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(900, 700));
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));

        try {
            InputStream is = getClass().getResourceAsStream("/Assets/Pixuf.ttf");
            customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(18f);
            setFont(customFont);
        } catch (Exception e) {
            setFont(new Font("Arial", Font.BOLD, 18));
        }

        loadBackgroundImage();

        JLabel title = new JLabel(success ? "LEVEL CLEARED" : "GAME OVER");
        title.setFont(getFont().deriveFont(42f));
        title.setForeground(success ? new Color(120, 220, 140) : new Color(220, 90, 90));
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel message = new JLabel(success ? "Backtracking succeeded!\n" : "Backtracking failed.");
        message.setFont(getFont().deriveFont(20f));
        message.setForeground(new Color(200, 200, 220));
        message.setAlignmentX(CENTER_ALIGNMENT);
        message.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel timeLabel = new JLabel("Time: " + timeSpent + " seconds");
        timeLabel.setFont(getFont().deriveFont(16f));
        timeLabel.setForeground(new Color(180, 180, 200));
        timeLabel.setAlignmentX(CENTER_ALIGNMENT);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 30, 0));

        center.add(Box.createVerticalStrut(60));
        center.add(message);
        center.add(timeLabel);
        center.add(Box.createVerticalStrut(30));

        RoundedButton menuBtn = new RoundedButton("MAIN MENU", new Color(80, 140, 220), new Color(120, 180, 250));
        menuBtn.setFont(getFont().deriveFont(18f));
        menuBtn.setAlignmentX(CENTER_ALIGNMENT);
        menuBtn.addActionListener(e -> onMainMenu());

        RoundedButton exitBtn = new RoundedButton("EXIT", new Color(220, 80, 80), new Color(255, 120, 120));
        exitBtn.setFont(getFont().deriveFont(18f));
        exitBtn.setAlignmentX(CENTER_ALIGNMENT);
        exitBtn.addActionListener(e -> System.exit(0));

        center.add(menuBtn);
        center.add(Box.createVerticalStrut(12));
        center.add(exitBtn);
        center.add(Box.createVerticalStrut(80));

        add(center, BorderLayout.CENTER);
    }

    private void onMainMenu() {
        parentFrame.setContentPane(new HUDPanel(parentFrame));
        parentFrame.revalidate();
        parentFrame.pack();
        parentFrame.setLocationRelativeTo(null);
    }

    private void loadBackgroundImage() {
        try {
            URL url = getClass().getResource("/Assets/TampilanAwal.png");
            if (url != null) {
                backgroundImage = ImageIO.read(url);
            } else {
                File file = new File("src/Assets/TampilanAwal.png");
                if (file.exists())
                    backgroundImage = ImageIO.read(file);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g2.setColor(new Color(12, 18, 30));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
