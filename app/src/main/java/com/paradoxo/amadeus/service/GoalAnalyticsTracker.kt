package com.paradoxo.amadeus.service

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.paradoxo.amadeus.BuildConfig
import com.paradoxo.amadeus.activity.ConfigPrimariaActivity
import com.paradoxo.amadeus.modelo.GoalAnalyticsEvent
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalAnalyticsTracker(
    private val context: Context,
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    fun track(goalText: String) {
        val sanitizedGoal = goalText.lowercase().trim()
        if (sanitizedGoal.isEmpty()) {
            Log.w(TAG, "Ignorando goal vazio")
            return
        }

        if (!getPrefBool(ConfigPrimariaActivity.PREF_UPLOAD_DADOS_AUTORIZADO, context, true)) {
            Log.d(TAG, "Goal analytics desativado por preferencia")
            return
        }

        val timestamp = System.currentTimeMillis()
        val dayKey = SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(timestamp))
        val event = GoalAnalyticsEvent(
            goalText = sanitizedGoal,
            timestamp = timestamp,
            versionApp = BuildConfig.VERSION_NAME
        )

        firebaseDatabase.reference
            .child(GOAL_ANALYTICS_NODE)
            .child(dayKey)
            .push()
            .setValue(event)
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao salvar goal analytics", error)
            }
            .addOnSuccessListener { temp ->
                Log.d(TAG, "Goal analytics salvo com sucesso: $event")
            }
    }

    companion object {
        private const val TAG = "GoalAnalyticsTracker"
        private const val GOAL_ANALYTICS_NODE = "goal-analytics"
        private const val DATE_PATTERN = "dd-MM-yyyy"
    }
}
