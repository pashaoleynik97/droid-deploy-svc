package com.pashaoleynik97.droiddeploy.service.apikey

import com.pashaoleynik97.droiddeploy.core.domain.ApiKey
import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.core.dto.apikey.ApiKeyDto
import com.pashaoleynik97.droiddeploy.core.dto.apikey.CreateApiKeyRequestDto
import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApiKeyRoleException
import com.pashaoleynik97.droiddeploy.core.repository.ApiKeyRepository
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.core.service.ApiKeyService
import com.pashaoleynik97.droiddeploy.security.ApiKeyHashingUtil
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ApiKeyServiceImpl(
    private val apiKeyRepository: ApiKeyRepository,
    private val applicationRepository: ApplicationRepository,
    private val hashingUtil: ApiKeyHashingUtil
) : ApiKeyService {

    override fun createApiKey(applicationId: UUID, request: CreateApiKeyRequestDto): ApiKeyDto {
        logger.info { "Creating API key for application: $applicationId, name: ${request.name}, role: ${request.role}" }

        // Validate application exists
        val application = applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        // Validate role
        val role = try {
            ApiKeyRole.valueOf(request.role)
        } catch (e: IllegalArgumentException) {
            throw InvalidApiKeyRoleException("Invalid API key role: ${request.role}. Must be CI or CONSUMER")
        }

        // Calculate expires_at
        val expiresAt = calculateExpiresAt(request.expireBy)

        // Generate random API key
        val rawApiKey = hashingUtil.generateApiKey()

        // Hash the API key
        val valueHash = hashingUtil.hashApiKey(rawApiKey)

        // Create API key domain object
        val now = Instant.now()
        val apiKey = ApiKey(
            id = UUID.randomUUID(),
            name = request.name,
            valueHash = valueHash,
            role = role,
            applicationId = applicationId,
            isActive = true,
            createdAt = now.toEpochMilli(),
            lastUsedAt = null,
            expiresAt = expiresAt?.toEpochMilli(),
            tokenVersion = 0
        )

        // Save to repository
        val savedApiKey = apiKeyRepository.save(apiKey)

        logger.info { "API key created successfully: id=${savedApiKey.id}, applicationId=$applicationId" }

        // Return DTO with raw API key (only returned once)
        return ApiKeyDto.fromDomain(savedApiKey, rawApiKey)
    }

    override fun listApiKeys(
        applicationId: UUID,
        role: ApiKeyRole?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<ApiKeyDto> {
        logger.info { "Listing API keys for application: $applicationId, role: $role, isActive: $isActive" }

        // Validate application exists
        applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        // Fetch API keys
        val apiKeysPage = apiKeyRepository.findAll(applicationId, role, isActive, pageable)

        logger.info { "Retrieved ${apiKeysPage.totalElements} API keys for application: $applicationId" }

        // Convert to DTOs (without raw API key)
        return apiKeysPage.map { ApiKeyDto.fromDomain(it) }
    }

    override fun revokeApiKey(applicationId: UUID, apiKeyId: UUID) {
        logger.info { "Revoking API key: id=$apiKeyId, applicationId=$applicationId" }

        // Validate application exists
        applicationRepository.findById(applicationId)
            ?: throw ApplicationNotFoundException(applicationId)

        // Find API key
        val apiKey = apiKeyRepository.findByIdAndApplicationId(apiKeyId, applicationId)
            ?: throw ApiKeyNotFoundException("API key with ID '$apiKeyId' not found for application '$applicationId'")

        // Set is_active = false
        val revokedApiKey = apiKey.copy(isActive = false)
        apiKeyRepository.save(revokedApiKey)

        logger.info { "API key revoked successfully: id=$apiKeyId" }
    }

    private fun calculateExpiresAt(expireBy: Long?): Instant? {
        return when {
            expireBy == null || expireBy == 0L -> null
            expireBy < 0 -> throw IllegalArgumentException("expireBy cannot be negative")
            else -> Instant.now().plusMillis(expireBy)
        }
    }
}
