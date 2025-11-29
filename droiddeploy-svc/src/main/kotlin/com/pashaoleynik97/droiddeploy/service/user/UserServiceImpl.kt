package com.pashaoleynik97.droiddeploy.service.user

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import com.pashaoleynik97.droiddeploy.core.exception.InvalidLoginFormatException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidPasswordException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidRoleException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidUserTypeException
import com.pashaoleynik97.droiddeploy.core.exception.LoginAlreadyExistsException
import com.pashaoleynik97.droiddeploy.core.exception.UserNotFoundException
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.core.service.UserService
import com.pashaoleynik97.droiddeploy.core.utils.CredentialsValidator
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
        if (!CredentialsValidator.isLoginValid(login)) {
            logger.warn { "Failed to create user: invalid login format: $login" }
            throw InvalidLoginFormatException()
        }

        // Validate login uniqueness (case-insensitive)
        if (userRepository.existsByLoginIgnoreCase(login)) {
            logger.warn { "Failed to create user: login '$login' already exists (case-insensitive check)" }
            throw LoginAlreadyExistsException(login)
        }

        // Validate password strength (min 10 chars, must contain uppercase, lowercase, digit)
        if (!CredentialsValidator.isPasswordValid(password)) {
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

    override fun updatePassword(userId: UUID, newPassword: String): User {
        logger.debug { "Attempting to update password for user: $userId" }

        // Find user
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException(userId)

        // Check if user is ADMIN (only ADMIN users can have passwords)
        if (user.role != UserRole.ADMIN) {
            logger.warn { "Failed to update password: user ${user.id} has role ${user.role}, only ADMIN users can have passwords" }
            throw InvalidUserTypeException(user.role)
        }

        // Validate password strength
        if (!CredentialsValidator.isPasswordValid(newPassword)) {
            logger.warn { "Failed to update password: password doesn't meet security requirements" }
            throw InvalidPasswordException()
        }

        // Update password, increment token version, and update timestamp
        val updatedUser = user.copy(
            passwordHash = passwordEncoder.encode(newPassword),
            tokenVersion = user.tokenVersion + 1,
            updatedAt = Instant.now()
        )

        val savedUser = userRepository.save(updatedUser)
        logger.info { "Password updated successfully for user: ${savedUser.id}, tokenVersion incremented to ${savedUser.tokenVersion}" }
        return savedUser
    }
}
