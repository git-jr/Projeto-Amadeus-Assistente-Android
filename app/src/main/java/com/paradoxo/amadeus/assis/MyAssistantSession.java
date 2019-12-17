package com.paradoxo.amadeus.assis;


import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.paradoxo.amadeus.activity.MainActivity;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyAssistantSession extends VoiceInteractionSession {
    public MyAssistantSession(Context context) {
        super(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
        super.onHandleAssist(data, structure, content);

        Log.e("MyAssistantSession", "Funcionando");
        //meuToast("Amadeus", getContext());

        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        finish();

    }

/*    @Override
    public View onCreateContentView() {
       super.onCreateContentView();

       return getLayoutInflater().inflate(R.layout.layout_inflado, null);
    }
*/

}
