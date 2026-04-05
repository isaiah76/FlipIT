package com.flipit.ui.panels;

import com.flipit.dao.CardProgressDAO;
import com.flipit.models.Deck;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class QuizResultsPanel extends JPanel {
    private JPanel root;
    private JButton backBtn;

    private JPanel cardPanel;
    private JPanel bluePanel;
    private JPanel rightPanel;
    private JLabel resultsHeaderLbl;
    private JLabel mainScorePctLbl;
    private JLabel mainScoreSubLbl;

    private JPanel statsWrapper;
    private JPanel statsSkeletonContainer;
    private JPanel statsPanel;
    private JPanel correctPanel;
    private JPanel missedPanel;
    private JPanel bestContentPanel;
    private JLabel correctValLbl;
    private JLabel missedValLbl;
    private JLabel bestScoreLbl;

    private JPanel footerPanel;
    private JPanel actionsPanel;
    private JPanel actionWrap1;
    private JPanel actionWrap2;
    private JPanel actionWrap3;
    private JButton reviewAnswersBtn;
    private JButton tryAgainBtn;
    private JButton leaderboardBtn;

    private final MainPanel mainPanel;
    private final Deck deck;
    private SkeletonPanel skeletonPanel;

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

    public QuizResultsPanel(MainPanel mainPanel, Deck deck, int finalScore, int totalCards) {
        this.mainPanel = mainPanel;
        this.deck = deck;

        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        mainScorePctLbl.setText(finalScore + " / " + totalCards);

        correctValLbl.setText(String.valueOf(finalScore));
        missedValLbl.setText(String.valueOf(totalCards - finalScore));

        skeletonPanel = new SkeletonPanel();
        statsSkeletonContainer.setLayout(new BorderLayout());
        statsSkeletonContainer.add(skeletonPanel, BorderLayout.CENTER);
        CardLayout cl = (CardLayout) statsWrapper.getLayout();
        cl.show(statsWrapper, "SKELETON");

        styleAll();
        bindEvents();

        new SwingWorker<CardProgressDAO.BestScoreResult, Void>() {
            @Override
            protected CardProgressDAO.BestScoreResult doInBackground() {
                return new CardProgressDAO().updateAndGetBestScore(
                        mainPanel.getUser().getId(), deck.getId(), finalScore, totalCards);
            }

            @Override
            protected void done() {
                try {
                    CardProgressDAO.BestScoreResult result = get();
                    bestScoreLbl.setText(result.bestPct + "%");

                    if (result.isNewBest) {
                        for (Component c : bestContentPanel.getComponents()) {
                            if (c instanceof JLabel) {
                                JLabel lbl = (JLabel) c;
                                if ("PERSONAL BEST".equals(lbl.getText())) {
                                    lbl.setText("NEW BEST!");
                                    lbl.setForeground(Color.decode("#ea580c"));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    bestScoreLbl.setText("--%");
                } finally {
                    skeletonPanel.stop();
                    ((CardLayout) statsWrapper.getLayout()).show(statsWrapper, "CONTENT");
                }
            }
        }.execute();
    }

    private void styleAll() {
        Color bgTheme = Color.decode("#f8fafc");
        root.setBackground(bgTheme);
        Font baseFont = getBaseFont();

        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        backBtn.setForeground(Color.decode("#2563eb"));
        backBtn.setIconTextGap(s(8));
        backBtn.setIcon(IconUtil.getIcon("BACK", Color.decode("#2563eb"), s(14)));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cardPanel.setOpaque(false);
        cardPanel.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = s(20);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);

                Shape clip = new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), arc, arc);
                g2.setClip(clip);
                g2.setColor(Color.decode("#2563eb"));
                int blueWidth = bluePanel.getWidth();
                g2.fillRect(0, 0, blueWidth, c.getHeight());

                g2.setClip(null);
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1f, s(1) * 1.5f)));
                g2.drawRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, arc, arc);

                g2.drawLine(blueWidth, 0, blueWidth, c.getHeight());
                g2.dispose();
            }
        });

        bluePanel.setMinimumSize(new Dimension(10, 10));
        bluePanel.setPreferredSize(new Dimension(10, 10));
        rightPanel.setMinimumSize(new Dimension(10, 10));
        rightPanel.setPreferredSize(new Dimension(10, 10));

        resultsHeaderLbl.setFont(baseFont.deriveFont(Font.BOLD, f(11f)));
        resultsHeaderLbl.setForeground(new Color(255, 255, 255, 200));

        mainScorePctLbl.setFont(baseFont.deriveFont(Font.BOLD, f(42f)));
        mainScorePctLbl.setForeground(Color.WHITE);
        mainScorePctLbl.setBorder(BorderFactory.createEmptyBorder(s(15), 0, 0, 0));

        mainScoreSubLbl.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        mainScoreSubLbl.setForeground(new Color(255, 255, 255, 200));

        correctPanel.setBorder(BorderFactory.createEmptyBorder(s(25), s(25), s(25), s(25)));
        bestContentPanel.setBorder(BorderFactory.createEmptyBorder(s(25), s(25), s(25), s(25)));

        missedPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, Math.max(1, s(1)), 0, Math.max(1, s(1)), Color.decode("#e2e8f0")),
                BorderFactory.createEmptyBorder(s(25), s(25), s(25), s(25))
        ));

        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(Math.max(1, s(1)), 0, 0, 0, Color.decode("#e2e8f0")),
                BorderFactory.createEmptyBorder(s(20), 0, s(25), 0)
        ));

        actionsPanel.setBorder(BorderFactory.createEmptyBorder(0, s(25), 0, s(25)));

        actionWrap1.setBorder(BorderFactory.createEmptyBorder());
        actionWrap2.setBorder(BorderFactory.createEmptyBorder());
        actionWrap3.setBorder(BorderFactory.createEmptyBorder());

        correctValLbl.setFont(baseFont.deriveFont(Font.BOLD, f(42f)));
        correctValLbl.setForeground(Color.decode("#16a34a")); // Green

        missedValLbl.setFont(baseFont.deriveFont(Font.BOLD, f(42f)));
        missedValLbl.setForeground(Color.decode("#dc2626")); // Red

        bestScoreLbl.setFont(baseFont.deriveFont(Font.BOLD, f(42f)));
        bestScoreLbl.setForeground(Color.decode("#ea580c")); // Orange

        styleSubLabels(statsPanel, baseFont);

        reviewAnswersBtn.setPreferredSize(new Dimension(0, s(42)));
        leaderboardBtn.setPreferredSize(new Dimension(0, s(42)));
        tryAgainBtn.setPreferredSize(new Dimension(0, s(42)));

        styleOutlineBtn(reviewAnswersBtn, baseFont);
        styleYellowBtn(leaderboardBtn, baseFont);
        stylePrimaryBtn(tryAgainBtn, baseFont);
    }

    private void styleSubLabels(Container parent, Font baseFont) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel) {
                JLabel lbl = (JLabel) c;
                String text = lbl.getText();
                if ("CORRECT".equals(text) || "MISSED".equals(text) || "PERSONAL BEST".equals(text)) {
                    lbl.setFont(baseFont.deriveFont(Font.BOLD, f(11f)));
                    lbl.setForeground(Color.decode("#94a3b8"));
                }
            } else if (c instanceof Container) {
                styleSubLabels((Container) c, baseFont);
            }
        }
    }

    private void stylePrimaryBtn(JButton btn, Font baseFont) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, f(13f)));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean enabled = c.isEnabled();
                boolean hov = ((AbstractButton) c).getModel().isRollover();

                if (!enabled) g2.setColor(Color.decode("#cbd5e1"));
                else {
                    g2.setColor(hov ? Color.decode("#1d4ed8") : Color.decode("#2563eb"));
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), s(10), s(10)));

                g2.setFont(c.getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String txt = ((JButton) c).getText();
                int x = (c.getWidth() - fm.stringWidth(txt)) / 2;
                int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(txt, x, y);
                g2.dispose();
            }
        });
        bindHoverRepaint(btn);
    }

    private void styleYellowBtn(JButton btn, Font baseFont) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, f(13f)));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean enabled = c.isEnabled();
                boolean hov = ((AbstractButton) c).getModel().isRollover();

                if (!enabled) g2.setColor(Color.decode("#fde047"));
                else {
                    g2.setColor(hov ? Color.decode("#ca8a04") : Color.decode("#eab308"));
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), s(10), s(10)));

                g2.setFont(c.getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String txt = ((JButton) c).getText();
                int x = (c.getWidth() - fm.stringWidth(txt)) / 2;
                int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(txt, x, y);
                g2.dispose();
            }
        });
        bindHoverRepaint(btn);
    }

    private void styleOutlineBtn(JButton btn, Font baseFont) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, f(13f)));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = ((AbstractButton) c).getModel().isRollover();

                g2.setColor(hov ? Color.decode("#eff6ff") : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), s(10), s(10)));

                g2.setColor(hov ? Color.decode("#2563eb") : Color.decode("#cbd5e1"));
                g2.setStroke(new BasicStroke(Math.max(1f, s(1) * 1.5f)));
                g2.draw(new RoundRectangle2D.Double(1, 1, c.getWidth() - 2, c.getHeight() - 2, s(8), s(8)));

                c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                g2.setFont(c.getFont());
                g2.setColor(Color.decode("#2563eb"));
                FontMetrics fm = g2.getFontMetrics();
                String txt = ((JButton) c).getText();
                int x = (c.getWidth() - fm.stringWidth(txt)) / 2;
                int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(txt, x, y);
                g2.dispose();
            }
        });
        bindHoverRepaint(btn);
    }

    private void bindHoverRepaint(JButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.repaint();
            }
        });
    }

    private void bindEvents() {
        backBtn.addActionListener(e -> {
            try {
                mainPanel.returnFromQuiz();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        reviewAnswersBtn.addActionListener(e -> {
            try {
                mainPanel.getClass().getMethod("resumeQuizForReview").invoke(mainPanel);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        leaderboardBtn.addActionListener(e -> mainPanel.showLeaderboard(deck));

        tryAgainBtn.addActionListener(e -> {
            tryAgainBtn.setEnabled(false);
            tryAgainBtn.setText("Resetting...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    CardProgressDAO progressDAO = new CardProgressDAO();
                    progressDAO.resetProgress(mainPanel.getUser().getId(), deck.getId());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        try {
                            mainPanel.getClass().getMethod("clearActiveSession").invoke(mainPanel);
                        } catch (Exception ignored) {
                        }

                        mainPanel.getClass().getMethod("startQuiz", Deck.class).invoke(mainPanel, deck);
                    } catch (Exception ex) {
                        tryAgainBtn.setEnabled(true);
                        tryAgainBtn.setText("Try Again");
                        Window parentWindow = SwingUtilities.getWindowAncestor(QuizResultsPanel.this);
                        new InfoDialog(parentWindow, "Error", "Failed to restart quiz.").setVisible(true);
                        ex.printStackTrace();
                    }
                }
            }.execute();
        });
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
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.decode("#e2e8f0"));

            int w = getWidth();
            int h = getHeight();
            int colWidth = w / 3;

            int centerY = h / 2;

            for (int i = 0; i < 3; i++) {
                int cx = (i * colWidth) + (colWidth / 2);

                g2.fillRoundRect(cx - s(40), centerY - s(25), s(80), s(40), s(8), s(8));
                g2.fillRoundRect(cx - s(35), centerY + s(25), s(70), s(12), s(6), s(6));
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
        root.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(25, 38, 25, 38), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel1.setOpaque(false);
        root.add(panel1, BorderLayout.NORTH);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        backBtn = new JButton();
        backBtn.setText("Back");
        panel1.add(backBtn);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel2.setOpaque(false);
        root.add(panel2, BorderLayout.CENTER);
        cardPanel = new JPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setOpaque(false);
        panel2.add(cardPanel, BorderLayout.CENTER);
        bluePanel = new JPanel();
        bluePanel.setLayout(new GridBagLayout());
        bluePanel.setOpaque(false);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        cardPanel.add(bluePanel, gbc);
        bluePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, 4));
        panel3.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        bluePanel.add(panel3, gbc);
        resultsHeaderLbl = new JLabel();
        resultsHeaderLbl.setHorizontalAlignment(0);
        resultsHeaderLbl.setText("QUIZ RESULTS");
        panel3.add(resultsHeaderLbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mainScorePctLbl = new JLabel();
        mainScorePctLbl.setHorizontalAlignment(0);
        mainScorePctLbl.setText("60%");
        panel3.add(mainScorePctLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mainScoreSubLbl = new JLabel();
        mainScoreSubLbl.setHorizontalAlignment(0);
        mainScoreSubLbl.setText("SCORE");
        panel3.add(mainScoreSubLbl, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(0, 0));
        rightPanel.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        cardPanel.add(rightPanel, gbc);
        statsWrapper = new JPanel();
        statsWrapper.setLayout(new CardLayout(0, 0));
        statsWrapper.setOpaque(false);
        rightPanel.add(statsWrapper, BorderLayout.CENTER);
        statsSkeletonContainer = new JPanel();
        statsSkeletonContainer.setLayout(new BorderLayout(0, 0));
        statsSkeletonContainer.setOpaque(false);
        statsWrapper.add(statsSkeletonContainer, "SKELETON");
        statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), 0, 0, true, false));
        statsPanel.setOpaque(false);
        statsWrapper.add(statsPanel, "CONTENT");
        correctPanel = new JPanel();
        correctPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, 4));
        correctPanel.setOpaque(false);
        statsPanel.add(correctPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        correctPanel.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        correctValLbl = new JLabel();
        correctValLbl.setHorizontalAlignment(0);
        correctValLbl.setText("6");
        correctPanel.add(correctValLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(0);
        label1.setText("CORRECT");
        correctPanel.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        correctPanel.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        missedPanel = new JPanel();
        missedPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, 4));
        missedPanel.setOpaque(false);
        statsPanel.add(missedPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        missedPanel.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        missedValLbl = new JLabel();
        missedValLbl.setHorizontalAlignment(0);
        missedValLbl.setText("4");
        missedPanel.add(missedValLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(0);
        label2.setText("MISSED");
        missedPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        missedPanel.add(spacer4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        bestContentPanel = new JPanel();
        bestContentPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, 4));
        bestContentPanel.setOpaque(false);
        statsPanel.add(bestContentPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        bestContentPanel.add(spacer5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        bestScoreLbl = new JLabel();
        bestScoreLbl.setHorizontalAlignment(0);
        bestScoreLbl.setText("80%");
        bestContentPanel.add(bestScoreLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setHorizontalAlignment(0);
        label3.setText("PERSONAL BEST");
        bestContentPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        bestContentPanel.add(spacer6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        footerPanel = new JPanel();
        footerPanel.setLayout(new BorderLayout(0, 0));
        footerPanel.setOpaque(false);
        rightPanel.add(footerPanel, BorderLayout.SOUTH);
        actionsPanel = new JPanel();
        actionsPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), 15, 0, true, false));
        actionsPanel.setOpaque(false);
        footerPanel.add(actionsPanel, BorderLayout.CENTER);
        actionWrap1 = new JPanel();
        actionWrap1.setLayout(new BorderLayout(0, 0));
        actionWrap1.setOpaque(false);
        actionsPanel.add(actionWrap1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        reviewAnswersBtn = new JButton();
        reviewAnswersBtn.setText("Review Answers");
        actionWrap1.add(reviewAnswersBtn, BorderLayout.CENTER);
        actionWrap2 = new JPanel();
        actionWrap2.setLayout(new BorderLayout(0, 0));
        actionWrap2.setOpaque(false);
        actionsPanel.add(actionWrap2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        leaderboardBtn = new JButton();
        leaderboardBtn.setText("Leaderboard");
        actionWrap2.add(leaderboardBtn, BorderLayout.CENTER);
        actionWrap3 = new JPanel();
        actionWrap3.setLayout(new BorderLayout(0, 0));
        actionWrap3.setOpaque(false);
        actionsPanel.add(actionWrap3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tryAgainBtn = new JButton();
        tryAgainBtn.setText("Try Again");
        actionWrap3.add(tryAgainBtn, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}