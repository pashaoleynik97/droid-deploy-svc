package com.pashaoleynik97.droiddeploy.service

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.core.service.UserService
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    override fun createUser(login: String, password: String, role: UserRole): User {
        logger.debug { "Attempting to create user with login: $login, role: $role" }

        if (userRepository.existsByLogin(login)) {
            logger.warn { "Failed to create user: login '$login' already exists" }
            throw IllegalArgumentException("User with login '$login' already exists")
        }

        val now = Instant.now()
        val user = User(
            id = UUID.randomUUID(),
            login = login,
            passwordHash = passwordEncoder.encode(password),
            role = role,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            lastLoginAt = null,
            lastInteractionAt = null,
            tokenVersion = 0
        )

        val savedUser = userRepository.save(user)
        logger.info { "User created successfully: login=${savedUser.login}, id=${savedUser.id}, role=${savedUser.role}" }
        return savedUser
    }

    override fun findByLogin(login: String): User? {
        logger.debug { "Finding user by login: $login" }
        val user = userRepository.findByLogin(login)
        if (user != null) {
            logger.debug { "User found: login=${user.login}, id=${user.id}" }
        } else {
            logger.debug { "User not found with login: $login" }
        }
        return user
    }

    override fun findById(id: UUID): User? {
        logger.debug { "Finding user by id: $id" }
        return userRepository.findById(id)
    }

    override fun userExists(login: String): Boolean {
        logger.debug { "Checking if user exists with login: $login" }
        return userRepository.existsByLogin(login)
    }
}
