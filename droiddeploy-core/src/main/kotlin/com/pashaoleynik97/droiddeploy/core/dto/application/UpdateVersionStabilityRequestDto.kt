package com.pashaoleynik97.droiddeploy.core.dto.application

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateVersionStabilityRequestDto(
    @Schema(description = "Stability flag - true to mark as stable, false to mark as unstable", example = "true", required = true)
    val stable: Boolean
)
