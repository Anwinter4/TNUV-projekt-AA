package si.uni_lj.fe.tnuv.artly;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import java.util.List;

import top.defaults.colorpicker.ColorPickerPopup;

public class UstvariSliko extends AppCompatActivity {

    private DrawingView drawingView;
    private RecyclerView elementRecyclerView;
    private ElementAdapter elementAdapter;
    private ImageButton previousArrow, nextArrow, addUstvariNalepko, dodajSliko, btnPencil, btnEraser, btnReverse, btnRedo, btnTrash, btnBack;
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

        btnShrani = findViewById(R.id.btnShrani);
        vnosnoPolje = findViewById(R.id.vnosno_polje);
        mColorPreview = findViewById(R.id.preview_selected_color);


        vsiElementi = BranjeElementov.getElementDrawables(this);
        elementRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        elementAdapter = new ElementAdapter(vsiElementi, drawableId -> {
            drawingView.addElement(drawableId);
        });

        elementRecyclerView.setAdapter(elementAdapter);

        // Preveri če urejamo obstoječo sliko
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("imagePath")) {
            String path = intent.getStringExtra("imagePath");
            String name = intent.getStringExtra("imageName");
            if (path != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                if (bitmap != null) {
                    drawingView.post(() -> drawingView.setBackgroundBitmap(bitmap));
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

        // Gumb za svinčnik - vklopi risanje
        btnPencil.setOnClickListener(v -> {
            drawingView.setDrawingEnabled(true);
            Toast.makeText(this, "Pero vklopljeno", Toast.LENGTH_SHORT).show();
        });

        // Gumb za radirko - postopno brisanje (gradual eraser)
        btnEraser.setOnClickListener(v -> {
            drawingView.setEraserMode(true);
            Toast.makeText(this, "Radirka vklopljena", Toast.LENGTH_SHORT).show();
        });

        btnPencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new ColorPickerPopup.Builder(UstvariSliko.this)
                        .initialColor(mDefaultColor)
                        .enableBrightness(true)
                        .enableAlpha(true)
                        .okTitle("V redu")
                        .cancelTitle("Prekliči")
                        .showIndicator(true)
                        .showValue(true)
                        .build()
                        .show(v, new ColorPickerPopup.ColorPickerObserver() {
                            @Override
                            public void onColorPicked(int color) {
                                mDefaultColor = color;
                                mColorPreview.setBackgroundColor(mDefaultColor);
                                drawingView.setPencilColor(mDefaultColor);
                            }
                        });
            }
        });

        // Gumb za koš (Trash) - pobriše vse (risbo in nalepke)
        btnTrash.setOnClickListener(v -> {
            drawingView.clearAll();
            Toast.makeText(this, "Platno očiščeno", Toast.LENGTH_SHORT).show();
        });

        // Gumb za nazaj (Undo)
        btnReverse.setOnClickListener(v -> {
            drawingView.undo();
        });

        // Gumb za naprej (Redo)
        btnRedo.setOnClickListener(v -> {
            drawingView.redo();
        });

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnShrani.setOnClickListener(v -> {
            if(!vnosnoPolje.getText().toString().isEmpty()) {
                // Shrani
                shraniSliko(vnosnoPolje.getText().toString());
            } else {
                // EditText is empty - show error or handle it
                Toast.makeText(this, "Vnosno polje ne sme biti prazno!", Toast.LENGTH_SHORT).show();
            }


        });

        posodobiGumbe();
    }

    private void shraniSliko(String imeSlike) {
        Bitmap bitmap = drawingView.getFinalBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "Napaka pri generiranju slike", Toast.LENGTH_SHORT).show();
            return;
        }

        // Shranimo v mapo 'album' znotraj internih datotek aplikacije
        File directory = new File(getFilesDir(), "album");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, imeSlike + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Toast.makeText(this, "Slika '" + imeSlike + "' shranjena!", Toast.LENGTH_SHORT).show();

            // Po shranjevanju počistimo polje in platno (opcijsko)
            vnosnoPolje.setText("");
            drawingView.clearAll();
            // Vrni se nazaj v Album
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Napaka pri shranjevanju datoteke", Toast.LENGTH_SHORT).show();
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

    private void setupColorClick(View popupView, int viewId, int color, PopupWindow popupWindow) {
        View colorView = popupView.findViewById(viewId);
        if (colorView != null) {
            colorView.setOnClickListener(v -> {
                drawingView.setPencilColor(color);
                popupWindow.dismiss();
            });
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
}
