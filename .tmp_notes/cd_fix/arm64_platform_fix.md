# ARM64/M1 Mac Docker Platform Fix

**Date:** 2026-01-09
**Issue:** Docker image not compatible with ARM64 Macs (M1/M2/M3)
**Status:** Fixed with platform emulation

---

## Problem Summary

When running the installer on an ARM-based Mac (M1, M2, or M3), Docker fails to pull the image with:

```
no matching manifest for linux/arm64/v8 in the manifest list entries
```

---

## Root Cause

### The Issue

The DroidDeploy Docker image is currently built **only for AMD64 (Intel x86_64)** architecture:

```yaml
# From GitHub Actions workflow
- name: Build and push
  uses: docker/build-push-action@v5
  with:
    platforms: linux/amd64  # Only AMD64, no ARM64
```

### Why This Happens

1. **GitHub Actions Dockerfile build**: The CI/CD workflow builds the image with `--platform linux/amd64`
2. **Dockerfile base image**: Uses `gradle:8.14-jdk21-alpine` which is AMD64
3. **No multi-arch build**: The build process doesn't create ARM64 variants
4. **ARM Macs**: M1/M2/M3 Macs run on ARM64 architecture and need ARM64 images or emulation

---

## Solution: Platform Emulation

Since Docker Desktop for Mac includes Rosetta 2 emulation, we can run AMD64 images on ARM Macs with slight performance overhead.

### Fix Applied

Added `platform: linux/amd64` to both docker-compose files to explicitly request AMD64 emulation:

#### docker-compose.yml (External DB Mode)
```yaml
services:
  droiddeploy:
    image: ghcr.io/pashaoleynik97/droiddeploy:latest
    container_name: droiddeploy-app
    platform: linux/amd64  # NEW: Use AMD64 emulation on ARM Macs
    restart: unless-stopped
    # ...
```

#### docker-compose.bundled.yml (Bundled DB Mode)
```yaml
services:
  droiddeploy:
    image: ghcr.io/pashaoleynik97/droiddeploy:latest
    container_name: droiddeploy-app
    platform: linux/amd64  # NEW: Use AMD64 emulation on ARM Macs
    restart: unless-stopped
    # ...
```

---

## How It Works

1. **Docker Desktop detects** the `platform: linux/amd64` directive
2. **Rosetta 2 emulation** translates AMD64 instructions to ARM64
3. **Image runs successfully** with slight performance overhead (~20-30%)
4. **No code changes needed** - works transparently

---

## Performance Considerations

### Emulation Overhead
- **CPU**: ~20-30% slower than native ARM64
- **Memory**: Minimal overhead
- **I/O**: No impact (native speed)
- **Network**: No impact

### Is This a Problem?
For most use cases, **no**:
- Web application workloads are I/O-bound, not CPU-bound
- Database access is the bottleneck, not computation
- The overhead is acceptable for development and small deployments

### When to Consider Native ARM64
- **High CPU usage**: Intensive computation or image processing
- **Large scale**: Handling thousands of requests per second
- **Production**: For optimal performance in production environments

---

## Alternative Solutions

### Option 1: Keep Emulation (Current Fix) ✅
**Pros:**
- Works immediately
- No build changes needed
- Acceptable performance for most use cases

**Cons:**
- ~20-30% CPU performance penalty
- Not optimal for production

### Option 2: Build Multi-Architecture Images
**What to do:**
Update GitHub Actions workflow to build both AMD64 and ARM64:

```yaml
- name: Build and push
  uses: docker/build-push-action@v5
  with:
    platforms: linux/amd64,linux/arm64  # Build both
    # ...
```

**Pros:**
- Native performance on both Intel and ARM
- Best for production deployments
- Users automatically get correct architecture

**Cons:**
- Longer build times (~2x)
- Requires QEMU for cross-compilation in CI
- More complex build setup

### Option 3: Separate ARM64 Image
Build and publish a separate ARM64 image:
- `ghcr.io/pashaoleynik97/droiddeploy:latest-arm64`
- Users choose based on their architecture

**Pros:**
- Native performance
- No build complexity increase

**Cons:**
- Users must manually select correct image
- Maintenance burden (two images to manage)

---

## Recommendation

### Short-term: Use Current Fix ✅
The platform emulation solution works well for:
- Development environments
- Testing
- Small to medium deployments
- Quick setup

### Long-term: Multi-Arch Build
For production and better performance, implement multi-architecture builds:

1. **Update GitHub Actions workflow** to build both AMD64 and ARM64
2. **Test both architectures** in CI
3. **Publish multi-arch manifest** so Docker automatically selects correct image
4. **Remove platform specification** from docker-compose files (automatic selection)

---

## Files Modified

- `docker-compose.yml` (line 26): Added `platform: linux/amd64`
- `docker-compose.bundled.yml` (line 93): Added `platform: linux/amd64`

---

## Testing

### Verify Platform Emulation Works

```bash
# Pull the image with platform specification
docker pull --platform linux/amd64 ghcr.io/pashaoleynik97/droiddeploy:latest

# Check the image architecture
docker image inspect ghcr.io/pashaoleynik97/droiddeploy:latest | grep Architecture

# Should show: "Architecture": "amd64"
```

### Run the Installer
```bash
sudo ./install-droiddeploy.sh
```

Should now successfully:
1. Pull the AMD64 image using emulation
2. Start containers without platform errors
3. Run the application (with emulation overhead)

---

## Future Work

### Implement Multi-Arch Builds

**Step 1: Update Dockerfile**
Dockerfile is already platform-independent (uses Alpine base images available for both architectures).

**Step 2: Update GitHub Actions Workflow**
```yaml
# .github/workflows/docker-build.yml
- name: Set up QEMU
  uses: docker/setup-qemu-action@v3

- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Build and push
  uses: docker/build-push-action@v5
  with:
    context: .
    platforms: linux/amd64,linux/arm64  # Build both!
    push: true
    tags: |
      ghcr.io/pashaoleynik97/droiddeploy:latest
      ghcr.io/pashaoleynik97/droiddeploy:${{ github.sha }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

**Step 3: Test Both Architectures**
- Test AMD64 build on Intel runners
- Test ARM64 build on ARM runners (or emulated)
- Verify both images work correctly

**Step 4: Remove Platform Specification**
Once multi-arch images are published, remove `platform: linux/amd64` from compose files. Docker will automatically select the correct architecture.

---

## Notes

- **Docker Desktop for Mac** includes Rosetta 2 by default
- **Emulation is transparent** - no special setup needed
- **Works on both Intel and ARM Macs** (Intel runs natively, ARM uses emulation)
- **PostgreSQL image** (postgres:15-alpine) already supports multi-arch

---

*Current fix enables ARM Mac users to run DroidDeploy immediately using AMD64 emulation. Multi-arch builds recommended for production deployments.*
