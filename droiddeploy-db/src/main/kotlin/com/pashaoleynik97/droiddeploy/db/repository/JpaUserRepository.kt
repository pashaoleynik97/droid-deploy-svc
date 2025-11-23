package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.db.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaUserRepository : JpaRepository<UserEntity, UUID> {
    fun findByLogin(login: String): UserEntity?
    fun existsByLogin(login: String): Boolean
}
