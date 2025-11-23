package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.domain.User
import com.pashaoleynik97.droiddeploy.core.domain.UserRole
import java.util.UUID

interface UserService {
    fun createUser(login: String, password: String, role: UserRole): User
    fun findByLogin(login: String): User?
    fun findById(id: UUID): User?
    fun userExists(login: String): Boolean
}
