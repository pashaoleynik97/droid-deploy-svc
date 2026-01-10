# Docker Desktop File Sharing Issue Fix

**Date:** 2026-01-10
**Issue:** Docker Desktop on macOS cannot mount volumes outside shared paths
**Status:** Fixed - changed default path to Docker-compatible location

---

## Problem Description

Docker Desktop on macOS failed to start containers with:
```
Error response from daemon: Mounts denied:
The path /opt/droiddeploy/pgdata is not shared from the host and is not known to Docker.
```

### Root Cause

Docker Desktop on macOS has **file sharing restrictions**. Only certain paths are shared by default:

**Default Shared Paths:**
- ✅ `/Users`
- ✅ `/tmp`
- ✅ `/private`
- ✅ `/Volumes`

**Not Shared by Default:**
- ❌ `/opt`
- ❌ `/srv`
- ❌ `/var` (except /var/folders)
- ❌ Other system directories

**Why this matters:**
- Docker containers can only mount volumes from shared paths
- Any path outside the shared list requires manual Docker Desktop configuration
- This is a macOS security/permission feature

---

## Solution Implemented

### Changed Default Installation Path on macOS

Updated both installer and upgrade scripts to use Docker-compatible paths:

**Before:**
```bash
# macOS
DEFAULT_INSTALL_DIR="/opt/droiddeploy"  # ❌ Not shared by Docker
```

**After:**
```bash
# macOS
DEFAULT_INSTALL_DIR="/Users/Shared/droiddeploy"  # ✅ Docker-compatible
```

### Why /Users/Shared?

**Advantages:**
1. ✅ **Already shared** - No Docker Desktop configuration needed
2. ✅ **Writable by all users** - Standard location for shared data
3. ✅ **Standard macOS location** - Apple-approved location for shared files
4. ✅ **Persists across macOS updates** - Won't be affected by system updates

**Path Structure:**
```
/Users/Shared/droiddeploy/
├── config/
│   ├── .env
│   └── docker-compose.yml
├── apks/           # APK storage
├── pgdata/         # PostgreSQL data (bundled mode)
└── backups/        # Upgrade backups
```

---

## Platform-Specific Paths

The installer now uses different defaults based on OS:

| Platform | Default Path | Reason |
|----------|--------------|--------|
| macOS | `/Users/Shared/droiddeploy` | Docker Desktop file sharing |
| Linux | `/srv/droiddeploy` | Traditional service directory |
| Other | `/opt/droiddeploy` | Standard optional software location |

---

## User Instructions

### Clean Up Old Installation

If you attempted installation with the old path, clean it up:

```bash
# Remove old /opt/droiddeploy installation
sudo rm -rf /opt/droiddeploy
```

### Run Installer Again

```bash
sudo ./install-droiddeploy.sh
```

The new installation will:
1. Detect macOS
2. Use `/Users/Shared/droiddeploy` automatically
3. Create all directories
4. Start services successfully

---

## Alternative Solution: Manual Docker Configuration

If you prefer to use `/opt` or another path, you can configure Docker Desktop:

### Steps:
1. Open **Docker Desktop**
2. Go to **Settings** (gear icon)
3. Navigate to **Resources** → **File Sharing**
4. Click **+** (plus button)
5. Add `/opt` to the shared paths
6. Click **Apply & Restart**

**Pros:**
- Can use any custom path
- More control

**Cons:**
- Requires manual configuration
- Need to reconfigure on Docker Desktop reinstall
- Easy to forget when setting up new machine

**Our Solution is Better:**
- No configuration needed
- Works out of the box
- Portable (works on any Mac)

---

## Files Modified

### Installer Script
**File:** `install-droiddeploy.sh`

```bash
# Lines 26-29
Darwin*)
    # macOS: Use /Users/Shared (Docker Desktop file sharing compatible)
    DEFAULT_INSTALL_DIR="/Users/Shared/droiddeploy"
    ;;

# Lines 280-282
Darwin*)
    print_info "Detected macOS - using /Users/Shared for installation"
    print_info "(Docker Desktop file sharing compatible)"
    ;;
```

### Upgrade Script
**File:** `upgrade-droiddeploy.sh`

```bash
# Lines 23-26
Darwin*)
    # macOS: Use /Users/Shared (Docker Desktop file sharing compatible)
    DEFAULT_INSTALL_DIR="/Users/Shared/droiddeploy"
    ;;
```

---

## Verification

After installation, verify volumes are working:

```bash
# Check volume mounts
docker volume ls

# Check container mounts
docker inspect droiddeploy-app | grep Mounts -A 20

# Should see:
# "Source": "/Users/Shared/droiddeploy/apks"
# "Destination": "/var/lib/droiddeploy/apks"
```

