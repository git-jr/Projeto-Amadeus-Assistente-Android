package com.paradoxo.amadeus.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Voz;

import java.util.List;

public class AdapterVozes extends RecyclerView.Adapter<AdapterVozes.ViewHolder> {

    List<Voz> vozes;
    OnItemClickListener onItemClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View viewCaixaConteudo;
        final TextView textViewNome, textViewIdioma, textViewCodigo;

        ViewHolder(View view) {
            super(view);
            textViewCodigo = view.findViewById(R.id.codigoTextView);
            textViewIdioma = view.findViewById(R.id.idiomaTextView);
            textViewNome = view.findViewById(R.id.nomeVozTextView);
            viewCaixaConteudo = view.findViewById(R.id.layoutItemComum);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public AdapterVozes(List<Voz> vozes) {
        this.vozes = vozes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tipo_voz, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Voz voz = vozes.get(position);

        if (voz != null) {

            holder.textViewNome.setText(voz.getNome());
            holder.textViewIdioma.setText(voz.getIdioma());
            holder.textViewCodigo.setText(voz.getCodigo());

            holder.viewCaixaConteudo.setOnClickListener(view -> onItemClickListener.onItemClickListener(view, position, voz));

        }
    }

    @Override
    public int getItemCount() {
        return vozes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public interface OnItemClickListener {
        void onItemClickListener(View view, int position, Voz voz);
    }

    public void add(Voz voz) {
        vozes.add(voz);
        notifyItemInserted(vozes.size());
    }
}
