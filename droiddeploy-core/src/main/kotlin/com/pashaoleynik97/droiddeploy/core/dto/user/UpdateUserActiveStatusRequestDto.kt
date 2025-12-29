package com.pashaoleynik97.droiddeploy.core.dto.user

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateUserActiveStatusRequestDto(
    @Schema(description = "Active status - true to activate, false to deactivate", example = "true", required = true)
    val setActive: Boolean
)
