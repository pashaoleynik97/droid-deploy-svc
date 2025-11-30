package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.db.entity.ApplicationVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaApplicationVersionRepository : JpaRepository<ApplicationVersionEntity, UUID> {
    fun existsByApplicationId(applicationId: UUID): Boolean
}
