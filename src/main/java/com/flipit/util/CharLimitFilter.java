package com.flipit.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class CharLimitFilter extends DocumentFilter {
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
            // if text is pasted cut it
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