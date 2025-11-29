package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.repository.UserRepository
import com.pashaoleynik97.droiddeploy.db.entity.UserEntity
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class UserRepositoryImpl(
    private val jpaUserRepository: JpaUserRepository
) : UserRepository {

    override fun findByLogin(login: String): User? {
        logger.trace { "Querying database for user by login: $login" }
        return jpaUserRepository.findByLogin(login)?.toDomain()
    }

    override fun findById(id: UUID): User? {
        logger.trace { "Querying database for user by id: $id" }
        return jpaUserRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun save(user: User): User {
        logger.debug { "Saving user to database: login=${user.login}, id=${user.id}" }
        val entity = UserEntity.fromDomain(user)
        val saved = jpaUserRepository.save(entity)
        logger.trace { "User saved successfully: id=${saved.id}" }
        return saved.toDomain()
    }

    override fun existsByLogin(login: String): Boolean {
        logger.trace { "Checking existence of user with login: $login" }
        return jpaUserRepository.existsByLogin(login)
    }

    override fun existsByLoginIgnoreCase(login: String): Boolean {
        logger.trace { "Checking existence of user with login (case-insensitive): $login" }
        return jpaUserRepository.existsByLoginIgnoreCase(login)
    }
}
