#!/usr/bin/env bash

# =============================================================================
# DroidDeploy Self-Hosted Installer
# =============================================================================
# Version: 1.0.0
# Description: Interactive installer for DroidDeploy APK distribution service
# Usage: sudo ./install-droiddeploy.sh [options]
# =============================================================================

set -e  # Exit on error
set -u  # Exit on undefined variable

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------

VERSION="1.0.0"
DOCKER_MIN_VERSION="20.10"
COMPOSE_MIN_VERSION="2.0"
MIN_DISK_SPACE_GB=5

# Detect OS and set appropriate defaults
OS_TYPE="$(uname -s)"
case "$OS_TYPE" in
    Darwin*)
        # macOS: Use /Users/Shared (Docker Desktop file sharing compatible)
        # /srv is read-only, /opt requires Docker Desktop configuration
        DEFAULT_INSTALL_DIR="/Users/Shared/droiddeploy"
        ;;
    Linux*)
        # Linux: use traditional /srv directory
        DEFAULT_INSTALL_DIR="/srv/droiddeploy"
        ;;
    *)
        # Other Unix-like systems: use /opt as fallback
        DEFAULT_INSTALL_DIR="/opt/droiddeploy"
        ;;
esac

# Derived default paths
DEFAULT_CONFIG_DIR="${DEFAULT_INSTALL_DIR}/config"
DEFAULT_APK_STORAGE_DIR="${DEFAULT_INSTALL_DIR}/apks"
DEFAULT_PGDATA_DIR="${DEFAULT_INSTALL_DIR}/pgdata"
DEFAULT_SERVER_PORT=8080
DEFAULT_DB_NAME="droiddeploy"
DEFAULT_DB_USERNAME="droiddeploy_user"
DEFAULT_ADMIN_USERNAME="admin"

# Docker image
DOCKER_IMAGE="ghcr.io/pashaoleynik97/droiddeploy:latest"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

print_header() {
    echo ""
    echo "================================================================="
    echo "          DroidDeploy Self-Hosted Installer v${VERSION}"
    echo "================================================================="
    echo ""
}

print_section() {
    echo ""
    echo "================================================================="
    echo "  $1"
    echo "================================================================="
    echo ""
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1" >&2
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Check Docker installation
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        print_info "Please install Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    local docker_version
    docker_version=$(docker --version | sed -E 's/.*version ([0-9]+\.[0-9]+).*/\1/')

    if ! version_gte "$docker_version" "$DOCKER_MIN_VERSION"; then
        print_error "Docker version $docker_version is too old (minimum: $DOCKER_MIN_VERSION)"
        exit 1
    fi

    print_success "Docker installed (version $docker_version)"
}

# Check Docker Compose installation
check_docker_compose() {
    if ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed"
        print_info "Please install Docker Compose: https://docs.docker.com/compose/install/"
        exit 1
    fi

    local compose_version
    compose_version=$(docker compose version 2>/dev/null | sed -E 's/.*version (v)?([0-9]+\.[0-9]+).*/\2/' | head -1)

    if ! version_gte "$compose_version" "$COMPOSE_MIN_VERSION"; then
        print_error "Docker Compose version $compose_version is too old (minimum: $COMPOSE_MIN_VERSION)"
        exit 1
    fi

    print_success "Docker Compose installed (version $compose_version)"
}

# Version comparison
version_gte() {
    printf '%s\n%s\n' "$2" "$1" | sort -V -C
}

# Check disk space
check_disk_space() {
    local available_gb
    # Use awk to calculate GB from 512-byte blocks (portable across Linux and macOS)
    available_gb=$(df / | tail -1 | awk '{printf "%.0f", $4/1024/1024}')

    if [[ $available_gb -lt $MIN_DISK_SPACE_GB ]]; then
        print_error "Insufficient disk space (available: ${available_gb}GB, required: ${MIN_DISK_SPACE_GB}GB)"
        exit 1
    fi

    print_success "Sufficient disk space (${available_gb}GB available)"
}

# Check port availability
check_port() {
    local port=$1
    if netstat -tuln 2>/dev/null | grep -q ":${port} " || ss -tuln 2>/dev/null | grep -q ":${port} "; then
        return 1
    fi
    return 0
}

