# DroidDeploy OpenAPI Documentation - Technical Notes

## Library Information

**SpringDoc OpenAPI Starter WebMVC UI v2.7.0**
- Official OpenAPI 3.0 implementation for Spring Boot 3+/4.0
- Automatic spec generation from Spring annotations
- Bundled Swagger UI for interactive documentation
- Full Kotlin support

## Project Structure

```
droiddeploy/
├── droiddeploy-core/          # Domain models, DTOs, interfaces
├── droiddeploy-db/            # JPA entities, repositories
├── droiddeploy-rest/          # REST controllers (ANNOTATIONS HERE)
└── droiddeploy-svc/           # Spring Boot application (CONFIG HERE)
```

## OpenAPI Configuration

### Configuration Class
**Location:** `droiddeploy-svc/src/main/kotlin/com/pashaoleynik97/droiddeploy/config/OpenApiConfig.kt`

**Key Components:**
- `@Configuration` - Spring configuration class
- `@Bean fun openAPI(): OpenAPI` - OpenAPI bean definition
- API Info: title, description, version, license
- Servers: localhost:8080 (development)
- Security Schemes: bearerAuth (JWT), apiKeyAuth (API Key)

### Security Schemes

#### 1. bearerAuth (JWT Bearer Token)
```kotlin
SecurityScheme()
    .type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("JWT")
    .description("JWT access token from /auth/login or /auth/refresh")
```

**Usage:**
- User authentication (username/password → JWT tokens)
- Roles: ADMIN, CI, CONSUMER
- Access token validity: 15 minutes (default)
- Refresh token validity: 30 days (default)

#### 2. apiKeyAuth (API Key Token)
```kotlin
SecurityScheme()
    .type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("API-KEY")
    .description("API key token from /auth/apikey")
```

**Usage:**
- CI/CD pipelines and consumer applications
- Roles: CI, CONSUMER only
- No refresh mechanism (use apiKeyAuth again for new token)

## Annotation Reference

### Class-Level Annotations

**@Tag**
```kotlin
@Tag(
    name = "Authentication",
    description = "User and API key authentication endpoints"
)
```
Groups endpoints in Swagger UI, provides category description.

**@SecurityRequirement**
```kotlin
@SecurityRequirement(name = "bearerAuth")  // Class level: applies to all endpoints
```
Declares required security scheme. Can be at class or method level.

### Method-Level Annotations

**@Operation**
```kotlin
@Operation(
    summary = "User login",  // Short summary (shown in endpoint list)
    description = "Detailed description with context, examples, and notes"
)
```

**@ApiResponses**
```kotlin
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "Success message"
            // Note: No explicit schema - SpringDoc infers from method return type
        ),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    ]
)
```

**@Parameter**
```kotlin
@Parameter(
    description = "Page number (0-indexed)",
    example = "0",
    schema = Schema(allowableValues = ["ADMIN", "CI", "CONSUMER"])  // For enums
)
```

**@io.swagger.v3.oas.annotations.parameters.RequestBody**
```kotlin
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "Login credentials",
    required = true
)
```

### Property-Level Annotations

**@Schema (on class)**
```kotlin
@Schema(
    description = "Standard response wrapper",
    example = """{"data": {...}, "message": "Success"}"""
)
data class RestResponse<T>(...)
```

**@Schema (on property)**
```kotlin
@Schema(
    description = "Response payload data",
    nullable = true,
    example = "..."
)
val data: T?
```

**@get:Schema (on computed property)**
```kotlin
@get:Schema(description = "Computed success flag")
val success: Boolean get() = errors.isEmpty()
```

## Endpoint Documentation Patterns

### Pattern 1: Public Endpoints (No Security)
```kotlin
@Tag(name = "Authentication", description = "...")
class AuthController {
    @Operation(summary = "User login", description = "...")
    @ApiResponses(...)
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequestDto)
}
```
No @SecurityRequirement needed.

### Pattern 2: Uniform Security (All Endpoints Same)
```kotlin
@Tag(name = "Applications", description = "...")
@SecurityRequirement(name = "bearerAuth")  // Class level
class ApplicationController {
    @Operation(...)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun list()
}
```
All endpoints require JWT bearer token.

