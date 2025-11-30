package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.db.entity.ApplicationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
    fun findByBundleId(bundleId: String): ApplicationEntity?
    fun existsByBundleId(bundleId: String): Boolean
}
