package com.flipit.ui.panels;

import com.flipit.dao.CardDAO;
import com.flipit.dao.CardProgressDAO;
import com.flipit.dao.DeckDAO;
import com.flipit.models.Deck;
import com.flipit.models.User;
import com.flipit.ui.dialogs.GenerationDialog;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.util.ImageUtil;
import com.flipit.util.IconUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.PanelUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class UploadPanel extends JPanel {
    private JPanel root;
    private JPanel scrollContent;
    private JPanel uploadPanel;
    private JPanel deckListPanel;
    private JPanel sectionHeaderPanel;
    private JLabel uploadIcon;
    private JLabel titleuploadLabel;
    private JLabel subtitleuploadLabel;
    private JPanel badgesPanel;
    private JLabel badgeLabel1;
    private JLabel badgeLabel2;
    private JLabel badgeLabel3;
    private JLabel badgeLabel4;
    private JLabel badgeLabel5;
    private JLabel recentdecksLabel;
    private JButton viewallBtn;

    private MainPanel mainPanel;
    private User user;
    private final DeckDAO deckDAO = new DeckDAO();
    private SwingWorker<List<DeckRenderData>, Void> deckLoaderWorker;

    private static class DeckRenderData {
        Deck deck;
        int totalCards;
        int answeredCards;

        public DeckRenderData(Deck deck, int totalCards, int answeredCards) {
            this.deck = deck;
            this.totalCards = totalCards;
            this.answeredCards = answeredCards;
        }
    }

    public UploadPanel() {
    }

    public UploadPanel(MainPanel mainPanel, User user) {
        this.mainPanel = mainPanel;
        this.user = user;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleRoot();
        styleUploadZone();
        styleLabels();
        styleSectionHeader();

        uploadPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openFilePicker();
            }
        });

        viewallBtn.addActionListener(e -> mainPanel.showDecks());

        loadRecentDecks();
    }

    private void styleRoot() {
        root.setBackground(Color.decode("#f8fafc"));
        if (scrollContent != null) scrollContent.setBackground(Color.decode("#f8fafc"));

        for (Component c : root.getComponents()) {
            if (c instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) c;
                scrollPane.getVerticalScrollBar().setUnitIncrement(ImageUtil.scale(16));
                scrollPane.getVerticalScrollBar().setBlockIncrement(ImageUtil.scale(64));
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
            }
        }
    }

    private void styleUploadZone() {
        final boolean[] hov = {false};

        uploadPanel.setPreferredSize(new Dimension(-1, ImageUtil.scale(340)));
        uploadPanel.setOpaque(false);
        uploadPanel.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(60), ImageUtil.scale(38),
                ImageUtil.scale(60), ImageUtil.scale(38)
        ));
        uploadPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GridBagLayout layout = (GridBagLayout) scrollContent.getLayout();
        GridBagConstraints gbc = layout.getConstraints(uploadPanel);
        gbc.insets = new Insets(ImageUtil.scale(20), ImageUtil.scale(20), ImageUtil.scale(25), ImageUtil.scale(20));
        layout.setConstraints(uploadPanel, gbc);

        uploadIcon.setText("");

        BufferedImage rawImg = ImageUtil.loadImage("/outbox.png");

        if (rawImg != null) {
            uploadIcon.setIcon(new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    g2.drawImage(rawImg, x, y, getIconWidth(), getIconHeight(), null);
                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return ImageUtil.scale(90);
                }

                @Override
                public int getIconHeight() {
                    return ImageUtil.scale(90);
                }
            });
        }

        uploadPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hov[0] = true;
                uploadPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hov[0] = false;
                uploadPanel.repaint();
            }
        });

        PanelUI painter = new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = ImageUtil.scale(20);
                int innerArc = ImageUtil.scale(18);

                Color bg1 = hov[0] ? Color.decode("#dbeafe") : Color.decode("#eff6ff");
                Color bg2 = hov[0] ? Color.decode("#bfdbfe") : Color.decode("#dbeafe");

                g2.setPaint(new GradientPaint(0, 0, bg1, c.getWidth(), c.getHeight(), bg2));
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), arc, arc));

                float[] dash = {ImageUtil.scale(12), ImageUtil.scale(6)};
                g2.setStroke(new BasicStroke(ImageUtil.scale(3), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 10, dash, 0));
                g2.setColor(hov[0] ? Color.decode("#1e40af") : Color.decode("#3b82f6"));
                g2.draw(new RoundRectangle2D.Double(2, 2, c.getWidth() - 4, c.getHeight() - 4, innerArc, innerArc));
                g2.dispose();
            }
        };
        uploadPanel.setUI(painter);
    }

    private void styleLabels() {
        badgesPanel.setOpaque(false);
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        if (titleuploadLabel != null) titleuploadLabel.setFont(baseFont.deriveFont(Font.BOLD, 19f * sf));
        if (subtitleuploadLabel != null) subtitleuploadLabel.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));

        for (JLabel badge : new JLabel[]{badgeLabel1, badgeLabel2, badgeLabel3, badgeLabel4, badgeLabel5}) {
            if (badge == null) continue;
            badge.setFont(baseFont.deriveFont(Font.BOLD, 11f * sf));
            badge.setForeground(Color.decode("#3b82f6"));
            badge.setOpaque(true);
            badge.setBackground(Color.WHITE);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.decode("#3b82f6"), Math.max(1, ImageUtil.scale(2))),
                    BorderFactory.createEmptyBorder(ImageUtil.scale(7), ImageUtil.scale(14),
                            ImageUtil.scale(7), ImageUtil.scale(14))));
        }
    }

    private void styleSectionHeader() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        sectionHeaderPanel.setBackground(Color.decode("#f8fafc"));
        sectionHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(20), 0, ImageUtil.scale(20)));

        if (recentdecksLabel != null) recentdecksLabel.setFont(baseFont.deriveFont(Font.BOLD, 17f * sf));

        viewallBtn.setContentAreaFilled(false);
        viewallBtn.setBorderPainted(false);
        viewallBtn.setFocusPainted(false);
        viewallBtn.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
        viewallBtn.setForeground(Color.decode("#3b82f6"));
        viewallBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewallBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                viewallBtn.setForeground(Color.decode("#1e40af"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                viewallBtn.setForeground(Color.decode("#3b82f6"));
            }
        });
    }

    private void loadRecentDecks() {
        if (deckListPanel == null) return;

        if (deckLoaderWorker != null && !deckLoaderWorker.isDone()) {
            deckLoaderWorker.cancel(true);
        }

        deckListPanel.removeAll();
        deckListPanel.setLayout(new BoxLayout(deckListPanel, BoxLayout.Y_AXIS));
        deckListPanel.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(20), 0, ImageUtil.scale(20)));

        for (int i = 0; i < 3; i++) {
            deckListPanel.add(buildSkeletonRow());
            deckListPanel.add(Box.createVerticalStrut(ImageUtil.scale(12)));
        }
        deckListPanel.add(Box.createVerticalGlue());
        deckListPanel.revalidate();
        deckListPanel.repaint();

        deckLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<DeckRenderData> doInBackground() throws Exception {
                List<Deck> decks = deckDAO.getAllDecksByUser(user.getId());
                if (isCancelled()) return null;

                List<Deck> generatedDecks = new ArrayList<>();
                for (Deck d : decks) {
                    if (d.getSourceFileName() != null && !d.getSourceFileName().trim().isEmpty()) {
                        generatedDecks.add(d);
                    }
                }

                List<Deck> recent = generatedDecks.subList(0, Math.min(3, generatedDecks.size()));

                CardDAO cardDAO = new CardDAO();
                CardProgressDAO progressDAO = new CardProgressDAO();

                List<DeckRenderData> renderDataList = new ArrayList<>();

                for (Deck deck : recent) {
                    int total = cardDAO.getAllCardsByDeck(deck.getId()).size();
                    int answered = progressDAO.getAnsweredCount(user.getId(), deck.getId());
                    renderDataList.add(new DeckRenderData(deck, total, answered));
                }

                return renderDataList;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<DeckRenderData> recent = get();
                    if (recent == null) return;

                    deckListPanel.removeAll();

                    if (recent.isEmpty()) {
                        JLabel empty = new JLabel("No decks yet, upload a file to get started!");
                        Font baseFont = UIManager.getFont("defaultFont");
                        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
                        empty.setFont(baseFont.deriveFont(Font.PLAIN, 13f * ImageUtil.getScaleFactor()));
                        empty.setForeground(Color.decode("#64748b"));
                        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                        deckListPanel.add(empty);
                    } else {
                        for (DeckRenderData data : recent) {
                            deckListPanel.add(buildDeckRow(data));
                            deckListPanel.add(Box.createVerticalStrut(ImageUtil.scale(12)));
                        }
                        deckListPanel.add(Box.createVerticalGlue());
                    }
                    deckListPanel.revalidate();
                    deckListPanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        deckLoaderWorker.execute();
    }

    private JPanel buildSkeletonRow() {
        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = ImageUtil.scale(16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);

                g2.setColor(Color.decode("#f1f5f9"));
                int rx = ImageUtil.scale(6);

                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(25), ImageUtil.scale(180), ImageUtil.scale(16), rx, rx);
                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(48), ImageUtil.scale(120), ImageUtil.scale(12), rx, rx);

                int btnWidth = ImageUtil.scale(110);
                int btnHeight = ImageUtil.scale(36);
                g2.fillRoundRect(getWidth() - btnWidth - ImageUtil.scale(20), (getHeight() - btnHeight) / 2, btnWidth, btnHeight, arc, arc);

                int progWidth = ImageUtil.scale(140);
                int progHeight = ImageUtil.scale(10);
                g2.fillRoundRect(getWidth() - btnWidth - progWidth - ImageUtil.scale(40), (getHeight() - progHeight) / 2, progWidth, progHeight, rx, rx);

                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(95)));
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(95)));
        return row;
    }

    private JPanel buildDeckRow(DeckRenderData data) {
        Deck deck = data.deck;

        JPanel row = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ImageUtil.scale(16), ImageUtil.scale(16));
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, ImageUtil.scale(16), ImageUtil.scale(16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(95)));
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(95)));
        row.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(15), ImageUtil.scale(20), ImageUtil.scale(15), ImageUtil.scale(20)));

        Font baseFont = UIManager.getFont("defaultFont");
        float sf = ImageUtil.getScaleFactor();

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(8), 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLbl = new JLabel(deck.getTitle());
        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, 15f * sf));
        titleLbl.setForeground(Color.decode("#0f172a"));
        titleRow.add(titleLbl);

        JLabel badge = createBadge(deck.isPublic());
        titleRow.add(badge);

        JLabel fileLbl = new JLabel(deck.getSourceFileName());
        fileLbl.setIcon(IconUtil.getIcon("DOC", Color.decode("#64748b"), ImageUtil.scale(14)));
        fileLbl.setIconTextGap(ImageUtil.scale(6));
        fileLbl.setFont(baseFont.deriveFont(Font.PLAIN, 12f * sf));
        fileLbl.setForeground(Color.decode("#64748b"));
        fileLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        fileLbl.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(2), ImageUtil.scale(8), 0, 0));

        String dateStr = deck.getCreatedAt() != null ? new SimpleDateFormat("MMM dd, yyyy").format(deck.getCreatedAt()) : "Unknown Date";

        JLabel infoLbl = new JLabel(dateStr + "   " + data.totalCards + " cards");
        infoLbl.setFont(baseFont.deriveFont(Font.PLAIN, 11f * sf));
        infoLbl.setForeground(Color.decode("#64748b"));
        infoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoLbl.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(4), ImageUtil.scale(8), 0, 0));

        leftPanel.add(titleRow);
        leftPanel.add(fileLbl);
        leftPanel.add(infoLbl);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ImageUtil.scale(12), 0));
        progressPanel.setOpaque(false);

        float percentage = data.totalCards > 0 ? ((float) data.answeredCards / data.totalCards) * 100 : 0f;
        int pctInt = Math.round(percentage);

        ProgressBar pBar = new ProgressBar(percentage, pctInt == 100);
        JLabel pctLabel = new JLabel(pctInt + "%");
        pctLabel.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
        pctLabel.setForeground(Color.decode("#64748b"));

        progressPanel.add(pBar);
        progressPanel.add(pctLabel);

        JButton actionBtn = new JButton(pctInt == 100 ? "Completed" : "Practice");
        actionBtn.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        actionBtn.setForeground(Color.WHITE);
        actionBtn.setBackground(pctInt == 100 ? Color.decode("#10b981") : Color.decode("#2563eb"));
        actionBtn.setFocusPainted(false);
        actionBtn.setBorderPainted(false);
        actionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actionBtn.setPreferredSize(new Dimension(ImageUtil.scale(110), ImageUtil.scale(36)));
        actionBtn.addActionListener(e -> mainPanel.startQuiz(deck));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(leftPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, ImageUtil.scale(20));
        row.add(progressPanel, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(actionBtn, gbc);

        return row;
    }

    private JLabel createBadge(boolean isPublic) {
        JLabel badge = new JLabel(isPublic ? "Public" : "Private") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isPublic ? Color.decode("#dcfce7") : Color.decode("#f1f5f9"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ImageUtil.scale(12), ImageUtil.scale(12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);

        badge.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, 10f * ImageUtil.getScaleFactor()));
        badge.setForeground(isPublic ? Color.decode("#15803d") : Color.decode("#475569"));

        badge.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(4), ImageUtil.scale(14), ImageUtil.scale(4), ImageUtil.scale(14)
        ));
        badge.setHorizontalAlignment(SwingConstants.CENTER);

        return badge;
    }

    private static class ProgressBar extends JComponent {
        private final float percentage;
        private final boolean isCompleted;

        public ProgressBar(float percentage, boolean isCompleted) {
            this.percentage = percentage;
            this.isCompleted = isCompleted;
            setPreferredSize(new Dimension(ImageUtil.scale(120), ImageUtil.scale(6)));
            setMinimumSize(new Dimension(ImageUtil.scale(120), ImageUtil.scale(6)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int h = getHeight();
            int w = getWidth();

            g2.setColor(Color.decode("#e2e8f0"));
            g2.fillRoundRect(0, 0, w, h, h, h);

            if (percentage > 0) {
                int fillWidth = (int) ((percentage / 100f) * w);
                g2.setColor(isCompleted ? Color.decode("#10b981") : Color.decode("#2563eb"));
                g2.fillRoundRect(0, 0, fillWidth, h, h, h);
            }
            g2.dispose();
        }
    }

    private void openFilePicker() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a study file");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Study Files (PDF, DOCX, PPTX, TXT, MD)",
                "pdf", "docx", "pptx", "txt", "md"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".pdf") && !fileName.endsWith(".docx") &&
                    !fileName.endsWith(".pptx") && !fileName.endsWith(".txt") &&
                    !fileName.endsWith(".md")) {

                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                new InfoDialog(parentWindow, "Invalid File", "Invalid file format! Only PDF, DOCX, PPTX, TXT, and MD are supported.").setVisible(true);
                return;
            }

            new GenerationDialog(mainPanel.getAppFrame(), mainPanel, user, file).setVisible(true);
        }
    }

    public void refresh() {
        loadRecentDecks();
    }

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
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, BorderLayout.CENTER);
        scrollContent = new JPanel();
        scrollContent.setLayout(new GridBagLayout());
        scrollPane1.setViewportView(scrollContent);
        scrollContent.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        uploadPanel = new JPanel();
        uploadPanel.setLayout(new GridBagLayout());
        uploadPanel.setPreferredSize(new Dimension(-1, 260));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 25, 0);
        scrollContent.add(uploadPanel, gbc);
        uploadIcon = new JLabel();
        uploadIcon.setHorizontalAlignment(0);
        uploadIcon.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 16, 0);
        uploadPanel.add(uploadIcon, gbc);
        titleuploadLabel = new JLabel();
        titleuploadLabel.setHorizontalAlignment(0);
        titleuploadLabel.setText("Upload Files");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        uploadPanel.add(titleuploadLabel, gbc);
        subtitleuploadLabel = new JLabel();
        subtitleuploadLabel.setHorizontalAlignment(0);
        subtitleuploadLabel.setText("Click here to browse files");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        uploadPanel.add(subtitleuploadLabel, gbc);
        badgesPanel = new JPanel();
        badgesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        uploadPanel.add(badgesPanel, gbc);
        badgeLabel1 = new JLabel();
        badgeLabel1.setText(".PDF");
        badgesPanel.add(badgeLabel1);
        badgeLabel2 = new JLabel();
        badgeLabel2.setText(".DOCX");
        badgesPanel.add(badgeLabel2);
        badgeLabel3 = new JLabel();
        badgeLabel3.setText(".PPTX");
        badgesPanel.add(badgeLabel3);
        badgeLabel4 = new JLabel();
        badgeLabel4.setText(".TXT");
        badgesPanel.add(badgeLabel4);
        badgeLabel5 = new JLabel();
        badgeLabel5.setText(".MD");
        badgesPanel.add(badgeLabel5);
        sectionHeaderPanel = new JPanel();
        sectionHeaderPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);
        scrollContent.add(sectionHeaderPanel, gbc);
        recentdecksLabel = new JLabel();
        recentdecksLabel.setText("Recent Generated Decks");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 6, 0, 0);
        sectionHeaderPanel.add(recentdecksLabel, gbc);
        viewallBtn = new JButton();
        viewallBtn.setText("View all →");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        sectionHeaderPanel.add(viewallBtn, gbc);
        deckListPanel = new JPanel();
        deckListPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        scrollContent.add(deckListPanel, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}