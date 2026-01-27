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
VERSION="1.2"
PROPS_FILE="$SCRIPT_DIR/VonixCore-NeoForge-Universal/gradle.properties"
if [ -f "$PROPS_FILE" ]; then
    VERSION=$(grep "^mod_version=" "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
fi
echo "Building version: $VERSION"
echo ""

BUILD_COUNT=0
SUCCESS_COUNT=0

# Build NeoForge
echo -e "${YELLOW}[1/6] Building NeoForge version...${NC}"
NEOFORGE_DIR="$SCRIPT_DIR/VonixCore-NeoForge-Universal"
if [ -d "$NEOFORGE_DIR" ]; then
    ((BUILD_COUNT++)) || true
    cd "$NEOFORGE_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-NeoForge-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-NeoForge-$VERSION.jar${NC}"
        ((SUCCESS_COUNT++)) || true
    else
        echo -e "${RED}  FAIL NeoForge build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  SKIP NeoForge directory not found${NC}"
fi

echo ""

# Build Forge 1.20.1
echo -e "${YELLOW}[2/6] Building Forge 1.20.1 version...${NC}"
FORGE_1201_DIR="$SCRIPT_DIR/VonixCore-Forge-1.20.1"
if [ -d "$FORGE_1201_DIR" ]; then
    ((BUILD_COUNT++)) || true
    cd "$FORGE_1201_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Forge-1.20.1-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Forge-1.20.1-$VERSION.jar${NC}"
        ((SUCCESS_COUNT++)) || true
    else
        echo -e "${RED}  FAIL Forge 1.20.1 build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  SKIP Forge 1.20.1 directory not found${NC}"
fi

echo ""

# Build Forge 1.18.2
echo -e "${YELLOW}[3/6] Building Forge 1.18.2 version...${NC}"
FORGE_1182_DIR="$SCRIPT_DIR/VonixCore-Template-Forge-1.18.2"
if [ -d "$FORGE_1182_DIR" ]; then
    ((BUILD_COUNT++)) || true
    cd "$FORGE_1182_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Forge-1.18.2-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Forge-1.18.2-$VERSION.jar${NC}"
        ((SUCCESS_COUNT++)) || true
    else
        echo -e "${RED}  FAIL Forge 1.18.2 build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  SKIP Forge 1.18.2 directory not found${NC}"
fi

echo ""

# Build Fabric 1.20.1
echo -e "${YELLOW}[4/6] Building Fabric 1.20.1 version...${NC}"
FABRIC_1201_DIR="$SCRIPT_DIR/vonixcore-template-fabric-1.20.1"
if [ -d "$FABRIC_1201_DIR" ]; then
    ((BUILD_COUNT++)) || true
    cd "$FABRIC_1201_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Fabric-1.20.1-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Fabric-1.20.1-$VERSION.jar${NC}"
        ((SUCCESS_COUNT++)) || true
    else
        echo -e "${RED}  FAIL Fabric 1.20.1 build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  SKIP Fabric 1.20.1 directory not found${NC}"
fi

echo ""

# Build Fabric 1.21.1
echo -e "${YELLOW}[5/6] Building Fabric 1.21.1 version...${NC}"
FABRIC_1211_DIR="$SCRIPT_DIR/vonixcore-template-fabric-1.21.1"
if [ -d "$FABRIC_1211_DIR" ]; then
    ((BUILD_COUNT++)) || true
    cd "$FABRIC_1211_DIR"
    if ./gradlew build --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Fabric-1.21.1-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Fabric-1.21.1-$VERSION.jar${NC}"
        ((SUCCESS_COUNT++)) || true
    else
        echo -e "${RED}  FAIL Fabric 1.21.1 build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  SKIP Fabric 1.21.1 directory not found${NC}"
fi

echo ""

# Build Bukkit
echo -e "${YELLOW}[6/6] Building Bukkit version...${NC}"
BUKKIT_DIR="$SCRIPT_DIR/VonixCore-Bukkit-Universal"
if [ -d "$BUKKIT_DIR" ]; then
    ((BUILD_COUNT++)) || true
    cd "$BUKKIT_DIR"
    if ./gradlew shadowJar --no-daemon; then
        find build/libs -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" -exec cp {} "$OUTPUT_DIR/VonixCore-Bukkit-$VERSION.jar" \;
        echo -e "${GREEN}  OK Built: VonixCore-Bukkit-$VERSION.jar${NC}"
        ((SUCCESS_COUNT++)) || true
    else
        echo -e "${RED}  FAIL Bukkit build failed!${NC}"
    fi
    cd "$SCRIPT_DIR"
else
    echo -e "${RED}  SKIP Bukkit directory not found${NC}"
fi

echo ""
echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}             Build Complete!${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""
echo "Builds attempted: $BUILD_COUNT, Successful: $SUCCESS_COUNT"
echo ""
echo "Output files in: $OUTPUT_DIR"
echo ""
ls -lh "$OUTPUT_DIR"
