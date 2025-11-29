package com.pashaoleynik97.droiddeploy.config

import com.pashaoleynik97.droiddeploy.security.JwtAuthenticationFilter
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

private val logger = KotlinLogging.logger {}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        logger.info { "Initializing BCrypt password encoder" }
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        logger.info { "Configuring security filter chain for stateless API" }

        http
            .csrf { it.disable() }  // Disable CSRF for stateless API
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }  // Stateless session
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()  // Allow anonymous access to auth endpoints
                    .anyRequest().authenticated()  // All other endpoints require authentication
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)  // Add JWT filter
            .httpBasic { it.disable() }  // Disable basic auth
            .formLogin { it.disable() }  // Disable form login

        return http.build()
    }
}
