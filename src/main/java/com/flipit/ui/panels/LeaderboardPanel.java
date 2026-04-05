package com.flipit.ui.panels;

import com.flipit.dao.CardProgressDAO;
import com.flipit.models.Deck;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

public class LeaderboardPanel extends JPanel {
    private JPanel root;
    private JButton backBtn;
    private JLabel headerTitleLbl;
    private JLabel deckBadgeLbl;
    private JScrollPane scrollPane;
    private JPanel listAlignmentWrapper;
    private JPanel boardContainer;

    private final MainPanel mainPanel;
    private final Deck deck;
    private final String previousScreen;

    private int s(int val) {
        return ImageUtil.scale((int) Math.round(val * 1.15f));
    }

    private float f(float val) {
        return val * 1.15f * ImageUtil.getScaleFactor();
    }

    private Font getBaseFont() {
        Font f = UIManager.getFont("defaultFont");
        return f != null ? f : new Font("SansSerif", Font.PLAIN, 12);
    }

    public LeaderboardPanel(MainPanel mainPanel, Deck deck, String previousScreen) {
        this.mainPanel = mainPanel;
        this.deck = deck;
        this.previousScreen = previousScreen;

        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        deckBadgeLbl.setText(deck.getTitle().toUpperCase());

        styleAll();
        bindEvents();
        loadLeaderboard();
    }

