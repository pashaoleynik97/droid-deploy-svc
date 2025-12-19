# CI/CD Implementation - Technical Notes

## Repository Analysis

### Project Structure
- **Build System**: Gradle 9.2.1 with Kotlin DSL
- **Java Version**: 21 (enforced via toolchain)
- **Kotlin Version**: 2.2.21
- **Spring Boot**: 4.0.0
- **Architecture**: Multi-module project (4 modules)

### Modules
1. **droiddeploy-core** - Domain models, interfaces
2. **droiddeploy-db** - JPA entities, repositories
3. **droiddeploy-rest** - REST controllers
4. **droiddeploy-svc** - Main executable module (Spring Boot app)

### Build Tasks
- `./gradlew build` - Full build + test
- `./gradlew test` - Run all tests
- `./gradlew check` - Run all checks
- `./gradlew :droiddeploy-svc:bootJar` - Create executable JAR

### JAR Output
- **Location**: `droiddeploy-svc/build/libs/`
- **Executable JAR**: `droiddeploy-svc-0.0.1-SNAPSHOT.jar` (fat JAR, ~69MB)
- **Plain JAR**: `droiddeploy-svc-0.0.1-SNAPSHOT-plain.jar` (thin JAR, ~74KB)

### Test Configuration
- **Framework**: JUnit 5 Platform
- **Test Libraries**:
  - Spring Boot Test
  - TestContainers (PostgreSQL)
  - Mockito Kotlin
  - Security Test

### Dockerfile
- **Exists**: Yes (created in deployment session)
- **Build Strategy**: Multi-stage build
- **Base**: gradle:8.5-jdk21-alpine (builder), eclipse-temurin:21-jre-alpine (runtime)
- **Build Command**: `gradle :droiddeploy-svc:bootJar --no-daemon`

---

## Dynamic Versioning Strategy

### Current State
Hardcoded in two places:
```kotlin
// build.gradle.kts (root)
group = "com.pashaoleynik97"
version = "0.0.1-SNAPSHOT"  // Line 10

// build.gradle.kts (subprojects)
group = "com.pashaoleynik97"
version = "0.0.1-SNAPSHOT"  // Line 18
```

### Proposed Solution

**Gradle Property Approach:**
```kotlin
// Read version from Gradle property or default to snapshot
val projectVersion = findProperty("version")?.toString() ?: "0.0.0-SNAPSHOT"

group = "com.pashaoleynik97"
version = projectVersion

subprojects {
    group = "com.pashaoleynik97"
    version = projectVersion  // Inherit from root
}
```

**Workflow Version Injection:**
```yaml
# On tag build (refs/tags/v1.2.3)
VERSION=1.2.3  # Strip 'v' prefix

# On non-tag build
VERSION=0.0.0-SNAPSHOT

# Pass to Gradle
./gradlew build -Pversion=$VERSION
```

---

## CI Pipeline Design

### Trigger
- `push` to `master` branch

### Jobs
1. **Build & Test**
   - Checkout code
   - Setup Java 21
   - Setup Gradle cache
   - Run `./gradlew build` (builds + tests all modules)
   - Upload test reports (if failed)
   - Upload JAR artifacts

### Artifacts
- JAR: `droiddeploy-svc/build/libs/*.jar`
- Test Reports: `**/build/test-results/test/*.xml`

### Version
- Use default snapshot: `0.0.0-SNAPSHOT`
- No Docker publishing

---

## Release Pipeline Design

### Triggers
1. **Manual**: `workflow_dispatch`
2. **Tag**: `push` with tag matching `v*` (e.g., v0.1.0, v1.2.3)

### Version Determination
```bash
if [[ $GITHUB_REF == refs/tags/v* ]]; then
  VERSION=${GITHUB_REF#refs/tags/v}  # Strip 'refs/tags/v'
  IS_TAG_BUILD=true
else
  VERSION=0.0.0-${GITHUB_SHA::7}  # Short commit SHA
  IS_TAG_BUILD=false
fi
```

### Docker Image Tags
**On tag build (v1.2.3):**
- `ghcr.io/pashaoleynik97/droiddeploy:1.2.3`
- `ghcr.io/pashaoleynik97/droiddeploy:latest`

**On manual build (no tag):**
- `ghcr.io/pashaoleynik97/droiddeploy:sha-abcdef1`
- `ghcr.io/pashaoleynik97/droiddeploy:manual-{run_number}`

### Jobs
1. **Build & Test**
   - Checkout code
   - Determine version
   - Setup Java 21
   - Setup Gradle cache
   - Run `./gradlew build -Pversion=$VERSION`
   - Upload JAR artifacts

2. **Publish Docker Image**
   - Checkout code
   - Setup Docker Buildx
   - Login to GHCR
   - Build & push image with appropriate tags
   - Add OCI labels (source, revision, version)

