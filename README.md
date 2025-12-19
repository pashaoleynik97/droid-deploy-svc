# DroidDeploy

> Self-hosted Android APK distribution service

DroidDeploy is a Spring Boot-based service for managing and distributing Android application packages (APKs). It provides version control, role-based access control, and automated APK metadata extraction for streamlined APK distribution workflows.

## Features

- **Multi-Application Support**: Manage multiple Android applications from a single instance
- **Version Management**: Upload, track, and manage APK versions with stability flags
- **APK Metadata Extraction**: Automatic extraction of versionCode, versionName, and signing certificates
- **Certificate Validation**: Ensures APKs are signed with consistent certificates across versions
- **Role-Based Access Control**:
  - ADMIN: Full system access
  - CI: Upload and manage application versions
  - CONSUMER: Download and view applications/versions
- **API Key Authentication**: Machine-to-machine authentication for CI/CD integration
- **JWT-Based Authentication**: Stateless, scalable authentication
- **Database Migrations**: Flyway-managed schema evolution
- **RESTful API**: Comprehensive REST API for all operations
- **Health Monitoring**: Built-in health checks and monitoring endpoints

## Quick Start (Self-Hosted)

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- PostgreSQL 13+ (optional, can use bundled container)
- Linux host (Ubuntu 20.04+, Debian 11+, RHEL 8+, or similar)
- 500MB disk space minimum (more for APK storage)

### Installation

#### Automated Installation (Recommended)

```bash
# Download installer
curl -fsSL https://raw.githubusercontent.com/pashaoleynik97/droid-deploy-svc/main/install-droiddeploy.sh -o install-droiddeploy.sh
chmod +x install-droiddeploy.sh

# Run installer (interactive)
sudo ./install-droiddeploy.sh
```

The installer will:
1. Check prerequisites (Docker, Docker Compose, disk space)
2. Guide you through database setup (external or bundled PostgreSQL)
3. Configure security (admin account, JWT secrets)
4. Set up storage and networking
5. Deploy the service automatically

#### Manual Installation

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed manual installation instructions.

### First Steps After Installation

1. **Test admin login:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"login":"admin","password":"YOUR_PASSWORD"}'
   ```

2. **Create your first application:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/application \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"MyApp","bundleId":"com.example.myapp"}'
   ```

3. **Upload an APK version:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/application/{APP_ID}/version \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -F "file=@path/to/your-app.apk"
   ```

## Architecture

### Components

- **Spring Boot 4.0.0**: Application framework
- **PostgreSQL 13+**: Database for metadata and user management
- **Flyway**: Database migration management
- **JWT**: Stateless authentication
- **Local File System**: APK file storage (pluggable architecture)

### Multi-Module Structure

```
droiddeploy/
├── droiddeploy-core/     # Domain models, interfaces (framework-agnostic)
├── droiddeploy-db/       # JPA entities, repository implementations
├── droiddeploy-rest/     # REST controllers, exception handlers
└── droiddeploy-svc/      # Main application, service implementations, security
```

## API Overview

Base path: `/api/v1`

### Authentication
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh access token
- `POST /auth/apikey` - API key authentication

### Applications
- `GET /application` - List applications (paginated)
- `POST /application` - Create application
- `GET /application/{id}` - Get application details
- `PUT /application/{id}` - Update application
- `DELETE /application/{id}` - Delete application

### Versions
- `GET /application/{appId}/version` - List versions
- `GET /application/{appId}/version/latest` - Get latest stable version
- `POST /application/{appId}/version` - Upload new version
- `GET /application/{appId}/version/{versionCode}/apk` - Download APK
- `PUT /application/{appId}/version/{versionCode}` - Update stability flag
- `DELETE /application/{appId}/version/{versionCode}` - Delete version

### Users (Admin only)
- `GET /user` - List users
- `POST /user` - Create user
- `PUT /user/{id}` - Update user
- `PUT /user/{id}/activate` - Activate/deactivate user
- `PUT /user/{id}/password` - Change password

### API Keys (Admin only)
- `GET /application/{appId}/security/apikey` - List API keys
- `POST /application/{appId}/security/apikey` - Create API key
- `DELETE /application/{appId}/security/apikey/{keyId}` - Delete API key

Full API documentation coming soon.

## Configuration

All configuration is done via environment variables. See [`.env.example`](.env.example) for all available options.

### Key Configuration Options

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DB_URL` | PostgreSQL connection URL | Yes | `jdbc:postgresql://localhost:5432/droiddeploy` |
| `DB_USERNAME` | Database username | Yes | `droiddeploy_user` |
| `DB_PASSWORD` | Database password | Yes | - |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Yes | - |
| `SUPER_ADMIN_LOGIN` | Initial admin username | Yes | `super_admin` |
| `SUPER_ADMIN_PWD` | Initial admin password | Yes | - |
| `SERVER_PORT` | HTTP server port | No | `8080` |
| `DROIDDEPLOY_STORAGE_ROOT` | APK storage path | No | `/var/lib/droiddeploy/apks` |

## Operations

### Viewing Logs

