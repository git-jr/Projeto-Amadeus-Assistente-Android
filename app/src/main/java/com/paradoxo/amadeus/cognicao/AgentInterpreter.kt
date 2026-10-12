package com.paradoxo.amadeus.cognicao

import android.graphics.Bitmap
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.AgentStepResult
import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement

interface AgentInterpreter {
    suspend fun interpret(
        command: String,
        elements: List<UiElement>
    ): List<ActionIntent>

    suspend fun interpretStep(
        goal: String,
        history: List<String>,
        elements: List<UiElement>,
        mapBitmap: Bitmap,
        fewShotExamples: List<MacroExample> = emptyList(),
        onRetry: (suspend (attempt: Int, waitMs: Long) -> Unit)? = null
    ): AgentStepResult
}
