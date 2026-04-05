package com.flipit.ui.panels;

import com.flipit.dao.CardProgressDAO;
import com.flipit.dao.DeckDAO;
import com.flipit.dao.FileDAO;
import com.flipit.dao.ReportDAO;
import com.flipit.dao.UserDAO;
import com.flipit.models.Deck;
import com.flipit.models.User;
import com.flipit.ui.dialogs.*;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DecksPanel extends JPanel {
    private JPanel root;
    private JPanel topBarPanel;
    private JPanel gridWrapperPanel;
    private JPanel searchPanel;
    private JTextField searchField;
    private JLabel searchLabel;
    private JPanel tabPanel;
    private JButton myDecksTab;
    private JButton savedDecksTab;
    private JComboBox<String> sortComboBox;
    private JButton refreshButton;
    private JButton addDeckBtn;
    private JPanel deckGridPanel;

    private MainPanel mainPanel;
    private User user;

    private final DeckDAO deckDAO = new DeckDAO();
    private final CardProgressDAO progressDAO = new CardProgressDAO();
    private final UserDAO userDAO = new UserDAO();

    private boolean isSavedView = false;
    private boolean isUpdatingPlaceholder = false;

    private SwingWorker<List<DeckRenderData>, Void> deckLoaderWorker;
    private final Timer searchDebounceTimer;

    private List<DeckRenderData> currentDisplayedDecks = new ArrayList<>();

    private List<DeckRenderData> cachedMyDecks = null;
    private List<DeckRenderData> cachedSavedDecks = null;
    private long cacheTimestampMyDecks = 0;
    private long cacheTimestampSavedDecks = 0;
    private static final long CACHE_TTL_MS = 30_000;

    private static class DeckRenderData {
        Deck deck;
        Image avatar;
        int answered;
        int saves;
        int views;
        boolean isSaved;
    }

    public DecksPanel() {
        searchDebounceTimer = new Timer(300, e -> loadDecks());
    }

    public DecksPanel(MainPanel mainPanel, User user) {
        this.mainPanel = mainPanel;
        this.user = user;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        searchDebounceTimer = new Timer(300, e -> loadDecks());
        searchDebounceTimer.setRepeats(false);

        styleAll();

        addDeckBtn.addActionListener(e -> createNewDeck());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (!isUpdatingPlaceholder) searchDebounceTimer.restart();
            }

            public void removeUpdate(DocumentEvent e) {
                if (!isUpdatingPlaceholder) searchDebounceTimer.restart();
            }

            public void changedUpdate(DocumentEvent e) {
                if (!isUpdatingPlaceholder) searchDebounceTimer.restart();
            }
        });

        myDecksTab.addActionListener(e -> {
            isSavedView = false;
            updateTabs();
            loadDecks();
        });
        savedDecksTab.addActionListener(e -> {
            isSavedView = true;
            updateTabs();
            loadDecks();
        });
        updateTabs();

        sortComboBox.addActionListener(e -> loadDecks());
        refreshButton.addActionListener(e -> refresh());

        if (user.isAdmin()) {
            tabPanel.setVisible(false);
            addDeckBtn.setVisible(false);
        }

        loadDecks();
    }

    public void clearCache() {
        cachedMyDecks = null;
        cachedSavedDecks = null;
    }

    private void updateTabs() {
        styleTab(myDecksTab, !isSavedView);
        styleTab(savedDecksTab, isSavedView);

        if (user != null && !user.isAdmin() && addDeckBtn != null && topBarPanel != null) {
            addDeckBtn.setVisible(!isSavedView);
            topBarPanel.revalidate();
            topBarPanel.repaint();
        }
    }

    private void styleTab(JButton tab, boolean isActive) {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        tab.setContentAreaFilled(false);
        tab.setBorderPainted(false);
        tab.setFocusPainted(false);
        tab.setFont(baseFont.deriveFont(Font.BOLD, 16f * ImageUtil.getScaleFactor()));
        tab.setForeground(isActive ? Color.decode("#3b5bdb") : Color.decode("#8792a8"));
        tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, ImageUtil.scale(3), 0, isActive ? Color.decode("#3b5bdb") : new Color(0, 0, 0, 0)),
                BorderFactory.createEmptyBorder(ImageUtil.scale(6), ImageUtil.scale(6), ImageUtil.scale(6), ImageUtil.scale(6))
        ));
    }

    private void styleAll() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        root.setBackground(Color.decode("#f0f2f8"));

        topBarPanel.setPreferredSize(null);

        topBarPanel.setBackground(Color.decode("#f0f2f8"));
        gridWrapperPanel.setBackground(Color.decode("#f0f2f8"));
        deckGridPanel.setBackground(Color.decode("#f0f2f8"));
        topBarPanel.setOpaque(false);

        searchPanel.setPreferredSize(new Dimension(-1, ImageUtil.scale(50)));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))),
                BorderFactory.createEmptyBorder(0, ImageUtil.scale(8), 0, ImageUtil.scale(8))
        ));

        int searchIconSize = ImageUtil.scale(18);
        searchLabel.setText("");
        searchLabel.setIcon(IconUtil.getIcon("SEARCH", Color.decode("#8792a8"), searchIconSize));
        searchLabel.setPreferredSize(new Dimension(searchIconSize + ImageUtil.scale(6), searchIconSize));

        searchField.setPreferredSize(new Dimension(-1, ImageUtil.scale(48)));
        searchField.setFont(baseFont.deriveFont(Font.PLAIN, 15f * sf));
        searchField.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(4), 0, ImageUtil.scale(4)));

        searchField.setText("Search by deck title or tags...");
        searchField.setForeground(Color.decode("#8792a8"));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                if (searchField.getText().equals("Search by deck title or tags...")) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("");
                    searchField.setForeground(Color.decode("#1a1f36"));
                    isUpdatingPlaceholder = false;
                }
            }

            public void focusLost(FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("Search by deck title or tags...");
                    searchField.setForeground(Color.decode("#8792a8"));
                    isUpdatingPlaceholder = false;
                }
            }
        });

        sortComboBox.setPreferredSize(new Dimension(ImageUtil.scale(162), ImageUtil.scale(50)));
        sortComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Newest", "Oldest", "Most Cards", "Fewest Cards", "Title (A-Z)", "Title (Z-A)", "Public Only", "Private Only"
        }));
        sortComboBox.setFont(baseFont.deriveFont(Font.BOLD, 14f * sf));
        sortComboBox.setBackground(Color.WHITE);
        sortComboBox.setForeground(Color.decode("#8792a8"));
        sortComboBox.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))));
        sortComboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        refreshButton.setPreferredSize(new Dimension(ImageUtil.scale(50), ImageUtil.scale(50)));
        refreshButton.setBackground(Color.WHITE);
        refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))));
        refreshButton.setFocusPainted(false);
        refreshButton.setOpaque(true);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#8792a8"), ImageUtil.scale(18)));

        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#3b82f6"), Math.max(1, ImageUtil.scale(2))));
                refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#3b82f6"), ImageUtil.scale(18)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))));
                refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#8792a8"), ImageUtil.scale(18)));
            }
        });

        addDeckBtn.setText("+ ADD DECK");
        addDeckBtn.setPreferredSize(new Dimension(ImageUtil.scale(130), ImageUtil.scale(50)));
        addDeckBtn.setContentAreaFilled(false);
        addDeckBtn.setBorderPainted(false);
        addDeckBtn.setFocusPainted(false);
        addDeckBtn.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        addDeckBtn.setForeground(Color.WHITE);
        addDeckBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addDeckBtn.setUI(new BasicButtonUI() {
            boolean hov = false;

            {
                addDeckBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        addDeckBtn.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        addDeckBtn.repaint();
                    }
                });
            }

            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0,
                        hov ? Color.decode("#2563eb") : Color.decode("#3b82f6"),
                        c.getWidth(), c.getHeight(),
                        hov ? Color.decode("#1e40af") : Color.decode("#1d4ed8")));
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), ImageUtil.scale(12), ImageUtil.scale(12)));
                g2.dispose();
                super.paint(g, c);
            }
        });

        root.setFocusable(true);
        MouseAdapter clearFocus = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                root.requestFocusInWindow();
            }
        };
        root.addMouseListener(clearFocus);
        deckGridPanel.addMouseListener(clearFocus);
        topBarPanel.addMouseListener(clearFocus);

        for (Component c : root.getComponents()) {
            if (c instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) c;
                scrollPane.getVerticalScrollBar().setUnitIncrement(ImageUtil.scale(20));
                scrollPane.getVerticalScrollBar().setBlockIncrement(ImageUtil.scale(64));
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                scrollPane.getViewport().addMouseListener(clearFocus);
            }
        }
    }

    private void executeOptimisticUpdate() {
        if (currentDisplayedDecks != null) {
            renderList(currentDisplayedDecks, sortComboBox.getSelectedIndex(), isSavedView);
        }
    }

    private void loadDecks() {
        if (deckGridPanel == null) return;

        if (deckLoaderWorker != null && !deckLoaderWorker.isDone()) {
            deckLoaderWorker.cancel(true);
        }

        String q = searchField == null ? "" : searchField.getText().toLowerCase().trim();
        if (q.equals("search by deck title or tags...")) q = "";
        final String searchQuery = q;
        final int sortIdx = sortComboBox.getSelectedIndex();
        final boolean currentSavedView = isSavedView;

        if (searchQuery.isEmpty()) {
            if (currentSavedView && cachedSavedDecks != null && (System.currentTimeMillis() - cacheTimestampSavedDecks < CACHE_TTL_MS)) {
                currentDisplayedDecks = new ArrayList<>(cachedSavedDecks);
                renderList(currentDisplayedDecks, sortIdx, true);
                return;
            } else if (!currentSavedView && cachedMyDecks != null && (System.currentTimeMillis() - cacheTimestampMyDecks < CACHE_TTL_MS)) {
                currentDisplayedDecks = new ArrayList<>(cachedMyDecks);
                renderList(currentDisplayedDecks, sortIdx, false);
                return;
            }
        }

        deckGridPanel.removeAll();
        deckGridPanel.setBackground(Color.decode("#f0f2f8"));

        deckGridPanel.setLayout(new GridBagLayout());
        deckGridPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(15), ImageUtil.scale(10), ImageUtil.scale(15), ImageUtil.scale(10)));
        for (int i = 0; i < 4; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i % 2;
            c.gridy = i / 2;
            c.weightx = 0.5;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.insets = new Insets(0, 0, ImageUtil.scale(16), (i % 2 == 0) ? ImageUtil.scale(16) : 0);
            deckGridPanel.add(buildSkeletonCard(), c);
        }

        GridBagConstraints glueGbc = new GridBagConstraints();
        glueGbc.gridx = 0;
        glueGbc.gridy = 100;
        glueGbc.weighty = 1.0;
        deckGridPanel.add(Box.createGlue(), glueGbc);

        deckGridPanel.revalidate();
        deckGridPanel.repaint();

        deckLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<DeckRenderData> doInBackground() throws Exception {
                List<Deck> decks;
                if (user.isAdmin()) {
                    decks = deckDAO.getAllSystemDecks();
                } else {
                    decks = currentSavedView ? deckDAO.getSavedDecksByUser(user.getId()) : deckDAO.getAllDecksByUser(user.getId());
                }

                if (isCancelled()) return null;

                List<Deck> filtered = new ArrayList<>();
                for (Deck d : decks) {
                    boolean matchesSearch = searchQuery.isEmpty() || d.getTitle().toLowerCase().contains(searchQuery);

                    if (!matchesSearch && d.getTags() != null) {
                        for (String tag : d.getTags()) {
                            if (tag.toLowerCase().contains(searchQuery)) {
                                matchesSearch = true;
                                break;
                            }
                        }
                    }

                    boolean matchesVisibility = true;

                    if (sortIdx == 6 && !d.isPublic()) matchesVisibility = false;
                    if (sortIdx == 7 && d.isPublic()) matchesVisibility = false;

                    if (currentSavedView && !d.isPublic() && !d.isDisabled() && d.getUserId() != user.getId()) {
                        matchesVisibility = false;
                    }

                    if (matchesSearch && matchesVisibility) {
                        filtered.add(d);
                    }
                }

                if (isCancelled()) return null;

                List<Integer> deckIds = filtered.stream().map(Deck::getId).collect(Collectors.toList());
                Map<Integer, Integer> answeredMap = progressDAO.getAnsweredCountBatch(user.getId(), deckIds);
                Map<Integer, Integer> viewsMap = progressDAO.getDeckViewsBatch(deckIds);
                Map<Integer, Integer> savesMap = deckDAO.getDeckSavesBatch(deckIds);
                Set<Integer> savedSet = deckDAO.getSavedDeckIdsBatch(user.getId(), deckIds);

                List<Integer> uniqueUserIds = filtered.stream()
                        .map(Deck::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

                Map<Integer, byte[]> batchAvatars = userDAO.getProfilePicturesBatch(uniqueUserIds);
                Map<Integer, Image> avatarCache = new HashMap<>();

                for (Map.Entry<Integer, byte[]> entry : batchAvatars.entrySet()) {
                    if (isCancelled()) return null;
                    avatarCache.put(entry.getKey(), ImageUtil.getImageFromBytes(entry.getValue()));
                }

                List<DeckRenderData> renderList = new ArrayList<>();

                for (Deck d : filtered) {
                    if (isCancelled()) return null;
                    DeckRenderData data = new DeckRenderData();
                    data.deck = d;

                    data.avatar = avatarCache.get(d.getUserId());
                    data.answered = answeredMap.getOrDefault(d.getId(), 0);
                    data.saves = savesMap.getOrDefault(d.getId(), 0);
                    data.views = viewsMap.getOrDefault(d.getId(), 0);
                    data.isSaved = savedSet.contains(d.getId());

                    renderList.add(data);
                }

                return renderList;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;

                try {
                    List<DeckRenderData> renderList = get();
                    if (renderList == null) return;

                    currentDisplayedDecks = new ArrayList<>(renderList);

                    if (searchQuery.isEmpty()) {
                        if (currentSavedView) {
                            cachedSavedDecks = new ArrayList<>(renderList);
                            cacheTimestampSavedDecks = System.currentTimeMillis();
                        } else {
                            cachedMyDecks = new ArrayList<>(renderList);
                            cacheTimestampMyDecks = System.currentTimeMillis();
                        }
                    }

                    renderList(currentDisplayedDecks, sortIdx, currentSavedView);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        deckLoaderWorker.execute();
    }

    private void renderList(List<DeckRenderData> listToRender, int sortIdx, boolean currentSavedView) {
        if (listToRender == null) return;

        listToRender.sort((d1, d2) -> {
            if (sortIdx == 1) return d1.deck.getCreatedAt().compareTo(d2.deck.getCreatedAt());
            if (sortIdx == 2) return Integer.compare(d2.deck.getCardCount(), d1.deck.getCardCount());
            if (sortIdx == 3) return Integer.compare(d1.deck.getCardCount(), d2.deck.getCardCount());
            if (sortIdx == 4) return d1.deck.getTitle().compareToIgnoreCase(d2.deck.getTitle());
            if (sortIdx == 5) return d2.deck.getTitle().compareToIgnoreCase(d1.deck.getTitle());
            return d2.deck.getCreatedAt().compareTo(d1.deck.getCreatedAt());
        });

        deckGridPanel.removeAll();

        if (listToRender.isEmpty()) {
            deckGridPanel.setLayout(new GridBagLayout());
            deckGridPanel.add(buildEmptyState(currentSavedView));
        } else {
            deckGridPanel.setLayout(new GridBagLayout());
            deckGridPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(15), ImageUtil.scale(10), ImageUtil.scale(15), ImageUtil.scale(10)));

            int col = 0, row = 0;
            for (DeckRenderData data : listToRender) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = col;
                c.gridy = row;
                c.weightx = 0.5;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTH;
                c.insets = new Insets(0, 0, ImageUtil.scale(16), col == 0 ? ImageUtil.scale(16) : 0);
                deckGridPanel.add(buildDeckCard(data, currentSavedView), c);

                col++;
                if (col == 2) {
                    col = 0;
                    row++;
                }
            }

            if (col == 1) {
                GridBagConstraints fillerGbc = new GridBagConstraints();
                fillerGbc.gridx = 1;
                fillerGbc.gridy = row;
                fillerGbc.weightx = 0.5;
                deckGridPanel.add(Box.createHorizontalGlue(), fillerGbc);
            }

            GridBagConstraints glueGbc = new GridBagConstraints();
            glueGbc.gridx = 0;
            glueGbc.gridy = row + 1;
            glueGbc.weighty = 1.0;
            deckGridPanel.add(Box.createGlue(), glueGbc);
        }

        deckGridPanel.revalidate();
        deckGridPanel.repaint();
    }

    private JPanel buildSkeletonCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 2, ImageUtil.scale(13), ImageUtil.scale(13)));
                g2.setColor(Color.decode("#e2e5f0"));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(1))));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 2, ImageUtil.scale(13), ImageUtil.scale(13)));

                g2.setColor(Color.decode("#f1f5f9"));
                int rad = ImageUtil.scale(6);
                int smRad = ImageUtil.scale(4);

                g2.fillRoundRect(ImageUtil.scale(22), ImageUtil.scale(22), ImageUtil.scale(172), ImageUtil.scale(22), rad, rad);
                g2.fillRoundRect(ImageUtil.scale(22), ImageUtil.scale(52), ImageUtil.scale(118), ImageUtil.scale(15), smRad, smRad);

                g2.fillRoundRect(ImageUtil.scale(22), ImageUtil.scale(97), getWidth() - ImageUtil.scale(44), ImageUtil.scale(28), rad, rad);

                g2.fillRoundRect(ImageUtil.scale(22), ImageUtil.scale(151), getWidth() - ImageUtil.scale(44), ImageUtil.scale(7), ImageUtil.scale(3), ImageUtil.scale(3));
                g2.fillRoundRect(ImageUtil.scale(22), ImageUtil.scale(172), ImageUtil.scale(86), ImageUtil.scale(35), rad, rad);
                g2.fillRoundRect(getWidth() - ImageUtil.scale(60), ImageUtil.scale(172), ImageUtil.scale(38), ImageUtil.scale(35), rad, rad);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(0, ImageUtil.scale(240)));
        return card;
    }

    private JPanel buildEmptyState(boolean isSavedView) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(26), ImageUtil.scale(26), ImageUtil.scale(26), ImageUtil.scale(26)));

        Font tempFont = UIManager.getFont("defaultFont");
        if (tempFont == null) tempFont = new Font("SansSerif", Font.PLAIN, 12);
        final Font baseFont = tempFont;
        float sf = ImageUtil.getScaleFactor();

        JLabel msg = new JLabel(isSavedView ? "No saved decks!" : "No decks yet!");
        msg.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));
        msg.setForeground(Color.decode("#1a1f36"));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(6), 0, ImageUtil.scale(4), 0));

        JLabel sub = new JLabel(isSavedView ? "Check the Browse tab to find public decks." : "Upload a file on the Upload tab or Add a deck.");
        sub.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
        sub.setForeground(Color.decode("#8792a8"));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(msg);
        p.add(sub);
        return p;
    }

    private String getTimeAgo(Timestamp ts) {
        if (ts == null) return "";
        LocalDateTime date = ts.toLocalDateTime();
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    private JPanel createAvatarPanel(Image avatarImg, String creatorName, int size) {
        User tempUser = new User(-1, creatorName, "user", true);
        final String initials = tempUser.getInitials();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                ImageUtil.paintAvatar((Graphics2D) g, 0, 0, getWidth(), avatarImg, initials);
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(size, size));
        panel.setMaximumSize(new Dimension(size, size));
        panel.setMinimumSize(new Dimension(size, size));
        return panel;
    }

    private JPanel createStatPill(String iconType, String text, Color bg, Color fg, Color border) {
        Font tempFont = UIManager.getFont("defaultFont");
        if (tempFont == null) tempFont = new Font("SansSerif", Font.PLAIN, 12);
        final Font baseFont = tempFont;

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, ImageUtil.scale(5), ImageUtil.scale(3))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ImageUtil.scale(14), ImageUtil.scale(14));
                g2.setColor(border);
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(1))));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ImageUtil.scale(14), ImageUtil.scale(14));
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(2), ImageUtil.scale(6), ImageUtil.scale(2), ImageUtil.scale(6)));

        JLabel iconLbl = new JLabel(IconUtil.getIcon(iconType, fg, ImageUtil.scale(13)));

        JLabel textLbl = new JLabel(text);
        textLbl.setFont(baseFont.deriveFont(Font.BOLD, 11f * ImageUtil.getScaleFactor()));
        textLbl.setForeground(fg);

        pill.add(iconLbl);
        pill.add(textLbl);
        return pill;
    }

    private JPanel createStatLabel(String iconType, String text, Color fg, float sf) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(4), 0));
        panel.setOpaque(false);

        Font tempFont = UIManager.getFont("defaultFont");
        if (tempFont == null) tempFont = new Font("SansSerif", Font.PLAIN, 12);
        final Font baseFont = tempFont;

        JLabel textLbl = new JLabel(text);
        textLbl.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
        textLbl.setForeground(fg);

        JLabel iconLbl = new JLabel(IconUtil.getIcon(iconType, fg, (int) (16 * sf)));

        panel.add(iconLbl);
        panel.add(textLbl);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, ImageUtil.scale(12)));

        return panel;
    }

    private JLabel createTagLabel(String text, boolean isMoreIndicator, Font baseFont, float sf) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = getHeight();

                if (isMoreIndicator) {
                    g2.setColor(Color.decode("#f8fafc"));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    g2.setColor(Color.decode("#e2e8f0"));
                } else {
                    g2.setColor(Color.decode("#eff6ff"));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    g2.setColor(Color.decode("#bfdbfe"));
                }
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(1))));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setFont(baseFont.deriveFont(Font.BOLD, 10f * sf));
        lbl.setForeground(isMoreIndicator ? Color.decode("#64748b") : Color.decode("#3b82f6"));
        lbl.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(3), ImageUtil.scale(8), ImageUtil.scale(3), ImageUtil.scale(8)));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JPanel createDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(Color.decode("#e2e5f0"));
        divider.setMinimumSize(new Dimension(0, Math.max(1, ImageUtil.scale(1))));
        divider.setPreferredSize(new Dimension(Integer.MAX_VALUE, Math.max(1, ImageUtil.scale(1))));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(1, ImageUtil.scale(1))));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        return divider;
    }

    private void addHoverListenerRecursively(Component comp, MouseAdapter listener) {
        comp.addMouseListener(listener);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                addHoverListenerRecursively(child, listener);
            }
        }
    }

    private JPanel buildDeckCard(DeckRenderData data, boolean isSavedView) {
        Deck deck = data.deck;
        int total = deck.getCardCount();
        boolean isOwnDeck = deck.getUserId() == user.getId();
        int pct = total == 0 ? 0 : (int) Math.round(((double) data.answered / total) * 100);

        String creatorName = deck.getCreatorName() != null ? deck.getCreatorName() : "Unknown";

        Font tempFont = UIManager.getFont("defaultFont");
        if (tempFont == null) tempFont = new Font("SansSerif", Font.PLAIN, 12);
        final Font baseFont = tempFont;
        final float sf = ImageUtil.getScaleFactor();

        final boolean[] isHovered = {false};

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape cardShape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 2, ImageUtil.scale(13), ImageUtil.scale(13));

                if (isHovered[0]) {
                    g2.setColor(new Color(59, 91, 219, 12));
                    g2.fillRoundRect(0, ImageUtil.scale(4), getWidth() - 1, getHeight() - 2, ImageUtil.scale(13), ImageUtil.scale(13));
                }

                g2.setColor(Color.WHITE);
                g2.fill(cardShape);

                if (isHovered[0]) {
                    g2.setClip(cardShape);
                    g2.setPaint(new GradientPaint(0, 0, Color.decode("#3b5bdb"), getWidth(), 0, Color.decode("#748ffc")));
                    g2.fillRect(0, 0, getWidth(), ImageUtil.scale(3));
                    g2.setClip(null);
                }

                g2.setColor(Color.decode("#e2e5f0"));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(1))));
                g2.draw(cardShape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(22), ImageUtil.scale(22), ImageUtil.scale(22), ImageUtil.scale(22)));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String displayTitle = deck.getTitle();
        if (displayTitle.length() > 20) {
            displayTitle = displayTitle.substring(0, 20) + "...";
        }
        JLabel titleLbl = new JLabel(displayTitle);
        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));
        titleLbl.setForeground(Color.decode("#1a1f36"));
        titleRow.add(titleLbl);

        titleRow.add(Box.createHorizontalStrut(ImageUtil.scale(11)));
        boolean isDis = deck.isDisabled();
        boolean isPub = deck.isPublic();
        JPanel visTag = createStatPill(
                isDis ? "DISABLED" : (isPub ? "PUBLIC" : "PRIVATE"),
                isDis ? "Disabled" : (isPub ? "Public" : "Private"),
                isDis ? Color.decode("#fee2e2") : (isPub ? Color.decode("#ebfbee") : Color.decode("#f1f5f9")),
                isDis ? Color.decode("#ef4444") : (isPub ? Color.decode("#2f9e44") : Color.decode("#64748b")),
                isDis ? Color.decode("#fca5a5") : (isPub ? Color.decode("#b2f2bb") : Color.decode("#e2e8f0"))
        );
        titleRow.add(visTag);

        titleBlock.add(titleRow);

        if (deck.getSourceFileName() != null && !deck.getSourceFileName().trim().isEmpty()) {
            String fName = deck.getSourceFileName();
            if (fName.length() > 30) fName = fName.substring(0, 27) + "...";

            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(4), 0));
            filePanel.setOpaque(false);
            filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel fileLbl = new JLabel(fName);
            fileLbl.setFont(baseFont.deriveFont(Font.PLAIN, 12f * sf));
            fileLbl.setForeground(Color.decode("#64748b"));

            JLabel iconLbl = new JLabel(IconUtil.getIcon("DOC", Color.decode("#64748b"), (int) (14 * sf)));

            filePanel.add(iconLbl);
            filePanel.add(fileLbl);
            filePanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(4), 0, ImageUtil.scale(4), 0));
            titleBlock.add(filePanel);
        } else {
            titleBlock.add(Box.createVerticalStrut(ImageUtil.scale(4)));
        }

        titleBlock.add(Box.createVerticalStrut(ImageUtil.scale(8)));

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(6), 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel avatarPanel = createAvatarPanel(data.avatar, creatorName, ImageUtil.scale(22));

        JLabel authorLbl = new JLabel(creatorName);
        authorLbl.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
        authorLbl.setForeground(Color.decode("#8792a8"));

        JLabel dotLbl = new JLabel("•");
        dotLbl.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
        dotLbl.setForeground(Color.decode("#e2e5f0"));

        JLabel dateLbl = new JLabel(getTimeAgo(deck.getCreatedAt()));
        dateLbl.setFont(baseFont.deriveFont(Font.PLAIN, 12f * sf));
        dateLbl.setForeground(Color.decode("#8792a8"));

        metaRow.add(avatarPanel);
        metaRow.add(authorLbl);
        metaRow.add(dotLbl);
        metaRow.add(dateLbl);
        titleBlock.add(metaRow);

        if (deck.getTags() != null && !deck.getTags().isEmpty()) {
            JPanel tagsContainer = new JPanel();
            tagsContainer.setLayout(new BoxLayout(tagsContainer, BoxLayout.Y_AXIS));
            tagsContainer.setOpaque(false);
            tagsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

            final List<String> tags = deck.getTags();
            final int maxVisible = 3;

            Runnable renderTags = new Runnable() {
                boolean expanded = false;

                @Override
                public void run() {
                    tagsContainer.removeAll();

                    JPanel currentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(4), ImageUtil.scale(4)));
                    currentRow.setOpaque(false);
                    currentRow.setAlignmentX(Component.LEFT_ALIGNMENT);

                    int limit = expanded ? Math.min(tags.size(), 6) : Math.min(tags.size(), maxVisible);

                    int tagsInCurrentRow = 0;
                    int charsInCurrentRow = 0;
                    final int MAX_CHARS_PER_ROW = 65;

                    for (int i = 0; i < limit; i++) {
                        String tagText = tags.get(i);

                        if (tagsInCurrentRow > 0 && (tagsInCurrentRow >= 6 || charsInCurrentRow + tagText.length() > MAX_CHARS_PER_ROW)) {
                            tagsContainer.add(currentRow);
                            currentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(4), ImageUtil.scale(4)));
                            currentRow.setOpaque(false);
                            currentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                            tagsInCurrentRow = 0;
                            charsInCurrentRow = 0;
                        }

                        currentRow.add(createTagLabel("#" + tagText, false, baseFont, sf));
                        tagsInCurrentRow++;
                        charsInCurrentRow += tagText.length() + 3;
                    }

                    if (!expanded && tags.size() > maxVisible) {
                        int remaining = Math.min(tags.size(), 6) - maxVisible;
                        String moreText = "+" + remaining + " more";

                        if (tagsInCurrentRow > 0 && (tagsInCurrentRow >= 6 || charsInCurrentRow + moreText.length() > MAX_CHARS_PER_ROW)) {
                            tagsContainer.add(currentRow);
                            currentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, ImageUtil.scale(4), ImageUtil.scale(4)));
                            currentRow.setOpaque(false);
                            currentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                        }

                        JLabel moreLbl = createTagLabel(moreText, true, baseFont, sf);
                        moreLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        moreLbl.setToolTipText("Click to view more tags");

                        moreLbl.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                expanded = true;
                                run();
                                tagsContainer.revalidate();
                                tagsContainer.repaint();
                            }
                        });
                        currentRow.add(moreLbl);
                    }
                    tagsContainer.add(currentRow);
                }
            };

            renderTags.run();

            titleBlock.add(Box.createVerticalStrut(ImageUtil.scale(6)));
            titleBlock.add(tagsContainer);
        }

        JPanel optWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        optWrapper.setOpaque(false);

        JPopupMenu optMenu = buildOptionsMenu(deck, isSavedView, baseFont);
        if (optMenu.getComponentCount() > 0) {
            JButton optBtn = optionsButton(baseFont);
            optBtn.addActionListener(e -> optMenu.show(optBtn, 0, optBtn.getHeight() + ImageUtil.scale(2)));
            optWrapper.add(optBtn);
        }

        headerRow.add(titleBlock, BorderLayout.CENTER);
        headerRow.add(optWrapper, BorderLayout.EAST);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsRow.add(createStatLabel("VIEW", data.views + " views", Color.decode("#64748b"), sf));
        statsRow.add(createStatLabel("SAVE", data.saves + " saves", Color.decode("#64748b"), sf));
        statsRow.add(createStatLabel("CARD", total + " cards", Color.decode("#64748b"), sf));

        JPanel progressRow = new JPanel();
        progressRow.setLayout(new BoxLayout(progressRow, BoxLayout.Y_AXIS));
        progressRow.setOpaque(false);
        progressRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel progLabels = new JPanel(new BorderLayout());
        progLabels.setOpaque(false);
        progLabels.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(20)));
        JLabel progText = new JLabel(data.answered + " / " + total + " answered");

        Color progColor = (pct == 100) ? Color.decode("#16a34a") : Color.decode("#3b5bdb");
        Color gradStart = (pct == 100) ? Color.decode("#22c55e") : Color.decode("#3b5bdb");
        Color gradEnd = (pct == 100) ? Color.decode("#4ade80") : Color.decode("#748ffc");

        progText.setFont(baseFont.deriveFont(Font.BOLD, 11f * sf));
        progText.setForeground(progColor);

        JLabel progPct = new JLabel(pct + "%");
        progPct.setFont(baseFont.deriveFont(Font.BOLD, 11f * sf));
        progPct.setForeground(Color.decode("#8792a8"));

        progLabels.add(progText, BorderLayout.WEST);
        progLabels.add(progPct, BorderLayout.EAST);

        JPanel progressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#e8eaf5"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                if (pct > 0) {
                    int w = (int) (getWidth() * (pct / 100.0));
                    g2.setPaint(new GradientPaint(0, 0, gradStart, w, 0, gradEnd));
                    g2.fillRoundRect(0, 0, w, getHeight(), getHeight(), getHeight());
                }
                g2.dispose();
            }
        };
        progressBar.setOpaque(false);
        progressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(7)));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(7)));

        progressRow.add(progLabels);
        progressRow.add(Box.createVerticalStrut(ImageUtil.scale(7)));
        progressRow.add(progressBar);

        JPanel actionRow = new JPanel(new BorderLayout(ImageUtil.scale(11), 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (user.isAdmin()) {
            JLabel adminInfo = new JLabel("Admin Override Access Active");
            adminInfo.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
            adminInfo.setForeground(Color.decode("#e03131"));
            actionRow.add(adminInfo, BorderLayout.CENTER);
        } else {
            String btnText = (pct == 100 && total > 0) ? "COMPLETED" : "PRACTICE";
            Color btnColor = (pct == 100 && total > 0) ? Color.decode("#16a34a") : Color.decode("#3b82f6");

            JButton practiceBtn = primaryButton(btnText, baseFont, btnColor);

            if (deck.isDisabled()) {
                practiceBtn.setEnabled(false);
                practiceBtn.setToolTipText("This deck has been disabled by a moderator.");
            } else {
                practiceBtn.addActionListener(e -> mainPanel.startQuiz(deck));
            }

            actionRow.add(practiceBtn, BorderLayout.CENTER);

            if (isSavedView) {
                final boolean[] savedState = {true};
                JButton saveBtn = saveButton(savedState, isOwnDeck);

                if (!isOwnDeck) {
                    saveBtn.addActionListener(e -> {
                        if (savedState[0]) {
                            WarningDialog dialog = new WarningDialog(SwingUtilities.getWindowAncestor(DecksPanel.this),
                                    "Unsave Deck",
                                    "Are you sure you want to remove this deck from your saved decks?",
                                    "Unsave");
                            dialog.setVisible(true);

                            if (!dialog.isApproved()) {
                                return;
                            }
                        }

                        savedState[0] = !savedState[0];
                        saveBtn.repaint();

                        new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() {
                                deckDAO.unsaveDeck(user.getId(), deck.getId());
                                return null;
                            }

                            @Override
                            protected void done() {
                                mainPanel.invalidateDeckCaches();
                                if (currentDisplayedDecks != null) currentDisplayedDecks.removeIf(d -> d.deck.getId() == deck.getId());
                                if (cachedSavedDecks != null) cachedSavedDecks.removeIf(d -> d.deck.getId() == deck.getId());
                                executeOptimisticUpdate();
                            }
                        }.execute();
                    });
                }
                actionRow.add(saveBtn, BorderLayout.EAST);
            }
        }

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        topSection.add(headerRow);
        topSection.add(Box.createVerticalStrut(ImageUtil.scale(10)));
        topSection.add(createDivider());
        topSection.add(Box.createVerticalStrut(ImageUtil.scale(10)));
        topSection.add(statsRow);
        topSection.add(Box.createVerticalStrut(ImageUtil.scale(12)));

        JPanel bottomSection = new JPanel();
        bottomSection.setLayout(new BoxLayout(bottomSection, BoxLayout.Y_AXIS));
        bottomSection.setOpaque(false);

        bottomSection.add(createDivider());
        bottomSection.add(Box.createVerticalStrut(ImageUtil.scale(10)));
        bottomSection.add(progressRow);
        bottomSection.add(Box.createVerticalStrut(ImageUtil.scale(12)));
        bottomSection.add(actionRow);

        card.add(topSection, BorderLayout.NORTH);
        card.add(bottomSection, BorderLayout.SOUTH);

        MouseAdapter hoverAdapter = new MouseAdapter() {
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

        return card;
    }

    private JButton primaryButton(String text, Font baseFont, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 13f * ImageUtil.getScaleFactor()));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(6), ImageUtil.scale(13), ImageUtil.scale(6), ImageUtil.scale(13)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setPreferredSize(new Dimension(0, ImageUtil.scale(40)));
        return btn;
    }

    private JButton saveButton(boolean[] savedState, boolean isOwnDeck) {
        JButton btn = new JButton() {
            boolean hov = false;

            {
                if (!isOwnDeck) {
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            hov = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            hov = false;
                            repaint();
                        }
                    });
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isSaved = savedState[0];
                int rad = ImageUtil.scale(8);

                if (isOwnDeck) {
                    g2.setColor(Color.decode("#e2e8f0"));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
                } else if (isSaved || hov) {
                    g2.setColor(Color.decode("#ebfbee"));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
                }

                g2.setColor(isOwnDeck ? Color.decode("#94a3b8") : (isSaved || hov ? Color.decode("#b2f2bb") : Color.decode("#e2e5f0")));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(1))));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, rad, rad);

                String iconType = isSaved ? "SAVED" : (hov && !isOwnDeck ? "SAVED" : "SAVE");
                Color iconColor = isSaved || hov ? Color.decode("#2f9e44") : Color.decode("#8792a8");
                if (isOwnDeck) iconColor = Color.decode("#94a3b8");

                Icon icon = IconUtil.getIcon(iconType, iconColor, ImageUtil.scale(24));
                int x = (getWidth() - icon.getIconWidth()) / 2;
                int y = (getHeight() - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, x, y);

                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(ImageUtil.scale(40), ImageUtil.scale(40)));

        if (!isOwnDeck) {
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(savedState[0] ? "Unsave Deck" : "Save Deck");
        } else {
            btn.setToolTipText("Cannot save your own deck");
        }

        return btn;
    }

    private JButton optionsButton(Font baseFont) {
        JButton btn = new JButton("•••") {
            boolean hov = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int rad = ImageUtil.scale(6);

                g2.setColor(hov ? Color.decode("#eef0ff") : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
                g2.setColor(hov ? Color.decode("#c5cfff") : Color.decode("#e2e5f0"));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(1))));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, rad, rad);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 13f * ImageUtil.getScaleFactor()));
        btn.setForeground(Color.decode("#8792a8"));
        btn.setPreferredSize(new Dimension(ImageUtil.scale(32), ImageUtil.scale(28)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPopupMenu buildOptionsMenu(Deck deck, boolean isSavedView, Font baseFont) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#e2e5f0"), Math.max(1, ImageUtil.scale(2))),
                BorderFactory.createEmptyBorder(ImageUtil.scale(4), ImageUtil.scale(4), ImageUtil.scale(4), ImageUtil.scale(4))
        ));

        if (user.isAdmin()) {
            JMenuItem edit = menuItem("Force Edit Cards", Color.decode("#1a1f36"), false, baseFont);
            JMenuItem delete = menuItem("Force Delete", Color.decode("#e03131"), true, baseFont);
            edit.addActionListener(e -> mainPanel.showCardEditor(deck));
            delete.addActionListener(e -> deleteDeck(deck));
            menu.add(edit);
            menu.addSeparator();
            menu.add(delete);
            return menu;
        }

        boolean hasItems = false;
        if (!isSavedView) {
            JMenuItem edit = menuItem("Edit Cards", Color.decode("#1a1f36"), false, baseFont);
            JMenuItem rename = menuItem("Rename", Color.decode("#1a1f36"), false, baseFont);
            JMenuItem updateTags = menuItem("Update Tags", Color.decode("#1a1f36"), false, baseFont);

            edit.addActionListener(e -> mainPanel.showCardEditor(deck));
            rename.addActionListener(e -> renameDeck(deck));
            updateTags.addActionListener(e -> updateTags(deck));

            menu.add(edit);
            menu.add(rename);
            menu.add(updateTags);
            hasItems = true;
        }

        JMenuItem leaderboard = menuItem("View Leaderboard", Color.decode("#1a1f36"), false, baseFont);
        leaderboard.addActionListener(e -> mainPanel.showLeaderboard(deck));
        menu.add(leaderboard);
        hasItems = true;

        if (deck.getFileId() > 0) {
            if (hasItems) menu.addSeparator();
            JMenuItem download = menuItem("Download Source File", Color.decode("#3b82f6"), false, baseFont);
            download.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Save File");
                fc.setSelectedFile(new File(deck.getSourceFileName()));

                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File target = fc.getSelectedFile();
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            FileDAO fd = new FileDAO();
                            byte[] fileData = fd.getFileData(deck.getFileId());
                            if (fileData != null) {
                                Files.write(target.toPath(), fileData);
                            } else {
                                throw new RuntimeException("File data not found in database.");
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                                new SuccessDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Success", "File downloaded successfully!").setVisible(true);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                new InfoDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Error", "Error saving file. " + ex.getMessage()).setVisible(true);
                            }
                        }
                    }.execute();
                }
            });
            menu.add(download);
            hasItems = true;
        }

        if (!isSavedView && !deck.isDisabled()) {
            if (hasItems) menu.addSeparator();
            JMenuItem togglePublic = menuItem(deck.isPublic() ? "Make Private" : "Make Public", Color.decode("#3b5bdb"), false, baseFont);
            togglePublic.addActionListener(e -> togglePublic(deck, !deck.isPublic()));
            menu.add(togglePublic);
            hasItems = true;
        }

        boolean needsDestructiveSeparator = true;

        if (isSavedView) {
            if (deck.getUserId() != user.getId() && !user.isModerator()) {
                if (hasItems && needsDestructiveSeparator) {
                    menu.addSeparator();
                    needsDestructiveSeparator = false;
                }
                JMenuItem report = menuItem("Report Deck", Color.decode("#e03131"), true, baseFont);
                report.addActionListener(e -> {
                    ReportDAO rDao = new ReportDAO();
                    Window parentWindow = SwingUtilities.getWindowAncestor(DecksPanel.this);
                    if (rDao.hasUserReported(user.getId(), deck.getId())) {
                        new InfoDialog(parentWindow, "Already Reported", "You have already reported this deck.").setVisible(true);
                        return;
                    }

                    ReportDialog dialog = new ReportDialog(parentWindow, "Report Deck");
                    dialog.setVisible(true);

                    if (dialog.isApproved()) {
                        String reason = dialog.getReportReason();
                        if (!reason.isEmpty()) {
                            rDao.submitReport(deck.getId(), user.getId(), reason);
                            new SuccessDialog(parentWindow, "Report Sent", "Report submitted successfully. A moderator will review it.").setVisible(true);
                        }
                    }
                });
                menu.add(report);
                hasItems = true;
            }
        } else {
            if (hasItems && needsDestructiveSeparator) {
                menu.addSeparator();
                needsDestructiveSeparator = false;
            }
            JMenuItem delete = menuItem("Delete Deck", Color.decode("#e03131"), true, baseFont);
            delete.addActionListener(e -> deleteDeck(deck));
            menu.add(delete);
            hasItems = true;
        }

        return menu;
    }

    private JMenuItem menuItem(String text, Color fg, boolean danger, Font baseFont) {
        JMenuItem it = new JMenuItem(text);
        it.setFont(baseFont.deriveFont(Font.BOLD, 13f * ImageUtil.getScaleFactor()));
        it.setForeground(fg);
        it.setBackground(Color.WHITE);
        it.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(8), ImageUtil.scale(12), ImageUtil.scale(8), ImageUtil.scale(12)));
        it.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        it.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                it.setBackground(danger ? Color.decode("#ffe3e3") : Color.decode("#f0f2f8"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                it.setBackground(Color.WHITE);
            }
        });
        return it;
    }

    private void togglePublic(Deck deck, boolean willBePublic) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);

        if (deck.isDisabled()) {
            new InfoDialog(parentWindow, "Disabled Deck", "This deck has been disabled by a moderator.\nYou cannot make it public.").setVisible(true);
            return;
        }

        if (willBePublic && deck.getCardCount() == 0) {
            new InfoDialog(parentWindow, "Empty Deck", "You cannot publish an empty deck. Please add cards first.").setVisible(true);
            return;
        }
        if (willBePublic && deckDAO.isPublicTitleTaken(deck.getTitle(), deck.getId())) {
            new InfoDialog(parentWindow, "Name Conflict", "A public deck with the name '" + deck.getTitle() + "' already exists.\nPlease rename your deck before publishing.").setVisible(true);
            return;
        }

        String actionTxt = willBePublic ? "make this deck public?" : "make this deck private?";

        WarningDialog dialog = new WarningDialog(parentWindow,
                "Confirm Visibility",
                "Are you sure you want to " + actionTxt,
                "Confirm");
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            deck.setPublic(willBePublic);
            executeOptimisticUpdate();

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    deckDAO.updateDeck(deck.getId(), deck.getTitle(), deck.getDescription(), willBePublic);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        mainPanel.invalidateDeckCaches();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        deck.setPublic(!willBePublic); // Fallback
                        executeOptimisticUpdate();
                        new InfoDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Error", "Failed to update visibility.").setVisible(true);
                    }
                }
            }.execute();

            new SuccessDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Success", willBePublic ? "Deck published successfully!" : "Deck is now private!").setVisible(true);
        }
    }

    private void createNewDeck() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        AddDeckDialog dialog = new AddDeckDialog(parentWindow);
        dialog.setVisible(true);

        if (!dialog.isApproved()) {
            return;
        }

        String name = dialog.getDeckName();
        String tagsInput = dialog.getTags();

        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Deck name cannot be empty.", "Invalid Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (name.length() > 45) {
            JOptionPane.showMessageDialog(this,
                    "Deck name cannot exceed 45 characters.\nPlease choose a shorter name.",
                    "Name Too Long",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (deckDAO.isUserTitleTaken(user.getId(), name, -1)) {
            JOptionPane.showMessageDialog(this, "You already have a deck with this name.", "Name Conflict", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final List<String> tagList = new ArrayList<>();
        if (!tagsInput.isBlank()) {
            String[] splitTags = tagsInput.split(",");
            for (String t : splitTags) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    if (trimmed.length() > 30) {
                        JOptionPane.showMessageDialog(this,
                                "Tags cannot exceed 30 characters.\n'" + trimmed + "' is too long.",
                                "Tag Too Long",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (!tagList.contains(trimmed)) {
                        tagList.add(trimmed);
                    }
                }
            }

            if (tagList.size() > 6) {
                JOptionPane.showMessageDialog(this,
                        "You can only add up to 6 tags. You entered " + tagList.size() + ".",
                        "Too Many Tags",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                int newDeckId = deckDAO.createDeck(user.getId(), null, name, "", false);
                if (newDeckId > 0 && !tagList.isEmpty()) {
                    deckDAO.updateDeckTags(newDeckId, tagList);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    mainPanel.invalidateDeckCaches();
                    refresh();
                    new SuccessDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Success", "Deck created successfully!").setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(DecksPanel.this, "Failed to create deck. " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renameDeck(Deck deck) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        InputDialog dialog = new InputDialog(parentWindow, "Rename", "New name:", deck.getTitle());
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            String inputName = dialog.getInputText();
            if (!inputName.isBlank()) {
                final String name = inputName.trim();

                if (name.length() > 45) {
                    new InfoDialog(parentWindow, "Name Too Long", "Deck name cannot exceed 45 characters.\nPlease choose a shorter name.").setVisible(true);
                    return;
                }

                if (!name.equalsIgnoreCase(deck.getTitle())) {
                    if (deck.isPublic() && deckDAO.isPublicTitleTaken(name, deck.getId())) {
                        new InfoDialog(parentWindow, "Conflict", "A public deck with this name already exists.").setVisible(true);
                        return;
                    }
                    if (deckDAO.isUserTitleTaken(user.getId(), name, deck.getId())) {
                        new InfoDialog(parentWindow, "Conflict", "You already have a deck with this name.").setVisible(true);
                        return;
                    }
                }

                String oldTitle = deck.getTitle();
                deck.setTitle(name);
                executeOptimisticUpdate();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        deckDAO.updateDeck(deck.getId(), name, deck.getDescription(), deck.isPublic());
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            mainPanel.invalidateDeckCaches();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            deck.setTitle(oldTitle);
                            executeOptimisticUpdate();
                            new InfoDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Error", "Failed to rename deck.").setVisible(true);
                        }
                    }
                }.execute();

                new SuccessDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Success", "Deck renamed successfully!").setVisible(true);
            }
        }
    }

    private void updateTags(Deck deck) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        String currentTags = deck.getTags() != null ? String.join(", ", deck.getTags()) : "";

        InputDialog dialog = new InputDialog(parentWindow, "Update Tags", "Update tags (comma separated):", currentTags);
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            String tagsInput = dialog.getInputText();
            final List<String> tagList = new ArrayList<>();
            if (!tagsInput.isBlank()) {
                String[] splitTags = tagsInput.split(",");
                for (String t : splitTags) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) {
                        if (trimmed.length() > 30) {
                            new InfoDialog(parentWindow, "Tag Too Long", "Tags cannot exceed 30 characters.\n'" + trimmed + "' is too long.").setVisible(true);
                            return;
                        }
                        if (!tagList.contains(trimmed)) {
                            tagList.add(trimmed);
                        }
                    }
                }

                if (tagList.size() > 6) {
                    new InfoDialog(parentWindow, "Too Many Tags", "You can only add up to 6 tags. You entered " + tagList.size() + ".").setVisible(true);
                    return;
                }
            }

            List<String> oldTags = deck.getTags();
            deck.setTags(tagList);
            executeOptimisticUpdate();

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    deckDAO.updateDeckTags(deck.getId(), tagList);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        mainPanel.invalidateDeckCaches();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        deck.setTags(oldTags);
                        executeOptimisticUpdate();
                        new InfoDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Error", "Failed to update tags.").setVisible(true);
                    }
                }
            }.execute();

            new SuccessDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Success", "Tags updated successfully!").setVisible(true);
        }
    }

    private void deleteDeck(Deck deck) {
        WarningDialog dialog = new WarningDialog(SwingUtilities.getWindowAncestor(this),
                "Confirm Delete",
                "Are you sure you want to permanently delete this deck?\nThis action cannot be undone.",
                "Delete");
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            if (currentDisplayedDecks != null) currentDisplayedDecks.removeIf(d -> d.deck.getId() == deck.getId());
            if (cachedMyDecks != null) cachedMyDecks.removeIf(d -> d.deck.getId() == deck.getId());
            if (cachedSavedDecks != null) cachedSavedDecks.removeIf(d -> d.deck.getId() == deck.getId());
            executeOptimisticUpdate();

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    int fileId = deck.getFileId();
                    boolean deleted = deckDAO.deleteDeck(deck.getId());
                    if (!deleted) {
                        throw new RuntimeException("Failed to delete deck. Constraint blocking operation.");
                    }
                    if (fileId > 0) {
                        try {
                            new FileDAO().deleteFile(fileId);
                        } catch (Exception ignored) {
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        mainPanel.invalidateDeckCaches();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        refresh();
                        new InfoDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Error", "Failed to delete deck. Database conflict.").setVisible(true);
                    }
                }
            }.execute();

            new SuccessDialog(SwingUtilities.getWindowAncestor(DecksPanel.this), "Deleted", "Deck deleted successfully!").setVisible(true);
        }
    }

    public void refresh() {
        clearCache();

        if (searchField != null) {
            isUpdatingPlaceholder = true;
            searchField.setText("Search by deck title or tags...");
            searchField.setForeground(Color.decode("#8792a8"));
            isUpdatingPlaceholder = false;
        }
        SwingUtilities.invokeLater(this::loadDecks);
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setOpaque(false);
        root.add(panel1, BorderLayout.NORTH);
        topBarPanel = new JPanel();
        topBarPanel.setLayout(new GridBagLayout());
        topBarPanel.setPreferredSize(new Dimension(-1, 65));
        panel1.add(topBarPanel, BorderLayout.NORTH);
        topBarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        searchPanel = new JPanel();
        searchPanel.setLayout(new GridBagLayout());
        searchPanel.setPreferredSize(new Dimension(-1, 50));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 16);
        topBarPanel.add(searchPanel, gbc);
        searchLabel = new JLabel();
        searchLabel.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 6);
        searchPanel.add(searchLabel, gbc);
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(74, 48));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        searchPanel.add(searchField, gbc);
        sortComboBox = new JComboBox();
        sortComboBox.setPreferredSize(new Dimension(162, 50));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 8);
        topBarPanel.add(sortComboBox, gbc);
        refreshButton = new JButton();
        refreshButton.setPreferredSize(new Dimension(50, 50));
        refreshButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 12);
        topBarPanel.add(refreshButton, gbc);
        addDeckBtn = new JButton();
        addDeckBtn.setPreferredSize(new Dimension(130, 50));
        addDeckBtn.setText("ADD DECK");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        topBarPanel.add(addDeckBtn, gbc);
        tabPanel = new JPanel();
        tabPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        tabPanel.setOpaque(false);
        panel1.add(tabPanel, BorderLayout.SOUTH);
        tabPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        myDecksTab = new JButton();
        myDecksTab.setText("My Decks");
        tabPanel.add(myDecksTab);
        savedDecksTab = new JButton();
        savedDecksTab.setText("Saved Decks");
        tabPanel.add(savedDecksTab);
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, BorderLayout.CENTER);
        gridWrapperPanel = new JPanel();
        gridWrapperPanel.setLayout(new BorderLayout(0, 0));
        scrollPane1.setViewportView(gridWrapperPanel);
        gridWrapperPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        deckGridPanel = new JPanel();
        deckGridPanel.setLayout(new GridBagLayout());
        gridWrapperPanel.add(deckGridPanel, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}