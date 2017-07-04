package net.tofvesson.coloursbycontrol.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import net.tofvesson.libtlang.Routine;


public class OperationCard extends ActionCard {
    public OperationCard(Context c) { super(c); }
    public OperationCard(Context c, @Nullable AttributeSet a) { super(c, a); }
    public OperationCard(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); }
    @android.support.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.LOLLIPOP) public OperationCard(Context c, @Nullable AttributeSet a, int s, int r) { super(c, a, s, r); }

    @Override public void processInstructions(Routine<?> r) { }

    @Override
    public void drawItems(android.graphics.Canvas canvas) {

    }
}
