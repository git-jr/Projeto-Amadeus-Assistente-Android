package com.paradoxo.amadeus.assis

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionSession
import android.util.Log
import androidx.annotation.RequiresApi
import com.paradoxo.amadeus.activity.MainActivity
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MyAssistantSession(context: Context) : VoiceInteractionSession(context) {

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        super.onHandleAssist(data, structure, content)
        Log.e("MyAssistantSession", "Funcionando")

        if (!Settings.canDrawOverlays(context)) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } else {
            context.startForegroundService(
                Intent(context, FloatingButtonAmadeusService::class.java).apply {
                    action = FloatingButtonAmadeusService.ACTION_START_LISTENING
                }
            )
        }
        finish()
    }
}
