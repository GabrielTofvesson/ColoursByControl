package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import net.tofvesson.libtlang.Routine;

public class StartIcon extends ActionView {
    protected final Rect bounds = new Rect();
    public StartIcon(Context c) { super(c); }
    public StartIcon(Context c, @Nullable AttributeSet a) { super(c, a); }
    public StartIcon(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); }
    @android.support.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.LOLLIPOP) public StartIcon(Context c, @Nullable AttributeSet a, int s, int r) { super(c, a, s, r); }

    @Override public void processInstructions(Routine<?> r) { }

    @Override
    public void onDraw(android.graphics.Canvas canvas) {
        float f;
        accentColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/1.25F);
        accentColor.getTextBounds("S", 0, 1, bounds);
        canvas.drawCircle(canvas.getWidth()/2F, canvas.getHeight()/2F, f=Math.min(canvas.getWidth(), canvas.getHeight())/2F, color);
        canvas.drawText("S", f - bounds.exactCenterX(), f - bounds.exactCenterY(), accentColor);
    }
}
