package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import net.tofvesson.coloursbycontrol.CodeBuilder;
import net.tofvesson.coloursbycontrol.Operations;

public class ConstantCard extends ActionCard {
    protected static final String name = "Constant", help = "Click to edit...";
    public String push;
    private final Rect tMeasure = new Rect();

    public ConstantCard(Context c) {
        super(c);
    }

    public ConstantCard(Context c, @Nullable AttributeSet a) {
        super(c, a);
    }

    public ConstantCard(Context c, @Nullable AttributeSet a, int d) {
        super(c, a, d);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConstantCard(Context c, @Nullable AttributeSet a, int s, int r) {
        super(c, a, s, r);
    }

    @Override public void processInstructions(CodeBuilder c) { c.add(Operations.LDC, c.addConstant(push==null?"null":push)); }

    @Override
    protected void drawItems(Canvas canvas) {
        String s = String.valueOf(push);
        if (s.length()>15) s = s.substring(0, 15)+"...";
        s = '\"' + s + '\"';
        textColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/7.5F);
        textColor.getTextBounds(name, 0, name.length(), tMeasure);
        canvas.drawText(name, canvas.getWidth()/2 - tMeasure.exactCenterX(), -tMeasure.exactCenterY() * 4, textColor);
        textColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/10F);
        textColor.getTextBounds(s, 0, s.length(), tMeasure);
        canvas.drawText(s, canvas.getWidth()/2 - tMeasure.exactCenterX(), canvas.getHeight()/1.5F, textColor);
        textColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/16F);
        textColor.getTextBounds(help, 0, help.length(), tMeasure);
        canvas.drawText(help, canvas.getWidth()/2 - tMeasure.exactCenterX(), canvas.getHeight()/1.125F, textColor);
    }
}
