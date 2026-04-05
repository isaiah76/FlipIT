package com.flipit.ui.panels;

import com.flipit.dao.UserDAO;
import com.flipit.db.DBConnection;
import com.flipit.models.User;
import com.flipit.util.ImageUtil;
import com.flipit.util.PasswordUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SettingsPanel extends JPanel {
    private JPanel root;
    private JPanel headerPanel;
    private JButton backBtn;
    private JLabel titleLbl;
    private JScrollPane scrollPane;
    private JPanel bodyPanel;

    private JPanel profileCard;
    private JLabel profTitleLbl;
    private JTextField usernameField;
    private JLabel profMsgLbl;
    private JButton saveProfBtn;

    private JPanel avatarPreviewLbl;
    private JButton uploadAvatarBtn;
    private JButton removeAvatarBtn;

    private JPanel passwordCard;
    private JLabel passTitleLbl;
    private JPasswordField curPassField;
    private JPasswordField newPassField;
    private JPasswordField confPassField;
    private JLabel passMsgLbl;
    private JButton changePassBtn;

    private final MainPanel mainPanel;
    private final User user;

    private boolean canChangeUsername = true;

    private Image cachedRawAvatar = null;
    private byte[] pendingAvatarBytes = null;
    private boolean avatarRemoved = false;

    public SettingsPanel(MainPanel mainPanel, User user) {
        this.mainPanel = mainPanel;
        this.user = user;

        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleAll();
        populateData();
        bindEvents();
    }

    public void refresh() {
        profMsgLbl.setText(" ");
        passMsgLbl.setText(" ");
        curPassField.setText("");
        newPassField.setText("");
        confPassField.setText("");
        populateData();
    }

    private void populateData() {
        pendingAvatarBytes = null;
        avatarRemoved = false;

        usernameField.setText(user.getUsername());

        UserDAO dao = new UserDAO();
        byte[] avatarBytes = dao.getProfilePicture(user.getId());
        cachedRawAvatar = ImageUtil.getImageFromBytes(avatarBytes);
        avatarPreviewLbl.repaint();

        Timestamp lastChange = getLastUsernameChange(user.getId());
        if (lastChange != null) {
            long daysSince = ChronoUnit.DAYS.between(lastChange.toLocalDateTime(), LocalDateTime.now());
            if (daysSince < 7) {
                canChangeUsername = false;
                long daysLeft = 7 - daysSince;

                usernameField.setToolTipText("You can change your username again in " + daysLeft + " days.");
                usernameField.setEditable(false);
                styleField(usernameField, true);

                profMsgLbl.setForeground(Color.decode("#f59e0b"));
                profMsgLbl.setText("Username locked for " + daysLeft + " more days.");
            } else {
                canChangeUsername = true;
                usernameField.setToolTipText("You can update your username.");
                usernameField.setEditable(true);
                styleField(usernameField, false);
            }
        } else {
            canChangeUsername = true;
            usernameField.setToolTipText("You can update your username.");
            usernameField.setEditable(true);
            styleField(usernameField, false);
        }
    }

    private void styleAll() {
        Color bgTheme = Color.decode("#f8fafc");
        root.setBackground(bgTheme);
        headerPanel.setBackground(bgTheme);

        scrollPane.getViewport().setBackground(bgTheme);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        scrollPane.getVerticalScrollBar().setUnitIncrement(ImageUtil.scale(16));
        scrollPane.getVerticalScrollBar().setBlockIncrement(ImageUtil.scale(64));

        bodyPanel.setBackground(bgTheme);

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);
        float sf = ImageUtil.getScaleFactor();

        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setFont(baseFont.deriveFont(Font.BOLD, 14f * sf));
        backBtn.setForeground(Color.decode("#3b82f6"));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        titleLbl.setFont(baseFont.deriveFont(Font.BOLD, 26f * sf));

        styleCard(profileCard);
        styleCard(passwordCard);

        profTitleLbl.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));
        passTitleLbl.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));

        avatarPreviewLbl.setPreferredSize(new Dimension(ImageUtil.scale(50), ImageUtil.scale(50)));
        avatarPreviewLbl.setOpaque(false);
        avatarPreviewLbl.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                int w = c.getWidth();
                int h = c.getHeight();
                int size = Math.min(w, h);
                int x = (w - size) / 2;
                int y = (h - size) / 2;
                ImageUtil.paintSquareAvatar((Graphics2D) g, x, y, size, cachedRawAvatar, user.getInitials(), ImageUtil.scale(12));
            }
        });

        styleField(curPassField, false);
        styleField(newPassField, false);
        styleField(confPassField, false);

        stylePrimaryBtn(saveProfBtn, baseFont, sf);
        stylePrimaryBtn(changePassBtn, baseFont, sf);

        styleOutlineBtn(uploadAvatarBtn, baseFont, sf);
        styleDangerOutlineBtn(removeAvatarBtn, baseFont, sf);

        profMsgLbl.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
        passMsgLbl.setFont(baseFont.deriveFont(Font.PLAIN, 13f * sf));
    }

    private void styleOutlineBtn(JButton btn, Font baseFont, float sf) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
        btn.setForeground(Color.decode("#475569"));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new BasicButtonUI() {
            boolean hov = false;

            {
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        btn.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        btn.repaint();
                    }
                });
            }

            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int rad = ImageUtil.scale(6);
                g2.setColor(hov ? Color.decode("#f1f5f9") : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), rad, rad));

                g2.setColor(Color.decode("#cbd5e1"));
                g2.setStroke(new BasicStroke(Math.max(1.5f, 1.5f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(0, 0, c.getWidth() - 1, c.getHeight() - 1, rad, rad));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void styleDangerOutlineBtn(JButton btn, Font baseFont, float sf) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(baseFont.deriveFont(Font.BOLD, 12f * sf));
        btn.setForeground(Color.decode("#ef4444"));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new BasicButtonUI() {
            boolean hov = false;

            {
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        btn.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        btn.repaint();
                    }
                });
            }

            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int rad = ImageUtil.scale(6);
                g2.setColor(hov ? Color.decode("#fee2e2") : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), rad, rad));

                g2.setColor(Color.decode("#fca5a5"));
                g2.setStroke(new BasicStroke(Math.max(1.5f, 1.5f * ImageUtil.getScaleFactor())));
                g2.draw(new RoundRectangle2D.Double(0, 0, c.getWidth() - 1, c.getHeight() - 1, rad, rad));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void styleCard(JPanel card) {
        card.setUI(new PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), ImageUtil.scale(15), ImageUtil.scale(15)));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void styleField(JTextField field, boolean disabled) {
        if (field == null) return;
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        field.setPreferredSize(new Dimension(-1, ImageUtil.scale(35))); // Override hardcoded form dims
        field.setFont(baseFont.deriveFont(Font.PLAIN, 14f * ImageUtil.getScaleFactor()));
        field.setForeground(Color.decode("#0f172a"));
        if (disabled) {
            field.setBackground(Color.decode("#f1f5f9"));
            field.setBorder(new RoundedFieldBorder(Color.decode("#e2e8f0"), ImageUtil.scale(5)));
        } else {
            field.setBackground(Color.WHITE);
            field.setCaretColor(Color.decode("#3b82f6"));
            field.setBorder(new RoundedFieldBorder(Color.decode("#cbd5e1"), ImageUtil.scale(5)));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (field.isEditable())
                        field.setBorder(new RoundedFieldBorder(Color.decode("#3b82f6"), ImageUtil.scale(5)));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (field.isEditable())
                        field.setBorder(new RoundedFieldBorder(Color.decode("#cbd5e1"), ImageUtil.scale(5)));
                }
            });
        }
    }

    private void stylePrimaryBtn(JButton btn, Font baseFont, float sf) {
        btn.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        btn.setBackground(Color.decode("#3b82f6"));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(8), ImageUtil.scale(16), ImageUtil.scale(8), ImageUtil.scale(16)));
    }

    private void bindEvents() {
        backBtn.addActionListener(e -> {
            if (user.isAdmin()) {
                mainPanel.showAdminDash();
            } else {
                mainPanel.showHome();
            }
        });

        uploadAvatarBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select Profile Picture");
            fc.setFileFilter(new FileNameExtensionFilter("Images (JPG, PNG)", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();

                String fileName = file.getName().toLowerCase();
                if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
                    showMsg(profMsgLbl, "Invalid format! Only JPG and PNG are allowed.", false);
                    return;
                }

                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    if (bytes.length > 2 * 1024 * 1024) {
                        showMsg(profMsgLbl, "Image too large! Maximum size is 2MB.", false);
                        return;
                    }

                    pendingAvatarBytes = bytes;
                    avatarRemoved = false;

                    cachedRawAvatar = ImageUtil.getImageFromBytes(bytes);
                    avatarPreviewLbl.repaint();

                    showMsg(profMsgLbl, "Picture selected. Click Save Profile to apply.", true);
                    profMsgLbl.setForeground(Color.decode("#f59e0b"));

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMsg(profMsgLbl, "Error reading image file.", false);
                }
            }
        });

        removeAvatarBtn.addActionListener(e -> {
            pendingAvatarBytes = null;
            avatarRemoved = true;

            cachedRawAvatar = null;
            avatarPreviewLbl.repaint();

            showMsg(profMsgLbl, "Picture removed. Click Save Profile to apply.", true);
            profMsgLbl.setForeground(Color.decode("#f59e0b"));
        });

        saveProfBtn.addActionListener(e -> {
            String newUsername = usernameField.getText().trim();

            if (newUsername.isEmpty()) {
                showMsg(profMsgLbl, "Username cannot be empty.", false);
                return;
            }

            boolean usernameChanged = !newUsername.equals(user.getUsername());
            boolean avatarChanged = (avatarRemoved || pendingAvatarBytes != null);

            if (usernameChanged && !canChangeUsername) {
                showMsg(profMsgLbl, "Username change is on a 7-day cooldown.", false);
                return;
            }

            if (!usernameChanged && !avatarChanged) {
                showMsg(profMsgLbl, "No changes detected.", true);
                return;
            }

            boolean success = true;
            UserDAO dao = new UserDAO();

            if (avatarChanged) {
                if (avatarRemoved) {
                    success = dao.removeProfilePicture(user.getId());
                } else if (pendingAvatarBytes != null) {
                    success = dao.updateProfilePicture(user.getId(), pendingAvatarBytes);
                }
            }

            if (usernameChanged && success) {
                if (updateProfile(user.getId(), newUsername)) {
                    user.setUsername(newUsername);
                    usernameField.repaint();
                    mainPanel.updateUsernameDisplay();
                } else {
                    success = false;
                    showMsg(profMsgLbl, "Update failed, username may be taken.", false);
                }
            }

            if (success) {
                pendingAvatarBytes = null;
                avatarRemoved = false;

                populateData();
                mainPanel.refreshCurrentScreen();

                if (usernameChanged) {
                    showMsg(profMsgLbl, "Profile updated! Username locked for 7 days.", true);
                } else {
                    showMsg(profMsgLbl, "Profile updated successfully!", true);
                }
            }
        });

        changePassBtn.addActionListener(e -> {
            Timestamp lastPassChange = getLastPasswordChange(user.getId());
            if (lastPassChange != null) {
                long minutesSince = ChronoUnit.MINUTES.between(lastPassChange.toLocalDateTime(), LocalDateTime.now());
                if (minutesSince < 5) {
                    long minutesLeft = 5 - minutesSince;
                    if (minutesLeft == 0) minutesLeft = 1;
                    showMsg(passMsgLbl, "Please wait " + minutesLeft + " minute(s) before changing again.", false);
                    return;
                }
            }

            String cur = new String(curPassField.getPassword());
            String np = new String(newPassField.getPassword());
            String conf = new String(confPassField.getPassword());

            if (!PasswordUtil.verify(cur, getStoredHash(user.getId()))) {
                showMsg(passMsgLbl, "Current password is incorrect.", false);
                return;
            }
            if (np.length() < 6) {
                showMsg(passMsgLbl, "New password must be at least 6 characters.", false);
                return;
            }
            if (!np.equals(conf)) {
                showMsg(passMsgLbl, "Passwords do not match.", false);
                return;
            }
            if (updatePassword(user.getId(), np)) {
                showMsg(passMsgLbl, "Password changed successfully!", true);
                curPassField.setText("");
                newPassField.setText("");
                confPassField.setText("");
            } else {
                showMsg(passMsgLbl, "Failed to update password.", false);
            }
        });
    }

    private void showMsg(JLabel label, String text, boolean success) {
        label.setForeground(success ? Color.decode("#10b981") : Color.decode("#ef4444"));
        label.setText(text);
    }

    private Timestamp getLastUsernameChange(int userId) {
        String sql = "SELECT last_username_change FROM users WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getTimestamp("last_username_change");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean updateProfile(int userId, String newUsername) {
        String sql = "UPDATE users SET username=?, last_username_change=CURRENT_TIMESTAMP WHERE id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updatePassword(int userId, String newPass) {
        String sql = "UPDATE users SET password=?, last_password_change=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPass));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getStoredHash(int userId) {
        String sql = "SELECT password FROM users WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("password");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Timestamp getLastPasswordChange(int userId) {
        String sql = "SELECT last_password_change FROM users WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getTimestamp("last_password_change");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        root.setOpaque(false);
        headerPanel = new JPanel();
        headerPanel.setLayout(new GridLayoutManager(2, 1, new Insets(18, 30, 18, 30), -1, 3));
        headerPanel.setOpaque(false);
        root.add(headerPanel, BorderLayout.NORTH);
        backBtn = new JButton();
        backBtn.setText("← Back");
        headerPanel.add(backBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLbl = new JLabel();
        titleLbl.setText("Settings");
        headerPanel.add(titleLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        root.add(scrollPane, BorderLayout.CENTER);
        bodyPanel = new JPanel();
        bodyPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 30, 45, 30), -1, 18));
        bodyPanel.setOpaque(false);
        scrollPane.setViewportView(bodyPanel);
        profileCard = new JPanel();
        profileCard.setLayout(new BorderLayout(0, 12));
        profileCard.setOpaque(false);
        bodyPanel.add(profileCard, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        profileCard.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(18, 21, 18, 21), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 6));
        panel1.setOpaque(false);
        profileCard.add(panel1, BorderLayout.NORTH);
        profTitleLbl = new JLabel();
        profTitleLbl.setText("Profile");
        panel1.add(profTitleLbl, BorderLayout.NORTH);
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, BorderLayout.SOUTH);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), 12, 5));
        panel2.setOpaque(false);
        profileCard.add(panel2, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panel3.setOpaque(false);
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        avatarPreviewLbl = new JPanel();
        avatarPreviewLbl.setPreferredSize(new Dimension(50, 50));
        panel3.add(avatarPreviewLbl);
        uploadAvatarBtn = new JButton();
        uploadAvatarBtn.setText("Upload Picture");
        panel3.add(uploadAvatarBtn);
        removeAvatarBtn = new JButton();
        removeAvatarBtn.setText("Remove");
        panel3.add(removeAvatarBtn);
        final JLabel label1 = new JLabel();
        label1.setText("Username");
        panel2.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usernameField = new JTextField();
        panel2.add(usernameField, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 33), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(9, 0));
        panel4.setOpaque(false);
        profileCard.add(panel4, BorderLayout.SOUTH);
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        profMsgLbl = new JLabel();
        profMsgLbl.setText(" ");
        panel4.add(profMsgLbl, BorderLayout.WEST);
        saveProfBtn = new JButton();
        saveProfBtn.setText("Save Profile");
        panel4.add(saveProfBtn, BorderLayout.EAST);
        passwordCard = new JPanel();
        passwordCard.setLayout(new BorderLayout(0, 12));
        passwordCard.setOpaque(false);
        bodyPanel.add(passwordCard, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        passwordCard.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(18, 21, 18, 21), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 6));
        panel5.setOpaque(false);
        passwordCard.add(panel5, BorderLayout.NORTH);
        passTitleLbl = new JLabel();
        passTitleLbl.setText("Change Password");
        panel5.add(passTitleLbl, BorderLayout.NORTH);
        final JSeparator separator2 = new JSeparator();
        panel5.add(separator2, BorderLayout.SOUTH);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), 0, 5));
        panel6.setOpaque(false);
        passwordCard.add(panel6, BorderLayout.CENTER);
        final JLabel label2 = new JLabel();
        label2.setText("Current Password");
        panel6.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        curPassField = new JPasswordField();
        panel6.add(curPassField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 33), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("New Password");
        panel6.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        newPassField = new JPasswordField();
        panel6.add(newPassField, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 33), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Confirm Password");
        panel6.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        confPassField = new JPasswordField();
        panel6.add(confPassField, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 33), null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new BorderLayout(9, 0));
        panel7.setOpaque(false);
        passwordCard.add(panel7, BorderLayout.SOUTH);
        panel7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        passMsgLbl = new JLabel();
        passMsgLbl.setText(" ");
        panel7.add(passMsgLbl, BorderLayout.WEST);
        changePassBtn = new JButton();
        changePassBtn.setText("Change Password");
        panel7.add(changePassBtn, BorderLayout.EAST);
        final Spacer spacer1 = new Spacer();
        bodyPanel.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

    static class RoundedFieldBorder extends AbstractBorder {
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
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Double(x, y, w - 1, h - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(ImageUtil.scale(8), ImageUtil.scale(12), ImageUtil.scale(8), ImageUtil.scale(12));
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.top = ImageUtil.scale(8);
            i.left = ImageUtil.scale(12);
            i.bottom = ImageUtil.scale(8);
            i.right = ImageUtil.scale(12);
            return i;
        }
    }
}