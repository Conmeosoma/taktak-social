package com.taktak.app.ui.theme;

import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.color.MaterialColors;
import com.taktak.app.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiReviewLayoutTest {
    @Test public void previewToolsFitWrappedLabelsAndDestructiveTextHasContrast() {
        var inst=InstrumentationRegistry.getInstrumentation();
        inst.runOnMainSync(()->{
            var context=inst.getTargetContext();
            for(AppTheme style:AppTheme.values())for(int mode:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})
                for(float font:new float[]{1f,2f})for(int width:new int[]{280,360}) {
                    var config=new Configuration(context.getResources().getConfiguration());
                    config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|mode;config.fontScale=font;
                    var themed=new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),
                            ThemeManager.resolve(new ThemeSelection(style,ColorMode.SYSTEM,0),ThemeManager.ScreenRole.PREVIEW).themeResource());
                    View root=LayoutInflater.from(themed).inflate(R.layout.activity_preview,null,false);
                    root.findViewById(R.id.previewScroll).setVisibility(View.VISIBLE);
                    float density=themed.getResources().getDisplayMetrics().density;
                    root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width*density),View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(Math.round(900*density),View.MeasureSpec.EXACTLY));
                    root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
                    for(int id:new int[]{R.id.btnToolDoodle,R.id.btnToolSticker,R.id.btnToolTag,R.id.btnToolUndo,R.id.btnToolClear}) {
                        TextView button=root.findViewById(id);View parent=(View)button.getParent();
                        assertTrue("Tool label must fit parent row",button.getBottom()<=parent.getHeight());
                        assertTrue("Tool label must fit own height",button.getLayout().getHeight()+button.getCompoundPaddingTop()+button.getCompoundPaddingBottom()<=button.getHeight()+1);
                    }
                    TextView clear=root.findViewById(R.id.btnToolClear);
                    assertTrue(style+" destructive text contrast",androidx.core.graphics.ColorUtils.calculateContrast(clear.getCurrentTextColor(),
                            MaterialColors.getColor(root,R.attr.taktakColorSurface))>=4.5);
                }
        });
    }
    @Test public void aiUsesHostPaletteAndReadableTextAcrossEightAppearances() {
        var inst = InstrumentationRegistry.getInstrumentation();
        inst.runOnMainSync(() -> {
            var context = inst.getTargetContext();
            for (AppTheme style : AppTheme.values()) for (int mode : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                var config = new Configuration(context.getResources().getConfiguration());
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
                var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),
                        ThemeManager.resolve(new ThemeSelection(style, ColorMode.SYSTEM, 0), ThemeManager.ScreenRole.MAIN).themeResource());
                View root = LayoutInflater.from(themed).inflate(R.layout.fragment_ai, null, false);
                int surface = MaterialColors.getColor(root, R.attr.taktakColorSurface);
                TextView title = root.findViewById(R.id.aiSubtitle), description = root.findViewById(R.id.aiDetail);
                assertEquals(MaterialColors.getColor(root, R.attr.taktakColorTextPrimary), title.getCurrentTextColor());
                assertTrue(style + " title contrast", androidx.core.graphics.ColorUtils.calculateContrast(title.getCurrentTextColor(), surface) >= 4.5);
                assertTrue(style + " body contrast", androidx.core.graphics.ColorUtils.calculateContrast(description.getCurrentTextColor(), surface) >= 4.5);
                TextView button = root.findViewById(R.id.aiNew);
                assertTrue(style + " CTA contrast", androidx.core.graphics.ColorUtils.calculateContrast(button.getCurrentTextColor(),
                        MaterialColors.getColor(root, androidx.appcompat.R.attr.colorPrimary)) >= 4.5);
            }
        });
    }

    @Test public void bothWidgetsInflateAsRemoteViewsWithoutFakeReactionActions() {
        var inst = InstrumentationRegistry.getInstrumentation();
        inst.runOnMainSync(() -> {
            var context = inst.getTargetContext();
            for (int layout : new int[]{R.layout.widget_taktak_2x2, R.layout.widget_taktak_4x2}) {
                View root = new RemoteViews(context.getPackageName(), layout).apply(context, new FrameLayout(context));
                assertEquals(View.GONE, root.findViewById(R.id.widget_reaction_bar).getVisibility());
                assertEquals(View.GONE, root.findViewById(R.id.widget_reaction_burst).getVisibility());
            }
        });
    }
}
