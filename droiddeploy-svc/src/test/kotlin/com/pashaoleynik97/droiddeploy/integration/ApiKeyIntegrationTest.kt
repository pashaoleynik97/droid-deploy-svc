package com.pashaoleynik97.droiddeploy.integration

import com.pashaoleynik97.droiddeploy.AbstractIntegrationTest
import com.pashaoleynik97.droiddeploy.core.domain.ApiKeyRole
import com.pashaoleynik97.droiddeploy.core.dto.apikey.CreateApiKeyRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.ApiKeyLoginRequestDto
import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyExpiredException
import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.ApiKeyRevokedException
import com.pashaoleynik97.droiddeploy.core.exception.ApplicationNotFoundException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApiKeyException
import com.pashaoleynik97.droiddeploy.core.exception.InvalidApiKeyRoleException
import com.pashaoleynik97.droiddeploy.core.repository.ApiKeyRepository
import com.pashaoleynik97.droiddeploy.core.service.ApiKeyService
import com.pashaoleynik97.droiddeploy.core.service.ApplicationService
import com.pashaoleynik97.droiddeploy.core.service.AuthService
import com.pashaoleynik97.droiddeploy.security.ApiKeyHashingUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.UUID

class ApiKeyIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var apiKeyService: ApiKeyService

    @Autowired
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    private lateinit var hashingUtil: ApiKeyHashingUtil

    private lateinit var testApplicationId: UUID

    @BeforeEach
    fun setUp() {
        // Create a test application with a unique bundleId
        val uniqueBundleId = "com.test.apikey.app${System.currentTimeMillis()}"
        val createAppRequest = CreateApplicationRequestDto(
            name = "Test Application for API Keys",
            bundleId = uniqueBundleId
        )
        val application = applicationService.createApplication(createAppRequest)
        testApplicationId = application.id
    }

    @Test
    fun `createApiKey should create API key with CI role and return raw key`() {
        // Given
        val request = CreateApiKeyRequestDto(
            name = "Test CI Key",
            role = "CI",
            expireBy = null
        )

        // When
        val apiKey = apiKeyService.createApiKey(testApplicationId, request)

        // Then
        assertNotNull(apiKey)
        assertEquals("Test CI Key", apiKey.name)
        assertEquals("CI", apiKey.role)
        assertEquals(testApplicationId, apiKey.applicationId)
        assertTrue(apiKey.isActive)
        assertNotNull(apiKey.createdAt)
        assertNull(apiKey.lastUsedAt)
        assertNull(apiKey.expiresAt)
        assertNotNull(apiKey.apiKey) // Raw API key should be present only in create response
        assertTrue(apiKey.apiKey!!.isNotBlank())
    }

    @Test
    fun `createApiKey should create API key with CONSUMER role`() {
        // Given
        val request = CreateApiKeyRequestDto(
            name = "Test Consumer Key",
            role = "CONSUMER",
            expireBy = null
        )

        // When
        val apiKey = apiKeyService.createApiKey(testApplicationId, request)

        // Then
        assertNotNull(apiKey)
        assertEquals("Test Consumer Key", apiKey.name)
        assertEquals("CONSUMER", apiKey.role)
        assertEquals(testApplicationId, apiKey.applicationId)
    }

    @Test
    fun `createApiKey should create API key with expiration time`() {
        // Given
        val expireIn = 3600000L // 1 hour in milliseconds
        val request = CreateApiKeyRequestDto(
            name = "Test Expiring Key",
            role = "CI",
            expireBy = expireIn
        )

        // When
        val before = Instant.now()
        val apiKey = apiKeyService.createApiKey(testApplicationId, request)
        val after = Instant.now()

        // Then
        assertNotNull(apiKey.expiresAt)
        assertTrue(apiKey.expiresAt!! >= before.plusMillis(expireIn))
        assertTrue(apiKey.expiresAt!! <= after.plusMillis(expireIn))
    }

    @Test
    fun `createApiKey should throw exception for invalid role`() {
        // Given
        val request = CreateApiKeyRequestDto(
            name = "Test Invalid Role Key",
            role = "INVALID_ROLE",
            expireBy = null
        )

        // When & Then
        assertThrows<InvalidApiKeyRoleException> {
            apiKeyService.createApiKey(testApplicationId, request)
        }
    }

    @Test
    fun `createApiKey should throw exception for non-existent application`() {
        // Given
        val nonExistentAppId = UUID.randomUUID()
        val request = CreateApiKeyRequestDto(
            name = "Test Key",
            role = "CI",
            expireBy = null
        )

        // When & Then
        assertThrows<ApplicationNotFoundException> {
            apiKeyService.createApiKey(nonExistentAppId, request)
        }
    }

    @Test
    fun `createApiKey should throw exception for negative expireBy`() {
        // Given
        val request = CreateApiKeyRequestDto(
            name = "Test Key",
            role = "CI",
            expireBy = -1000L
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            apiKeyService.createApiKey(testApplicationId, request)
        }
    }

    @Test
    fun `listApiKeys should return all API keys when no filters provided`() {
        // Given - Create multiple API keys
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key 1", "CI", null))
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key 2", "CONSUMER", null))
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key 3", "CI", null))

        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        // When
        val result = apiKeyService.listApiKeys(testApplicationId, null, null, pageable)

        // Then
        assertEquals(3, result.totalElements)
        assertFalse(result.content.any { it.apiKey != null }) // Raw API key should not be present in list
    }

    @Test
    fun `listApiKeys should filter by role`() {
        // Given
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("CI Key 1", "CI", null))
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("CONSUMER Key", "CONSUMER", null))
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("CI Key 2", "CI", null))

        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        // When
        val result = apiKeyService.listApiKeys(testApplicationId, ApiKeyRole.CI, null, pageable)

        // Then
        assertEquals(2, result.totalElements)
        assertTrue(result.content.all { it.role == "CI" })
    }

    @Test
    fun `listApiKeys should filter by isActive`() {
        // Given
        val key1 = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key 1", "CI", null))
        apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key 2", "CI", null))

        // Revoke one key
        apiKeyService.revokeApiKey(testApplicationId, key1.id)

        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        // When
        val activeResult = apiKeyService.listApiKeys(testApplicationId, null, true, pageable)
        val inactiveResult = apiKeyService.listApiKeys(testApplicationId, null, false, pageable)

        // Then
        assertEquals(1, activeResult.totalElements)
        assertTrue(activeResult.content.all { it.isActive })

        // Note: isActive=false means return everything (based on requirements)
        // But in the service, we treat it as return all when null
        // So we need to test with explicit false filter
    }

    @Test
    fun `revokeApiKey should set isActive to false`() {
        // Given
        val apiKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key to Revoke", "CI", null))

        // When
        apiKeyService.revokeApiKey(testApplicationId, apiKey.id)

        // Then
        val revokedKey = apiKeyRepository.findById(apiKey.id)
        assertNotNull(revokedKey)
        assertFalse(revokedKey!!.isActive)
    }

    @Test
    fun `revokeApiKey should be idempotent`() {
        // Given
        val apiKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Key to Revoke", "CI", null))

        // When - Revoke twice
        apiKeyService.revokeApiKey(testApplicationId, apiKey.id)
        apiKeyService.revokeApiKey(testApplicationId, apiKey.id)

        // Then - No exception should be thrown
        val revokedKey = apiKeyRepository.findById(apiKey.id)
        assertNotNull(revokedKey)
        assertFalse(revokedKey!!.isActive)
    }

    @Test
    fun `revokeApiKey should throw exception for non-existent API key`() {
        // Given
        val nonExistentKeyId = UUID.randomUUID()

        // When & Then
        assertThrows<ApiKeyNotFoundException> {
            apiKeyService.revokeApiKey(testApplicationId, nonExistentKeyId)
        }
    }

    @Test
    fun `loginWithApiKey should return access token for valid API key`() {
        // Given
        val apiKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Valid Key", "CI", null))
        val request = ApiKeyLoginRequestDto(apiKey = apiKey.apiKey!!)

        // When
        val result = authService.loginWithApiKey(request)

        // Then
        assertNotNull(result)
        assertNotNull(result.accessToken)
        assertTrue(result.accessToken.isNotBlank())
    }

    @Test
    fun `loginWithApiKey should update lastUsedAt timestamp`() {
        // Given
        val apiKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Valid Key", "CI", null))
        val request = ApiKeyLoginRequestDto(apiKey = apiKey.apiKey!!)

        val beforeLogin = Instant.now()

        // When
        authService.loginWithApiKey(request)

        // Wait a bit to ensure different timestamp
        Thread.sleep(100)

        // Then
        val updatedKey = apiKeyRepository.findById(apiKey.id)
        assertNotNull(updatedKey?.lastUsedAt)
        assertTrue(updatedKey!!.lastUsedAt!! >= beforeLogin.toEpochMilli())
    }

    @Test
    fun `loginWithApiKey should throw exception for invalid API key`() {
        // Given
        val request = ApiKeyLoginRequestDto(apiKey = "invalid-api-key-value")

        // When & Then
        assertThrows<InvalidApiKeyException> {
            authService.loginWithApiKey(request)
        }
    }

    @Test
    fun `loginWithApiKey should throw exception for revoked API key`() {
        // Given
        val apiKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Revoked Key", "CI", null))
        apiKeyService.revokeApiKey(testApplicationId, apiKey.id)

        val request = ApiKeyLoginRequestDto(apiKey = apiKey.apiKey!!)

        // When & Then
        assertThrows<ApiKeyRevokedException> {
            authService.loginWithApiKey(request)
        }
    }

    @Test
    fun `loginWithApiKey should throw exception for expired API key`() {
        // Given - Create an API key that expires in 1 millisecond
        val apiKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Expiring Key", "CI", 1))

        // Wait for it to expire
        Thread.sleep(100)

        val request = ApiKeyLoginRequestDto(apiKey = apiKey.apiKey!!)

        // When & Then
        assertThrows<ApiKeyExpiredException> {
            authService.loginWithApiKey(request)
        }
    }

    @Test
    fun `API key hash should be deterministic`() {
        // Given
        val rawKey = "test-api-key-123"

        // When
        val hash1 = hashingUtil.hashApiKey(rawKey)
        val hash2 = hashingUtil.hashApiKey(rawKey)

        // Then
        assertEquals(hash1, hash2)
    }

    @Test
    fun `API key generation should produce unique keys`() {
        // Given & When
        val key1 = hashingUtil.generateApiKey()
        val key2 = hashingUtil.generateApiKey()
        val key3 = hashingUtil.generateApiKey()

        // Then
        assertNotEquals(key1, key2)
        assertNotEquals(key2, key3)
        assertNotEquals(key1, key3)
    }

    @Test
    fun `API key should support both CI and CONSUMER roles`() {
        // Given & When
        val ciKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("CI Key", "CI", null))
        val consumerKey = apiKeyService.createApiKey(testApplicationId, CreateApiKeyRequestDto("Consumer Key", "CONSUMER", null))

        // Then
        assertEquals("CI", ciKey.role)
        assertEquals("CONSUMER", consumerKey.role)

        // Verify authentication works for both
        val ciToken = authService.loginWithApiKey(ApiKeyLoginRequestDto(ciKey.apiKey!!))
        val consumerToken = authService.loginWithApiKey(ApiKeyLoginRequestDto(consumerKey.apiKey!!))

        assertNotNull(ciToken.accessToken)
        assertNotNull(consumerToken.accessToken)
    }
}
