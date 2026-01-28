I have analyzed the differences between the Forge and Fabric versions. The Forge version includes a sophisticated system for parsing Discord embeds (Advancements, Join/Leave/Death events) and converting them into rich Minecraft chat components, which is currently simplified or missing in the Fabric version.

To make the Fabric version a 1:1 copy of the Forge functionality, I will:

1.  **Port the Core Data & Logic Classes**:
    I will create the following classes in `network.vonix.viscord` (adapted from Forge):
    *   **Data Models**: `AdvancementType.java`, `AdvancementData.java`, `EventData.java`, `ExtractionException.java`
    *   **Configuration**: `ServerPrefixConfig.java` (for handling multi-server prefixes)
    *   **Detectors**: `AdvancementEmbedDetector.java`, `EventEmbedDetector.java` (to identify embed types)
    *   **Extractors**: `AdvancementDataExtractor.java`, `EventDataExtractor.java` (to parse data from embeds)
    *   **Builder**: `VanillaComponentBuilder.java` (to create rich Minecraft chat components)

2.  **Refactor `DiscordManager.java`**:
    I will significantly update `DiscordManager.java` to:
    *   Initialize and use the new detectors, extractors, and builders.
    *   Replace the simple `parseEventEmbed` method with the robust pipeline from Forge.
    *   Implement the full `processJavacordMessage` logic, including specific handling for advancements, events, and fallback strategies.
    *   Integrate `ServerPrefixConfig` for consistent server prefix handling.
    *   Ensure all configuration accesses match the Fabric `Config` class structure.

3.  **Verify Configuration**:
    *   Ensure `Config.java` in Fabric supports all necessary fields (it appears mostly complete, but I will verify integration).

This approach ensures that Discord embed processing (Discord -> Minecraft) will behave exactly as it does in the Forge version, providing the same visual output and reliability.