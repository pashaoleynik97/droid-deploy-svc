package com.pashaoleynik97.droiddeploy.core.dto.apikey

import io.swagger.v3.oas.annotations.media.Schema

data class CreateApiKeyRequestDto(
    @Schema(description = "Human-readable API key name", example = "CI Pipeline Key", required = true)
    val name: String,
    @Schema(description = "API key role (CI for CI/CD pipelines, CONSUMER for app downloads)", example = "CI", allowableValues = ["CI", "CONSUMER"], required = true)
    val role: String,
    @Schema(description = "Optional expiration timestamp in milliseconds (null for no expiration)", example = "1767196200000", nullable = true, required = false)
    val expireBy: Long?
)
