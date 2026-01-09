# Docker Build Failure Analysis - CD Fix

**Date:** 2026-01-09
**Issue:** Docker build failing during publish step
**Status:** Root cause identified

---

## Error Summary

The Docker build is failing with the following error:

```
Spring Boot plugin requires Gradle 8.x (8.14 or later) or 9.x. The current version is Gradle 8.5
```

Build fails at step 27 (builder stage) when attempting to run:
```bash
RUN gradle :droiddeploy-svc:bootJar -Prevision=0.0.0-5647e3a --no-daemon
```

---

## Root Cause Analysis

### The Mismatch

There is a **version mismatch** between the Docker build environment and the project requirements:

1. **Dockerfile (line 2):** Uses base image `gradle:8.5-jdk21-alpine`
   - This provides Gradle version 8.5

2. **Project's gradle-wrapper.properties (line 3):** Specifies `gradle-9.2.1-bin.zip`
   - The project is configured to use Gradle 9.2.1

3. **build.gradle.kts (line 5):** Uses Spring Boot version `4.0.0`
   - Spring Boot 4.0.0 **requires** Gradle 8.14+ or 9.x

### Why It Fails

The Docker build does NOT use the Gradle wrapper (`./gradlew`) but instead directly invokes `gradle` command, which uses the Gradle version installed in the Docker image (8.5). This version is too old for Spring Boot 4.0.0.

The error appears twice:
- Step #22: During `gradle dependencies --no-daemon` (line 20)
- Step #27: During `gradle :droiddeploy-svc:bootJar` (line 29)

Both fail because they're using Gradle 8.5 from the Docker image.

---

## Why The Project Works Locally

When developers run `./gradlew` locally, it uses the Gradle wrapper which automatically downloads and uses Gradle 9.2.1 (as specified in gradle-wrapper.properties). This version satisfies Spring Boot 4.0.0's requirements.

The Docker build bypasses this mechanism.

---

## Impact

- **CI/CD Pipeline:** Blocked - cannot publish Docker images
- **Deployments:** Blocked - no new images can be built
- **Severity:** Critical - prevents any releases

---

## Solution Options

### Option 1: Update Dockerfile to use Gradle 9.x (Recommended)
Change line 2 in Dockerfile:
```dockerfile
FROM gradle:9.2.1-jdk21-alpine AS builder
```

**Pros:**
- Matches the project's gradle wrapper version exactly
- Aligns Docker build environment with local development
- Future-proof for Spring Boot 4.0.0+

**Cons:**
- Need to verify gradle:9.2.1-jdk21-alpine image exists on Docker Hub

### Option 2: Use Gradle wrapper in Dockerfile
Keep the base image but use `./gradlew` instead of `gradle`:
```dockerfile
FROM gradle:8.5-jdk21-alpine AS builder  # Or any version
...
RUN ./gradlew dependencies --no-daemon || true
...
RUN ./gradlew :droiddeploy-svc:bootJar -Prevision=${VERSION} --no-daemon
```

**Pros:**
- Uses the same Gradle version as local development (9.2.1)
- Base image version becomes less critical
- Wrapper will download the correct Gradle version

**Cons:**
- First build will download Gradle (but can be cached)
- Slightly slower initial build

### Option 3: Use minimum required Gradle version
Change to use Gradle 8.14+:
```dockerfile
FROM gradle:8.14-jdk21-alpine AS builder
```

**Pros:**
- Meets minimum requirements
- Potentially more stable than latest version

**Cons:**
- Still doesn't match wrapper version
- May need updates when Spring Boot requirements change

---

## Recommended Fix

**Option 1** is recommended: Update Dockerfile to use `gradle:9.2.1-jdk21-alpine`

This provides:
- Exact version match with project configuration
- Best compatibility with Spring Boot 4.0.0
- Consistency across all build environments

---

## Additional Observations

### Documentation Issue
The deployment-tools/NOTES.md (line 6) mentions "Gradle 8.5+" but this is incorrect:
- Should be "Gradle 8.14+" or "Gradle 9.x" to accurately reflect Spring Boot 4.0.0 requirements

### Testing Recommendations
After fix:
1. Test Docker build locally: `docker build -t droiddeploy:test .`
2. Verify JAR is created: Check builder stage output
3. Test runtime: Run container with proper environment variables
4. Verify health endpoint responds

---

## Files Involved

- `Dockerfile` (line 2) - Base image version
- `gradle/wrapper/gradle-wrapper.properties` (line 3) - Wrapper version
- `build.gradle.kts` (line 5) - Spring Boot version
- `.tmp_notes/deployment-tools/NOTES.md` (line 6) - Documentation

---

## Next Steps

1. Update Dockerfile with correct Gradle version
2. Update documentation in NOTES.md
3. Test build locally
4. Push changes to trigger CI/CD
5. Verify successful image publish
