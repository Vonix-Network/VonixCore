# Requirements Document

## Introduction

The VonixCore-Forge-1.20.1 mod currently displays Discord advancement messages as ugly Discord-style embeds in Minecraft chat, which creates visual inconsistency with vanilla advancement messages. This feature will convert Discord advancement embeds to vanilla-style advancement messages with proper formatting, hover effects, and server identification.

## Glossary

- **Discord_Manager**: The component responsible for processing Discord messages via Javacord
- **Advancement_Embed**: Discord embed messages with titles like "Advancement Made" containing player advancement information
- **Vanilla_Advancement_Component**: Minecraft's native text component format for displaying advancement messages
- **Server_Prefix**: Text identifier showing which Discord server the advancement originated from
- **Hover_Text**: Interactive text that appears when players hover over advancement messages

## Requirements

### Requirement 1: Discord Advancement Detection

**User Story:** As a player, I want the system to automatically detect advancement messages from Discord, so that they can be processed differently from regular event message embeds from other servers.

#### Acceptance Criteria

1. WHEN a Discord webhook message contains an embed with advancement-related footer text, THE Discord_Manager SHALL identify it as an advancement embed
2. WHEN processing Discord messages, THE Discord_Manager SHALL distinguish advancement embeds from regular chat embeds by checking footer content
3. WHEN an advancement embed is detected via footer analysis, THE Discord_Manager SHALL extract the embed fields for further processing
4. IF a Discord message contains multiple embeds, THE Discord_Manager SHALL process each advancement embed separately based on footer detection

### Requirement 2: Advancement Data Extraction

**User Story:** As a system component, I want to extract player name, advancement title, and description from Discord embeds, so that I can reconstruct the advancement information in vanilla format.

#### Acceptance Criteria

1. WHEN processing an advancement embed, THE Discord_Manager SHALL extract the player name from the embed fields
2. WHEN processing an advancement embed, THE Discord_Manager SHALL extract the advancement title from the embed fields
3. WHEN processing an advancement embed, THE Discord_Manager SHALL extract the advancement description from the embed fields
4. IF any required field is missing from the advancement embed, THE Discord_Manager SHALL log an error and skip processing that embed
5. WHEN extraction is complete, THE Discord_Manager SHALL validate that all required data is present and non-empty

### Requirement 3: Vanilla Advancement Component Generation

**User Story:** As a player, I want advancement messages to appear in vanilla Minecraft format with proper colors and styling, so that they match the visual consistency of regular advancement messages.

#### Acceptance Criteria

1. WHEN generating advancement components, THE Discord_Manager SHALL create text components using vanilla advancement formatting
2. WHEN formatting advancement text, THE Discord_Manager SHALL apply the same color scheme as vanilla advancement messages
3. WHEN creating advancement components, THE Discord_Manager SHALL include hover text containing the advancement description
4. THE Vanilla_Advancement_Component SHALL maintain the same visual structure as native Minecraft advancement messages
5. WHEN displaying advancement components, THE Discord_Manager SHALL ensure proper text styling matches vanilla advancement appearance

### Requirement 4: Server Identification

**User Story:** As a player, I want to know which Discord server an advancement came from, so that I can identify the source of cross-server advancement notifications.

#### Acceptance Criteria

1. WHEN processing advancement embeds, THE Discord_Manager SHALL determine the source Discord server
2. WHEN generating advancement components, THE Discord_Manager SHALL prepend a server prefix to identify the source
3. THE Server_Prefix SHALL be visually distinct from the advancement message content
4. WHEN multiple servers send advancement messages, THE Discord_Manager SHALL use different prefixes for each server
5. THE Server_Prefix SHALL be configurable to allow customization of server identification format

### Requirement 5: Message Integration

**User Story:** As a player, I want converted advancement messages to appear seamlessly in chat, so that they integrate naturally with the existing chat flow.

#### Acceptance Criteria

1. WHEN advancement components are generated, THE Discord_Manager SHALL send them to the Minecraft chat system
2. WHEN displaying converted advancement messages, THE Discord_Manager SHALL ensure they appear in the correct chat channel
3. THE Discord_Manager SHALL replace the original Discord embed with the converted vanilla advancement message
4. WHEN processing advancement embeds, THE Discord_Manager SHALL prevent duplicate messages from appearing in chat
5. THE Discord_Manager SHALL maintain the original timestamp and ordering of advancement messages

### Requirement 6: Error Handling and Validation

**User Story:** As a system administrator, I want robust error handling for malformed advancement embeds, so that the system continues functioning even with unexpected Discord message formats.

#### Acceptance Criteria

1. WHEN an advancement embed has malformed or missing fields, THE Discord_Manager SHALL log the error and continue processing other messages
2. WHEN Discord API errors occur during embed processing, THE Discord_Manager SHALL handle them gracefully without crashing
3. IF advancement data extraction fails, THE Discord_Manager SHALL fall back to displaying the original Discord embed
4. WHEN validation errors occur, THE Discord_Manager SHALL provide detailed error messages for debugging
5. THE Discord_Manager SHALL maintain system stability even when processing invalid or corrupted advancement embeds