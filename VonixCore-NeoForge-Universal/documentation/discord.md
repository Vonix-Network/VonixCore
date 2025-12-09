# VonixCore Discord Integration

Bridge your Minecraft server with Discord for chat relay, event notifications, and account linking.

---

## 📋 Overview

The Discord integration provides:
- **Chat Relay**: Sync messages between Discord and Minecraft
- **Event Notifications**: Player joins, leaves, deaths, achievements
- **Account Linking**: Connect Minecraft and Discord accounts
- **Console Output**: Optional console log streaming to Discord

---

## 🚀 Setup Guide

### Step 1: Create a Discord Bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"** and give it a name
3. Go to the **"Bot"** section
4. Click **"Add Bot"**
5. Under **"Privileged Gateway Intents"**, enable:
   - **Message Content Intent**
   - **Server Members Intent**
6. Click **"Reset Token"** and copy your bot token

### Step 2: Invite the Bot

1. Go to the **"OAuth2"** section
2. Under **"URL Generator"**, select:
   - Scopes: `bot`, `applications.commands`
   - Permissions: `Send Messages`, `Read Message History`, `Embed Links`
3. Copy the generated URL and open it in your browser
4. Select your Discord server and authorize

### Step 3: Configure VonixCore

Edit `config/vonixcore-discord.toml`:

```toml
[discord]
enabled = true
bot_token = "YOUR_BOT_TOKEN_HERE"

# Right-click the channel > Copy ID
chat_channel_id = "123456789012345678"
```

### Step 4: Restart Server

Restart your Minecraft server. You should see:
```
[VonixCore] Discord bot connected as YourBotName#1234
```

---

## ⚙️ Configuration

**File:** `vonixcore-discord.toml`

```toml
[discord]
# Enable Discord integration
enabled = false

# Bot token (KEEP SECRET!)
bot_token = "YOUR_BOT_TOKEN_HERE"

# Channel for chat relay
chat_channel_id = "000000000000000000"

# Optional: Console log channel (admin only)
console_channel_id = ""

# Debug logging
debug_logging = false

[discord.messages]
# Minecraft -> Discord format
# Variables: {player}, {message}
minecraft_to_discord = "**{player}**: {message}"

# Discord -> Minecraft format
# Variables: {user}, {message}
discord_to_minecraft = "&9[Discord] &b{user}&r: {message}"

# Server start message
server_start = "🟢 Server is starting..."

# Server stop message
server_stop = "🔴 Server is shutting down..."

[discord.events]
# Announce player joins
announce_join = true
join_message = "📥 **{player}** joined the server"

# Announce player leaves
announce_leave = true
leave_message = "📤 **{player}** left the server"

# Announce deaths
announce_death = true

# Announce advancements
announce_advancement = true
advancement_message = "🏆 **{player}** earned **{advancement}**"

[discord.account_linking]
# Enable account linking
enabled = true

# Link code expiration (seconds)
link_code_expiry = 300

# Required role ID for linked accounts (optional)
linked_role_id = ""
```

---

## 💬 Chat Relay

### Minecraft to Discord

Messages sent in Minecraft are relayed to the configured Discord channel:

**In Minecraft:**
```
<Steve> Hello everyone!
```

**In Discord:**
```
Steve: Hello everyone!
```

### Discord to Minecraft

Messages in the Discord channel are relayed to Minecraft:

**In Discord:**
```
JohnDoe: Hey server!
```

**In Minecraft:**
```
[Discord] JohnDoe: Hey server!
```

---

## 🔗 Account Linking

### How It Works

1. **In Minecraft**: Run `/discord link`
2. You receive a 6-digit code (valid for 5 minutes)
3. **In Discord**: Send `/link <code>` or `!link <code>`
4. Accounts are now linked!

### Benefits of Linking
- Synced display names
- Role assignment in Discord
- Linked account identification

### Commands

**Minecraft:**
- `/discord link` - Generate a link code
- `/discord unlink` - Unlink your account

**Discord:**
- `/link <code>` - Link with a code
- `/unlink` - Unlink your account
- `/whois <minecraft_name>` - View linked account

---

## 📢 Event Notifications

### Player Join/Leave
```
📥 Steve joined the server
📤 Steve left the server
```

### Deaths
```
💀 Steve was slain by Zombie
```

### Advancements
```
🏆 Steve earned Getting an Upgrade
```

### Server Status
```
🟢 Server is starting...
🔴 Server is shutting down...
```

---

## 🔑 Permissions

| Permission | Description |
|------------|-------------|
| `vonixcore.discord.link` | Link Discord account |
| `vonixcore.discord.unlink` | Unlink Discord account |

---

## 🛠️ Troubleshooting

### Bot Not Connecting

1. **Check token**: Ensure the bot token is correct
2. **Check intents**: Verify all required intents are enabled
3. **Check logs**: Look for error messages in server console

### Messages Not Relaying

1. **Check channel ID**: Verify the channel ID is correct
2. **Check permissions**: Bot needs `Send Messages` and `Read Message History`
3. **Check enabled**: Ensure `enabled = true`

### Link Codes Not Working

1. **Check expiry**: Codes expire after 5 minutes
2. **Check format**: Code should be 6 digits
3. **Check enabled**: Ensure `account_linking.enabled = true`

---

## 💡 Tips

1. **Separate Channels**: Use different channels for chat and console
2. **Role Sync**: Assign a role to linked players for verification
3. **Webhook Alternative**: For high-traffic, consider webhooks
4. **Rate Limiting**: Discord has rate limits; don't spam messages

---

## 🔗 Related Documentation

- [Configuration Guide](configuration.md)
- [Commands Reference](commands.md)
