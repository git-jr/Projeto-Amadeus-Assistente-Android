package com.paradoxo.amadeus.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.util.SpeechToText;
import com.paradoxo.amadeus.util.SpeechToTextSegundoPlano;

public class DialogSegundoPlanoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_segundo_plano);

        final SpeechToTextSegundoPlano speechToText = new SpeechToTextSegundoPlano(this,"tomate");

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speechToText.backgroundVoiceListener.run();
            }
        });

    }
}
