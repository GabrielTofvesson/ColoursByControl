package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import net.tofvesson.coloursbycontrol.R;
import net.tofvesson.libtlang.Routine;

public abstract class ActionView extends View {
    protected final Paint color = new Paint(), backgroundColor = new Paint(), accentColor = new Paint(), textColor = new Paint(), errorColor = new Paint();
    public ActionView(Context c) { super(c); init(c, null, 0, 0); }
    public ActionView(Context c, @Nullable AttributeSet a) { super(c, a); init(c, a, 0, 0); }
    public ActionView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(c, a, d, 0); }
    @android.support.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.LOLLIPOP) public ActionView(Context c, @Nullable AttributeSet a, int s, int r) { super(c, a, s, r); init(c, a, s, r); }
    private void init(Context ctx, AttributeSet set, int da, int dr){
        if(set==null){
            color.setColor(0xffffffff);
            backgroundColor.setColor(0xffffffff);
            accentColor.setColor(0xffffffff);
            textColor.setColor(0xffffffff);
            errorColor.setColor(0xffffffff);
        }
        else{
            TypedArray a = ctx.getTheme().obtainStyledAttributes(set, R.styleable.CustomVal, da, dr);
            try {
                color.setColor(a.getColor(R.styleable.CustomVal_color, 0xffffffff));
                backgroundColor.setColor(a.getColor(R.styleable.CustomVal_backgroundColor, 0xffffffff));
                accentColor.setColor(a.getColor(R.styleable.CustomVal_accentColor, 0xffffffff));
                textColor.setColor(a.getColor(R.styleable.CustomVal_textColor, 0xffffffff));
                errorColor.setColor(a.getColor(R.styleable.CustomVal_errorColor, 0xffffffff));
            } finally { a.recycle(); }
        }
    }

    public abstract void processInstructions(Routine<?> r);
    public abstract void onDraw(android.graphics.Canvas canvas);
}
