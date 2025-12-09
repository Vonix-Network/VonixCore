# Contributing to VonixCore

Thank you for your interest in contributing to VonixCore! This document provides guidelines and information for contributors.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Making Changes](#making-changes)
- [Submitting Changes](#submitting-changes)
- [Style Guidelines](#style-guidelines)

---

## 📜 Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help maintain a welcoming environment for all contributors

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (for NeoForge development)
- **Java 17+** (for Paper/Bukkit development)
- **Gradle 9.x** (included via wrapper)
- **Git**
- An IDE (IntelliJ IDEA recommended)

### Cloning the Repository

```bash
git clone https://github.com/Vonix-Network/VonixCore.git
cd VonixCore
```

---

## 🔧 Development Setup

### Building All Platforms

**Windows:**
```powershell
.\build-all.ps1
```

**Linux/macOS:**
```bash
chmod +x build-all.sh
./build-all.sh
```

### Building Individual Platforms

```bash
# NeoForge
cd VonixCore-NeoForge-Universal
./gradlew build

# Paper
cd VonixCore-Paper-Universal
./gradlew fatJar

# Bukkit
cd VonixCore-Bukkit-Universal
./gradlew fatJar
```

### IDE Setup (IntelliJ IDEA)

1. Open the root `VonixCore` folder
2. Import each subproject as a Gradle project
3. Let Gradle sync dependencies
4. For NeoForge: Run `./gradlew genIntellijRuns` to generate run configurations

---

## 📁 Project Structure

```
VonixCore/
├── VonixCore-NeoForge-Universal/   # NeoForge mod (1.20.2-1.21.x)
│   ├── src/main/java/              # Java source code
│   ├── src/main/resources/         # Resources (mods.toml, etc.)
│   └── build.gradle                # Gradle build config
│
├── VonixCore-Paper-Universal/      # Paper plugin (1.18.2-1.21.x)
│   ├── src/main/java/              # Java source code
│   ├── src/main/resources/         # Resources (plugin.yml, etc.)
│   └── build.gradle                # Gradle build config
│
├── VonixCore-Bukkit-Universal/     # Bukkit/Spigot plugin
│   ├── src/main/java/              # Java source code
│   ├── src/main/resources/         # Resources (plugin.yml, etc.)
│   └── build.gradle                # Gradle build config
│
├── documentation/                  # User documentation
├── BuildOutput/                    # Compiled JARs (after build)
├── README.md                       # Main README
├── CHANGELOG.md                    # Version history
├── CONTRIBUTING.md                 # This file
└── build-all.ps1/sh               # Build scripts
```

---

## ✏️ Making Changes

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring

### Commit Messages

Use clear, descriptive commit messages:

```
feat: Add /fly command with permission check
fix: Resolve database connection leak on shutdown
docs: Update configuration guide for Turso
refactor: Optimize XP sync batch processing
```

### Cross-Platform Considerations

When adding new features, consider:

1. **NeoForge** uses Forge's event system and `Component` for text
2. **Paper/Bukkit** uses Bukkit's event system and legacy `ChatColor`
3. **Database code** should be identical across platforms
4. **Config structure** should be consistent

---

## 📤 Submitting Changes

### Pull Request Process

1. **Fork** the repository
2. **Create** a feature branch from `main`
3. **Make** your changes with clear commits
4. **Test** on at least one platform
5. **Push** to your fork
6. **Open** a Pull Request

### PR Requirements

- [ ] Code compiles without errors on all platforms
- [ ] New features are documented
- [ ] Existing tests pass
- [ ] Code follows style guidelines
- [ ] PR description explains the changes

---

## 📝 Style Guidelines

### Java Style

- **4 spaces** for indentation (no tabs)
- **UTF-8** encoding
- **Descriptive** variable and method names
- **Javadoc** for public methods
- **Consistent** with existing code style

### Example

```java
/**
 * Teleports a player to their home location.
 *
 * @param player The player to teleport
 * @param homeName The name of the home
 * @return true if teleport was successful
 */
public boolean teleportToHome(Player player, String homeName) {
    Home home = homeManager.getHome(player.getUniqueId(), homeName);
    if (home == null) {
        player.sendMessage("Home not found!");
        return false;
    }
    
    player.teleport(home.getLocation());
    return true;
}
```

### Documentation Style

- Use **Markdown** formatting
- Include **code examples** where helpful
- Keep docs **up to date** with code changes
- Use **tables** for command/permission lists

---

## 💬 Getting Help

- **Discord**: [Vonix Network Discord](https://discord.gg/vonix)
- **Issues**: [GitHub Issues](https://github.com/Vonix-Network/VonixCore/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Vonix-Network/VonixCore/discussions)

---

## 🙏 Thank You!

Every contribution helps make VonixCore better for the entire community. Whether it's code, documentation, bug reports, or feature suggestions – we appreciate your help!