# Validate password strength
validate_password() {
    local password=$1
    local min_length=12

    if [[ ${#password} -lt $min_length ]]; then
        print_error "Password too short (minimum: ${min_length} characters)"
        return 1
    fi

    if ! echo "$password" | grep -q '[A-Z]'; then
        print_error "Password must contain at least one uppercase letter"
        return 1
    fi

    if ! echo "$password" | grep -q '[a-z]'; then
        print_error "Password must contain at least one lowercase letter"
        return 1
    fi

    if ! echo "$password" | grep -q '[0-9]'; then
        print_error "Password must contain at least one number"
        return 1
    fi

    if ! echo "$password" | grep -q '[^a-zA-Z0-9]'; then
        print_error "Password must contain at least one special character"
        return 1
    fi

    return 0
}

# Generate secure random string
generate_secret() {
    openssl rand -base64 48 | tr -d '\n'
}

# Test PostgreSQL connection
test_postgres_connection() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5

    print_info "Testing database connection..."

    # Use Docker to test connection (avoids need for psql on host)
    if docker run --rm postgres:15-alpine psql \
        "postgresql://${username}:${password}@${host}:${port}/${database}" \
        -c "SELECT 1" &> /dev/null; then
        print_success "Database connection successful"
        return 0
    else
        print_error "Database connection failed"
        return 1
    fi
}

# Prompt for input with default value
prompt() {
    local prompt_text=$1
    local default_value=$2
    local var_name=$3
    local is_secret=${4:-false}

    if [[ -n "$default_value" ]]; then
        prompt_text="${prompt_text} [${default_value}]"
    fi

    if [[ "$is_secret" == "true" ]]; then
        read -rsp "${prompt_text}: " input_value
        echo ""  # New line after hidden input
    else
        read -rp "${prompt_text}: " input_value
    fi

    if [[ -z "$input_value" ]]; then
        eval "$var_name=\"$default_value\""
    else
        eval "$var_name=\"$input_value\""
    fi
}

# Confirm action
confirm() {
    local prompt_text=$1
    local response

    read -rp "${prompt_text} [y/N]: " response
    case "$response" in
        [yY][eE][sS]|[yY])
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# -----------------------------------------------------------------------------
# Main Installation Functions
# -----------------------------------------------------------------------------

run_preflight_checks() {
    print_section "Pre-flight Checks"

    # Show detected OS
    case "$OS_TYPE" in
        Darwin*)
            print_info "Detected macOS - using /Users/Shared for installation"
            print_info "(Docker Desktop file sharing compatible)"
            ;;
        Linux*)
            print_info "Detected Linux - using /srv for installation"
            ;;
        *)
            print_info "Detected $OS_TYPE - using /opt for installation"
            ;;
    esac
    echo ""

    check_root
    check_docker
    check_docker_compose
    check_disk_space

    if check_port "$DEFAULT_SERVER_PORT"; then
        print_success "Port $DEFAULT_SERVER_PORT is available"
    else
        print_warning "Port $DEFAULT_SERVER_PORT is in use"
    fi
}

configure_database() {
    print_section "Database Configuration"

    echo "DroidDeploy requires a PostgreSQL database."
    echo ""
    echo "Choose database mode:"
    echo "  1) Use existing PostgreSQL instance (recommended for production)"
    echo "  2) Bundle PostgreSQL container (convenient for testing)"
    echo ""

    local choice
    read -rp "Your choice [1]: " choice
    choice=${choice:-1}

    if [[ "$choice" == "1" ]]; then
        DB_MODE="external"
        configure_external_database
    elif [[ "$choice" == "2" ]]; then
        DB_MODE="bundled"
        configure_bundled_database
    else
        print_error "Invalid choice"
        exit 1
    fi
}

