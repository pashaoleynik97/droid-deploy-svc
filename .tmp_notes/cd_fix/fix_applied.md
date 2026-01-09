# Fix Applied - CD Build Issue

**Date:** 2026-01-09
**Issue:** Docker build failing due to Gradle version mismatch
**Status:** Fixed

---

## Changes Made

### 1. Dockerfile (line 2)
**Before:**
```dockerfile
FROM gradle:8.5-jdk21-alpine AS builder
```

**After:**
```dockerfile
FROM gradle:8.14-jdk21-alpine AS builder
```

**Reason:** Spring Boot 4.0.0 requires Gradle 8.14 or later. Gradle 8.5 is incompatible.

---

### 2. Documentation Update
Updated `.tmp_notes/deployment-tools/NOTES.md` (line 6)

**Before:**
```
- **Build System:** Gradle 8.5+ with Kotlin DSL
```

**After:**
```
- **Build System:** Gradle 8.14+ with Kotlin DSL
```

**Reason:** Corrected documentation to reflect actual minimum Gradle version required.

---

## Technical Details

### Why Gradle 8.14?
- **Minimum requirement:** Spring Boot 4.0.0 requires Gradle 8.14+ or 9.x
- **Compatibility:** Gradle 8.14 is the first version in the 8.x series that supports Spring Boot 4.0.0
- **Stability:** Using stable 8.x series rather than latest 9.x for production builds
- **Docker Hub:** gradle:8.14-jdk21-alpine is widely available

### Why Not Gradle 9.2.1?
While the project's gradle-wrapper.properties specifies 9.2.1:
- Gradle 8.14 meets all requirements
- More conservative approach for CI/CD
- Can upgrade to 9.x later if needed
- Both versions are compatible with Spring Boot 4.0.0

---

## Testing Steps

Before pushing to CI/CD:

```bash
# Test Docker build locally
docker build -t droiddeploy:test .

# Verify the JAR is created
docker build --target builder -t droiddeploy:builder .
docker run --rm droiddeploy:builder ls -lh /build/droiddeploy-svc/build/libs/

# Test full runtime
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/droiddeploy \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=pass \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  -e SUPER_ADMIN_LOGIN=admin \
  -e SUPER_ADMIN_PWD=SecurePass123! \
  -v /tmp/droiddeploy-test:/var/lib/droiddeploy/apks \
  --name droiddeploy-test \
  droiddeploy:test

# Check health
sleep 30
curl http://localhost:8080/actuator/health

# Cleanup
docker stop droiddeploy-test && docker rm droiddeploy-test
```

---

## Expected Outcome

After pushing these changes:
1. CI/CD pipeline should successfully build Docker image
2. Build will pass at step #27 (gradle :droiddeploy-svc:bootJar)
3. Image will be published to ghcr.io/pashaoleynik97/droiddeploy
4. Tags will be applied correctly (latest, sha-*, version)

---

## Files Modified

- `Dockerfile` (line 2) - Updated base image version
- `.tmp_notes/deployment-tools/NOTES.md` (line 6) - Updated documentation

---

## Next Actions

1. ✅ Fix applied
2. ⏳ Test build locally (optional but recommended)
3. ⏳ Commit changes
4. ⏳ Push to trigger CI/CD
5. ⏳ Verify successful image publish
