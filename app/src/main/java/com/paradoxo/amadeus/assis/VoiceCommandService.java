package com.paradoxo.amadeus.assis;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.VoiceInteractionService;
import android.speech.SpeechRecognizer;
import android.service.voice.AlwaysOnHotwordDetector.Callback;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.paradoxo.amadeus.activity.MainActivity;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VoiceCommandService extends VoiceInteractionService {
    private static final String TAG = "AlwaysOnHotwordDetector";
    Locale locale = new Locale("pt-BR");
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;

    public final Callback mHotwordCallback = new Callback() {
        @Override
        public void onAvailabilityChanged(int status) {
            Log.e(TAG, "onAvailabilityChanged(" + status + ")");
            hotwordAvailabilityChangeHelper(status);
        }

        @Override
        public void onDetected(@NonNull AlwaysOnHotwordDetector.EventPayload eventPayload) {
            Log.e(TAG, "onDetected");
        }

        @Override
        public void onError() {
            Log.e(TAG, "onError");
        }

        @Override
        public void onRecognitionPaused() {
            Log.e(TAG, "onRecognitionPaused");
        }

        @Override
        public void onRecognitionResumed() {
            Log.e(TAG, "onRecognitionResumed");
        }
    };

    private AlwaysOnHotwordDetector mHotwordDetector;

    @Override
    public void onCreate(){
       Log.e(TAG, "Entered on create");
       super.onCreate();

    }

    @Override
    public void onReady() {
        super.onReady();
        Log.e(TAG, "Creating " + this);
        mHotwordDetector = createAlwaysOnHotwordDetector(
                "Teste", Locale.forLanguageTag("pt-BR"), mHotwordCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle args = new Bundle();
        args.putParcelable("intent", new Intent(this, MainActivity.class));
/*        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showSession(args, 0);
        }
*/
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void hotwordAvailabilityChangeHelper(int availability) {
        Log.e(TAG, "Hotword availability = " + availability);
        switch (availability) {
            case AlwaysOnHotwordDetector.STATE_HARDWARE_UNAVAILABLE:
                Log.e(TAG, "STATE_HARDWARE_UNAVAILABLE");
                break;
            case AlwaysOnHotwordDetector.STATE_KEYPHRASE_UNSUPPORTED:
                Log.e(TAG, "STATE_KEYPHRASE_UNSUPPORTED");
                break;
            case AlwaysOnHotwordDetector.STATE_KEYPHRASE_UNENROLLED:
                Log.e(TAG, "STATE_KEYPHRASE_UNENROLLED");
                Intent enroll = mHotwordDetector.createEnrollIntent();
                Log.e(TAG, "Need to enroll with " + enroll);
                break;
            case AlwaysOnHotwordDetector.STATE_KEYPHRASE_ENROLLED:
                Log.e(TAG, "STATE_KEYPHRASE_ENROLLED - starting recognition");
                if (mHotwordDetector.startRecognition(0)) {
                    Log.e(TAG, "startRecognition succeeded");
                } else {
                    Log.e(TAG, "startRecognition failed");
                }
                break;
        }
    }}