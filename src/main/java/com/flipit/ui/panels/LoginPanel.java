package com.flipit.ui.panels;

import com.flipit.dao.UserDAO;
import com.flipit.models.User;
import com.flipit.ui.AppFrame;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.util.DeviceUtil;
import com.flipit.util.NetworkUtil;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.Timestamp;
import java.util.prefs.Preferences;

public class LoginPanel extends JPanel {
    private JPanel root;
    private JPanel cardPanel;
    private JPanel header;
    private JTextField usernameField;
    private JPanel passwordWrapper;
    private JPasswordField passwordField;
    private JToggleButton togglePwdBtn;
    private JLabel errorLabel;
    private JButton loginBtn;
    private JPanel footer;
    private JButton signupBtn;
    private JLabel titleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel footerLabel;

    private AppFrame appFrame;
    private final UserDAO userDAO = new UserDAO();

    public LoginPanel() {
    }

    public LoginPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleAll();

        loginBtn.addActionListener(e -> attemptLogin());
        signupBtn.addActionListener(e -> appFrame.showSignup());
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        usernameField.getDocument().addDocumentListener(clearError());
        passwordField.getDocument().addDocumentListener(clearError());
    }

    private void styleAll() {
        root.setOpaque(false);
        setOpaque(false);

        cardPanel.setOpaque(true);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1, true),
                BorderFactory.createEmptyBorder(22, 30, 22, 30)
        ));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        if (titleLabel != null) {
            titleLabel.setFont(baseFont.deriveFont(Font.BOLD, 22f));
            titleLabel.setForeground(Color.decode("#1e40af"));
        }

        styleLabels(cardPanel, baseFont);
        styleField(usernameField, baseFont);
        stylePasswordField(passwordWrapper, passwordField, togglePwdBtn, baseFont);

        if (loginBtn != null) stylePrimaryBtn(loginBtn, baseFont);
        if (footer != null) footer.setOpaque(false);
        if (header != null) header.setOpaque(false);

        if (signupBtn != null) {
            signupBtn.setContentAreaFilled(false);
            signupBtn.setBorderPainted(false);
            signupBtn.setFocusPainted(false);
            signupBtn.setFont(baseFont.deriveFont(Font.BOLD, 12f));
            signupBtn.setForeground(Color.decode("#3b82f6"));
            signupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            signupBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    signupBtn.setForeground(Color.decode("#1e40af"));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    signupBtn.setForeground(Color.decode("#3b82f6"));
                }
            });
        }
    }

    private void styleLabels(Container container, Font baseFont) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel) {
                JLabel lbl = (JLabel) c;
                if (lbl == titleLabel) continue;

                if (lbl == errorLabel) {
                    lbl.setFont(baseFont.deriveFont(Font.PLAIN, 11f));
                    lbl.setForeground(Color.decode("#ef4444"));
                } else if (lbl == footerLabel || lbl.getText().contains("account")) {
                    lbl.setFont(baseFont.deriveFont(Font.PLAIN, 12f));
                    lbl.setForeground(Color.decode("#64748b"));
                } else {
                    lbl.setFont(baseFont.deriveFont(Font.BOLD, 12f));
                    lbl.setForeground(Color.decode("#0f172a"));
                }
            } else if (c instanceof Container) {
                styleLabels((Container) c, baseFont);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, Color.decode("#1e40af"),
                getWidth(), getHeight(), Color.decode("#3b82f6")));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 15));
        g2.fillOval(-80, -80, 300, 300);
        g2.fillOval(getWidth() - 150, getHeight() - 150, 300, 300);
        g2.dispose();
        super.paintComponent(g);
    }

    private void styleField(JTextField field, Font baseFont) {
        if (field == null) return;

        field.setPreferredSize(new Dimension(220, 36));
        field.setFont(baseFont.deriveFont(Font.PLAIN, 13f));
        field.setForeground(Color.decode("#0f172a"));
        field.setBackground(Color.WHITE);
        field.setCaretColor(Color.decode("#3b82f6"));
        field.setBorder(new RoundedFieldBorder(Color.decode("#e2e8f0"), 9));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(new RoundedFieldBorder(Color.decode("#3b82f6"), 9));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(new RoundedFieldBorder(Color.decode("#e2e8f0"), 9));
            }
        });
    }

    private void stylePasswordField(JPanel wrapper, JPasswordField field, JToggleButton toggleBtn, Font baseFont) {
        if (wrapper == null || field == null || toggleBtn == null) return;

        wrapper.setPreferredSize(new Dimension(220, 36));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new RoundedFieldBorder(Color.decode("#e2e8f0"), 9));

        field.setFont(baseFont.deriveFont(Font.PLAIN, 13f));
        field.setForeground(Color.decode("#0f172a"));
        field.setBackground(Color.WHITE);
        field.setCaretColor(Color.decode("#3b82f6"));
        field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        field.setEchoChar('•');

        toggleBtn.setText("");
        toggleBtn.setPreferredSize(new Dimension(30, 30));
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        toggleBtn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hov = ((AbstractButton) c).getModel().isRollover();
                g2.setColor(hov ? Color.decode("#1e40af") : Color.decode("#64748b"));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = c.getWidth() / 2;
                int cy = c.getHeight() / 2;

                Path2D eye = new Path2D.Double();
                eye.moveTo(cx - 7, cy);
                eye.quadTo(cx, cy - 6, cx + 7, cy);
                eye.quadTo(cx, cy + 6, cx - 7, cy);
                g2.draw(eye);

                g2.draw(new Ellipse2D.Double(cx - 2.5, cy - 2.5, 5, 5));

                if (!toggleBtn.isSelected()) {
                    g2.drawLine(cx - 7, cy - 6, cx + 7, cy + 6);
                }

                g2.dispose();
            }
        });

        toggleBtn.addActionListener(e -> {
            if (toggleBtn.isSelected()) {
                field.setEchoChar((char) 0);
            } else {
                field.setEchoChar('•');
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrapper.setBorder(new RoundedFieldBorder(Color.decode("#3b82f6"), 9));
            }

            @Override
            public void focusLost(FocusEvent e) {
                wrapper.setBorder(new RoundedFieldBorder(Color.decode("#e2e8f0"), 9));
            }
        });
    }

    private void stylePrimaryBtn(JButton btn, Font baseFont) {
        btn.setFont(baseFont.deriveFont(Font.BOLD, 13f));
        btn.setBackground(Color.decode("#3b82f6"));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in...");
        if (errorLabel != null) errorLabel.setText(" ");

        new SwingWorker<User, Void>() {
            String errorMsg = null;

            @Override
            protected User doInBackground() {
                if (!NetworkUtil.isOnline()) {
                    errorMsg = "Please check your internet connection.";
                    return null;
                }

                String deviceId = DeviceUtil.getDeviceId();
                Timestamp lockedUntil = userDAO.getDeviceLockoutTime(deviceId, username);

                if (lockedUntil != null) {
                    long remainingMillis = lockedUntil.getTime() - System.currentTimeMillis();
                    if (remainingMillis > 0) {
                        long mins = remainingMillis / (60 * 1000);
                        long secs = (remainingMillis / 1000) % 60;
                        errorMsg = String.format("Account locked on this device due to too many attempts. Try again in %dm %ds.", mins, secs);
                        return null;
                    } else {
                        userDAO.resetDeviceFailedAttempts(deviceId, username);
                    }
                }

                User user = userDAO.login(username, password);
                if (user == null) {
                    int attempts = userDAO.recordFailedDeviceAttempt(deviceId, username);
                    int remaining = 9 - attempts;

                    if (remaining <= 0) {
                        errorMsg = "Too many failed attempts. locked for 5 minutes.";
                    } else if (remaining <= 5) {
                        errorMsg = "Invalid credentials. " + remaining + " attempt" + (remaining == 1 ? "" : "s") + " left.";
                    } else {
                        errorMsg = "Invalid credentials.";
                    }
                } else {
                    userDAO.resetDeviceFailedAttempts(deviceId, username);
                }
                return user;
            }

            @Override
            protected void done() {
                loginBtn.setEnabled(true);
                loginBtn.setText("Sign in");

                try {
                    User user = get();
                    if (errorMsg != null) {
                        if (errorMsg.contains("servers")) {
                            Window parentWindow = SwingUtilities.getWindowAncestor(LoginPanel.this);
                            new InfoDialog(parentWindow, "Offline", errorMsg).setVisible(true);
                        } else {
                            passwordField.setText("");
                            showError(errorMsg);
                            passwordField.requestFocus();
                        }
                        return;
                    }

                    if (user != null) {
                        Preferences prefs = Preferences.userNodeForPackage(AppFrame.class);
                        prefs.put("saved_username", user.getUsername());

                        usernameField.setText("");
                        passwordField.setText("");
                        if (errorLabel != null) errorLabel.setText(" ");

                        appFrame.showMain(user);
                    }
                } catch (Exception ex) {
                    showError("An unexpected error occurred.");
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setText(msg);
        errorLabel.setForeground(Color.decode("#ef4444"));
    }

    private DocumentListener clearError() {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (errorLabel != null) errorLabel.setText(" ");
            }

            public void removeUpdate(DocumentEvent e) {
                if (errorLabel != null) errorLabel.setText(" ");
            }

            public void changedUpdate(DocumentEvent e) {
                if (errorLabel != null) errorLabel.setText(" ");
            }
        };
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
        root.setLayout(new GridBagLayout());
        cardPanel = new JPanel();
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(cardPanel, gbc);
        header = new JPanel();
        header.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        cardPanel.add(header, gbc);
        titleLabel = new JLabel();
        titleLabel.setText("LOG IN");
        header.add(titleLabel);
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(0);
        label1.setHorizontalTextPosition(0);
        label1.setText("Sign in to your account");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        cardPanel.add(label1, gbc);
        usernameLabel = new JLabel();
        usernameLabel.setText("Username");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(usernameLabel, gbc);
        usernameField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cardPanel.add(usernameField, gbc);
        passwordLabel = new JLabel();
        passwordLabel.setText("Password");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(passwordLabel, gbc);
        passwordWrapper = new JPanel();
        passwordWrapper.setLayout(new BorderLayout(0, 0));
        passwordWrapper.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cardPanel.add(passwordWrapper, gbc);
        passwordField = new JPasswordField();
        passwordWrapper.add(passwordField, BorderLayout.CENTER);
        togglePwdBtn = new JToggleButton();
        togglePwdBtn.setBorderPainted(false);
        togglePwdBtn.setContentAreaFilled(false);
        togglePwdBtn.setFocusPainted(false);
        togglePwdBtn.setText("\uD83D\uDC41");
        passwordWrapper.add(togglePwdBtn, BorderLayout.EAST);
        final Spacer spacer1 = new Spacer();
        passwordWrapper.add(spacer1, BorderLayout.NORTH);
        errorLabel = new JLabel();
        errorLabel.setText(" ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(errorLabel, gbc);
        loginBtn = new JButton();
        loginBtn.setText("Sign in");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cardPanel.add(loginBtn, gbc);
        footer = new JPanel();
        footer.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.fill = GridBagConstraints.BOTH;
        cardPanel.add(footer, gbc);
        footerLabel = new JLabel();
        footerLabel.setText("Don't have an account?");
        footer.add(footerLabel);
        signupBtn = new JButton();
        signupBtn.setText("Create an account");
        footer.add(signupBtn);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer4, gbc);
        final JPanel spacer5 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer5, gbc);
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
            g2.setStroke(new BasicStroke(2));
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 10, 8, 10);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.top = 8;
            i.left = 10;
            i.bottom = 8;
            i.right = 10;
            return i;
        }
    }
}