package com.flipit.ui.panels;

import com.flipit.dao.CardDAO;
import com.flipit.dao.CardProgressDAO;
import com.flipit.models.Card;
import com.flipit.models.Deck;
import com.flipit.models.User;
import com.flipit.ui.dialogs.EditCardDialog;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.dialogs.WarningDialog;
import com.flipit.util.ImageUtil;
import com.flipit.util.IconUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.Locale;

public class CardEditorPanel extends JPanel {
    private JPanel root;
    private JPanel headerPanel;
    private JButton backBtn;
    private JButton addCardBtn;
    private JLabel deckTitle;
    private JScrollPane cardScrollPane;
    private JPanel cardListPanel;
    private MainPanel mainPanel;
    private User user;
    private Deck deck;

    private final CardDAO cardDAO = new CardDAO();
    private final CardProgressDAO progressDAO = new CardProgressDAO();

    private JPanel rowsContainer;
    private SwingWorker<List<Card>, Void> cardLoaderWorker;

    private int s(int val) {
        return ImageUtil.scale((int) Math.round(val * 1.2f));
    }

    private float f(float val) {
        return val * 1.2f * ImageUtil.getScaleFactor();
    }

    private Font getBaseFont() {
        Font f = UIManager.getFont("defaultFont");
        return f != null ? f : new Font("SansSerif", Font.PLAIN, 12);
    }

    public CardEditorPanel() {
    }

    public CardEditorPanel(MainPanel mainPanel, User user, Deck deck) {
        this.mainPanel = mainPanel;
        this.user = user;
        this.deck = deck;

        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleAll();
        wireButtons();
        loadCards();
    }

    private void styleAll() {
        Font baseFont = getBaseFont();
        root.setBackground(Color.decode("#f8fafc"));

        headerPanel.setBackground(Color.decode("#f8fafc"));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(s(10), s(15), s(10), s(15)));