    private void styleAll() {
        Color bgTheme = Color.decode("#f8fafc");
        root.setBackground(bgTheme);
        root.setBorder(BorderFactory.createEmptyBorder(s(20), s(38), s(20), s(38)));

        Font baseFont = getBaseFont();

        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        backBtn.setForeground(Color.decode("#2563eb"));
        backBtn.setIconTextGap(s(8));
        backBtn.setIcon(IconUtil.getIcon("BACK", Color.decode("#2563eb"), s(14)));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        headerTitleLbl.setFont(baseFont.deriveFont(Font.BOLD, f(13f)));
        headerTitleLbl.setForeground(Color.decode("#94a3b8"));
        headerTitleLbl.setText("LEADERBOARD");

        deckBadgeLbl.setFont(baseFont.deriveFont(Font.BOLD, f(11f)));
        deckBadgeLbl.setForeground(Color.decode("#2563eb"));
        deckBadgeLbl.setOpaque(true);
        deckBadgeLbl.setBackground(Color.decode("#eff6ff"));
        deckBadgeLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#dbeafe"), Math.max(1, s(1))),
                BorderFactory.createEmptyBorder(s(6), s(14), s(6), s(14))
        ));

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(bgTheme);
        scrollPane.getVerticalScrollBar().setUnitIncrement(s(16));

        listAlignmentWrapper.setBackground(bgTheme);

        boardContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = s(20);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));

                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1, s(1))));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, arc, arc));

                g2.dispose();
            }
        };
        boardContainer.setOpaque(false);
        boardContainer.setLayout(new BoxLayout(boardContainer, BoxLayout.Y_AXIS));
    }

    private void bindEvents() {
        backBtn.addActionListener(e -> mainPanel.showScreen(previousScreen));
    }

    private void loadLeaderboard() {
        SkeletonPanel skeletonPanel = new SkeletonPanel();

        listAlignmentWrapper.removeAll();
        listAlignmentWrapper.add(skeletonPanel, BorderLayout.NORTH);
        listAlignmentWrapper.revalidate();
        listAlignmentWrapper.repaint();

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                CardProgressDAO dao = new CardProgressDAO();
                List<Object[]> rawEntries = dao.getLeaderboard(deck.getId());

                for (Object[] entry : rawEntries) {
                    byte[] picBytes = (byte[]) entry[3];
                    entry[3] = ImageUtil.getImageFromBytes(picBytes);
                }

                return rawEntries;
            }

            @Override
            protected void done() {
                skeletonPanel.stop();

                try {
                    List<Object[]> entries = get();

                    boardContainer.removeAll();
                    boardContainer.add(Box.createVerticalStrut(s(5)));

                    if (entries.isEmpty()) {
                        JLabel emptyLbl = new JLabel("No scores recorded yet.");
                        emptyLbl.setFont(getBaseFont().deriveFont(Font.PLAIN, f(14f)));
                        emptyLbl.setForeground(Color.decode("#64748b"));
                        emptyLbl.setBorder(BorderFactory.createEmptyBorder(s(30), s(30), s(30), s(30)));
                        boardContainer.add(emptyLbl);
                    } else {
                        String currentUsername = mainPanel.getUser().getUsername();

                        for (int i = 0; i < entries.size(); i++) {
                            Object[] entry = entries.get(i);
                            int rank = (int) entry[0];
                            String username = (String) entry[2];
                            Image picImage = (Image) entry[3];
                            int best = (int) entry[4];
                            int total = (int) entry[5];
                            Timestamp date = (Timestamp) entry[6];

                            boolean isCurrentUser = username.equalsIgnoreCase(currentUsername);
                            boolean isLast = (i == entries.size() - 1);

                            JPanel row = createRow(rank, username, picImage, best, total, isCurrentUser, isLast, date);
                            boardContainer.add(row);
                        }
                    }
                    boardContainer.add(Box.createVerticalStrut(s(5)));

                    listAlignmentWrapper.removeAll();
                    listAlignmentWrapper.add(boardContainer, BorderLayout.NORTH);
                    listAlignmentWrapper.revalidate();
                    listAlignmentWrapper.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    Window parentWindow = SwingUtilities.getWindowAncestor(LeaderboardPanel.this);
                    new InfoDialog(parentWindow, "Error", "Failed to load leaderboard.").setVisible(true);
                }
            }
        }.execute();
    }

    private JPanel createRow(int rank, String username, Image profilePic, int score, int total, boolean isCurrentUser, boolean isLast, Timestamp date) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(true);
        row.setBackground(isCurrentUser ? Color.decode("#f8fafc") : Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(65)));
        row.setPreferredSize(new Dimension(0, s(65)));

        if (!isLast) {
            row.setBorder(BorderFactory.createMatteBorder(0, 0, Math.max(1, s(1)), 0, Color.decode("#f1f5f9")));
        }

        Font baseFont = getBaseFont();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, s(25), 0, s(15));

        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        rankPanel.setOpaque(false);
        rankPanel.setPreferredSize(new Dimension(s(30), s(30)));

        JLabel rankLbl = new JLabel(String.valueOf(rank));
        rankLbl.setFont(baseFont.deriveFont(Font.BOLD, f(16f)));
        if (rank == 1) rankLbl.setForeground(Color.decode("#d97706"));
        else if (rank == 2) rankLbl.setForeground(Color.decode("#94a3b8"));
        else if (rank == 3) rankLbl.setForeground(Color.decode("#b45309"));
        else rankLbl.setForeground(Color.decode("#94a3b8"));
        rankPanel.add(rankLbl);

        gbc.gridx = 0;
        row.add(rankPanel, gbc);

        JLabel avatarLbl = new JLabel();
        avatarLbl.setPreferredSize(new Dimension(s(36), s(36)));
        avatarLbl.setIcon(new AvatarIcon(profilePic, getInitials(username)));

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, s(15));
        row.add(avatarLbl, gbc);

        JPanel userInfoContainer = new JPanel();
        userInfoContainer.setLayout(new BoxLayout(userInfoContainer, BoxLayout.Y_AXIS));
        userInfoContainer.setOpaque(false);

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        namePanel.setOpaque(false);
        namePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLbl = new JLabel(username);
        nameLbl.setFont(baseFont.deriveFont(Font.BOLD, f(15f)));
        nameLbl.setForeground(Color.decode("#0f172a"));
        namePanel.add(nameLbl);

        if (isCurrentUser) {
            JLabel youBadge = new JLabel("YOU") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.decode("#3b82f6"));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(10), s(10));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            youBadge.setOpaque(false);
            youBadge.setFont(baseFont.deriveFont(Font.BOLD, f(9f)));
            youBadge.setForeground(Color.WHITE);
            youBadge.setBorder(BorderFactory.createEmptyBorder(s(2), s(6), s(2), s(6)));

            JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, s(8), 0));
            badgeWrapper.setOpaque(false);
            badgeWrapper.add(youBadge);
            badgeWrapper.setBorder(BorderFactory.createEmptyBorder(s(1), 0, 0, 0));

            namePanel.add(badgeWrapper);
        }

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        datePanel.setOpaque(false);
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateLbl = new JLabel();
        if (date != null) {
            dateLbl.setText("Achieved on " + new SimpleDateFormat("MMM dd, yyyy").format(date));
        } else {
            dateLbl.setText("Unknown date");
        }
        dateLbl.setFont(baseFont.deriveFont(Font.PLAIN, f(11f)));
        dateLbl.setForeground(Color.decode("#64748b"));
        datePanel.add(dateLbl);

        userInfoContainer.add(namePanel);
        userInfoContainer.add(Box.createVerticalStrut(s(2)));
        userInfoContainer.add(datePanel);

        JPanel nameWrapper = new JPanel(new GridBagLayout());
        nameWrapper.setOpaque(false);
        GridBagConstraints nGbc = new GridBagConstraints();
        nGbc.anchor = GridBagConstraints.WEST;
        nGbc.weightx = 1.0;
        nameWrapper.add(userInfoContainer, nGbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        row.add(nameWrapper, gbc);

        JLabel scoreLbl = new JLabel(score + "/" + total);
        scoreLbl.setFont(baseFont.deriveFont(Font.BOLD, f(16f)));
        scoreLbl.setForeground(Color.decode("#1e40af"));
        scoreLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, s(35));
        row.add(scoreLbl, gbc);

        return row;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        if (name.equalsIgnoreCase("admin")) return "AD";
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    private class AvatarIcon implements Icon {
        private final Image profilePic;
        private final String text;

        public AvatarIcon(Image profilePic, String text) {
            this.profilePic = profilePic;
            this.text = text;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ImageUtil.paintAvatar((Graphics2D) g, x, y, getIconWidth(), profilePic, text);
        }

        @Override
        public int getIconWidth() {
            return s(36);
        }

        @Override
        public int getIconHeight() {
            return s(36);
        }
    }

    private class SkeletonPanel extends JPanel {
        private float alpha = 0.3f;
        private boolean increasing = true;
        private final Timer timer;

        SkeletonPanel() {
            setOpaque(false);
            timer = new Timer(50, e -> {
                if (increasing) {
                    alpha += 0.05f;
                    if (alpha >= 0.8f) increasing = false;
                } else {
                    alpha -= 0.05f;
                    if (alpha <= 0.3f) increasing = true;
                }
                repaint();
            });
            timer.start();
        }

        void stop() {
            timer.stop();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(-1, s(5) + (5 * s(65)) + s(5));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = s(20);

            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            g2.setColor(Color.decode("#e2e8f0"));
            g2.setStroke(new BasicStroke(Math.max(1, s(1))));
            g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, arc, arc));

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.decode("#e2e8f0"));

            int rowHeight = s(65);
            int maxRows = 5;
            for (int i = 0; i < maxRows; i++) {
                int y = s(5) + (i * rowHeight);

                g2.fillRoundRect(s(32), y + s(25), s(15), s(15), s(4), s(4));
                g2.fillOval(s(70), y + s(14), s(36), s(36));

                g2.fillRoundRect(s(121), y + s(18), s(100), s(14), s(6), s(6));
                g2.fillRoundRect(s(121), y + s(38), s(75), s(10), s(4), s(4));

                g2.fillRoundRect(getWidth() - s(80), y + s(25), s(45), s(14), s(6), s(6));

                if (i < maxRows - 1) {
                    g2.setColor(Color.decode("#f1f5f9"));
                    g2.fillRect(0, y + rowHeight - 1, getWidth(), Math.max(1, s(1)));
                    g2.setColor(Color.decode("#e2e8f0"));
                }
            }
            g2.dispose();
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JPanel();
        root.setLayout(new BorderLayout(0, 0));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setOpaque(false);
        root.add(panel1, BorderLayout.NORTH);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        backBtn = new JButton();
        backBtn.setText("Back");
        panel1.add(backBtn, BorderLayout.WEST);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel2.setOpaque(false);
        root.add(panel2, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel3.setOpaque(false);
        panel2.add(panel3, BorderLayout.NORTH);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 5, 15, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        headerTitleLbl = new JLabel();
        headerTitleLbl.setText("LEADERBOARD");
        panel3.add(headerTitleLbl, BorderLayout.WEST);
        deckBadgeLbl = new JLabel();
        deckBadgeLbl.setText("DECK NAME");
        panel3.add(deckBadgeLbl, BorderLayout.EAST);
        scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        panel2.add(scrollPane, BorderLayout.CENTER);
        listAlignmentWrapper = new JPanel();
        listAlignmentWrapper.setLayout(new BorderLayout(0, 0));
        listAlignmentWrapper.setOpaque(false);
        scrollPane.setViewportView(listAlignmentWrapper);
        listAlignmentWrapper.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 2, 10, 2), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}