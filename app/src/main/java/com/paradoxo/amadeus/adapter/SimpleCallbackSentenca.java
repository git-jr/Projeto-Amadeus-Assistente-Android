package com.paradoxo.amadeus.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.SentencaDAO;

import static com.paradoxo.amadeus.util.Toasts.meuToast;

public class SimpleCallbackSentenca extends ItemTouchHelper.SimpleCallback {

    Drawable icon;
    Context context;
    ColorDrawable background;
    AdapterSimples adapterSentenca;

    public SimpleCallbackSentenca(AdapterSimples adapter, Context context) {
        super(0, ItemTouchHelper.RIGHT);
        adapterSentenca = adapter;
        this.context = context;
        icon = ContextCompat.getDrawable(context,
                R.drawable.ic_delete);
        assert icon != null;
        icon.setColorFilter(new LightingColorFilter(Color.WHITE, Color.WHITE));
        background = new ColorDrawable(Color.RED);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        int deslocamentoBorda = 20;

        int margemIcone = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconePosiTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconePosiBottom = iconePosiTop + icon.getIntrinsicHeight();

        if (dX > 0) { // Deslizar para direita
            int iconePoisEsquerda = itemView.getLeft() + margemIcone;
            int iconePosiDireita = iconePoisEsquerda + icon.getIntrinsicWidth();
            icon.setBounds(iconePoisEsquerda, iconePosiTop, iconePosiDireita, iconePosiBottom);

            background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX) + deslocamentoBorda, itemView.getBottom());
        } else if (dX < 0) { // Deslizar para esquerda
            int iconLeft = itemView.getRight() - margemIcone - icon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - margemIcone;
            icon.setBounds(iconLeft, iconePosiTop, iconRight, iconePosiBottom);

            background.setBounds(itemView.getRight() + ((int) dX) - deslocamentoBorda,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else { // Caso a view não tenha sido deslizada
            background.setBounds(0, 0, 0, 0);
        }

        background.draw(c);
        icon.draw(c);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        excluirMensagem(position);
    }

    private void excluirMensagem(final int pos) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.confirmar_exclusao));
        builder.setMessage(context.getString(R.string.cornfimar_exclusao_item));
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {

            try {

                SentencaDAO sentencaDAO = new SentencaDAO(context, false);
                sentencaDAO.excluir(adapterSentenca.getItens().get(pos));
                adapterSentenca.remove(pos);

                meuToast(context.getString(R.string.excluido_com_sucesso), context);

                if (adapterSentenca.getItens().size() == 0) {
                    meuToast(context.getString(R.string.sem_sentencas_no_momento), context);
                }

            } catch (Exception e) {
                meuToast(context.getString(R.string.erro_excluir_sentenca), context);
                e.printStackTrace();
            }
        });
        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> adapterSentenca.notifyItemChanged(pos));

        builder.show();
    }
}
