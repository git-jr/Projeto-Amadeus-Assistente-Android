package com.paradoxo.amadeus.fragments;
/* Código original: Nelson Glauber
 https://medium.com/@nglauber/adeus-alertdialog-bem-vindo-dialogfragment-dfaa887b575d*/

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;

public class DialogSimples extends DialogFragment implements DialogInterface.OnClickListener {
    int posiItem;

    static final String EXTRA_POSI = "posi";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_BUTTONS = "buttons";
    static final String DIALOG_TAG = "DialogSimples";

    public static DialogSimples newDialog(String title, String message, int posi, int[] buttonTexts) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_POSI, posi);
        bundle.putString(EXTRA_TITLE, title);
        bundle.putString(EXTRA_MESSAGE, message);
        bundle.putIntArray(EXTRA_BUTTONS, buttonTexts);

        DialogSimples dialog = new DialogSimples();
        dialog.setArguments(bundle);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String titulo = Objects.requireNonNull(getArguments()).getString(EXTRA_TITLE);
        String mensagem = getArguments().getString(EXTRA_MESSAGE);
        int[] botoes = getArguments().getIntArray(EXTRA_BUTTONS);
        posiItem = getArguments().getInt(EXTRA_POSI);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        alertDialogBuilder.setTitle(titulo);
        alertDialogBuilder.setMessage(mensagem);

        switch (Objects.requireNonNull(botoes).length) {
            case 3:
                alertDialogBuilder.setNeutralButton(botoes[2], this);

            case 2:
                alertDialogBuilder.setNegativeButton(botoes[1], this);

            case 1:
                alertDialogBuilder.setPositiveButton(botoes[0], this);
        }

        return alertDialogBuilder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ((FragmentDialogInterface) Objects.requireNonNull(getActivity())).onClick(posiItem, which);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void openDialog(FragmentManager supportFragmentManager) {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG) == null) {
            show(supportFragmentManager, DIALOG_TAG);
        }
    }

    public interface FragmentDialogInterface {
        void onClick(int posi, int which);
    }
}
