package com.paradoxo.amadeus.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Banco;

import java.util.List;

public class AdapterBancosOnline extends RecyclerView.Adapter {

    private Context context;
    private List<Banco> bancos;
    private final int VIEW_ITEM_BANCO = 0;
    private OnItemClickListener onItemClickListener;
    private OnItemVerMaisClickListener onItemVerMaisClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View viewCaixaConteudo;
        final ImageView imagemViewStatus;
        final ImageButton imagemButtonVerMais;
        final TextView textViewNomeBanco, textViewDataBanco, textViewAutorBanco, textViewTamanhoBanco;

        ViewHolder(View view) {
            super(view);
            imagemButtonVerMais = view.findViewById(R.id.maisImageButton);
            textViewNomeBanco = view.findViewById(R.id.nomeBancoTextView);
            textViewDataBanco = view.findViewById(R.id.dataBancoTextView);
            textViewAutorBanco = view.findViewById(R.id.autorBancoTextView);
            imagemViewStatus = view.findViewById(R.id.imgemStatus);
            textViewTamanhoBanco = view.findViewById(R.id.tamanhoTextView);
            viewCaixaConteudo = view.findViewById(R.id.caixaConteudo);
        }
    }

    public void atualizar(int indiceBancoBaixado) {
        this.notifyItemChanged(indiceBancoBaixado);
    }

    public void add(Banco banco) {
        bancos.add(banco);
        notifyItemInserted(bancos.size());
    }

    public void deletar(Banco banco, int position) {
        bancos.remove(banco);
        notifyItemRemoved(position);
    }

    public void setOnItemVerMaisClickListener(OnItemVerMaisClickListener onItemVerMaisClickListener) {
        this.onItemVerMaisClickListener = onItemVerMaisClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public AdapterBancosOnline(List<Banco> bancos, Context context) {
        this.bancos = bancos;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = null;
        View v;
        if (viewType == VIEW_ITEM_BANCO) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banco, parent, false);
            return new ViewHolder(v);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Banco banco = bancos.get(position);

        if (holder instanceof ViewHolder) {
            final ViewHolder view = (ViewHolder) holder;
            view.textViewNomeBanco.setText(banco.getNome());

            if (banco.getIdAutor() != null) {
                view.textViewAutorBanco.setText(banco.getIdAutor());
                view.textViewAutorBanco.setVisibility(View.VISIBLE);
            } else {
                view.textViewAutorBanco.setVisibility(View.GONE);
            }

            view.textViewTamanhoBanco.setText(banco.getTamanho());
            view.textViewDataBanco.setText(banco.getDtAtualizadoExibicao());

            if (banco.getBaixado()) {
                view.imagemViewStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_baixado));
            } else {
                view.imagemViewStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_baixar));
            }

            view.imagemButtonVerMais.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemVerMaisClickListener.onItemMaisClickListener(view, position, banco);
                }
            });

            view.viewCaixaConteudo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClickListener.onItemClickListener(view, position, banco);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return bancos.size();
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_ITEM_BANCO;
    }

    public interface OnItemVerMaisClickListener {
        void onItemMaisClickListener(View view, int position, Banco banco);
    }

    public interface OnItemClickListener {
        void onItemClickListener(View view, int position, Banco banco);
    }
}
