package com.flipit.ui.panels;

import com.flipit.dao.CardDAO;
import com.flipit.dao.CardProgressDAO;
import com.flipit.dao.UserDAO;
import com.flipit.models.Card;
import com.flipit.models.Deck;
import com.flipit.models.User;
import com.flipit.ui.AppFrame;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.panels.admin.AdminDashboardPanel;
import com.flipit.ui.panels.admin.AdminUsersPanel;
import com.flipit.ui.panels.moderator.ReportsPanel;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainPanel extends JPanel {
    private JPanel root;
    private JPanel contentPanel;
    private JPanel navbarPanel;
    private JPanel logoPanel;
    private JLabel logoLabel;
    private JPanel linksPanel;
    private JPanel profilePanel;
    private JLabel usernameLabel;
    private JButton AdminDashBtn;
    private JButton AdminUsersBtn;
    private JButton ModReportsBtn;
    private JButton HomeBtn;
    private JButton BrowseBtn;
    private JButton DecksBtn;
    private JButton UploadBtn;
    private JButton FilesBtn;
    private JButton avatarBtn;

    private AdminDashboardPanel adminDashboardPanel;
    private AdminUsersPanel adminUsersPanel;
    private ReportsPanel reportsPanel;

    private HomePanel homePanel;
    private BrowsePanel browsePanel;
    private UploadPanel uploadPanel;
    private FilesPanel filesPanel;
    private DecksPanel decksPanel;
    private SettingsPanel settingsPanel;

    private final CardLayout innerCards = new CardLayout();
    private AppFrame appFrame;
    private User user;

    private Image cachedAvatar = null;
    public static final String ADMIN_DASH = "ADMIN_DASH";
    public static final String ADMIN_USERS = "ADMIN_USERS";
    public static final String MOD_REPORTS = "MOD_REPORTS";
    public static final String HOME = "HOME";
    public static final String UPLOAD = "UPLOAD";
    public static final String BROWSE = "BROWSE";
    public static final String FILES = "FILES";
    public static final String DECKS = "DECKS";
    public static final String QUIZ_RESULTS = "QUIZ_RESULTS";

    private JButton activeNav = null;
    private Deck activeDeck = null;
    private QuizSession activeSession = null;
    private QuizPanel activeQuizPanel = null;

    private String currentScreenStr = HOME;
    private String preEditorScreen = DECKS;
    private JButton preEditorNav = null;

    private String preQuizScreen = DECKS;
    private JButton preQuizNav = null;

    public void clearActiveSession() {
        this.activeDeck = null;
        this.activeSession = null;
        this.activeQuizPanel = null;
    }

    public void syncActiveSession() {
        if (activeSession == null || activeDeck == null) return;

        new SwingWorker<QuizSession, Void>() {
            @Override
            protected QuizSession doInBackground() {
                CardDAO cardDAO = new CardDAO();
                CardProgressDAO progressDAO = new CardProgressDAO();

                List<Card> dbCards = cardDAO.getAllCardsByDeck(activeDeck.getId());
                Map<Integer, Card> dbCardMap = new HashMap<>();
                for (Card c : dbCards) dbCardMap.put(c.getId(), c);

                List<Card> syncedCards = new ArrayList<>();
                for (Card sc : activeSession.cards) {
                    if (dbCardMap.containsKey(sc.getId()))
                        syncedCards.add(dbCardMap.get(sc.getId()));
                }
                for (Card dbCard : dbCards) {
                    boolean exists = activeSession.cards.stream()
                            .anyMatch(sc -> sc.getId() == dbCard.getId());
                    if (!exists) syncedCards.add(dbCard);
                }

                int newScore = progressDAO.getCorrectCount(
                        user.getId(), activeDeck.getId());

                QuizSession updated = new QuizSession(
                        syncedCards, syncedCards.size(), newScore,
                        Math.min(activeSession.currentIndex,
                                Math.max(0, syncedCards.size() - 1))
                );
                updated.sessionAnswers.putAll(activeSession.sessionAnswers);
                return updated;
            }

            @Override
            protected void done() {
                try {
                    QuizSession updated = get();
                    activeSession = updated;

                    if (activeQuizPanel != null && updated.totalDeckCards > 0) {
                        for (Component c : contentPanel.getComponents())
                            if (c instanceof QuizPanel) contentPanel.remove(c);
                        activeQuizPanel = new QuizPanel(
                                MainPanel.this, user, activeDeck, activeSession);
                        contentPanel.add(activeQuizPanel, "QUIZ_SCREEN");
                    } else if (updated.totalDeckCards == 0) {
                        clearActiveSession();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    public MainPanel() {
    }

    public MainPanel(AppFrame appFrame, User user) {
        this.appFrame = appFrame;
        this.user = user;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleNavbar();
        styleNavButtons();
        styleAvatar();

        contentPanel.setLayout(innerCards);

        if (user.isAdmin()) {
            adminDashboardPanel = new AdminDashboardPanel(this, user);
            adminUsersPanel = new AdminUsersPanel(this, user);
            browsePanel = new BrowsePanel(this, user);

            contentPanel.add(adminDashboardPanel, ADMIN_DASH);
            contentPanel.add(adminUsersPanel, ADMIN_USERS);
            contentPanel.add(browsePanel, BROWSE);
        }

        if (user.isModerator()) {
            reportsPanel = new ReportsPanel(this, user);
            contentPanel.add(reportsPanel, MOD_REPORTS);
        }

        if (!user.isAdmin()) {
            homePanel = new HomePanel(this, user);
            uploadPanel = new UploadPanel(this, user);
            browsePanel = new BrowsePanel(this, user);
            filesPanel = new FilesPanel(this, user);
            decksPanel = new DecksPanel(this, user);

            contentPanel.add(homePanel, HOME);
            contentPanel.add(uploadPanel, UPLOAD);
            contentPanel.add(browsePanel, BROWSE);
            contentPanel.add(filesPanel, FILES);
            contentPanel.add(decksPanel, DECKS);
        }

        settingsPanel = new SettingsPanel(this, user);
        contentPanel.add(settingsPanel, "SETTINGS");

        AdminDashBtn.addActionListener(e -> {
            setActiveNav(AdminDashBtn);
            showScreen(ADMIN_DASH);
            if (adminDashboardPanel != null) adminDashboardPanel.refresh();
        });
        AdminUsersBtn.addActionListener(e -> {
            setActiveNav(AdminUsersBtn);
            showScreen(ADMIN_USERS);
            if (adminUsersPanel != null) adminUsersPanel.refresh();
        });
        if (ModReportsBtn != null) {
            ModReportsBtn.addActionListener(e -> {
                setActiveNav(ModReportsBtn);
                showScreen(MOD_REPORTS);
                if (reportsPanel != null) reportsPanel.refresh();
            });
        }

        HomeBtn.addActionListener(e -> {
            setActiveNav(HomeBtn);
            showHome();
        });
        BrowseBtn.addActionListener(e -> {
            setActiveNav(BrowseBtn);
            showBrowse();
        });
        DecksBtn.addActionListener(e -> {
            setActiveNav(DecksBtn);
            showDecks();
        });
        UploadBtn.addActionListener(e -> {
            setActiveNav(UploadBtn);
            showUpload();
        });
        FilesBtn.addActionListener(e -> {
            setActiveNav(FilesBtn);
            showFiles();
        });

        avatarBtn.addActionListener(e -> showProfileMenu());

        if (user.isAdmin()) {
            HomeBtn.setVisible(false);
            BrowseBtn.setVisible(true);
            DecksBtn.setVisible(false);
            UploadBtn.setVisible(false);
            FilesBtn.setVisible(false);

            AdminDashBtn.setVisible(true);
            AdminUsersBtn.setVisible(true);
            if (ModReportsBtn != null) ModReportsBtn.setVisible(true);

            setActiveNav(AdminDashBtn);
            showScreen(ADMIN_DASH);
        } else if (user.isModerator()) {
            AdminDashBtn.setVisible(false);
            AdminUsersBtn.setVisible(false);
            if (ModReportsBtn != null) ModReportsBtn.setVisible(true);

            HomeBtn.setVisible(true);
            BrowseBtn.setVisible(true);
            DecksBtn.setVisible(true);
            UploadBtn.setVisible(true);
            FilesBtn.setVisible(true);

            setActiveNav(HomeBtn);
            showScreen(HOME);
        } else {
            AdminDashBtn.setVisible(false);
            AdminUsersBtn.setVisible(false);
            if (ModReportsBtn != null) ModReportsBtn.setVisible(false);

            HomeBtn.setVisible(true);
            BrowseBtn.setVisible(true);
            DecksBtn.setVisible(true);
            UploadBtn.setVisible(true);
            FilesBtn.setVisible(true);

            setActiveNav(HomeBtn);
            showScreen(HOME);
        }

        reloadAvatar();
    }

    public void reloadAvatar() {
        SwingWorker<byte[], Void> avatarWorker = new SwingWorker<>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                UserDAO dao = new UserDAO();
                return dao.getProfilePicture(user.getId());
            }

            @Override
            protected void done() {
                try {
                    byte[] bytes = get();
                    cachedAvatar = ImageUtil.getImageFromBytes(bytes);
                    if (avatarBtn != null) avatarBtn.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        avatarWorker.execute();
    }

    public void updateUsernameDisplay() {
        if (usernameLabel != null && user != null) {
            usernameLabel.setText(user.getUsername());
            usernameLabel.revalidate();
            usernameLabel.repaint();
        }
    }

    private void styleNavbar() {
        navbarPanel.setPreferredSize(new Dimension(-1, ImageUtil.scale(56)));
        navbarPanel.setBackground(Color.WHITE);
        navbarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.decode("#e2e8f0")));

        logoPanel.setBackground(Color.WHITE);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(16), 0, 0));

        profilePanel.setBackground(Color.WHITE);
        profilePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, ImageUtil.scale(16)));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        logoLabel.setText("FlipIT");
        logoLabel.setForeground(Color.decode("#2563eb"));

        Font logoFont = UIManager.getFont("logoFont");
        if (logoFont != null) {
            logoLabel.setFont(logoFont.deriveFont(22f * ImageUtil.getScaleFactor()));
        } else {
            logoLabel.setFont(baseFont.deriveFont(Font.BOLD, 22f * ImageUtil.getScaleFactor()));
        }

        usernameLabel.setText(user.getUsername());
        usernameLabel.setFont(baseFont.deriveFont(Font.BOLD, 14f * ImageUtil.getScaleFactor()));
        usernameLabel.setForeground(Color.decode("#0f172a"));

        linksPanel.setBackground(Color.WHITE);
        contentPanel.setBackground(Color.decode("#f8fafc"));
        root.setBackground(Color.decode("#f8fafc"));
    }

    private void styleNavButtons() {
        linksPanel.setLayout(new BoxLayout(linksPanel, BoxLayout.X_AXIS));

        JButton[] buttons = ModReportsBtn != null
                ? new JButton[]{AdminDashBtn, AdminUsersBtn, ModReportsBtn, HomeBtn, BrowseBtn, DecksBtn, UploadBtn, FilesBtn}
                : new JButton[]{AdminDashBtn, AdminUsersBtn, HomeBtn, BrowseBtn, DecksBtn, UploadBtn, FilesBtn};

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        linksPanel.removeAll();

        for (JButton btn : buttons) {
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFont(baseFont.deriveFont(Font.BOLD, 14f * ImageUtil.getScaleFactor()));
            btn.setForeground(Color.decode("#64748b"));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setOpaque(true);
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createEmptyBorder(
                    ImageUtil.scale(8), ImageUtil.scale(12),
                    ImageUtil.scale(8), ImageUtil.scale(12)
            ));

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (btn != activeNav) {
                        btn.setBackground(Color.decode("#f1f5f9"));
                        btn.setForeground(Color.decode("#1e40af"));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (btn != activeNav) {
                        btn.setBackground(Color.WHITE);
                        btn.setForeground(Color.decode("#64748b"));
                    }
                }
            });

            linksPanel.add(btn);
            linksPanel.add(Box.createHorizontalStrut(ImageUtil.scale(4)));
        }
    }

    private void setActiveNav(JButton btn) {
        if (activeNav != null) {
            activeNav.setBackground(Color.WHITE);
            activeNav.setForeground(Color.decode("#64748b"));
        }
        activeNav = btn;
        if (btn != null) {
            btn.setBackground(Color.decode("#dbeafe"));
            btn.setForeground(Color.decode("#1e40af"));
        }
    }

    private void styleAvatar() {
        avatarBtn.setPreferredSize(new Dimension(ImageUtil.scale(40), ImageUtil.scale(40)));
        avatarBtn.setMinimumSize(new Dimension(ImageUtil.scale(40), ImageUtil.scale(40)));
        avatarBtn.setContentAreaFilled(false);
        avatarBtn.setBorderPainted(false);
        avatarBtn.setFocusPainted(false);
        avatarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarBtn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                int size = Math.min(c.getWidth(), c.getHeight()) - 4;
                int x = (c.getWidth() - size) / 2;
                int y = (c.getHeight() - size) / 2;

                ImageUtil.paintAvatar((Graphics2D) g, x, y, size, cachedAvatar, user.getInitials());
            }
        });
    }

    public void showScreen(String name) {
        currentScreenStr = name;
        innerCards.show(contentPanel, name);
    }

    public void showDecks() {
        setActiveNav(DecksBtn);
        showScreen(DECKS);
        decksPanel.refresh();
    }

    public void showUpload() {
        setActiveNav(UploadBtn);
        showScreen(UPLOAD);
        if (uploadPanel != null) uploadPanel.refresh();
    }

    public void showFiles() {
        setActiveNav(FilesBtn);
        showScreen(FILES);
        filesPanel.refresh();
    }

    public void showHome() {
        setActiveNav(HomeBtn);
        showScreen(HOME);
        homePanel.refresh();
    }

    public void showAdminDash() {
        setActiveNav(AdminDashBtn);
        showScreen(ADMIN_DASH);
        if (adminDashboardPanel != null) adminDashboardPanel.refresh();
    }

    public void showBrowse() {
        setActiveNav(BrowseBtn);
        showScreen(BROWSE);
        browsePanel.refresh();
    }

    public void returnFromQuiz() {
        if (preQuizNav != null) {
            setActiveNav(preQuizNav);
        }
        showScreen(preQuizScreen);
        refreshCurrentScreen();
    }

    public void showCardEditor(Deck deck) {
        if (!"EDITOR".equals(currentScreenStr)) {
            preEditorScreen = currentScreenStr;
            preEditorNav = activeNav;
        }

        for (Component c : contentPanel.getComponents()) {
            if (c.getClass().getSimpleName().equals("CardEditorPanel")) {
                contentPanel.remove(c);
            }
        }

        CardEditorPanel editorPanel = new CardEditorPanel(this, user, deck);
        contentPanel.add(editorPanel, "EDITOR");

        currentScreenStr = "EDITOR";
        innerCards.show(contentPanel, "EDITOR");

        if (activeNav != null) {
            activeNav.setBackground(Color.WHITE);
            activeNav.setForeground(Color.decode("#64748b"));
            activeNav = null;
        }
    }

    public void returnFromEditor() {
        if (preEditorNav != null) {
            setActiveNav(preEditorNav);
        }
        showScreen(preEditorScreen);
        refreshCurrentScreen();
    }

    public AppFrame getAppFrame() {
        return appFrame;
    }

    public User getUser() {
        return user;
    }

    public void invalidateDeckCaches() {
        if (browsePanel != null) browsePanel.clearCache();
        if (decksPanel != null) decksPanel.clearCache();
    }

    public void refreshCurrentScreen() {
        switch (currentScreenStr) {
            case HOME:
                if (homePanel != null) homePanel.refresh();
                break;
            case UPLOAD:
                if (uploadPanel != null) uploadPanel.refresh();
                break;
            case DECKS:
                if (decksPanel != null) decksPanel.refresh();
                break;
            case BROWSE:
                if (browsePanel != null) browsePanel.refresh();
                break;
            case ADMIN_DASH:
                if (adminDashboardPanel != null) adminDashboardPanel.refresh();
                break;
            case ADMIN_USERS:
                if (adminUsersPanel != null) adminUsersPanel.refresh();
                break;
            case MOD_REPORTS:
                if (reportsPanel != null) reportsPanel.refresh();
                break;
        }
        if (avatarBtn != null && user != null) {
            reloadAvatar();
        }
    }

    private void showProfileMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 2),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        JPanel info = new JPanel(new GridLayout(1, 1, 0, 2));
        info.setBackground(Color.WHITE);
        info.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel nameL = new JLabel(user.getUsername());
        nameL.setFont(baseFont.deriveFont(Font.BOLD, 14f));
        nameL.setForeground(Color.decode("#0f172a"));

        info.add(nameL);
        menu.add(info);
        menu.addSeparator();

        JMenuItem settings = styledMenuItem("Settings", false, baseFont);
        settings.addActionListener(e -> {
            if (settingsPanel != null) settingsPanel.refresh();
            showScreen("SETTINGS");
        });

        JMenuItem logout = styledMenuItem("Sign Out", true, baseFont);
        logout.addActionListener(e -> appFrame.logout());

        menu.add(settings);
        menu.addSeparator();
        menu.add(logout);

        menu.show(avatarBtn, 0, avatarBtn.getHeight() + 4);
    }

    private JMenuItem styledMenuItem(String text, boolean danger, Font baseFont) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(baseFont.deriveFont(Font.BOLD, 13f));
        item.setForeground(danger ? Color.decode("#ef4444") : Color.decode("#475569"));
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(danger ? Color.decode("#fee2e2") : Color.decode("#f1f5f9"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(Color.WHITE);
            }
        });
        return item;
    }

    public static class QuizSession {
        public List<Card> cards;
        public int currentIndex = 0, score = 0, totalDeckCards;
        public Map<Integer, String> sessionAnswers = new HashMap<>();

        public QuizSession(List<Card> cards, int totalDeckCards, int initialScore, int startIndex) {
            this.cards = cards;
            this.totalDeckCards = totalDeckCards;
            this.score = initialScore;
            this.currentIndex = startIndex;
        }
    }

    public void showQuizResults(Deck deck, int finalScore, int totalCards) {
        for (Component c : contentPanel.getComponents()) if (c instanceof QuizResultsPanel) contentPanel.remove(c);
        QuizResultsPanel resultsPanel = new QuizResultsPanel(this, deck, finalScore, totalCards);
        contentPanel.add(resultsPanel, QUIZ_RESULTS);
        contentPanel.revalidate();
        contentPanel.repaint();
        showScreen(QUIZ_RESULTS);
    }

    public void showLeaderboard(Deck deck) {
        String prevScreen = currentScreenStr;

        for (Component c : contentPanel.getComponents()) {
            if (c instanceof LeaderboardPanel) contentPanel.remove(c);
        }

        LeaderboardPanel lbPanel = new LeaderboardPanel(this, deck, prevScreen);
        contentPanel.add(lbPanel, "LEADERBOARD");
        contentPanel.revalidate();
        contentPanel.repaint();
        showScreen("LEADERBOARD");
    }

    public void resumeQuizForReview() {
        if (activeQuizPanel != null) {
            showScreen("QUIZ_SCREEN");
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }

    public void startQuiz(Deck deck) {
        if (!"QUIZ_SCREEN".equals(currentScreenStr) && !QUIZ_RESULTS.equals(currentScreenStr)) {
            preQuizScreen = currentScreenStr;
            preQuizNav = activeNav;
        }

        if (activeDeck != null && activeDeck.getId() == deck.getId() && activeQuizPanel != null) {
            CardProgressDAO progressDAO = new CardProgressDAO();
            int answered = progressDAO.getAnsweredCount(user.getId(), deck.getId());
            if (answered >= activeSession.totalDeckCards)
                showQuizResults(deck, activeSession.score, activeSession.totalDeckCards);
            else {
                showScreen("QUIZ_SCREEN");
                contentPanel.revalidate();
                contentPanel.repaint();
            }
            if (activeNav != null) {
                activeNav.setBackground(Color.WHITE);
                activeNav.setForeground(Color.decode("#64748b"));
                activeNav = null;
            }
            return;
        }

        if (activeNav != null) {
            activeNav.setBackground(Color.WHITE);
            activeNav.setForeground(Color.decode("#64748b"));
            activeNav = null;
        }

        new SwingWorker<QuizSession, Void>() {
            List<Card> allCards;
            int initialScore, answered;

            @Override
            protected QuizSession doInBackground() {
                CardDAO cardDAO = new CardDAO();
                CardProgressDAO progressDAO = new CardProgressDAO();

                allCards = cardDAO.getAllCardsByDeck(deck.getId());
                if (allCards.isEmpty()) return null;

                List<Integer> cardIds = allCards.stream()
                        .map(Card::getId).collect(Collectors.toList());
                Map<Integer, String> answers =
                        progressDAO.getSelectedAnswersBatch(user.getId(), cardIds);

                List<Card> answeredCards = new ArrayList<>(), unansweredCards = new ArrayList<>();
                for (Card c : allCards) {
                    if (answers.containsKey(c.getId())) answeredCards.add(c);
                    else unansweredCards.add(c);
                }

                answered = answeredCards.size();
                initialScore = progressDAO.getCorrectCount(user.getId(), deck.getId());

                int startIndex = answeredCards.size();
                if (startIndex == 0) Collections.shuffle(unansweredCards);
                else if (startIndex >= allCards.size()) startIndex = allCards.size() - 1;

                List<Card> sessionCards = new ArrayList<>();
                sessionCards.addAll(answeredCards);
                sessionCards.addAll(unansweredCards);

                QuizSession s = new QuizSession(sessionCards, allCards.size(), initialScore, startIndex);
                s.sessionAnswers.putAll(answers);
                return s;
            }

            @Override
            protected void done() {
                try {
                    QuizSession newSession = get();
                    Window parentWindow = SwingUtilities.getWindowAncestor(MainPanel.this);

                    if (newSession == null) {
                        new InfoDialog(parentWindow, "Empty Deck", "This deck has no cards yet.").setVisible(true);
                        return;
                    }

                    activeDeck = deck;
                    activeSession = newSession;

                    for (Component c : contentPanel.getComponents())
                        if (c instanceof QuizPanel) contentPanel.remove(c);

                    activeQuizPanel = new QuizPanel(MainPanel.this, user, deck, activeSession);
                    contentPanel.add(activeQuizPanel, "QUIZ_SCREEN");
                    contentPanel.revalidate();
                    contentPanel.repaint();

                    if (answered >= newSession.totalDeckCards)
                        showQuizResults(deck, initialScore, newSession.totalDeckCards);
                    else
                        showScreen("QUIZ_SCREEN");

                } catch (Exception e) {
                    e.printStackTrace();
                    Window parentWindow = SwingUtilities.getWindowAncestor(MainPanel.this);
                    new InfoDialog(parentWindow, "Error", "Failed to load quiz. Please try again.").setVisible(true);
                }
            }
        }.execute();
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
        navbarPanel = new JPanel();
        navbarPanel.setLayout(new GridBagLayout());
        navbarPanel.setPreferredSize(new Dimension(-1, 56));
        root.add(navbarPanel, BorderLayout.NORTH);
        navbarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        logoPanel = new JPanel();
        logoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        navbarPanel.add(logoPanel, gbc);
        logoLabel = new JLabel();
        logoLabel.setText("FlipIT");
        logoPanel.add(logoLabel);
        linksPanel = new JPanel();
        linksPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        navbarPanel.add(linksPanel, gbc);
        AdminDashBtn = new JButton();
        AdminDashBtn.setText("Dashboard");
        linksPanel.add(AdminDashBtn);
        AdminUsersBtn = new JButton();
        AdminUsersBtn.setText("Users");
        linksPanel.add(AdminUsersBtn);
        HomeBtn = new JButton();
        HomeBtn.setText("Home");
        linksPanel.add(HomeBtn);
        BrowseBtn = new JButton();
        BrowseBtn.setText("Browse");
        linksPanel.add(BrowseBtn);
        DecksBtn = new JButton();
        DecksBtn.setText("Decks");
        linksPanel.add(DecksBtn);
        UploadBtn = new JButton();
        UploadBtn.setText("Upload");
        linksPanel.add(UploadBtn);
        FilesBtn = new JButton();
        FilesBtn.setText("Files");
        linksPanel.add(FilesBtn);
        ModReportsBtn = new JButton();
        ModReportsBtn.setText("Reports");
        linksPanel.add(ModReportsBtn);
        profilePanel = new JPanel();
        profilePanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        navbarPanel.add(profilePanel, gbc);
        usernameLabel = new JLabel();
        usernameLabel.setText("Username");
        profilePanel.add(usernameLabel);
        avatarBtn = new JButton();
        avatarBtn.setMinimumSize(new Dimension(48, 48));
        avatarBtn.setPreferredSize(new Dimension(48, 48));
        avatarBtn.setText("");
        profilePanel.add(avatarBtn);
        contentPanel = new JPanel();
        contentPanel.setLayout(new CardLayout(0, 0));
        root.add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}