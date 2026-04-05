package com.flipit;

import com.flipit.dao.UserDAO;
import com.flipit.models.User;
import com.flipit.ui.AppFrame;
import com.flipit.ui.SplashFrame;
import com.flipit.ui.dialogs.InfoDialog;
import com.flipit.util.NetworkUtil;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.prefs.Preferences;

// TODO: fix caching, export deck, setup installer

public class Main {
    public static void main(String[] args) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
            if (waylandDisplay != null && System.getProperty("sun.java2d.uiScale") == null) {
//         String gdkScale = System.getenv("GDK_SCALE");
                String gdkScale = "2.0";
                if (gdkScale != null) {
                    System.setProperty("sun.java2d.uiScale", gdkScale);
                }
            }
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                InputStream fontStream = Main.class.getResourceAsStream("/fonts/Inter/static/Inter_18pt-Regular.ttf");
                InputStream logoFontStream = Main.class.getResourceAsStream("/fonts/Inter/static/Inter_24pt-Black.ttf");

                if (fontStream != null) {
                    Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(13f);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(customFont);
                    UIManager.put("defaultFont", customFont);
                } else {
                    System.err.println("Font not found");
                }

                if (logoFontStream != null) {
                    Font logoFont = Font.createFont(Font.TRUETYPE_FONT, logoFontStream);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(logoFont);
                    UIManager.put("logoFont", logoFont);
                }

                UIManager.setLookAndFeel(new FlatLightLaf());
                UIManager.put("Panel.background", java.awt.Color.decode("#f8fafc"));
                UIManager.put("Window.background", java.awt.Color.decode("#f8fafc"));
            } catch (Exception e) {
                System.err.println("Failed to initialize FlatLaf or Fonts");
                e.printStackTrace();
            }

            SplashFrame splash = new SplashFrame();
            splash.setVisible(true);

            SwingWorker<User, Integer> worker = new SwingWorker<>() {
                @Override
                protected User doInBackground() throws Exception {
                    publish(20);

                    // warm up DB connection pool.
                    new UserDAO().getTotalUsers();

                    publish(60);

                    User autoUser = null;
                    Preferences prefs = Preferences.userNodeForPackage(AppFrame.class);
                    String savedUsername = prefs.get("saved_username", null);

                    // check if online
                    if (savedUsername != null && !savedUsername.isBlank()) {
                        if (NetworkUtil.isOnline()) {
                            autoUser = new UserDAO().getUserByUsername(savedUsername);
                        }
                    }

                    publish(100);
                    return autoUser;
                }

                @Override
                protected void process(List<Integer> chunks) {
                    int latestProgress = chunks.get(chunks.size() - 1);
                    splash.setProgress(latestProgress);
                }

                @Override
                protected void done() {
                    splash.dispose();

                    try {
                        User autoUser = get();

                        AppFrame app = new AppFrame();
                        app.setVisible(true);

                        if (autoUser != null) {
                            app.showMain(autoUser);
                        } else {
                            Preferences prefs = Preferences.userNodeForPackage(AppFrame.class);
                            String savedUsername = prefs.get("saved_username", null);
                            if (savedUsername != null && !savedUsername.isBlank() && !NetworkUtil.isOnline()) {
                                new InfoDialog(app, "No Internet Connection", "Please connect to the internet and try again.").setVisible(true);
                            }
                            app.showLogin();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        new InfoDialog(null, "Database Error", "Please check your internet connection.").setVisible(true);
                        System.exit(1);
                    }
                }
            };
            worker.execute();
        });
    }
}
