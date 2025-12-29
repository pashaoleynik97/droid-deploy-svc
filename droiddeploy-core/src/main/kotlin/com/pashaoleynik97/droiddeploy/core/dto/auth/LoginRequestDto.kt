package com.pashaoleynik97.droiddeploy.core.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class LoginRequestDto(
    @Schema(description = "User login (username)", example = "admin", required = true)
    val login: String,
    @Schema(description = "User password", example = "SecurePassword123!", required = true)
    val password: String
)