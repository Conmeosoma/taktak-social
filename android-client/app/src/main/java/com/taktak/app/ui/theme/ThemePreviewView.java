package com.taktak.app.ui.theme;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.view.ContextThemeWrapper;
import com.google.android.material.color.MaterialColors;
import com.taktak.app.R;

/** Thumbnail minh họa tĩnh bằng tokens chooser; không blur/bitmap/render loop. */
public final class ThemePreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private AppTheme style = AppTheme.ORIGINAL;
    private int background, ink, primary, start, end;
    private Shader gradient;
    public ThemePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelection(AppTheme.ORIGINAL, ColorMode.SYSTEM);
    }
    public void setSelection(AppTheme style, ColorMode mode) {
        this.style = style;
        var config = new Configuration(getContext().getApplicationContext().getResources().getConfiguration());
        if (mode != ColorMode.SYSTEM) {
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | (mode == ColorMode.DARK ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        }
        var themed = new ContextThemeWrapper(getContext().createConfigurationContext(config), ThemeManager.chooserThemeResource(style));
        background = color(themed, com.google.android.material.R.attr.colorSurface);
        ink = color(themed, com.google.android.material.R.attr.colorOnSurface);
        primary = color(themed, androidx.appcompat.R.attr.colorPrimary);
        start = color(themed, R.attr.ttPreviewStart);
        end = color(themed, R.attr.ttPreviewEnd);
        updateShader();
        invalidate();
    }
    private int color(Context context, int attribute) { return MaterialColors.getColor(context, attribute, "Theme preview"); }
    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) { updateShader(); }
    private void updateShader() {
        gradient = new LinearGradient(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight()), start, end, Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight(), radius = Math.min(width, height) * .14f;
        paint.setShader(gradient);
        rect.set(0, 0, width, height);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);
        paint.setColor(background);
        paint.setAlpha(style == AppTheme.GLASS ? 190 : 235);
        rect.set(width * .08f, height * .12f, width * .92f, height * .88f);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setAlpha(255);
        if (style == AppTheme.GLASS || style == AppTheme.SOFT) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
            paint.setColor(MaterialColors.layer(background, ink, .25f));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setColor(MaterialColors.layer(background, ink, .7f));
        rect.set(width * .16f, height * .26f, width * .62f, height * .32f);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setColor(MaterialColors.layer(background, ink, .24f));
        rect.set(width * .16f, height * .41f, width * .82f, height * .52f);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setColor(primary);
        rect.set(width * .16f, height * .64f, width * .76f, height * .76f);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }
}
