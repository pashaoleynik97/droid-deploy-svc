# Context

Project: multi-module Gradle KTS backend (Spring Boot 3)

Modules:
- droiddeploy-core
- droiddeploy-db
- droiddeploy-rest
- droiddeploy-svc

Key structure (already exists):

- droiddeploy-core
    - src/main/kotlin/com/pashaoleynik97/droiddeploy/core
        - config/StorageProperties.kt
        - domain/Application.kt
        - domain/ApplicationVersion.kt
        - dto/application/ApplicationResponseDto.kt
        - dto/application/VersionDto.kt
        - exception/*
            - ApplicationNotFoundException.kt
            - ApkNotFoundException.kt
            - ApkStorageException.kt
            - DroidDeployException.kt
            - ...
        - repository/ApplicationRepository.kt
        - service/ApplicationService.kt
        - storage/
            - ApkPathResolver.kt
            - ApkStorage.kt
        - utils/CredentialsValidator.kt

- droiddeploy-db
    - src/main/kotlin/com/pashaoleynik97/droiddeploy/db
        - entity/ApplicationEntity.kt
        - entity/ApplicationVersionEntity.kt
        - repository/ApplicationRepositoryImpl.kt
        - repository/JpaApplicationRepository.kt
        - repository/JpaApplicationVersionRepository.kt

- droiddeploy-rest
    - src/main/kotlin/com/pashaoleynik97/droiddeploy/rest
        - controller/ApplicationController.kt
        - handler/GlobalExceptionHandler.kt
        - model/wrapper/RestResponse.kt
        - model/wrapper/PagedResponse.kt
        - security/JwtAuthentication.kt

- droiddeploy-svc
    - src/main/kotlin/com/pashaoleynik97/droiddeploy
        - DroiddeployApplication.kt
        - config/SecurityConfig.kt
        - security/JwtAuthenticationFilter.kt
        - security/JwtTokenProvider.kt
        - service/application/ApplicationServiceImpl.kt
        - service/storage/LocalFileSystemApkStorage.kt
        - service/auth/AuthServiceImpl.kt
        - service/user/UserServiceImpl.kt
    - src/test/kotlin/com/pashaoleynik97/droiddeploy
        - AbstractIntegrationTest.kt
        - integration/ApplicationControllerIntegrationTest.kt
        - integration/ApplicationListIntegrationTest.kt
        - integration/ApplicationGetByIdIntegrationTest.kt
        - integration/User*IntegrationTest.kt
        - security/JwtTokenProviderTest.kt
        - service/auth/AuthServiceImplTest.kt
        - service/user/UserServiceImplTest.kt

Flyway migrations (for understanding DB schema):
- droiddeploy-svc/src/main/resources/db/migration/V1__initial_schema.sql
- droiddeploy-svc/src/main/resources/db/migration/V2__application_extend_and_versions.sql

There is already a storage abstraction:
- ApkPathResolver + ApkStorage in droiddeploy-core/storage
- LocalFileSystemApkStorage in droiddeploy-svc/service/storage implements ApkStorage
- Exceptions: ApkNotFoundException, ApkStorageException already exist.

ROLES & SECURITY:
- Role model is in core.domain.UserRole.
- JWT/security config already exists and is used in integration tests.
- Only ADMIN and CI should be allowed to upload new versions.
- Other roles (e.g., CONSUMER) must be denied.

EXISTING REST PATTERN:
- Controllers use `RestResponse<T>` as a wrapper.
- ApplicationController already exposes CRUD + list endpoints under `/api/v1/application`.
- Tests for controllers live in droiddeploy-svc/src/test/.../integration and extend AbstractIntegrationTest.
- Application list and getById tests show how paths, auth, and wrappers are used.

# TASK: Implement “Upload new application version” flow

New endpoint:

- Controller: new `ApplicationVersionController` in droiddeploy-rest
- Path: `POST /api/v1/application/{applicationId}/version`
    - NOTE: base path is `/application` (NOT `/applications`).
- Consumes: `multipart/form-data`
- Request:
    - Path variable: `applicationId: Long`
    - Body: single part `file` with APK binary (`MultipartFile`).
- Response:
    - On success: HTTP 201 Created
    - Body: `RestResponse<VersionDto>` containing the created version’s data.

Business rules (core of the feature):

1. Authentication & Authorization
    - Endpoint must require JWT auth.
    - Only users with roles ADMIN or CI are allowed.
    - Other roles must be rejected (consistent with existing security behaviour/patterns).
    - Reuse existing security wiring from ApplicationController and integration tests.

2. APK processing
    - From the uploaded APK file:
        - Extract `versionCode` (Int/Long, monotonic)
        - Extract `versionName` (String)
        - Extract signing certificate and derive `signing_certificate_sha256` fingerprint (as hex string, upper- or lower-case consistently).
    - This extraction logic should live in svc module (e.g. dedicated component like `ApkMetadataExtractor`), NOT in the rest or db modules.
    - For now, it is OK to:
        - Either introduce a real implementation using a suitable APK parser library, OR
        - Create a placeholder `ApkMetadataExtractor` interface and a basic implementation with TODOs.
    - In tests you will mock the extractor; do not rely on real APK parsing for unit/integration tests.

3. Signing certificate consistency
    - Application table already has a `signing_certificate_sha256` column (string, nullable initially).
    - For the given `applicationId`:
        - Load Application aggregate (via ApplicationRepository).
        - If not found → throw `ApplicationNotFoundException`.
        - If `application.signingCertificateSha256` is null:
            - Set it to the fingerprint from the uploaded APK and persist the application.
        - If `application.signingCertificateSha256` is NOT null:
            - Compare stored fingerprint with the one extracted from APK.
            - If they differ → throw a dedicated domain exception, e.g. `SigningCertificateMismatchException` (extends `DroidDeployException`).
                - This should be mapped by GlobalExceptionHandler to a 400 Bad Request or other appropriate status (see existing patterns).

4. VersionCode rules
    - Use `application_version` table in DB (and its mapping ApplicationVersionEntity, ApplicationVersion domain).
    - For the given application:
        - Find max existing versionCode (e.g., via JpaApplicationVersionRepository or abstraction over it).
    - Rules:
        - If there is already a record with the same versionCode → throw a dedicated exception, e.g. `ApplicationVersionAlreadyExistsException` (extends `DroidDeployException`).
            - Map to HTTP 409 CONFLICT in GlobalExceptionHandler (consistent with e.g. BundleIdAlreadyExistsException).
        - Enforce that `newVersionCode > currentMaxVersionCode` (if any versions exist).
            - If `newVersionCode <= maxExistingVersionCode` → throw `InvalidVersionCodeException` (extends `DroidDeployException`), map to HTTP 400 BAD_REQUEST.

5. Creating new ApplicationVersion
    - If all checks pass (application exists, certificate matches, versionCode is higher and unique):
        - Create new `ApplicationVersion` domain object with:
            - `versionCode` (from APK)
            - `versionName` (from APK)
            - `stable = false` (by default, newly uploaded versions are not stable until explicitly promoted)
            - Timestamps as required by the domain (createdAt, etc.) using existing patterns.
        - Persist it using repository abstractions in core/db.
        - No file path column: path is derived purely by convention.

6. APK file storage
    - Use the existing `ApkStorage` + `ApkPathResolver` abstractions in droiddeploy-core/storage and their implementation in `LocalFileSystemApkStorage` (svc).
    - Required path convention:
        - All APKs MUST be stored using the standard pattern:
          `<storageRoot>/app/<application_id>/ver/<version_code>/base.apk`
        - `storageRoot` is defined by `StorageProperties` (and configured via application.yaml/application-test.yaml).
    - When persisting a new version:
        - First decide on the exact path (use ApkPathResolver).
        - Save the uploaded file via ApkStorage (e.g. `saveApk(applicationId, versionCode, inputStream)` or similar).
        - If storage operation fails → throw `ApkStorageException`.

7. Transaction / Compensation behaviour
    - The upload flow must be transactional from the point of view of DB + filesystem as best as possible:
        - Preferred approach:
            - Use a Spring @Transactional method in ApplicationServiceImpl that:
                - Validates & updates application signingCertificateSha256.
                - Enforces version rules.
                - Saves ApplicationVersion in DB.
            - Call `ApkStorage.saveApk(...)` within the same service method.
        - Compensation:
            - If the DB persist fails AFTER the file has been written:
                - Attempt to delete the file (e.g. `ApkStorage.deleteApk(applicationId, versionCode)`).
                - If deletion fails, log a warning, but do not mask the original DB error.
    - Keep in mind that true ACID across filesystem and DB is not possible; just implement best-effort compensation.

8. DTOs & mapping
    - Use existing `VersionDto` in droiddeploy-core/dto/application as response model.
    - Implement / extend appropriate mapping logic between:
        - ApplicationVersionEntity ↔ ApplicationVersion domain
        - ApplicationVersion domain ↔ VersionDto
    - Follow existing patterns used for Application ↔ ApplicationEntity ↔ ApplicationResponseDto.

9. REST controller details
    - Create a new `ApplicationVersionController` in droiddeploy-rest/controller.
    - Base mapping: `@RequestMapping("/api/v1/application")`
    - Endpoint method:
        - `@PostMapping("/{applicationId}/version", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])`
        - Parameters:
            - `@PathVariable applicationId: Long`
            - `@RequestPart("file") file: MultipartFile`
            - Authentication principal can be obtained from SecurityContext if needed, but role checks should be handled via security configuration + annotations.
        - Behaviour:
            - Delegate to a service method in ApplicationService (or dedicated service), passing `applicationId` and the file’s InputStream / bytes.
            - Wrap response in `RestResponse.success(data)` as per existing convention.
            - Return HTTP 201 Created; use `ResponseEntity.status(HttpStatus.CREATED)` if necessary.
    - Security:
        - Restrict access via `@PreAuthorize` or via SecurityConfig (e.g. `.antMatchers("/api/v1/application/*/version").hasAnyRole("ADMIN", "CI")`) consistent with the existing style.
        - Make sure integration tests use ADMIN and CI tokens and verify access is allowed, and that other roles are denied.

