package si.uni_lj.fe.tnuv.artly;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import top.defaults.colorpicker.ColorPickerView;

public class UstvariSliko extends AppCompatActivity {

    private DrawingView drawingView;
    private RecyclerView elementRecyclerView;
    private ElementAdapter elementAdapter;
    private ImageButton previousArrow, nextArrow, addUstvariNalepko, dodajSliko, btnPencil, btnEraser, btnReverse, btnRedo, btnTrash, btnBack, btnExport;

    private Button btnAlbum;
    private Button btnShrani;
    private EditText vnosnoPolje;
    private List<String> vsiElementi;
    private View mColorPreview;
    private int mDefaultColor = Color.BLACK;

    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ustvari_sliko);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ustvariSliko), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawingView = findViewById(R.id.canvas);
        elementRecyclerView = findViewById(R.id.elementRecyclerView);
        previousArrow = findViewById(R.id.previousArrow);
        nextArrow = findViewById(R.id.nextArrow);
        addUstvariNalepko = findViewById(R.id.addUstvariNalepkoGrey);
        dodajSliko = findViewById(R.id.btnPlus);
        btnPencil = findViewById(R.id.btnPencil);
        btnEraser = findViewById(R.id.btnEraser);
        btnReverse = findViewById(R.id.btnReverse);
        btnRedo = findViewById(R.id.btnRedo);
        btnTrash = findViewById(R.id.btnTrash);
        btnBack = findViewById(R.id.btnBack);
        btnExport = findViewById(R.id.btnExport);
        btnAlbum = findViewById(R.id.btnAlbum);

        btnShrani = findViewById(R.id.btnShrani);
        vnosnoPolje = findViewById(R.id.vnosno_polje);
        mColorPreview = findViewById(R.id.preview_selected_color);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                prikaziDialogZaIzhod();
            }
        });

        vsiElementi = BranjeElementov.getElementDrawables(this);
        elementRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        elementAdapter = new ElementAdapter(vsiElementi, drawableId -> {
            drawingView.addElement(drawableId);
        });

        elementRecyclerView.setAdapter(elementAdapter);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("imagePath")) {
            String path = intent.getStringExtra("imagePath");
            String name = intent.getStringExtra("imageName");
            if (path != null) {
                String statePath = path.substring(0, path.lastIndexOf(".")) + R.string.koncnica_json;
                File stateFile = new File(statePath);
                
                if (stateFile.exists()) {
                    String json = preberiDatoteko(stateFile);
                    if (json != null) {
                        try {
                            DrawingState state = new Gson().fromJson(json, DrawingState.class);
                            drawingView.post(() -> drawingView.loadState(state));
                        } catch (Exception e) {
                            e.printStackTrace();
                            naloziSamoSliko(path);
                        }
                    } else {
                        naloziSamoSliko(path);
                    }
                } else {
                    naloziSamoSliko(path);
                }
            }
            if (name != null) {
                vnosnoPolje.setText(name);
            }
        }

        previousArrow.setOnClickListener(v -> {
            elementAdapter.naPrejsnjoStran();
            posodobiGumbe();
        });

        nextArrow.setOnClickListener(v -> {
            elementAdapter.naNaslednjoStran();
            posodobiGumbe();
        });

        addUstvariNalepko.setOnClickListener(v -> {
            Intent nextIntent = new Intent(UstvariSliko.this, UstvariNalepko.class);
            startActivity(nextIntent);
        });

        dodajSliko.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, PICK_IMAGE);
        });

        btnEraser.setOnClickListener(v -> {
            if (drawingView.isEraserMode()) {
                drawingView.setDrawingEnabled(false);
            } else {
                drawingView.setEraserMode(true);
                Toast.makeText(this, R.string.radirka_vklopljena, Toast.LENGTH_SHORT).show();
            }
            posodobiGumbe();
        });

        btnPencil.setOnClickListener(v -> {if (drawingView.isDrawingEnabled() && !drawingView.isEraserMode()) {
            // Če je pero že vklopljeno, ga ob navadnem kliku IZKLOPI
            drawingView.setDrawingEnabled(false);
            posodobiGumbe();
        } else {
            // Če pero ni vklopljeno, ga vklopi in odpri paleto
            drawingView.setEraserMode(false);
            drawingView.setDrawingEnabled(true);
            prikaziBarvnoPaleto();
            posodobiGumbe();
        }
        });

        btnPencil.setOnLongClickListener(v -> {
            if (drawingView.isDrawingEnabled() && !drawingView.isEraserMode()) {
                prikaziBarvnoPaleto();
            } else {
                // Če pero še ni vklopljeno, ga vklopi in odpri paleto
                drawingView.setEraserMode(false);
                drawingView.setDrawingEnabled(true);
                prikaziBarvnoPaleto();
                posodobiGumbe();
            }
            return true; // true pomeni, da smo dogodek "porabili" in se navaden klik ne izvede
        });

        btnTrash.setOnClickListener(v -> {
            if (drawingView.hasSelection()) {
                drawingView.deleteSelected();
                Toast.makeText(this, R.string.element_izbrisan, Toast.LENGTH_SHORT).show();
            } else {
                drawingView.clearAll();
                Toast.makeText(this, R.string.platno_ociseno, Toast.LENGTH_SHORT).show();
            }
        });

        btnReverse.setOnClickListener(v -> {
            drawingView.undo();
        });

        btnRedo.setOnClickListener(v -> {
            drawingView.redo();
        });

        drawingView.setOnStateChangeListener(() -> {
            drawingView.greyUndo(btnReverse);
            drawingView.greyRedo(btnRedo);
        });

        drawingView.greyUndo(btnReverse);
        drawingView.greyRedo(btnRedo);

        btnBack.setOnClickListener(v -> prikaziDialogZaIzhod());

        btnShrani.setOnClickListener(v -> {
            if(!vnosnoPolje.getText().toString().isEmpty()) {
                shraniSliko(vnosnoPolje.getText().toString());
            } else {
                Toast.makeText(this, R.string.opozorilo_prazno_vnosno_polje, Toast.LENGTH_SHORT).show();
            }
        });

        btnExport.setOnClickListener(v -> exportajSliko());

        btnAlbum.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.pojdi_v_album)
                    .setMessage(R.string.preveri_pred_zapustitvijo_strani)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        Intent nextIntent = new Intent(UstvariSliko.this, Album.class);
                        startActivity(nextIntent);
                    })
                    .setNegativeButton(R.string.preklici, null)
                    .show();
        });

        posodobiGumbe();
    }

    private void prikaziBarvnoPaleto() {
        View popupView = LayoutInflater.from(UstvariSliko.this).inflate(R.layout.top_defaults_view_color_picker_popup, null);
        AlertDialog dialog = new AlertDialog.Builder(UstvariSliko.this)
                .setView(popupView)
                .create();

        ColorPickerView colorPickerView = popupView.findViewById(R.id.colorPickerView);
        colorPickerView.setInitialColor(mDefaultColor);

        View colorIndicator = popupView.findViewById(R.id.colorIndicator);
        if (colorIndicator != null) {
            colorIndicator.setBackgroundColor(mDefaultColor);
        }

        colorPickerView.subscribe((color, fromUser, shouldPropagate) -> {
            if (colorIndicator != null) {
                colorIndicator.setBackgroundColor(color);
            }
        });

        TextView btnOk = popupView.findViewById(R.id.ok);
        btnOk.setText(R.string.ok);
        btnOk.setOnClickListener(view -> {
            mDefaultColor = colorPickerView.getColor();
            mColorPreview.setBackgroundColor(mDefaultColor);
            drawingView.setPencilColor(mDefaultColor);
            dialog.dismiss();
        });

        TextView btnCancel = popupView.findViewById(R.id.cancel);
        btnCancel.setText(R.string.preklici);
        btnCancel.setOnClickListener(view -> dialog.dismiss());

        popupView.findViewById(R.id.btnSize1).setOnClickListener(view -> {
            drawingView.penSize1();
            Toast.makeText(UstvariSliko.this, R.string.tanka_crta, Toast.LENGTH_SHORT).show();
        });
        popupView.findViewById(R.id.btnSize3).setOnClickListener(view -> {
            drawingView.penSize3();
            Toast.makeText(UstvariSliko.this, R.string.navadna_crta, Toast.LENGTH_SHORT).show();
        });
        popupView.findViewById(R.id.btnSize5).setOnClickListener(view -> {
            drawingView.penSize5();
            Toast.makeText(UstvariSliko.this, R.string.debela_crta, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }


    private void naloziSamoSliko(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            drawingView.post(() -> drawingView.setBackgroundBitmap(bitmap));
        }
    }

    private String preberiDatoteko(File file) {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            return text.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void prikaziDialogZaIzhod() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vrni_na_domaco_stran)
                .setMessage(R.string.preveri_pred_zapustitvijo_strani)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    finish();
                })
                .setNegativeButton(R.string.preklici, null)
                .show();
    }

    private void shraniSliko(String imeSlike) {
        Bitmap bitmap = drawingView.getFinalBitmap();
        if (bitmap == null) {
            Toast.makeText(this, R.string.napaka_pri_generiranju_slike, Toast.LENGTH_SHORT).show();
            return;
        }

        File directory = new File(getFilesDir(), "album");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, imeSlike + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,R.string.napaka_shrani_png, Toast.LENGTH_SHORT).show();
            return;
        }

        DrawingState state = drawingView.getState();
        String json = new Gson().toJson(state);
        File stateFile = new File(directory, imeSlike + ".json");
        try (FileWriter writer = new FileWriter(stateFile)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,R.string.napaka_shrani_stanje, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.slika_imeSlike) + imeSlike + getString(R.string.imeSlike_shranjena), Toast.LENGTH_SHORT).show();        vnosnoPolje.setText("");
        drawingView.clearAll();
        finish();
    }

    public void exportajSliko() {
        Bitmap bitmap = drawingView.getFinalBitmap();
        if (bitmap == null) {
            Toast.makeText(this, R.string.napaka_pri_generiranju_slike, Toast.LENGTH_SHORT).show();
            return;
        }

        String vnesenoIme = vnosnoPolje.getText().toString().trim();
        String imeDatoteke;

        if (vnesenoIme.isEmpty()) {
            imeDatoteke = getString(R.string.artly_slika) + System.currentTimeMillis() + R.string.koncnica_png;
        } else {
            imeDatoteke = vnesenoIme + "_" + System.currentTimeMillis() + R.string.koncnica_png;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imeDatoteke);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Artly");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }

                Toast.makeText(this, R.string.uspeh_shranjena_slika, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.napaka_izvoz_slike, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        osveziElemente();
    }

    private void osveziElemente() {
        vsiElementi = BranjeElementov.getElementDrawables(this);
        if (elementAdapter != null) {
            elementAdapter.setElementi(vsiElementi);
            posodobiGumbe();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri == null) return;

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();

                if (bitmap != null) {
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    drawingView.dodajSliko(bitmap);
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.napaka_nalaganje_slike, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (android.os.Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        } else {
            ei = new ExifInterface(selectedImage.getPath());
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if (input != null) input.close();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
            }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }



    private void posodobiGumbe() {
        int barvaAktivna = ContextCompat.getColor(this, R.color.temno_roza); // Tvoja barva iz colors.xml
        int barvaNeaktivna = Color.TRANSPARENT; // Prosojno ozadje

        // Gumb za svinčnik
        if (drawingView.isDrawingEnabled() && !drawingView.isEraserMode()) {
            btnPencil.setBackgroundTintList(ColorStateList.valueOf(barvaAktivna));
        } else {
            btnPencil.setBackgroundTintList(ColorStateList.valueOf(barvaNeaktivna));
        }

        // Gumb za radirko
        if (drawingView.isEraserMode()) {
            btnEraser.setBackgroundTintList(ColorStateList.valueOf(barvaAktivna));
        } else {
            btnEraser.setBackgroundTintList(ColorStateList.valueOf(barvaNeaktivna));
        }

        if (elementAdapter != null) {
            previousArrow.setEnabled(elementAdapter.imaPrejsnjoStran());
            previousArrow.setAlpha(elementAdapter.imaPrejsnjoStran() ? 1.0f : 0.5f);

            nextArrow.setEnabled(elementAdapter.imaNaslednjoStran());
            nextArrow.setAlpha(elementAdapter.imaNaslednjoStran() ? 1.0f : 0.5f);
        }
    }
}