        deckTitle.setText(deck != null ? deck.getTitle() : "");
        deckTitle.setFont(baseFont.deriveFont(Font.BOLD, f(14f)));
        deckTitle.setForeground(Color.decode("#0f172a"));

        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setFont(baseFont.deriveFont(Font.BOLD, f(10f)));
        backBtn.setForeground(Color.decode("#2563eb"));
        backBtn.setIconTextGap(s(8));
        backBtn.setIcon(IconUtil.getIcon("BACK", Color.decode("#2563eb"), s(12)));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setText("Back");
        backBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                backBtn.setForeground(Color.decode("#1e40af"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                backBtn.setForeground(Color.decode("#3b82f6"));
            }
        });

        addCardBtn.setUI(new BasicButtonUI() {
            boolean hov = false;

            {
                addCardBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        addCardBtn.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        addCardBtn.repaint();
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
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), s(8), s(8)));
                g2.dispose();
                super.paint(g, c);
            }
        });

        addCardBtn.setPreferredSize(new Dimension(s(110), s(30)));
        addCardBtn.setContentAreaFilled(false);
        addCardBtn.setBorderPainted(false);
        addCardBtn.setFocusPainted(false);
        addCardBtn.setFont(baseFont.deriveFont(Font.BOLD, f(9f)));
        addCardBtn.setForeground(Color.WHITE);
        addCardBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (cardScrollPane != null) {
            cardScrollPane.setBorder(BorderFactory.createEmptyBorder());
            cardScrollPane.getViewport().setBackground(Color.decode("#f8fafc"));
            cardScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            cardScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            cardScrollPane.getVerticalScrollBar().setUnitIncrement(s(16));

            JPanel viewportWrapper = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    Container parent = getParent();
                    if (parent instanceof JViewport) {
                        d.width = parent.getWidth();
                    }
                    return d;
                }
            };
            viewportWrapper.setBackground(Color.decode("#f8fafc"));
            viewportWrapper.setBorder(BorderFactory.createEmptyBorder(s(15), s(15), s(15), s(20)));

            rowsContainer = new JPanel();
            rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
            rowsContainer.setBackground(Color.decode("#f8fafc"));

            viewportWrapper.add(rowsContainer, BorderLayout.NORTH);
            cardScrollPane.setViewportView(viewportWrapper);
        }
    }

    private void wireButtons() {
        backBtn.addActionListener(e -> mainPanel.returnFromEditor());
        addCardBtn.addActionListener(e -> showCardDialog(null));
    }

    private void loadCards() {
        if (rowsContainer == null || deck == null) return;

        if (cardLoaderWorker != null && !cardLoaderWorker.isDone()) {
            cardLoaderWorker.cancel(true);
        }

        rowsContainer.removeAll();
        for (int i = 0; i < 3; i++) {
            JPanel row = buildSkeletonCardRow();
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowsContainer.add(row);
            rowsContainer.add(Box.createVerticalStrut(s(10)));
        }
        rowsContainer.revalidate();
        rowsContainer.repaint();

        cardLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<Card> doInBackground() throws Exception {
                return cardDAO.getAllCardsByDeck(deck.getId());
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<Card> cards = get();
                    rowsContainer.removeAll();

                    if (cards.isEmpty()) {
                        JPanel empty = buildEmptyState();
                        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                        rowsContainer.add(empty);
                    } else {
                        for (int i = 0; i < cards.size(); i++) {
                            JPanel row = buildCardRow(cards.get(i), i + 1, cards.size());
                            row.setAlignmentX(Component.LEFT_ALIGNMENT);
                            rowsContainer.add(row);
                            if (i < cards.size() - 1) rowsContainer.add(Box.createVerticalStrut(s(10)));
                        }
                    }

                    rowsContainer.revalidate();
                    rowsContainer.repaint();
                } catch (
                        Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        ;
        cardLoaderWorker.execute();
    }

    private JPanel buildSkeletonCardRow() {
        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), s(10), s(10)));
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, s(9), s(9)));

                g2.setColor(Color.decode("#f1f5f9"));

                g2.fillRoundRect(s(15), s(12), s(80), s(10), s(4), s(4));
                g2.fillRoundRect(getWidth() - s(85), s(12), s(30), s(20), s(4), s(4));
                g2.fillRoundRect(getWidth() - s(45), s(12), s(30), s(20), s(4), s(4));

                g2.fillRoundRect(s(15), s(45), s(250), s(14), s(4), s(4));
                g2.fillRoundRect(s(15), s(65), s(180), s(14), s(4), s(4));

                int w = (getWidth() - s(40)) / 2;
                g2.fillRoundRect(s(15), s(95), w, s(40), s(6), s(6));
                g2.fillRoundRect(s(15) + w + s(10), s(95), w, s(40), s(6), s(6));
                g2.fillRoundRect(s(15), s(145), w, s(40), s(6), s(6));
                g2.fillRoundRect(s(15) + w + s(10), s(145), w, s(40), s(6), s(6));

                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, s(200)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(200)));
        return row;
    }

    private JPanel buildEmptyState() {
        Font baseFont = getBaseFont();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(s(25), 0, 0, 0));

        JLabel ico = new JLabel("📝");
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, (int) f(22f))); // Keep specific emoji font, but scale
        ico.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel("No cards yet.");
        msg.setFont(baseFont.deriveFont(Font.BOLD, f(10f)));
        msg.setForeground(Color.decode("#0f172a"));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setBorder(BorderFactory.createEmptyBorder(s(6), 0, s(3), 0));

        JLabel sub = new JLabel("Click Add Card");
        sub.setFont(baseFont.deriveFont(Font.PLAIN, f(9f)));
        sub.setForeground(Color.decode("#64748b"));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(ico);
        p.add(msg);
        p.add(sub);
        return p;
    }

    private JPanel buildCardRow(Card card, int index, int total) {
        Font baseFont = getBaseFont();
        JPanel panel = new JPanel(new BorderLayout(0, s(9))) {
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
                g2.setColor(hov ? Color.decode("#eff6ff") : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), s(10), s(10)));
                g2.setColor(hov ? Color.decode("#3b82f6") : Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, s(9), s(9)));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(s(12), s(15), s(12), s(15)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + s(12)));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel numLbl = new JLabel("CARD " + index + " OF " + total);
        numLbl.setFont(baseFont.deriveFont(Font.BOLD, f(8f)));
        numLbl.setForeground(Color.decode("#64748b"));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, s(5), 0));
        btns.setOpaque(false);

        JButton editBtn = outlineButton("Edit");
        editBtn.addActionListener(e -> showCardDialog(card));

        JButton delBtn = iconButton("🗑", Color.decode("#ef4444"));
        delBtn.addActionListener(e -> {
            if (deck.isPublic() && total <= 1) {
                new InfoDialog(SwingUtilities.getWindowAncestor(this),
                        "Cannot Delete",
                        "There must be at least one card in a public deck!").setVisible(true);
                return;
            }

            WarningDialog warn = new WarningDialog(SwingUtilities.getWindowAncestor(this),
                    "Confirm Delete",
                    "Are you sure you want to delete this card?",
                    "Delete");
            warn.setVisible(true);

            if (warn.isApproved()) {
                progressDAO.adjustHighscoresForCardDeletion(card.getId(), deck.getId());
                cardDAO.deleteCard(card.getId());
                mainPanel.clearActiveSession();
                loadCards();
            }
        });

        btns.add(editBtn);
        btns.add(delBtn);
        topRow.add(numLbl, BorderLayout.WEST);
        topRow.add(btns, BorderLayout.EAST);

        JPanel qSection = new JPanel(new BorderLayout(0, s(4)));
        qSection.setOpaque(false);
        qSection.add(capsLabel("QUESTION"), BorderLayout.NORTH);

        JPanel qBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#f8fafc"));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), s(6), s(6)));
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, s(5), s(5)));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        qBox.setOpaque(false);
        qBox.setBorder(BorderFactory.createEmptyBorder(s(6), s(9), s(6), s(9)));

        JLabel qText = new JLabel("<html><div style='width:" + s(310) + "px'>" +
                escHtml(card.getQuestion()) + "</div></html>");
        qText.setFont(baseFont.deriveFont(Font.BOLD, f(9f)));
        qText.setForeground(Color.decode("#0f172a"));
        qBox.add(qText, BorderLayout.CENTER);
        qSection.add(qBox, BorderLayout.CENTER);

        JPanel aSection = new JPanel(new BorderLayout(0, s(5)));
        aSection.setOpaque(false);
        aSection.add(capsLabel("ANSWERS"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, s(6), s(6)));
        grid.setOpaque(false);
        String[] letters = {"A", "B", "C", "D"};
        String[] answers = {card.getAnswerA(), card.getAnswerB(),
                card.getAnswerC(), card.getAnswerD()};
        for (int i = 0; i < 4; i++) {
            grid.add(answerChip(letters[i], answers[i],
                    letters[i].equals(card.getCorrectAnswer())));
        }
        aSection.add(grid, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, s(9)));
        body.setOpaque(false);
        body.add(topRow, BorderLayout.NORTH);
        body.add(qSection, BorderLayout.CENTER);
        body.add(aSection, BorderLayout.SOUTH);

        panel.add(body, BorderLayout.CENTER);

        Dimension pref = panel.getPreferredSize();
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height + s(12)));
        return panel;
    }

    private JPanel answerChip(String letter, String text, boolean correct) {
        Font baseFont = getBaseFont();
        JPanel chip = new JPanel(new BorderLayout(s(6), 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(correct ? Color.decode("#d1fae5") : Color.decode("#f8fafc"));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), s(6), s(6)));
                g2.setColor(correct ? Color.decode("#10b981") : Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, s(5), s(5)));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(s(5), s(8), s(5), s(8)));

        JPanel badge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (correct) g2.setPaint(new GradientPaint(0, 0, Color.decode("#10b981"),
                        getWidth(), getHeight(), Color.decode("#059669")));
                else g2.setColor(Color.decode("#cbd5e1"));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), s(4), s(4)));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(s(18), s(18)));
        badge.setMinimumSize(new Dimension(s(18), s(18)));
        badge.setMaximumSize(new Dimension(s(18), s(18)));

        JLabel bLbl = new JLabel(letter);
        bLbl.setFont(baseFont.deriveFont(Font.BOLD, f(8f)));
        bLbl.setForeground(correct ? Color.WHITE : Color.decode("#64748b"));
        badge.add(bLbl);

        JLabel ansLbl = new JLabel("<html>" + escHtml(text) + (correct ? " ✓" : "") + "</html>");
        ansLbl.setFont(baseFont.deriveFont(Font.PLAIN, f(9f)));
        ansLbl.setForeground(correct ? Color.decode("#065f46") : Color.decode("#0f172a"));

        chip.add(badge, BorderLayout.WEST);
        chip.add(ansLbl, BorderLayout.CENTER);
        return chip;
    }

    private void showCardDialog(Card existing) {
        EditCardDialog dialog = new EditCardDialog(SwingUtilities.getWindowAncestor(this), existing);
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            boolean ok;
            boolean addedNew = false;
            boolean answerChanged = false;

            String q = dialog.getQuestion();
            String a = dialog.getAnswerA();
            String b = dialog.getAnswerB();
            String c = dialog.getAnswerC();
            String d = dialog.getAnswerD();
            String ca = dialog.getCorrectAnswer();

            if (existing == null) {
                int newId = cardDAO.addCard(new Card(deck.getId(), q, a, b, c, d, ca));
                ok = newId > 0;
                addedNew = true;
            } else {
                if (!existing.getCorrectAnswer().equals(ca)) {
                    answerChanged = true;
                }
                existing.setQuestion(q);
                existing.setAnswerA(a);
                existing.setAnswerB(b);
                existing.setAnswerC(c);
                existing.setAnswerD(d);
                existing.setCorrectAnswer(ca);
                ok = cardDAO.updateCard(existing);
            }

            if (!ok) {
                new InfoDialog(SwingUtilities.getWindowAncestor(this), "Error", "Failed to save the card.").setVisible(true);
                return;
            }

            if (addedNew) {
                progressDAO.incrementHighscoreTotalCards(deck.getId());
                mainPanel.clearActiveSession();

            } else if (answerChanged) {
                progressDAO.adjustHighscoresForAnswerChange(existing.getId(), deck.getId());
                progressDAO.resetSingleCardProgress(existing.getId());
                mainPanel.clearActiveSession();

            } else {
                mainPanel.syncActiveSession();
            }

            SwingUtilities.invokeLater(this::loadCards);
        }
    }

    private JLabel capsLabel(String text) {
        Font baseFont = getBaseFont();
        JLabel l = new JLabel(text);
        l.setFont(baseFont.deriveFont(Font.BOLD, f(8f)));
        l.setForeground(Color.decode("#64748b"));
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, s(3), 0));
        return l;
    }

    private JButton outlineButton(String text) {
        Font baseFont = getBaseFont();
        JButton b = new JButton(text);
        b.setFont(baseFont.deriveFont(Font.BOLD, f(8f)));
        b.setForeground(Color.decode("#64748b"));
        b.setBackground(Color.decode("#f1f5f9"));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1),
                BorderFactory.createEmptyBorder(s(3), s(8), s(3), s(8))));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton iconButton(String icon, Color fg) {
        JButton b = new JButton(icon) {
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
                if (hov) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.decode("#fee2e2"));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), s(5), s(5)));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, (int) f(9f)));
        b.setForeground(fg);
        b.setPreferredSize(new Dimension(s(24), s(23)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
        headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBackground(new Color(-16777216));
        root.add(headerPanel, BorderLayout.NORTH);
        headerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        backBtn = new JButton();
        backBtn.setText("← Back");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        headerPanel.add(backBtn, gbc);
        deckTitle = new JLabel();
        Font deckTitleFont = this.$$$getFont$$$(null, Font.BOLD, 14, deckTitle.getFont());
        if (deckTitleFont != null) deckTitle.setFont(deckTitleFont);
        deckTitle.setText("Deck Title");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        headerPanel.add(deckTitle, gbc);
        addCardBtn = new JButton();
        addCardBtn.setPreferredSize(new Dimension(80, 30));
        addCardBtn.setText("+ ADD CARD");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        headerPanel.add(addCardBtn, gbc);
        cardScrollPane = new JScrollPane();
        root.add(cardScrollPane, BorderLayout.CENTER);
        cardListPanel = new JPanel();
        cardListPanel.setLayout(new BorderLayout(0, 0));
        cardListPanel.setBackground(new Color(-16777216));
        cardScrollPane.setViewportView(cardListPanel);
        cardListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}