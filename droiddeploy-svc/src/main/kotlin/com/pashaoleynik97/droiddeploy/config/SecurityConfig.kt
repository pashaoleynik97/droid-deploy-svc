package com.pashaoleynik97.droiddeploy.config

import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

private val logger = KotlinLogging.logger {}

@Configuration
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        logger.info { "Initializing BCrypt password encoder" }
        return BCryptPasswordEncoder()
    }
}
