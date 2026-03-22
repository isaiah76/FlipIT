package com.flipit.ui;

import com.flipit.ui.panels.LoginPanel;
import com.flipit.ui.panels.SignupPanel;

import javax.swing.*;
import java.awt.*;

public class AppFrame extends JFrame {
    public static final String LOGIN_SCREEN = "LOGIN", SIGNUP_SCREEN = "SIGNUP", MAIN_SCREEN = "MAIN", ADMIN_SCREEN = "ADMIN";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    private LoginPanel loginPanel;
    private SignupPanel signupPanel;

    public AppFrame() {
        setTitle("FlipIT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gd.setFullScreenWindow(this);

        loginPanel = new LoginPanel(this);
        signupPanel = new SignupPanel(this);

        root.add(loginPanel, LOGIN_SCREEN);
        root.add(signupPanel, SIGNUP_SCREEN);

        setContentPane(root);
        showLogin();
    }

    public void showLogin(){
        cardLayout.show(root, LOGIN_SCREEN);
    }

    public void showSignup(){
        cardLayout.show(root, SIGNUP_SCREEN);
    }
}