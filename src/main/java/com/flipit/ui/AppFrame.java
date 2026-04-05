package com.flipit.ui;

import com.flipit.models.User;
import com.flipit.ui.panels.LoginPanel;
import com.flipit.ui.panels.MainPanel;
import com.flipit.ui.panels.SignupPanel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.prefs.Preferences;

public class AppFrame extends JFrame {
    public static final String LOGIN_SCREEN = "LOGIN", SIGNUP_SCREEN = "SIGNUP", MAIN_SCREEN = "MAIN", ADMIN_SCREEN = "ADMIN";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);
    private LoginPanel loginPanel;
    private SignupPanel signupPanel;
    private MainPanel mainPanel;

    public AppFrame() throws IOException {
        setTitle("FlipIT");

        java.awt.Image appIcon = javax.imageio.ImageIO.read(getClass().getResourceAsStream("/icon.png"));
        setIconImage(appIcon);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        loginPanel = new LoginPanel(this);
        signupPanel = new SignupPanel(this);

        root.add(loginPanel, LOGIN_SCREEN);
        root.add(signupPanel, SIGNUP_SCREEN);

        setContentPane(root);
    }

    public void showLogin() {
        cardLayout.show(root, LOGIN_SCREEN);
    }

    public void showSignup() {
        cardLayout.show(root, SIGNUP_SCREEN);
    }

    public void showMain(User user) {
        if (mainPanel != null) root.remove(mainPanel);

        mainPanel = new MainPanel(this, user);
        root.add(mainPanel, MAIN_SCREEN);
        cardLayout.show(root, MAIN_SCREEN);
    }

    public void logout() {
        Preferences prefs = Preferences.userNodeForPackage(AppFrame.class);
        prefs.remove("saved_username");

        if (mainPanel != null) {
            root.remove(mainPanel);
            mainPanel = null;
        }
        showLogin();
    }
}