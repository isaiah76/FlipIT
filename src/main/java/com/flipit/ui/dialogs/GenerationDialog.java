package com.flipit.ui.dialogs;

import com.flipit.dao.CardDAO;
import com.flipit.dao.DeckDAO;
import com.flipit.dao.FileDAO;
import com.flipit.models.Card;
import com.flipit.models.User;
import com.flipit.service.FileTextExtractor;
import com.flipit.service.GeminiService;
import com.flipit.ui.AppFrame;
import com.flipit.ui.panels.MainPanel;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GenerationDialog extends JDialog {
    private JPanel root;
    private JPanel settingsCard;
    private JPanel loadingCard;
    private JLabel titleLbl, fileLbl, deckTitleLbl, tagsLbl, amountLbl;
    private JTextField titleField, tagsField;
    private JSpinner amountSpinner;
    private JButton cancelBtn, generateBtn;
    private JPanel spinnerPanel;
    private JLabel loadingSub;

    private final AppFrame appFrame;
    private final MainPanel mainPanel;
    private final User user;
    private final File file;
    private final CardLayout cardLayout;

    private Timer spinnerTimer;
    private int spinnerAngle = 0;

    private SwingWorker<Integer, String> generationWorker;

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

    public GenerationDialog(AppFrame appFrame, MainPanel mainPanel, User user, File file) {
        super(appFrame, "Generate Flashcards", true);
        this.appFrame = appFrame;
        this.mainPanel = mainPanel;
        this.user = user;
        this.file = file;

        $$$setupUI$$$();
        setContentPane(root);
        cardLayout = (CardLayout) root.getLayout();

        root.setPreferredSize(null);
        settingsCard.setBorder(BorderFactory.createEmptyBorder(s(24), s(32), s(24), s(32)));
        loadingCard.setBorder(BorderFactory.createEmptyBorder(s(40), s(40), s(40), s(40)));

        styleUI();
        setupDefaults();

        cancelBtn.addActionListener(e -> dispose());
        generateBtn.addActionListener(e -> startGeneration());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (spinnerTimer != null) spinnerTimer.stop();
                if (generationWorker != null && !generationWorker.isDone()) {
                    generationWorker.cancel(true);
                }
            }
        });

        pack();
        setLocationRelativeTo(appFrame);
        setResizable(false);
    }

    private void styleUI() {
        Font baseFont = getBaseFont();
        root.setBackground(Color.WHITE);
        settingsCard.setBackground(Color.WHITE);
        loadingCard.setBackground(Color.WHITE);

        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, f(20f)));
        titleLbl.setForeground(Color.decode("#0f172a"));

        fileLbl.setFont(baseFont.deriveFont(Font.PLAIN, f(12f)));
        fileLbl.setForeground(Color.decode("#64748b"));

        deckTitleLbl.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        deckTitleLbl.setForeground(Color.decode("#334155"));

        tagsLbl.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        tagsLbl.setForeground(Color.decode("#334155"));

        amountLbl.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        amountLbl.setForeground(Color.decode("#334155"));

        titleField.setPreferredSize(new Dimension(-1, s(36)));
        titleField.setFont(baseFont.deriveFont(Font.PLAIN, f(13f)));
        titleField.setBorder(new RoundedFieldBorder(Color.decode("#cbd5e1"), s(8)));

        tagsField.setPreferredSize(new Dimension(-1, s(36)));
        tagsField.setFont(baseFont.deriveFont(Font.PLAIN, f(13f)));
        tagsField.setBorder(new RoundedFieldBorder(Color.decode("#cbd5e1"), s(8)));

        amountSpinner.setPreferredSize(new Dimension(-1, s(36)));
        amountSpinner.setFont(baseFont.deriveFont(Font.PLAIN, f(13f)));
        amountSpinner.setModel(new SpinnerNumberModel(10, 1, 50, 1));
        amountSpinner.setBorder(new RoundedFieldBorder(Color.decode("#cbd5e1"), s(8)));

        cancelBtn.setPreferredSize(new Dimension(s(100), s(36)));
        generateBtn.setPreferredSize(new Dimension(s(120), s(36)));
        styleOutlineBtn(cancelBtn, baseFont);
        stylePrimaryBtn(generateBtn, baseFont);

        loadingSub.setFont(baseFont.deriveFont(Font.BOLD, f(13f)));
        loadingSub.setForeground(Color.decode("#0f172a"));

        spinnerPanel.setPreferredSize(new Dimension(s(120), s(120)));
        spinnerPanel.setMinimumSize(new Dimension(s(120), s(120)));
        spinnerPanel.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int padding = s(8);
                int size = Math.min(c.getWidth(), c.getHeight()) - (padding * 2);
                int x = (c.getWidth() - size) / 2;
                int y = (c.getHeight() - size) / 2;

                g2.setColor(Color.decode("#f1f5f9"));
                g2.setStroke(new BasicStroke(s(6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(x, y, size, size, 0, 360);

                g2.setColor(Color.decode("#3b82f6"));
                g2.drawArc(x, y, size, size, spinnerAngle, 100);
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void setupDefaults() {
        fileLbl.setText("Selected File: " + file.getName());
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) name = name.substring(0, dotIndex);
        titleField.setText(name);
    }

    private void startGeneration() {
        String deckTitle = titleField.getText().trim();
        String tagsInput = tagsField.getText().trim();
        int cardCount = (int) amountSpinner.getValue();

        if (deckTitle.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a deck title.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final List<String> tagList = new ArrayList<>();
        if (!tagsInput.isEmpty()) {
            String[] splitTags = tagsInput.split(",");
            for (String t : splitTags) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    if (trimmed.length() > 30) {
                        JOptionPane.showMessageDialog(this, "Tags cannot exceed 30 characters.\n'" + trimmed + "' is too long.", "Tag Too Long", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (!tagList.contains(trimmed)) {
                        tagList.add(trimmed);
                    }
                }
            }

            if (tagList.size() > 6) {
                JOptionPane.showMessageDialog(this, "You can only add up to 6 tags. You entered " + tagList.size() + ".", "Too Many Tags", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        cardLayout.show(root, "LOADING");

        spinnerTimer = new Timer(15, e -> {
            spinnerAngle -= 6;
            if (spinnerAngle < 0) spinnerAngle += 360;
            spinnerPanel.repaint();
        });
        spinnerTimer.start();

        generationWorker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                DeckDAO deckDAO = new DeckDAO();
                if (deckDAO.isUserTitleTaken(user.getId(), deckTitle, -1)) {
                    throw new IllegalStateException("CONFLICT:You already have a deck with this name.\nPlease choose a different title.");
                }

                FileDAO fileDAO = new FileDAO();
                int existingFileId = fileDAO.getFileIdByName(user.getId(), file.getName());

                if (isCancelled()) return -1;

                publish("Extracting text from document...");
                String extractedText = FileTextExtractor.extract(file);

                if (extractedText == null || extractedText.trim().isEmpty()) {
                    throw new Exception("The uploaded file appears to be empty or contains no readable text.");
                }

                if (isCancelled()) return -1;

                publish("Generating flashcards...");
                GeminiService gemini = new GeminiService();
                List<Card> generatedCards = gemini.generateCards(extractedText, cardCount);

                if (generatedCards == null || generatedCards.isEmpty()) {
                    throw new Exception("Could not generate any flashcards from this document. The content might be too short or lacks factual information.");
                }

                if (isCancelled()) return -1;

                int finalFileId = existingFileId;

                if (existingFileId <= 0) {
                    publish("Saving...");
                    String ext = "";
                    int dotIndex = file.getName().lastIndexOf('.');
                    if (dotIndex > 0) ext = file.getName().substring(dotIndex);

                    byte[] fileData = Files.readAllBytes(file.toPath());
                    long fileSize = file.length();

                    finalFileId = fileDAO.logFile(user.getId(), file.getName(), ext, fileSize, fileData);
                } else {
                    publish("Linking to existing file...");
                }

                if (isCancelled()) return -1;

                int deckId = deckDAO.createDeck(user.getId(), (finalFileId > 0 ? finalFileId : null), deckTitle, "Generated from " + file.getName(), false);

                if (deckId > 0 && !tagList.isEmpty()) {
                    deckDAO.updateDeckTags(deckId, tagList);
                }

                CardDAO cardDAO = new CardDAO();
                cardDAO.addCardsBatch(deckId, generatedCards);

                return generatedCards.size();
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) loadingSub.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                if (spinnerTimer != null) spinnerTimer.stop();
                if (isCancelled()) return;

                try {
                    int actualCount = get();
                    dispose();

                    String successMsg = "Successfully generated " + actualCount + " flashcards!";

                    if (actualCount < cardCount) {
                        successMsg += "\n\n(Note: You requested " + cardCount + " cards, but the document was too short or lacked enough factual data to generate that many unique questions.)";
                        new InfoDialog(appFrame, "Partial Success", successMsg).setVisible(true);
                    } else {
                        new SuccessDialog(appFrame, "Success", successMsg).setVisible(true);
                    }

                    mainPanel.refreshCurrentScreen();

                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String errorMsg = cause.getMessage() != null ? cause.getMessage() : "An unexpected error occurred.";

                    if (errorMsg.startsWith("CONFLICT:")) {
                        cardLayout.show(root, "SETTINGS");
                        new InfoDialog(GenerationDialog.this, "Conflict", errorMsg.substring(9)).setVisible(true);
                    } else {
                        cardLayout.show(root, "SETTINGS");
                        new InfoDialog(GenerationDialog.this, "Error", "Error generating cards:\n" + errorMsg).setVisible(true);
                    }
                }
            }
        };
        generationWorker.execute();
    }

    private void stylePrimaryBtn(JButton btn, Font baseFont) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        btn.setForeground(Color.WHITE);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Color.decode("#3b82f6"), c.getWidth(), c.getHeight(), Color.decode("#1d4ed8")));
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), s(8), s(8)));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void styleOutlineBtn(JButton btn, Font baseFont) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, f(12f)));
        btn.setForeground(Color.decode("#64748b"));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), s(8), s(8)));
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(2))));
                g2.draw(new RoundRectangle2D.Double(1, 1, c.getWidth() - 2, c.getHeight() - 2, s(6), s(6)));
                g2.dispose();
                super.paint(g, c);
            }
        });
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
        root.setPreferredSize(new Dimension(450, 420));
        settingsCard = new JPanel();
        settingsCard.setLayout(new GridBagLayout());
        settingsCard.setBackground(new Color(-1));
        root.add(settingsCard, "SETTINGS");
        settingsCard.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        titleLbl = new JLabel();
        titleLbl.setText("Generate Flashcards");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 4, 0);
        settingsCard.add(titleLbl, gbc);
        fileLbl = new JLabel();
        fileLbl.setText("Selected File: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 24, 0);
        settingsCard.add(fileLbl, gbc);
        deckTitleLbl = new JLabel();
        deckTitleLbl.setText("Deck Title");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 6, 0);
        settingsCard.add(deckTitleLbl, gbc);
        titleField = new JTextField();
        titleField.setPreferredSize(new Dimension(-1, 36));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 16, 0);
        settingsCard.add(titleField, gbc);
        tagsLbl = new JLabel();
        tagsLbl.setText("Tags (comma separated, optional)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 6, 0);
        settingsCard.add(tagsLbl, gbc);
        tagsField = new JTextField();
        tagsField.setPreferredSize(new Dimension(-1, 36));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 16, 0);
        settingsCard.add(tagsField, gbc);
        amountLbl = new JLabel();
        amountLbl.setText("Number of Cards to Generate");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 6, 0);
        settingsCard.add(amountLbl, gbc);
        amountSpinner = new JSpinner();
        amountSpinner.setPreferredSize(new Dimension(-1, 36));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 24, 0);
        settingsCard.add(amountSpinner, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panel1.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        settingsCard.add(panel1, gbc);
        cancelBtn = new JButton();
        cancelBtn.setPreferredSize(new Dimension(100, 36));
        cancelBtn.setText("Cancel");
        panel1.add(cancelBtn);
        generateBtn = new JButton();
        generateBtn.setPreferredSize(new Dimension(120, 36));
        generateBtn.setText("Generate");
        panel1.add(generateBtn);
        loadingCard = new JPanel();
        loadingCard.setLayout(new GridBagLayout());
        loadingCard.setBackground(new Color(-1));
        root.add(loadingCard, "LOADING");
        loadingCard.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new BorderLayout(0, 0));
        spinnerPanel.setMinimumSize(new Dimension(80, 80));
        spinnerPanel.setOpaque(false);
        spinnerPanel.setPreferredSize(new Dimension(80, 80));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 24, 0);
        loadingCard.add(spinnerPanel, gbc);
        loadingSub = new JLabel();
        loadingSub.setHorizontalAlignment(0);
        loadingSub.setText("Initializing...");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        loadingCard.add(loadingSub, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

    // Moved outside as it requires ImageUtil scaling methods cleanly
    class RoundedFieldBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        RoundedFieldBorder(Color c, int r) {
            color = c;
            radius = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(2))));
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(s(6), s(10), s(6), s(10));
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.top = i.bottom = s(6);
            i.left = i.right = s(10);
            return i;
        }
    }
}