10. Exceptions & GlobalExceptionHandler
- Add new domain exceptions in droiddeploy-core/exception:
    - `SigningCertificateMismatchException`
    - `ApplicationVersionAlreadyExistsException`
    - `InvalidVersionCodeException`
- Ensure they extend `DroidDeployException`.
- In droiddeploy-rest/handler/GlobalExceptionHandler:
    - Map `ApplicationVersionAlreadyExistsException` to HTTP 409 CONFLICT.
    - Map `InvalidVersionCodeException` to HTTP 400 BAD_REQUEST.
    - Map `SigningCertificateMismatchException` to HTTP 400 BAD_REQUEST.
    - Reuse RestError / RestResponse wrappers in the same way as other domain exceptions (see existing mappings for BundleIdAlreadyExistsException, InvalidBundleIdException, etc.).

## CHANGES BY MODULE

1) droiddeploy-core

   - Update `ApplicationService` interface to add a method for uploading a version, something like:

     ```kotlin
     interface ApplicationService {
         // existing methods...

         fun uploadNewVersion(applicationId: Long, apkContent: ByteArray): VersionDto
     }
     ```
     or using InputStream if preferred. The important part is that this interface is Spring-agnostic and lives in core.

   - If necessary, adjust ApplicationVersion domain model to ensure required fields are present (versionCode, versionName, stable, createdAt etc.), but do NOT introduce file path fields – the path is derived by convention only.
   - Add new exceptions:
     - SigningCertificateMismatchException : DroidDeployException
     - ApplicationVersionAlreadyExistsException : DroidDeployException
     - InvalidVersionCodeException : DroidDeployException

