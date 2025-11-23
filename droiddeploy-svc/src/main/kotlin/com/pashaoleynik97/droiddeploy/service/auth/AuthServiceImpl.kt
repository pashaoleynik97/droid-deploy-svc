package com.pashaoleynik97.droiddeploy.service.auth

import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidCredentialsException
import com.pashaoleynik97.droiddeploy.core.exception.UnauthorizedAccessException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotActiveException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.rest.model.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.auth.TokenPairDto
import com.pashaoleynik97.droiddeploy.rest.service.AuthService
import com.pashaoleynik97.droiddeploy.security.JwtTokenProvider
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

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
}
