package com.paradoxo.amadeus.cognicao.localmodel

// Modelos verificados em https://huggingface.co/litert-community (2026-05-26)
// HuggingFace requer token para download — o DownloadManager usa redirect automático.
// Se o download falhar com 401, configure um token HF no cabeçalho da requisição.
object SupportedModels {

    data class ModelDescriptor(
        val modelId: String,
        val displayName: String,
        val description: String,
        val sizeBytes: Long,
        val downloadUrl: String,
        val requiresHfToken: Boolean = false
    )

    val CATALOG: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            modelId = "gemma3-1b-it-int4",
            displayName = "Gemma 3 1B (Leve)",
            description = "584 MB · Mais leve, roda em qualquer dispositivo com 4 GB+ de RAM.",
            sizeBytes = 584_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm",
            requiresHfToken = true
        ),
        ModelDescriptor(
            modelId = "gemma4-e2b-it",
            displayName = "Gemma 4 E2B (Balanceado)",
            description = "2.59 GB · Geração mais recente, boa qualidade. Requer 6 GB+ de RAM.",
            sizeBytes = 2_590_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        ),
        ModelDescriptor(
            modelId = "gemma4-e4b-it",
            displayName = "Gemma 4 E4B (Alta Qualidade)",
            description = "3.66 GB · Maior qualidade. Requer 8 GB+ de RAM e ~4 GB de espaço livre.",
            sizeBytes = 3_660_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
        )
    )
}