configure_external_database() {
    echo ""
    echo "Enter PostgreSQL connection details:"
    echo ""

    prompt "  Host" "localhost" DB_HOST
    prompt "  Port" "5432" DB_PORT
    prompt "  Database name" "$DEFAULT_DB_NAME" DB_NAME
    prompt "  Username" "$DEFAULT_DB_USERNAME" DB_USERNAME
    prompt "  Password" "" DB_PASSWORD true

    # Validate connection
    if ! test_postgres_connection "$DB_HOST" "$DB_PORT" "$DB_NAME" "$DB_USERNAME" "$DB_PASSWORD"; then
        echo ""
        if confirm "Connection failed. Continue anyway?"; then
            print_warning "Skipping connection test"
        else
            print_error "Installation aborted"
            exit 1
        fi
    fi

    DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
}

configure_bundled_database() {
    echo ""
    print_info "A PostgreSQL container will be created with auto-generated credentials."
    echo ""

    DB_NAME="$DEFAULT_DB_NAME"
    DB_USERNAME="$DEFAULT_DB_USERNAME"
    DB_PASSWORD=$(generate_secret | cut -c1-32)  # 32 char password
    DB_URL="jdbc:postgresql://droiddeploy-postgres:5432/${DB_NAME}"

    prompt "  Database storage path" "$PGDATA_DIR" PGDATA_DIR

    print_warning "Remember to backup this directory regularly: $PGDATA_DIR"
}

configure_security() {
    print_section "Security Configuration"

    echo "Configure initial administrator account:"
    echo ""

    # Admin username
    prompt "  Admin username" "$DEFAULT_ADMIN_USERNAME" ADMIN_USERNAME

    # Admin password with validation
    while true; do
        prompt "  Admin password" "" ADMIN_PASSWORD true

        if [[ -z "$ADMIN_PASSWORD" ]]; then
            print_error "Password cannot be empty"
            continue
        fi

        prompt "  Confirm password" "" ADMIN_PASSWORD_CONFIRM true

        if [[ "$ADMIN_PASSWORD" != "$ADMIN_PASSWORD_CONFIRM" ]]; then
            print_error "Passwords do not match"
            continue
        fi

        if validate_password "$ADMIN_PASSWORD"; then
            break
        fi
    done

    # Generate JWT secret
    echo ""
    print_info "Generating secure JWT signing secret..."
    JWT_SECRET=$(generate_secret)
    print_success "JWT secret generated"
}

configure_installation_path() {
    print_section "Installation Path"

    echo "Choose installation directory for DroidDeploy."
    echo "All configuration and data will be stored under this path."
    echo ""

    prompt "  Installation directory" "$DEFAULT_INSTALL_DIR" INSTALL_DIR

    # Update all derived paths
    CONFIG_DIR="${INSTALL_DIR}/config"
    APK_STORAGE_DIR="${INSTALL_DIR}/apks"
    PGDATA_DIR="${INSTALL_DIR}/pgdata"

    print_success "Installation directory set to: $INSTALL_DIR"
}

configure_storage() {
    print_section "Storage Configuration"

    echo "Where should APK files be stored?"
    echo ""

    prompt "  Storage path" "$APK_STORAGE_DIR" APK_STORAGE_DIR

    print_success "APK files will be stored at: $APK_STORAGE_DIR"
}

configure_network() {
    print_section "Network Configuration"

    while true; do
        prompt "  HTTP port" "$DEFAULT_SERVER_PORT" SERVER_PORT

        if check_port "$SERVER_PORT"; then
            break
        else
            print_error "Port $SERVER_PORT is already in use"
            if ! confirm "Choose a different port?"; then
                print_warning "Installation will continue, but the port conflict must be resolved"
                break
            fi
        fi
    done

    print_warning "DroidDeploy will be accessible at: http://YOUR_HOST_IP:${SERVER_PORT}"
    echo ""
    print_info "For production use, place behind a reverse proxy with HTTPS!"
}

show_summary() {
    print_section "Installation Summary"

    echo "Configuration summary:"
    echo "  • Installation directory: $INSTALL_DIR"
    echo "  • Database mode: $( [[ "$DB_MODE" == "bundled" ]] && echo "Bundled PostgreSQL container" || echo "External PostgreSQL" )"
    echo "  • Admin username: $ADMIN_USERNAME"
    echo "  • Storage path: $APK_STORAGE_DIR"
    echo "  • HTTP port: $SERVER_PORT"
    echo ""
    echo "Generated files will be created at:"
    echo "  ${CONFIG_DIR}/.env"
    echo "  ${CONFIG_DIR}/docker-compose.yml"
    echo ""

    if ! confirm "Proceed with installation?"; then
        print_info "Installation aborted by user"
        exit 0
    fi
}

