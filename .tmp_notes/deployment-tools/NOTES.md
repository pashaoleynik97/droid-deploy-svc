# Implementation Notes

## Key Technical Details

### Project Structure
- **Build System:** Gradle 8.14+ with Kotlin DSL
- **Java Version:** 21 (Eclipse Temurin)
- **Spring Boot:** 4.0.0
- **Main Module:** droiddeploy-svc (produces bootJar)

### Build Commands
```bash
# Build executable JAR
./gradlew :droiddeploy-svc:bootJar

# Output location
droiddeploy-svc/build/libs/droiddeploy-svc-0.0.1-SNAPSHOT.jar
```

### Docker Build Context
- Multi-module Gradle project
- Only droiddeploy-svc produces executable JAR
- Need to copy entire project for Gradle build
- Use .dockerignore to exclude unnecessary files

### Required Runtime Dependencies
- PostgreSQL JDBC driver (included in JAR)
- Flyway migrations (included in JAR)
- JWT libraries (included in JAR)
- All dependencies bundled in fat JAR

### Volume Mounts Required
1. `/var/lib/droiddeploy/apks` - APK file storage (MUST persist)
2. `/srv/droiddeploy/pgdata` - PostgreSQL data (bundled mode only)

### Network Configuration
- Application listens on port 8080 (configurable via SERVER_PORT)
- PostgreSQL in bundled mode uses standard port 5432 (internal network)
- Only application port needs to be exposed to host

### Health Checks
- Endpoint: `http://localhost:8080/actuator/health`
- Returns JSON: `{"status":"UP"}` when healthy
- Used by Docker healthcheck and installer verification

### Security Considerations
- Container MUST run as non-root user
- APK storage directory needs write permissions
- JWT secret MUST be at least 256 bits (32+ characters)
- Database password should be strong (installer validation)

### Flyway Migration Behavior
- Auto-run on startup (spring.flyway.enabled=true)
- Baseline on migrate enabled (handles existing DBs)
- Migrations in: droiddeploy-svc/src/main/resources/db/migration/
- Currently 2 migrations: V1 (initial), V2 (versions)

### Bootstrap Process
1. Spring Boot starts
2. Flyway runs migrations
3. SuperAdminInitializer creates admin user (if not exists)
4. Application becomes ready
5. Health check returns UP

### Environment Variable Naming
Spring Boot property to env var mapping:
- `spring.datasource.url` → `SPRING_DATASOURCE_URL` or `DB_URL` (custom mapping)
- `droiddeploy.storage.root` → `DROIDDEPLOY_STORAGE_ROOT`
- `security.jwt.secret` → `SECURITY_JWT_SECRET` or `JWT_SECRET` (custom mapping)

Note: application.yaml uses custom env var names (DB_URL, JWT_SECRET, etc.)

### Installer Requirements
- Check Docker version ≥ 20.10
- Check Docker Compose version ≥ 2.0
- Validate port 8080 availability (or custom port)
- Test PostgreSQL connectivity (external DB mode)
- Generate 256-bit+ JWT secret using `openssl rand -base64 48`
- Validate password complexity (min 12 chars, mixed case, numbers, symbols)
- Create directories with proper permissions
- Poll health endpoint during startup (max 2-3 minutes)

### Testing Matrix
| Mode | Database | Expected Behavior |
|------|----------|-------------------|
| External DB | User-provided PostgreSQL | App connects to external DB, runs migrations |
| Bundled DB | PostgreSQL container | Both containers start, app waits for DB health |
| Upgrade | Existing DB | New image, migrations auto-apply, data persists |

### Image Tagging Strategy
```
ghcr.io/pashaoleynik97/droiddeploy:latest         # Always latest stable
ghcr.io/pashaoleynik97/droiddeploy:0.0.1          # Specific version
ghcr.io/pashaoleynik97/droiddeploy:0.0.1-SNAPSHOT # Development builds
```

### Gradle Build Optimization
- Use Gradle daemon in container build: `--no-daemon` to avoid issues
- Build only droiddeploy-svc module
- Cache Gradle dependencies in Docker layer
- Use multi-stage build to separate build and runtime

### Known Issues to Handle
1. **APK storage permissions:** Container user needs write access
2. **Database connection timeout:** Add connection validation timeout
3. **JWT secret length:** Validate minimum 32 characters in installer
4. **Port conflicts:** Installer should detect and prompt for alternative

### Installer Script Features to Implement
- [x] Pre-flight checks (Docker, Compose, disk space, port)
- [ ] Interactive prompts with validation
- [ ] Non-interactive mode for CI/CD
- [ ] Dry-run mode (--dry-run)
- [ ] Secret generation (JWT, DB password)
- [ ] Database connectivity test (external mode)
- [ ] Configuration file generation (.env, docker-compose.yml)
- [ ] Directory creation with permissions
- [ ] Docker image pull
- [ ] Container startup
- [ ] Health check polling
- [ ] Admin login verification
- [ ] Summary and next steps output

### Upgrade Script Features to Implement
- [ ] Version detection (current vs latest)
- [ ] Changelog display
- [ ] Backup prompt
- [ ] Configuration backup
- [ ] Image pull
- [ ] Graceful shutdown
- [ ] Startup with new version
- [ ] Health verification
- [ ] Rollback instructions

### Documentation Structure
```
README.md
├── Quick Start (brief, link to DEPLOYMENT.md)
├── Features
├── API Overview
└── Development Setup

DEPLOYMENT.md (new)
├── Prerequisites
├── Installation Methods
│   ├── Automated Installer (recommended)
│   └── Manual Docker Compose
├── Configuration Reference
├── Database Options
│   ├── External PostgreSQL
│   └── Bundled PostgreSQL
├── Security Best Practices
├── Upgrading
├── Troubleshooting
└── Production Recommendations
```

### Files Not to Include in Docker Build
(for .dockerignore)
- `.git/`
- `.gradle/`
- `.idea/`
- `build/` (all modules)
- `.tmp_notes/`
- `*.md` (except README for reference)
- `.gitignore`
- `.dockerignore`
- Any IDE-specific files

### Systemd Integration (Optional)
- Service file manages docker-compose lifecycle
- Enables auto-start on boot
- Provides systemctl interface (start/stop/status)
- Logs to journald
- Not required but nice for production deployments

---

## Quick Reference Commands

### Building JAR Locally
```bash
./gradlew :droiddeploy-svc:bootJar
```

### Testing Docker Build
```bash
docker build -t droiddeploy:test .
```

### Running Container Manually
```bash
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/droiddeploy \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=pass \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  -e SUPER_ADMIN_LOGIN=admin \
  -e SUPER_ADMIN_PWD=SecurePass123! \
  -v /srv/droiddeploy/apks:/var/lib/droiddeploy/apks \
  droiddeploy:test
```

### Testing Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Testing Admin Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"SecurePass123!"}'
```

---

*This file contains detailed technical notes for implementation reference.*
