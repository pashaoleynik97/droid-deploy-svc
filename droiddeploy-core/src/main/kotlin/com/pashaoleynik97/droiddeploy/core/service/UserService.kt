package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UserService {
    fun createUser(login: String, password: String, role: UserRole): User
    fun findByLogin(login: String): User?
    fun findById(id: UUID): User?
    fun userExists(login: String): Boolean
    fun findAll(role: UserRole?, isActive: Boolean?, pageable: Pageable): Page<User>
    fun updatePassword(userId: UUID, newPassword: String): User
    fun updateActiveStatus(userId: UUID, setActive: Boolean, currentUserId: UUID, superAdminLogin: String): User
}
