package com.flipit.ui.panels;

import com.flipit.dao.FileDAO;
import com.flipit.models.UploadedFile;
import com.flipit.models.User;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.dialogs.SuccessDialog;
import com.flipit.ui.dialogs.WarningDialog;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class FilesPanel extends JPanel {
    private JPanel root;
    private JPanel controlsPanel;
    private JPanel searchPanel;
    private JLabel searchLabel;
    private JPanel fileListPanel;
    private JTextField searchField;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> sortComboBox;
    private JButton refreshButton;

    private MainPanel mainPanel;
    private User user;
    private final FileDAO fileDAO = new FileDAO();

    private boolean isUpdatingPlaceholder = false;

    private SwingWorker<List<UploadedFile>, Void> fileLoaderWorker;
    private final Timer searchDebounceTimer;

    private List<UploadedFile> cachedFiles = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30_000;

    private static final BufferedImage ICON_DOC = ImageUtil.loadImage("/document.png");
    private static final Image ICON_DOWNLOAD = ImageUtil.loadImage("/download.png");
    private static final Image ICON_TRASH = ImageUtil.loadImage("/trash.png");

    public FilesPanel() {
        searchDebounceTimer = new Timer(300, e -> loadFiles());
        searchDebounceTimer.setRepeats(false);
    }

    public FilesPanel(MainPanel mainPanel, User user) {
        this.mainPanel = mainPanel;
        this.user = user;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        searchDebounceTimer = new Timer(300, e -> loadFiles());
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

        typeComboBox.addActionListener(e -> loadFiles());
        sortComboBox.addActionListener(e -> loadFiles());
        refreshButton.addActionListener(e -> refresh());

        loadFiles();
    }

    public void clearCache() {
        cachedFiles = null;
    }

    private void styleAll() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        root.setBackground(Color.decode("#f8fafc"));
        controlsPanel.setBackground(Color.decode("#f8fafc"));
        controlsPanel.setOpaque(false);
        controlsPanel.setPreferredSize(null);

        searchPanel.setPreferredSize(new Dimension(-1, ImageUtil.scale(50)));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(2))),
                BorderFactory.createEmptyBorder(0, ImageUtil.scale(8), 0, ImageUtil.scale(8))));

        int searchIconSize = ImageUtil.scale(18);
        searchLabel.setText("");
        searchLabel.setIcon(IconUtil.getIcon("SEARCH", Color.decode("#8792a8"), searchIconSize));
        searchLabel.setPreferredSize(new Dimension(searchIconSize + ImageUtil.scale(6), searchIconSize));

        searchField.setPreferredSize(new Dimension(-1, ImageUtil.scale(48)));
        searchField.setFont(baseFont.deriveFont(Font.PLAIN, 15f * sf));
        searchField.setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(4), 0, ImageUtil.scale(4)));

        searchField.setText("Search by file name...");
        searchField.setForeground(Color.decode("#94a3b8"));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                if (searchField.getText().equals("Search by file name...")) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("");
                    searchField.setForeground(Color.decode("#0f172a"));
                    isUpdatingPlaceholder = false;
                }
            }

            public void focusLost(FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("Search by file name...");
                    searchField.setForeground(Color.decode("#94a3b8"));
                    isUpdatingPlaceholder = false;
                }
            }
        });

        typeComboBox.setPreferredSize(new Dimension(ImageUtil.scale(200), ImageUtil.scale(50)));
        typeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "All File Types", "PDF Files", "Word Documents", "PowerPoint", "Text Files"
        }));

        sortComboBox.setPreferredSize(new Dimension(ImageUtil.scale(200), ImageUtil.scale(50)));
        sortComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Most Recent", "Oldest", "Name (A-Z)", "Name (Z-A)"
        }));

        for (JComboBox cb : new JComboBox[]{typeComboBox, sortComboBox}) {
            if (cb == null) continue;
            cb.setFont(baseFont.deriveFont(Font.BOLD, 14f * sf));
            cb.setBackground(Color.WHITE);
            cb.setForeground(Color.decode("#8792a8"));
            cb.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(2))));
            cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        refreshButton.setPreferredSize(new Dimension(ImageUtil.scale(50), ImageUtil.scale(50)));
        refreshButton.setBackground(Color.WHITE);
        refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(2))));
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
                refreshButton.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(2))));
                refreshButton.setIcon(IconUtil.getIcon("REFRESH", Color.decode("#8792a8"), ImageUtil.scale(18)));
            }
        });

        fileListPanel.setBackground(Color.decode("#f8fafc"));

        for (Component c : root.getComponents()) {
            if (c instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) c;
                scrollPane.getVerticalScrollBar().setUnitIncrement(ImageUtil.scale(20));
                scrollPane.getVerticalScrollBar().setBlockIncrement(ImageUtil.scale(64));
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
            }
        }
    }

    private void loadFiles() {
        if (fileListPanel == null) return;

        if (fileLoaderWorker != null && !fileLoaderWorker.isDone()) {
            fileLoaderWorker.cancel(true);
        }

        String q = searchField == null ? "" : searchField.getText().toLowerCase().trim();
        if (q.equals("search by file name...")) q = "";
        final String searchQuery = q;
        final int typeIdx = typeComboBox.getSelectedIndex();
        final int sortIdx = sortComboBox.getSelectedIndex();

        if (searchQuery.isEmpty() && typeIdx == 0 && cachedFiles != null && (System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS)) {
            renderList(new ArrayList<>(cachedFiles), sortIdx);
            return;
        }

        fileListPanel.removeAll();
        fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
        fileListPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(20), ImageUtil.scale(10), 0, ImageUtil.scale(10)));
        for (int i = 0; i < 4; i++) {
            fileListPanel.add(buildSkeletonRow());
            fileListPanel.add(Box.createVerticalStrut(ImageUtil.scale(10)));
        }
        fileListPanel.revalidate();
        fileListPanel.repaint();

        fileLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<UploadedFile> doInBackground() throws Exception {
                List<UploadedFile> dbFiles = fileDAO.getAllFilesByUser(user.getId());
                if (isCancelled()) return null;

                List<UploadedFile> filtered = new ArrayList<>();
                for (UploadedFile fe : dbFiles) {
                    if (!searchQuery.isEmpty() && !fe.getFileName().toLowerCase().contains(searchQuery)) continue;

                    String t = fe.getFileType().toLowerCase();
                    if (typeIdx == 1 && !t.contains("pdf")) continue;
                    if (typeIdx == 2 && !(t.contains("doc") || t.contains("docx"))) continue;
                    if (typeIdx == 3 && !(t.contains("ppt") || t.contains("pptx"))) continue;
                    if (typeIdx == 4 && !t.contains("txt")) continue;

                    filtered.add(fe);
                }

                return filtered;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<UploadedFile> filtered = get();
                    if (filtered == null) return;

                    if (searchQuery.isEmpty() && typeIdx == 0) {
                        cachedFiles = new ArrayList<>(filtered);
                        cacheTimestamp = System.currentTimeMillis();
                    }

                    renderList(filtered, sortIdx);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        fileLoaderWorker.execute();
    }

    private void renderList(List<UploadedFile> listToRender, int sortIdx) {
        listToRender.sort((f1, f2) -> {
            if (sortIdx == 1) return f1.getUploadedAt().compareTo(f2.getUploadedAt());
            if (sortIdx == 2) return f1.getFileName().compareToIgnoreCase(f2.getFileName());
            if (sortIdx == 3) return f2.getFileName().compareToIgnoreCase(f1.getFileName());
            return f2.getUploadedAt().compareTo(f1.getUploadedAt());
        });

        fileListPanel.removeAll();
        fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
        fileListPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(20), ImageUtil.scale(10), 0, ImageUtil.scale(10)));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        if (listToRender.isEmpty()) {
            JLabel empty = new JLabel("No files found.");
            empty.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));
            empty.setForeground(Color.decode("#94a3b8"));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            fileListPanel.add(empty);
        } else {
            for (UploadedFile fe : listToRender) {
                JPanel row = buildFileRow(fe, baseFont);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                fileListPanel.add(row);
                fileListPanel.add(Box.createVerticalStrut(ImageUtil.scale(12)));
            }
        }

        fileListPanel.revalidate();
        fileListPanel.repaint();
    }

    private JPanel buildSkeletonRow() {
        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = ImageUtil.scale(12);
                int innerArc = ImageUtil.scale(10);

                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
                g2.setColor(Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(2))));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, innerArc, innerArc));

                g2.setColor(Color.decode("#f1f5f9"));
                int smRad = ImageUtil.scale(4);
                int mdRad = ImageUtil.scale(6);
                int lgRad = ImageUtil.scale(9);

                g2.fillRoundRect(ImageUtil.scale(22), ImageUtil.scale(17), ImageUtil.scale(46), ImageUtil.scale(46), lgRad, lgRad);
                g2.fillRoundRect(ImageUtil.scale(86), ImageUtil.scale(24), ImageUtil.scale(172), ImageUtil.scale(15), smRad, smRad);
                g2.fillRoundRect(ImageUtil.scale(86), ImageUtil.scale(48), ImageUtil.scale(118), ImageUtil.scale(13), smRad, smRad);

                g2.fillRoundRect(getWidth() - ImageUtil.scale(100), ImageUtil.scale(21), ImageUtil.scale(38), ImageUtil.scale(38), mdRad, mdRad);
                g2.fillRoundRect(getWidth() - ImageUtil.scale(56), ImageUtil.scale(21), ImageUtil.scale(38), ImageUtil.scale(38), mdRad, mdRad);

                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(80)));
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(80)));
        return row;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private JButton createIconButton(Image iconImg, Color hoverBg) {
        JButton btn = new JButton() {
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

                if (hov) {
                    g2.setColor(hoverBg);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ImageUtil.scale(6), ImageUtil.scale(6)));
                }

                if (iconImg != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    int size = ImageUtil.scale(22);
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;

                    g2.drawImage(iconImg, x, y, size, size, null);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(ImageUtil.scale(40), ImageUtil.scale(40)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel buildFileRow(UploadedFile fe, Font baseFont) {
        JPanel row = new JPanel(new BorderLayout(ImageUtil.scale(15), 0)) {
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
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ImageUtil.scale(12), ImageUtil.scale(12)));
                g2.setColor(hov ? Color.decode("#3b82f6") : Color.decode("#e2e8f0"));
                g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(2))));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, ImageUtil.scale(10), ImageUtil.scale(10)));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(80)));
        row.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(18), ImageUtil.scale(22), ImageUtil.scale(18), ImageUtil.scale(22)));

        JPanel iconBox = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2.setColor(Color.decode("#3b82f6"));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ImageUtil.scale(9), ImageUtil.scale(9)));

                if (ICON_DOC != null) {
                    int targetW = ImageUtil.scale(26);
                    int targetH = ImageUtil.scale(26);
                    int x = (getWidth() - targetW) / 2;
                    int y = (getHeight() - targetH) / 2;
                    g2.drawImage(ICON_DOC, x, y, targetW, targetH, null);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(ImageUtil.scale(46), ImageUtil.scale(46)));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, ImageUtil.scale(4)));
        info.setOpaque(false);

        float sf = ImageUtil.getScaleFactor();

        JLabel nameLbl = new JLabel(fe.getFileName());
        nameLbl.setFont(baseFont.deriveFont(Font.BOLD, 15f * sf));
        nameLbl.setForeground(Color.decode("#0f172a"));

        String dateString = new SimpleDateFormat("MMM dd, yyyy").format(fe.getUploadedAt());
        String sizeString = formatFileSize(fe.getFileSize());

        JLabel dateLbl = new JLabel("Uploaded: " + dateString + "  •  " + sizeString);
        dateLbl.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
        dateLbl.setForeground(Color.decode("#64748b"));

        info.add(nameLbl);
        info.add(dateLbl);

        JButton downloadBtn = createIconButton(ICON_DOWNLOAD, Color.decode("#dcfce7"));
        downloadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save File");
            fc.setSelectedFile(new File(fe.getFileName()));

            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File target = fc.getSelectedFile();
                byte[] fileData = fileDAO.getFileData(fe.getId());

                Window parentWindow = SwingUtilities.getWindowAncestor(this);

                if (fileData != null) {
                    try {
                        Files.write(target.toPath(), fileData);
                        new SuccessDialog(parentWindow, "Success", "File downloaded successfully!").setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new InfoDialog(parentWindow, "Error", "Error saving file.").setVisible(true);
                    }
                } else {
                    new InfoDialog(parentWindow, "File Not Found", "This file is missing.").setVisible(true);
                }
            }
        });

        JButton delBtn = createIconButton(ICON_TRASH, Color.decode("#fee2e2"));
        delBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            WarningDialog dialog = new WarningDialog(parentWindow, "Delete File",
                    "Are you sure you want to delete this file?\nIt will be removed from your history and will no longer be available for download on any associated decks.\n\n(Your generated flashcards will NOT be deleted.)",
                    "Delete");
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                fileDAO.deleteFile(fe.getId());
                cachedFiles = null;
                refresh();
            }
        });

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ImageUtil.scale(6), 0));
        actionPanel.setOpaque(false);
        actionPanel.add(downloadBtn);
        actionPanel.add(delBtn);

        row.add(iconBox, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(actionPanel, BorderLayout.EAST);
        return row;
    }

    public void refresh() {
        clearCache();

        if (searchField != null) {
            isUpdatingPlaceholder = true;
            searchField.setText("Search by file name...");
            searchField.setForeground(Color.decode("#94a3b8"));
            isUpdatingPlaceholder = false;
        }
        SwingUtilities.invokeLater(this::loadFiles);
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
        controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridBagLayout());
        controlsPanel.setPreferredSize(new Dimension(-1, 65));
        root.add(controlsPanel, BorderLayout.NORTH);
        controlsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
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
        gbc.insets = new Insets(0, 0, 0, 12);
        controlsPanel.add(searchPanel, gbc);
        searchLabel = new JLabel();
        searchLabel.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 6);
        searchPanel.add(searchLabel, gbc);
        searchField = new JTextField();
        searchField.setForeground(new Color(-15788246));
        searchField.setPreferredSize(new Dimension(74, 48));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        searchPanel.add(searchField, gbc);
        typeComboBox = new JComboBox();
        typeComboBox.setPreferredSize(new Dimension(200, 50));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 12);
        controlsPanel.add(typeComboBox, gbc);
        sortComboBox = new JComboBox();
        sortComboBox.setPreferredSize(new Dimension(200, 50));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 12);
        controlsPanel.add(sortComboBox, gbc);
        refreshButton = new JButton();
        refreshButton.setPreferredSize(new Dimension(50, 50));
        refreshButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(refreshButton, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        scrollPane1.setViewportView(panel1);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        fileListPanel = new JPanel();
        fileListPanel.setLayout(new GridBagLayout());
        panel1.add(fileListPanel, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}