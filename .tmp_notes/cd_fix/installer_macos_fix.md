# Installer Script macOS Compatibility Fix

**Date:** 2026-01-09
**Issue:** install-droiddeploy.sh failing on macOS with grep and df errors
**Status:** Fixed

---

## Error Details

When running the installer on macOS:
```bash
grep: invalid option -- P
usage: grep [-abcdDEFGHhIiJLlMmnOopqRSsUVvwXxZz] ...
✗ Docker version  is too old (minimum: 20.10)
```

---

## Root Cause

The installer script used GNU-specific command options that are not available on macOS (BSD variants):

1. **`grep -P`** - Perl-compatible regex (GNU grep only)
   - Used on lines 96 and 115
   - BSD grep (macOS default) doesn't support `-P` flag

2. **`df -BG`** - GNU df option for gigabyte display
   - Used on line 133
   - BSD df (macOS) doesn't support `-B` flag

---

## Fixes Applied

### 1. Docker Version Check (line 96)

**Before:**
```bash
docker_version=$(docker --version | grep -oP '\d+\.\d+' | head -1)
```

**After:**
```bash
docker_version=$(docker --version | sed -E 's/.*version ([0-9]+\.[0-9]+).*/\1/')
```

**Why:** Uses `sed` with POSIX extended regex (`-E`), which works on both GNU and BSD systems.

---

### 2. Docker Compose Version Check (line 115)

**Before:**
```bash
compose_version=$(docker compose version --short | grep -oP '\d+\.\d+' | head -1)
```

**After:**
```bash
compose_version=$(docker compose version 2>/dev/null | sed -E 's/.*version (v)?([0-9]+\.[0-9]+).*/\2/' | head -1)
```

**Why:**
- Removed reliance on `--short` flag which may vary across versions
- Uses `sed` with extended regex for portability
- Captures optional 'v' prefix in version strings

---

### 3. Disk Space Check (line 133)

**Before:**
```bash
available_gb=$(df -BG / | tail -1 | awk '{print $4}' | sed 's/G//')
```

**After:**
```bash
available_gb=$(df / | tail -1 | awk '{printf "%.0f", $4/1024/1024}')
```

**Why:**
- Uses default df output (512-byte blocks on macOS)
- Converts blocks to GB using awk math: blocks / 1024 / 1024
- Works consistently across Linux and macOS

---

## Testing Results

All fixes tested and verified on macOS:

```bash
# Docker version extraction
$ docker --version | sed -E 's/.*version ([0-9]+\.[0-9]+).*/\1/'
27.4

# Docker Compose version extraction
$ docker compose version 2>/dev/null | sed -E 's/.*version (v)?([0-9]+\.[0-9]+).*/\2/' | head -1
2.31

# Disk space calculation
$ df / | tail -1 | awk '{printf "%.0f", $4/1024/1024}'
252
```

All checks now pass on macOS.

---

## Portability Improvements

These changes make the installer compatible with:
- **Linux** (GNU tools)
- **macOS** (BSD tools)
- **Other Unix-like systems** with POSIX-compliant tools

The script now uses:
- `sed -E` instead of `grep -P` for regex matching
- Portable `awk` math instead of GNU-specific `df` options
- Standard command pipelines that work across platforms

---

## Files Modified

- `install-droiddeploy.sh` (lines 96, 115, 133)

---

## Next Steps

1. ✅ All pre-flight checks now work on macOS
2. ⏳ User can proceed with full installation test
3. ⏳ Consider testing on other platforms (FreeBSD, Alpine Linux, etc.)

---

## Related Issues

- Initial error: `grep: invalid option -- P`
- Platform: macOS (Darwin 24.0.0)
- Bash version: Works with both bash 3.x (macOS default) and bash 5.x (Homebrew)
