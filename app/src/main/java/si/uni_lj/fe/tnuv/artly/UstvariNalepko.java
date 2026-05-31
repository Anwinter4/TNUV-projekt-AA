package si.uni_lj.fe.tnuv.artly;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.UUID;

import top.defaults.colorpicker.ColorPickerPopup;

public class UstvariNalepko extends AppCompatActivity {

    private NalepkaView drawingView;
    private RecyclerView elementRecyclerView;
    private ElementAdapter elementAdapter;
    private ImageButton previousArrow, nextArrow;
    private ImageButton btnReverse, btnRedo;
    private ImageButton btnEraser, btnPencil, dodajSliko, btnTrash, btnBack;
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

        btnPencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new ColorPickerPopup.Builder(UstvariNalepko.this)
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

        btnPreklici.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UstvariNalepko.this, UstvariSliko.class);
                startActivity(intent);
                finish();
            }
        });
        
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

    private void showColorPickerPopup(View anchor) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.color_picker_popup, null);

        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Required to make it dismiss when clicking outside
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Set up color square clicks
        setupColorClick(popupView, R.id.colorBlack, Color.BLACK, popupWindow);
        setupColorClick(popupView, R.id.colorRed, Color.RED, popupWindow);
        setupColorClick(popupView, R.id.colorBlue, Color.BLUE, popupWindow);
        setupColorClick(popupView, R.id.colorGreen, Color.GREEN, popupWindow);
        setupColorClick(popupView, R.id.colorYellow, Color.YELLOW, popupWindow);
        setupColorClick(popupView, R.id.colorCyan, Color.CYAN, popupWindow);
        setupColorClick(popupView, R.id.colorMagenta, Color.MAGENTA, popupWindow);
        setupColorClick(popupView, R.id.colorOrange, Color.parseColor("#FFA500"), popupWindow);

        // Calculate location to show above the button
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();

        // Show the popup above the anchor button
        popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight() - popupHeight - 20);
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
