package com.paradoxo.amadeus.assis;

import android.os.Build;
import android.service.voice.VoiceInteractionService;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyAssistantService extends VoiceInteractionService {
}