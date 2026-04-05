package com.flipit.ui.dialogs;

import com.flipit.models.Card;
import com.flipit.util.ImageUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;

public class EditCardDialog extends JDialog {
    private JPanel contentPane;
    private JLabel titleLabel;
    private JTextArea qArea;
    private JTextField aField;
    private JTextField bField;
    private JTextField cField;
    private JTextField dField;
    private JComboBox<String> correctCb;
    private JLabel errorLabel;
    private JButton buttonCancel;
    private JButton buttonOK;

    private boolean isApproved = false;
    private Card existingCard;

    public EditCardDialog(Window owner, Card existingCard) {
        super(owner, existingCard == null ? "Add New Card" : "Edit Card", ModalityType.APPLICATION_MODAL);
        this.existingCard = existingCard;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        styleUI(owner);
        populateData();

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
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        titleLabel.setText(existingCard == null ? "Add New Card" : "Edit Card");
        titleLabel.setFont(baseFont.deriveFont(Font.BOLD, 16f * sf));
        titleLabel.setForeground(Color.decode("#0f172a"));

        qArea.setFont(baseFont.deriveFont(Font.PLAIN, 14f * sf));
        qArea.setForeground(Color.decode("#0f172a"));
        qArea.setBorder(BorderFactory.createEmptyBorder(ImageUtil.scale(4), ImageUtil.scale(6), ImageUtil.scale(4), ImageUtil.scale(6)));
        ((AbstractDocument) qArea.getDocument()).setDocumentFilter(new CharLimitFilter(500));

        if (qArea.getParent() instanceof JViewport && qArea.getParent().getParent() instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) qArea.getParent().getParent();
            scroll.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1));
        }

        JTextField[] fields = {aField, bField, cField, dField};
        for (JTextField f : fields) {
            f.setFont(baseFont.deriveFont(Font.PLAIN, 14f * sf));
            f.setPreferredSize(new Dimension(-1, ImageUtil.scale(32)));
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1),
                    BorderFactory.createEmptyBorder(0, ImageUtil.scale(8), 0, ImageUtil.scale(8))
            ));
            ((AbstractDocument) f.getDocument()).setDocumentFilter(new CharLimitFilter(150));
        }

        correctCb.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        correctCb.setPreferredSize(new Dimension(-1, ImageUtil.scale(32)));

        errorLabel.setFont(baseFont.deriveFont(Font.PLAIN, 11f * sf));
        errorLabel.setForeground(Color.decode("#ef4444"));

        buttonOK.setText(existingCard == null ? "Add Card" : "Save");
        buttonOK.setBackground(Color.decode("#3b82f6"));
        buttonOK.setForeground(Color.WHITE);
        buttonOK.setFont(baseFont.deriveFont(Font.BOLD, 13f * sf));
        buttonOK.setFocusPainted(false);
        buttonOK.setBorderPainted(false);
        buttonOK.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buttonOK.setPreferredSize(new Dimension(ImageUtil.scale(95), ImageUtil.scale(36)));

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
        buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1));

        buttonCancel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#94a3b8"), 1));
                buttonCancel.setForeground(Color.decode("#475569"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#e2e8f0"), 1));
                buttonCancel.setForeground(Color.decode("#64748b"));
            }
        });

        int baseWidth = ImageUtil.scale(550);
        int maxWidth = owner != null ? owner.getWidth() - ImageUtil.scale(40) : baseWidth;
        int targetWidth = Math.min(baseWidth, maxWidth);

        Dimension targetSize = new Dimension(targetWidth, ImageUtil.scale(450));

        contentPane.setPreferredSize(targetSize);
        contentPane.setMinimumSize(targetSize);
    }

    private void populateData() {
        if (existingCard != null) {
            qArea.setText(existingCard.getQuestion());
            aField.setText(existingCard.getAnswerA());
            bField.setText(existingCard.getAnswerB());
            cField.setText(existingCard.getAnswerC());
            dField.setText(existingCard.getAnswerD());

            int idx = "ABCD".indexOf(existingCard.getCorrectAnswer());
            if (idx >= 0 && idx < correctCb.getItemCount()) {
                correctCb.setSelectedIndex(idx);
            }
        }
    }

    private void onOK() {
        if (getQuestion().isEmpty() || getAnswerA().isEmpty() || getAnswerB().isEmpty() ||
                getAnswerC().isEmpty() || getAnswerD().isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        if (existingCard != null) {
            WarningDialog warn = new WarningDialog(this, "Confirm Edit", "Save changes to this card?", "Save");
            warn.setVisible(true);
            if (!warn.isApproved()) return;
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

    public String getQuestion() {
        return qArea.getText().trim();
    }

    public String getAnswerA() {
        return aField.getText().trim();
    }

    public String getAnswerB() {
        return bField.getText().trim();
    }

    public String getAnswerC() {
        return cField.getText().trim();
    }

    public String getAnswerD() {
        return dField.getText().trim();
    }

    public String getCorrectAnswer() {
        return String.valueOf("ABCD".charAt(correctCb.getSelectedIndex()));
    }

    private static class CharLimitFilter extends DocumentFilter {
        private final int limit;

        public CharLimitFilter(int limit) {
            this.limit = limit;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String str, AttributeSet attr) throws BadLocationException {
            if (str == null) return;

            if ((fb.getDocument().getLength() + str.length()) <= limit) {
                super.insertString(fb, offset, str, attr);
            } else {
                int spaceLeft = limit - fb.getDocument().getLength();
                if (spaceLeft > 0) {
                    super.insertString(fb, offset, str.substring(0, spaceLeft), attr);
                }
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet attrs) throws BadLocationException {
            if (str == null) return;

            int currentLength = fb.getDocument().getLength();
            int overLimit = (currentLength - length) + str.length() - limit;

            if (overLimit > 0) {
                str = str.substring(0, str.length() - overLimit);
            }

            if (str.length() > 0 || length > 0) {
                super.replace(fb, offset, length, str, attrs);
            }
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(15, 20, 15, 20), 10, 10));
        contentPane.setBackground(new Color(-1));
        titleLabel = new JLabel();
        titleLabel.setText("Edit Card");
        contentPane.add(titleLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), 10, 10));
        panel1.setBackground(new Color(-1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Question");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        qArea = new JTextArea();
        qArea.setLineWrap(true);
        qArea.setRows(4);
        qArea.setWrapStyleWord(true);
        scrollPane1.setViewportView(qArea);
        final JLabel label2 = new JLabel();
        label2.setText("Answer A");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aField = new JTextField();
        panel1.add(aField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Answer B");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bField = new JTextField();
        panel1.add(bField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Answer C");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cField = new JTextField();
        panel1.add(cField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Answer D");
        panel1.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dField = new JTextField();
        panel1.add(dField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Correct");
        panel1.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        correctCb = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("A – First Answer");
        defaultComboBoxModel1.addElement("B – Second Answer");
        defaultComboBoxModel1.addElement("C – Third Answer");
        defaultComboBoxModel1.addElement("D – Fourth Answer");
        correctCb.setModel(defaultComboBoxModel1);
        panel1.add(correctCb, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setBackground(new Color(-1));
        contentPane.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        errorLabel = new JLabel();
        errorLabel.setText(" ");
        panel2.add(errorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 10, -1, true, false));
        panel3.setBackground(new Color(-1));
        panel2.add(panel3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel3.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Save");
        panel3.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}