package com.paradoxo.amadeus.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.paradoxo.amadeus.R;

public class SobreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sobre);

        final String linkPlaylistAmadeusYouTube = getString(R.string.link_playlist_amadeus_yt);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versao = getString(R.string.versao) + " " + pInfo.versionName;

            TextView textView = findViewById(R.id.versaoTextView);
            textView.setText(versao);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        (findViewById(R.id.irParaYouTubeButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent activityAmadeusYouTube = new Intent(Intent.ACTION_VIEW);
                activityAmadeusYouTube.setData(Uri.parse(linkPlaylistAmadeusYouTube));
                startActivity(activityAmadeusYouTube);

            }
        });

        (findViewById(R.id.irParaYouTubeButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent activityAmadeusYouTube = new Intent(Intent.ACTION_VIEW);
                activityAmadeusYouTube.setData(Uri.parse(linkPlaylistAmadeusYouTube));
                startActivity(activityAmadeusYouTube);

            }
        });

        findViewById(R.id.licencaCodigoTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SobreActivity.this, OssLicensesMenuActivity.class));

            }
        });

        findViewById(R.id.lincencaAnimacoesTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentActivtyAnimcaoes = new Intent(SobreActivity.this, LicencaAnimacoesActivity.class);
                startActivity(intentActivtyAnimcaoes);
            }
        });
    }

}
