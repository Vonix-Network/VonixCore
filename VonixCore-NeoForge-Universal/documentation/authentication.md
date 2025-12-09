# VonixCore Authentication System

The authentication system provides secure login/register functionality for offline-mode (cracked) servers, with integration to the Vonix Network API.

---

## 📋 Overview

The authentication system is designed for:
- **Offline-mode servers** that need account verification
- **Vonix Network integration** for centralized account management
- **Session management** with configurable timeouts
- **Secure password storage** via external API

---

## ⚠️ Important Notes

> **This feature is designed for the Vonix Network ecosystem.**
> 
> If you're running a standard online-mode server with Mojang authentication, you do NOT need this feature.

---

## ⚙️ Configuration

Authentication is configured in `AuthConfig.java` constants:

```java
// API Configuration
API_BASE_URL = "https://vonix.network/api/minecraft/auth"
REGISTRATION_API_KEY = "YOUR_API_KEY"

// Session Settings
SESSION_TIMEOUT_MINUTES = 30
AUTO_LOGIN_ENABLED = true

// Behavior
FREEZE_UNAUTHENTICATED = true
DISABLE_COMMANDS_UNTIL_LOGIN = true
```

---

## 🔐 How It Works

### Registration Flow

1. New player joins the server
2. Player is frozen and prompted to register
3. Player runs `/register <password> <password>`
4. Password is sent to Vonix Network API
5. Account is created and player is authenticated

### Login Flow

1. Returning player joins the server
2. Player is frozen and prompted to login
3. Player runs `/login <password>`
4. Password is verified against Vonix Network API
5. Player is authenticated and can play

### Session Management

- Sessions are tracked by UUID + IP address
- Session timeout is configurable (default: 30 minutes)
- Auto-login can be enabled for returning sessions

---

## 🎮 Player Commands

### Register

```
/register <password> <confirm_password>
```

Create a new account with a password.

**Requirements:**
- Passwords must match
- Minimum 6 characters (configurable)
- Cannot contain spaces

### Login

```
/login <password>
```

Log in to an existing account.

**Features:**
- 3 attempts before timeout
- Configurable lockout period
- Session remembering

---

## 🔒 Security Features

### Unauthenticated Restrictions

While not logged in, players:
- **Cannot move** (frozen in place)
- **Cannot break/place blocks**
- **Cannot interact with entities**
- **Cannot use commands** (except /login, /register)
- **Cannot open inventories**
- **Cannot take damage**
- **Cannot chat**

### Password Security

- Passwords are **never stored locally**
- All verification happens via **HTTPS API calls**
- Passwords are **hashed server-side** by Vonix Network
- Session tokens are used after authentication

### Rate Limiting

- Maximum 3 login attempts per session
- 30-second cooldown after failed attempts
- IP-based tracking for abuse prevention

---

## 🌐 API Integration

VonixCore communicates with the Vonix Network API:

### Registration Endpoint
```
POST /api/minecraft/auth/register
{
  "uuid": "player-uuid",
  "username": "PlayerName",
  "password": "hashed-password",
  "ip": "player-ip"
}
```

### Login Endpoint
```
POST /api/minecraft/auth/login
{
  "uuid": "player-uuid",
  "password": "hashed-password",
  "ip": "player-ip"
}
```

### Response Format
```json
{
  "success": true,
  "token": "session-token",
  "message": "Login successful"
}
```

---

## 🔑 LuckPerms Integration

When a player authenticates successfully, VonixCore can:
- Sync donation ranks from the Vonix Network
- Apply permission groups automatically
- Update prefixes/suffixes based on rank

This requires LuckPerms to be installed.

---

## 💡 Tips

1. **Use HTTPS**: Ensure API communication is encrypted
2. **Strong Passwords**: Encourage players to use strong passwords
3. **Session Duration**: Balance security with convenience
4. **Backup API**: Have fallback if API is unavailable

---

## 🛠️ Troubleshooting

### Players Can't Register

1. Check API key is correct
2. Verify API endpoint is reachable
3. Check server logs for API errors

### Sessions Not Persisting

1. Check IP hasn't changed
2. Verify session timeout hasn't expired
3. Check database connectivity

### API Connection Failed

1. Verify internet connectivity
2. Check API endpoint URL
3. Ensure API key has proper permissions

---

## 🔗 Related Documentation

- [Configuration Guide](configuration.md)
- [Discord Integration](discord.md) (for linked accounts)
