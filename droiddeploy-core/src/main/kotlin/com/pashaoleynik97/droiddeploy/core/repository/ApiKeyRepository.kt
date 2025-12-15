package com.pashaoleynik97.droiddeploy.core.repository

import com.pashaoleynik97.droiddeploy.core.domain.ApiKey
import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ApiKeyRepository {
    fun save(apiKey: ApiKey): ApiKey
    fun findById(id: UUID): ApiKey?
    fun findByIdAndApplicationId(id: UUID, applicationId: UUID): ApiKey?
    fun findByValueHash(valueHash: String): ApiKey?
    fun findAll(
        applicationId: UUID,
        role: ApiKeyRole?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<ApiKey>
}
