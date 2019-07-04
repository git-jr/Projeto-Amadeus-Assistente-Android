package com.paradoxo.amadeus.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Mensagem;

import java.util.Collections;
import java.util.List;

public class AdapterMensagensBusca extends RecyclerView.Adapter<AdapterMensagensBusca.ViewHolder> {

    private List<Mensagem> mensagens;
    private OnItemClickListener onItemClickListener;

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewConteudoPergunta;
        private View viewCaixaConteudo;

        private ViewHolder(View view) {
            super(view);
            textViewConteudoPergunta = view.findViewById(R.id.conteudoTextView);
            viewCaixaConteudo = view.findViewById(R.id.ll_caixa_conteudo);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_consultar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Mensagem mensagem = mensagens.get(position);

        if (holder != null) {
            final String conteudoMensagem = mensagem.getConteudo();
            holder.textViewConteudoPergunta.setText(conteudoMensagem);
            holder.viewCaixaConteudo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    onItemClickListener.onItemClick(v, conteudoMensagem, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    public void setMensagens(List<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public AdapterMensagensBusca(List<Mensagem> mensagens) {
        this.mensagens = mensagens;
        Collections.reverse(this.mensagens); // Para inveter a exibição
    }

    public interface OnItemClickListener {
        void onItemClick(View view, String viewModel, int pos);
    }

    public void atualiza_recycler() {
        Collections.reverse(this.mensagens);
        notifyDataSetChanged();
    }
}