create_directories() {
    print_info "Creating directories..."

    # Create directories with error handling
    if ! mkdir -p "$CONFIG_DIR" 2>/dev/null; then
        print_error "Failed to create directory: $CONFIG_DIR"
        print_info "Please ensure you have write permissions or choose a different path"
        exit 1
    fi

    if ! mkdir -p "$APK_STORAGE_DIR" 2>/dev/null; then
        print_error "Failed to create directory: $APK_STORAGE_DIR"
        print_info "Please ensure you have write permissions or choose a different path"
        exit 1
    fi

    if [[ "$DB_MODE" == "bundled" ]]; then
        if ! mkdir -p "$PGDATA_DIR" 2>/dev/null; then
            print_error "Failed to create directory: $PGDATA_DIR"
            print_info "Please ensure you have write permissions or choose a different path"
            exit 1
        fi
    fi

    # Set permissions
    chmod 755 "$CONFIG_DIR"
    chmod 755 "$APK_STORAGE_DIR"

    if [[ "$DB_MODE" == "bundled" ]]; then
        chmod 700 "$PGDATA_DIR"  # More restrictive for database
    fi

    print_success "Directories created successfully"
}

generate_env_file() {
    print_info "Writing configuration files..."

    cat > "${CONFIG_DIR}/.env" <<EOF
# DroidDeploy Configuration
# Generated on $(date)

# Database Configuration
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# Admin Bootstrap
SUPER_ADMIN_LOGIN=${ADMIN_USERNAME}
SUPER_ADMIN_PWD=${ADMIN_PASSWORD}

# JWT Security
JWT_SECRET=${JWT_SECRET}
JWT_ISSUER=droiddeploy
JWT_ACCESS_TOKEN_VALIDITY=900
JWT_REFRESH_TOKEN_VALIDITY=2592000

# Storage
DROIDDEPLOY_STORAGE_ROOT=/var/lib/droiddeploy/apks
DROIDDEPLOY_STORAGE_PATH=${APK_STORAGE_DIR}

# Server
SERVER_PORT=${SERVER_PORT}
EOF

    if [[ "$DB_MODE" == "bundled" ]]; then
        cat >> "${CONFIG_DIR}/.env" <<EOF

# PostgreSQL Container Configuration
POSTGRES_DB=${DB_NAME}
POSTGRES_USER=${DB_USERNAME}
POSTGRES_PASSWORD=${DB_PASSWORD}
POSTGRES_DATA_PATH=${PGDATA_DIR}
EOF
    fi

    chmod 600 "${CONFIG_DIR}/.env"  # Restrict access to secrets
    print_success "Configuration file created: ${CONFIG_DIR}/.env"
}

generate_compose_file() {
    local source_file

    if [[ "$DB_MODE" == "bundled" ]]; then
        source_file="docker-compose.bundled.yml"
    else
        source_file="docker-compose.yml"
    fi

    # Download compose file from repository or copy if running from repo
    if [[ -f "$source_file" ]]; then
        cp "$source_file" "${CONFIG_DIR}/docker-compose.yml"
    else
        print_error "Could not find $source_file"
        print_info "Please ensure you're running the installer from the repository directory"
        exit 1
    fi

    print_success "Compose file created: ${CONFIG_DIR}/docker-compose.yml"
}

pull_images() {
    print_info "Pulling Docker images..."

    cd "$CONFIG_DIR"

    # Try docker compose pull first
    if docker compose pull 2>/dev/null; then
        print_success "Docker images pulled successfully"
        return 0
    fi

    # Fallback: Pull images directly (workaround for credential helper issues)
    print_warning "docker compose pull failed, trying direct pull..."

    # Pull application image
    if docker pull "$DOCKER_IMAGE"; then
        print_success "Application image pulled: $DOCKER_IMAGE"
    else
        print_error "Failed to pull application image: $DOCKER_IMAGE"
        exit 1
    fi

    # Pull PostgreSQL image if bundled mode
    if [[ "$DB_MODE" == "bundled" ]]; then
        if docker pull postgres:15-alpine; then
            print_success "PostgreSQL image pulled: postgres:15-alpine"
        else
            print_error "Failed to pull PostgreSQL image"
            exit 1
        fi
    fi

    print_success "All images pulled successfully"
}

