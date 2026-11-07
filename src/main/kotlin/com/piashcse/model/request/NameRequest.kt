package com.piashcse.model.request

import kotlinx.serialization.Serializable

/** Common body for catalog create/update endpoints that also accept `?name=`. */
@Serializable
data class NameRequest(val name: String)