2) droiddeploy-db 
  - Ensure ApplicationVersionEntity already supports required fields (versionCode, versionName, stable, etc.). If anything is missing, add fields + mappings, and extend Flyway migration (V3 if needed).
  - Add helper query in JpaApplicationVersionRepository:
    - find max versionCode by application id.
    - check existence by application id + versionCode.
  - Wire these queries into ApplicationRepositoryImpl or a dedicated repository implementation if appropriate.
  - Keep using existing mapping helpers between entities and domain models.

3) droiddeploy-svc – service layer

   *   Responsibilities inside this method:
       *   Load application by id via ApplicationRepository.
           *   If not found → throw ApplicationNotFoundException.
       *   Use ApkMetadataExtractor (new component you will create in this module) to parse:
           *   versionCode
           *   versionName
           *   signingCertificateSha256
       *   Enforce signing certificate rules:
           *   If application.signingCertificateSha256 == null → set it and persist application.
           *   If not null and does not match → throw SigningCertificateMismatchException.
       *   Query current max versionCode for this application.
           *   If given versionCode already exists → throw ApplicationVersionAlreadyExistsException.
           *   If versionCode <= max versionCode → throw InvalidVersionCodeException.
       *   Create new ApplicationVersion domain object with stable=false and timestamps.
       *   Persist it via repository.
       *   Save APK via ApkStorage/LocalFileSystemApkStorage using convention path /app//ver//base.apk.
       *   Implement compensation: if DB persist fails after save, attempt to remove the stored file.
       *   Annotate method with @Transactional as appropriate.
   *   Introduce ApkMetadataExtractor:
       *   Define interface in svc module (e.g. service/application/ApkMetadataExtractor.kt).
       *   Implementation can be basic for now; tests will mock it.
       *   It should return a small data class with versionCode, versionName, signingCertificateSha256.

