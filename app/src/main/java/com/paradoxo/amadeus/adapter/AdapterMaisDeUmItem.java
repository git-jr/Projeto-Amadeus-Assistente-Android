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

public class AdapterMaisDeUmItem extends RecyclerView.Adapter<AdapterMaisDeUmItem.ViewHolder> {
    List<Sentenca> itens;

    final int VIEW_ITEM_2 = 2;
    final int VIEW_ITEM_3 = 3;
    final int VIEW_ITEM_5 = 5;

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView conteudoTexView, tituloTexView;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_1, parent, false);
        switch (viewType) {
            case (VIEW_ITEM_2): {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_2, parent, false);
                break;
            }
            case (VIEW_ITEM_3): {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_3, parent, false);
                break;
            }

            case (VIEW_ITEM_5): {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_5, parent, false);
                break;
            }
        }

        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        int tipoView = 1;

        switch (itens.get(position).getTipo_item()) {
            case 2: {
                tipoView = VIEW_ITEM_2;
                break;
            }

            case 3: {
                tipoView = VIEW_ITEM_3;
                break;
            }

            case 5: {
                tipoView = VIEW_ITEM_5;
                break;
            }
        }

        return tipoView;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Sentenca sentenca = itens.get(position);
        int itemViewType = getItemViewType(position);

        if (itemViewType == VIEW_ITEM_5) {
            return;
        }

        if (sentenca.getTipo_item() >= 0) {
            holder.conteudoTexView.setText(sentenca.getRespostas().get(0));

            if (itemViewType == VIEW_ITEM_3 && sentenca.getRespostas() != null) {
                holder.tituloTexView.setText(sentenca.getRespostas().get(1));
                holder.conteudoTexView.setText(sentenca.getRespostas().get(0));
            }

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
        void onLongClickListener(View view, int position, Sentenca mensagem);
    }


    /* Manipulação da lista de dados */
    public void add(Sentenca sentenca) {
        itens.add(sentenca);
        notifyItemInserted(getItemCount());
        // notifyItemInserted(itens.size());
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

    public AdapterMaisDeUmItem(List<Sentenca> itens) {
        this.itens = itens;
    }

}
