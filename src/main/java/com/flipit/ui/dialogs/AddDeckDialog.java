package com.flipit.ui.dialogs;

import com.flipit.util.ImageUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AddDeckDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField deckNameField;
    private JTextField tagsField;
    private JLabel lbl1;
    private JLabel lbl2;

    private boolean isApproved = false;

    public AddDeckDialog(Window owner) {
        super(owner, "Create New Deck", ModalityType.APPLICATION_MODAL);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

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

    private void styleUI(Window owner) {
        float sf = ImageUtil.getScaleFactor();
        contentPane.setBackground(Color.WHITE);
        contentPane.setBorder(BorderFactory.createEmptyBorder(
                ImageUtil.scale(20), ImageUtil.scale(25), ImageUtil.scale(20), ImageUtil.scale(25)
        ));

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        lbl1.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        lbl2.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));

        deckNameField.setFont(baseFont.deriveFont(14f * sf));
        tagsField.setFont(baseFont.deriveFont(14f * sf));
        deckNameField.setPreferredSize(new Dimension(ImageUtil.scale(320), ImageUtil.scale(36)));
        tagsField.setPreferredSize(new Dimension(ImageUtil.scale(320), ImageUtil.scale(36)));

        buttonOK.setBackground(Color.decode("#3b82f6"));
        buttonOK.setForeground(Color.WHITE);
        buttonOK.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        buttonOK.setFocusPainted(false);
        buttonOK.setBorderPainted(false);
        buttonOK.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buttonOK.setPreferredSize(new Dimension(ImageUtil.scale(85), ImageUtil.scale(36)));

        buttonOK.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonOK.setBackground(Color.decode("#2563eb"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonOK.setBackground(Color.decode("#3b82f6"));
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
                buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#3b82f6"), Math.max(1, ImageUtil.scale(1))));
                buttonCancel.setForeground(Color.decode("#3b82f6"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), Math.max(1, ImageUtil.scale(1))));
                buttonCancel.setForeground(Color.decode("#64748b"));
            }
        });

        int maxWidth = owner != null ? owner.getWidth() - ImageUtil.scale(40) : ImageUtil.scale(420);
        int targetWidth = Math.min(ImageUtil.scale(420), maxWidth);

        contentPane.setPreferredSize(null);
        contentPane.setMinimumSize(new Dimension(targetWidth, ImageUtil.scale(210)));
    }

    private void onOK() {
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

    public String getDeckName() {
        return deckNameField.getText() != null ? deckNameField.getText().trim() : "";
    }

    public String getTags() {
        return tagsField.getText() != null ? tagsField.getText().trim() : "";
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
        buttonOK.setText("Create");
        panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, 5));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        lbl1 = new JLabel();
        lbl1.setText("Deck Name:");
        panel3.add(lbl1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deckNameField = new JTextField();
        panel3.add(deckNameField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lbl2 = new JLabel();
        lbl2.setText("Tags (comma separated, max 6, optional):");
        panel3.add(lbl2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tagsField = new JTextField();
        panel3.add(tagsField, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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