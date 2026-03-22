package com.flipit.ui.panels;

import com.flipit.ui.AppFrame;

import javax.swing.*;
import java.awt.*;

public class SignupPanel extends JPanel {
    private JPanel rootPanel;
    private JPanel cardPanel;
    private JPanel header;
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmpasswordField;
    private JButton signupBtn;
    private JPanel footer;
    private JButton signinButton;
    private JLabel titleLabel;
    private JLabel usernameLabel;
    private JLabel emailLabel;
    private JLabel passwordLabel;
    private JLabel confirmpasswordLabel;
    private JLabel errorLabel;

    private AppFrame appFrame;

    public SignupPanel() {
    }

    public SignupPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);

        signupBtn.addActionListener(e -> signup());
        signinButton.addActionListener(e -> appFrame.showLogin());
    }

    private void signup(){

    }
}
