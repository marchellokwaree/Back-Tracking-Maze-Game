package Main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;


/**
 * Custom JButton dengan rounded corners untuk tampilan yang lebih modern.
 */
class RoundedButton extends JButton {
	private static final long serialVersionUID = 1L;
	private int arcWidth = 20;
	private int arcHeight = 20;
	private Color hoverColor;
	private Color defaultColor;
	private boolean isHovered = false;

	public RoundedButton(String text, Color bgColor, Color borderColor) {
		super(text);
		this.defaultColor = bgColor;
		this.hoverColor = new Color(
				Math.min(bgColor.getRed() + 30, 255),
				Math.min(bgColor.getGreen() + 30, 255),
				Math.min(bgColor.getBlue() + 30, 255));

		setContentAreaFilled(false);
		setOpaque(false);
		setFocusPainted(false);
		setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		setBackground(bgColor);
		setForeground(Color.WHITE);

		addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				isHovered = true;
				repaint();
			}

			public void mouseExited(java.awt.event.MouseEvent e) {
				isHovered = false;
				repaint();
			}
		});

	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color color = isHovered ? hoverColor : defaultColor;
		g2.setColor(color);
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), arcWidth, arcHeight);

		super.paintComponent(g);
	}

	@Override
	protected void paintBorder(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(getForeground());
		g2.setStroke(new java.awt.BasicStroke(2));
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcHeight);
	}
}

/**
 * HUDPanel adalah panel menu utama sebelum masuk ke GamePanel.
 * Panel ini memungkinkan pemain memilih difficulty, memulai permainan, atau
 * keluar.
 */
