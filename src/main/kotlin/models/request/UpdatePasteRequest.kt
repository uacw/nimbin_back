package tech.nimbus.models.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePasteRequest(
    val title: String? = null,
    val content: String? = null,
    val syntaxLanguage: String? = null,
    val visibility: String? = null,
    val expiresAt: String? = null
)
