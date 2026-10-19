package com.taktak.app.ui.auth;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.taktak.app.R;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.MaterialColors;

/** Trang trí riêng cho Login, không có tương tác hoặc dữ liệu nghiệp vụ. */
public final class LoginBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader ambient, coral, lavender;
    private final int kind, surface, start, end, ink;
    public LoginBackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        var value = new android.util.TypedValue();
        context.getTheme().resolveAttribute(R.attr.ttLoginKind, value, true);
        kind = value.data;
        surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface);
        ink = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        start = kind == 0 ? surface : MaterialColors.getColor(this, R.attr.ttLoginStart);
        end = kind == 0 ? surface : MaterialColors.getColor(this, R.attr.ttLoginEnd);
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == 0 || h == 0) { return; }
        if (kind != 0) {
            float saturation = kind == 1 ? .48f : kind == 2 ? .36f : .2f;
            ambient = new LinearGradient(0, 0, w, h,
                    ColorUtils.blendARGB(surface, start, saturation),
                    ColorUtils.blendARGB(surface, end, saturation), Shader.TileMode.CLAMP);
            coral = new RadialGradient(w*.12f, h*.06f, w*.75f,
                    new int[]{ColorUtils.setAlphaComponent(start, 180), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
            lavender = new RadialGradient(w*.94f, h*.82f, w*.8f,
                    new int[]{ColorUtils.setAlphaComponent(end, 170), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
            return;
        }
        ambient = new RadialGradient(w*.45f, h*.28f, w*.55f,
                new int[]{0x263E2338, 0x0019191F}, null, Shader.TileMode.CLAMP);
        coral = new LinearGradient(0, 0, w*.35f, h*.22f, 0x99642C3B, 0x22472A39, Shader.TileMode.CLAMP);
        lavender = new LinearGradient(w, h*.18f, w*.7f, h*.6f, 0x775D286D, 0x0019191F, Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (kind != 0) {
            paint.setShader(ambient); canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(coral); canvas.drawCircle(w*.12f, h*.06f, w*.75f, paint);
            paint.setShader(lavender); canvas.drawCircle(w*.94f, h*.82f, w*.8f, paint);
            paint.setShader(null);
            if (kind == 2) {
                // Kính tĩnh bằng lớp màu; không blur bitmap hoặc capture nội dung màn hình.
                paint.setColor(ColorUtils.setAlphaComponent(surface, 150));
                canvas.drawRoundRect(w*.025f, h*.15f, w*.975f, h*.96f, 32, 32, paint);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(getResources().getDisplayMetrics().density);
                paint.setColor(ColorUtils.setAlphaComponent(ink, 65));
                canvas.drawRoundRect(w*.025f, h*.15f, w*.975f, h*.96f, 32, 32, paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (kind == 3) {
                paint.setColor(ColorUtils.setAlphaComponent(ink, 22));
                float unit = getResources().getDisplayMetrics().density;
                for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) {
                    canvas.drawCircle(w*.82f + x*10*unit, h*.1f + y*10*unit, 1.5f*unit, paint);
                }
            }
            return;
        }
        canvas.drawColor(Color.rgb(25, 25, 31));
        paint.setShader(ambient);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(coral);
        canvas.drawOval(-w*.55f, -w*.76f, w*.35f, w*.365f, paint);
        paint.setShader(lavender);
        canvas.drawOval(w*.76f, h*.22f, w*1.6f, h*.72f, paint);
        paint.setShader(null);
    }
}
