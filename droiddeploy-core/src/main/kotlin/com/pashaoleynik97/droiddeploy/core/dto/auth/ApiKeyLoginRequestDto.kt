package com.pashaoleynik97.droiddeploy.core.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class ApiKeyLoginRequestDto(
    @Schema(description = "Application API key (created via /api/v1/application/{id}/security/apikey)", example = "dd_ak_1a2b3c4d5e6f7g8h9i0j", required = true)
    val apiKey: String
)
