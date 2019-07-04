package com.paradoxo.amadeus.adapter;

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

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewConteudoPergunta, textViewConteudoResposta;
        ImageButton imageViewEditar, imageViewExcluir;

        ViewHolder(View view) {
            super(view);
            textViewConteudoPergunta = view.findViewById(R.id.conteudoPerguntaTextView);
            textViewConteudoResposta = view.findViewById(R.id.conteudoRespostaTextView);
            imageViewEditar = view.findViewById(R.id.editarTextView);
            imageViewExcluir = view.findViewById(R.id.excluirTextView);
        }
    }

    public AdapterEditaMensagem(List<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_editar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Mensagem mensagem = mensagens.get(position);

        if (holder instanceof ViewHolder) {
            final ViewHolder view = (ViewHolder) holder;

            view.textViewConteudoPergunta.setText(mensagem.getConteudo());
            view.textViewConteudoResposta.setText(mensagem.getConteudo_resposta());

            view.imageViewEditar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    onItemClickListenerEditar.onItemClick(view, position);
                }
            });

            view.imageViewExcluir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClickListenerExcluir.onItemClick(view, position);
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

    public void remover(int position) {
        mensagens.remove(position);
        notifyItemRemoved(position);
    }

    public void atualizar(Mensagem mensagem, int posicao) {
        mensagens.set(posicao, mensagem);
        notifyItemChanged(posicao);
    }

}
