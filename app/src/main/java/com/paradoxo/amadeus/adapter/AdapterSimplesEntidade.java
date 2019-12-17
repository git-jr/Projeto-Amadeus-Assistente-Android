package com.paradoxo.amadeus.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Entidade;

import java.util.List;

public class AdapterSimplesEntidade extends RecyclerView.Adapter<AdapterSimplesEntidade.ViewHolder> {
    List<Entidade> itens;

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
        final Entidade entidade = itens.get(position);

        if (entidade != null) {
            holder.tituloTexView.setText(entidade.getNome());
            holder.conteudoTexView.setText(TextUtils.join(",", entidade.getSinonimos()));

            holder.layoutItemComum.setOnClickListener(v -> onItemClickListener.onItemClick(v, entidade, position));

            holder.layoutItemComum.setOnLongClickListener(v -> {
                onLongClickListener.onLongClickListener(v, position, entidade);
                return true;
            });
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
    private OnLongClickListener onLongClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, Entidade entidade, int pos);
    }

    public interface OnLongClickListener {
        void onLongClickListener(View view, int position, Entidade entidade);
    }


    /* Manipulação da lista de dados */
    public void add(Entidade entidade) {
        itens.add(entidade);
        notifyItemInserted(itens.size());
    }

    public void remove(int position) {
        itens.remove(position);
        notifyItemRemoved(position);
    }

    public void add(Entidade entidade, int posi) {
        this.itens.add(posi, entidade);
        notifyItemChanged(posi, entidade);
    }

    public void addAll(List<Entidade> itens) {
        this.itens.addAll(itens);
        notifyDataSetChanged();
    }

    public List<Entidade> getItens() {
        return itens;
    }

    public void trocarLista(List<Entidade> itens) {
        this.itens = itens;
        notifyDataSetChanged();
    }


    public AdapterSimplesEntidade(List<Entidade> itens) {
        this.itens = itens;
    }

}
