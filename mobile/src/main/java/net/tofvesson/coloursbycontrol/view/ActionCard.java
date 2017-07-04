package net.tofvesson.coloursbycontrol.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

@SuppressWarnings("unused")
public abstract class ActionCard extends ActionView {
    protected static final float rX = 16, rY = 9, cornerSharpness = 30; // Ratio
    protected float sRel;
    private Bitmap cardSurface;
    private Canvas cardDraw;
    private final RectF ovCard = new RectF();
    public ActionCard(Context c) { super(c); }
    public ActionCard(Context c, @Nullable AttributeSet a) { super(c, a); }
    public ActionCard(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); }
    @android.support.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.LOLLIPOP) public ActionCard(Context c, @Nullable AttributeSet a, int s, int r) { super(c, a, s, r); }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) rX*10;
        int desiredHeight = (int) rY*10;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        boolean wDef = widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST, hDef = heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST;

        if(wDef && !hDef) {
            width = widthSize;
            height = (int)(width * rY / rX);
        }else if(hDef && !wDef) {
            height = heightSize;
            width = (int)(height * rX / rY);
        }else if(hDef){
            width = widthSize;
            height = heightSize;
        }else{
            height = desiredHeight;
            width = desiredWidth;
        }

        setMeasuredDimension(width, height);

        if(cardSurface == null || cardSurface.getWidth() != getMeasuredWidth() || cardSurface.getHeight() != getMeasuredHeight()) {
            boolean verticalPad;
            float
                    ratio = ((float)getMeasuredHeight()) / ((float)getMeasuredWidth()),
                    ratioI = ((float)getMeasuredWidth()) / ((float)getMeasuredHeight()),
                    iRY = rY * ratioI,
                    padding = (verticalPad=iRY>rX)?getMeasuredWidth() - (rX*getMeasuredHeight()/rY) : getMeasuredHeight() - (rY*getMeasuredWidth()/rX);
            int x = (int) (((float)getMeasuredWidth())-(verticalPad?padding:0.0F)), y = (int) (((float)getMeasuredHeight()) - (verticalPad?0.0F:padding));
            cardSurface = Bitmap.createBitmap(x<=0?1:x, y<=0?1:y, Bitmap.Config.ARGB_8888);
            cardDraw = new Canvas(cardSurface);
        }
    }

    private int autoDef(int size, int cT, int cN){
        return size * cN / cT;
    }

    @SuppressWarnings("SuspiciousNameCombination") // They don't know
    @Override
    public final void onDraw(Canvas canvas) {
        //if(cardDraw.getWidth()==1 || cardDraw.getHeight()==1) return; // View is impossibly small (probably off-screen or something) so just skip rendering

        // Call pre-draw function in case something may need to be partially occluded
        preDraw(cardDraw);

        // Clear everything
        cardDraw.drawColor(0);

        // Draw actual card
        ovCard.left = 0;
        ovCard.top = 0;
        ovCard.right = cardDraw.getWidth()/cornerSharpness;
        ovCard.bottom = ovCard.right;

        cardDraw.drawArc(ovCard, 180, 90, true, color);

        ovCard.right = cardDraw.getWidth();
        ovCard.left = cardDraw.getWidth() - cardDraw.getWidth()/cornerSharpness;
        ovCard.top = 0;
        ovCard.bottom = cardDraw.getWidth()/cornerSharpness;

        cardDraw.drawArc(ovCard, 270, 90, true, color);

        ovCard.top = (ovCard.bottom = cardDraw.getHeight()) - cardDraw.getWidth()/cornerSharpness;

        cardDraw.drawArc(ovCard, 0, 90, true, color);

        ovCard.left = 0;
        ovCard.right = cardDraw.getWidth()/cornerSharpness;

        cardDraw.drawArc(ovCard, 90, 90, true, color);

        ovCard.top = ovCard.right/2F;

        ovCard.bottom = cardDraw.getHeight() - ovCard.top;

        cardDraw.drawRect(0, cardDraw.getWidth()/(2*cornerSharpness), cardDraw.getWidth()/cornerSharpness, cardDraw.getHeight()-cardDraw.getWidth()/(2*cornerSharpness), color);
        cardDraw.drawRect(cardDraw.getWidth()-cardDraw.getWidth()/(2*cornerSharpness), cardDraw.getWidth()/(2*cornerSharpness), cardDraw.getWidth(), cardDraw.getHeight()-cardDraw.getWidth()/(2*cornerSharpness), color);
        cardDraw.drawRect(cardDraw.getWidth()/(2*cornerSharpness), 0, cardDraw.getWidth()-cardDraw.getWidth()/(2*cornerSharpness), cardDraw.getHeight(), color);

        // Let sub-classes draw class-specific items on top of card
        drawItems(cardDraw);

        // Draw to actual canvas
        canvas.drawBitmap(cardSurface,(canvas.getWidth()-cardDraw.getWidth())/2, (canvas.getHeight()-cardDraw.getHeight())/2, null); // Draw bitmap with calculated padding

        // Process whether or not view should be invalidated
        if(shouldUpdate()) invalidate();
    }

    /**
     * Draw action-specific card items on top of card.
     * @param canvas Canvas to draw to
     */
    protected abstract void    drawItems   (Canvas canvas);
    protected          void    preDraw     (Canvas canvas){ }
    protected          boolean shouldUpdate()             { return false; }
}
