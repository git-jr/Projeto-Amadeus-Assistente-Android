package com.paradoxo.amadeus.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Mensagem;

import java.util.List;

public class AdapterEditaMensagem extends RecyclerView.Adapter {

    private List<Mensagem> mensagens;
    private OnItemClickListener onItemClickListenerEditar, onItemClickListenerExcluir;
    private OnLongClickListener onLongClickListener;


    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewConteudoPergunta, textViewConteudoResposta;
        final View layoutItemComum;

        ViewHolder(View view) {
            super(view);
            textViewConteudoPergunta = view.findViewById(R.id.conteudoPerguntaTextView);
            textViewConteudoResposta = view.findViewById(R.id.conteudoRespostaTextView);
            layoutItemComum = view.findViewById(R.id.layoutItemComum);

        }
    }

    public AdapterEditaMensagem(List<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_tipo_4, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Mensagem mensagem = mensagens.get(position);

        if (holder instanceof ViewHolder) {
            final ViewHolder view = (ViewHolder) holder;

            view.textViewConteudoPergunta.setText(mensagem.getConteudo());
            view.textViewConteudoResposta.setText(mensagem.getConteudo_resposta());

            view.layoutItemComum.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    onItemClickListenerEditar.onItemClick(view, position);
                }
            });

            view.layoutItemComum.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    onLongClickListener.onLongClickListener(view, position, mensagem);
                    return false;
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    public void setOnItemClickListenerEditar(AdapterEditaMensagem.OnItemClickListener onItemClickListenerEditar) {
        this.onItemClickListenerEditar = onItemClickListenerEditar;
    }

    public void setOnItemClickListenerExcluir(OnItemClickListener onItemClickListenerExcluir) {
        this.onItemClickListenerExcluir = onItemClickListenerExcluir;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int pos);
    }

    public interface OnLongClickListener {
        void onLongClickListener(View view, int position, Mensagem mensagem);
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void remover(int position) {
        mensagens.remove(position);
        notifyItemRemoved(position);
    }

    public void atualizar(Mensagem mensagem, int posicao) {
        mensagens.set(posicao, mensagem);
        notifyItemChanged(posicao);
    }

    public void delete(int position) {
        mensagens.remove(position);
        notifyItemRemoved(position);
    }

    public List<Mensagem> getMensagens() {
        return mensagens;
    }
}
