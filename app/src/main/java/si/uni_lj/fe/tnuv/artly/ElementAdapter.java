package si.uni_lj.fe.tnuv.artly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class ElementAdapter extends RecyclerView.Adapter<ElementAdapter.ViewHolder> {

    private List<String> elementi;
    private OnElementClickListener listener;
    private int trenutnaStran = 0;
    private int elementiNaStran = 7;

    public interface OnElementClickListener {
        void onElementClick(String identifier);
    }

    public ElementAdapter(List<String> elementi, OnElementClickListener listener) {
        this.elementi = elementi;
        this.listener = listener;
    }

    public void setElementi(List<String> noviElementi) {
        this.elementi = noviElementi;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_element, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int realPosition = trenutnaStran * elementiNaStran + position;

        if (realPosition < elementi.size()) {
            String elementIdentifier = elementi.get(realPosition);
            Context context = holder.itemView.getContext();

            if (elementIdentifier.startsWith("/")) {
                Bitmap bitmap = BitmapFactory.decodeFile(elementIdentifier);
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                }
            } else {
                int drawableId = context.getResources().getIdentifier(elementIdentifier, "drawable", context.getPackageName());
                if (drawableId != 0) {
                    holder.imageView.setImageResource(drawableId);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onElementClick(elementIdentifier);
                }
            });

            holder.itemView.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (elementi == null) return 0;
        int totalElements = elementi.size();
        int remainingElements = totalElements - (trenutnaStran * elementiNaStran);
        return Math.min(elementiNaStran, Math.max(0, remainingElements));
    }

    public void naPrejsnjoStran() {
        if (trenutnaStran > 0) {
            trenutnaStran--;
            notifyDataSetChanged();
        }
    }

    public void naNaslednjoStran() {
        if ((trenutnaStran + 1) * elementiNaStran < elementi.size()) {
            trenutnaStran++;
            notifyDataSetChanged();
        }
    }

    public boolean imaPrejsnjoStran() {
        return trenutnaStran > 0;
    }

    public boolean imaNaslednjoStran() {
        return elementi != null && (trenutnaStran + 1) * elementiNaStran < elementi.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.elementImage);
        }
    }
}
