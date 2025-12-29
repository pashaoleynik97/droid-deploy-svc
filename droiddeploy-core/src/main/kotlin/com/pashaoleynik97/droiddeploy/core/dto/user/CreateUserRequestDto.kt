package com.pashaoleynik97.droiddeploy.core.dto.user

import io.swagger.v3.oas.annotations.media.Schema

data class CreateUserRequestDto(
    @Schema(description = "User login (username)", example = "newuser", required = true)
    val login: String,
    @Schema(description = "User password (minimum 12 characters)", example = "SecurePassword123!", required = true)
    val password: String,
    @Schema(description = "User role (ADMIN or CI only, CONSUMER not allowed)", example = "CI", allowableValues = ["ADMIN", "CI"], required = true)
    val role: String  // "ADMIN" or "CI"
)