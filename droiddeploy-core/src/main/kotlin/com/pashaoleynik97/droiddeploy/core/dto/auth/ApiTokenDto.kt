package com.pashaoleynik97.droiddeploy.core.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class ApiTokenDto(
    @Schema(description = "API access token for CI/CD or consumer authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    val accessToken: String
)
