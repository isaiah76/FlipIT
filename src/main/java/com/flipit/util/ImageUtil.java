package com.flipit.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageUtil {
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();

    public static float getScaleFactor() {
        return Toolkit.getDefaultToolkit().getScreenResolution() / 96f;
    }

    public static int scale(int px) {
        return Math.round(px * getScaleFactor());
    }

    public static BufferedImage loadImage(String path) {
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }
        try {
            URL imageUrl = ImageUtil.class.getResource(path);
            if (imageUrl == null) {
                System.err.println("Could not find image: " + path);
                return null;
            }
            BufferedImage img = ImageIO.read(imageUrl);
            imageCache.put(path, img);
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Image getImageFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    public static void paintAvatar(Graphics2D g, int x, int y, int size, Image rawAvatar, String initials) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (rawAvatar != null) {
            Shape clip = new Ellipse2D.Double(x, y, size, size);
            g2.setClip(clip);

            int imgW = rawAvatar.getWidth(null);
            int imgH = rawAvatar.getHeight(null);
            int min = Math.min(imgW, imgH);
            int sx = (imgW - min) / 2;
            int sy = (imgH - min) / 2;

            g2.drawImage(rawAvatar, x, y, x + size, y + size, sx, sy, sx + min, sy + min, null);
            g2.setClip(null);
        } else {
            g2.setPaint(new GradientPaint(x, y, Color.decode("#3b5bdb"), x + size, y + size, Color.decode("#748ffc")));
            g2.fillOval(x, y, size, size);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.45)));
            FontMetrics fm = g2.getFontMetrics();
            String txt = (initials != null && !initials.trim().isEmpty()) ? initials : "?";

            int textX = x + (size - fm.stringWidth(txt)) / 2;
            int textY = y + (size + fm.getAscent() - fm.getDescent()) / 2;

            g2.drawString(txt, textX, textY);
        }

        g2.dispose();
    }

    public static void paintSquareAvatar(Graphics2D g, int x, int y, int size, Image rawAvatar, String initials, int arc) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (rawAvatar != null) {
            Shape clip = new RoundRectangle2D.Double(x, y, size, size, arc, arc);
            g2.setClip(clip);

            int imgW = rawAvatar.getWidth(null);
            int imgH = rawAvatar.getHeight(null);
            int min = Math.min(imgW, imgH);
            int sx = (imgW - min) / 2;
            int sy = (imgH - min) / 2;

            g2.drawImage(rawAvatar, x, y, x + size, y + size, sx, sy, sx + min, sy + min, null);
            g2.setClip(null);
        } else {
            g2.setPaint(new GradientPaint(x, y, Color.decode("#3b5bdb"), x + size, y + size, Color.decode("#748ffc")));
            g2.fillRoundRect(x, y, size, size, arc, arc);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.45)));
            FontMetrics fm = g2.getFontMetrics();
            String txt = (initials != null && !initials.trim().isEmpty()) ? initials : "?";

            int textX = x + (size - fm.stringWidth(txt)) / 2;
            int textY = y + (size + fm.getAscent() - fm.getDescent()) / 2;

            g2.drawString(txt, textX, textY);
        }

        g2.dispose();
    }
}