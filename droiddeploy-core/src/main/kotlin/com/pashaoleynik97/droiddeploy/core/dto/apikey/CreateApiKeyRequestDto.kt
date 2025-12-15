package com.pashaoleynik97.droiddeploy.core.dto.apikey

data class CreateApiKeyRequestDto(
    val name: String,
    val role: String,
    val expireBy: Long?
)