start_services() {
    print_info "Starting DroidDeploy services..."

    cd "$CONFIG_DIR"

    if docker compose up -d; then
        print_success "Services started"
    else
        print_error "Failed to start services"
        exit 1
    fi
}

wait_for_health() {
    print_info "Waiting for application to be ready..."

    local max_attempts=24  # 2 minutes (24 * 5 seconds)
    local attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        echo -n "  ⏳ Health check (attempt $attempt/$max_attempts)..."

        if curl -f -s "http://localhost:${SERVER_PORT}/actuator/health" > /dev/null 2>&1; then
            echo ""
            print_success "Application is healthy!"
            return 0
        fi

        echo " not ready yet"
        sleep 5
        ((attempt++))
    done

    echo ""
    print_error "Application did not become healthy within expected time"
    print_info "Check logs: docker compose -f ${CONFIG_DIR}/docker-compose.yml logs -f"
    return 1
}

show_completion() {
    print_section "Installation Complete! 🎉"

    echo "DroidDeploy is now running!"
    echo ""
    echo "  🌐 URL: http://$(hostname -I | awk '{print $1}'):${SERVER_PORT}"
    echo "  👤 Admin username: ${ADMIN_USERNAME}"
    echo "  🔐 Admin password: (as entered)"
    echo ""
    echo "Next steps:"
    echo "  1. Test admin login:"
    echo "     curl -X POST http://localhost:${SERVER_PORT}/api/v1/auth/login \\"
    echo "       -H \"Content-Type: application/json\" \\"
    echo "       -d '{\"login\":\"${ADMIN_USERNAME}\",\"password\":\"YOUR_PASSWORD\"}'"
    echo ""
    echo "  2. Set up HTTPS reverse proxy (nginx/Caddy)"
    echo ""
    echo "  3. Configure regular database backups:"

    if [[ "$DB_MODE" == "bundled" ]]; then
        echo "     - Database: $PGDATA_DIR"
    else
        echo "     - Database: Use your PostgreSQL backup strategy"
    fi

    echo "     - APK files: $APK_STORAGE_DIR"
    echo ""
    echo "Useful commands:"
    echo "  • View logs:     docker compose -f ${CONFIG_DIR}/docker-compose.yml logs -f"
    echo "  • Stop service:  docker compose -f ${CONFIG_DIR}/docker-compose.yml down"
    echo "  • Start service: docker compose -f ${CONFIG_DIR}/docker-compose.yml up -d"
    echo ""
    echo "Documentation: https://github.com/pashaoleynik97/droid-deploy-svc"
    echo ""
    print_section "End of Installation"
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

main() {
    print_header

    # Set default values
    INSTALL_DIR="$DEFAULT_INSTALL_DIR"
    CONFIG_DIR="$DEFAULT_CONFIG_DIR"
    APK_STORAGE_DIR="$DEFAULT_APK_STORAGE_DIR"
    PGDATA_DIR="$DEFAULT_PGDATA_DIR"
    SERVER_PORT="$DEFAULT_SERVER_PORT"

    # Run installation steps
    run_preflight_checks
    configure_installation_path
    configure_database
    configure_security
    configure_storage
    configure_network
    show_summary

    # Execute installation
    echo ""
    print_section "Installing..."
    echo "[1/6] Creating directories..."
    create_directories

    echo "[2/6] Writing configuration files..."
    generate_env_file
    generate_compose_file

    echo "[3/6] Pulling Docker images..."
    pull_images

    echo "[4/6] Starting services..."
    start_services

    echo "[5/6] Waiting for application to be ready..."
    wait_for_health

    echo "[6/6] Installation complete"

    show_completion
}

# Run main function
main "$@"
