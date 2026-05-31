package si.uni_lj.fe.tnuv.artly;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

import java.io.File;
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

        //klici
        slikaRecyclerView = findViewById(R.id.slikaRecyclerView);
        btnBack = findViewById(R.id.btnBack);
        txtAlbumPrazen = findViewById(R.id.txtAlbumPrazen);

        btnBack.setOnClickListener(v -> finish());

        slikaRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        osveziAlbum();
    }

    private void osveziAlbum() {
        File directory = new File(getFilesDir(), "album");
        File[] files = directory.listFiles();
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
        
        // Nastavimo ozadje dialoga na prozorno, da vidimo zaobljene robove in dimming
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Dimming effect
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.5f; // 50% darker
            dialog.getWindow().setAttributes(lp);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        ImageView popupSlika = dialog.findViewById(R.id.albumSlika);
        TextView popupNaslov = dialog.findViewById(R.id.naslovSlike);
        ImageButton btnUrediSliko = dialog.findViewById(R.id.btnUrediSliko);
        ImageButton btnIzbrisiSliko = dialog.findViewById(R.id.btnIzbrisiSliko);

        // Nastavi ime
        String fileName = file.getName();
        if (fileName.indexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        popupNaslov.setText(fileName);

        // Naloži sliko
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        popupSlika.setImageBitmap(bitmap);

        // Gumb Uredi Sliko
        btnUrediSliko.setOnClickListener(v -> {
            // Odpremo sliko v UstvariSliko (ki služi tudi kot urejevalnik)
            Intent intent = new Intent(Album.this, UstvariSliko.class);
            intent.putExtra("imagePath", file.getAbsolutePath());
            intent.putExtra("imageName", popupNaslov.getText().toString());
            startActivity(intent);
            dialog.dismiss();
        });

        // Gumb Izbriši Sliko
        btnIzbrisiSliko.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Izbriši sliko")
                    .setMessage("Ali ste prepričani, da želite trajno izbrisati to sliko?")
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        if (file.delete()) {
                            Toast.makeText(Album.this, "Slika izbrisana", Toast.LENGTH_SHORT).show();
                            osveziAlbum();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(Album.this, "Napaka pri brisanju slike", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Prekliči", null)
                    .show();
        });

        // Zapri ob kliku izven
        dialog.setCanceledOnTouchOutside(true);

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        osveziAlbum();
    }
}
