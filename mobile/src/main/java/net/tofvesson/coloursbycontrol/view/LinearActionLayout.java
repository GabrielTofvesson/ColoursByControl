package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class LinearActionLayout  extends LinearLayout {
    public LinearActionLayout(Context context) {
        super(context);
    }

    public LinearActionLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearActionLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LinearActionLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public final void addView(View child, int index, ViewGroup.LayoutParams params) {
        if(!(child instanceof ActionView)) throw new RuntimeException("Attempted to add a non-ActionView view to Action layout!");
        super.addView(child, index, params);
    }

    @Override public ActionView getChildAt(int index) { return (ActionView) super.getChildAt(index); }
}
