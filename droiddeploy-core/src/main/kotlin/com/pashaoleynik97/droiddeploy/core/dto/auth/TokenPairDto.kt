package com.pashaoleynik97.droiddeploy.core.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class TokenPairDto(
    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    val accessToken: String,
    @Schema(description = "Unix timestamp (seconds) when access token expires", example = "1735560600")
    val accessTokenExpiresAt: Long,  // Unix timestamp in seconds
    @Schema(description = "JWT refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.cThIIoDvwdueQB468K5xDc5633seEFoqwxjF_xSJyQQ")
    val refreshToken: String,
    @Schema(description = "Unix timestamp (seconds) when refresh token expires", example = "1738152600")
    val refreshTokenExpiresAt: Long  // Unix timestamp in seconds
)