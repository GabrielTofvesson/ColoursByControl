package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import net.tofvesson.libtlang.Routine;

import net.tofvesson.coloursbycontrol.R;

public class FunctionCard extends ActionCard {
    private static final String func = "Function", inputs = "Inputs: ", out = "Output: ", n = "Name: ";
    protected String name;
    public Routine<?> instruction;
    public boolean inline = false;
    private final Rect tMeasure = new Rect();

    public FunctionCard(Context c) { super(c); init(c, null); }
    public FunctionCard(Context c, @Nullable AttributeSet a) { super(c, a); init(c, a); }
    public FunctionCard(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(c, a); }
    @android.support.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.LOLLIPOP) public FunctionCard(Context c, @Nullable AttributeSet a, int s, int r) { super(c, a, s, r); init(c, a); }

    private void init(Context c, AttributeSet a){
        if(a!=null) {
            TypedArray a1 = c.getTheme().obtainStyledAttributes(a, R.styleable.CustomVal, 0, 0);
            try { setName(a1.getString(R.styleable.CustomVal_name)); } finally { a1.recycle(); }
        }
        if (name == null) name = "NAMELESS";
    }

    public void setName(String name){ if(name==null || name.length()==0) return; this.name = name; }
    public String getName(){ return name; }

    @Override public void processInstructions(Routine<?> r) {
        if(instruction == null) return;
        if(inline) r.inline(instruction);
        else r.add(instruction);
    }

    @Override
    public void drawItems(android.graphics.Canvas canvas) {
        String s;
        textColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/7.5F);
        textColor.getTextBounds(func, 0, func.length(), tMeasure);
        canvas.drawText(func, canvas.getWidth()/2 - tMeasure.exactCenterX(), -tMeasure.exactCenterY() * 4, textColor);
        textColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/12F);
        textColor.getTextBounds(s=inputs + (instruction==null?"0":instruction.getParamTypes().length), 0, s.length(), tMeasure);
        canvas.drawText(s, canvas.getWidth()/2 - tMeasure.exactCenterX(), canvas.getHeight()*2F/3F - tMeasure.exactCenterY() * 3, textColor);
        textColor.getTextBounds(s=out+(instruction==null || instruction.getReturnType()==Void.class?"No":"Yes"), 0, s.length(), tMeasure);
        canvas.drawText(s, canvas.getWidth()/2 - tMeasure.exactCenterX(), canvas.getHeight()+tMeasure.exactCenterY() * 4, textColor);
        textColor.setTextSize((float) Math.sqrt(canvas.getWidth()*canvas.getHeight())/14F);
        if (name.length()>20) s = n+name.substring(0, 20)+"...";
        else s = n+name;
        textColor.getTextBounds(s, 0, s.length(), tMeasure);
        canvas.drawText(s, canvas.getWidth()/2 - tMeasure.exactCenterX(), canvas.getHeight()/2.5F, textColor);
    }
}
