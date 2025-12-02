package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.db.entity.ApplicationVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaApplicationVersionRepository : JpaRepository<ApplicationVersionEntity, UUID> {
    fun existsByApplicationId(applicationId: UUID): Boolean

    fun existsByApplicationIdAndVersionCode(applicationId: UUID, versionCode: Long): Boolean

    @Query("SELECT MAX(v.versionCode) FROM ApplicationVersionEntity v WHERE v.application.id = :applicationId")
    fun findMaxVersionCodeByApplicationId(@Param("applicationId") applicationId: UUID): Long?

    fun findByApplicationIdAndVersionCode(applicationId: UUID, versionCode: Long): ApplicationVersionEntity?
}
