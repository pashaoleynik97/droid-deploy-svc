package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.core.dto.apikey.ApiKeyDto
import com.pashaoleynik97.droiddeploy.core.dto.apikey.CreateApiKeyRequestDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ApiKeyService {
    fun createApiKey(applicationId: UUID, request: CreateApiKeyRequestDto): ApiKeyDto
    fun listApiKeys(
        applicationId: UUID,
        role: ApiKeyRole?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<ApiKeyDto>
    fun revokeApiKey(applicationId: UUID, apiKeyId: UUID)
}
