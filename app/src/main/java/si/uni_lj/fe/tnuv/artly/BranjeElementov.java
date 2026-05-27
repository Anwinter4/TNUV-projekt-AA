package si.uni_lj.fe.tnuv.artly;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BranjeElementov {

    // Vrne seznam imen vseh drawable-jev, ki se začnejo z "element" -- tako poimenuj vse nalepke!!
    public static List<String> getElementDrawables() {
        List<String> elementNames = new ArrayList<>();

        Field[] drawables = R.drawable.class.getFields();

        for (Field field : drawables) {
            if (field.getName().startsWith("element")) {
                elementNames.add(field.getName());
            }
        }
        Log.d("Element names: ", elementNames.toString());

        return elementNames;
    }
}