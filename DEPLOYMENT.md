# DroidDeploy Deployment Guide

Complete guide for deploying and operating DroidDeploy in production and development environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation Methods](#installation-methods)
  - [Automated Installation](#automated-installation-recommended)
  - [Manual Installation](#manual-installation)
- [Database Configuration](#database-configuration)
  - [External PostgreSQL](#external-postgresql)
  - [Bundled PostgreSQL](#bundled-postgresql)
- [Configuration Reference](#configuration-reference)
- [Security Best Practices](#security-best-practices)
- [Upgrading](#upgrading)
- [Backup & Restore](#backup--restore)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Production Recommendations](#production-recommendations)

---

## Prerequisites

### System Requirements

- **Operating System**: Linux (Ubuntu 20.04+, Debian 11+, RHEL 8+, CentOS 8+, or similar)
- **CPU**: 2+ cores recommended
- **Memory**: 2GB RAM minimum, 4GB recommended
- **Disk Space**: 5GB minimum (more for APK storage)
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher

### Optional

- **PostgreSQL 13+**: If using external database
- **Reverse Proxy**: Nginx, Caddy, or similar for HTTPS
- **Systemd**: For automatic startup management

### Verify Prerequisites

```bash
# Check Docker
docker --version
# Expected: Docker version 20.10.0 or higher

# Check Docker Compose
docker compose version
# Expected: Docker Compose version v2.0.0 or higher

# Check disk space
df -h /
# Ensure at least 5GB available

# Check if port 8080 is available
ss -tuln | grep 8080
# Should return nothing if port is free
```

---

## Installation Methods

### Automated Installation (Recommended)

The automated installer handles all setup steps interactively.

#### 1. Download Installer

```bash
# Download from repository
curl -fsSL https://raw.githubusercontent.com/pashaoleynik97/droid-deploy-svc/master/install-droiddeploy.sh -o install-droiddeploy.sh

# Make executable
chmod +x install-droiddeploy.sh
```

#### 2. Run Installer

**Interactive Mode:**
```bash
sudo ./install-droiddeploy.sh
```

The installer will prompt you for:
- Database configuration (external or bundled)
- Admin credentials
- Storage paths
- Network settings

**Non-Interactive Mode (CI/CD):**
```bash
sudo ./install-droiddeploy.sh \
  --non-interactive \
  --db-mode=external \
  --db-host=postgres.example.com \
  --db-port=5432 \
  --db-name=droiddeploy \
  --db-user=droiddeploy_user \
  --db-password="${DB_PASSWORD}" \
  --admin-user=admin \
  --admin-password="${ADMIN_PASSWORD}" \
  --jwt-secret="${JWT_SECRET}" \
  --storage-path=/srv/droiddeploy/apks \
  --server-port=8080
```

#### 3. Verify Installation

```bash
# Check service status
docker compose -f /srv/droiddeploy/config/docker-compose.yml ps

# Test health endpoint
curl http://localhost:8080/actuator/health

# Test admin login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"YOUR_PASSWORD"}'
```

---

### Manual Installation

For more control over the installation process.

#### 1. Create Directories

```bash
sudo mkdir -p /srv/droiddeploy/config
sudo mkdir -p /srv/droiddeploy/apks
sudo mkdir -p /srv/droiddeploy/pgdata  # If using bundled database

sudo chmod 755 /srv/droiddeploy/config
sudo chmod 755 /srv/droiddeploy/apks
sudo chmod 700 /srv/droiddeploy/pgdata  # More restrictive for database
```

#### 2. Create Configuration File

```bash
# Copy example configuration
sudo cp .env.example /srv/droiddeploy/config/.env

# Edit configuration
sudo nano /srv/droiddeploy/config/.env
```

Required changes:
- Set `DB_PASSWORD` to a strong password
- Set `SUPER_ADMIN_PWD` to a strong password
- Generate `JWT_SECRET` with: `openssl rand -base64 48`
- Update `DB_URL` if using external database

#### 3. Copy Docker Compose File

**For external database:**
```bash
sudo cp docker-compose.yml /srv/droiddeploy/config/
```

**For bundled database:**
```bash
sudo cp docker-compose.bundled.yml /srv/droiddeploy/config/docker-compose.yml
```

#### 4. Start Services

```bash
cd /srv/droiddeploy/config
sudo docker compose pull
sudo docker compose up -d
```

#### 5. Wait for Services to Start

```bash
# Watch logs
sudo docker compose logs -f

# Or wait for health check
watch curl -f http://localhost:8080/actuator/health
```

---

## Database Configuration

### External PostgreSQL

Use this for production deployments.

#### Prerequisites

- PostgreSQL 13 or higher
- Database created
- User with appropriate permissions

#### Create Database and User

```sql
-- Connect to PostgreSQL as superuser
CREATE DATABASE droiddeploy;
CREATE USER droiddeploy_user WITH PASSWORD 'your_secure_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE droiddeploy TO droiddeploy_user;

-- For PostgreSQL 15+, also grant schema permissions
\c droiddeploy
GRANT ALL ON SCHEMA public TO droiddeploy_user;
```

#### Configure Connection

In `/srv/droiddeploy/config/.env`:

```bash
DB_URL=jdbc:postgresql://your-db-host:5432/droiddeploy
DB_USERNAME=droiddeploy_user
DB_PASSWORD=your_secure_password
```

#### Test Connection

```bash
# Using Docker (doesn't require psql on host)
docker run --rm postgres:15-alpine psql \
  "postgresql://droiddeploy_user:your_password@your-db-host:5432/droiddeploy" \
  -c "SELECT 1"
```

#### Network Configuration

Ensure the Docker container can reach your database:

- **Same host**: Use `host.docker.internal` (Mac/Windows) or `172.17.0.1` (Linux)
- **Different host**: Ensure firewall allows connections from Docker host
- **Cloud database**: Configure security groups/firewall rules

### Bundled PostgreSQL

Use for development, testing, or small deployments.

#### Advantages
- Simple setup
- No external dependencies
- Self-contained deployment

#### Disadvantages
- Requires backup management
- Limited scalability
- No managed service features

#### Configuration

In `/srv/droiddeploy/config/.env`:

```bash
# Application connects to bundled container
DB_URL=jdbc:postgresql://droiddeploy-postgres:5432/droiddeploy
DB_USERNAME=droiddeploy_user
DB_PASSWORD=your_secure_password

# PostgreSQL container configuration
POSTGRES_DB=droiddeploy
POSTGRES_USER=droiddeploy_user
POSTGRES_PASSWORD=your_secure_password  # Must match DB_PASSWORD
POSTGRES_DATA_PATH=/srv/droiddeploy/pgdata
```

#### Start Services

```bash
cd /srv/droiddeploy/config
docker compose -f docker-compose.bundled.yml up -d
```

---

## Configuration Reference

### Environment Variables

All configuration in `/srv/droiddeploy/config/.env`:

#### Database

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DB_URL` | JDBC connection URL | Yes | `jdbc:postgresql://localhost:5432/droiddeploy` |
| `DB_USERNAME` | Database username | Yes | `droiddeploy_user` |
| `DB_PASSWORD` | Database password | Yes | - |

#### Security

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `JWT_SECRET` | JWT signing secret (256+ bits) | Yes | - |
| `JWT_ISSUER` | JWT issuer claim | No | `droiddeploy` |
| `JWT_ACCESS_TOKEN_VALIDITY` | Access token TTL (seconds) | No | `900` (15 min) |
| `JWT_REFRESH_TOKEN_VALIDITY` | Refresh token TTL (seconds) | No | `2592000` (30 days) |
| `SUPER_ADMIN_LOGIN` | Initial admin username | Yes | `super_admin` |
| `SUPER_ADMIN_PWD` | Initial admin password | Yes | - |

#### Storage

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DROIDDEPLOY_STORAGE_ROOT` | APK storage path (in container) | No | `/var/lib/droiddeploy/apks` |
| `DROIDDEPLOY_STORAGE_PATH` | APK storage path (on host) | No | `/srv/droiddeploy/apks` |

#### Network

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SERVER_PORT` | HTTP server port | No | `8080` |

#### PostgreSQL (Bundled Mode Only)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `POSTGRES_DB` | Database name | Yes | `droiddeploy` |
| `POSTGRES_USER` | Database username | Yes | `droiddeploy_user` |
| `POSTGRES_PASSWORD` | Database password | Yes | - |
| `POSTGRES_DATA_PATH` | PostgreSQL data path (on host) | No | `/srv/droiddeploy/pgdata` |

### Generating Secrets

**JWT Secret (256+ bits):**
```bash
openssl rand -base64 48
```

**Strong Password:**
```bash
openssl rand -base64 32 | tr -d '\n'
```

---

## Security Best Practices

### 1. Strong Credentials

**Admin Password Requirements:**
- Minimum 12 characters
- Include uppercase, lowercase, numbers, symbols
- Avoid common words or patterns

**Generate strong password:**
```bash
openssl rand -base64 24
```

### 2. JWT Secret

- **Minimum 256 bits** (32+ characters)
- Use cryptographically secure random generation
- **Never reuse** across environments
- **Never commit** to version control

### 3. Database Security

- Use strong database passwords
- Restrict network access (firewall rules)
- Enable SSL/TLS for database connections (production)
- Regular security updates
- Limit database user permissions (principle of least privilege)

### 4. HTTPS/TLS

DroidDeploy serves HTTP only. For production, use a reverse proxy:

**Nginx Example:**
```nginx
server {
    listen 443 ssl http2;
    server_name droiddeploy.example.com;

    ssl_certificate /etc/letsencrypt/live/droiddeploy.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/droiddeploy.example.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Caddy Example:**
```
droiddeploy.example.com {
    reverse_proxy localhost:8080
}
```

### 5. File Permissions

```bash
# Configuration files (contains secrets)
chmod 600 /srv/droiddeploy/config/.env

# Directories
chmod 755 /srv/droiddeploy/config
chmod 755 /srv/droiddeploy/apks
chmod 700 /srv/droiddeploy/pgdata  # Database only
```

### 6. Network Security

- **Firewall**: Only expose necessary ports (443 for HTTPS)
- **Database**: Not exposed to internet (use internal network)
- **Docker network**: Use bridge networks for isolation

### 7. Regular Updates

```bash
# Check for updates
docker pull ghcr.io/pashaoleynik97/droiddeploy:latest

# Apply updates
sudo ./upgrade-droiddeploy.sh
```

---

## Upgrading

### Automated Upgrade

```bash
# Download upgrade script
curl -fsSL https://raw.githubusercontent.com/pashaoleynik97/droid-deploy-svc/main/upgrade-droiddeploy.sh -o upgrade-droiddeploy.sh
chmod +x upgrade-droiddeploy.sh

# Run upgrade
sudo ./upgrade-droiddeploy.sh
```

The upgrade script will:
1. Backup current configuration
2. Pull new Docker image
3. Stop current service
4. Start new version
5. Verify health
6. Provide rollback instructions if needed

### Manual Upgrade

```bash
cd /srv/droiddeploy/config

# Backup configuration
cp .env .env.backup.$(date +%Y%m%d)

# Pull latest image
docker compose pull

# Restart with new version
docker compose down
docker compose up -d

# Verify upgrade
docker compose ps
docker compose logs -f
```

### Database Migrations

Flyway migrations run automatically on startup. No manual intervention required.

### Rollback

If upgrade fails:

```bash
cd /srv/droiddeploy/config

# Stop failed deployment
docker compose down

# Restore configuration if changed
cp .env.backup.YYYYMMDD .env

# Start previous version
docker compose up -d
```

---

## Backup & Restore

### What to Backup

1. **Database** (critical)
2. **APK Files** (critical)
3. **Configuration** (`.env` file)

### Backup Database

**Bundled PostgreSQL:**
```bash
# Create backup
docker exec droiddeploy-postgres pg_dump -U droiddeploy_user droiddeploy > backup_$(date +%Y%m%d).sql

# Or backup entire data directory
tar -czf pgdata_backup_$(date +%Y%m%d).tar.gz /srv/droiddeploy/pgdata
```

**External PostgreSQL:**
```bash
pg_dump -h your-db-host -U droiddeploy_user droiddeploy > backup_$(date +%Y%m%d).sql
```

### Backup APK Files

```bash
# Create archive
tar -czf apks_backup_$(date +%Y%m%d).tar.gz /srv/droiddeploy/apks
```

### Backup Configuration

```bash
cp /srv/droiddeploy/config/.env /srv/droiddeploy/config/.env.backup.$(date +%Y%m%d)
```

### Automated Backup Script

```bash
#!/bin/bash
# save as /usr/local/bin/droiddeploy-backup.sh

BACKUP_DIR="/srv/droiddeploy/backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Backup database
docker exec droiddeploy-postgres pg_dump -U droiddeploy_user droiddeploy > "$BACKUP_DIR/database.sql"

# Backup APKs
tar -czf "$BACKUP_DIR/apks.tar.gz" /srv/droiddeploy/apks

# Backup configuration
cp /srv/droiddeploy/config/.env "$BACKUP_DIR/.env"

# Remove old backups (keep last 7 days)
find /srv/droiddeploy/backups -type d -mtime +7 -exec rm -rf {} +

echo "Backup completed: $BACKUP_DIR"
```

**Schedule with cron:**
```bash
# Edit crontab
sudo crontab -e

# Add daily backup at 2 AM
0 2 * * * /usr/local/bin/droiddeploy-backup.sh
```

### Restore Database

**Bundled PostgreSQL:**
```bash
# Stop application
docker compose -f /srv/droiddeploy/config/docker-compose.yml down

# Restore database
docker compose -f /srv/droiddeploy/config/docker-compose.yml up -d droiddeploy-postgres
sleep 10  # Wait for PostgreSQL to start

docker exec -i droiddeploy-postgres psql -U droiddeploy_user droiddeploy < backup.sql

# Start application
docker compose -f /srv/droiddeploy/config/docker-compose.yml up -d
```

**External PostgreSQL:**
```bash
psql -h your-db-host -U droiddeploy_user droiddeploy < backup.sql
```

### Restore APK Files

```bash
# Extract backup
tar -xzf apks_backup.tar.gz -C /
```

---

## Monitoring

### Health Checks

**Application Health:**
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### Container Status

```bash
# Check all containers
docker compose -f /srv/droiddeploy/config/docker-compose.yml ps

# Check specific container
docker inspect droiddeploy-app
```

### Logs

**Application Logs:**
```bash
# Follow logs
docker compose -f /srv/droiddeploy/config/docker-compose.yml logs -f droiddeploy

# Last 100 lines
docker compose -f /srv/droiddeploy/config/docker-compose.yml logs --tail=100 droiddeploy

# Logs since specific time
docker compose -f /srv/droiddeploy/config/docker-compose.yml logs --since="2024-01-01T00:00:00" droiddeploy
```

**Database Logs (Bundled):**
```bash
docker compose -f /srv/droiddeploy/config/docker-compose.yml logs -f droiddeploy-postgres
```

**Systemd Logs (if configured):**
```bash
sudo journalctl -u droiddeploy -f
```

### Metrics

Access actuator info endpoint:
```bash
curl http://localhost:8080/actuator/info
```

For production, consider integrating:
- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **Alertmanager**: Alerting

---

## Troubleshooting

### Service Won't Start

**Check logs:**
```bash
docker compose -f /srv/droiddeploy/config/docker-compose.yml logs
```

**Common causes:**
- Port 8080 already in use
- Database connection failed
- Invalid configuration
- Insufficient permissions

### Database Connection Failed

**Symptoms:**
```
Could not open JDBC Connection for transaction
```

**Solutions:**
1. Verify database is running
2. Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in `.env`
3. Test database connection manually
4. Check firewall rules
5. Verify database user permissions

### Port Already in Use

**Check what's using the port:**
```bash
sudo ss -tulpn | grep 8080
sudo lsof -i :8080
```

**Solutions:**
1. Stop conflicting service
2. Change `SERVER_PORT` in `.env`
3. Update port mapping in `docker-compose.yml`

### Out of Memory

**Symptoms:**
```
OutOfMemoryError
Container exited with code 137
```

**Solutions:**
1. Increase Docker memory limits in `docker-compose.yml`:
   ```yaml
   services:
     droiddeploy:
       deploy:
         resources:
           limits:
             memory: 2G
   ```

2. Increase JVM heap size:
   ```yaml
   environment:
     JAVA_OPTS: "-Xmx1g -Xms512m"
   ```

### APK Upload Fails

**Symptoms:**
```
Failed to upload APK
IOException writing to file
```

**Solutions:**
1. Check disk space: `df -h`
2. Verify storage directory permissions:
   ```bash
   ls -la /srv/droiddeploy/apks
   ```
3. Ensure directory is writable by container user (UID 1001)
4. Check APK file is valid and properly signed

### Migrations Fail

**Symptoms:**
```
FlywayException: Migration failed
```

**Solutions:**
1. Check database logs
2. Verify database user has schema permissions
3. Manual migration repair:
   ```bash
   docker exec -it droiddeploy-app java -jar app.jar --flyway.repair=true
   ```

### Authentication Issues

**Invalid JWT:**
```
JWT signature does not match
```

**Solutions:**
1. Verify `JWT_SECRET` is correctly set
2. Ensure secret hasn't changed (tokens become invalid)
3. Check token hasn't expired
4. Verify system clock is synchronized (NTP)

**Admin Login Failed:**
1. Check `SUPER_ADMIN_LOGIN` and `SUPER_ADMIN_PWD` in `.env`
2. Admin account only created on first startup
3. Password may have been changed via API

---

## Production Recommendations

### Infrastructure

1. **External Database**: Use managed PostgreSQL (AWS RDS, Google Cloud SQL, Azure Database)
2. **Load Balancer**: For high availability
3. **CDN**: For APK distribution (CloudFront, CloudFlare)
4. **Object Storage**: Consider S3/GCS for APK storage instead of local filesystem

### Security

1. **HTTPS Only**: Always use TLS in production
2. **Secrets Management**: Use Vault, AWS Secrets Manager, or similar
3. **Network Isolation**: Private subnets, security groups
4. **WAF**: Web Application Firewall (CloudFlare, AWS WAF)
5. **Rate Limiting**: Prevent abuse
6. **Security Scanning**: Regular vulnerability scans

### Monitoring

1. **Application Monitoring**: Datadog, New Relic, Application Insights
2. **Log Aggregation**: ELK Stack, Splunk, CloudWatch
3. **Alerting**: PagerDuty, Opsgenie
4. **Uptime Monitoring**: Pingdom, UptimeRobot

### Backup

1. **Automated Backups**: Daily database snapshots
2. **Retention Policy**: 30-day retention minimum
3. **Offsite Storage**: Cross-region backup storage
4. **Disaster Recovery**: Documented recovery procedures
5. **Test Restores**: Regular restore testing

### Performance

1. **Database Connection Pooling**: Configure HikariCP
   ```bash
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
   SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
   ```

2. **JVM Tuning**: Optimize for your workload
   ```bash
   JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```

3. **Resource Limits**: Set appropriate limits
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2.0'
         memory: 4G
       reservations:
         cpus: '1.0'
         memory: 2G
   ```

### High Availability

1. **Multiple Instances**: Run 2+ application instances
2. **Stateless Design**: Application is stateless (ready for HA)
3. **Database Replication**: PostgreSQL streaming replication
4. **Shared Storage**: Network storage for APKs (NFS, EFS, GCS Fuse)
5. **Health Checks**: Configure load balancer health checks

### Compliance

1. **Audit Logging**: Enable detailed audit trails
2. **Data Retention**: Implement retention policies
3. **Access Control**: RBAC for administrative access
4. **Encryption**: At-rest and in-transit encryption
5. **Regular Reviews**: Security and compliance audits

---

## Systemd Integration (Optional)

For automatic startup and systemd management:

### 1. Create Service File

```bash
# Copy template
sudo cp deployment/droiddeploy.service.template /etc/systemd/system/droiddeploy.service

# Edit service file
sudo nano /etc/systemd/system/droiddeploy.service

# Replace {{CONFIG_DIR}} with /srv/droiddeploy/config
```

### 2. Enable and Start

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable auto-start on boot
sudo systemctl enable droiddeploy

# Start service
sudo systemctl start droiddeploy

# Check status
sudo systemctl status droiddeploy
```

### 3. Manage Service

```bash
# Start
sudo systemctl start droiddeploy

# Stop
sudo systemctl stop droiddeploy

# Restart
sudo systemctl restart droiddeploy

# Status
sudo systemctl status droiddeploy

# Logs
sudo journalctl -u droiddeploy -f
```

---

## Support

For additional help:

- **GitHub Issues**: https://github.com/pashaoleynik97/droid-deploy-svc/issues
- **Documentation**: README.md, this file
- **Logs**: Always check logs first for error details

---

**DroidDeploy** - Self-hosted APK distribution made easy