public class HUDPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	// Referensi ke frame utama agar HUDPanel bisa diganti dengan GamePanel ketika
	// tombol Start ditekan.
	private final JFrame parentFrame;

	// Combo box untuk memilih tingkat kesulitan.
	private JComboBox<String> difficultyCombo;

	// Background image
	private BufferedImage backgroundImage;

	private Font customFont; // custom font untuk tombol dan teks lainnya

	/**
	 * Konstruktor HUDPanel.
	 * 
	 * @param frame JFrame induk tempat panel ini akan ditambahkan.
	 */
	public HUDPanel(JFrame frame) {
		this.parentFrame = frame;
		setPreferredSize(new Dimension(900, 700));
		setOpaque(false);
		setLayout(new BorderLayout(0, 0));
		
		try {
			// Ganti path ini sesuai dengan lokasi file .ttf Anda!
			InputStream is = getClass().getResourceAsStream("/Assets/Pixuf.ttf"); 
			
			// Buat font dari file, lalu atur ukurannya (misal: ukuran 16)
			customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f); 
			setFont(customFont);
		} catch (Exception e) {
			System.out.println("Gagal load font untuk button, kembali ke Arial biasa.");
			e.printStackTrace();
			setFont(new Font("Arial", Font.BOLD, 16)); // Font cadangan jika error
		}

		// Load background image
		loadBackgroundImage();

		// Membuat judul menu.
		JLabel titleLabel = createTitleLabel();
		add(titleLabel, BorderLayout.NORTH);

		// Membuat area tengah berisi pilihan difficulty dan tombol.
		JPanel centerPanel = createCenterPanel();
		add(centerPanel, BorderLayout.CENTER);

		// Menambahkan teks footer sebagai instruksi tambahan.
		JLabel footerLabel = createFooterLabel();
		add(footerLabel, BorderLayout.SOUTH);

		
	}

	/**
	 * Load background image dari folder assets
	 */
	private void loadBackgroundImage() {
		try {
			// Try loading from classpath first
			URL url = getClass().getResource("/Assets/TampilanAwal.png");
			if (url != null) {
				backgroundImage = ImageIO.read(url);
			} else {
				// Try loading from file system
				File file = new File("src/Assets/TampilanAwal.png");
				if (file.exists()) {
					backgroundImage = ImageIO.read(file);
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load menu background: " + e.getMessage());
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Draw background image if available
		if (backgroundImage != null) {
			g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);

			// Add semi-transparent overlay for better text visibility
			g2.setColor(new Color(0, 0, 0, 100));
			g2.fillRect(0, 0, getWidth(), getHeight());
		} else {
			// Fallback gradient if no image
			g2.setColor(new Color(10, 18, 30));
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	/**
	 * Membuat label judul untuk HUD panel.
	 */
	private JLabel createTitleLabel() {
		JLabel titleLabel = new JLabel("");
		titleLabel.setFont(customFont.deriveFont(48f)); // Ukuran font lebih besar untuk judul
		titleLabel.setForeground(new Color(255, 100, 100));
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 30, 0));
		return titleLabel;
	}

	/**
	 * Membuat panel tengah berisi pilihan difficulty dan tombol Start/Exit.
	 */
	private JPanel createCenterPanel() {
		JPanel centerPanel = new JPanel();
		centerPanel.setOpaque(false);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

		JLabel instructionLabel = new JLabel("Pilih Level / Difficulty");
		instructionLabel.setFont(customFont.deriveFont(20f));
		instructionLabel.setForeground(new Color(173, 216, 230));
		instructionLabel.setAlignmentX(CENTER_ALIGNMENT);
		instructionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

		difficultyCombo = new JComboBox<>(new String[] { "Easy", "Medium", "Hard", "Extreme" });
		difficultyCombo.setFont(customFont.deriveFont(16f));
		difficultyCombo.setMaximumSize(new Dimension(300, 45));
		difficultyCombo.setAlignmentX(CENTER_ALIGNMENT);
		difficultyCombo.setBackground(new Color(30, 40, 60));
		difficultyCombo.setForeground(new Color(173, 216, 230));

		RoundedButton startButton = new RoundedButton("PLAY", new Color(100, 200, 100), new Color(150, 255, 150));
		startButton.setFont(customFont.deriveFont(22f));
		startButton.setAlignmentX(CENTER_ALIGNMENT);
		startButton.setMaximumSize(new Dimension(300, 50));
		startButton.setForeground(Color.WHITE);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onStartPressed();
			}
		});

		RoundedButton exitButton = new RoundedButton("EXIT", new Color(220, 80, 80), new Color(255, 150, 150));
		exitButton.setFont(customFont.deriveFont(22f));
		exitButton.setAlignmentX(CENTER_ALIGNMENT);
		exitButton.setMaximumSize(new Dimension(300, 50));
		exitButton.setForeground(Color.WHITE);
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onExitPressed();
			}
		});

		centerPanel.add(Box.createVerticalStrut(80)); // tengahin untuk play exit
		centerPanel.add(instructionLabel);
		centerPanel.add(difficultyCombo);
		centerPanel.add(Box.createVerticalStrut(40)); // tengahin play exit
		centerPanel.add(startButton);
		centerPanel.add(Box.createVerticalStrut(20));
		centerPanel.add(exitButton);
		centerPanel.add(Box.createVerticalStrut(80));

		return centerPanel;
	}

	/**
	 * Membuat label footer untuk HUD panel.
	 */
	private JLabel createFooterLabel() {
		JLabel footerLabel = new JLabel("Gunakan WASD untuk bergerak - Hindari jebakan - Selamatkan semua Red Hood!");
		footerLabel.setFont(customFont.deriveFont(13f));
		footerLabel.setForeground(new Color(180, 180, 200));
		footerLabel.setHorizontalAlignment(JLabel.CENTER);
		footerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
		return footerLabel;
	}

	/**
	 * Menangani aksi ketika tombol Start ditekan.
	 * Panel kemudian diganti menjadi GamePanel dan game dimulai.
	 */
	private void onStartPressed() {

		// Ganti panel HUD dengan GamePanel.
		GamePanel gamePanel = new GamePanel();
		parentFrame.setContentPane(gamePanel);
		parentFrame.revalidate();
		parentFrame.pack();
		parentFrame.setLocationRelativeTo(null);
		gamePanel.requestFocusInWindow();
		gamePanel.startGameThread();
	}

	/**
	 * Menangani aksi ketika tombol Exit ditekan.
	 */
	private void onExitPressed() {
		System.exit(0);
	}

}


