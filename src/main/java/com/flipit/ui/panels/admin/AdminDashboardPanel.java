package com.flipit.ui.panels.admin;

import com.flipit.dao.AdminStatsDAO;
import com.flipit.models.User;
import com.flipit.ui.panels.MainPanel;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AdminDashboardPanel extends JPanel {
    private JPanel root;
    private JScrollPane scrollPane;
    private JPanel bodyPanel;
    private JButton refreshButton;

    // ── State ──────────────────────────────────────────────────────────────────
    private final MainPanel mainPanel;
    private final User adminUser;
    private final AdminStatsDAO statsDAO = new AdminStatsDAO();

    // ── Colours ────────────────────────────────────────────────────────────────
    private static final Color BG = Color.decode("#f0f2f8");
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = Color.decode("#e2e8f0");
    private static final Color BLUE = Color.decode("#3b82f6");
    private static final Color GREEN = Color.decode("#10b981");
    private static final Color RED = Color.decode("#ef4444");
    private static final Color AMBER = Color.decode("#f59e0b");
    private static final Color PURPLE = Color.decode("#8b5cf6");
    private static final Color SLATE = Color.decode("#64748b");

    // ── Constructor ────────────────────────────────────────────────────────────
    public AdminDashboardPanel(MainPanel mainPanel, User adminUser) {
        this.mainPanel = mainPanel;
        this.adminUser = adminUser;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        initRefreshButton();
        styleRoot();
        refresh();
    }

    public void refresh() {
        showSkeletons();

        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                return statsDAO.getAllDashboardStats();
            }

            @Override
            protected void done() {
                try {
                    buildDashboard(get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError();
                }
            }
        }.execute();
    }

    // ── Layout builders ────────────────────────────────────────────────────────

    private void buildDashboard(int[] s) {
        bodyPanel.removeAll();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        // Perfectly balanced padding to prevent scrollbars
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(20), ImageUtil.scale(30), ImageUtil.scale(20), ImageUtil.scale(30)
        ));

        // ── Section 1: Users (With Aligned Refresh Button) ───────────────────
        JPanel usersHeaderRow = new JPanel(new BorderLayout());
        usersHeaderRow.setOpaque(false);
        usersHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        usersHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(55)));

        usersHeaderRow.add(sectionLabel("Users", BLUE), BorderLayout.WEST);

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.setOpaque(false);
        btnWrapper.add(refreshButton);
        usersHeaderRow.add(btnWrapper, BorderLayout.EAST);

        bodyPanel.add(usersHeaderRow);
        bodyPanel.add(Box.createVerticalStrut(ImageUtil.scale(10)));
        bodyPanel.add(buildRow(new JPanel[]{
                statCard("Total Users", s[0], BLUE, "All registered accounts"),
                statCard("Active Users", s[1], GREEN, "Active accounts"),
                statCard("Disabled Users", s[2], RED, "Suspended accounts"),
                statCard("New This Week", s[3], PURPLE, "Registered in last 7 days"),
        }));
        bodyPanel.add(Box.createVerticalStrut(ImageUtil.scale(20)));

        // ── Section 2: Content (Title Removed) ───────────────────────────────
        bodyPanel.add(buildRow(new JPanel[]{
                statCard("Total Decks", s[4], BLUE, "All decks"),
                statCard("Total Cards", s[5], AMBER, "Flashcards across all decks"),
                statCard("Public Decks", s[6], GREEN, "Visible across users"),
                statCard("Disabled Decks", s[7], RED, "Disabled by moderators"),
        }));
        bodyPanel.add(Box.createVerticalStrut(ImageUtil.scale(20)));

        // ── Section 3: Moderation ────────────────────────────────────────────
        bodyPanel.add(sectionLabel("Moderation", RED));
        bodyPanel.add(Box.createVerticalStrut(ImageUtil.scale(10)));
        bodyPanel.add(buildRow(new JPanel[]{
                statCard("Pending Reports", s[8], s[8] > 0 ? RED : GREEN, s[8] > 0 ? "Needs review" : "Nothing to review"),
                emptyCard(),
                emptyCard(),
                emptyCard(),
        }));

        bodyPanel.revalidate();
        bodyPanel.repaint();

        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    // ── Card builders ──────────────────────────────────────────────────────────

    private JPanel statCard(String title, int value, Color accent, String subtitle) {
        JPanel card = roundCard();
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(15), ImageUtil.scale(20), ImageUtil.scale(15), ImageUtil.scale(20)
        ));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        JPanel stripe = stripePanel(accent);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        titleLbl.setForeground(SLATE);

        JLabel valueLbl = new JLabel(String.valueOf(value));
        valueLbl.setFont(baseFont.deriveFont(Font.BOLD, 36f * sf));
        valueLbl.setForeground(accent);
        valueLbl.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(baseFont.deriveFont(Font.PLAIN, 11f * sf));
        subLbl.setForeground(Color.decode("#94a3b8"));

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.add(titleLbl, BorderLayout.NORTH);
        wrapper.add(valueLbl, BorderLayout.CENTER);
        wrapper.add(subLbl, BorderLayout.SOUTH);

        card.add(stripe, BorderLayout.NORTH);
        card.add(wrapper, BorderLayout.CENTER);

        return card;
    }

    private JPanel emptyCard() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    // ── Skeleton loading ───────────────────────────────────────────────────────

    private void showSkeletons() {
        bodyPanel.removeAll();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(20), ImageUtil.scale(30), ImageUtil.scale(20), ImageUtil.scale(30)
        ));

        for (int s = 0; s < 3; s++) {
            if (s == 0) {
                JPanel headerRow = new JPanel(new BorderLayout());
                headerRow.setOpaque(false);
                headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(55)));
                headerRow.add(skeletonSectionLabel(), BorderLayout.WEST);
                bodyPanel.add(headerRow);
            } else {
                bodyPanel.add(skeletonSectionLabel());
            }

            bodyPanel.add(Box.createVerticalStrut(ImageUtil.scale(10)));
            JPanel row = new JPanel(new GridLayout(1, 4, ImageUtil.scale(20), 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(145)));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (int i = 0; i < 4; i++) row.add(skeletonCard());
            bodyPanel.add(row);
            bodyPanel.add(Box.createVerticalStrut(ImageUtil.scale(20)));
        }

        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    private JPanel skeletonCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int rad = ImageUtil.scale(15);

                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, rad, rad));
                g2.setColor(BORDER);
                // FIX: Use getScaleFactor() directly for floating-point stroke widths
                g2.setStroke(new BasicStroke(Math.max(1f, 1.3f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, rad - 1, rad - 1));

                g2.setColor(Color.decode("#f1f5f9"));
                int smRad = ImageUtil.scale(4);
                int mdRad = ImageUtil.scale(6);

                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(20), ImageUtil.scale(66), ImageUtil.scale(10), smRad, smRad);
                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(50), ImageUtil.scale(88), ImageUtil.scale(35), mdRad, mdRad);
                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(105), ImageUtil.scale(110), ImageUtil.scale(8), ImageUtil.scale(3), ImageUtil.scale(3));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JLabel skeletonSectionLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setPreferredSize(new Dimension(0, ImageUtil.scale(22)));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void showError() {
        bodyPanel.removeAll();
        bodyPanel.setLayout(new BorderLayout());
        JLabel err = new JLabel("Failed to load statistics. Check your connection.", SwingConstants.CENTER);
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        err.setFont(baseFont.deriveFont(Font.PLAIN, 13f * ImageUtil.getScaleFactor()));
        err.setForeground(RED);
        bodyPanel.add(err);
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private void initRefreshButton() {
        refreshButton = new JButton();
        refreshButton.setPreferredSize(new Dimension(ImageUtil.scale(44), ImageUtil.scale(44)));
        refreshButton.setBackground(Color.WHITE);
        refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))));
        refreshButton.setFocusPainted(false);
        refreshButton.setOpaque(true);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#8792a8"), ImageUtil.scale(16)));

        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#3b82f6"), Math.max(1, ImageUtil.scale(2))));
                refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#3b82f6"), ImageUtil.scale(16)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))));
                refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#8792a8"), ImageUtil.scale(16)));
            }
        });

        refreshButton.addActionListener(e -> refresh());
    }

    /**
     * Replaces FlowLayout with GridLayout to force perfect column fitting, preventing any horizontal scroll.
     */
    private JPanel buildRow(JPanel[] cards) {
        JPanel row = new JPanel(new GridLayout(1, cards.length, ImageUtil.scale(20), 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(145)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JPanel c : cards) {
            row.add(c);
        }
        return row;
    }

    private JLabel sectionLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        lbl.setFont(baseFont.deriveFont(Font.BOLD, 15f * ImageUtil.getScaleFactor()));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(2), 0, 0));
        return lbl;
    }

    private JPanel roundCard() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int rad = ImageUtil.scale(15);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, rad, rad));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(Math.max(1f, 1.3f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, rad - 1, rad - 1));
                g2.dispose();
                super.paintComponent(g);
            }
        };
    }

    private JPanel stripePanel(Color color) {
        JPanel stripe = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(color);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(0, ImageUtil.scale(4)));
        return stripe;
    }

    private void styleRoot() {
        root.setBackground(BG);
        bodyPanel.setBackground(BG);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder());

        // ── ScrollPane styling ──────────────────────────────────────────────
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BG);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(ImageUtil.scale(20));
    }

    // ── IntelliJ GUI Designer boilerplate ──────────────────────────────────────

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(31);
        root.add(scrollPane, BorderLayout.CENTER);
        bodyPanel = new JPanel();
        bodyPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 33, 44));
        scrollPane.setViewportView(bodyPanel);
        bodyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}