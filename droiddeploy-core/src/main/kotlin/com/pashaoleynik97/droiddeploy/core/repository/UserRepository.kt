package com.pashaoleynik97.droiddeploy.core.repository

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UserRepository {
    fun findByLogin(login: String): User?
    fun findById(id: UUID): User?
    fun save(user: User): User
    fun existsByLogin(login: String): Boolean
    fun existsByLoginIgnoreCase(login: String): Boolean
    fun findAll(role: UserRole?, isActive: Boolean?, pageable: Pageable): Page<User>
}
