package com.taktak.app.ui.camera;

import android.text.*;

/** Đếm Unicode code point, không cắt đôi surrogate hoặc mất span của IME. */
public final class CaptionFilter implements InputFilter {
    public static final int MAX_CODE_POINTS = 50;
    @Override public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        for (int i = start; i < end; i++) { if (source.charAt(i) == '\0') { return dest.subSequence(dstart, dend); } }
        int available = MAX_CODE_POINTS - Character.codePointCount(dest, 0, dest.length())
                + Character.codePointCount(dest, dstart, dend);
        if (Character.codePointCount(source, start, end) <= available) { return null; }
        if (available <= 0) { return ""; }
        int limit = Character.offsetByCodePoints(source, start, available);
        return source.subSequence(start, limit);
    }
}
