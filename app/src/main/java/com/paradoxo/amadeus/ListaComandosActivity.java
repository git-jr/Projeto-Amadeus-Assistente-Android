package com.paradoxo.amadeus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ListaComandosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_comandos);
        configurarToolBarBranca(this);
    }
}
