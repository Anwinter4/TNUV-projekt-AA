package si.uni_lj.fe.tnuv.artly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Paint paint;
    private Path path;
    private Canvas canvas;
    private Bitmap drawingBitmap;
    private float lastX, lastY;
    private Bitmap backgroundBitmap;

    private boolean isDrawingEnabled = false;
    private boolean isEraserMode = false;

    // Seznami za Undo/Redo in elemente
    private final List<Action> undoStack = new ArrayList<>();
    private final List<Action> redoStack = new ArrayList<>();
    private final List<SlikovniElement> elementi = new ArrayList<>();
    
    private SlikovniElement izbranElement = null;
    private ScaleGestureDetector scaleGestureDetector;
    private float lastRotationAngle = 0;

    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        path = new Path();

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (izbranElement != null && !isDrawingEnabled) {
                    izbranElement.matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                    invalidate();
                    return true;
                }
                return false;
            }
        });
    }

    public void setDrawingEnabled(boolean enabled) {
        this.isDrawingEnabled = enabled;
        this.isEraserMode = false;
        paint.setXfermode(null);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10f);
        if (enabled) izbranElement = null;
        invalidate();
    }

    public void setEraserMode(boolean eraser) {
        this.isEraserMode = eraser;
        this.isDrawingEnabled = eraser;
        if (eraser) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            paint.setStrokeWidth(60f);
        } else {
            paint.setXfermode(null);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(10f);
        }
        invalidate();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.add(undoStack.remove(undoStack.size() - 1));
            rebuildEverything();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.add(redoStack.remove(redoStack.size() - 1));
            rebuildEverything();
        }
    }

    public void clearAll() {
        undoStack.clear();
        redoStack.clear();
        rebuildEverything();
    }

    private void rebuildEverything() {
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            elementi.clear();
            for (Action action : undoStack) {
                action.perform(canvas, elementi);
            }
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(drawingBitmap);
            canvas.drawColor(Color.TRANSPARENT);
            rebuildEverything();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundBitmap != null) {
            srcRect.set(0, 0, backgroundBitmap.getWidth(), backgroundBitmap.getHeight());
            dstRect.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(backgroundBitmap, srcRect, dstRect, null);
        } else {
            canvas.drawColor(Color.WHITE);
        }

        for (SlikovniElement el : elementi) el.draw(canvas);

        if (drawingBitmap != null) {
            canvas.drawBitmap(drawingBitmap, 0, 0, null);
        }
        
        if (isDrawingEnabled && !isEraserMode) {
            canvas.drawPath(path, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();

        if (event.getPointerCount() == 2 && izbranElement != null && !isDrawingEnabled) {
            float angle = getAngle(event);
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                lastRotationAngle = angle;
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float deltaAngle = angle - lastRotationAngle;
                izbranElement.matrix.postRotate(deltaAngle, getMidX(event), getMidY(event));
                lastRotationAngle = angle;
            }
            invalidate();
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                
                izbranElement = null;
                // Vedno najprej preverimo nalepke - če kliknemo nanjo, pero izklopimo
                for (int i = elementi.size() - 1; i >= 0; i--) {
                    if (elementi.get(i).contains(x, y)) {
                        izbranElement = elementi.get(i);
                        elementi.remove(i);
                        elementi.add(izbranElement);
                        isDrawingEnabled = false;
                        break;
                    }
                }

                if (izbranElement == null && isDrawingEnabled) {
                    path.moveTo(x, y);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (izbranElement != null && !isDrawingEnabled) {
                    izbranElement.matrix.postTranslate(x - lastX, y - lastY);
                } else if (isDrawingEnabled) {
                    path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                    if (isEraserMode && canvas != null) {
                        canvas.drawPath(path, paint);
                    }
                }
                lastX = x;
                lastY = y;
                break;

            case MotionEvent.ACTION_UP:
                if (izbranElement == null && isDrawingEnabled) {
                    path.lineTo(x, y);
                    if (canvas != null) {
                        canvas.drawPath(path, paint);
                        undoStack.add(new StrokeAction(new Path(path), new Paint(paint)));
                        redoStack.clear();
                    }
                }
                path.reset();
                break;
        }
        invalidate();
        return true;
    }

    private float getAngle(MotionEvent event) {
        double dx = event.getX(0) - event.getX(1);
        double dy = event.getY(0) - event.getY(1);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
    private float getMidX(MotionEvent event) { return (event.getX(0) + event.getX(1)) / 2; }
    private float getMidY(MotionEvent event) { return (event.getY(0) + event.getY(1)) / 2; }

    public void dodajSliko(Bitmap bitmap) {
        if (bitmap != null) {
            float offset = elementi.size() * 60f;
            SlikovniElement el = new SlikovniElement(bitmap, 150 + offset, 150 + offset, 450);
            undoStack.add(new ElementAction(el));
            redoStack.clear();
            rebuildEverything();
            setDrawingEnabled(false);
        }
    }

    public void addSvgElement(int resId) {
        Drawable d = ContextCompat.getDrawable(getContext(), resId);
        if (d != null) {
            Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            d.setBounds(0, 0, c.getWidth(), c.getHeight());
            d.draw(c);
            dodajSliko(b);
        }
    }

    private interface Action {
        void perform(Canvas drawingCanvas, List<SlikovniElement> elements);
    }

    private static class StrokeAction implements Action {
        Path path; Paint paint;
        StrokeAction(Path p, Paint pt) { this.path = p; this.paint = pt; }
        public void perform(Canvas c, List<SlikovniElement> e) { c.drawPath(path, paint); }
    }

    private static class ElementAction implements Action {
        SlikovniElement element;
        ElementAction(SlikovniElement el) { this.element = el; }
        public void perform(Canvas c, List<SlikovniElement> e) { e.add(element); }
    }

    private static class SlikovniElement {
        Bitmap bitmap; Matrix matrix = new Matrix();
        SlikovniElement(Bitmap b, float x, float y, float w) {
            this.bitmap = b;
            float s = w / b.getWidth();
            matrix.postScale(s, s); matrix.postTranslate(x, y);
        }
        void draw(Canvas c) { c.drawBitmap(bitmap, matrix, null); }
        boolean contains(float x, float y) {
            Matrix inv = new Matrix(); matrix.invert(inv);
            float[] pts = {x, y}; inv.mapPoints(pts);
            return pts[0] >= 0 && pts[0] <= bitmap.getWidth() && pts[1] >= 0 && pts[1] <= bitmap.getHeight();
        }
    }
}