### Pattern 3: Mixed Security (Different Per Endpoint)
```kotlin
@Tag(name = "Versions", description = "...")
class ApplicationVersionController {
    @SecurityRequirement(name = "bearerAuth")  // Method level
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun list()

    @SecurityRequirement(name = "bearerAuth")  // Both JWT and API Key accepted
    @SecurityRequirement(name = "apiKeyAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONSUMER')")
    @GetMapping("/latest")
    fun getLatest()
}
```

### Pattern 4: Multipart File Upload
```kotlin
@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
fun uploadVersion(
    @Parameter(
        description = "APK file",
        content = [Content(mediaType = "multipart/form-data")]
    )
    @RequestPart("file") file: MultipartFile
)
```

### Pattern 5: Binary Download
```kotlin
@ApiResponse(
    responseCode = "200",
    content = [Content(
        mediaType = "application/vnd.android.package-archive",
        schema = Schema(type = "string", format = "binary")
    )]
)
fun downloadApk(): ResponseEntity<InputStreamResource>
```

## Response Structure Documentation

### Standard Wrapper (All JSON Endpoints)
```json
{
  "data": <T>,           // Payload (null on error)
  "message": "string",   // Human-readable message
  "errors": [],          // List of RestError objects
  "success": true        // Computed: errors.isEmpty()
}
```

### Error Object
```json
{
  "type": "VALIDATION",  // ErrorType enum
  "message": "string",   // Error description
  "field": "email"       // Optional field name
}
```

### Paginated Wrapper (List Endpoints)
```json
{
  "content": [],         // List of items
  "page": 0,             // Current page (0-indexed)
  "size": 20,            // Items per page
  "totalElements": 100,  // Total items across all pages
  "totalPages": 5        // Total number of pages
}
```

## Role-Based Access Control

### User Roles
- **ADMIN**: Full system access (all endpoints)
- **CI**: Upload versions, update stability flags
- **CONSUMER**: Download APKs, view latest versions

### Authorization Patterns

**Simple Role Check:**
```kotlin
@PreAuthorize("hasRole('ADMIN')")
```

**Multiple Roles (Any):**
```kotlin
@PreAuthorize("hasAnyRole('ADMIN', 'CI')")
```

**Authenticated (Any Role):**
```kotlin
@PreAuthorize("isAuthenticated()")
// Then check in code: if (auth.userRole != ADMIN && auth.userId != targetUserId) throw Forbidden
```

## Swagger UI Configuration

### Access Points
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Features Enabled
- Try-it-out: Yes
- Operations sorting: By HTTP method
- Tags sorting: Alphabetical
- Actuator endpoints: Hidden

### Using Swagger UI

**Step 1: Obtain Token**
- Use `/api/v1/auth/login` with username/password
- Or use `/api/v1/auth/apikey` with application API key

**Step 2: Authorize**
- Click "Authorize" button (top right)
- Enter token in format: `<paste-token-here>` (without "Bearer" prefix)
- Click "Authorize" then "Close"

**Step 3: Try Endpoints**
- Click endpoint to expand
- Click "Try it out"
- Fill parameters/body
- Click "Execute"
- View response

## Common Issues & Solutions

### Issue: Computed Property Annotation Error
**Error:** `@Schema annotation is not applicable to target 'member property without backing field'`
**Solution:** Use `@get:Schema` instead of `@Schema` for computed properties.

### Issue: Swagger UI 403 Forbidden
**Problem:** SecurityConfig not permitting Swagger paths
**Solution:** Add paths to permitAll in SecurityConfig:
```kotlin
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
```

### Issue: Missing Security Schemes in UI
**Problem:** OpenApiConfig not loaded
**Solution:** Ensure OpenApiConfig is in a package scanned by Spring Boot (@Configuration in same package as other configs)

### Issue: Generic Types Not Resolved
**Problem:** RestResponse<T> showing as RestResponse<Object>
**Solution:** Use @Schema on wrapper class properties, SpringDoc will infer types from endpoints