3. **Create GitHub Release** (only on tag builds)
   - Create release with tag name
   - Attach JAR as release asset
   - Generate changelog (optional)

### Permissions Required
```yaml
permissions:
  contents: write    # For creating releases
  packages: write    # For publishing to GHCR
```

---

## Docker Image Configuration

### Registry
- **GHCR**: `ghcr.io`
- **Repository**: `ghcr.io/pashaoleynik97/droiddeploy`

### Authentication
- Use `GITHUB_TOKEN` (automatically available in GitHub Actions)
- Login: `docker/login-action@v3`

### Build Action
- `docker/build-push-action@v5`
- Context: `.` (repository root)
- File: `./Dockerfile`
- Platforms: `linux/amd64` (can add arm64 if needed)

### OCI Labels
```dockerfile
org.opencontainers.image.source=https://github.com/pashaoleynik97/droid-deploy-svc
org.opencontainers.image.revision=${{ github.sha }}
org.opencontainers.image.version=${{ env.VERSION }}
org.opencontainers.image.created=${{ timestamp }}
```

### Build Args
Pass version to Docker build:
```yaml
build-args: |
  VERSION=${{ env.VERSION }}
```

Dockerfile can use it:
```dockerfile
ARG VERSION=0.0.0-SNAPSHOT
RUN gradle :droiddeploy-svc:bootJar -Pversion=$VERSION --no-daemon
```

---

## Gradle Cache Strategy

Use official Gradle caching:
```yaml
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v3
  with:
    cache-read-only: ${{ github.ref != 'refs/heads/master' }}
```

This caches:
- Gradle wrapper
- Dependencies
- Build cache

---

## Test Reporting

### On Success
- Tests pass, no special action needed

### On Failure
- Upload test results as artifacts
- JUnit XML reports for analysis

```yaml
- name: Upload Test Results
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: |
      **/build/test-results/test/**/*.xml
      **/build/reports/tests/test/**
```

---

## JAR Publishing Strategy

### Workflow Artifacts (Always)
Upload JAR in both CI and Release pipelines:
```yaml
- name: Upload JAR
  uses: actions/upload-artifact@v4
  with:
    name: droiddeploy-jar
    path: droiddeploy-svc/build/libs/*-SNAPSHOT.jar
    if-no-files-found: error
```

### GitHub Release (Tag builds only)
Attach JAR to release:
```yaml
- name: Create Release
  if: startsWith(github.ref, 'refs/tags/v')
  uses: softprops/action-gh-release@v1
  with:
    files: droiddeploy-svc/build/libs/*.jar
    generate_release_notes: true
```

---

## Files to Create/Modify

### Modify
1. `build.gradle.kts` - Dynamic versioning (lines 10, 18)

### Create
2. `.github/workflows/ci.yml` - CI pipeline
3. `.github/workflows/release.yml` - Release pipeline

---

## Testing Checklist

### Local Testing
- [ ] Verify version injection: `./gradlew build -Pversion=1.2.3`
- [ ] Check JAR name includes version
- [ ] Run tests: `./gradlew test`

### CI Pipeline Testing
- [ ] Push to master triggers CI
- [ ] Tests run successfully
- [ ] JAR uploaded as artifact
- [ ] No Docker publishing occurs

### Release Pipeline Testing
- [ ] Manual trigger works
- [ ] Tag trigger works (git tag v0.1.0 && git push origin v0.1.0)
- [ ] Docker image published to GHCR
- [ ] JAR uploaded as artifact
- [ ] GitHub Release created on tag
- [ ] Version appears correctly in JAR name and Docker tags

---

## Manual Release Process

### Creating a Tag Release
```bash
# Create annotated tag
git tag -a v0.1.0 -m "Release version 0.1.0"

# Push tag to trigger release
git push origin v0.1.0

# Or push all tags
git push --tags
```

### Manual Workflow Dispatch
1. Go to GitHub Actions tab
2. Select "Release" workflow
3. Click "Run workflow"
4. Select branch (usually master)
5. Click "Run workflow" button

This will:
- Build with version `0.0.0-{sha}`
- Publish Docker image with `sha-{sha}` and `manual-{run_number}` tags
- Upload JAR as artifact
- NOT create a GitHub Release (only tags do that)

---

## Security Considerations

### Secrets
- `GITHUB_TOKEN` - Automatically provided, has permissions for packages and contents
- No manual secrets needed

### Permissions
- Minimal required permissions granted per job
- Write access only where needed (packages, contents)

### Docker Image Security
- Non-root user (UID 1001) in Dockerfile
- Minimal base image (Alpine)
- Multi-stage build (no build tools in final image)

---

*Last Updated: 2025-12-19*
