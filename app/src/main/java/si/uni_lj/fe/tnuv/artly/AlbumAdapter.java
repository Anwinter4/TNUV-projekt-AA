package si.uni_lj.fe.tnuv.artly;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    private List<File> slikeFiles;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public AlbumAdapter(List<File> slikeFiles, OnItemClickListener listener) {
        this.slikeFiles = slikeFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_slika, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = slikeFiles.get(position);

        String fileName = file.getName();
        if (fileName.indexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        holder.naslovSlike.setText(fileName);

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        holder.albumSlika.setImageBitmap(bitmap);

        holder.albumCelaSlika.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return slikeFiles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumSlika;
        TextView naslovSlike;
        LinearLayout albumCelaSlika;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumSlika = itemView.findViewById(R.id.albumSlika);
            naslovSlike = itemView.findViewById(R.id.naslovSlike);
            albumCelaSlika = itemView.findViewById(R.id.albumCelaSlika);
        }
    }
}