### Issue: Response Schema Shows Only Wrapper Type (CRITICAL)
**Problem:** Documentation shows only `RestResponse` instead of full generic type like `RestResponse<ApplicationResponseDto>`
**Root Cause:** Explicitly specifying `schema = Schema(implementation = RestResponse::class)` in @ApiResponse annotations overrides SpringDoc's automatic type inference
**Symptom:** OpenAPI documentation shows the wrapper structure but doesn't reveal what's in the `data` field, making documentation useless
**Solution:** Remove all explicit schema specifications from @ApiResponse annotations:
```kotlin
// WRONG - Don't do this:
@ApiResponse(
    responseCode = "200",
    description = "Success",
    content = [Content(mediaType = "application/json", schema = Schema(implementation = RestResponse::class))]
)

// CORRECT - Do this instead:
@ApiResponse(
    responseCode = "200",
    description = "Success"
)
```
**Explanation:** SpringDoc automatically infers the complete generic type `RestResponse<ApplicationResponseDto>` from the method's return type `ResponseEntity<RestResponse<ApplicationResponseDto>>`. Explicit schema specification prevents this inference.

**When to use explicit schema:** Only for special cases like binary downloads:
```kotlin
@ApiResponse(
    responseCode = "200",
    content = [Content(
        mediaType = "application/vnd.android.package-archive",
        schema = Schema(type = "string", format = "binary")
    )]
)
```

## Testing Checklist

### Build Testing
- [ ] Gradle build succeeds
- [ ] No compilation errors
- [ ] All annotations syntax valid

### Runtime Testing
- [ ] Application starts successfully
- [ ] Swagger UI accessible at /swagger-ui.html
- [ ] OpenAPI JSON accessible at /v3/api-docs
- [ ] All 5 controller tags visible
- [ ] All 23 endpoints listed

### Functional Testing
- [ ] "Authorize" button present
- [ ] Can input JWT token
- [ ] Can input API key token
- [ ] "Try it out" works for public endpoints
- [ ] "Try it out" requires auth for protected endpoints
- [ ] Request/response schemas accurate
- [ ] Error responses documented

### Documentation Quality
- [ ] Summaries are clear and concise
- [ ] Descriptions provide sufficient context
- [ ] Security requirements are obvious
- [ ] Role requirements mentioned
- [ ] Examples are realistic
- [ ] Parameters have descriptions
- [ ] Error codes documented

## Future Enhancements

### Optional DTO Annotations
Add @Schema to DTOs in `droiddeploy-core/src/main/kotlin/com/pashaoleynik97/droiddeploy/core/dto/`:
- Auth DTOs: LoginRequestDto, TokenPairDto, etc.
- Application DTOs: CreateApplicationRequestDto, ApplicationResponseDto, etc.
- Version DTOs: VersionDto, UpdateVersionStabilityRequestDto
- User DTOs: UserResponseDto, CreateUserRequestDto, etc.
- API Key DTOs: ApiKeyDto, CreateApiKeyRequestDto

Benefits:
- More detailed field descriptions
- Examples in schema
- Validation constraint documentation
- Better IDE autocomplete in Swagger UI

### Client SDK Generation
Use OpenAPI spec to generate type-safe client libraries:
```bash
# Download spec
curl http://localhost:8080/v3/api-docs > openapi.json

# Generate Kotlin client
openapi-generator-cli generate \
  -i openapi.json \
  -g kotlin \
  -o clients/kotlin

# Generate TypeScript client
openapi-generator-cli generate \
  -i openapi.json \
  -g typescript-axios \
  -o clients/typescript
```

### API Versioning
If API versions are needed in the future:
- Add version to path: `/api/v2/auth/login`
- Update OpenAPI configuration with multiple servers
- Document deprecation timeline in descriptions
- Maintain backward compatibility

## Reference Links

- SpringDoc OpenAPI: https://springdoc.org/
- OpenAPI 3.0 Spec: https://spec.openapis.org/oas/v3.0.0
- Swagger UI: https://swagger.io/tools/swagger-ui/
- OpenAPI Generator: https://openapi-generator.tech/
