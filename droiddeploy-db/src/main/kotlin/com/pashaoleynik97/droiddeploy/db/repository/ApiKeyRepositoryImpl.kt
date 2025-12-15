package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.core.domain.ApiKey
import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.core.repository.ApiKeyRepository
import com.pashaoleynik97.droiddeploy.db.entity.ApiKeyEntity
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class ApiKeyRepositoryImpl(
    private val jpaApiKeyRepository: JpaApiKeyRepository
) : ApiKeyRepository {

    override fun save(apiKey: ApiKey): ApiKey {
        logger.debug { "Saving API key to database: name=${apiKey.name}, id=${apiKey.id}" }
        val entity = ApiKeyEntity.fromDomain(apiKey)
        val saved = jpaApiKeyRepository.save(entity)
        logger.trace { "API key saved successfully: id=${saved.id}" }
        return saved.toDomain()
    }

    override fun findById(id: UUID): ApiKey? {
        logger.trace { "Querying database for API key by id: $id" }
        return jpaApiKeyRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun findByIdAndApplicationId(id: UUID, applicationId: UUID): ApiKey? {
        logger.trace { "Querying database for API key by id: $id and applicationId: $applicationId" }
        val apiKey = jpaApiKeyRepository.findByIdOrNull(id)?.toDomain()
        return if (apiKey?.applicationId == applicationId) apiKey else null
    }

    override fun findByValueHash(valueHash: String): ApiKey? {
        logger.trace { "Querying database for API key by value hash" }
        return jpaApiKeyRepository.findByValueHash(valueHash)?.toDomain()
    }

    override fun findAll(
        applicationId: UUID,
        role: ApiKeyRole?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<ApiKey> {
        logger.debug { "Querying database for API keys: applicationId=$applicationId, role=$role, isActive=$isActive" }
        val result = jpaApiKeyRepository.findAllByApplicationIdWithFilters(
            applicationId = applicationId,
            role = role,
            isActive = isActive,
            pageable = pageable
        )
        logger.trace { "Found ${result.totalElements} API keys" }
        return result.map(ApiKeyEntity::toDomain)
    }
}
