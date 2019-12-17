package com.paradoxo.amadeus.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Sentenca;

import java.util.List;

public class AdapterSimples extends RecyclerView.Adapter<AdapterSimples.ViewHolder> {
    List<Sentenca> itens;

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tituloTexView, conteudoTexView;
        private View layoutItemComum;

        private ViewHolder(View view) {
            super(view);
            tituloTexView = view.findViewById(R.id.tituloTexView);
            conteudoTexView = view.findViewById(R.id.conteudoTexView);
            layoutItemComum = view.findViewById(R.id.layoutItemComum);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_4, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Sentenca sentenca = itens.get(position);

        if (sentenca != null) {
            holder.tituloTexView.setText(sentenca.getChave());
            holder.conteudoTexView.setText(sentenca.getRespostas().get(0));

            holder.layoutItemComum.setOnClickListener(v -> onItemClickListener.onItemClick(v, sentenca, position));

            holder.layoutItemComum.setOnLongClickListener(v -> {
                onLongClickListener.onLongClickListener(v, position, sentenca);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    /* Mainuplução dos eventos de click simples e longo */
    private OnItemClickListener onItemClickListener;
    private OnLongClickListener onLongClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, Sentenca sentenca, int pos);
    }

    public interface OnLongClickListener {
        void onLongClickListener(View view, int position, Sentenca sentenca);
    }


    /* Manipulação da lista de dados */
    public void add(Sentenca sentenca) {
        itens.add(sentenca);
        notifyItemInserted(itens.size());
    }

    public void remove(int position) {
        itens.remove(position);
        notifyItemRemoved(position);
    }

    public void add(Sentenca sentenca, int posi) {
        this.itens.add(posi, sentenca);
        notifyItemChanged(posi, sentenca);
    }

    public void addAll(List<Sentenca> itens) {
        this.itens.addAll(itens);
        notifyDataSetChanged();
    }

    public List<Sentenca> getItens() {
        return itens;
    }

    public void trocarLista(List<Sentenca> itens) {
        this.itens = itens;
        notifyDataSetChanged();
    }

    public AdapterSimples(List<Sentenca> itens) {
        this.itens = itens;
    }

}
