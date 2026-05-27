package si.uni_lj.fe.tnuv.artly;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class UstvariNalepko extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This connects the Java code to your XML layout
        setContentView(R.layout.ustvari_nalepko);

        // Optional: Add back button functionality if you have btnBack in ustvari_nalepko.xml
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}