import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'XP Sync - VonixCore Docs',
    description: 'Synchronize player XP, playtime, and statistics with an external API for cross-server or website integration.',
};

export default function XPSyncPage() {
    return (
        <DocPageLayout
            title="XP Sync"
            description="Synchronize player XP, playtime, and statistics with an external API. Designed for the Vonix Network website but compatible with any REST API."
            prevPage={{ title: 'Authentication', href: '/docs/authentication' }}
        >
            {/* Overview */}
            <section>
                <h2 id="overview">📋 Overview</h2>
                <p>XP Sync provides:</p>
                <ul>
                    <li><strong>Automatic synchronization</strong> at configurable intervals</li>
                    <li><strong>Startup/shutdown sync</strong> for all player data</li>
                    <li><strong>Real-time sync</strong> on player join/leave</li>
                    <li><strong>Playtime tracking</strong> and optional health/position data</li>
                    <li><strong>Multi-server support</strong> with server identifiers</li>
                </ul>
            </section>

            {/* Configuration */}
            <section>
                <h2 id="configuration">⚙️ Configuration</h2>
                <p><strong>File:</strong> <code>vonixcore-xpsync.toml</code></p>
                <CodeBlock
                    code={`[xpsync]
# Enable XP synchronization
enabled = false

[api]
# API endpoint for syncing data
endpoint = "https://yoursite.com/api/minecraft/sync/xp"

# API key for authentication (KEEP SECRET!)
api_key = "YOUR_API_KEY_HERE"

# Server name for multi-server identification
server_name = "Server-1"

# Sync interval in seconds (default: 5 minutes)
sync_interval = 300

[data]
# Track and sync playtime
track_playtime = true

# Track and sync current health
track_health = true

# Track and sync hunger level
track_hunger = false

# Track and sync position (privacy implications!)
track_position = false

[advanced]
# Enable verbose debug logging
verbose_logging = false

# Connection timeout in milliseconds
connection_timeout = 10000

# Maximum retry attempts on failure
max_retries = 3`}
                    language="toml"
                    filename="vonixcore-xpsync.toml"
                />
            </section>

            {/* Sync Behavior */}
            <section>
                <h2 id="behavior">🔄 Sync Behavior</h2>

                <h3>When Data is Synced</h3>
                <ol>
                    <li><strong>Server Startup</strong>: Syncs ALL players from world data (NeoForge only)</li>
                    <li><strong>Regular Interval</strong>: Syncs all online players in batches (default: every 5 minutes)</li>
                    <li><strong>Server Shutdown</strong>: Syncs ALL players from world data (NeoForge only)</li>
                </ol>
                <blockquote>
                    <strong>Note:</strong> Per-player sync on join/leave was removed in v1.0.0 for performance optimization. All syncing now happens in batches at configured intervals, reducing API calls and server load.
                </blockquote>

                <h3>What Gets Synced</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Data</th>
                                <th>Config Option</th>
                                <th>Default</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td>UUID</td><td>Always</td><td>✅</td></tr>
                            <tr><td>Username</td><td>Always</td><td>✅</td></tr>
                            <tr><td>XP Level</td><td>Always</td><td>✅</td></tr>
                            <tr><td>Total XP</td><td>Always</td><td>✅</td></tr>
                            <tr><td>Playtime</td><td><code>track_playtime</code></td><td>✅</td></tr>
                            <tr><td>Health</td><td><code>track_health</code></td><td>✅</td></tr>
                            <tr><td>Hunger</td><td><code>track_hunger</code></td><td>❌</td></tr>
                            <tr><td>Position</td><td><code>track_position</code></td><td>❌</td></tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* API Specification */}
            <section>
                <h2 id="api">📊 API Specification</h2>

                <h3>Endpoint</h3>
                <CodeBlock
                    code={`POST {api_endpoint}
Authorization: Bearer {api_key}
Content-Type: application/json`}
                    language="http"
                />

                <h3>Request Payload</h3>
                <CodeBlock
                    code={`{
  "serverName": "Survival-1",
  "players": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "username": "Steve",
      "level": 30,
      "totalExperience": 1395,
      "playtimeSeconds": 36000,
      "currentHealth": 20.0
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "username": "Alex",
      "level": 15,
      "totalExperience": 315,
      "playtimeSeconds": 18000,
      "currentHealth": 18.5
    }
  ]
}`}
                    language="json"
                />

                <h3>Expected Response</h3>
                <CodeBlock
                    code={`{
  "success": true,
  "syncedCount": 2,
  "message": "Sync completed successfully"
}`}
                    language="json"
                />

                <h3>Error Response</h3>
                <CodeBlock
                    code={`{
  "success": false,
  "error": "Invalid API key"
}`}
                    language="json"
                />
            </section>

            {/* Authentication */}
            <section>
                <h2 id="authentication">🔒 Authentication</h2>
                <p>XP Sync uses Bearer token authentication:</p>
                <CodeBlock code={`Authorization: Bearer YOUR_API_KEY_HERE`} language="http" />

                <h3>Security Recommendations</h3>
                <ol>
                    <li><strong>Use HTTPS</strong>: Always use encrypted endpoints</li>
                    <li><strong>Rotate Keys</strong>: Periodically rotate API keys</li>
                    <li><strong>Restrict IPs</strong>: Whitelist server IPs if possible</li>
                    <li><strong>Monitor Usage</strong>: Watch for unusual sync patterns</li>
                </ol>
            </section>

            {/* Multi-Server Setup */}
            <section>
                <h2 id="multi-server">📈 Multi-Server Setup</h2>
                <p>For networks with multiple servers:</p>
                <p><strong>Server 1 Config:</strong></p>
                <CodeBlock code={`server_name = "Survival-1"`} language="toml" />
                <p><strong>Server 2 Config:</strong></p>
                <CodeBlock code={`server_name = "Creative"`} language="toml" />
                <p><strong>Server 3 Config:</strong></p>
                <CodeBlock code={`server_name = "SkyBlock"`} language="toml" />
                <p>The API can then aggregate data across all servers.</p>
            </section>

            {/* Debugging */}
            <section>
                <h2 id="debugging">🔍 Debugging</h2>
                <p>Enable verbose logging for troubleshooting:</p>
                <CodeBlock
                    code={`[advanced]
verbose_logging = true`}
                    language="toml"
                />

                <h3>Log Messages</h3>
                <p><strong>Successful sync:</strong></p>
                <CodeBlock code={`[XPSync] Successfully synced 15 players`} language="text" />
                <p><strong>Connection error:</strong></p>
                <CodeBlock code={`[XPSync] Cannot connect to API: https://yoursite.com/api/...`} language="text" />
                <p><strong>Authentication error:</strong></p>
                <CodeBlock code={`[XPSync] Authentication failed! Check your API key.`} language="text" />
                <p><strong>Timeout:</strong></p>
                <CodeBlock code={`[XPSync] Request timed out: https://yoursite.com/api/...`} language="text" />
            </section>

            {/* Offline Player Data */}
            <section>
                <h2 id="offline">💾 Offline Player Data</h2>
                <p>XP Sync reads offline player data from:</p>
                <ul>
                    <li><code>world/playerdata/*.dat</code> files</li>
                    <li><code>world/stats/*.json</code> files</li>
                </ul>
                <p>This allows syncing ALL players on startup/shutdown, not just online players.</p>
            </section>

            {/* Sample Website Integration */}
            <section>
                <h2 id="sample">📊 Sample Website Integration</h2>
                <p>Example PHP endpoint to receive XP data:</p>
                <CodeBlock
                    code={`<?php
// Verify API key
$headers = getallheaders();
$authHeader = $headers['Authorization'] ?? '';
if ($authHeader !== 'Bearer YOUR_API_KEY') {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Invalid API key']);
    exit;
}

// Parse request
$data = json_decode(file_get_contents('php://input'), true);
$serverName = $data['serverName'];
$players = $data['players'];

// Store in database
$pdo = new PDO('mysql:host=localhost;dbname=website', 'user', 'pass');
foreach ($players as $player) {
    $stmt = $pdo->prepare('
        INSERT INTO player_stats (uuid, username, server, level, xp, playtime)
        VALUES (?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE 
        username = VALUES(username),
        level = VALUES(level),
        xp = VALUES(xp),
        playtime = VALUES(playtime),
        updated_at = NOW()
    ');
    $stmt->execute([
        $player['uuid'],
        $player['username'],
        $serverName,
        $player['level'],
        $player['totalExperience'],
        $player['playtimeSeconds'] ?? 0
    ]);
}

echo json_encode([
    'success' => true,
    'syncedCount' => count($players)
]);`}
                    language="php"
                    showLineNumbers
                />
            </section>

            {/* Troubleshooting */}
            <section>
                <h2 id="troubleshooting">🛠️ Troubleshooting</h2>

                <h3>Sync Not Working</h3>
                <ol>
                    <li><strong>Check enabled</strong>: Ensure <code>enabled = true</code></li>
                    <li><strong>Check endpoint</strong>: Verify URL is correct and reachable</li>
                    <li><strong>Check API key</strong>: Ensure key is valid</li>
                    <li><strong>Check logs</strong>: Enable verbose logging</li>
                </ol>

                <h3>Players Not Appearing</h3>
                <ol>
                    <li><strong>Check sync interval</strong>: Data syncs at configured intervals (default 5 minutes)</li>
                    <li><strong>Wait for next batch</strong>: Players will sync with the next batch interval</li>
                    <li><strong>Check UUID format</strong>: Must be valid UUID string</li>
                </ol>

                <h3>High Latency</h3>
                <ol>
                    <li><strong>Increase timeout</strong>: Raise <code>connection_timeout</code></li>
                    <li><strong>Reduce frequency</strong>: Increase <code>sync_interval</code></li>
                    <li><strong>Optimize API</strong>: Check API server performance</li>
                </ol>
            </section>
        </DocPageLayout>
    );
}
