package com.paradoxo.amadeus.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;

import java.util.List;

public class AdapterSinonimos extends RecyclerView.Adapter<AdapterSinonimos.ViewHolder> {
    List<String> itens;

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tituloTexView;
        private ImageView botaoDeletearButton;

        private ViewHolder(View view) {
            super(view);
            tituloTexView = view.findViewById(R.id.tituloTexView);
            botaoDeletearButton = view.findViewById(R.id.botaoDeletearButton);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_6, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final String sinonimo = itens.get(position);

        if (sinonimo != null) {
            holder.tituloTexView.setText(sinonimo);

            holder.tituloTexView.setOnClickListener(v -> onItemClickListener.onItemClick(v, sinonimo, position, false));

            holder.botaoDeletearButton.setOnClickListener(v -> onItemClickListener.onItemClick(v, sinonimo, position, true));
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    /* Mainuplução dos eventos de click simples e longo */
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, String sinonimo, int pos, boolean tipoDeletar);
    }


    /* Manipulação da lista de dados */
    public void add(String sinonimo) {
        itens.add(sinonimo);
        notifyItemInserted(itens.size());
    }

    public void remove(int position) {
        Log.e("TM itens", String.valueOf(itens.size()));

        itens.remove(position);
        notifyItemRemoved(position);

        Log.e("TM itens", String.valueOf(itens.size()));
    }

    public void altera(int position, String novo) {
        itens.set(position, novo);
        notifyItemChanged(position);
    }

    public void add(String sinonimo, int posi) {
        this.itens.add(posi, sinonimo);
        notifyItemChanged(posi, sinonimo);
    }

    public void addAll(List<String> sinonimo) {
        this.itens.addAll(sinonimo);
        notifyDataSetChanged();
    }

    public List<String> getItens() {
        return itens;
    }

    public AdapterSinonimos(List<String> sinonimo) {
        this.itens = sinonimo;
    }

}
