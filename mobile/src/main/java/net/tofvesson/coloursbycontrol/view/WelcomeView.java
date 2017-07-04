package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import net.tofvesson.coloursbycontrol.R;

public class WelcomeView extends View {
    //static{ System.loadLibrary("native-lib"); } // Load native (rendering) code for this class

    protected static final float precision = 30, delimiter = 30, milestone = delimiter+precision;
    protected static final Interpolator i = new AccelerateDecelerateInterpolator();
    protected long frame;
    private int maxHeight;
    private int maxWidth;
    protected final RectF s1_oval = new RectF(), s1_innerOval = new RectF(), s2_topCir = new RectF(), s2_botCir = new RectF(), s2_topInnerCir = new RectF(), s2_botInnerCir = new RectF();
    protected final Paint drawColor = new Paint(Paint.ANTI_ALIAS_FLAG), clearColor = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected boolean trigger = false;
    protected OnWelcomeFinishedListener listener;

    public WelcomeView(Context c)                                                                                            { super(c);            init(c, null, 0, 0); }
    public WelcomeView(Context c, @Nullable AttributeSet a)                                                                  { super(c, a);         init(c, a, 0, 0);    }
    public WelcomeView(Context c, @Nullable AttributeSet a, int d)                                                           { super(c, a, d);      init(c, a, d, 0);    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) public WelcomeView(Context c, @Nullable AttributeSet a, int d, int r)   { super(c, a, d, r);   init(c, a, d, r);    }

    private void init(Context ctx, AttributeSet set, int da, int dr){
        if(set==null){
            clearColor.setColor(0xffffffff);
            drawColor.setColor(0xffffffff);
        }
        else{
            TypedArray a = ctx.getTheme().obtainStyledAttributes(set, R.styleable.CustomVal, da, dr);
            try {
                drawColor.setColor(a.getColor(R.styleable.CustomVal_color, 0xffffffff));
                clearColor.setColor(a.getColor(R.styleable.CustomVal_backgroundColor, 0xffffffff));
            } finally { a.recycle(); }
        }
        s1_oval.left = 1;
        s1_oval.top = 1;
    }
    public void setDrawColor(Paint drawColor) { this.drawColor.setColor(drawColor.getColor()); }
    public void setClearColor(Paint clearColor) { this.clearColor.setColor(clearColor.getColor()); }
    public void triggerDraw(){ if(!trigger){ trigger = true; invalidate(); } }
    public WelcomeView setOnFinishedListener(OnWelcomeFinishedListener listener){ this.listener = listener; return this; }

    @Override
    protected final void onDraw(Canvas canvas){
        if(!trigger){
            canvas.drawPaint(drawColor);
            return;
        }
             if(frame<delimiter) invalidate();
        else if(frame<milestone   || frame<milestone  +delimiter || frame <milestone*2 || frame <milestone*2+delimiter) drawC1(canvas);
        else if(frame<milestone*3 || frame<milestone*3+delimiter) drawB(canvas);
        else if(frame<milestone*4 || frame<milestone*4+delimiter) clearB(canvas);
        else if(frame<milestone*5 || frame<milestone*5+delimiter || frame <milestone*6 || frame <milestone*6+delimiter) drawC2(canvas);
        else { trigger = false; frame = 0; if(listener!=null) listener.onTriggerFinished(this); }
        if(trigger) ++frame;
    }

    protected void drawC1(Canvas canvas){
        boolean b = frame<milestone || frame<milestone+delimiter;
        float
                f = frame<milestone ? i.getInterpolation((frame-delimiter)/precision) : b ? 1 :
                frame<milestone*2 ? i.getInterpolation((frame-milestone-delimiter)/precision) : 1,                          // Render factor
                f1 = 45 + 135 * (b ? (1-f) : f),                                                                            // Bottom half render size
                f2 = 270 * (b ? f : (1-f));                                                                                 // Top half render size

        s1_oval.right = maxWidth = canvas.getWidth() - 1;
        s1_oval.bottom = maxHeight = canvas.getHeight() - 1;

        s1_innerOval.left = canvas.getWidth() * 0.15F;
        s1_innerOval.top = canvas.getHeight() * 0.15F;
        s1_innerOval.right = maxWidth - s1_innerOval.left + 1;
        s1_innerOval.bottom = maxHeight - s1_innerOval.top + 1;

        canvas.drawArc(s1_oval, f1, f2, true, drawColor);                                                                   // Draw arc
        canvas.drawArc(s1_innerOval, f1 - 1, f2 + 2, true, clearColor);                                                     // Remove inner part (with 1 degree's overlap)
        invalidate();                                                                                                       // Make sure method is called again
    }

