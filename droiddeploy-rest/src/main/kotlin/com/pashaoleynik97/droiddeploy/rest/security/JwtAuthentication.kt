package com.pashaoleynik97.droiddeploy.rest.security

import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * Custom Authentication implementation for JWT-based authentication.
 * Stores user ID and role extracted from JWT token.
 */
class JwtAuthentication(
    val userId: UUID,
    val userRole: UserRole,
    private val authenticated: Boolean = true
) : Authentication {

    override fun getName(): String = "user:$userId"

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${userRole.name}"))
    }

    override fun getCredentials(): Any? = null

    override fun getDetails(): Any? = null

    override fun getPrincipal(): Any = this

    override fun isAuthenticated(): Boolean = authenticated

    override fun setAuthenticated(isAuthenticated: Boolean) {
        throw IllegalArgumentException("Cannot change authentication state")
    }
}
