package si.uni_lj.fe.tnuv.artly;

import java.util.ArrayList;
import java.util.List;

public class DrawingState {
    public List<ActionData> actions = new ArrayList<>();

    public static class ActionData {
        public boolean isStroke;
        public StrokeData stroke;
        public ElementData element;
    }

    public static class StrokeData {
        public List<Float> points = new ArrayList<>();
        public int color;
        public float width;
        public boolean isEraser;
    }

    public static class ElementData {
        public String identifier;
        public float[] matrixValues = new float[9];
    }
}
