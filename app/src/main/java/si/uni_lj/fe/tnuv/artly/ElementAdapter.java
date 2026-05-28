package si.uni_lj.fe.tnuv.artly;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ElementAdapter extends RecyclerView.Adapter<ElementAdapter.ViewHolder> {

    private List<String> elementi;
    private OnElementClickListener listener;
    private int trenutnaStran = 0;
    private int elementiNaStran = 12; // Povečano na 12, da zapolni ves razpoložljiv prostor na večini telefonov

    public interface OnElementClickListener {
        void onElementClick(int drawableResId);
    }

    public ElementAdapter(List<String> elementi, OnElementClickListener listener) {
        this.elementi = elementi;
        this.listener = listener;
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
            String drawableName = elementi.get(realPosition);
            Context context = holder.itemView.getContext();
            int drawableId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());

            if (drawableId != 0) {
                holder.imageView.setImageResource(drawableId);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null && drawableId != 0) {
                    listener.onElementClick(drawableId);
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