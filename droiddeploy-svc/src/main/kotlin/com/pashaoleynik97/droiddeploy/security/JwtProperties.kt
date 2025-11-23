package com.pashaoleynik97.droiddeploy.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties(
    val secret: String,
    val issuer: String,
    val accessTokenValiditySeconds: Long,
    val refreshTokenValiditySeconds: Long
)
