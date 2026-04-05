package com.flipit.ui.panels.moderator;

import com.flipit.dao.DeckDAO;
import com.flipit.dao.ReportDAO;
import com.flipit.models.DeckReport;
import com.flipit.models.User;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.dialogs.SuccessDialog;
import com.flipit.ui.dialogs.WarningDialog;
import com.flipit.ui.panels.MainPanel;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ReportsPanel extends JPanel {
    private JPanel root;
    private JPanel topBarPanel;
    private JPanel searchPanel;
    private JLabel searchLabel;
    private JTextField searchField;
    private JComboBox<String> sortComboBox;
    private JButton refreshButton;
    private JPanel gridWrapperPanel;
    private JPanel reportListPanel;

    private MainPanel mainPanel;
    private User user;

    private final ReportDAO reportDAO = new ReportDAO();
    private final DeckDAO deckDAO = new DeckDAO();

    private boolean isUpdatingPlaceholder = false;
    private SwingWorker<List<DeckReport>, Void> reportLoaderWorker;
    private final Timer searchDebounceTimer;

    private List<DeckReport> currentDisplayedReports = new ArrayList<>();

    private List<DeckReport> cachedReports = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30_000;

    public ReportsPanel() {
        searchDebounceTimer = new Timer(300, e -> loadReports());
        searchDebounceTimer.setRepeats(false);
    }

    public ReportsPanel(MainPanel mainPanel, User user) {
        this.mainPanel = mainPanel;
        this.user = user;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        searchDebounceTimer = new Timer(300, e -> loadReports());
        searchDebounceTimer.setRepeats(false);

        styleAll();

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

        sortComboBox.addActionListener(e -> loadReports());
        refreshButton.addActionListener(e -> refresh());

        loadReports();
    }

    public void clearCache() {
        cachedReports = null;
    }

    private void styleAll() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        root.setBackground(Color.decode("#f0f2f8"));

        topBarPanel.setPreferredSize(null);
        topBarPanel.setBackground(Color.decode("#f0f2f8"));
        topBarPanel.setOpaque(false);

        gridWrapperPanel.setBackground(Color.decode("#f0f2f8"));
        reportListPanel.setBackground(Color.decode("#f0f2f8"));

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
        searchField.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(12), 0, ImageUtil.scale(12)));

        searchField.setText("Search by deck, user, or reason...");
        searchField.setForeground(Color.decode("#8792a8"));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                if (searchField.getText().equals("Search by deck, user, or reason...")) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("");
                    searchField.setForeground(Color.decode("#1a1f36"));
                    isUpdatingPlaceholder = false;
                }
            }

            public void focusLost(FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("Search by deck, user, or reason...");
                    searchField.setForeground(Color.decode("#8792a8"));
                    isUpdatingPlaceholder = false;
                }
            }
        });

        sortComboBox.setPreferredSize(new Dimension(ImageUtil.scale(184), ImageUtil.scale(50)));
        sortComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Oldest First", "Newest First"
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

        root.setFocusable(true);
        MouseAdapter clearFocus = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                root.requestFocusInWindow();
            }
        };
        root.addMouseListener(clearFocus);
        reportListPanel.addMouseListener(clearFocus);
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
        if (currentDisplayedReports != null) {
            renderList(currentDisplayedReports, sortComboBox.getSelectedIndex());
        }
    }

    private void loadReports() {
        if (reportListPanel == null) return;

        if (reportLoaderWorker != null && !reportLoaderWorker.isDone()) {
            reportLoaderWorker.cancel(true);
        }

        String q = searchField == null ? "" : searchField.getText().toLowerCase().trim();
        if (q.equals("search by deck, user, or reason...")) q = "";
        final String searchQuery = q;
        final int sortIdx = sortComboBox.getSelectedIndex();

        if (searchQuery.isEmpty() && cachedReports != null && (System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS)) {
            currentDisplayedReports = new ArrayList<>(cachedReports);
            renderList(currentDisplayedReports, sortIdx);
            return;
        }

        reportListPanel.removeAll();
        reportListPanel.setLayout(new BoxLayout(reportListPanel, BoxLayout.Y_AXIS));
        reportListPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(15), ImageUtil.scale(40), ImageUtil.scale(15), ImageUtil.scale(40)));
        for (int i = 0; i < 4; i++) {
            reportListPanel.add(buildSkeletonRow());
            reportListPanel.add(Box.createVerticalStrut(ImageUtil.scale(12)));
        }
        reportListPanel.revalidate();
        reportListPanel.repaint();

        reportLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<DeckReport> doInBackground() throws Exception {
                List<DeckReport> reports = reportDAO.getPendingReports();
                if (isCancelled()) return null;

                List<DeckReport> filtered = new ArrayList<>();
                for (DeckReport r : reports) {
                    if (searchQuery.isEmpty() ||
                            (r.getDeckTitle() != null && r.getDeckTitle().toLowerCase().contains(searchQuery)) ||
                            (r.getReporterName() != null && r.getReporterName().toLowerCase().contains(searchQuery)) ||
                            (r.getReason() != null && r.getReason().toLowerCase().contains(searchQuery))) {
                        filtered.add(r);
                    }
                }
                return filtered;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<DeckReport> filtered = get();
                    if (filtered == null) return;

                    currentDisplayedReports = new ArrayList<>(filtered);

                    if (searchQuery.isEmpty()) {
                        cachedReports = new ArrayList<>(filtered);
                        cacheTimestamp = System.currentTimeMillis();
                    }

                    renderList(currentDisplayedReports, sortIdx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        reportLoaderWorker.execute();
    }

    private void renderList(List<DeckReport> listToRender, int sortIdx) {
        if (listToRender == null) return;

        listToRender.sort((r1, r2) -> {
            if (sortIdx == 1) return r2.getCreatedAt().compareTo(r1.getCreatedAt());
            return r1.getCreatedAt().compareTo(r2.getCreatedAt());
        });

        reportListPanel.removeAll();
        reportListPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(15), ImageUtil.scale(40), ImageUtil.scale(15), ImageUtil.scale(40)));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        if (listToRender.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(40), ImageUtil.scale(40), ImageUtil.scale(40), ImageUtil.scale(40)));

            JLabel msg = new JLabel("No pending reports.");
            msg.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));
            msg.setForeground(Color.decode("#64748b"));
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(msg);
            reportListPanel.add(empty);
        } else {
            for (DeckReport r : listToRender) {
                JPanel row = buildReportRow(r, baseFont, sf);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                reportListPanel.add(row);
                reportListPanel.add(Box.createVerticalStrut(ImageUtil.scale(12)));
            }
        }

        reportListPanel.revalidate();
        reportListPanel.repaint();
    }

    private JPanel buildSkeletonRow() {
        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int rad = ImageUtil.scale(12);
                int radSm = ImageUtil.scale(6);
                int radXSm = ImageUtil.scale(4);

                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 2, rad, rad));
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1.5f, 1.5f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 2, rad - 1, rad - 1));

                g2.setColor(Color.decode("#f1f5f9"));
                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(20), ImageUtil.scale(180), ImageUtil.scale(16), radSm, radSm);
                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(45), ImageUtil.scale(120), ImageUtil.scale(12), radXSm, radXSm);

                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(75), getWidth() - ImageUtil.scale(40), ImageUtil.scale(12), radXSm, radXSm);
                g2.fillRoundRect(ImageUtil.scale(20), ImageUtil.scale(95), getWidth() - ImageUtil.scale(140), ImageUtil.scale(12), radXSm, radXSm);

                g2.fillRoundRect(getWidth() - ImageUtil.scale(280), ImageUtil.scale(130), ImageUtil.scale(110), ImageUtil.scale(32), radSm, radSm);
                g2.fillRoundRect(getWidth() - ImageUtil.scale(160), ImageUtil.scale(130), ImageUtil.scale(140), ImageUtil.scale(32), radSm, radSm);

                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(180)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(180)));
        return row;
    }

    private String escapeHtmlAndNewlines(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private JPanel buildReportRow(DeckReport report, Font baseFont, float sf) {
        JPanel card = new JPanel(new BorderLayout(0, ImageUtil.scale(12))) {
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
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 2, ImageUtil.scale(12), ImageUtil.scale(12)));
                g2.setColor(hov ? Color.decode("#3b82f6") : Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1.5f, 1.5f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 2, ImageUtil.scale(11), ImageUtil.scale(11)));
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(20), ImageUtil.scale(20), ImageUtil.scale(20), ImageUtil.scale(20)));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, ImageUtil.scale(4)));
        header.setOpaque(false);

        JLabel deckLbl = new JLabel(report.getDeckTitle());
        deckLbl.setFont(baseFont.deriveFont(Font.BOLD, 15f * sf));
        deckLbl.setForeground(Color.decode("#0f172a"));

        String dateStr = new SimpleDateFormat("MMM dd, yyyy HH:mm").format(report.getCreatedAt());
        JLabel reporterLbl = new JLabel("Reported by " + report.getReporterName() + " on " + dateStr);
        reporterLbl.setFont(baseFont.deriveFont(Font.PLAIN, 12f * sf));
        reporterLbl.setForeground(Color.decode("#64748b"));

        header.add(deckLbl);
        header.add(reporterLbl);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, ImageUtil.scale(4), 0, 0, Color.decode("#cbd5e1")),
                BorderFactory.createEmptyBorder(ImageUtil.scale(4), ImageUtil.scale(12), ImageUtil.scale(4), ImageUtil.scale(12))
        ));

        String safeReason = escapeHtmlAndNewlines(report.getReason());
        String fontFamily = baseFont.getFamily();
        JLabel reasonLbl = new JLabel("<html><div style='width: " + ImageUtil.scale(450) + "px; font-family: \"" + fontFamily + "\"; font-size: " + (int) (12 * sf) + "px; color: #334155; line-height: 1.4;'>" + safeReason + "</div></html>");
        body.add(reasonLbl, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ImageUtil.scale(10), 0));
        footer.setOpaque(false);

        JButton dismissBtn = outlineButton("Dismiss", baseFont, sf);
        JButton disableBtn = dangerButton("Disable Deck", baseFont, sf);

        dismissBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            WarningDialog dialog = new WarningDialog(parentWindow, "Confirm Dismiss",
                    "Are you sure you want to dismiss this report?", "Dismiss");
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                if (currentDisplayedReports != null) currentDisplayedReports.removeIf(r -> r.getId() == report.getId());
                if (cachedReports != null) cachedReports.removeIf(r -> r.getId() == report.getId());
                executeOptimisticUpdate();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        reportDAO.updateReportStatus(report.getId(), "dismissed");
                        return null;
                    }

                    @Override
                    protected void done() {
                    }
                }.execute();

                new SuccessDialog(parentWindow, "Report Dismissed", "The report has been successfully dismissed.").setVisible(true);
            }
        });

        disableBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            WarningDialog dialog = new WarningDialog(parentWindow, "Disable Deck",
                    "Disable this deck?\nThis will make it invisible to the public instantly.", "Disable");
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                if (currentDisplayedReports != null) currentDisplayedReports.removeIf(r -> r.getId() == report.getId());
                if (cachedReports != null) cachedReports.removeIf(r -> r.getId() == report.getId());
                executeOptimisticUpdate();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        deckDAO.setDeckDisabled(report.getDeckId(), true);
                        reportDAO.updateReportStatus(report.getId(), "resolved");
                        return null;
                    }

                    @Override
                    protected void done() {
                        mainPanel.invalidateDeckCaches();
                    }
                }.execute();

                new SuccessDialog(parentWindow, "Deck Disabled", "The deck has been successfully disabled and report resolved.").setVisible(true);
            }
        });

        footer.add(dismissBtn);
        footer.add(disableBtn);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JButton outlineButton(String text, Font baseFont, float sf) {
        JButton btn = new JButton(text);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int rad = ImageUtil.scale(8);
                int radSm = ImageUtil.scale(6);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), rad, rad);
                g2.setColor(Color.decode("#94a3b8"));
                g2.setStroke(new BasicStroke(Math.max(1.5f, 1.5f * ImageUtil.getScaleFactor())));
                g2.drawRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, radSm, radSm);

                g2.setFont(c.getFont());
                g2.setColor(Color.decode("#475569"));
                FontMetrics fm = g2.getFontMetrics();
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
        btn.setPreferredSize(new Dimension(ImageUtil.scale(100), ImageUtil.scale(38)));
        return btn;
    }

    private JButton dangerButton(String text, Font baseFont, float sf) {
        JButton btn = new JButton(text) {
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
                g2.setPaint(new GradientPaint(0, 0, hov ? Color.decode("#dc2626") : Color.decode("#ef4444"),
                        getWidth(), getHeight(),
                        hov ? Color.decode("#b91c1c") : Color.decode("#dc2626")));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ImageUtil.scale(8), ImageUtil.scale(8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
        btn.setForeground(Color.WHITE);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(ImageUtil.scale(160), ImageUtil.scale(38)));
        return btn;
    }

    public void refresh() {
        clearCache();

        if (searchField != null) {
            isUpdatingPlaceholder = true;
            searchField.setText("Search by deck, user, or reason...");
            searchField.setForeground(Color.decode("#8792a8"));
            isUpdatingPlaceholder = false;
        }
        SwingUtilities.invokeLater(this::loadReports);
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
        topBarPanel = new JPanel();
        topBarPanel.setLayout(new GridBagLayout());
        topBarPanel.setPreferredSize(new Dimension(-1, 65));
        root.add(topBarPanel, BorderLayout.NORTH);
        topBarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
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
        sortComboBox.setPreferredSize(new Dimension(184, 50));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 12);
        topBarPanel.add(sortComboBox, gbc);
        refreshButton = new JButton();
        refreshButton.setPreferredSize(new Dimension(50, 50));
        refreshButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topBarPanel.add(refreshButton, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, BorderLayout.CENTER);
        gridWrapperPanel = new JPanel();
        gridWrapperPanel.setLayout(new BorderLayout(0, 0));
        scrollPane1.setViewportView(gridWrapperPanel);
        gridWrapperPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        reportListPanel = new JPanel();
        reportListPanel.setLayout(new GridBagLayout());
        gridWrapperPanel.add(reportListPanel, BorderLayout.NORTH);
        reportListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}