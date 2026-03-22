package com.flipit.ui.panels;

import com.flipit.ui.AppFrame;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private JPanel rootPanel;
    private JPanel cardPanel;
    private JPanel header;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginBtn;
    private JPanel footer;
    private JButton signupBtn;
    private JLabel titleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel footerLabel;

    private AppFrame appFrame;

    public LoginPanel() {
    }

    public LoginPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> login());
        signupBtn.addActionListener(e -> appFrame.showSignup());
    }

    private void login(){

    }
}
