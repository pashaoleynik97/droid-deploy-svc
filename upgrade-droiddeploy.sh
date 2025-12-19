#!/usr/bin/env bash

# =============================================================================
# DroidDeploy Upgrade Utility
# =============================================================================
# Version: 1.0.0
# Description: Automated upgrade tool for DroidDeploy
# Usage: sudo ./upgrade-droiddeploy.sh [options]
# =============================================================================

set -e  # Exit on error
set -u  # Exit on undefined variable

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------

VERSION="1.0.0"
DEFAULT_CONFIG_DIR="/srv/droiddeploy/config"
BACKUP_DIR_BASE="/srv/droiddeploy/backups"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# -----------------------------------------------------------------------------
# Command Line Arguments
# -----------------------------------------------------------------------------

CONFIG_DIR="$DEFAULT_CONFIG_DIR"
SKIP_BACKUP=false
NON_INTERACTIVE=false
TARGET_VERSION="latest"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --config-dir)
            CONFIG_DIR="$2"
            shift 2
            ;;
        --skip-backup)
            SKIP_BACKUP=true
            shift
            ;;
        --non-interactive)
            NON_INTERACTIVE=true
            shift
            ;;
        --version)
            TARGET_VERSION="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --config-dir DIR      Configuration directory (default: $DEFAULT_CONFIG_DIR)"
            echo "  --skip-backup         Skip configuration backup"
            echo "  --non-interactive     Run without prompts"
            echo "  --version VERSION     Target version tag (default: latest)"
            echo "  -h, --help            Show this help message"
            echo ""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

print_header() {
    echo ""
    echo "================================================================="
    echo "          DroidDeploy Upgrade Utility v${VERSION}"
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

check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "This script must be run as root (use sudo)"
        exit 1
    fi
}

