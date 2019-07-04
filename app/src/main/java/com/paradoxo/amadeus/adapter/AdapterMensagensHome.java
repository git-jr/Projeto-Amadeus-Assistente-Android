package com.paradoxo.amadeus.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.modelo.Mensagem;

import java.util.List;

public class AdapterMensagensHome extends RecyclerView.Adapter {

    private List<Mensagem> mensagens;
    private final int VIEW_ITEM_MSG_IA = 0;
    private final int VIEW_ITEM_MSG_USU = 1;
    private final int VIEW_PROGRESSO = 2;
    private OnLongClickListener onLongClickListener;
    private OnItemClickListener onItemClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder {

        final TextView textViewConteudoMensagem;
        final View viewCaixaMensagem;

        ViewHolder(View view) {
            super(view);
            textViewConteudoMensagem = view.findViewById(R.id.conteudoTextView);
            viewCaixaMensagem = view.findViewById(R.id.caixaMensagem);
        }

    }

    public static class ProgessoViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout progress_bar;

        ProgessoViewHolder(View v) {
            super(v);
            progress_bar = v.findViewById(R.id.progress_bar);
        }
    }

    public AdapterMensagensHome(List<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = null;
        View v;
        switch (viewType) {
            case (VIEW_ITEM_MSG_IA):
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_ia, parent, false);
                return new ViewHolder(v);
            case (VIEW_ITEM_MSG_USU):
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_usu, parent, false);
                return new ViewHolder(v);
            case (VIEW_PROGRESSO):
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_progresso, parent, false);
                return new AdapterMensagensHome.ProgessoViewHolder(v);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Mensagem mensagem = mensagens.get(position);

        if (holder instanceof ViewHolder) {
            final ViewHolder view = (ViewHolder) holder;
            view.textViewConteudoMensagem.setText(mensagem.getConteudo());
            view.viewCaixaMensagem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!mensagem.iAEhAutor()) {
                        onLongClickListener.onLongClickListener(v, position, mensagem);
                    }
                    return false;
                }
            });

            view.viewCaixaMensagem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mensagem.iAEhAutor() && mensagem.getAcao() == AcaoEnum.ACAO_APP_NAO_ENCONTRADO)
                        onItemClickListener.onItemClickListener(v, position, mensagem);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    @Override
    public int getItemViewType(int position) {
        int tipoView;

        if (this.mensagens.get(position).getProgresso()) {
            tipoView = VIEW_PROGRESSO;
        } else if (this.mensagens.get(position).iAEhAutor()) {
            tipoView = VIEW_ITEM_MSG_IA;
        } else {
            tipoView = VIEW_ITEM_MSG_USU;
        }
        return tipoView;
    }

    public interface OnLongClickListener {

        void onLongClickListener(View view, int position, Mensagem mensagem);
    }

    public interface OnItemClickListener {

        void onItemClickListener(View view, int position, Mensagem mensagem);
    }

    public void add(Mensagem objMensagem) {
        mensagens.add(objMensagem);
        notifyItemInserted(mensagens.size());
    }

    public void remove() {
        mensagens.remove(mensagens.size() - 1);
        notifyItemRemoved(mensagens.size());
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
