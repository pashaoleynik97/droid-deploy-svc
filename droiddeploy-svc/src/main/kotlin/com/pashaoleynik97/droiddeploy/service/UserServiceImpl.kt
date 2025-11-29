package com.pashaoleynik97.droiddeploy.service

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.core.service.UserService
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

        // Validate role - CONSUMER not allowed via this method
        if (role == UserRole.CONSUMER) {
            logger.warn { "Failed to create user: CONSUMER role not allowed" }
            throw InvalidRoleException("CONSUMER role cannot be created via this endpoint. CONSUMER users are created with applications.")
        }

        // Validate login format (3-20 chars, alphanumeric + underscore/dash)
        val loginPattern = Regex("^[a-zA-Z0-9_-]{3,20}$")
        if (!login.matches(loginPattern)) {
            logger.warn { "Failed to create user: invalid login format: $login" }
            throw InvalidLoginFormatException()
        }

        // Validate login uniqueness (case-insensitive)
        if (userRepository.existsByLoginIgnoreCase(login)) {
            logger.warn { "Failed to create user: login '$login' already exists (case-insensitive check)" }
            throw LoginAlreadyExistsException(login)
        }

        // Validate password strength (min 10 chars, must contain uppercase, lowercase, digit)
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$")
        if (!password.matches(passwordPattern)) {
            logger.warn { "Failed to create user: password doesn't meet security requirements" }
            throw InvalidPasswordException()
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

    override fun findAll(role: UserRole?, isActive: Boolean?, pageable: Pageable): Page<User> {
        logger.debug { "Finding users with filters: role=$role, isActive=$isActive, page=${pageable.pageNumber}, size=${pageable.pageSize}" }
        val users = userRepository.findAll(role, isActive, pageable)
        logger.info { "Found ${users.totalElements} users matching filters" }
        return users
    }
}