```bash
# Using Docker Compose
docker compose -f /srv/droiddeploy/config/docker-compose.yml logs -f

# Using systemd (if configured)
sudo journalctl -u droiddeploy -f
```

### Stopping the Service

```bash
# Using Docker Compose
docker compose -f /srv/droiddeploy/config/docker-compose.yml down

# Using systemd
sudo systemctl stop droiddeploy
```

### Starting the Service

```bash
# Using Docker Compose
docker compose -f /srv/droiddeploy/config/docker-compose.yml up -d

# Using systemd
sudo systemctl start droiddeploy
```

### Upgrading

```bash
# Automated upgrade
sudo ./upgrade-droiddeploy.sh

# Manual upgrade
cd /srv/droiddeploy/config
docker compose pull
docker compose down
docker compose up -d
```

Database migrations run automatically on startup.

### Backup & Restore

**What to backup:**
- Database (PostgreSQL data)
- APK files (`/srv/droiddeploy/apks` by default)
- Configuration files (`/srv/droiddeploy/config/.env`)

**Database backup:**
```bash
# Bundled PostgreSQL
docker exec droiddeploy-postgres pg_dump -U droiddeploy_user droiddeploy > backup.sql

# External PostgreSQL
pg_dump -U droiddeploy_user -h your-db-host droiddeploy > backup.sql
```

**Database restore:**
```bash
# Bundled PostgreSQL
docker exec -i droiddeploy-postgres psql -U droiddeploy_user droiddeploy < backup.sql

# External PostgreSQL
psql -U droiddeploy_user -h your-db-host droiddeploy < backup.sql
```

## Security Best Practices

1. **Change Default Credentials**: Always change default admin password
2. **Strong JWT Secret**: Generate with `openssl rand -base64 48` (256+ bits)
3. **HTTPS**: Place behind reverse proxy (nginx, Caddy) with TLS
4. **Database Security**: Use strong database passwords, restrict network access
5. **Regular Backups**: Automate database and file backups
6. **Keep Updated**: Apply security updates promptly
7. **Network Segmentation**: Restrict access to database and internal services

## Production Recommendations

- **External Database**: Use managed PostgreSQL (AWS RDS, Google Cloud SQL, etc.)
- **Reverse Proxy**: Nginx or Caddy with Let's Encrypt for HTTPS
- **Monitoring**: Prometheus metrics + Grafana dashboards
- **Logging**: Centralized logging (ELK, Loki, CloudWatch)
- **Secrets Management**: HashiCorp Vault or cloud provider secrets manager
- **Resource Limits**: Configure appropriate Docker memory/CPU limits
- **High Availability**: Multiple app instances behind load balancer

## Development Setup

### Prerequisites

- Java 21
- Gradle 8.5+
- PostgreSQL 13+
- Docker (for TestContainers)

### Build

```bash
# Build all modules
./gradlew build

# Build only main application
./gradlew :droiddeploy-svc:bootJar

# Run tests
./gradlew test

# Run with PostgreSQL in Docker
docker run -d --name droiddeploy-postgres \
  -e POSTGRES_DB=droiddeploy \
  -e POSTGRES_USER=droiddeploy_user \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 \
  postgres:15-alpine
```

### Run Locally

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/droiddeploy
export DB_USERNAME=droiddeploy_user
export DB_PASSWORD=dev_password
export JWT_SECRET=$(openssl rand -base64 48)
export SUPER_ADMIN_LOGIN=admin
export SUPER_ADMIN_PWD=DevPassword123!

# Run application
./gradlew :droiddeploy-svc:bootRun
```

Application will be available at http://localhost:8080

### Docker Build

```bash
# Build Docker image
docker build -t droiddeploy:dev .

# Run container
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/droiddeploy \
  -e DB_USERNAME=droiddeploy_user \
  -e DB_PASSWORD=dev_password \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  -e SUPER_ADMIN_LOGIN=admin \
  -e SUPER_ADMIN_PWD=DevPassword123! \
  -v /tmp/apks:/var/lib/droiddeploy/apks \
  droiddeploy:dev
```

## Troubleshooting

### Service won't start

1. Check logs: `docker compose logs -f`
2. Verify database connectivity
3. Ensure port 8080 is not in use
4. Check file permissions on APK storage directory

### Database connection errors

1. Verify database is running
2. Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in `.env`
3. Ensure database is accessible from Docker container
4. Check firewall rules

### Authentication issues

1. Verify `JWT_SECRET` is set and at least 256 bits
2. Check admin credentials in `.env`
3. Token may have expired (15-minute default)

### APK upload fails

1. Check disk space: `df -h`
2. Verify APK storage directory permissions
3. Ensure APK is properly signed
4. Check application bundle ID matches

For more help, see [DEPLOYMENT.md](DEPLOYMENT.md) or open an issue.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Links

- **Repository**: https://github.com/pashaoleynik97/droid-deploy-svc
- **Docker Image**: ghcr.io/pashaoleynik97/droiddeploy
- **Documentation**: [DEPLOYMENT.md](DEPLOYMENT.md)

## Support

For questions, issues, or feature requests, please open an issue on GitHub.

---

**Built with Spring Boot, Kotlin, and ❤️**
