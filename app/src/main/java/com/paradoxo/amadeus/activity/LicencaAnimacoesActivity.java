/*
 * Created by Junior Obom on 29/06/19 17:12
 * Copyright (c) 2019 . All rights reserved.
 * Last modified 29/06/19 17:12
 */

package com.paradoxo.amadeus.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.paradoxo.amadeus.R;

public class LicencaAnimacoesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenca_animacoes);
        configurarCliqueLicenca();
    }

    private void configurarCliqueLicenca() {
        findViewById(R.id.caixaConteudo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent abrirUrl = new Intent(Intent.ACTION_VIEW);
                abrirUrl.setData(Uri.parse("https://lottiefiles.com/1808-scaling-loader#"));
                startActivity(abrirUrl);
            }
        });

        // Por hora basta isso, mas migrar para recyclerView se mais animcações forem utilizadas
    }
}
