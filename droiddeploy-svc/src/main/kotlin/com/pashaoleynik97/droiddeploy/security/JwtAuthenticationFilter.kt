package com.pashaoleynik97.droiddeploy.security

import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.rest.security.JwtAuthentication
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

/**
 * Filter that extracts and validates JWT tokens from Authorization header
 * and sets JwtAuthentication in SecurityContext
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)

            if (token != null) {
                authenticateWithToken(token)
            } else {
                logger.trace { "No JWT token found in request to ${request.requestURI}" }
            }
        } catch (e: Exception) {
            logger.warn { "JWT authentication failed: ${e.message}" }
            // Clear security context on error
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Extract JWT token from Authorization header
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        return if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    /**
     * Validate token and set authentication in SecurityContext
     */
    private fun authenticateWithToken(token: String) {
        val claims = jwtTokenProvider.validateAndParseClaims(token)

        if (claims == null) {
            logger.debug { "Token validation failed" }
            return
        }

        // Extract token type - only "access" tokens are valid for API calls
        val tokenType = jwtTokenProvider.getTokenType(claims)
        if (tokenType != "access") {
            logger.debug { "Invalid token type: $tokenType (expected: access)" }
            return
        }

        // Extract user ID
        val userId = jwtTokenProvider.extractUserId(claims)
        if (userId == null) {
            logger.warn { "Failed to extract user ID from token" }
            return
        }

        // Extract role
        val roleString = jwtTokenProvider.getRole(claims)
        if (roleString == null) {
            logger.warn { "Failed to extract role from token" }
            return
        }

        val userRole = try {
            UserRole.valueOf(roleString)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Invalid role in token: $roleString" }
            return
        }

        // Create and set authentication
        val authentication = JwtAuthentication(userId, userRole)
        SecurityContextHolder.getContext().authentication = authentication

        logger.debug { "JWT authentication successful for user: $userId, role: $userRole" }
    }
}
