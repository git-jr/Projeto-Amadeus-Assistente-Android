package com.paradoxo.amadeus.activity.redesign;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterEditaMensagem;
import com.paradoxo.amadeus.dao.MensagemDAO;

import java.util.Objects;

import static com.paradoxo.amadeus.util.Toasts.meuToast;

public class SimpleCallback extends ItemTouchHelper.SimpleCallback {

    private AdapterEditaMensagem mAdapter;
    ColorDrawable background;
    Drawable icon;
    Context context;

    public SimpleCallback(AdapterEditaMensagem adapter, Context context) {
        super(0, ItemTouchHelper.RIGHT);
        mAdapter = adapter;
        this.context = context;
        icon = ContextCompat.getDrawable(context,
                R.drawable.ic_deletar);
        assert icon != null;
        icon.setColorFilter(new LightingColorFilter(Color.WHITE, Color.WHITE));
        background = new ColorDrawable(Color.RED);


    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX,
                dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 20;


        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        if (dX > 0) { // Swiping to the right
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            background.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
        } else if (dX < 0) { // Swiping to the left
            int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else { // view is unSwiped
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
        /*   mAdapter.delete(position);*/
        excluirMensagem(position);
    }

    private void excluirMensagem(final int pos) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.confirmar_excluir));
        builder.setMessage(context.getString(R.string.cornfimar_exclusao_item));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


                try {
                    MensagemDAO mensagemDAO = new MensagemDAO(context);
                    mensagemDAO.excluirResposta(mAdapter.getMensagens().get(pos));

                    mAdapter.notifyItemRemoved(pos);

                    meuToast(String.valueOf(context.getText(R.string.msg_deletada_sucesso)),context);

                    if (mAdapter.getMensagens().size() == 0) {
                        meuToast(String.valueOf(context.getText(R.string.nenhuma_resposta_gravada)),context);

                    }

                } catch (Exception e) {
                    meuToast(String.valueOf(context.getText(R.string.erro_apagar_msg)), context);
                }
            }

        });
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAdapter.notifyItemChanged(pos);
            }
        });

        builder.show();
    }
}
