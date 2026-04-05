package com.flipit.ui.panels;

import com.flipit.dao.UserDAO;
import com.flipit.ui.AppFrame;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.ui.dialogs.SuccessDialog;
import com.flipit.util.NetworkUtil;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class SignupPanel extends JPanel {
    private JPanel root;
    private JPanel cardPanel;
    private JPanel header;
    private JPanel footer;
    private JTextField usernameField;
    private JPanel passwordWrapper;
    private JPasswordField passwordField;
    private JToggleButton togglePwdBtn;
    private JPanel confirmPasswordWrapper;
    private JPasswordField confirmPasswordField;
    private JToggleButton toggleConfirmPwdBtn;
    private JButton signupBtn;
    private JButton signinBtn;
    private JLabel errorLabel;
    private JLabel titleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel confirmPasswordLabel;
    private JLabel footerLabel;

    private AppFrame appFrame;
    private final UserDAO userDAO = new UserDAO();

    public SignupPanel() {
    }

    public SignupPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        styleAll();

        signupBtn.addActionListener(e -> attemptRegister());
        signinBtn.addActionListener(e -> appFrame.showLogin());
        confirmPasswordField.addActionListener(e -> attemptRegister());
    }

    private void styleAll() {
        root.setOpaque(false);
        setOpaque(false);

        if (cardPanel != null) {
            cardPanel.setOpaque(true);
            cardPanel.setBackground(Color.WHITE);
            cardPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1, true),
                    BorderFactory.createEmptyBorder(22, 30, 22, 30)
            ));
        }

        if (header != null) header.setOpaque(false);
        if (footer != null) footer.setOpaque(false);

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        if (titleLabel != null) {
            titleLabel.setFont(baseFont.deriveFont(Font.BOLD, 22f));
            titleLabel.setForeground(Color.decode("#1e40af"));
        }

        if (cardPanel != null) styleLabels(cardPanel, baseFont);

        styleField(usernameField, baseFont);
        stylePasswordField(passwordWrapper, passwordField, togglePwdBtn, baseFont);
        stylePasswordField(confirmPasswordWrapper, confirmPasswordField, toggleConfirmPwdBtn, baseFont);

        if (signupBtn != null) stylePrimaryBtn(signupBtn, baseFont);

        if (signinBtn != null) {
            signinBtn.setContentAreaFilled(false);
            signinBtn.setBorderPainted(false);
            signinBtn.setFocusPainted(false);
            signinBtn.setFont(baseFont.deriveFont(Font.BOLD, 12f));
            signinBtn.setForeground(Color.decode("#3b82f6"));
            signinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            signinBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    signinBtn.setForeground(Color.decode("#1e40af"));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    signinBtn.setForeground(Color.decode("#3b82f6"));
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
                } else if (lbl == footerLabel || lbl.getText().contains("Already have")) {
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
        g2.dispose();
        super.paintComponent(g);
    }

    private void styleField(JTextField f, Font baseFont) {
        if (f == null) return;
        f.setPreferredSize(new Dimension(220, 36));
        f.setFont(baseFont.deriveFont(Font.PLAIN, 13f));
        f.setForeground(Color.decode("#0f172a"));
        f.setBackground(Color.WHITE);
        f.setCaretColor(Color.decode("#3b82f6"));
        f.setBorder(new LoginPanel.RoundedFieldBorder(Color.decode("#e2e8f0"), 9));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.setBorder(new LoginPanel.RoundedFieldBorder(Color.decode("#3b82f6"), 9));
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.setBorder(new LoginPanel.RoundedFieldBorder(Color.decode("#e2e8f0"), 9));
            }
        });
    }

    private void stylePasswordField(JPanel wrapper, JPasswordField field, JToggleButton toggleBtn, Font baseFont) {
        if (wrapper == null || field == null || toggleBtn == null) return;

        wrapper.setPreferredSize(new Dimension(220, 36));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new LoginPanel.RoundedFieldBorder(Color.decode("#e2e8f0"), 9));

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
                wrapper.setBorder(new LoginPanel.RoundedFieldBorder(Color.decode("#3b82f6"), 9));
            }

            @Override
            public void focusLost(FocusEvent e) {
                wrapper.setBorder(new LoginPanel.RoundedFieldBorder(Color.decode("#e2e8f0"), 9));
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

    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("All fields are required.");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        signupBtn.setEnabled(false);
        signupBtn.setText("Creating Account...");
        if (errorLabel != null) errorLabel.setText(" ");

        new SwingWorker<Boolean, Void>() {
            String errorMsg = null;

            @Override
            protected Boolean doInBackground() {
                if (!NetworkUtil.isOnline()) {
                    errorMsg = "Cannot reach the servers. Please check your internet connection.";
                    return false;
                }

                if (userDAO.usernameExists(username)) {
                    errorMsg = "Username already taken.";
                    return false;
                }

                return userDAO.signup(username, password);
            }

            @Override
            protected void done() {
                signupBtn.setEnabled(true);
                signupBtn.setText("Create Account");

                try {
                    boolean success = get();
                    Window parentWindow = SwingUtilities.getWindowAncestor(SignupPanel.this);

                    if (errorMsg != null) {
                        if (errorMsg.contains("servers")) {
                            new InfoDialog(parentWindow, "Offline", errorMsg).setVisible(true);
                        } else {
                            showError(errorMsg);
                        }
                        return;
                    }

                    if (success) {
                        clearFields();
                        new SuccessDialog(parentWindow, "Welcome", "Account created successfully!").setVisible(true);
                        appFrame.showLogin();
                    } else {
                        showError("Registration failed. Please try again.");
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

    private void clearFields() {
        for (JTextField f : new JTextField[]{usernameField}) if (f != null) f.setText("");
        for (JPasswordField f : new JPasswordField[]{passwordField, confirmPasswordField}) if (f != null) f.setText("");
        if (errorLabel != null) errorLabel.setText(" ");
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
        titleLabel.setText("SIGN UP");
        header.add(titleLabel);
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(0);
        label1.setHorizontalTextPosition(0);
        label1.setText("Create your account");
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
        gbc.gridy = 9;
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
        confirmPasswordLabel = new JLabel();
        confirmPasswordLabel.setText("Confirm Password");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(confirmPasswordLabel, gbc);
        confirmPasswordWrapper = new JPanel();
        confirmPasswordWrapper.setLayout(new BorderLayout(0, 0));
        confirmPasswordWrapper.setBackground(new Color(-1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cardPanel.add(confirmPasswordWrapper, gbc);
        confirmPasswordField = new JPasswordField();
        confirmPasswordWrapper.add(confirmPasswordField, BorderLayout.CENTER);
        toggleConfirmPwdBtn = new JToggleButton();
        toggleConfirmPwdBtn.setBorderPainted(false);
        toggleConfirmPwdBtn.setContentAreaFilled(false);
        toggleConfirmPwdBtn.setFocusPainted(false);
        toggleConfirmPwdBtn.setText("\uD83D\uDC41");
        confirmPasswordWrapper.add(toggleConfirmPwdBtn, BorderLayout.EAST);
        final Spacer spacer1 = new Spacer();
        confirmPasswordWrapper.add(spacer1, BorderLayout.NORTH);
        errorLabel = new JLabel();
        errorLabel.setText(" ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(errorLabel, gbc);
        signupBtn = new JButton();
        signupBtn.setText("Create Account");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cardPanel.add(signupBtn, gbc);
        footer = new JPanel();
        footer.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 16;
        gbc.fill = GridBagConstraints.BOTH;
        cardPanel.add(footer, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Already have an account?");
        footer.add(label2);
        signinBtn = new JButton();
        signinBtn.setText("Sign in");
        footer.add(signinBtn);
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
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer5, gbc);
        final JPanel spacer6 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer6, gbc);
        final JPanel spacer7 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 15;
        gbc.fill = GridBagConstraints.VERTICAL;
        cardPanel.add(spacer7, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}