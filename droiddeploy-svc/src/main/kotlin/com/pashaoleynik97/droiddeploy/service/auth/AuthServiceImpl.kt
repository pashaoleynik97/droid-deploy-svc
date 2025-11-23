package com.pashaoleynik97.droiddeploy.service.auth

import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidCredentialsException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRefreshTokenException
import com.pashaoleynik97.droiddeploy.core.exception.UnauthorizedAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotActiveException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.rest.model.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.auth.RefreshTokenRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.auth.TokenPairDto
import com.pashaoleynik97.droiddeploy.rest.service.AuthService
import com.pashaoleynik97.droiddeploy.security.JwtTokenProvider
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) : AuthService {

    override fun login(request: LoginRequestDto): TokenPairDto {
        logger.info { "Login attempt for user: ${request.login}" }

        // Find user by login
        val user = userRepository.findByLogin(request.login)
            ?: run {
                logger.warn { "Login failed: user not found with login: ${request.login}" }
                throw InvalidCredentialsException()
            }

        // Check if user is ADMIN
        if (user.role != UserRole.ADMIN) {
            logger.warn { "Login failed: user ${user.login} is not an ADMIN (role: ${user.role})" }
            throw UnauthorizedAccessException("Only ADMIN users can log in with username and password")
        }

        // Check if user is active
        if (!user.isActive) {
            logger.warn { "Login failed: user ${user.login} is not active" }
            throw UserNotActiveException()
        }

        // Verify password
        if (user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            logger.warn { "Login failed: invalid password for user: ${user.login}" }
            throw InvalidCredentialsException()
        }

        // Update last login time
        val now = Instant.now()
        val updatedUser = user.copy(
            lastLoginAt = now,
            updatedAt = now
        )
        userRepository.save(updatedUser)

        logger.info { "User ${user.login} logged in successfully" }

        // Generate token pair
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(updatedUser)

        return TokenPairDto(
            accessToken = tokenPair.accessToken,
            accessTokenExpiresAt = tokenPair.accessTokenExpiresAt.epochSecond,
            refreshToken = tokenPair.refreshToken,
            refreshTokenExpiresAt = tokenPair.refreshTokenExpiresAt.epochSecond
        )
    }

    override fun refreshToken(request: RefreshTokenRequestDto): TokenPairDto {
        logger.info { "Refresh token request received" }

        // Parse and validate the refresh token
        val claims = jwtTokenProvider.validateAndParseClaims(request.refreshToken)
            ?: run {
                logger.warn { "Refresh token validation failed: invalid or expired token" }
                throw InvalidRefreshTokenException()
            }

        // Check token type is "refresh"
        val tokenType = jwtTokenProvider.getTokenType(claims)
        if (tokenType != "refresh") {
            logger.warn { "Refresh token validation failed: token type is not 'refresh' (type: $tokenType)" }
            throw InvalidRefreshTokenException()
        }

        // Check role is ADMIN
        val role = jwtTokenProvider.getRole(claims)
        if (role != UserRole.ADMIN.name) {
            logger.warn { "Refresh token validation failed: token role is not ADMIN (role: $role)" }
            throw InvalidRefreshTokenException()
        }

        // Extract userId from subject
        val userId = jwtTokenProvider.extractUserId(claims)
            ?: run {
                logger.warn { "Refresh token validation failed: could not extract user ID from subject" }
                throw InvalidRefreshTokenException()
            }

        // Load user from repository
        val user = userRepository.findById(userId)
            ?: run {
                logger.warn { "Refresh token validation failed: user not found with ID: $userId" }
                throw InvalidRefreshTokenException()
            }

        // Validate user is active
        if (!user.isActive) {
            logger.warn { "Refresh token validation failed: user ${user.login} is not active" }
            throw InvalidRefreshTokenException()
        }

        // Validate user role is ADMIN
        if (user.role != UserRole.ADMIN) {
            logger.warn { "Refresh token validation failed: user ${user.login} is not ADMIN (role: ${user.role})" }
            throw InvalidRefreshTokenException()
        }

        // Validate token version matches
        val tokenVersion = jwtTokenProvider.getTokenVersion(claims)
        if (tokenVersion != user.tokenVersion) {
            logger.warn { "Refresh token validation failed: token version mismatch (token: $tokenVersion, user: ${user.tokenVersion})" }
            throw InvalidRefreshTokenException()
        }

        logger.info { "Refresh token validated successfully for user: ${user.login}" }

        // Generate new token pair
        val tokenPair = jwtTokenProvider.generateTokenPairForAdmin(user)

        return TokenPairDto(
            accessToken = tokenPair.accessToken,
            accessTokenExpiresAt = tokenPair.accessTokenExpiresAt.epochSecond,
            refreshToken = tokenPair.refreshToken,
            refreshTokenExpiresAt = tokenPair.refreshTokenExpiresAt.epochSecond
        )
    }
}
