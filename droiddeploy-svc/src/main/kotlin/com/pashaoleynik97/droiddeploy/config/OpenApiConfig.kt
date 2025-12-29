package com.pashaoleynik97.droiddeploy.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${spring.application.version:0.0.0-SNAPSHOT}")
    private lateinit var appVersion: String

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("DroidDeploy REST API")
                    .description("""
                        DroidDeploy is a self-hosted Android APK distribution platform.

                        ## Authentication
                        This API supports two authentication methods:
                        - **JWT Bearer Token**: For user authentication (obtained from /api/v1/auth/login or /api/v1/auth/refresh endpoints)
                        - **API Key**: For CI/CD and consumer application integration (obtained from /api/v1/auth/apikey endpoint)

                        ## User Roles
                        - **ADMIN**: Full system access - can manage applications, versions, users, and API keys
                        - **CI**: Can upload application versions and update stability flags
                        - **CONSUMER**: Can download APKs and view latest versions

                        ## Response Format
                        All endpoints (except binary downloads) return responses wrapped in a standard format:
                        ```json
                        {
                          "data": {...},
                          "message": "Success message",
                          "errors": [],
                          "success": true
                        }
                        ```

                        Paginated endpoints return:
                        ```json
                        {
                          "content": [...],
                          "page": 0,
                          "size": 20,
                          "totalElements": 100,
                          "totalPages": 5
                        }
                        ```

                        ## Error Handling
                        Error responses include detailed error information:
                        - **VALIDATION**: Input validation failures
                        - **AUTHENTICATION**: Invalid or expired credentials
                        - **AUTHORIZATION**: Insufficient permissions
                        - **NOT_FOUND**: Resource not found
                        - **CONFLICT**: Resource conflict (e.g., duplicate bundle ID)
                        - **INTERNAL_ERROR**: Server-side errors
                    """.trimIndent())
                    .version(appVersion)
                    .license(License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Local development server")
            )
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT access token obtained from /api/v1/auth/login or /api/v1/auth/refresh endpoints")
                    )
                    .addSecuritySchemes(
                        "apiKeyAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("API-KEY")
                            .description("API key token obtained from /api/v1/auth/apikey endpoint using application-specific API key credentials")
                    )
            )
    }
}
