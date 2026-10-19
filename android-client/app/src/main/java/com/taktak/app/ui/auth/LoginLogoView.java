package com.taktak.app.ui.auth;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.taktak.app.R;

/** Giữ nguyên asset gốc; chỉ crop viền trắng và mask góc khi hiển thị. */
public final class LoginLogoView extends View {
    private final Bitmap logo;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path mask = new Path();
    private final Rect source = new Rect(96, 96, 1158, 1158);
    private final RectF target = new RectF();
    public LoginLogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        logo = BitmapFactory.decodeResource(getResources(), R.drawable.login_logo_original);
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float side = Math.min(w, h);
        target.set((w-side)/2, (h-side)/2, (w+side)/2, (h+side)/2);
        mask.reset(); mask.addRoundRect(target, side*.24f, side*.24f, Path.Direction.CW);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int checkpoint = canvas.save();
        canvas.clipPath(mask); canvas.drawBitmap(logo, source, target, paint); canvas.restoreToCount(checkpoint);
    }
}
