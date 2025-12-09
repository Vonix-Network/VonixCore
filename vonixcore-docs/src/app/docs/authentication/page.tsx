import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'Authentication System - VonixCore Docs',
    description: 'Secure login/register functionality for offline-mode (cracked) servers with API integration.',
};

export default function AuthenticationPage() {
    return (
        <DocPageLayout
            title="Authentication System"
            description="The authentication system provides secure login/register functionality for offline-mode (cracked) servers, with integration to the Vonix Network API."
            prevPage={{ title: 'Discord', href: '/docs/discord' }}
            nextPage={{ title: 'XP Sync', href: '/docs/xpsync' }}
        >
            {/* Overview */}
            <section>
                <h2 id="overview">📋 Overview</h2>
                <p>The authentication system is designed for:</p>
                <ul>
                    <li><strong>Offline-mode servers</strong> that need account verification</li>
                    <li><strong>Vonix Network integration</strong> for centralized account management</li>
                    <li><strong>Session management</strong> with configurable timeouts</li>
                    <li><strong>Secure password storage</strong> via external API</li>
                </ul>
            </section>

            {/* Important Notes */}
            <section>
                <h2 id="notes">⚠️ Important Notes</h2>
                <blockquote>
                    <strong>This feature is designed for the Vonix Network ecosystem.</strong>
                    <br /><br />
                    If you&apos;re running a standard online-mode server with Mojang authentication, you do NOT need this feature.
                </blockquote>
            </section>

            {/* Configuration */}
            <section>
                <h2 id="configuration">⚙️ Configuration</h2>
                <p>Authentication is configured in <code>AuthConfig.java</code> constants:</p>
                <CodeBlock
                    code={`// API Configuration
API_BASE_URL = "https://vonix.network/api/minecraft/auth"
REGISTRATION_API_KEY = "YOUR_API_KEY"

// Session Settings
SESSION_TIMEOUT_MINUTES = 30
AUTO_LOGIN_ENABLED = true

// Behavior
FREEZE_UNAUTHENTICATED = true
DISABLE_COMMANDS_UNTIL_LOGIN = true`}
                    language="java"
                />
            </section>

            {/* How It Works */}
            <section>
                <h2 id="how-it-works">🔐 How It Works</h2>

                <h3>Registration Flow</h3>
                <ol>
                    <li>New player joins the server</li>
                    <li>Player is frozen and prompted to register</li>
                    <li>Player runs <code>/register &lt;password&gt; &lt;password&gt;</code></li>
                    <li>Password is sent to Vonix Network API</li>
                    <li>Account is created and player is authenticated</li>
                </ol>

                <h3>Login Flow</h3>
                <ol>
                    <li>Returning player joins the server</li>
                    <li>Player is frozen and prompted to login</li>
                    <li>Player runs <code>/login &lt;password&gt;</code></li>
                    <li>Password is verified against Vonix Network API</li>
                    <li>Player is authenticated and can play</li>
                </ol>

                <h3>Session Management</h3>
                <ul>
                    <li>Sessions are tracked by UUID + IP address</li>
                    <li>Session timeout is configurable (default: 30 minutes)</li>
                    <li>Auto-login can be enabled for returning sessions</li>
                </ul>
            </section>

            {/* Player Commands */}
            <section>
                <h2 id="commands">🎮 Player Commands</h2>

                <h3>Register</h3>
                <CodeBlock code={`/register <password> <confirm_password>`} language="bash" />
                <p>Create a new account with a password.</p>
                <p><strong>Requirements:</strong></p>
                <ul>
                    <li>Passwords must match</li>
                    <li>Minimum 6 characters (configurable)</li>
                    <li>Cannot contain spaces</li>
                </ul>

                <h3>Login</h3>
                <CodeBlock code={`/login <password>`} language="bash" />
                <p>Log in to an existing account.</p>
                <p><strong>Features:</strong></p>
                <ul>
                    <li>3 attempts before timeout</li>
                    <li>Configurable lockout period</li>
                    <li>Session remembering</li>
                </ul>
            </section>

            {/* Security Features */}
            <section>
                <h2 id="security">🔒 Security Features</h2>

                <h3>Unauthenticated Restrictions</h3>
                <p>While not logged in, players:</p>
                <ul>
                    <li><strong>Cannot move</strong> (frozen in place)</li>
                    <li><strong>Cannot break/place blocks</strong></li>
                    <li><strong>Cannot interact with entities</strong></li>
                    <li><strong>Cannot use commands</strong> (except /login, /register)</li>
                    <li><strong>Cannot open inventories</strong></li>
                    <li><strong>Cannot take damage</strong></li>
                    <li><strong>Cannot chat</strong></li>
                </ul>

                <h3>Password Security</h3>
                <ul>
                    <li>Passwords are <strong>never stored locally</strong></li>
                    <li>All verification happens via <strong>HTTPS API calls</strong></li>
                    <li>Passwords are <strong>hashed server-side</strong> by Vonix Network</li>
                    <li>Session tokens are used after authentication</li>
                </ul>

                <h3>Rate Limiting</h3>
                <ul>
                    <li>Maximum 3 login attempts per session</li>
                    <li>30-second cooldown after failed attempts</li>
                    <li>IP-based tracking for abuse prevention</li>
                </ul>
            </section>

            {/* API Integration */}
            <section>
                <h2 id="api">🌐 API Integration</h2>
                <p>VonixCore communicates with the Vonix Network API:</p>

                <h3>Registration Endpoint</h3>
                <CodeBlock
                    code={`POST /api/minecraft/auth/register
{
  "uuid": "player-uuid",
  "username": "PlayerName",
  "password": "hashed-password",
  "ip": "player-ip"
}`}
                    language="json"
                />

                <h3>Login Endpoint</h3>
                <CodeBlock
                    code={`POST /api/minecraft/auth/login
{
  "uuid": "player-uuid",
  "password": "hashed-password",
  "ip": "player-ip"
}`}
                    language="json"
                />

                <h3>Response Format</h3>
                <CodeBlock
                    code={`{
  "success": true,
  "token": "session-token",
  "message": "Login successful"
}`}
                    language="json"
                />
            </section>

            {/* LuckPerms Integration */}
            <section>
                <h2 id="luckperms">🔑 LuckPerms Integration</h2>
                <p>When a player authenticates successfully, VonixCore can:</p>
                <ul>
                    <li>Sync donation ranks from the Vonix Network</li>
                    <li>Apply permission groups automatically</li>
                    <li>Update prefixes/suffixes based on rank</li>
                </ul>
                <p>This requires LuckPerms to be installed.</p>
            </section>

            {/* Tips */}
            <section>
                <h2 id="tips">💡 Tips</h2>
                <ol>
                    <li><strong>Use HTTPS</strong>: Ensure API communication is encrypted</li>
                    <li><strong>Strong Passwords</strong>: Encourage players to use strong passwords</li>
                    <li><strong>Session Duration</strong>: Balance security with convenience</li>
                    <li><strong>Backup API</strong>: Have fallback if API is unavailable</li>
                </ol>
            </section>

            {/* Troubleshooting */}
            <section>
                <h2 id="troubleshooting">🛠️ Troubleshooting</h2>

                <h3>Players Can&apos;t Register</h3>
                <ol>
                    <li>Check API key is correct</li>
                    <li>Verify API endpoint is reachable</li>
                    <li>Check server logs for API errors</li>
                </ol>

                <h3>Sessions Not Persisting</h3>
                <ol>
                    <li>Check IP hasn&apos;t changed</li>
                    <li>Verify session timeout hasn&apos;t expired</li>
                    <li>Check database connectivity</li>
                </ol>

                <h3>API Connection Failed</h3>
                <ol>
                    <li>Verify internet connectivity</li>
                    <li>Check API endpoint URL</li>
                    <li>Ensure API key has proper permissions</li>
                </ol>
            </section>
        </DocPageLayout>
    );
}
