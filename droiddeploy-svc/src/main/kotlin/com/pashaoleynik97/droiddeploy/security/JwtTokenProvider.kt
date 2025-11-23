package com.pashaoleynik97.droiddeploy.security

import com.pashaoleynik97.droiddeploy.core.domain.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

private val logger = KotlinLogging.logger {}

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    data class TokenPair(
        val accessToken: String,
        val accessTokenExpiresAt: Instant,
        val refreshToken: String,
        val refreshTokenExpiresAt: Instant
    )

    /**
     * Generate both access and refresh tokens for an ADMIN user
     */
    fun generateTokenPairForAdmin(user: User): TokenPair {
        logger.debug { "Generating token pair for user: ${user.id}" }

        val now = Instant.now()
        val accessExpiresAt = now.plusSeconds(jwtProperties.accessTokenValiditySeconds)
        val refreshExpiresAt = now.plusSeconds(jwtProperties.refreshTokenValiditySeconds)

        val accessToken = generateToken(
            subject = "user:${user.id}",
            role = user.role.name,
            tokenType = "access",
            tokenVersion = user.tokenVersion,
            issuedAt = now,
            expiresAt = accessExpiresAt
        )

        val refreshToken = generateToken(
            subject = "user:${user.id}",
            role = user.role.name,
            tokenType = "refresh",
            tokenVersion = user.tokenVersion,
            issuedAt = now,
            expiresAt = refreshExpiresAt
        )

        logger.info { "Token pair generated successfully for user: ${user.id}" }

        return TokenPair(
            accessToken = accessToken,
            accessTokenExpiresAt = accessExpiresAt,
            refreshToken = refreshToken,
            refreshTokenExpiresAt = refreshExpiresAt
        )
    }

    /**
     * Generate a JWT token with the given parameters
     */
    private fun generateToken(
        subject: String,
        role: String,
        tokenType: String,
        tokenVersion: Int,
        issuedAt: Instant,
        expiresAt: Instant
    ): String {
        return Jwts.builder()
            .subject(subject)
            .claim("role", role)
            .claim("tokenType", tokenType)
            .claim("tokenVersion", tokenVersion)
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact()
    }

    /**
     * Validate JWT token and return claims if valid
     */
    fun validateAndParseClaims(token: String): Claims? {
        return try {
            logger.trace { "Validating JWT token" }

            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer)
                .build()
                .parseSignedClaims(token)
                .payload

            logger.trace { "Token validated successfully for subject: ${claims.subject}" }
            claims
        } catch (e: Exception) {
            logger.warn { "Token validation failed: ${e.message}" }
            null
        }
    }

    /**
     * Extract user ID from token subject (format: "user:{userId}")
     */
    fun extractUserId(claims: Claims): UUID? {
        return try {
            val subject = claims.subject
            if (subject.startsWith("user:")) {
                UUID.fromString(subject.substring(5))
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn { "Failed to extract user ID from claims: ${e.message}" }
            null
        }
    }

    /**
     * Get token type from claims
     */
    fun getTokenType(claims: Claims): String? {
        return claims["tokenType"] as? String
    }

    /**
     * Get token version from claims
     */
    fun getTokenVersion(claims: Claims): Int? {
        return claims["tokenVersion"] as? Int
    }

    /**
     * Get role from claims
     */
    fun getRole(claims: Claims): String? {
        return claims["role"] as? String
    }
}
