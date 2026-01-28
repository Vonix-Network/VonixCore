Based on the comprehensive gap analysis between `VonixCore-Forge-1.20.1` and `VonixCore-Template-Forge-1.18.2`, I have identified significant architectural and feature discrepancies. The 1.18.2 version is missing approximately 60% of the codebase, including critical safety systems, entire gameplay modules, and the GUI framework.

Here is the step-by-step implementation plan to achieve 1:1 parity:

# VonixCore 1.18.2 Backport Implementation Plan

## Phase 1: Core Infrastructure & Critical Safety (Teleportation)
The current 1.18.2 teleport system is unsafe and rudimentary. We must port the advanced RTP engine.
1.  **Implement Support Structures**: Create `LocationSearchEngine`, `ChunkLoadingManager`, and `SafetyValidationEngine`.
2.  **Port OptimizedRTPManager**: Backport the async teleport logic, adapting 1.20.1 `CompletableFuture` chains to 1.18.2's `ServerLevel` API.
3.  **API Adaptation**: Map 1.20.1 `BlockState` and `Holder<Biome>` lookups to 1.18.2 equivalents.

## Phase 2: Missing Feature Modules
Entire packages are missing and need to be ported and registered.
1.  **Jobs System**:
    - Port `network.vonix.vonixcore.jobs` package.
    - Implement `JobsManager`, `PlayerJob`, and `Job` classes.
    - Register `JobsCommands` and database schemas.
2.  **Kits System**:
    - Port `network.vonix.vonixcore.kits` package.
    - Implement `KitManager` and cooldown tracking logic.
3.  **Permissions System**:
    - Port `network.vonix.vonixcore.permissions` package.
    - Implement `PermissionManager` to handle group-based access.

## Phase 3: Economy & GUI System
The 1.18.2 version lacks the visual shop interface.
1.  **GUI Framework**: Implement `ShopMenu` (extending `ChestMenu`) and `ShopGUIManager`.
2.  **Interaction Handling**: Port `ShopEventListener` to intercept inventory clicks and prevent item theft.
3.  **Command Integration**: Update `ShopCommands` to open these GUIs instead of sending text messages.

## Phase 4: Event Handling & Logic Parity
1.  **Listener Expansion**: Create missing listeners:
    - `PlayerEventListener` (Chat formatting, logging)
    - `BlockEventListener` (Protection)
    - `EntityEventListener` (Combat logging, death events)
    - `ExtendedProtectionListener`
2.  **Main Class Update**: Update `VonixCore.java` to initialize these new managers and listeners dynamically based on config, matching 1.20.1's modular structure.

## Phase 5: Quality Assurance
1.  **Unit Tests**: Port the test suite (currently 0 tests in 1.18.2) to ensure algorithms work correctly.
2.  **Verification**: Verify database table creation for all new modules and test GUI interactions.
