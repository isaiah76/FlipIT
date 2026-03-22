package com.flipit.ui.panels;

import com.flipit.ui.AppFrame;

import javax.swing.*;
import java.awt.*;

public class SignupPanel extends JPanel {
    private JPanel rootPanel;
    private JPanel cardPanel;

    private AppFrame appFrame;

    public SignupPanel() {
    }

    public SignupPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
    }
}
