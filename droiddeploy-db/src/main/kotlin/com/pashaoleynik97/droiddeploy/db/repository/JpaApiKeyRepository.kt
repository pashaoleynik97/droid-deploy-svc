package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.db.entity.ApiKeyEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface JpaApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findByValueHash(valueHash: String): ApiKeyEntity?

    @Query("""
        SELECT a FROM ApiKeyEntity a
        WHERE a.applicationId = :applicationId
        AND (:role IS NULL OR a.role = :role)
        AND (:isActive IS NULL OR a.isActive = :isActive)
    """)
    fun findAllByApplicationIdWithFilters(
        @Param("applicationId") applicationId: UUID,
        @Param("role") role: ApiKeyRole?,
        @Param("isActive") isActive: Boolean?,
        pageable: Pageable
    ): Page<ApiKeyEntity>
}