4)  droiddeploy-rest – REST controller & handler

*   Create ApplicationVersionController in droiddeploy-rest/src/main/kotlin/com/.../rest/controller:
    *   Annotate with @RestController.
    *   @RequestMapping("/api/v1/application").
    *   Inject ApplicationService.
    *   @PostMapping("/{applicationId}/version", consumes = \[MediaType.MULTIPART\_FORM\_DATA\_VALUE\])fun uploadVersion( @PathVariable applicationId: Long, @RequestPart("file") file: MultipartFile): ResponseEntity\> { val versionDto = applicationService.uploadNewVersion(applicationId, file.bytes) return ResponseEntity .status(HttpStatus.CREATED) .body(RestResponse.success(versionDto))}(Adjust minor details to match project conventions.)
*   Update security configuration (in droiddeploy-svc/config/SecurityConfig.kt) to restrict this endpoint to ADMIN and CI roles, consistent with existing style.
*   Extend GlobalExceptionHandler to handle:
    *   SigningCertificateMismatchException → 400
    *   ApplicationVersionAlreadyExistsException → 409
    *   InvalidVersionCodeException → 400

## TESTS

1.  droiddeploy-svc – unit tests

*   Use mocks for:
    *   Test successful flow:
        *   Application exists, certificate null:
            *   metadata extractor returns cert; service sets it; persists; creates version; calls ApkStorage.saveApk.
        *   Application exists, cert matches:
            *   service DOES NOT change cert; just creates version and saves APK.
    *   Test mismatched cert:
        *   application.signingCertificateSha256 != metadata.cert → throws SigningCertificateMismatchException.
    *   Test duplicate versionCode:
        *   repository reports existing version with same versionCode → ApplicationVersionAlreadyExistsException.
    *   Test non-increasing versionCode:
        *   maxVersionCode = 10, newVersionCode = 9 → InvalidVersionCodeException.
    *   Test compensation:
        *   Simulate DB persist failure after ApkStorage.saveApk is called; verify that deleteApk is attempted.
    *   ApplicationRepository
    *   ApkMetadataExtractor
    *   ApkStorage 
    *   Any other collaborators.

2.  droiddeploy-svc – integration tests


*   Create ApplicationVersionControllerIntegrationTest in droiddeploy-svc/src/test/kotlin/.../integration, extending AbstractIntegrationTest.
*   Cover scenarios:

    1.  Happy path (ADMIN):
        *   Use existing test helpers to create an application.
        *   Upload a “fake” APK (can be just some bytes; parsing is mocked or simplified).
        *   Expect:
            *   HTTP 201 Created.
            *   RestResponse with VersionDto (correct versionCode and versionName as per extractor stub).
            *   ApplicationVersion row exists in DB.
            *   Apk file exists on disk at /app//ver//base.apk (you can verify using the same path resolver or directly check filesystem in test).

    2.  Happy path (CI):

        *   Same as above, but using CI user token → should also be allowed.

    3.  Application not found:

        *   Upload version for non-existing applicationId → ApplicationNotFoundException → correct HTTP status and error payload.

    4.  Signing certificate mismatch:

        *   Pre-create application with some signing\_certificate\_sha256.
        *   Configure extractor to return a different fingerprint.
        *   Expect HTTP 400 and appropriate error type.

    5.  Duplicate versionCode:

        *   Pre-create version with versionCode X.
        *   Upload APK whose metadata also has versionCode X.
        *   Expect HTTP 409 and appropriate error type.

    6.  Non-increasing versionCode:

        *   Existing max versionCode = 5.
        *   Upload APK with versionCode 3.
        *   Expect HTTP 400 with InvalidVersionCode error.

    7.  Forbidden for non-ADMIN/CI:

        *   Use token for a non-allowed role (e.g. CONSUMER).
        *   Expect 403 (or 401, depending on existing config), with no side effects in DB/storage.
