package si.uni_lj.fe.tnuv.artly;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ustvariSliko extends AppCompatActivity {

    private DrawingView drawingView;
    private RecyclerView elementRecyclerView;
    private ElementAdapter elementAdapter;
    private ImageButton previousArrow, nextArrow, addElementButton;
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
        addElementButton = findViewById(R.id.addElementButton);

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

        addElementButton.setOnClickListener(v -> {
            // Tukaj lahko kasneje dodaš funkcionalnost za dodajanje novih elementov
            Toast.makeText(this, "Dodajanje elementov še ni implementirano", Toast.LENGTH_SHORT).show();
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
