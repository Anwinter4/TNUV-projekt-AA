package si.uni_lj.fe.tnuv.artly;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UstvariSliko extends AppCompatActivity {

    private DrawingView drawingView;
    private RecyclerView elementRecyclerView;
    private ElementAdapter elementAdapter;
    private ImageButton previousArrow, nextArrow, addUstvariNalepko;
    private List<String> vsiElementi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ustvari_sliko);

        // Poveži poglede
        drawingView = findViewById(R.id.canvas);
        elementRecyclerView = findViewById(R.id.elementRecyclerView);
        previousArrow = findViewById(R.id.previousArrow);
        nextArrow = findViewById(R.id.nextArrow);
        addUstvariNalepko = findViewById(R.id.addUstvariNalepkoGrey);

    // Inside onCreate:
        vsiElementi = BranjeElementov.getElementDrawables();

    // Setup RecyclerView with a vertical layout
        elementRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    // Initialize the adapter with the list and a click listener
        elementAdapter = new ElementAdapter(vsiElementi, drawableId -> {
            drawingView.addSvgElement(drawableId);
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


        addUstvariNalepko.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UstvariSliko.this, UstvariNalepko.class);
                startActivity(intent);
            }
        });

        posodobiGumbe();
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
