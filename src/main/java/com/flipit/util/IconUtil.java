package com.flipit.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class IconUtil {
    public static Icon getIcon(String type, Color color, int size) {
        return new VectorIcon(type, color, size);
    }

    private static class VectorIcon implements Icon {
        private final String type;
        private final Color color;
        private final int size;

        public VectorIcon(String type, Color color, int size) {
            this.type = type;
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);

            float sf = size / 16f;
            g2.setStroke(new BasicStroke(Math.max(1f, 1.5f * sf), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);

            float cx = size / 2f;
            float cy = size / 2f;

            switch (type.toUpperCase()) {
                case "BACK":
                    Path2D back = new Path2D.Double();
                    back.moveTo(cx + 2.0f * sf, cy - 4.0f * sf);
                    back.lineTo(cx - 3.0f * sf, cy);
                    back.lineTo(cx + 2.0f * sf, cy + 4.0f * sf);
                    g2.draw(back);
                    break;
                case "PLUS":
                    g2.draw(new Line2D.Double(cx - 4.5f * sf, cy, cx + 4.5f * sf, cy));
                    g2.draw(new Line2D.Double(cx, cy - 4.5f * sf, cx, cy + 4.5f * sf));
                    break;
                case "VIEW":
                    Path2D eye = new Path2D.Double();
                    eye.moveTo(cx - 4.0f * sf, cy);
                    eye.curveTo(
                            cx - 2.7f * sf, cy - 3.2f * sf,
                            cx + 2.7f * sf, cy - 3.2f * sf,
                            cx + 4.0f * sf, cy
                    );
                    eye.curveTo(
                            cx + 2.7f * sf, cy + 3.2f * sf,
                            cx - 2.7f * sf, cy + 3.2f * sf,
                            cx - 4.0f * sf, cy
                    );
                    g2.draw(eye);

                    g2.draw(new Ellipse2D.Double(cx - 1.8f * sf, cy - 1.8f * sf, 3.6f * sf, 3.6f * sf));
                    g2.fill(new Ellipse2D.Double(cx - 0.65f * sf, cy - 0.65f * sf, 1.3f * sf, 1.3f * sf));
                    break;
                case "SAVE":
                    Path2D bm = new Path2D.Double();
                    bm.moveTo(cx - 3.5f * sf, cy - 5.5f * sf);
                    bm.lineTo(cx + 3.5f * sf, cy - 5.5f * sf);
                    bm.lineTo(cx + 3.5f * sf, cy + 5.5f * sf);
                    bm.lineTo(cx, cy + 3f * sf);
                    bm.lineTo(cx - 3.5f * sf, cy + 5.5f * sf);
                    bm.closePath();
                    g2.draw(bm);
                    break;
                case "SAVED":
                    Path2D bmf = new Path2D.Double();
                    bmf.moveTo(cx - 3.5f * sf, cy - 5.5f * sf);
                    bmf.lineTo(cx + 3.5f * sf, cy - 5.5f * sf);
                    bmf.lineTo(cx + 3.5f * sf, cy + 5.5f * sf);
                    bmf.lineTo(cx, cy + 3f * sf);
                    bmf.lineTo(cx - 3.5f * sf, cy + 5.5f * sf);
                    bmf.closePath();
                    g2.fill(bmf);
                    break;
                case "CARD":
                    g2.draw(new RoundRectangle2D.Double(cx - 5.5f * sf, cy - 4 * sf, 11 * sf, 8 * sf, 2 * sf, 2 * sf));
                    g2.draw(new Line2D.Double(cx - 5.5f * sf, cy - 1.5f * sf, cx + 5.5f * sf, cy - 1.5f * sf));
                    break;
                case "DOC":
                    Path2D doc = new Path2D.Double();
                    doc.moveTo(cx - 3.5f * sf, cy + 5 * sf);
                    doc.lineTo(cx - 3.5f * sf, cy - 5 * sf);
                    doc.lineTo(cx + 1f * sf, cy - 5 * sf);
                    doc.lineTo(cx + 3.5f * sf, cy - 2.5f * sf);
                    doc.lineTo(cx + 3.5f * sf, cy + 5 * sf);
                    doc.closePath();
                    g2.draw(doc);
                    g2.draw(new Line2D.Double(cx + 1f * sf, cy - 5 * sf, cx + 1f * sf, cy - 2.5f * sf));
                    g2.draw(new Line2D.Double(cx + 1f * sf, cy - 2.5f * sf, cx + 3.5f * sf, cy - 2.5f * sf));
                    break;
                case "SEARCH":
                    g2.draw(new Ellipse2D.Double(cx - 5.5f * sf, cy - 5.5f * sf, 7f * sf, 7f * sf));
                    g2.draw(new Line2D.Double(cx + 0.5f * sf, cy + 0.5f * sf, cx + 5.5f * sf, cy + 5.5f * sf));
                    break;
                case "PUBLIC":
                    g2.draw(new Ellipse2D.Double(cx - 5.5f * sf, cy - 5.5f * sf, 11 * sf, 11 * sf));
                    g2.draw(new Ellipse2D.Double(cx - 2.5f * sf, cy - 5.5f * sf, 5 * sf, 11 * sf));
                    g2.draw(new Line2D.Double(cx - 5.5f * sf, cy, cx + 5.5f * sf, cy));
                    break;
                case "PRIVATE":
                    g2.draw(new RoundRectangle2D.Double(cx - 3.5f * sf, cy - 0.5f * sf, 7 * sf, 5.5f * sf, 1.5f * sf, 1.5f * sf));
                    g2.draw(new Arc2D.Double(cx - 2.5f * sf, cy - 4.5f * sf, 5 * sf, 5 * sf, 0, 180, Arc2D.OPEN));
                    g2.fill(new Ellipse2D.Double(cx - 0.75f * sf, cy + 1.5f * sf, 1.5f * sf, 1.5f * sf));
                    break;
                case "DISABLED":
                    g2.draw(new Ellipse2D.Double(cx - 5.5f * sf, cy - 5.5f * sf, 11 * sf, 11 * sf));
                    g2.draw(new Line2D.Double(cx - 3.8f * sf, cy - 3.8f * sf, cx + 3.8f * sf, cy + 3.8f * sf));
                    break;
                case "REFRESH":
                    float r = 4.8f * sf;
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 35, 140, Arc2D.OPEN));
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 215, 140, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(cx + 2.9f * sf, cy - 4.1f * sf, cx + 4.8f * sf, cy - 4.0f * sf));
                    g2.draw(new Line2D.Double(cx + 2.9f * sf, cy - 4.1f * sf, cx + 3.9f * sf, cy - 2.5f * sf));
                    g2.draw(new Line2D.Double(cx - 2.9f * sf, cy + 4.1f * sf, cx - 4.8f * sf, cy + 4.0f * sf));
                    g2.draw(new Line2D.Double(cx - 2.9f * sf, cy + 4.1f * sf, cx - 3.9f * sf, cy + 2.5f * sf));
                    break;
                case "SUCCESS":
                    g2.draw(new Ellipse2D.Double(cx - 5.5f * sf, cy - 5.5f * sf, 11 * sf, 11 * sf));
                    Path2D check = new Path2D.Double();
                    check.moveTo(cx - 2.5f * sf, cy);
                    check.lineTo(cx - 0.5f * sf, cy + 2.5f * sf);
                    check.lineTo(cx + 3.0f * sf, cy - 2.5f * sf);
                    g2.draw(check);
                    break;
                case "WARNING":
                    Path2D tri = new Path2D.Double();
                    tri.moveTo(cx, cy - 5.0f * sf);
                    tri.lineTo(cx + 5.5f * sf, cy + 4.5f * sf);
                    tri.lineTo(cx - 5.5f * sf, cy + 4.5f * sf);
                    tri.closePath();
                    g2.draw(tri);
                    g2.draw(new Line2D.Double(cx, cy - 1.5f * sf, cx, cy + 1.5f * sf));
                    g2.fill(new Ellipse2D.Double(cx - 0.75f * sf, cy + 2.5f * sf, 1.5f * sf, 1.5f * sf));
                    break;
                case "INFO":
                    g2.draw(new Ellipse2D.Double(cx - 5.5f * sf, cy - 5.5f * sf, 11 * sf, 11 * sf));
                    g2.draw(new Line2D.Double(cx, cy - 1.5f * sf, cx, cy + 3.5f * sf));
                    g2.fill(new Ellipse2D.Double(cx - 0.75f * sf, cy - 4.5f * sf, 1.5f * sf, 1.5f * sf));
                    break;
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}