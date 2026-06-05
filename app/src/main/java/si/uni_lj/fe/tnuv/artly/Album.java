package si.uni_lj.fe.tnuv.artly;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Album extends AppCompatActivity {

    private RecyclerView slikaRecyclerView;
    private AlbumAdapter albumAdapter;
    private ImageButton btnBack;
    private TextView txtAlbumPrazen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.albumLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        slikaRecyclerView = findViewById(R.id.slikaRecyclerView);
        btnBack = findViewById(R.id.btnBack);
        txtAlbumPrazen = findViewById(R.id.txtAlbumPrazen);

        btnBack.setOnClickListener(v -> finish());
        slikaRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        osveziAlbum();
    }

    private void osveziAlbum() {
        File directory = new File(getFilesDir(), "album");
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        List<File> listSlik = new ArrayList<>();

        if (files != null && files.length > 0) {
            txtAlbumPrazen.setVisibility(View.GONE);
            slikaRecyclerView.setVisibility(View.VISIBLE);
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            listSlik.addAll(Arrays.asList(files));
        } else {
            txtAlbumPrazen.setVisibility(View.VISIBLE);
            slikaRecyclerView.setVisibility(View.GONE);
        }

        albumAdapter = new AlbumAdapter(listSlik, file -> prikaziPopup(file));
        slikaRecyclerView.setAdapter(albumAdapter);
    }

    private void prikaziPopup(File file) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.album_popup);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.5f;
            dialog.getWindow().setAttributes(lp);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        ImageView popupSlika = dialog.findViewById(R.id.albumSlika);
        TextView popupNaslov = dialog.findViewById(R.id.naslovSlike);
        ImageButton btnUrediSliko = dialog.findViewById(R.id.btnUrediSliko);
        ImageButton btnIzbrisiSliko = dialog.findViewById(R.id.btnIzbrisiSliko);
        ImageButton btnExport = dialog.findViewById(R.id.btnIzvoziSliko);

        String fileName = file.getName();
        if (fileName.indexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        popupNaslov.setText(fileName);

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        popupSlika.setImageBitmap(bitmap);

        btnUrediSliko.setOnClickListener(v -> {
            Intent intent = new Intent(Album.this, UstvariSliko.class);
            intent.putExtra("imagePath", file.getAbsolutePath());
            intent.putExtra("imageName", popupNaslov.getText().toString());
            startActivity(intent);
            dialog.dismiss();
        });

        btnIzbrisiSliko.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.izbrisi_sliko)
                    .setMessage(R.string.preveri_pred_izbrisom_slike)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        // Izbriši PNG
                        boolean deletedPng = file.delete();
                        
                        // Poskusi izbrisati še pripadajoči JSON
                        String statePath = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".json";
                        File stateFile = new File(statePath);
                        if (stateFile.exists()) {
                            stateFile.delete();
                        }

                        if (deletedPng) {
                            Toast.makeText(Album.this, R.string.slika_izbrisana, Toast.LENGTH_SHORT).show();
                            osveziAlbum();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(Album.this, R.string.napaka_brisanje_slike, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.preklici, null)
                    .show();
        });

        btnExport.setOnClickListener(v -> exportajSliko(bitmap, popupNaslov.getText().toString()));
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void exportajSliko(Bitmap bitmap, String imeSlike) {
        if (bitmap == null) {
            Toast.makeText(this, R.string.napaka_nalaganje_slike, Toast.LENGTH_SHORT).show();
            return;
        }

        String imeDatoteke = (imeSlike == null || imeSlike.isEmpty()) ? getString(R.string.artly_izvoz) : imeSlike;
        imeDatoteke += "_" + System.currentTimeMillis() + ".png";

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
                Toast.makeText(this, R.string.uspesen_izvoz_slike, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.neuspesen_izvoz_slike, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        osveziAlbum();
    }
}
