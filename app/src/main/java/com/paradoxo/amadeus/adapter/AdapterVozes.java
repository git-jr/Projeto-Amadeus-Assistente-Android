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
import com.paradoxo.amadeus.modelo.Voz;

import java.util.List;

public class AdapterVozes extends RecyclerView.Adapter {

    private Context context;
    private List<Voz> vozes;
    private final int VIEW_ITEM_TIPO_VOZ = 0;
    private OnItemClickListener onItemClickListener;
    private OnItemVerMaisClickListener onItemVerMaisClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View viewCaixaConteudo;
        final TextView textViewNome, textViewIdioma, textViewCodigo;

        ViewHolder(View view) {
            super(view);
            textViewCodigo = view.findViewById(R.id.codigoTextView);
            textViewIdioma = view.findViewById(R.id.idiomaTextView);
            textViewNome = view.findViewById(R.id.nomeVozTextView);
            viewCaixaConteudo = view.findViewById(R.id.caixaConteudoVoz);
        }
    }

    public void atualizar(int indiceBancoBaixado) {
        this.notifyItemChanged(indiceBancoBaixado);
    }

    public void add(Voz voz) {
        vozes.add(voz);
        notifyItemInserted(vozes.size());
    }

    public void deletar(Banco banco, int position) {
        vozes.remove(banco);
        notifyItemRemoved(position);
    }

    public void setOnItemVerMaisClickListener(OnItemVerMaisClickListener onItemVerMaisClickListener) {
        this.onItemVerMaisClickListener = onItemVerMaisClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public AdapterVozes(List<Voz> vozes, Context context) {
        this.vozes = vozes;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = null;
        View v;
        if (viewType == VIEW_ITEM_TIPO_VOZ) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tipo_voz, parent, false);
            return new ViewHolder(v);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Voz voz = vozes.get(position);

        if (holder instanceof ViewHolder) {
            final ViewHolder view = (ViewHolder) holder;

            try {
                view.textViewNome.setText(voz.getNome());
                view.textViewIdioma.setText(voz.getIdioma());
                view.textViewCodigo.setText(voz.getCodigo());

                view.viewCaixaConteudo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onItemClickListener.onItemClickListener(view, position, voz);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return vozes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_ITEM_TIPO_VOZ;
    }

    public interface OnItemVerMaisClickListener {
        void onItemMaisClickListener(View view, int position, Voz voz);
    }

    public interface OnItemClickListener {
        void onItemClickListener(View view, int position, Voz voz);
    }
}
