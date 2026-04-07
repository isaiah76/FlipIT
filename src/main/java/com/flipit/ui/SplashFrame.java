package com.flipit.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SplashFrame extends JWindow {
    private final JProgressBar progressBar;
    private final JPanel content;

    public SplashFrame() {
        setSize(450, 280);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setAlwaysOnTop(true);

        content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#3b82f6"));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(Color.decode("#2563eb"));
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 14, 14));
                g2.dispose();
            }
        };
        content.setOpaque(false);

        JLabel title = new JLabel("FlipIT", SwingConstants.CENTER);
        Font logoFont = UIManager.getFont("logoFont");
        if (logoFont != null) {
            title.setFont(logoFont.deriveFont(56f));
        } else {
            title.setFont(new Font("SansSerif", Font.BOLD, 56));
        }

        title.setForeground(Color.WHITE);
        content.add(title, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(100, 6));
        progressBar.setBorderPainted(false);
        progressBar.setForeground(Color.WHITE);
        progressBar.setBackground(Color.decode("#1e40af"));
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        content.add(bottomPanel, BorderLayout.SOUTH);
        add(content);
        setBackground(new Color(0, 0, 0, 0));
    }

    public void setProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
        });
    }
}