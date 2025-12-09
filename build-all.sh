#!/bin/bash
# VonixCore Build Script (Bash)
# Builds all platform versions and outputs to BuildOutput folder

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/BuildOutput"

# Clean and create output directory
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}     VonixCore Multi-Platform Build${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""

# Get version from gradle.properties
VERSION="1.0.0"
PROPS_FILE="$SCRIPT_DIR/VonixCore-NeoForge-Universal/gradle.properties"
if [ -f "$PROPS_FILE" ]; then
    VERSION=$(grep "^mod_version=" "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
fi
echo "Building version: $VERSION"
echo ""

# Build NeoForge
echo -e "${YELLOW}[1/4] Building NeoForge version...${NC}"
NEOFORGE_DIR="$SCRIPT_DIR/VonixCore-NeoForge-Universal"
if [ -d "$NEOFORGE_DIR" ]; then
    cd "$NEOFORGE_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-NeoForge-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-NeoForge-$VERSION.jar${NC}"
    else
        echo -e "${RED}  FAIL NeoForge build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  FAIL NeoForge directory not found${NC}"
fi

echo ""

# Build Forge 1.20.1
echo -e "${YELLOW}[2/4] Building Forge 1.20.1 version...${NC}"
FORGE_1201_DIR="$SCRIPT_DIR/VonixCore-Forge-1.20.1"
if [ -d "$FORGE_1201_DIR" ]; then
    cd "$FORGE_1201_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Forge-1.20.1-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Forge-1.20.1-$VERSION.jar${NC}"
    else
        echo -e "${RED}  FAIL Forge 1.20.1 build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  FAIL Forge 1.20.1 directory not found${NC}"
fi

echo ""

# Build Paper
echo -e "${YELLOW}[3/4] Building Paper version...${NC}"
PAPER_DIR="$SCRIPT_DIR/VonixCore-Paper-Universal"
if [ -d "$PAPER_DIR" ]; then
    cd "$PAPER_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Paper-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Paper-$VERSION.jar${NC}"
    else
        echo -e "${RED}  FAIL Paper build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  FAIL Paper directory not found${NC}"
fi

echo ""

# Build Bukkit
echo -e "${YELLOW}[4/4] Building Bukkit version...${NC}"
BUKKIT_DIR="$SCRIPT_DIR/VonixCore-Bukkit-Universal"
if [ -d "$BUKKIT_DIR" ]; then
    cd "$BUKKIT_DIR"
    if ./gradlew fatJar --no-daemon; then
        find build/libs -name "*-all.jar" -exec cp {} "$OUTPUT_DIR/VonixCore-Bukkit-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Bukkit-$VERSION.jar${NC}"
    else
        echo -e "${RED}  FAIL Bukkit build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  FAIL Bukkit directory not found${NC}"
fi

echo ""
echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}             Build Complete!${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""
echo "Output files in: $OUTPUT_DIR"
echo ""
ls -lh "$OUTPUT_DIR"
