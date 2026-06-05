package si.uni_lj.fe.tnuv.artly;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import top.defaults.colorpicker.ColorPickerView;

public class UstvariNalepko extends AppCompatActivity {

    private NalepkaView drawingView;
    private RecyclerView elementRecyclerView;
    private ElementAdapter elementAdapter;
    private ImageButton previousArrow, nextArrow;
    private ImageButton btnReverse, btnRedo;
    private ImageButton btnEraser, btnPencil, dodajSliko, btnTrash, btnBack,btnExport;
    private Button btnShrani, btnPreklici;
    private List<String> vsiElementi;
    private View mColorPreview;

    private static final int PICK_IMAGE = 1;

    // this is the default color of the preview box
    private int mDefaultColor = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ustvari_nalepko);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ustvariNalepko), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Poveži poglede
        drawingView = findViewById(R.id.canvasNalepka);
        elementRecyclerView = findViewById(R.id.elementRecyclerView);
        previousArrow = findViewById(R.id.previousArrow);
        nextArrow = findViewById(R.id.nextArrow);
        btnReverse = findViewById(R.id.btnReverse);
        btnRedo = findViewById(R.id.btnRedo);
        btnEraser = findViewById(R.id.btnEraser);
        btnPencil = findViewById(R.id.btnPencil);
        dodajSliko = findViewById(R.id.btnPlus);
        btnTrash = findViewById(R.id.btnTrash);
        btnExport = findViewById(R.id.btnExport);

        btnShrani = findViewById(R.id.btnShrani);
        btnPreklici = findViewById(R.id.btnPreklici);

        mColorPreview = findViewById(R.id.preview_selected_color);

        vsiElementi = BranjeElementov.getElementDrawables(this);

        // Setup RecyclerView with a vertical layout
        elementRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        elementAdapter = new ElementAdapter(vsiElementi, identifier -> {
            drawingView.addSvgElement(getResources().getIdentifier(identifier, "drawable", getPackageName()));
        });

        elementRecyclerView.setAdapter(elementAdapter);

        // Navigacija
        previousArrow.setOnClickListener(v -> {
            elementAdapter.naPrejsnjoStran();
            posodobiGumbe();
        });

        nextArrow.setOnClickListener(v -> {
            elementAdapter.naNaslednjoStran();
            posodobiGumbe();
        });

        dodajSliko.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnEraser.setOnClickListener(v -> {
            drawingView.setEraserMode(true);
            Toast.makeText(this, "Radirka vklopljena", Toast.LENGTH_SHORT).show();
        });
        // POSODOBLJEN GUMB ZA KOŠ
        btnTrash.setOnClickListener(v -> {
            if (drawingView.hasSelection()) {
                drawingView.deleteSelected();
                Toast.makeText(this, "Element izbrisan", Toast.LENGTH_SHORT).show();
            } else {
                drawingView.clearAll();
                Toast.makeText(this, "Platno očiščeno", Toast.LENGTH_SHORT).show();
            }
        });
        btnExport.setOnClickListener(v -> exportajNalepko());
        // Gumb za nazaj (Undo)
        btnReverse.setOnClickListener(v -> {
            drawingView.undo();
        });

        // Gumb za naprej (Redo)
        btnRedo.setOnClickListener(v -> {
            drawingView.redo();
        });

        drawingView.setOnStateChangeListener(() -> {
            drawingView.greyUndo(btnReverse);
            drawingView.greyRedo(btnRedo);
        });

        // Pokličite še ročno na začetku, da nastavite začetno stanje (oba siva)
        drawingView.greyUndo(btnReverse);
        drawingView.greyRedo(btnRedo);

        btnPencil.setOnClickListener(v -> {
            drawingView.setEraserMode(false);
            
            View popupView = LayoutInflater.from(UstvariNalepko.this).inflate(R.layout.top_defaults_view_color_picker_popup, null);
            AlertDialog dialog = new AlertDialog.Builder(UstvariNalepko.this)
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
            btnOk.setText("V redu");
            btnOk.setOnClickListener(view -> {
                mDefaultColor = colorPickerView.getColor();
                mColorPreview.setBackgroundColor(mDefaultColor);
                drawingView.setPencilColor(mDefaultColor);
                dialog.dismiss();
            });

            TextView btnCancel = popupView.findViewById(R.id.cancel);
            btnCancel.setText("Prekliči");
            btnCancel.setOnClickListener(view -> dialog.dismiss());

            popupView.findViewById(R.id.btnSize1).setOnClickListener(view -> {
                drawingView.penSize1();
                Toast.makeText(UstvariNalepko.this, "Debelina 1 izbrana", Toast.LENGTH_SHORT).show();
            });
            popupView.findViewById(R.id.btnSize3).setOnClickListener(view -> {
                drawingView.penSize3();
                Toast.makeText(UstvariNalepko.this, "Debelina 3 izbrana", Toast.LENGTH_SHORT).show();
            });
            popupView.findViewById(R.id.btnSize5).setOnClickListener(view -> {
                drawingView.penSize5();
                Toast.makeText(UstvariNalepko.this, "Debelina 5 izbrana", Toast.LENGTH_SHORT).show();
            });

            dialog.show();
        });

        btnPreklici.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        
        btnShrani.setOnClickListener(v -> {
            shraniNalepko();
        });

        posodobiGumbe();
    }

    private void shraniNalepko() {
        Bitmap bitmap = drawingView.getFinalBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "Napaka pri shranjevanju", Toast.LENGTH_SHORT).show();
            return;
        }

        String uniqueName = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "element_13_" + uniqueName + ".png";
        
        File directory = new File(getFilesDir(), "custom_elements");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Toast.makeText(this, "Nalepka shranjena", Toast.LENGTH_SHORT).show();
            posodobiGumbe();
            
            Intent intent = new Intent(UstvariNalepko.this, UstvariSliko.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Napaka pri shranjevanju datoteke", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Napaka pri nalaganju slike", Toast.LENGTH_SHORT).show();
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
    public void exportajNalepko() {
        Bitmap bitmap = drawingView.getFinalBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "Napaka pri generiranju nalepke", Toast.LENGTH_SHORT).show();
            return;
        }

        String imeDatoteke = "Artly_nalepka_" + System.currentTimeMillis() + ".png";

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

                Toast.makeText(this, "Nalepka uspešno izvožena v galerijo (Pictures/Artly)!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Napaka pri izvozu nalepke", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void posodobiGumbe() {
        if (elementAdapter != null) {
            previousArrow.setEnabled(elementAdapter.imaPrejsnjoStran());
            previousArrow.setAlpha(elementAdapter.imaPrejsnjoStran() ? 1.0f : 0.5f);

            nextArrow.setEnabled(elementAdapter.imaNaslednjoStran());
            nextArrow.setAlpha(elementAdapter.imaNaslednjoStran() ? 1.0f : 0.5f);
        }
    }

    private void setupColorClick(View popupView, int viewId, int color, PopupWindow popupWindow) {
        View colorView = popupView.findViewById(viewId);
        if (colorView != null) {
            colorView.setOnClickListener(v -> {
                drawingView.setPencilColor(color);
                popupWindow.dismiss();
            });
        }
    }
}
