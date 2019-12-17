package com.paradoxo.amadeus.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.paradoxo.amadeus.R;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class SobreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sobre);

        configurarInterface();

    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        carregarTextoVersao();
        configurarClickBotoes();
        configurarClikLicencas();
    }

    private void configurarClickBotoes() {
        configurarClickLink(R.id.irParaYouTubeLayout, getString(R.string.link_playlist_amadeus_yt));
        configurarClickLink(R.id.enviarFeedBackLayout, getString(R.string.linkAmadeusGooglePlayStore));
        configurarClickLink(R.id.gitHubLayout, getString(R.string.linkAmadeusGitHub));
        configurarClickLink(R.id.creditosLottieApi1TextView,getString(R.string.link_lottie_api_1));
        configurarClickLink(R.id.creditosLottieApi2TextView, getString(R.string.link_lottie_api_2));
    }

    private void carregarTextoVersao() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versao = getString(R.string.versao) + " " + pInfo.versionName;

            TextView textView = findViewById(R.id.versaoTextView);
            textView.setText(versao);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void configurarClickLink(int viewSelecionada, String link) {
        (findViewById(viewSelecionada)).setOnClickListener(v -> {
            Intent intentPlayStore = new Intent(Intent.ACTION_VIEW);
            intentPlayStore.setData(Uri.parse(link));
            startActivity(intentPlayStore);

        });
    }

    private void configurarClikLicencas() {
        findViewById(R.id.licencaCodigoLayout).setOnClickListener(v -> startActivity(new Intent(SobreActivity.this, OssLicensesMenuActivity.class)));
    }
}
