package com.flipit.ui.panels;

import com.flipit.dao.CardDAO;
import com.flipit.dao.CardProgressDAO;
import com.flipit.dao.DeckDAO;
import com.flipit.models.Deck;
import com.flipit.models.User;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.dialogs.InputDialog;
import com.flipit.util.ImageUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.plaf.PanelUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class HomePanel extends JPanel {
    private JPanel root;
    private JScrollPane scrollPane;
    private JPanel scrollContent;
    private JPanel headerPanel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JPanel headerGraphicPanel;
    private JPanel actionsPanel;
    private JPanel generateCard;
    private JPanel createCard;
    private JPanel recentHeaderPanel;
    private JLabel recentLabel;
    private JButton viewAllBtn;
    private JPanel deckListPanel;

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

    public HomePanel() {
    }

    public HomePanel(MainPanel mainPanel, User user) {
        this.mainPanel = mainPanel;
        this.user = user;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleRoot();
        styleHeader();
        setupActionCards();
        styleRecentSection();

        viewAllBtn.addActionListener(e -> mainPanel.showDecks());

        loadRecentDecks();
    }

    private void styleRoot() {
        root.setBackground(Color.decode("#f8fafc"));
        scrollContent.setBackground(Color.decode("#f8fafc"));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        headerPanel.setOpaque(false);
        actionsPanel.setOpaque(false);
        recentHeaderPanel.setOpaque(false);
        deckListPanel.setOpaque(false);

        actionsPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(30), ImageUtil.scale(40), 0, ImageUtil.scale(40)));

        actionsPanel.removeAll();
        actionsPanel.setLayout(new GridLayout(1, 2, ImageUtil.scale(30), 0));
        actionsPanel.add(generateCard);
        actionsPanel.add(createCard);
    }

    private void styleHeader() {
        Font logoFont = UIManager.getFont("logoFont");
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        titleLabel.setText("<html>Welcome back, <font color='#2563eb'>" + user.getUsername() + "</font></html>");

        if (logoFont != null) {
            titleLabel.setFont(logoFont.deriveFont(42f * sf));
        } else {
            titleLabel.setFont(baseFont.deriveFont(Font.BOLD, 42f * sf));
        }
        titleLabel.setForeground(Color.decode("#0f172a"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(10), 0, ImageUtil.scale(15), 0));
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        subtitleLabel.setText("<html><div style='text-align: left; width: "
                + ImageUtil.scale(360) + "px;'>Your personal flashcard reviewer for your everyday studying.</div></html>");
        subtitleLabel.setFont(baseFont.deriveFont(Font.PLAIN, 18f * sf));
        subtitleLabel.setForeground(Color.decode("#475569"));
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        headerPanel.removeAll();
        headerPanel.setLayout(new GridBagLayout());

        JPanel textWrapper = new JPanel();
        textWrapper.setLayout(new BoxLayout(textWrapper, BoxLayout.Y_AXIS));
        textWrapper.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textWrapper.add(titleLabel);
        textWrapper.add(subtitleLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(ImageUtil.scale(30), ImageUtil.scale(40), ImageUtil.scale(20), 0);
        headerPanel.add(textWrapper, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(ImageUtil.scale(20), 0, ImageUtil.scale(20), ImageUtil.scale(40));
        headerPanel.add(buildHeaderCardStack(), gbc);

        headerPanel.revalidate();
        headerPanel.repaint();
    }

    private JPanel buildHeaderCardStack() {
        JPanel stackPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int arc = ImageUtil.scale(16);

                Color backCardColor = Color.decode("#eff6ff");
                Color frontCardColor = Color.WHITE;
                Color shadowColor = new Color(0, 0, 0, 15);
                Color accentColor = Color.decode("#2563eb");

                int backX = w - ImageUtil.scale(250);
                int backY = ImageUtil.scale(15);

                int midX = w - ImageUtil.scale(280);
                int midY = ImageUtil.scale(35);

                int frontX = w - ImageUtil.scale(300);
                int frontY = ImageUtil.scale(55);

                int cardW = ImageUtil.scale(280);
                int cardH = ImageUtil.scale(140);

                g2.translate(backX, backY);
                g2.rotate(Math.toRadians(5));
                g2.setColor(shadowColor);
                g2.fillRoundRect(2, 4, cardW, cardH, arc, arc);
                g2.setColor(backCardColor);
                g2.fillRoundRect(0, 0, cardW, cardH, arc, arc);
                g2.setColor(Color.decode("#bfdbfe"));
                g2.drawRoundRect(0, 0, cardW, cardH, arc, arc);
                g2.rotate(-Math.toRadians(5));
                g2.translate(-backX, -backY);

                g2.translate(midX, midY);
                g2.setColor(shadowColor);
                g2.fillRoundRect(2, 4, cardW, cardH, arc, arc);
                g2.setColor(frontCardColor);
                g2.fillRoundRect(0, 0, cardW, cardH, arc, arc);
                g2.setColor(Color.decode("#e2e8f0"));
                g2.drawRoundRect(0, 0, cardW, cardH, arc, arc);
                g2.translate(-midX, -midY);

                g2.translate(frontX, frontY);
                g2.setColor(shadowColor);
                g2.fillRoundRect(0, 8, cardW, cardH, arc, arc);
                g2.setColor(frontCardColor);
                g2.fillRoundRect(0, 0, cardW, cardH, arc, arc);
                g2.setColor(Color.decode("#e2e8f0"));
                g2.drawRoundRect(0, 0, cardW, cardH, arc, arc);

                g2.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, 12f * ImageUtil.getScaleFactor()));
                g2.setColor(accentColor);
                g2.drawString("FLIPIT · QUICK TIP", ImageUtil.scale(25), ImageUtil.scale(30));

                g2.setFont(UIManager.getFont("defaultFont").deriveFont(Font.PLAIN, 15f * ImageUtil.getScaleFactor()));
                g2.setColor(Color.decode("#0f172a"));
                g2.drawString("Upload any document", ImageUtil.scale(25), ImageUtil.scale(60));
                g2.drawString("to instantly generate", ImageUtil.scale(25), ImageUtil.scale(80));
                g2.drawString("smart flashcards.", ImageUtil.scale(25), ImageUtil.scale(100));

                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(ImageUtil.scale(25), ImageUtil.scale(122), ImageUtil.scale(255), ImageUtil.scale(122));

                g2.translate(-frontX, -frontY);
                g2.dispose();
            }
        };
        stackPanel.setOpaque(false);
        stackPanel.setPreferredSize(new Dimension(ImageUtil.scale(350), ImageUtil.scale(220)));
        return stackPanel;
    }

    private void setupActionCards() {
        buildActionCard(generateCard, "/generate.png", "Generate from a File", "Upload a PDF, DOCX, PPTX, or TXT and get flashcards instantly.", () -> mainPanel.showUpload());

        buildActionCard(createCard, "/pencil.png", "Create a Deck", "Build your own flashcard deck from scratch, card by card.", () -> {
            mainPanel.showDecks();
            SwingUtilities.invokeLater(this::createNewDeck);
        });
    }

    private void createNewDeck() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        InputDialog dialog = new InputDialog(parentWindow, "Create Deck", "Deck name:", "");
        dialog.setVisible(true);

        if (!dialog.isApproved()) {
            return;
        }

        String name = dialog.getInputText();
        if (name.isBlank()) return;
        name = name.trim();

        if (name.length() > 45) {
            new InfoDialog(parentWindow, "Name Too Long", "Deck name cannot exceed 45 characters.\nPlease choose a shorter name.").setVisible(true);
            return;
        }

        if (deckDAO.isUserTitleTaken(user.getId(), name, -1)) {
            new InfoDialog(parentWindow, "Name Conflict", "You already have a deck with this name.").setVisible(true);
            return;
        }

        deckDAO.createDeck(user.getId(), null, name, "", false);
        mainPanel.invalidateDeckCaches();
        mainPanel.showDecks();
    }

    private void addHoverListenerRecursively(Component comp, MouseAdapter listener) {
        comp.addMouseListener(listener);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                addHoverListenerRecursively(child, listener);
            }
        }
    }

    private void buildActionCard(JPanel card, String iconPath, String title, String desc, Runnable onClick) {
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.setPreferredSize(new Dimension(0, ImageUtil.scale(150)));
        card.setMinimumSize(new Dimension(ImageUtil.scale(350), ImageUtil.scale(150)));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        JLabel iconLbl = new JLabel();
        BufferedImage rawImg = ImageUtil.loadImage(iconPath);
        if (rawImg != null) {
            iconLbl.setIcon(new Icon() {
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
                    return ImageUtil.scale(42);
                }

                @Override
                public int getIconHeight() {
                    return ImageUtil.scale(42);
                }
            });
        }

        JPanel iconWrapper = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#eff6ff"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ImageUtil.scale(18), ImageUtil.scale(18));
                g2.dispose();
            }
        };
        iconWrapper.setOpaque(false);
        iconWrapper.setPreferredSize(new Dimension(ImageUtil.scale(72), ImageUtil.scale(72)));
        iconWrapper.setMinimumSize(new Dimension(ImageUtil.scale(72), ImageUtil.scale(72)));
        iconWrapper.add(iconLbl);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, 17f * sf));
        titleLbl.setForeground(Color.decode("#0f172a"));
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea descArea = new JTextArea(desc);
        descArea.setFont(baseFont.deriveFont(Font.PLAIN, 14f * sf));
        descArea.setForeground(Color.decode("#64748b"));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setHighlighter(null);
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descArea.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(6), 0, 0, 0));
        descArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        textPanel.add(titleLbl);
        textPanel.add(descArea);

        card.removeAll();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, ImageUtil.scale(25), 0, ImageUtil.scale(20));
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(iconWrapper, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, ImageUtil.scale(25));
        card.add(textPanel, gbc);

        final boolean[] isHovered = {false};

        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered[0] = true;
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    isHovered[0] = false;
                    card.repaint();
                }
            }
        };

        addHoverListenerRecursively(card, hoverAdapter);

        int sOx = ImageUtil.scale(-4);
        int sOy = ImageUtil.scale(4);

        card.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = ImageUtil.scale(20);

                int cardX = Math.max(0, -sOx);
                int cardY = Math.max(0, -sOy);
                int shadowDrawX = Math.max(0, sOx);
                int shadowDrawY = Math.max(0, sOy);

                int drawWidth = c.getWidth() - Math.abs(sOx);
                int drawHeight = c.getHeight() - Math.abs(sOy);

                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(shadowDrawX, shadowDrawY, drawWidth, drawHeight, arc, arc);
                g2.setColor(isHovered[0] ? Color.decode("#f8fafc") : Color.WHITE);
                g2.fillRoundRect(cardX, cardY, drawWidth, drawHeight, arc, arc);

                if (isHovered[0]) {
                    g2.setColor(Color.decode("#3b82f6"));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(cardX + 1, cardY + 1, drawWidth - 2, drawHeight - 2, arc, arc);
                }

                g2.dispose();
            }
        });

        card.setBorder(BorderFactory.createEmptyBorder(
                sOy < 0 ? Math.abs(sOy) : 0,
                sOx < 0 ? Math.abs(sOx) : 0,
                sOy > 0 ? sOy : 0,
                sOx > 0 ? sOx : 0
        ));
    }

    private void styleRecentSection() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        recentHeaderPanel.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(50), ImageUtil.scale(40), ImageUtil.scale(15), ImageUtil.scale(40)
        ));

        deckListPanel.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(40), ImageUtil.scale(40), ImageUtil.scale(40)));

        recentLabel.setFont(baseFont.deriveFont(Font.BOLD, 18f * sf));
        recentLabel.setForeground(Color.decode("#0f172a"));

        viewAllBtn.setContentAreaFilled(false);
        viewAllBtn.setBorderPainted(false);
        viewAllBtn.setFocusPainted(false);
        viewAllBtn.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        viewAllBtn.setForeground(Color.decode("#2563eb"));
        viewAllBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        viewAllBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                viewAllBtn.setForeground(Color.decode("#1d4ed8"));
            }

            public void mouseExited(MouseEvent e) {
                viewAllBtn.setForeground(Color.decode("#2563eb"));
            }
        });
    }

    private void loadRecentDecks() {
        if (deckLoaderWorker != null && !deckLoaderWorker.isDone()) deckLoaderWorker.cancel(true);

        deckListPanel.removeAll();
        deckListPanel.setLayout(new BoxLayout(deckListPanel, BoxLayout.Y_AXIS));

        for (int i = 0; i < 3; i++) {
            deckListPanel.add(buildSkeletonRow());
            deckListPanel.add(Box.createVerticalStrut(ImageUtil.scale(16)));
        }
        deckListPanel.add(Box.createVerticalGlue());
        deckListPanel.revalidate();
        deckListPanel.repaint();

        deckLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<DeckRenderData> doInBackground() {
                List<Deck> decks = deckDAO.getAllDecksByUser(user.getId());
                List<Deck> recent = decks.subList(0, Math.min(3, decks.size()));

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
                    deckListPanel.removeAll();

                    if (recent == null || recent.isEmpty()) {
                        JLabel empty = new JLabel("No decks yet. Start by generating or creating one!");
                        empty.setFont(UIManager.getFont("defaultFont").deriveFont(15f * ImageUtil.getScaleFactor()));
                        empty.setForeground(Color.decode("#64748b"));
                        deckListPanel.add(empty);
                    } else {
                        for (DeckRenderData data : recent) {
                            deckListPanel.add(buildDeckRow(data));
                            deckListPanel.add(Box.createVerticalStrut(ImageUtil.scale(16)));
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

                int arc = ImageUtil.scale(20);
                int sOx = ImageUtil.scale(-4);
                int sOy = ImageUtil.scale(4);

                int cardX = Math.max(0, -sOx);
                int cardY = Math.max(0, -sOy);
                int shadowDrawX = Math.max(0, sOx);
                int shadowDrawY = Math.max(0, sOy);

                int drawWidth = getWidth() - Math.abs(sOx);
                int drawHeight = getHeight() - Math.abs(sOy);

                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(shadowDrawX, shadowDrawY, drawWidth, drawHeight, arc, arc);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(cardX, cardY, drawWidth, drawHeight, arc, arc);

                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(cardX + 1, cardY + 1, drawWidth - 2, drawHeight - 2, arc, arc);

                g2.setColor(Color.decode("#f1f5f9"));
                int rx = ImageUtil.scale(8);

                g2.fillRoundRect(cardX + ImageUtil.scale(25), cardY + ImageUtil.scale(26), ImageUtil.scale(220), ImageUtil.scale(20), rx, rx);
                g2.fillRoundRect(cardX + ImageUtil.scale(25), cardY + ImageUtil.scale(56), ImageUtil.scale(140), ImageUtil.scale(14), rx, rx);

                int btnWidth = ImageUtil.scale(130);
                int btnHeight = ImageUtil.scale(42);
                g2.fillRoundRect(cardX + drawWidth - btnWidth - ImageUtil.scale(25), cardY + (drawHeight - btnHeight) / 2, btnWidth, btnHeight, arc, arc);

                int progWidth = ImageUtil.scale(160);
                int progHeight = ImageUtil.scale(10);
                g2.fillRoundRect(cardX + drawWidth - btnWidth - progWidth - ImageUtil.scale(45), cardY + (drawHeight - progHeight) / 2, progWidth, progHeight, rx, rx);

                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(104)));
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(104)));
        return row;
    }

    private JPanel buildDeckRow(DeckRenderData data) {
        Deck deck = data.deck;

        int sOx = ImageUtil.scale(-4);
        int sOy = ImageUtil.scale(4);

        JPanel row = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = ImageUtil.scale(20);

                int cardX = Math.max(0, -sOx);
                int cardY = Math.max(0, -sOy);
                int shadowDrawX = Math.max(0, sOx);
                int shadowDrawY = Math.max(0, sOy);

                int drawWidth = getWidth() - Math.abs(sOx);
                int drawHeight = getHeight() - Math.abs(sOy);

                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(shadowDrawX, shadowDrawY, drawWidth, drawHeight, arc, arc);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(cardX, cardY, drawWidth, drawHeight, arc, arc);

                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(cardX + 1, cardY + 1, drawWidth - 2, drawHeight - 2, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(104)));
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(104)));
        row.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(15) + (sOy < 0 ? Math.abs(sOy) : 0),
                ImageUtil.scale(25) + (sOx < 0 ? Math.abs(sOx) : 0),
                ImageUtil.scale(19) + (sOy > 0 ? sOy : 0),
                ImageUtil.scale(25) + (sOx > 0 ? sOx : 0)
        ));

        Font baseFont = UIManager.getFont("defaultFont");
        float sf = ImageUtil.getScaleFactor();

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(10), 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLbl = new JLabel(deck.getTitle());
        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, 18f * sf));
        titleLbl.setForeground(Color.decode("#0f172a"));
        titleRow.add(titleLbl);

        JLabel badge = createBadge(deck.isPublic());
        titleRow.add(badge);

        String dateStr = deck.getCreatedAt() != null ? new SimpleDateFormat("MMM dd, yyyy").format(deck.getCreatedAt()) : "Unknown Date";

        JLabel infoLbl = new JLabel(dateStr + "   " + data.totalCards + " cards");
        infoLbl.setFont(baseFont.deriveFont(Font.PLAIN, 14f * sf));
        infoLbl.setForeground(Color.decode("#94a3b8"));
        infoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoLbl.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(4), ImageUtil.scale(10), 0, 0));

        leftPanel.add(titleRow);
        leftPanel.add(infoLbl);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ImageUtil.scale(14), 0));
        progressPanel.setOpaque(false);

        float percentage = data.totalCards > 0 ? ((float) data.answeredCards / data.totalCards) * 100 : 0f;
        int pctInt = Math.round(percentage);

        ProgressBar pBar = new ProgressBar(percentage, pctInt == 100);
        JLabel pctLabel = new JLabel(pctInt + "%");
        pctLabel.setFont(baseFont.deriveFont(Font.PLAIN, 14f * sf));
        pctLabel.setForeground(Color.decode("#64748b"));

        progressPanel.add(pBar);
        progressPanel.add(pctLabel);

        JButton actionBtn = new JButton(pctInt == 100 ? "Completed" : "Practice");
        actionBtn.setFont(baseFont.deriveFont(Font.BOLD, 14f * sf));
        actionBtn.setForeground(Color.WHITE);
        actionBtn.setBackground(pctInt == 100 ? Color.decode("#10b981") : Color.decode("#2563eb"));
        actionBtn.setFocusPainted(false);
        actionBtn.setBorderPainted(false);
        actionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actionBtn.setPreferredSize(new Dimension(ImageUtil.scale(130), ImageUtil.scale(40)));
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
        gbc.insets = new Insets(0, 0, 0, ImageUtil.scale(25));
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ImageUtil.scale(14), ImageUtil.scale(14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);

        badge.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, 11f * ImageUtil.getScaleFactor()));
        badge.setForeground(isPublic ? Color.decode("#15803d") : Color.decode("#475569"));

        badge.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(4), ImageUtil.scale(14), ImageUtil.scale(4), ImageUtil.scale(14)
        ));
        badge.setHorizontalAlignment(SwingConstants.CENTER);

        return badge;
    }

    public void refresh() {
        loadRecentDecks();
    }

    private static class ProgressBar extends JComponent {
        private final float percentage;
        private final boolean isCompleted;

        public ProgressBar(float percentage, boolean isCompleted) {
            this.percentage = percentage;
            this.isCompleted = isCompleted;
            setPreferredSize(new Dimension(ImageUtil.scale(140), ImageUtil.scale(8)));
            setMinimumSize(new Dimension(ImageUtil.scale(140), ImageUtil.scale(8)));
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
        scrollContent = new JPanel();
        scrollContent.setLayout(new GridBagLayout());
        scrollPane.setViewportView(scrollContent);
        headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scrollContent.add(headerPanel, gbc);
        titleLabel = new JLabel();
        titleLabel.setText("FlipIT");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        headerPanel.add(titleLabel, gbc);
        subtitleLabel = new JLabel();
        subtitleLabel.setText("Subtitle");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        headerPanel.add(subtitleLabel, gbc);
        headerGraphicPanel = new JPanel();
        headerGraphicPanel.setLayout(new BorderLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.EAST;
        headerPanel.add(headerGraphicPanel, gbc);
        actionsPanel = new JPanel();
        actionsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, true));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scrollContent.add(actionsPanel, gbc);
        generateCard = new JPanel();
        generateCard.setLayout(new BorderLayout(0, 0));
        actionsPanel.add(generateCard, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        createCard = new JPanel();
        createCard.setLayout(new BorderLayout(0, 0));
        actionsPanel.add(createCard, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        recentHeaderPanel = new JPanel();
        recentHeaderPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scrollContent.add(recentHeaderPanel, gbc);
        recentLabel = new JLabel();
        recentLabel.setText("Recent Decks");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        recentHeaderPanel.add(recentLabel, gbc);
        viewAllBtn = new JButton();
        viewAllBtn.setText("View all →");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        recentHeaderPanel.add(viewAllBtn, gbc);
        deckListPanel = new JPanel();
        deckListPanel.setLayout(new BorderLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scrollContent.add(deckListPanel, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}