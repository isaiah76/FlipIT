package com.flipit.ui.dialogs;

import com.flipit.util.ImageUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class ReportDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel messageLabel;
    private JTextArea reportArea;
    private JScrollPane scrollPane;

    private boolean isApproved = false;

    public ReportDialog(Window owner, String title) {
        super(owner, title != null ? title : "Report", ModalityType.APPLICATION_MODAL);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        setupTextArea();
        styleUI(owner);

        pack();
        setLocationRelativeTo(owner);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void setupTextArea() {
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setRows(6);

        reportArea.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (str == null) return;

                if ((getLength() + str.length()) > 500) {
                    int spaceLeft = 500 - getLength();
                    if (spaceLeft <= 0) return;
                    str = str.substring(0, spaceLeft);
                }

                String currentText = getText(0, getLength());
                int currentNewlines = currentText.length() - currentText.replace("\n", "").length();
                int addedNewlines = str.length() - str.replace("\n", "").length();

                if (currentNewlines + addedNewlines > 4) {
                    return;
                }

                super.insertString(offs, str, a);
            }
        });
    }

    private void styleUI(Window owner) {
        float sf = ImageUtil.getScaleFactor();
        contentPane.setBackground(Color.WHITE);
        contentPane.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(20), ImageUtil.scale(25), ImageUtil.scale(20), ImageUtil.scale(25)
        ));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        if (messageLabel != null) {
            messageLabel.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
            messageLabel.setForeground(Color.decode("#1a1f36"));
        }

        reportArea.setFont(baseFont.deriveFont(14f * sf));
        reportArea.setBackground(Color.WHITE);
        reportArea.setForeground(Color.decode("#0f172a"));
        reportArea.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(8), ImageUtil.scale(10), ImageUtil.scale(8), ImageUtil.scale(10)));

        scrollPane.setPreferredSize(new Dimension(ImageUtil.scale(320), ImageUtil.scale(130)));
        scrollPane.setMinimumSize(new Dimension(ImageUtil.scale(320), ImageUtil.scale(130)));

        scrollPane.setBorder(new RoundedFieldBorder(Color.decode("#e2e8f0"), ImageUtil.scale(9)));
        scrollPane.setBackground(Color.WHITE);

        buttonOK.setBackground(Color.decode("#ef4444"));
        buttonOK.setForeground(Color.WHITE);
        buttonOK.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        buttonOK.setFocusPainted(false);
        buttonOK.setBorderPainted(false);
        buttonOK.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buttonOK.setPreferredSize(new Dimension(ImageUtil.scale(85), ImageUtil.scale(36)));

        buttonOK.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonOK.setBackground(Color.decode("#dc2626"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonOK.setBackground(Color.decode("#ef4444"));
            }
        });

        buttonCancel.setBackground(Color.WHITE);
        buttonCancel.setForeground(Color.decode("#64748b"));
        buttonCancel.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        buttonCancel.setFocusPainted(false);
        buttonCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buttonCancel.setPreferredSize(new Dimension(ImageUtil.scale(85), ImageUtil.scale(36)));
        buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(1))));

        buttonCancel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#94a3b8"), Math.max(1, ImageUtil.scale(1))));
                buttonCancel.setForeground(Color.decode("#475569"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(1))));
                buttonCancel.setForeground(Color.decode("#64748b"));
            }
        });

        int maxWidth = owner != null ? owner.getWidth() - ImageUtil.scale(40) : ImageUtil.scale(420);
        contentPane.setPreferredSize(new Dimension(Math.min(ImageUtil.scale(420), maxWidth), ImageUtil.scale(260)));
    }

    private void onOK() {
        if (getReportReason().isEmpty()) {
            new InfoDialog(SwingUtilities.getWindowAncestor(this), "Empty Field", "Please enter a reason for reporting.").setVisible(true);
            return;
        }
        isApproved = true;
        dispose();
    }

    private void onCancel() {
        isApproved = false;
        dispose();
    }

    public boolean isApproved() {
        return isApproved;
    }

    public String getReportReason() {
        return reportArea.getText() != null ? reportArea.getText().trim() : "";
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
            g2.setStroke(new BasicStroke(Math.max(1, ImageUtil.scale(2))));
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(ImageUtil.scale(1), ImageUtil.scale(1), ImageUtil.scale(1), ImageUtil.scale(1));
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.top = i.bottom = i.left = i.right = ImageUtil.scale(1);
            return i;
        }
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(10, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 8, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Submit");
        panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, 8));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        messageLabel = new JLabel();
        messageLabel.setText("Reason for reporting:");
        panel3.add(messageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        panel3.add(scrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        reportArea = new JTextArea();
        scrollPane.setViewportView(reportArea);
        final Spacer spacer2 = new Spacer();
        contentPane.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}