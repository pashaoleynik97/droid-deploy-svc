package com.pashaoleynik97.droiddeploy.core.dto.application

import io.swagger.v3.oas.annotations.media.Schema

data class CreateApplicationRequestDto(
    @Schema(description = "Application display name", example = "My Android App", required = true)
    val name: String,
    @Schema(description = "Android application bundle ID (must be unique)", example = "com.example.myapp", required = true)
    val bundleId: String
)