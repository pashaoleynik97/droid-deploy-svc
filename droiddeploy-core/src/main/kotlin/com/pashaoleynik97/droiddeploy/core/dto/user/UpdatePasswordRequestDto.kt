package com.pashaoleynik97.droiddeploy.core.dto.user

import io.swagger.v3.oas.annotations.media.Schema

data class UpdatePasswordRequestDto(
    @Schema(description = "New password (minimum 12 characters)", example = "NewSecurePassword456!", required = true)
    val newPassword: String
)
