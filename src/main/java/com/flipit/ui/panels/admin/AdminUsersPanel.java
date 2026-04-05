package com.flipit.ui.panels.admin;

import com.flipit.dao.UserDAO;
import com.flipit.models.User;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.dialogs.WarningDialog;
import com.flipit.ui.panels.MainPanel;
import com.flipit.util.IconUtil;
import com.flipit.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AdminUsersPanel extends JPanel {
    private JPanel root;
    private JPanel bodyPanel;
    private JPanel topBarPanel;
    private JPanel searchPanel;
    private JLabel searchLabel;
    private JTextField searchField;
    private JComboBox<String> sortComboBox;
    private JButton refreshButton;
    private JScrollPane tableScrollPane;
    private JTable userTable;
    private JPanel actionPanel;
    private JButton changeRoleBtn;
    private JButton toggleStatusBtn;

    private JPanel centerCardPanel;
    private JPanel skeletonPanel;

    private MainPanel mainPanel;
    private User adminUser;
    private final UserDAO userDAO = new UserDAO();
    private DefaultTableModel tableModel;

    private boolean isUpdatingPlaceholder = false;
    private SwingWorker<List<User>, Void> userLoaderWorker;
    private final Timer searchDebounceTimer;

    private List<User> cachedUsers = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30_000;

    public AdminUsersPanel(MainPanel mainPanel, User adminUser) {
        this.mainPanel = mainPanel;
        this.adminUser = adminUser;

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        searchDebounceTimer = new Timer(300, e -> loadUsers());
        searchDebounceTimer.setRepeats(false);

        setupTable();
        styleAll();
        bindEvents();

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

        loadUsers();
    }

    public void clearCache() {
        cachedUsers = null;
    }

    private void setupTable() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        String[] columns = {"ID", "Username", "Role", "Status", "Registered"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable.setModel(tableModel);

        userTable.setRowHeight(ImageUtil.scale(42));
        userTable.setFont(baseFont.deriveFont(Font.PLAIN, 15.5f * sf));
        userTable.setForeground(Color.decode("#334155"));
        userTable.setShowGrid(true);
        userTable.setGridColor(Color.decode("#e2e8f0"));
        userTable.setSelectionBackground(Color.decode("#dbeafe"));
        userTable.setSelectionForeground(Color.decode("#0f172a"));

        JTableHeader tableHeader = userTable.getTableHeader();
        tableHeader.setFont(baseFont.deriveFont(Font.BOLD, 14.5f * sf));
        tableHeader.setBackground(Color.decode("#f1f5f9"));
        tableHeader.setForeground(Color.decode("#475569"));
        tableHeader.setBorder(BorderFactory.createLineBorder(Color.decode("#cbd5e1"), 1));
        tableHeader.setPreferredSize(new Dimension(ImageUtil.scale(100), ImageUtil.scale(42)));

        Font finalBaseFont = baseFont;
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, ImageUtil.scale(12), 0, ImageUtil.scale(12)));

                if (column == 3) {
                    setFont(finalBaseFont.deriveFont(Font.BOLD, 14.5f * sf));
                    if ("ACTIVE".equals(value)) setForeground(Color.decode("#059669"));
                    else setForeground(Color.decode("#dc2626"));
                } else {
                    setFont(finalBaseFont.deriveFont(Font.PLAIN, 15.5f * sf));
                    setForeground(isSelected ? Color.decode("#0f172a") : Color.decode("#334155"));
                }
                return c;
            }
        };

        for (int i = 0; i < userTable.getColumnCount(); i++) {
            userTable.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }
        userTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
    }

    private void styleAll() {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        root.setBackground(Color.decode("#f0f2f8"));
        bodyPanel.setBackground(Color.decode("#f0f2f8"));
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));

        topBarPanel.setPreferredSize(null);
        topBarPanel.setBackground(Color.decode("#f0f2f8"));
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

        searchField.setText("Search by ID or username...");
        searchField.setForeground(Color.decode("#8792a8"));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                if (searchField.getText().equals("Search by ID or username...")) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("");
                    searchField.setForeground(Color.decode("#1a1f36"));
                    isUpdatingPlaceholder = false;
                }
            }

            public void focusLost(FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    isUpdatingPlaceholder = true;
                    searchField.setText("Search by ID or username...");
                    searchField.setForeground(Color.decode("#8792a8"));
                    isUpdatingPlaceholder = false;
                }
            }
        });

        sortComboBox.setPreferredSize(new Dimension(ImageUtil.scale(184), ImageUtil.scale(50)));
        sortComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "ID (Ascending)", "ID (Descending)", "Name (A-Z)", "Name (Z-A)",
                "Role: Admins Only", "Role: Moderators Only", "Role: Users Only"
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

        bodyPanel.remove(tableScrollPane);
        centerCardPanel = new JPanel(new CardLayout());
        centerCardPanel.setOpaque(false);

        skeletonPanel = new JPanel();
        skeletonPanel.setLayout(new BoxLayout(skeletonPanel, BoxLayout.Y_AXIS));
        skeletonPanel.setOpaque(false);

        centerCardPanel.add(tableScrollPane, "TABLE");
        centerCardPanel.add(skeletonPanel, "SKELETON");

        bodyPanel.add(centerCardPanel, BorderLayout.CENTER);

        tableScrollPane.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e5f0"), 1));
        tableScrollPane.getViewport().setBackground(Color.WHITE);

        actionPanel.setBackground(Color.decode("#f0f2f8"));
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, ImageUtil.scale(12), 0));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(18), 0, 0, 0));

        styleSolidButton(changeRoleBtn, Color.decode("#f59e0b"), Color.WHITE, baseFont, sf);
        styleSolidButton(toggleStatusBtn, Color.decode("#ef4444"), Color.WHITE, baseFont, sf);

        updateButtonStates();
    }

    private void styleSolidButton(JButton btn, Color bg, Color fg, Font baseFont, float sf) {
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setForeground(fg);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 14.5f * sf));
        btn.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(10), ImageUtil.scale(20), ImageUtil.scale(10), ImageUtil.scale(20)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int rad = ImageUtil.scale(8);

                if (!btn.isEnabled()) {
                    g2.setColor(Color.decode("#f1f5f9"));
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), rad, rad);
                    g2.setColor(Color.decode("#cbd5e1"));
                } else {
                    if (btn.getModel().isPressed()) {
                        g2.setColor(bg.darker());
                    } else if (btn.getModel().isRollover()) {
                        g2.setColor(new Color(
                                Math.max(bg.getRed() - 20, 0),
                                Math.max(bg.getGreen() - 20, 0),
                                Math.max(bg.getBlue() - 20, 0)
                        ));
                    } else {
                        g2.setColor(bg);
                    }
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), rad, rad);
                    g2.setColor(bg.darker());
                }
                g2.setStroke(new BasicStroke(Math.max(1f, 1.5f * ImageUtil.getScaleFactor())));
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, rad, rad);
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void loadUsers() {
        if (userLoaderWorker != null && !userLoaderWorker.isDone()) {
            userLoaderWorker.cancel(true);
        }

        String q = searchField == null ? "" : searchField.getText().toLowerCase().trim();
        if (q.equals("search by id or username...")) q = "";
        final String searchQuery = q;
        final int sortIdx = sortComboBox.getSelectedIndex();

        skeletonPanel.removeAll();
        JPanel headerSkel = new JPanel() {
            protected void paintComponent(Graphics g) {
                g.setColor(Color.decode("#f8fafc"));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.decode("#e2e5f0"));
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        headerSkel.setPreferredSize(new Dimension(0, ImageUtil.scale(42)));
        headerSkel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(42)));
        skeletonPanel.add(headerSkel);

        for (int i = 0; i < 8; i++) {
            skeletonPanel.add(buildSkeletonRow());
        }
        skeletonPanel.revalidate();
        skeletonPanel.repaint();

        CardLayout cl = (CardLayout) centerCardPanel.getLayout();
        cl.show(centerCardPanel, "SKELETON");

        userLoaderWorker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                List<User> allUsers;

                // OPTIMIZATION: Only hit database if cache is missing/expired
                if (cachedUsers != null && (System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS)) {
                    allUsers = cachedUsers;
                } else {
                    allUsers = userDAO.getAllUsers();
                    if (isCancelled()) return null;
                    cachedUsers = allUsers;
                    cacheTimestamp = System.currentTimeMillis();
                }

                // Apply Filters and Search
                List<User> filtered = new ArrayList<>();
                for (User u : allUsers) {
                    boolean matchesSearch = searchQuery.isEmpty() ||
                            u.getUsername().toLowerCase().contains(searchQuery) ||
                            String.valueOf(u.getId()).contains(searchQuery);

                    boolean matchesRole = true;
                    if (sortIdx == 4 && !u.getRole().equalsIgnoreCase("admin")) matchesRole = false;
                    if (sortIdx == 5 && !u.getRole().equalsIgnoreCase("moderator")) matchesRole = false;
                    if (sortIdx == 6 && !u.getRole().equalsIgnoreCase("user")) matchesRole = false;

                    if (matchesSearch && matchesRole) {
                        filtered.add(u);
                    }
                }
                return filtered;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<User> filtered = get();
                    if (filtered == null) return;
                    renderList(filtered, sortIdx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        userLoaderWorker.execute();
    }

    private void renderList(List<User> listToRender, int sortIdx) {
        listToRender.sort((u1, u2) -> {
            if (sortIdx == 1) return Integer.compare(u2.getId(), u1.getId());
            if (sortIdx == 2) return u1.getUsername().compareToIgnoreCase(u2.getUsername());
            if (sortIdx == 3) return u2.getUsername().compareToIgnoreCase(u1.getUsername());
            // Default, and fallback for 4, 5, 6
            return Integer.compare(u1.getId(), u2.getId());
        });

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        tableModel.setRowCount(0);
        for (User u : listToRender) {
            String regDate = u.getCreatedAt() != null ? sdf.format(u.getCreatedAt()) : "N/A";

            // Capitalize the first letter of the role for a cleaner table look
            String displayRole = u.getRole().substring(0, 1).toUpperCase() + u.getRole().substring(1).toLowerCase();

            tableModel.addRow(new Object[]{u.getId(), u.getUsername(), displayRole, u.isActive() ? "ACTIVE" : "DISABLED", regDate});
        }

        CardLayout cl = (CardLayout) centerCardPanel.getLayout();
        cl.show(centerCardPanel, "TABLE");
    }

    private JPanel buildSkeletonRow() {
        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.decode("#e2e5f0"));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);

                g2.setColor(Color.decode("#f1f5f9"));
                int smRad = ImageUtil.scale(5);
                g2.fillRoundRect(ImageUtil.scale(24), ImageUtil.scale(15), ImageUtil.scale(36), ImageUtil.scale(14), smRad, smRad);
                g2.fillRoundRect(ImageUtil.scale(144), ImageUtil.scale(15), ImageUtil.scale(120), ImageUtil.scale(14), smRad, smRad);
                g2.fillRoundRect(ImageUtil.scale(420), ImageUtil.scale(15), ImageUtil.scale(72), ImageUtil.scale(14), smRad, smRad);
                g2.fillRoundRect(ImageUtil.scale(600), ImageUtil.scale(15), ImageUtil.scale(96), ImageUtil.scale(14), smRad, smRad);
                g2.fillRoundRect(ImageUtil.scale(760), ImageUtil.scale(15), ImageUtil.scale(100), ImageUtil.scale(14), smRad, smRad);

                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, ImageUtil.scale(42)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ImageUtil.scale(42)));
        return row;
    }

    private void bindEvents() {
        sortComboBox.addActionListener(e -> loadUsers());
        refreshButton.addActionListener(e -> refresh());

        changeRoleBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row == -1) return;
            int userId = (int) tableModel.getValueAt(row, 0);
            String currentRole = (String) tableModel.getValueAt(row, 2);
            Window parentWindow = SwingUtilities.getWindowAncestor(this);

            if (currentRole.equalsIgnoreCase("admin") || userId == adminUser.getId()) {
                new InfoDialog(parentWindow, "Action Denied", "Admin roles cannot be changed.").setVisible(true);
                return;
            }

            String lowerCurrentRole = currentRole.toLowerCase();
            String newRole = lowerCurrentRole.equals("user") ? "moderator" : "user";
            String displayNewRole = newRole.substring(0, 1).toUpperCase() + newRole.substring(1);

            WarningDialog dialog = new WarningDialog(parentWindow, "Change Role",
                    "Are you sure you want to change this user's role to " + displayNewRole + "?", "Change Role");
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                if (userDAO.updateUserRole(userId, newRole)) {
                    cachedUsers = null;
                    SwingUtilities.invokeLater(this::refresh);
                } else {
                    new InfoDialog(parentWindow, "Database Error", "Failed to update role.").setVisible(true);
                }
            }
        });

        toggleStatusBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row == -1) return;
            int userId = (int) tableModel.getValueAt(row, 0);
            String currentStatus = (String) tableModel.getValueAt(row, 3);
            String role = (String) tableModel.getValueAt(row, 2);
            Window parentWindow = SwingUtilities.getWindowAncestor(this);

            if (userId == adminUser.getId()) {
                new InfoDialog(parentWindow, "Error", "You cannot disable your own account.").setVisible(true);
                return;
            }

            if (role.equalsIgnoreCase("admin")) {
                new InfoDialog(parentWindow, "Error", "Cannot disable another Admin.").setVisible(true);
                return;
            }

            boolean currentlyDisabled = currentStatus.equals("DISABLED");
            String action = currentlyDisabled ? "Activate" : "Disable";

            WarningDialog dialog = new WarningDialog(parentWindow, action + " User",
                    "Are you sure you want to " + action.toLowerCase() + " this user?", action);
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                userDAO.updateUserStatus(userId, currentlyDisabled);
                cachedUsers = null;
                SwingUtilities.invokeLater(this::refresh);
            }
        });
    }

    private void updateButtonStates() {
        boolean hasSelection = userTable.getSelectedRow() != -1;
        toggleStatusBtn.setEnabled(hasSelection);
        changeRoleBtn.setEnabled(hasSelection);
    }

    public void refresh() {
        clearCache(); // Force data to fetch completely fresh

        if (searchField != null) {
            isUpdatingPlaceholder = true;
            searchField.setText("Search by ID or username...");
            searchField.setForeground(Color.decode("#8792a8"));
            isUpdatingPlaceholder = false;
        }
        SwingUtilities.invokeLater(this::loadUsers);
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
        bodyPanel = new JPanel();
        bodyPanel.setLayout(new BorderLayout(0, 15));
        root.add(bodyPanel, BorderLayout.CENTER);
        bodyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        topBarPanel = new JPanel();
        topBarPanel.setLayout(new GridBagLayout());
        topBarPanel.setPreferredSize(new Dimension(-1, 65));
        bodyPanel.add(topBarPanel, BorderLayout.NORTH);
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
        tableScrollPane = new JScrollPane();
        bodyPanel.add(tableScrollPane, BorderLayout.CENTER);
        userTable = new JTable();
        tableScrollPane.setViewportView(userTable);
        actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bodyPanel.add(actionPanel, BorderLayout.SOUTH);
        changeRoleBtn = new JButton();
        changeRoleBtn.setText("Change Role");
        actionPanel.add(changeRoleBtn);
        toggleStatusBtn = new JButton();
        toggleStatusBtn.setText("Disable / Activate");
        actionPanel.add(toggleStatusBtn);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }
}