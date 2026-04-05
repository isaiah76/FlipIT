package com.flipit.ui.panels;

import com.flipit.dao.CardProgressDAO;
import com.flipit.models.Card;
import com.flipit.models.Deck;
import com.flipit.models.User;
import com.flipit.service.GoogleTTSService;
import com.flipit.ui.dialogs.WarningDialog;
import com.flipit.util.ImageUtil;
import com.flipit.util.IconUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizPanel extends JPanel {
    private JPanel root;
    private JPanel quizContainer;
    private JButton backBtn;
    private JLabel deckNameLbl;
    private JLabel counterLbl;
    private JProgressBar progressBar;
    private JPanel bodyPanel;
    private JPanel qCard;
    private JLabel questionNumLbl;
    private JToggleButton ttsToggleBtn;
    private JLabel questionLbl;
    private JPanel optionsGridPanel;
    private JPanel optionPanelA, optionPanelB, optionPanelC, optionPanelD;
    private JPanel badgePanelA, badgePanelB, badgePanelC, badgePanelD;
    private JLabel badgeA, badgeB, badgeC, badgeD;
    private JLabel textA, textB, textC, textD;
    private JButton prevBtn;
    private JButton nextBtn;
    private JButton skipBtn;
    private JButton submitBtn;
    private JLabel questionStatusLbl;

    private final MainPanel mainPanel;
    private final User user;
    private final Deck deck;
    private final MainPanel.QuizSession session;
    private final CardProgressDAO progressDAO = new CardProgressDAO();

    private final JPanel[] optionPanels;
    private final JPanel[] badgePanels;
    private final JLabel[] optionLetters;
    private final JLabel[] optionTexts;

    private String selected = null;
    private boolean submitted = false;
    private boolean isFinished = false;
    private final CardLayout cardLayout;

    private final Map<Integer, List<String>> shuffleMap = new HashMap<>();

    private SwingWorker<Void, Void> ttsWorker;
    private volatile SourceDataLine audioLine;

    private JPanel skeletonOverlay;

    public QuizPanel(MainPanel mainPanel, User user, Deck deck, MainPanel.QuizSession session) {
        this.mainPanel = mainPanel;
        this.user = user;
        this.deck = deck;
        this.session = session;

        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
        cardLayout = (CardLayout) root.getLayout();

        optionPanels = new JPanel[]{optionPanelA, optionPanelB, optionPanelC, optionPanelD};
        badgePanels = new JPanel[]{badgePanelA, badgePanelB, badgePanelC, badgePanelD};
        optionLetters = new JLabel[]{badgeA, badgeB, badgeC, badgeD};
        optionTexts = new JLabel[]{textA, textB, textC, textD};

        if (questionStatusLbl == null) {
            questionStatusLbl = new JLabel(" ");
            questionStatusLbl.setHorizontalAlignment(SwingConstants.CENTER);
            qCard.add(questionStatusLbl, BorderLayout.SOUTH);
        }

        styleAll();
        bindEvents();
        setupSkeletonOverlay();

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                stopAudio();
                ttsToggleBtn.setSelected(false);
                ttsToggleBtn.repaint();
            }
        });

        setButtonsEnabled(false);

        cardLayout.show(root, "SKELETON");

        new SwingWorker<Map<Integer, String>, Void>() {
            @Override
            protected Map<Integer, String> doInBackground() {
                List<Integer> cardIds = session.cards.stream()
                        .map(Card::getId)
                        .collect(Collectors.toList());
                return progressDAO.getSelectedAnswersBatch(user.getId(), cardIds);
            }

            @Override
            protected void done() {
                try {
                    Map<Integer, String> batchAnswers = get();
                    if (batchAnswers != null) {
                        session.sessionAnswers.putAll(batchAnswers);
                    }
                    if (session.sessionAnswers.size() >= session.totalDeckCards) {
                        isFinished = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                cardLayout.show(root, "QUIZ");
                setButtonsEnabled(true);
                loadQuestion();
            }
        }.execute();
    }

    private void setupSkeletonOverlay() {
        skeletonOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.decode("#f8fafc"));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(Color.decode("#e2e8f0"));

                int scale = ImageUtil.scale(10);
                g2.fillRoundRect(scale * 5, scale * 3, scale * 8, scale * 2, scale, scale);
                g2.fillRoundRect(getWidth() - (scale * 13), scale * 3, scale * 8, scale * 2, scale, scale);
                g2.fillRoundRect(scale * 5, scale * 6, getWidth() - (scale * 10), scale, scale, scale);

                int qY = scale * 10;
                g2.fill(new RoundRectangle2D.Double(scale * 5, qY, getWidth() - (scale * 10), scale * 15, scale * 2, scale * 2));

                g2.dispose();
            }
        };
        root.add(skeletonOverlay, "SKELETON");
    }

    private void setButtonsEnabled(boolean enabled) {
        prevBtn.setEnabled(enabled);
        nextBtn.setEnabled(enabled);
        skipBtn.setEnabled(enabled);
        submitBtn.setEnabled(enabled);
    }

    private void styleAll() {
        Color bgTheme = Color.decode("#f8fafc");
        root.setBackground(bgTheme);
        quizContainer.setBackground(bgTheme);

        float sf = ImageUtil.getScaleFactor();

        BorderLayout mainLayout = (BorderLayout) quizContainer.getLayout();
        mainLayout.setVgap(ImageUtil.scale(15));

        quizContainer.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(25), ImageUtil.scale(40), ImageUtil.scale(25), ImageUtil.scale(40)
        ));

        BorderLayout bodyLayout = (BorderLayout) bodyPanel.getLayout();
        bodyLayout.setVgap(ImageUtil.scale(15));

        optionsGridPanel.setLayout(new GridLayout(2, 2, ImageUtil.scale(15), ImageUtil.scale(15)));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        backBtn.setForeground(Color.decode("#2563eb"));
        backBtn.setIconTextGap(ImageUtil.scale(8));
        backBtn.setIcon(IconUtil.getIcon("BACK", Color.decode("#2563eb"), ImageUtil.scale(14)));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setText("Back");

        Container headerPanel = backBtn.getParent();
        if (headerPanel.getLayout() instanceof BorderLayout) {
            ((BorderLayout) headerPanel.getLayout()).setVgap(ImageUtil.scale(8));
            headerPanel.setFont(baseFont);
        }

        ttsToggleBtn.setContentAreaFilled(false);
        ttsToggleBtn.setBorderPainted(false);
        ttsToggleBtn.setFocusPainted(false);
        ttsToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ttsToggleBtn.setText("");

        Image soundOnImg = ImageUtil.loadImage("/sound-on.png");
        Image soundOffImg = ImageUtil.loadImage("/sound-off.png");

        ttsToggleBtn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                AbstractButton b = (AbstractButton) c;
                Image iconImg = b.isSelected() ? soundOnImg : soundOffImg;

                if (iconImg != null) {
                    int size = ImageUtil.scale(22);
                    int x = (c.getWidth() - size) / 2;
                    int y = (c.getHeight() - size) / 2;
                    g2.drawImage(iconImg, x, y, size, size, null);
                }
                g2.dispose();
            }
        });

        int btnSize = ImageUtil.scale(36);
        ttsToggleBtn.setPreferredSize(new Dimension(btnSize, btnSize));

        Container qHead = ttsToggleBtn.getParent();
        if (qHead != null && qHead.getLayout() instanceof BorderLayout) {
            qHead.add(Box.createRigidArea(new Dimension(btnSize, btnSize)), BorderLayout.EAST);
        }

        progressBar.setForeground(Color.decode("#3b82f6"));
        progressBar.setBackground(Color.decode("#e2e8f0"));
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, ImageUtil.scale(10)));

        qCard.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(25), ImageUtil.scale(35), ImageUtil.scale(25), ImageUtil.scale(35)
        ));

        qCard.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Color.decode("#eff6ff"), c.getWidth(), c.getHeight(), Color.decode("#dbeafe")));
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), ImageUtil.scale(20), ImageUtil.scale(20)));
                g2.setColor(Color.decode("#3b82f6"));
                g2.setStroke(new BasicStroke(Math.max(1.5f, 3f * sf)));
                g2.draw(new RoundRectangle2D.Double(1.5, 1.5, c.getWidth() - 3, c.getHeight() - 3, ImageUtil.scale(18), ImageUtil.scale(18)));
                g2.dispose();
                super.paint(g, c);
            }
        });

        questionNumLbl.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        questionNumLbl.setForeground(Color.decode("#64748b"));

        questionStatusLbl.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));

        questionLbl.setFont(baseFont.deriveFont(Font.BOLD, 19f * sf));
        questionLbl.setForeground(Color.decode("#0f172a"));

        for (int i = 0; i < 4; i++) {
            optionLetters[i].setFont(baseFont.deriveFont(Font.BOLD, 15f * sf));
            optionTexts[i].setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));

            badgePanels[i].setPreferredSize(new Dimension(ImageUtil.scale(39), ImageUtil.scale(39)));
            badgePanels[i].setMinimumSize(new Dimension(ImageUtil.scale(39), ImageUtil.scale(39)));

            BorderLayout optLyt = (BorderLayout) optionPanels[i].getLayout();
            optLyt.setHgap(ImageUtil.scale(13));

            optionPanels[i].setBorder(BorderFactory.createEmptyBorder(
                    ImageUtil.scale(18), ImageUtil.scale(22), ImageUtil.scale(18), ImageUtil.scale(22)
            ));

            styleOptionPanel(i, new String[]{"A", "B", "C", "D"}[i]);
        }

        styleSecondaryBtn(skipBtn, baseFont.deriveFont(Font.BOLD, 13f * sf));
        styleSecondaryBtn(prevBtn, baseFont.deriveFont(Font.BOLD, 13f * sf));
        stylePrimaryBtn(submitBtn, baseFont.deriveFont(Font.BOLD, 13f * sf));
        stylePrimaryBtn(nextBtn, baseFont.deriveFont(Font.BOLD, 13f * sf));

        Container footer = prevBtn.getParent();
        if (footer != null && footer.getLayout() instanceof BorderLayout) {
            ((BorderLayout) footer.getLayout()).setHgap(ImageUtil.scale(13));
        }

        Container centerAction = skipBtn.getParent();
        if (centerAction != null && centerAction.getLayout() instanceof FlowLayout) {
            ((FlowLayout) centerAction.getLayout()).setHgap(ImageUtil.scale(13));
        }
    }

    private void styleOptionPanel(int i, String letter) {
        JPanel p = optionPanels[i];
        JPanel badge = badgePanels[i];

        final boolean[] hov = {false};

        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hov[0] = true;
                p.repaint();
                badge.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hov[0] = false;
                p.repaint();
                badge.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!submitted && !isFinished) {
                    Card cur = session.cards.get(session.currentIndex);
                    List<String> mapped = shuffleMap.get(cur.getId());
                    if (mapped != null) {
                        selectOption(mapped.get(letter.charAt(0) - 'A'));
                    } else {
                        selectOption(letter);
                    }
                }
            }
        });

        p.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = Color.WHITE, border = Color.decode("#e2e8f0");

                Card cur = null;
                if (session != null && session.cards != null && session.currentIndex < session.cards.size()) {
                    cur = session.cards.get(session.currentIndex);
                }
                String originalLetter = letter;
                if (cur != null) {
                    List<String> mapped = shuffleMap.get(cur.getId());
                    if (mapped != null) {
                        originalLetter = mapped.get(letter.charAt(0) - 'A');
                    }
                }

                if (submitted && cur != null) {
                    if (originalLetter.equals(cur.getCorrectAnswer())) {
                        bg = Color.decode("#dcfce7");
                        border = Color.decode("#22c55e");
                    } else if (originalLetter.equals(selected)) {
                        bg = Color.decode("#fee2e2");
                        border = Color.decode("#ef4444");
                    }
                } else if (originalLetter.equals(selected)) {
                    bg = Color.decode("#dbeafe");
                    border = Color.decode("#3b82f6");
                } else if (hov[0]) {
                    bg = Color.decode("#eff6ff");
                    border = Color.decode("#3b82f6");
                }

                int arc = ImageUtil.scale(12);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), arc, arc));
                g2.setColor(border);
                g2.setStroke(new BasicStroke(Math.max(1f, 3f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(1.5, 1.5, c.getWidth() - 3, c.getHeight() - 3, arc - 2, arc - 2));
                g2.dispose();
                super.paint(g, c);
            }
        });

        badge.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Card cur = null;
                if (session != null && session.cards != null && session.currentIndex < session.cards.size()) {
                    cur = session.cards.get(session.currentIndex);
                }
                String originalLetter = letter;
                if (cur != null) {
                    List<String> mapped = shuffleMap.get(cur.getId());
                    if (mapped != null) {
                        originalLetter = mapped.get(letter.charAt(0) - 'A');
                    }
                }

                if (submitted && cur != null) {
                    if (originalLetter.equals(cur.getCorrectAnswer())) {
                        g2.setPaint(new GradientPaint(0, 0, Color.decode("#22c55e"), c.getWidth(), c.getHeight(), Color.decode("#16a34a")));
                    } else if (originalLetter.equals(selected)) {
                        g2.setPaint(new GradientPaint(0, 0, Color.decode("#ef4444"), c.getWidth(), c.getHeight(), Color.decode("#dc2626")));
                    } else g2.setColor(Color.decode("#f1f5f9"));
                } else if (originalLetter.equals(selected)) {
                    g2.setPaint(new GradientPaint(0, 0, Color.decode("#3b82f6"), c.getWidth(), c.getHeight(), Color.decode("#1e40af")));
                } else g2.setColor(Color.decode("#f1f5f9"));

                int arc = ImageUtil.scale(8);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), arc, arc));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void styleSecondaryBtn(JButton btn, Font f) {
        btn.setFont(f);
        btn.setBackground(Color.decode("#f1f5f9"));
        btn.setForeground(Color.decode("#475569"));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(10), ImageUtil.scale(20), ImageUtil.scale(10), ImageUtil.scale(20)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void stylePrimaryBtn(JButton btn, Font f) {
        btn.setFont(f);
        btn.setBackground(Color.decode("#3b82f6"));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(10), ImageUtil.scale(20), ImageUtil.scale(10), ImageUtil.scale(20)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void playTTS(String text) {
        stopAudio();

        ttsWorker = new SwingWorker<>() {
            private File tempAudioFile;

            @Override
            protected Void doInBackground() {
                try {
                    GoogleTTSService googleTTS = new GoogleTTSService();
                    tempAudioFile = googleTTS.generateAudioFile(text);

                    if (isCancelled()) return null;

                    try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(tempAudioFile)) {
                        AudioFormat baseFormat = audioStream.getFormat();

                        AudioFormat decodedFormat = new AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                baseFormat.getSampleRate(),
                                16,
                                baseFormat.getChannels(),
                                baseFormat.getChannels() * 2,
                                baseFormat.getSampleRate(),
                                false
                        );

                        try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream)) {
                            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);

                            if (!AudioSystem.isLineSupported(info)) {
                                System.err.println("Decoded audio format not supported by hardware.");
                                return null;
                            }

                            if (isCancelled()) return null;
                            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                            line.open(decodedFormat);
                            line.start();
                            audioLine = line;

                            byte[] buffer = new byte[4096];
                            int bytesRead;

                            while ((bytesRead = decodedStream.read(buffer)) != -1 && !isCancelled()) {
                                SourceDataLine activeLine = audioLine;
                                if (activeLine != null && activeLine.isOpen()) {
                                    activeLine.write(buffer, 0, bytesRead);
                                } else {
                                    break;
                                }
                            }

                            SourceDataLine closingLine = audioLine;
                            if (closingLine != null && closingLine.isOpen()) {
                                closingLine.drain();
                                closingLine.stop();
                                closingLine.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!(e.getCause() instanceof InterruptedException)) {
                        System.err.println("TTS Error: " + e.getMessage());
                    }
                } finally {
                    if (tempAudioFile != null && tempAudioFile.exists()) {
                        tempAudioFile.delete();
                    }
                }
                return null;
            }
        };
        ttsWorker.execute();
    }

    private void stopAudio() {
        if (ttsWorker != null && !ttsWorker.isDone()) {
            ttsWorker.cancel(true);
        }
        SourceDataLine line = audioLine;
        audioLine = null;

        if (line != null && line.isOpen()) {
            try {
                line.stop();
                line.flush();
                line.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void attemptFinish() {
        if (isFinished) {
            showResults();
            return;
        }

        int skippedCount = 0;
        List<Card> skippedCards = new ArrayList<>();

        for (Card c : session.cards) {
            String ans = session.sessionAnswers.get(c.getId());
            if (ans == null || ans.equals("S")) {
                skippedCount++;
                skippedCards.add(c);
            }
        }

        if (skippedCount > 0) {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            WarningDialog dialog = new WarningDialog(parentWindow,
                    "Unanswered Questions",
                    "You still have " + skippedCount + " skipped question(s).\nIf you submit now, they will be marked as incorrect.\n\nAre you sure you want to submit?",
                    "Submit Anyway");
            dialog.setVisible(true);

            if (!dialog.isApproved()) {
                return;
            }

            submitBtn.setEnabled(false);
            skipBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    for (Card c : skippedCards) {
                        progressDAO.setAnswered(user.getId(), c.getId(), true, "S");
                    }
                    return null;
                }

                @Override
                protected void done() {
                    for (Card c : skippedCards) {
                        session.sessionAnswers.put(c.getId(), "S");
                    }
                    isFinished = true;
                    loadQuestion();
                    showResults();
                }
            }.execute();
        } else {
            isFinished = true;
            loadQuestion();
            showResults();
        }
    }

    private void bindEvents() {
        backBtn.addActionListener(e -> {
            stopAudio();
            ttsToggleBtn.setSelected(false);
            mainPanel.returnFromQuiz();
        });

        ttsToggleBtn.addActionListener(e -> {
            if (ttsToggleBtn.isSelected() && session.currentIndex < session.cards.size()) {
                Card c = session.cards.get(session.currentIndex);
                String ttsText;

                List<String> mapped = shuffleMap.get(c.getId());
                String[] originalAnswers = {c.getAnswerA(), c.getAnswerB(), c.getAnswerC(), c.getAnswerD()};

                if (!submitted) {
                    ttsText = c.getQuestion() + ".\n" +
                            "A. " + originalAnswers[mapped.get(0).charAt(0) - 'A'] + ".\n" +
                            "B. " + originalAnswers[mapped.get(1).charAt(0) - 'A'] + ".\n" +
                            "C. " + originalAnswers[mapped.get(2).charAt(0) - 'A'] + ".\n" +
                            "D. " + originalAnswers[mapped.get(3).charAt(0) - 'A'] + ".";
                } else {
                    String correctAnswerText = originalAnswers[c.getCorrectAnswer().charAt(0) - 'A'];
                    ttsText = c.getQuestion() + ". The correct answer is: " + correctAnswerText;
                }
                playTTS(ttsText);
            } else {
                stopAudio();
            }
        });

        prevBtn.addActionListener(e -> {
            if (session.currentIndex > 0) {
                session.currentIndex--;
                loadQuestion();
            }
        });

        skipBtn.addActionListener(e -> {
            stopAudio();
            Card cur = session.cards.get(session.currentIndex);
            session.sessionAnswers.put(cur.getId(), "S");

            if (session.currentIndex + 1 >= session.cards.size()) {
                attemptFinish();
            } else {
                session.currentIndex++;
                loadQuestion();
            }
        });

        nextBtn.addActionListener(e -> {
            stopAudio();
            if (session.currentIndex + 1 >= session.cards.size()) {
                attemptFinish();
            } else {
                session.currentIndex++;
                loadQuestion();
            }
        });

        submitBtn.addActionListener(e -> handleAction(selected));
    }

    private void loadQuestion() {
        stopAudio();

        Card c = session.cards.get(session.currentIndex);
        int overallDone = session.currentIndex;

        if (!shuffleMap.containsKey(c.getId())) {
            List<String> letters = Arrays.asList("A", "B", "C", "D");
            Collections.shuffle(letters);
            shuffleMap.put(c.getId(), letters);
        }
        List<String> mappedLetters = shuffleMap.get(c.getId());

        deckNameLbl.setText(deck.getTitle());

        String safeQuestion = c.getQuestion()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");

        questionLbl.setText("<html><div style='text-align:center;'>" + safeQuestion + "</div></html>");
        counterLbl.setText("Card " + (overallDone + 1) + " of " + session.totalDeckCards);

        int progress = (int) (((float) overallDone / session.totalDeckCards) * 100);
        progressBar.setValue(progress);

        String[] originalAnswers = {c.getAnswerA(), c.getAnswerB(), c.getAnswerC(), c.getAnswerD()};
        for (int i = 0; i < 4; i++) {
            String originalLetter = mappedLetters.get(i);
            int originalIndex = originalLetter.charAt(0) - 'A';

            String safeAnswer = originalAnswers[originalIndex]
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");

            optionTexts[i].setText("<html>" + safeAnswer + "</html>");
            optionPanels[i].repaint();
            badgePanels[i].repaint();
        }

        questionNumLbl.setText("QUESTION " + (overallDone + 1));

        String actualAnswer = session.sessionAnswers.get(c.getId());
        boolean isSkipped = "S".equals(actualAnswer);

        if (actualAnswer != null && !isSkipped) {
            submitted = true;
            selected = actualAnswer;

            submitBtn.setVisible(false);
            skipBtn.setVisible(false);
            nextBtn.setEnabled(true);

            boolean correct = actualAnswer.equals(c.getCorrectAnswer());
            questionStatusLbl.setText(correct ? "CORRECT" : "INCORRECT");
            questionStatusLbl.setForeground(correct ? Color.decode("#10b981") : Color.decode("#ef4444"));
        } else {
            submitted = false;
            selected = null;

            if (isFinished) {
                submitBtn.setVisible(false);
                skipBtn.setVisible(false);
                nextBtn.setEnabled(true);

                if (isSkipped) {
                    questionStatusLbl.setText("SKIPPED");
                    questionStatusLbl.setForeground(Color.decode("#f59e0b"));
                } else {
                    questionStatusLbl.setText(" ");
                }
            } else {
                submitBtn.setVisible(true);
                submitBtn.setEnabled(false);
                skipBtn.setVisible(true);
                skipBtn.setEnabled(true);

                if (isSkipped) {
                    nextBtn.setEnabled(true);
                    questionStatusLbl.setText(" ");
                } else {
                    nextBtn.setEnabled(false);
                    questionStatusLbl.setText(" ");
                }
            }
        }

        prevBtn.setEnabled(session.currentIndex > 0);

        if (session.currentIndex + 1 >= session.cards.size()) {
            nextBtn.setText("Finish");
        } else {
            nextBtn.setText("Next →");
        }

        updateOptionUIs();

        this.revalidate();
        this.repaint();

        if (ttsToggleBtn.isSelected()) {
            String ttsText;
            if (!submitted) {
                ttsText = c.getQuestion() + ".\n" +
                        "A. " + originalAnswers[mappedLetters.get(0).charAt(0) - 'A'] + ".\n" +
                        "B. " + originalAnswers[mappedLetters.get(1).charAt(0) - 'A'] + ".\n" +
                        "C. " + originalAnswers[mappedLetters.get(2).charAt(0) - 'A'] + ".\n" +
                        "D. " + originalAnswers[mappedLetters.get(3).charAt(0) - 'A'] + ".";
            } else {
                String correctAnswerText = originalAnswers[c.getCorrectAnswer().charAt(0) - 'A'];
                ttsText = c.getQuestion() + ". The correct answer is: " + correctAnswerText;
            }
            playTTS(ttsText);
        }
    }

    private void selectOption(String originalLetter) {
        selected = originalLetter;
        updateOptionUIs();
        submitBtn.setEnabled(true);
    }

    private void handleAction(String choice) {
        if (choice == null || choice.equals("S")) return;

        stopAudio();

        Card cur = session.cards.get(session.currentIndex);
        session.sessionAnswers.put(cur.getId(), choice);
        submitted = true;

        boolean correct = choice.equals(cur.getCorrectAnswer());
        if (correct) session.score++;

        questionStatusLbl.setText(correct ? "CORRECT" : "INCORRECT");
        questionStatusLbl.setForeground(correct ? Color.decode("#10b981") : Color.decode("#ef4444"));

        updateOptionUIs();
        submitBtn.setVisible(false);
        skipBtn.setVisible(false);
        nextBtn.setEnabled(true);

        if (session.currentIndex + 1 >= session.cards.size()) {
            nextBtn.setText("Finish");
            progressBar.setValue(100);
        }

        this.revalidate();
        this.repaint();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                progressDAO.setAnswered(user.getId(), cur.getId(), true, choice);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    System.err.println("Failed to save answer to database: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void updateOptionUIs() {
        Card cur = session.cards.get(session.currentIndex);
        List<String> mapped = shuffleMap.get(cur.getId());

        for (int i = 0; i < 4; i++) {
            String originalLetter = mapped.get(i);
            boolean isSelected = originalLetter.equals(selected);
            boolean isCorrect = false;

            if (submitted) {
                isCorrect = originalLetter.equals(cur.getCorrectAnswer());
            }

            optionLetters[i].setForeground((isCorrect || isSelected) ? Color.WHITE : Color.decode("#64748b"));
            optionPanels[i].repaint();
            badgePanels[i].repaint();
        }
    }

    private void showResults() {
        progressBar.setValue(100);
        mainPanel.showQuizResults(deck, session.score, session.totalDeckCards);
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
        root.setLayout(new CardLayout(0, 0));
        root.setOpaque(false);
        quizContainer = new JPanel();
        quizContainer.setLayout(new BorderLayout(0, 0));
        quizContainer.setOpaque(false);
        root.add(quizContainer, "QUIZ");
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setOpaque(false);
        quizContainer.add(panel1, BorderLayout.NORTH);
        backBtn = new JButton();
        backBtn.setHorizontalAlignment(2);
        backBtn.setText("← Back");
        panel1.add(backBtn, BorderLayout.NORTH);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel2.setOpaque(false);
        panel1.add(panel2, BorderLayout.CENTER);
        deckNameLbl = new JLabel();
        deckNameLbl.setText("Deck Name");
        panel2.add(deckNameLbl, BorderLayout.WEST);
        counterLbl = new JLabel();
        counterLbl.setText("Card 1 of 10");
        panel2.add(counterLbl, BorderLayout.EAST);
        progressBar = new JProgressBar();
        panel1.add(progressBar, BorderLayout.SOUTH);
        bodyPanel = new JPanel();
        bodyPanel.setLayout(new BorderLayout(0, 0));
        bodyPanel.setOpaque(false);
        quizContainer.add(bodyPanel, BorderLayout.CENTER);
        qCard = new JPanel();
        qCard.setLayout(new BorderLayout(0, 0));
        qCard.setOpaque(false);
        bodyPanel.add(qCard, BorderLayout.NORTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel3.setOpaque(false);
        qCard.add(panel3, BorderLayout.NORTH);
        questionNumLbl = new JLabel();
        questionNumLbl.setHorizontalAlignment(0);
        questionNumLbl.setText("QUESTION 1");
        panel3.add(questionNumLbl, BorderLayout.CENTER);
        ttsToggleBtn = new JToggleButton();
        ttsToggleBtn.setText("");
        panel3.add(ttsToggleBtn, BorderLayout.WEST);
        questionLbl = new JLabel();
        questionLbl.setHorizontalAlignment(0);
        questionLbl.setText("Question Text");
        qCard.add(questionLbl, BorderLayout.CENTER);
        questionStatusLbl = new JLabel();
        questionStatusLbl.setHorizontalAlignment(0);
        questionStatusLbl.setText(" ");
        qCard.add(questionStatusLbl, BorderLayout.SOUTH);
        optionsGridPanel = new JPanel();
        optionsGridPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), 15, 15));
        optionsGridPanel.setOpaque(false);
        bodyPanel.add(optionsGridPanel, BorderLayout.CENTER);
        optionPanelA = new JPanel();
        optionPanelA.setLayout(new BorderLayout(0, 0));
        optionPanelA.setOpaque(false);
        optionsGridPanel.add(optionPanelA, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        badgePanelA = new JPanel();
        badgePanelA.setLayout(new GridBagLayout());
        badgePanelA.setOpaque(false);
        optionPanelA.add(badgePanelA, BorderLayout.WEST);
        badgeA = new JLabel();
        badgeA.setText("A");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        badgePanelA.add(badgeA, gbc);
        textA = new JLabel();
        textA.setText("Option A Text");
        optionPanelA.add(textA, BorderLayout.CENTER);
        optionPanelB = new JPanel();
        optionPanelB.setLayout(new BorderLayout(0, 0));
        optionPanelB.setOpaque(false);
        optionsGridPanel.add(optionPanelB, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        badgePanelB = new JPanel();
        badgePanelB.setLayout(new GridBagLayout());
        badgePanelB.setOpaque(false);
        optionPanelB.add(badgePanelB, BorderLayout.WEST);
        badgeB = new JLabel();
        badgeB.setText("B");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        badgePanelB.add(badgeB, gbc);
        textB = new JLabel();
        textB.setText("Option B Text");
        optionPanelB.add(textB, BorderLayout.CENTER);
        optionPanelC = new JPanel();
        optionPanelC.setLayout(new BorderLayout(0, 0));
        optionPanelC.setOpaque(false);
        optionsGridPanel.add(optionPanelC, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        badgePanelC = new JPanel();
        badgePanelC.setLayout(new GridBagLayout());
        badgePanelC.setOpaque(false);
        optionPanelC.add(badgePanelC, BorderLayout.WEST);
        badgeC = new JLabel();
        badgeC.setText("C");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        badgePanelC.add(badgeC, gbc);
        textC = new JLabel();
        textC.setText("Option C Text");
        optionPanelC.add(textC, BorderLayout.CENTER);
        optionPanelD = new JPanel();
        optionPanelD.setLayout(new BorderLayout(0, 0));
        optionPanelD.setOpaque(false);
        optionsGridPanel.add(optionPanelD, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        badgePanelD = new JPanel();
        badgePanelD.setLayout(new GridBagLayout());
        badgePanelD.setOpaque(false);
        optionPanelD.add(badgePanelD, BorderLayout.WEST);
        badgeD = new JLabel();
        badgeD.setText("D");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        badgePanelD.add(badgeD, gbc);
        textD = new JLabel();
        textD.setText("Option D Text");
        optionPanelD.add(textD, BorderLayout.CENTER);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel4.setOpaque(false);
        quizContainer.add(panel4, BorderLayout.SOUTH);
        prevBtn = new JButton();
        prevBtn.setText("← Prev");
        panel4.add(prevBtn, BorderLayout.WEST);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel5.setOpaque(false);
        panel4.add(panel5, BorderLayout.CENTER);
        skipBtn = new JButton();
        skipBtn.setText("Skip");
        panel5.add(skipBtn);
        submitBtn = new JButton();
        submitBtn.setText("Submit Answer");
        panel5.add(submitBtn);
        nextBtn = new JButton();
        nextBtn.setText("Next →");
        panel4.add(nextBtn, BorderLayout.EAST);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}