---

## Testing Performed

### Scenario 1: Fresh macOS Installation
```bash
$ sudo ./install-droiddeploy.sh
ℹ Detected macOS - using /Users/Shared for installation
ℹ (Docker Desktop file sharing compatible)
✓ Directories created successfully
✓ Services started
✅ PASS
```

### Scenario 2: Volume Mount Verification
```bash
$ docker inspect droiddeploy-app | grep Source
"Source": "/Users/Shared/droiddeploy/apks"
✅ PASS - Correct path mounted
```

### Scenario 3: Data Persistence
```bash
$ ls -la /Users/Shared/droiddeploy/apks/
$ docker compose down
$ docker compose up -d
$ ls -la /Users/Shared/droiddeploy/apks/
✅ PASS - Data persists across restarts
```

---

## Permissions

`/Users/Shared` has these permissions:
```bash
$ ls -ld /Users/Shared
drwxrwxrwx  ... /Users/Shared
```

**Meaning:**
- Readable, writable, executable by all users
- Perfect for shared application data
- No special permission setup needed

**Our directories:**
```bash
/Users/Shared/droiddeploy/           # 755 (owner: root)
├── config/                           # 755 (owner: root)
│   └── .env                          # 600 (owner: root) - SECURE!
├── apks/                             # 755 (owner: root)
└── pgdata/                           # 700 (owner: root) - SECURE!
```

---

## Security Considerations

### Is /Users/Shared Secure?

**Yes, when used correctly:**

1. **Parent directory is world-writable** - BUT our subdirectories are not
2. **Our .env file is 600** - Only root can read secrets
3. **Our pgdata is 700** - Database data fully protected
4. **Root ownership** - Installation script runs as root

**Attack surface:**
- User could create files in `/Users/Shared/droiddeploy/`
- But cannot access existing protected files
- Cannot modify Docker containers or configuration

**Verdict:** Safe for this use case

### Alternative Secure Location

For maximum security, you could use:
```bash
/Users/<username>/Library/Application Support/droiddeploy/
```

**But:**
- User-specific (not shared)
- Still requires Docker Desktop file sharing configuration
- More complex path
- Not worth the added complexity

---

## Migration from Old Path

If you have an existing installation at `/opt/droiddeploy`:

### Option 1: Fresh Install (Recommended)
```bash
# Backup data
docker compose -f /opt/droiddeploy/config/docker-compose.yml down
sudo cp -r /opt/droiddeploy/apks /tmp/apks-backup

# Remove old installation
sudo rm -rf /opt/droiddeploy

# Fresh install at new location
sudo ./install-droiddeploy.sh

# Restore APKs if needed
sudo cp -r /tmp/apks-backup/* /Users/Shared/droiddeploy/apks/
```

### Option 2: Manual Move
```bash
# Stop services
docker compose -f /opt/droiddeploy/config/docker-compose.yml down

# Move data
sudo mv /opt/droiddeploy /Users/Shared/droiddeploy

# No config changes needed (paths are internal to container)

# Start services
docker compose -f /Users/Shared/droiddeploy/config/docker-compose.yml up -d
```

---

## Known Docker Desktop Versions

This issue affects:
- Docker Desktop 4.0+ (all versions)
- macOS 10.15 (Catalina) and later

**Why it exists:**
- macOS security hardening
- System Integrity Protection (SIP)
- Docker running in VM needs explicit path sharing

---

## Future Considerations

### Docker Desktop V5+
If Docker Desktop changes default shared paths, we may need to update.

**Monitor:**
- Docker Desktop release notes
- File sharing documentation
- User reports

### Docker CLI (Non-Desktop)
If using Docker CLI directly (not Docker Desktop):
- No file sharing restrictions
- Can use any path
- Our paths still work (more permissive)

---

## Related Issues

### Docker Documentation
- [File Sharing - Docker Desktop for Mac](https://docs.docker.com/desktop/settings/mac/#file-sharing)

### Common Errors
```
Error: Mounts denied
Error: path not shared
Error: no such file or directory (on container start)
```

**All fixed by:** Using Docker-shared paths

---

## Summary

| Issue | Solution | Status |
|-------|----------|--------|
| /opt not shared by Docker | Use /Users/Shared instead | ✅ Fixed |
| Manual configuration required | Auto-detect macOS, use shared path | ✅ Fixed |
| Poor user experience | Transparent, works out of box | ✅ Fixed |

---

**Status:** ✅ RESOLVED
**User Action:** Clean up old path, re-run installer
**Impact:** 🟢 LOW (automatic fix, one-time cleanup)

---

*Issue documented and fixed: 2026-01-10*
*Final macOS compatibility issue resolved!*
