package si.uni_lj.fe.tnuv.artly;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

public class DrawingView extends View {

    private Paint paint;
    private Path path;
    private Canvas canvas;
    private Bitmap bitmap;
    private float lastX, lastY;
    private boolean erasing = false;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                lastX = x;
                lastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - lastX);
                float dy = Math.abs(y - lastY);
                if (dx >= 4 || dy >= 4) {
                    path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                    lastX = x;
                    lastY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                path.lineTo(x, y);
                if (canvas != null) {
                    canvas.drawPath(path, paint);
                }
                path.reset();
                break;
        }
        invalidate();
        return true;
    }

    public void setErasing(boolean erasing) {
        this.erasing = erasing;
        if (erasing) {
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(30f);
        } else {
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(10f);
        }
    }

    public void clearCanvas() {
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
            invalidate();
        }
    }

    public void exportImage() {
        // Implementiraj shranjevanje slike
    }

    public void saveToGallery() {
        // Implementiraj shranjevanje v galerijo
    }

    public void addSvgElement(int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), drawableResId);

        if (drawable != null && canvas != null) {
            int width = 100;
            int height = 100;
            float left = (getWidth() - width) / 2f;
            float top = (getHeight() - height) / 2f;

            drawable.setBounds((int) left, (int) top, (int) left + width, (int) top + height);
            drawable.draw(canvas);
            invalidate();
        }
    }
}