confirm() {
    if [[ "$NON_INTERACTIVE" == "true" ]]; then
        return 0
    fi

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
# Validation Functions
# -----------------------------------------------------------------------------

validate_installation() {
    if [[ ! -d "$CONFIG_DIR" ]]; then
        print_error "Configuration directory not found: $CONFIG_DIR"
        print_info "Please specify correct path with --config-dir"
        exit 1
    fi

    if [[ ! -f "${CONFIG_DIR}/docker-compose.yml" ]]; then
        print_error "docker-compose.yml not found in $CONFIG_DIR"
        print_info "This doesn't appear to be a valid DroidDeploy installation"
        exit 1
    fi

    if [[ ! -f "${CONFIG_DIR}/.env" ]]; then
        print_error ".env file not found in $CONFIG_DIR"
        exit 1
    fi

    print_success "Valid installation found at: $CONFIG_DIR"
}

get_current_version() {
    cd "$CONFIG_DIR"

    # Get currently running image version
    local current_image
    current_image=$(docker compose ps -q droiddeploy 2>/dev/null | xargs -r docker inspect --format='{{.Config.Image}}' 2>/dev/null | head -1)

    if [[ -z "$current_image" ]]; then
        echo "unknown"
    else
        echo "$current_image"
    fi
}

check_for_updates() {
    print_info "Checking for updates..."

    cd "$CONFIG_DIR"

    # Pull latest image to check if update available
    local old_image_id
    old_image_id=$(docker images -q ghcr.io/pashaoleynik97/droiddeploy:${TARGET_VERSION} 2>/dev/null)

    docker pull -q "ghcr.io/pashaoleynik97/droiddeploy:${TARGET_VERSION}" >/dev/null 2>&1 || true

    local new_image_id
    new_image_id=$(docker images -q ghcr.io/pashaoleynik97/droiddeploy:${TARGET_VERSION} 2>/dev/null)

    if [[ "$old_image_id" == "$new_image_id" ]] && [[ -n "$old_image_id" ]]; then
        print_info "Already running the latest version"
        if [[ "$NON_INTERACTIVE" != "true" ]]; then
            if ! confirm "Re-deploy anyway?"; then
                print_info "Upgrade cancelled"
                exit 0
            fi
        fi
    else
        print_success "New version available"
    fi
}

# -----------------------------------------------------------------------------
# Backup Functions
# -----------------------------------------------------------------------------

create_backup() {
    if [[ "$SKIP_BACKUP" == "true" ]]; then
        print_warning "Skipping backup (--skip-backup flag set)"
        return 0
    fi

    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_dir="${BACKUP_DIR_BASE}/${timestamp}"

    print_info "Creating backup at: $backup_dir"

    mkdir -p "$backup_dir"

    # Backup configuration files
    cp "${CONFIG_DIR}/.env" "${backup_dir}/.env.backup"
    cp "${CONFIG_DIR}/docker-compose.yml" "${backup_dir}/docker-compose.yml.backup"

    # Save current container state
    cd "$CONFIG_DIR"
    docker compose config > "${backup_dir}/docker-compose.resolved.yml" 2>/dev/null || true

    # Record current image versions
    docker compose images > "${backup_dir}/images.txt" 2>/dev/null || true

    print_success "Backup created: $backup_dir"

    # Save backup location for potential rollback
    echo "$backup_dir" > "${CONFIG_DIR}/.last_backup"
}

# -----------------------------------------------------------------------------
# Upgrade Functions
# -----------------------------------------------------------------------------

pull_new_image() {
    print_info "Pulling new Docker image..."

    cd "$CONFIG_DIR"

    # Update image reference if not using 'latest'
    if [[ "$TARGET_VERSION" != "latest" ]]; then
        # Update docker-compose.yml to use specific version
        sed -i.bak "s|image: ghcr.io/pashaoleynik97/droiddeploy:.*|image: ghcr.io/pashaoleynik97/droiddeploy:${TARGET_VERSION}|" docker-compose.yml
    fi

    if docker compose pull droiddeploy; then
        print_success "New image pulled successfully"
    else
        print_error "Failed to pull new image"
        exit 1
    fi
}

stop_services() {
    print_info "Stopping current service..."

    cd "$CONFIG_DIR"

    if docker compose down; then
        print_success "Service stopped"
    else
        print_error "Failed to stop service"
        exit 1
    fi
}

start_services() {
    print_info "Starting service with new version..."

    cd "$CONFIG_DIR"

    if docker compose up -d; then
        print_success "Service started"
    else
        print_error "Failed to start service"
        print_warning "You may need to rollback manually"
        show_rollback_instructions
        exit 1
    fi
}

wait_for_health() {
    print_info "Verifying service health..."

    # Get server port from .env file
    local server_port
    server_port=$(grep "^SERVER_PORT=" "${CONFIG_DIR}/.env" | cut -d= -f2)
    server_port=${server_port:-8080}

    local max_attempts=24  # 2 minutes
    local attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "http://localhost:${server_port}/actuator/health" > /dev/null 2>&1; then
            print_success "Service is healthy!"
            return 0
        fi

        sleep 5
        ((attempt++))
    done

    print_error "Service did not become healthy within expected time"
    print_warning "Check logs: docker compose -f ${CONFIG_DIR}/docker-compose.yml logs -f"
    show_rollback_instructions
    return 1
}

clean_old_images() {
    print_info "Cleaning up old Docker images..."

    # Remove dangling images
    docker image prune -f > /dev/null 2>&1 || true

    print_success "Cleanup complete"
}

# -----------------------------------------------------------------------------
# Rollback Functions
# -----------------------------------------------------------------------------

show_rollback_instructions() {
    local last_backup
    if [[ -f "${CONFIG_DIR}/.last_backup" ]]; then
        last_backup=$(cat "${CONFIG_DIR}/.last_backup")
    fi

    print_section "Rollback Instructions"

    echo "If you need to rollback to the previous version:"
    echo ""
    echo "1. Stop the current service:"
    echo "   docker compose -f ${CONFIG_DIR}/docker-compose.yml down"
    echo ""

    if [[ -n "$last_backup" ]] && [[ -d "$last_backup" ]]; then
        echo "2. Restore configuration from backup:"
        echo "   cp ${last_backup}/.env.backup ${CONFIG_DIR}/.env"
        echo "   cp ${last_backup}/docker-compose.yml.backup ${CONFIG_DIR}/docker-compose.yml"
        echo ""
    fi

    echo "3. Start the previous version:"
    echo "   docker compose -f ${CONFIG_DIR}/docker-compose.yml up -d"
    echo ""
}

# -----------------------------------------------------------------------------
# Main Functions
# -----------------------------------------------------------------------------

show_upgrade_info() {
    print_section "Upgrade Information"

    local current_version
    current_version=$(get_current_version)

    echo "Current version: $current_version"
    echo "Target version:  ghcr.io/pashaoleynik97/droiddeploy:${TARGET_VERSION}"
    echo ""
    echo "This upgrade will:"
    echo "  • Pull the new Docker image"
    echo "  • Stop the current service"
    echo "  • Start the new version"
    echo "  • Run database migrations automatically"
    echo ""

    print_warning "This will restart the DroidDeploy service!"
    echo ""

    if [[ "$SKIP_BACKUP" != "true" ]]; then
        echo "Backup recommendations:"
        echo "  ☐ Database backup created"
        echo "  ☐ APK files backed up (if necessary)"
        echo "  ☐ Configuration backed up (will be done automatically)"
        echo ""
    fi

    if ! confirm "Continue with upgrade?"; then
        print_info "Upgrade cancelled by user"
        exit 0
    fi
}

show_completion() {
    print_section "Upgrade Complete! 🎉"

    local current_version
    current_version=$(get_current_version)

    echo "Current version: $current_version"
    echo ""
    echo "The service has been upgraded successfully."
    echo ""
    echo "Useful commands:"
    echo "  • View logs: docker compose -f ${CONFIG_DIR}/docker-compose.yml logs -f"
    echo "  • Check status: docker compose -f ${CONFIG_DIR}/docker-compose.yml ps"
    echo "  • Health check: curl http://localhost:8080/actuator/health"
    echo ""

    if [[ "$SKIP_BACKUP" != "true" ]]; then
        local last_backup
        if [[ -f "${CONFIG_DIR}/.last_backup" ]]; then
            last_backup=$(cat "${CONFIG_DIR}/.last_backup")
            echo "Backup location: $last_backup"
            echo ""
        fi
    fi

    show_rollback_instructions
}

main() {
    print_header

    check_root
    validate_installation

    local current_version
    current_version=$(get_current_version)

    echo "Installation directory: $CONFIG_DIR"
    echo "Current version: $current_version"
    echo ""

    check_for_updates
    show_upgrade_info

    # Execute upgrade
    print_section "Upgrading..."

    echo "[1/6] Creating backup..."
    create_backup

    echo "[2/6] Pulling new Docker image..."
    pull_new_image

    echo "[3/6] Stopping current service..."
    stop_services

    echo "[4/6] Starting service with new version..."
    start_services

    echo "[5/6] Verifying service health..."
    wait_for_health

    echo "[6/6] Cleaning up..."
    clean_old_images

    show_completion
}

# Run main function
main "$@"
