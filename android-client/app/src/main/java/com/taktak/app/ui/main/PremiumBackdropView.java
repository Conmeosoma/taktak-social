package com.taktak.app.ui.main;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.taktak.app.R;

/** Ánh sáng tĩnh, không blur/animation liên tục hoặc xử lý ảnh camera. */
public final class PremiumBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader top, right, bottom;
    private boolean cameraMode;
    public PremiumBackdropView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setCameraMode(boolean cameraMode) {
        if (this.cameraMode != cameraMode) { this.cameraMode = cameraMode; invalidate(); }
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        if(w==0 || h==0) return;
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        top = new LinearGradient(0,0,w*.5f,w*.4f,dark ? 0xD9EF4C67 : 0x70FFB1A5,dark ? 0x55472955 : 0x08FFF8F7,Shader.TileMode.CLAMP);
        right = new RadialGradient(w*1.04f,h*.24f,w*.5f,dark ? 0x996D2475 : 0x55C599E8,0x006D2475,Shader.TileMode.CLAMP);
        bottom = new RadialGradient(w*1.04f,h*.88f,w*.6f,dark ? 0xB8812D83 : 0x65DBA1D6,0x00812D83,Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight(); canvas.drawColor(getContext().getColor(R.color.premium_bg));
        paint.setAlpha(cameraMode ? 45 : 255);
        paint.setShader(top); canvas.drawOval(-w*.65f,-w*.75f,w*.48f,w*.3f,paint);
        paint.setShader(right); canvas.drawOval(w*.72f,h*.08f,w*1.42f,h*.45f,paint);
        paint.setShader(bottom); canvas.drawOval(w*.62f,h*.7f,w*1.48f,h*1.1f,paint);
        paint.setShader(null); paint.setAlpha(255);
    }
}
