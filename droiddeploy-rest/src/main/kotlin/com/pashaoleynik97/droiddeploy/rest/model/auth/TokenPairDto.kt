package com.pashaoleynik97.droiddeploy.rest.model.auth

data class TokenPairDto(
    val accessToken: String,
    val accessTokenExpiresAt: Long,  // Unix timestamp in seconds
    val refreshToken: String,
    val refreshTokenExpiresAt: Long  // Unix timestamp in seconds
)
