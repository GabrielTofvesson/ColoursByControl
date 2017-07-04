package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import net.tofvesson.coloursbycontrol.R;

public class CircleHideView extends View {
    protected static final float FRAME_TOTAL = 60;
    protected final RectF circleSize = new RectF();
    protected Bitmap drawOn = null;
    protected Canvas c;
    protected boolean trigger = false;
    protected long frame = 0;
    protected Paint drawColor = new Paint();
    public boolean reverse = false;
    protected OnHideFinishListener listener;

    public CircleHideView(Context c)                                                                                            { super(c);            init(c, null, 0, 0); }
    public CircleHideView(Context c, @Nullable AttributeSet a)                                                                  { super(c, a);         init(c, a, 0, 0);    }
    public CircleHideView(Context c, @Nullable AttributeSet a, int d)                                                           { super(c, a, d);      init(c, a, d, 0);    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) public CircleHideView(Context c, @Nullable AttributeSet a, int d, int r)   { super(c, a, d, r);   init(c, a, d, r);    }

    private void init(Context ctx, AttributeSet set, int da, int dr){
        if(set==null) drawColor.setColor(0xffffffff);
        else{
            TypedArray a = ctx.getTheme().obtainStyledAttributes(set, R.styleable.CustomVal, da, dr);
            try { drawColor.setColor(a.getColor(R.styleable.CustomVal_color, 0xffffffff)); } finally { a.recycle(); }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(drawOn == null) drawOn = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888); // Allocate once
        if(c == null) c = new Canvas(drawOn);
    }

    public void triggerDraw(){ if(!trigger){ trigger = true; invalidate(); } }
    public CircleHideView setOnFinishedListner(OnHideFinishListener listener){ this.listener = listener; return this; }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!trigger) canvas.drawPaint(drawColor);
        else if(frame<=FRAME_TOTAL){
            float vCenter = canvas.getHeight() / 2.0F, hCenter = canvas.getWidth() / 2.0F, time = frame / FRAME_TOTAL;
            circleSize.top = reverse ? (1-time) * vCenter : time * vCenter;
            circleSize.bottom = reverse ? (1+time)*vCenter : (2-time) * vCenter;
            circleSize.left = reverse ? (1-time) * hCenter : time * hCenter;
            circleSize.right = reverse ? (1+time)*hCenter : (2-time) * hCenter;
            c.drawColor(Color.TRANSPARENT);
            c.drawCircle(hCenter, vCenter, (float) Math.sqrt((vCenter*vCenter)+(hCenter*hCenter))*time, drawColor);
            canvas.drawBitmap(drawOn, 0, 0, null);
            ++frame;
            invalidate();
        }else{
            trigger = false;
            frame = 0;
            if(listener!=null) listener.onHideFinished(this);
        }
    }

    public interface OnHideFinishListener{ void onHideFinished(CircleHideView view); }
}
