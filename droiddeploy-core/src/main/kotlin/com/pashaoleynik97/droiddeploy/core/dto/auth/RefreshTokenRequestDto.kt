package com.pashaoleynik97.droiddeploy.core.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class RefreshTokenRequestDto(
    @Schema(description = "Refresh token received from login endpoint", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.cThIIoDvwdueQB468K5xDc5633seEFoqwxjF_xSJyQQ", required = true)
    val refreshToken: String
)