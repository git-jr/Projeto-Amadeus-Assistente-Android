package com.paradoxo.amadeus.assis

import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MyAssistantSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle): VoiceInteractionSession = MyAssistantSession(this)
}