    protected void drawB(Canvas canvas){
        float
                hf = canvas.getHeight() / 2.0F, width = canvas.getWidth(), hq = width / 4.0F,
                f = frame <milestone * 3 ? (frame -milestone*2-delimiter)/precision : 1,
                f1 = 0;
        canvas.drawRect(canvas.getWidth() / 8.0F, f <= 0.3F ? hf * (1-(f1=(f/0.3F))) : 0, hq, f <= 0.3F ? hf * (1 + f1) : 2 * hf, drawColor);
        if(f>=0.3){
            s2_topCir.left = -hq*0.5F;
            s2_topCir.right = width - hq*1.5F;
            s2_topCir.bottom = canvas.getHeight() / 2.5F;

            float tWFact = (s2_topCir.right - s2_topCir.left) * 0.15F, tHFact = s2_topCir.bottom * 0.15F;

            s2_topInnerCir.left = s2_topCir.left + tWFact;
            s2_topInnerCir.right = s2_topCir.right - tWFact;
            s2_topInnerCir.top = s2_topCir.top + tHFact;
            s2_topInnerCir.bottom = s2_topCir.bottom - tHFact;

            canvas.drawArc(s2_topCir, 270, f<=0.6F ? 180 * (f1=(f-0.3F)/0.3F): 180, true, drawColor);
            canvas.drawArc(s2_topInnerCir, 270, f<=0.6F ? 180 * f1: 180, true, clearColor);
        }
        if(f>=0.6){
            s2_botCir.left = -hq;
            s2_botCir.top = canvas.getHeight() / 2.5F;
            s2_botCir.right = width - hq;
            s2_botCir.bottom = canvas.getHeight();

            float bWFact = (s2_botCir.right - s2_botCir.left) * 0.15F, bHFact = (s2_botCir.bottom - s2_botCir.top) * 0.15F;

            s2_botInnerCir.left = s2_botCir.left + bWFact;
            s2_botInnerCir.right = s2_botCir.right - bWFact;
            s2_botInnerCir.top = s2_botCir.top + bHFact;
            s2_botInnerCir.bottom = s2_botCir.bottom - bHFact;

            canvas.drawArc(s2_botCir, 270, f<=1 ? 180 * (f1=(f-0.6F)/0.4F) : 180, true, drawColor);
            canvas.drawArc(s2_botInnerCir, 270, f<=1 ? 180 * f1 : 180, true, clearColor);
        }
        invalidate();
    }

    protected void clearB(Canvas canvas){
        float
                hf = canvas.getHeight() / 2.0F,
                f = frame <milestone * 4 ? (frame -milestone*3-delimiter)/precision : 1;
        canvas.drawRect(canvas.getWidth() / 8.0F, f >= 0.5F ? hf * (f-0.5F)/0.5F : 0, canvas.getWidth() / 4.0F, f >= 0.5F ? hf * (2-(f-0.5F)/0.5F) : 2 * hf, drawColor);

        canvas.drawArc(s2_topCir, 270, f<=0.5F ? 180 * (1-(f/0.5F)) : 0, true, drawColor);
        canvas.drawArc(s2_topInnerCir, 270, f<=0.5F ? 180 * (1-(f/0.5F)) : 0, true, clearColor);

        canvas.drawArc(s2_botCir, 270 + (f<=0.5F ? 180 * (f/0.5F) : 180), f<=0.5F ? 180 * (1-(f/0.5F)) : 0, true, drawColor);
        canvas.drawArc(s2_botInnerCir, 270 + (f<=0.5F ? 180 * (f/0.5F) : 180), f<=0.5F ? 180 * (1-(f/0.5F)) : 0, true, clearColor);
        invalidate();
    }

    protected void drawC2(Canvas canvas){
        boolean b = frame <milestone*5 || frame <milestone*5+delimiter;
        float f= frame <milestone*5?i.getInterpolation((frame -milestone*4-delimiter)/precision): frame <milestone*5+delimiter?1: frame <milestone*6?i.getInterpolation((frame -milestone*5-delimiter)/precision):1,
                f2 =  b ? 0 : 270 * f,
                f1 = b ? 270 * f: 270 - f2;
        canvas.drawArc(s1_oval, 45 + f2, f1, true, drawColor);
        canvas.drawArc(s1_innerOval, 45 + f2, f1, true, clearColor);
        invalidate();
    }

    //protected native void _onDraw(Canvas canvas);

    public interface OnWelcomeFinishedListener{ void onTriggerFinished(WelcomeView view); }
}