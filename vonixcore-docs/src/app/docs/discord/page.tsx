import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'Discord Integration - VonixCore Docs',
    description: 'Bridge your Minecraft server with Discord for chat relay, event notifications, and account linking.',
};

export default function DiscordPage() {
    return (
        <DocPageLayout
            title="Discord Integration"
            description="Bridge your Minecraft server with Discord for chat relay, event notifications, and account linking."
            prevPage={{ title: 'Economy', href: '/docs/economy' }}
            nextPage={{ title: 'Authentication', href: '/docs/authentication' }}
        >
            {/* Overview */}
            <section>
                <h2 id="overview">📋 Overview</h2>
                <p>The Discord integration provides:</p>
                <ul>
                    <li><strong>Chat Relay</strong>: Sync messages between Discord and Minecraft</li>
                    <li><strong>Event Notifications</strong>: Player joins, leaves, deaths, achievements</li>
                    <li><strong>Account Linking</strong>: Connect Minecraft and Discord accounts</li>
                    <li><strong>Console Output</strong>: Optional console log streaming to Discord</li>
                </ul>
            </section>

            {/* Setup Guide */}
            <section>
                <h2 id="setup">🚀 Setup Guide</h2>

                <h3>Step 1: Create a Discord Bot</h3>
                <ol>
                    <li>Go to the <a href="https://discord.com/developers/applications" target="_blank" rel="noopener noreferrer">Discord Developer Portal</a></li>
                    <li>Click <strong>&quot;New Application&quot;</strong> and give it a name</li>
                    <li>Go to the <strong>&quot;Bot&quot;</strong> section</li>
                    <li>Click <strong>&quot;Add Bot&quot;</strong></li>
                    <li>Under <strong>&quot;Privileged Gateway Intents&quot;</strong>, enable:
                        <ul>
                            <li><strong>Message Content Intent</strong></li>
                            <li><strong>Server Members Intent</strong></li>
                        </ul>
                    </li>
                    <li>Click <strong>&quot;Reset Token&quot;</strong> and copy your bot token</li>
                </ol>

                <h3>Step 2: Invite the Bot</h3>
                <ol>
                    <li>Go to the <strong>&quot;OAuth2&quot;</strong> section</li>
                    <li>Under <strong>&quot;URL Generator&quot;</strong>, select:
                        <ul>
                            <li>Scopes: <code>bot</code>, <code>applications.commands</code></li>
                            <li>Permissions: <code>Send Messages</code>, <code>Read Message History</code>, <code>Embed Links</code></li>
                        </ul>
                    </li>
                    <li>Copy the generated URL and open it in your browser</li>
                    <li>Select your Discord server and authorize</li>
                </ol>

                <h3>Step 3: Configure VonixCore</h3>
                <p>Edit <code>config/vonixcore-discord.toml</code>:</p>
                <CodeBlock
                    code={`[discord]
enabled = true
bot_token = "YOUR_BOT_TOKEN_HERE"

# Right-click the channel > Copy ID
chat_channel_id = "123456789012345678"`}
                    language="toml"
                    filename="vonixcore-discord.toml"
                />

                <h3>Step 4: Restart Server</h3>
                <p>Restart your Minecraft server. You should see:</p>
                <CodeBlock code={`[VonixCore] Discord bot connected as YourBotName#1234`} language="text" />
            </section>

            {/* Configuration */}
            <section>
                <h2 id="configuration">⚙️ Configuration</h2>
                <p><strong>File:</strong> <code>vonixcore-discord.toml</code></p>
                <CodeBlock
                    code={`[discord]
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
discord_to_minecraft = "§9[Discord] §b{user}§r: {message}"

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
linked_role_id = ""`}
                    language="toml"
                    filename="vonixcore-discord.toml"
                />
            </section>

            {/* Chat Relay */}
            <section>
                <h2 id="chat-relay">💬 Chat Relay</h2>

                <h3>Minecraft to Discord</h3>
                <p>Messages sent in Minecraft are relayed to the configured Discord channel:</p>
                <p><strong>In Minecraft:</strong></p>
                <CodeBlock code={`<Steve> Hello everyone!`} language="text" />
                <p><strong>In Discord:</strong></p>
                <CodeBlock code={`Steve: Hello everyone!`} language="text" />

                <h3>Discord to Minecraft</h3>
                <p>Messages in the Discord channel are relayed to Minecraft:</p>
                <p><strong>In Discord:</strong></p>
                <CodeBlock code={`JohnDoe: Hey server!`} language="text" />
                <p><strong>In Minecraft:</strong></p>
                <CodeBlock code={`[Discord] JohnDoe: Hey server!`} language="text" />
            </section>

            {/* Account Linking */}
            <section>
                <h2 id="linking">🔗 Account Linking</h2>

                <h3>How It Works</h3>
                <ol>
                    <li><strong>In Minecraft</strong>: Run <code>/discord link</code></li>
                    <li>You receive a 6-digit code (valid for 5 minutes)</li>
                    <li><strong>In Discord</strong>: Send <code>/link &lt;code&gt;</code> or <code>!link &lt;code&gt;</code></li>
                    <li>Accounts are now linked!</li>
                </ol>

                <h3>Benefits of Linking</h3>
                <ul>
                    <li>Synced display names</li>
                    <li>Role assignment in Discord</li>
                    <li>Linked account identification</li>
                </ul>

                <h3>Commands</h3>
                <p><strong>Minecraft:</strong></p>
                <ul>
                    <li><code>/vonix discord link</code> - Generate a link code</li>
                    <li><code>/vonix discord unlink</code> - Unlink your account</li>
                    <li><code>/vonix discord messages &lt;enable|disable&gt;</code> - Toggle Discord messages from other servers</li>
                    <li><code>/vonix discord events &lt;enable|disable&gt;</code> - Toggle Discord event notifications</li>
                    <li><code>/vonix discord help</code> - Show all Discord commands</li>
                </ul>
                <p><strong>Discord:</strong></p>
                <ul>
                    <li><code>/link &lt;code&gt;</code> - Link with a code</li>
                    <li><code>/unlink</code> - Unlink your account</li>
                    <li><code>/list</code> - Show online players</li>
                </ul>
            </section>

            {/* Message Filtering */}
            <section>
                <h2 id="filtering">🔇 Message Filtering</h2>

                <h3>Server Messages</h3>
                <p>In multi-server setups, you can filter messages from other Minecraft servers using VonixCore:</p>
                <CodeBlock code={`/vonix discord messages disable`} language="text" />
                <p>This will hide chat messages from other servers while still showing messages from your current server.</p>

                <h3>Event Notifications</h3>
                <p>You can also toggle event notifications (joins, leaves, deaths, advancements) from appearing in your chat:</p>
                <CodeBlock code={`/vonix discord events disable`} language="text" />
                <p>These events will still be sent to Discord, but won&apos;t appear in your Minecraft chat.</p>

                <h3>Check Current Status</h3>
                <p>To see your current filter settings, run the commands without arguments:</p>
                <CodeBlock code={`/vonix discord messages
/vonix discord events`} language="text" />
            </section>

            {/* Event Notifications */}
            <section>
                <h2 id="events">📢 Event Notifications</h2>
                <h3>Player Join/Leave</h3>
                <CodeBlock code={`📥 Steve joined the server
📤 Steve left the server`} language="text" />

                <h3>Deaths</h3>
                <CodeBlock code={`💀 Steve was slain by Zombie`} language="text" />

                <h3>Advancements</h3>
                <CodeBlock code={`🏆 Steve earned Getting an Upgrade`} language="text" />

                <h3>Server Status</h3>
                <CodeBlock code={`🟢 Server is starting...
🔴 Server is shutting down...`} language="text" />
            </section>

            {/* Permissions */}
            <section>
                <h2 id="permissions">🔑 Permissions</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>vonixcore.discord.link</code></td>
                                <td>Link Discord account</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.discord.unlink</code></td>
                                <td>Unlink Discord account</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Troubleshooting */}
            <section>
                <h2 id="troubleshooting">🛠️ Troubleshooting</h2>

                <h3>Bot Not Connecting</h3>
                <ol>
                    <li><strong>Check token</strong>: Ensure the bot token is correct</li>
                    <li><strong>Check intents</strong>: Verify all required intents are enabled</li>
                    <li><strong>Check logs</strong>: Look for error messages in server console</li>
                </ol>

                <h3>Messages Not Relaying</h3>
                <ol>
                    <li><strong>Check channel ID</strong>: Verify the channel ID is correct</li>
                    <li><strong>Check permissions</strong>: Bot needs <code>Send Messages</code> and <code>Read Message History</code></li>
                    <li><strong>Check enabled</strong>: Ensure <code>enabled = true</code></li>
                </ol>

                <h3>Link Codes Not Working</h3>
                <ol>
                    <li><strong>Check expiry</strong>: Codes expire after 5 minutes</li>
                    <li><strong>Check format</strong>: Code should be 6 digits</li>
                    <li><strong>Check enabled</strong>: Ensure <code>account_linking.enabled = true</code></li>
                </ol>
            </section>

            {/* Tips */}
            <section>
                <h2 id="tips">💡 Tips</h2>
                <ol>
                    <li><strong>Separate Channels</strong>: Use different channels for chat and console</li>
                    <li><strong>Role Sync</strong>: Assign a role to linked players for verification</li>
                    <li><strong>Webhook Alternative</strong>: For high-traffic, consider webhooks</li>
                    <li><strong>Rate Limiting</strong>: Discord has rate limits; don&apos;t spam messages</li>
                </ol>
            </section>
        </DocPageLayout>
    );
}
