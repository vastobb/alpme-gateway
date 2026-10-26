package com.vastobb.alpmegateway.data

data class AlpmeModel(
    val id: String,
    val displayName: String,
    val contextWindow: Long?,
    val maxOutputTokens: Long?,
    val ownedBy: String?,
    val inputPrice: Double?,
    val outputPrice: Double?,
    val cachedInputPrice: Double?,
) {
    val label: String
        get() = if (displayName.isBlank() || displayName == id) id else displayName
}
