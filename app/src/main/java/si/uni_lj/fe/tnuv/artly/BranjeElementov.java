package si.uni_lj.fe.tnuv.artly;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BranjeElementov {

    // Vrne seznam imen vseh drawable-jev in poti do shranjenih elementov
    public static List<String> getElementDrawables(Context context) {
        List<String> elementNames = new ArrayList<>();

        // Drawable resursi
        Field[] drawables = R.drawable.class.getFields();
        for (Field field : drawables) {
            if (field.getName().startsWith("element")) {
                elementNames.add(field.getName());
            }
        }

        // Lokalno shranjeni elementi
        File directory = new File(context.getFilesDir(), "custom_elements");
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("element")) {
                        elementNames.add(file.getAbsolutePath());
                    }
                }
            }
        }

        Log.d("Element names: ", elementNames.toString());
        return elementNames;
    }
}
