package si.uni_lj.fe.tnuv.artly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class NalepkaView extends View {

    private Paint paint;
    private Paint selectionPaint; // Čopič za okvirček izbire
    private Path path;
    private Canvas canvas;
    private Bitmap drawingBitmap;
    private float lastX, lastY;
    private Bitmap backgroundBitmap;
    private boolean isDrawingEnabled = false;
    private boolean isEraserMode = false;
    private float currentPenSize = 10f;
    private int currentPencilColor = Color.BLACK;

    private final List<Action> undoStack = new ArrayList<>();
    private final List<Action> redoStack = new ArrayList<>();
    private final List<SlikovniElement> elementi = new ArrayList<>();

    private SlikovniElement izbranElement = null;
    private Matrix startMatrix = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private float lastRotationAngle = 0;

    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    public NalepkaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(currentPencilColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(currentPenSize);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        path = new Path();

        // Nastavitev čopiča za okvirček izbrane nalepke
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.parseColor("#C06084"));
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(3f);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (izbranElement != null && !isDrawingEnabled) {
                    izbranElement.matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                    rebuildEverything(); // Osveži bitmap med skaliranjem
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
        paint.setColor(currentPencilColor);
        paint.setStrokeWidth(currentPenSize);
        if (enabled) izbranElement = null;
        invalidate();
    }

    public void setEraserMode(boolean eraser) {
        this.isEraserMode = eraser;
        this.isDrawingEnabled = true; // Both pencil and eraser use drawing mode
        if (eraser) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            paint.setStrokeWidth(60f);
            izbranElement = null;
        } else {
            paint.setXfermode(null);
            paint.setColor(currentPencilColor);
            paint.setStrokeWidth(currentPenSize);
        }
        invalidate();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.add(undoStack.remove(undoStack.size() - 1));
            rebuildEverything();
            notifyStateChanged();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.add(redoStack.remove(redoStack.size() - 1));
            rebuildEverything();
            notifyStateChanged();
        }
    }

    public boolean hasSelection() {
        return izbranElement != null;
    }

    public void deleteSelected() {
        if (izbranElement != null) {
            undoStack.add(new RemoveElementAction(izbranElement));
            redoStack.clear();
            izbranElement = null;
            rebuildEverything();
            notifyStateChanged();
        }
    }

    public interface OnStateChangeListener {
        void onStateChanged();
    }

    private OnStateChangeListener stateChangeListener;

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    private void notifyStateChanged() {
        if (stateChangeListener != null) {
            stateChangeListener.onStateChanged();
        }
    }

    public void greyUndo(ImageButton button) {
        boolean canUndo = !undoStack.isEmpty();
        button.setAlpha(canUndo ? 1.0f : 0.5f);
        button.setEnabled(canUndo);
    }

    public void greyRedo(ImageButton button) {
        boolean canRedo = !redoStack.isEmpty();
        button.setAlpha(canRedo ? 1.0f : 0.5f);
        button.setEnabled(canRedo);
    }

    public void clearAll() {
        undoStack.clear();
        redoStack.clear();
        izbranElement = null;
        rebuildEverything();
        notifyStateChanged();
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

        // 1. Ozadje
        if (backgroundBitmap != null) {
            srcRect.set(0, 0, backgroundBitmap.getWidth(), backgroundBitmap.getHeight());
            dstRect.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(backgroundBitmap, srcRect, dstRect, null);
        } else {
            canvas.drawColor(Color.TRANSPARENT);
        }

        // 2. IZRIS VSEGA (Nalepke in svinčnik so zdaj v drawingBitmap v pravilnem vrstnem redu)
        if (drawingBitmap != null) {
            canvas.drawBitmap(drawingBitmap, 0, 0, null);
        }

        // 3. Okvirček za izbrano nalepko (narišemo na vrhu, da se vidi med premikanjem)
        if (izbranElement != null) {
            izbranElement.drawSelectionFrame(canvas, selectionPaint);
        }

        // 4. Trenutna linija, ki jo ravno rišemo (še ni v stacku)
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
                rebuildEverything(); // Osveži ob rotaciji
            }
            invalidate();
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;

                SlikovniElement najdenElement = null;
                for (int i = elementi.size() - 1; i >= 0; i--) {
                    if (elementi.get(i).contains(x, y)) {
                        najdenElement = elementi.get(i);
                        startMatrix.set(najdenElement.matrix);
                        moveElementActionToFront(najdenElement);
                        rebuildEverything(); // Takoj premakni na vrh tudi v bitmapu
                        isDrawingEnabled = false;
                        break;
                    }
                }

                izbranElement = najdenElement;


                if (izbranElement == null && isDrawingEnabled) {
                    path.moveTo(x, y);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (izbranElement != null && !isDrawingEnabled) {
                    izbranElement.matrix.postTranslate(x - lastX, y - lastY);
                    rebuildEverything(); // Osveži med premikanjem nalepke
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
                if (izbranElement != null && !isDrawingEnabled) {
                    if (!isPointInView(x, y)) {
                        izbranElement.matrix.set(startMatrix);
                        rebuildEverything();
                    }
                } else if (izbranElement == null && isDrawingEnabled) {
                    path.lineTo(x, y);
                    if (canvas != null) {
                        canvas.drawPath(path, paint);
                        undoStack.add(new StrokeAction(new Path(path), new Paint(paint)));
                        redoStack.clear();
                        notifyStateChanged();
                    }
                }
                path.reset();
                break;
        }
        invalidate();
        return true;
    }


    // Preveri če je prst še vedno znotraj belega platna
    private boolean isPointInView(float x, float y) {
        return (x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight());
    }

    private void moveElementActionToFront(SlikovniElement element) {
        Action found = null;
        for (int i = 0; i < undoStack.size(); i++) {
            Action a = undoStack.get(i);
            if (a instanceof ElementAction && ((ElementAction) a).element == element) {
                found = undoStack.remove(i);
                break;
            }
        }
        if (found != null) {
            undoStack.add(found);
        }
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
            Drawable d = new BitmapDrawable(getResources(), bitmap);
            float offset = elementi.size() * 60f;
            SlikovniElement el = new SlikovniElement(d, 150 + offset, 150 + offset, 450);
            undoStack.add(new ElementAction(el));
            redoStack.clear();
            rebuildEverything();
            setDrawingEnabled(false);
            izbranElement = el;
            notifyStateChanged();
        }
    }

    public void addSvgElement(int resId) {
        Drawable d = ContextCompat.getDrawable(getContext(), resId);
        if (d != null) {
            float offset = elementi.size() * 60f;
            SlikovniElement el = new SlikovniElement(d, 150 + offset, 150 + offset, 450);
            undoStack.add(new ElementAction(el));
            redoStack.clear();
            rebuildEverything();
            izbranElement = el;
            notifyStateChanged();
        }
    }

    public void addElement(String identifier) {
        if (identifier.startsWith("/")) {
            Bitmap b = BitmapFactory.decodeFile(identifier);
            if (b != null) {
                dodajSliko(b);
            }
        } else {
            int resId = getContext().getResources().getIdentifier(identifier, "drawable", getContext().getPackageName());
            if (resId != 0) {
                addSvgElement(resId);
            }
        }
    }

    private interface Action {
        void perform(Canvas drawingCanvas, List<SlikovniElement> elements);
    }

    private static class StrokeAction implements NalepkaView.Action {
        Path path; Paint paint;
        StrokeAction(Path p, Paint pt) { this.path = p; this.paint = pt; }
        public void perform(Canvas c, List<SlikovniElement> e) { c.drawPath(path, paint); }
    }

    private static class ElementAction implements NalepkaView.Action {
        SlikovniElement element;
        ElementAction(SlikovniElement el) { this.element = el; }
        public void perform(Canvas c, List<SlikovniElement> e) {
            e.add(element);
            element.draw(c); // Nalepka se zdaj nariše v bitmap
        }
    }

    private static class RemoveElementAction implements NalepkaView.Action {
        SlikovniElement element;
        RemoveElementAction(SlikovniElement el) { this.element = el; }
        public void perform(Canvas c, List<SlikovniElement> e) { e.remove(element); }
    }

    private static class SlikovniElement {
        Drawable drawable;
        Matrix matrix = new Matrix();
        int width, height;

        SlikovniElement(Drawable d, float x, float y, float w) {
            this.drawable = d;
            this.width = d.getIntrinsicWidth();
            this.height = d.getIntrinsicHeight();
            float s = w / width;
            matrix.postScale(s, s);
            matrix.postTranslate(x, y);
        }

        void draw(Canvas c) {
            c.save();
            c.concat(matrix);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(c);
            c.restore();
        }

        void drawSelectionFrame(Canvas c, Paint p) {
            float[] pts = {0, 0, width, 0, width, height, 0, height};
            matrix.mapPoints(pts);

            Path framePath = new Path();
            framePath.moveTo(pts[0], pts[1]);
            framePath.lineTo(pts[2], pts[3]);
            framePath.lineTo(pts[4], pts[5]);
            framePath.lineTo(pts[6], pts[7]);
            framePath.close();
            c.drawPath(framePath, p);

            p.setStyle(Paint.Style.FILL);
            c.drawCircle(pts[0], pts[1], 15f, p); // Zgoraj levo
            c.drawCircle(pts[4], pts[5], 15f, p); // Spodaj desno
            p.setStyle(Paint.Style.STROKE);

        }

        boolean contains(float x, float y) {
            Matrix inv = new Matrix();
            matrix.invert(inv);
            float[] pts = {x, y};
            inv.mapPoints(pts);
            return pts[0] >= 0 && pts[0] <= width && pts[1] >= 0 && pts[1] <= height;
        }
    }

    public void setPencilColor(int color) {
        this.currentPencilColor = color;
        this.isDrawingEnabled = true;
        this.isEraserMode = false;
        this.izbranElement = null;

        paint.setXfermode(null);
        paint.setColor(currentPencilColor);
        paint.setStrokeWidth(currentPenSize);
        invalidate();
    }

    public Bitmap getFinalBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;
        Bitmap result = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.TRANSPARENT);
        if (drawingBitmap != null) {
            canvas.drawBitmap(drawingBitmap, 0, 0, null);
        }
        return result;
    }

    public void penSize1(){
        currentPenSize = 10f;
        updatePaint();
    }
    public void penSize3(){
        currentPenSize = 30f;
        updatePaint();
    }
    public void penSize5(){
        currentPenSize = 50f;
        updatePaint();
    }

    private void updatePaint() {
        if (isDrawingEnabled && !isEraserMode) {
            paint.setStrokeWidth(currentPenSize);
        }
        invalidate();
    }
}
