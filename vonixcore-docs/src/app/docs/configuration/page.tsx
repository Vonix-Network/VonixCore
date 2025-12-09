import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';
import { Database, Server, Cloud, Zap, Settings } from 'lucide-react';

export const metadata: Metadata = {
    title: 'Configuration Guide - VonixCore Docs',
    description: 'Complete configuration guide for VonixCore. Learn how to set up database, protection, essentials, discord, and XP sync.',
};

export default function ConfigurationPage() {
    return (
        <DocPageLayout
            title="Configuration Guide"
            description="This guide covers all configuration files and their options. VonixCore uses a modular configuration system where each feature has its own config file."
            nextPage={{ title: 'Commands Reference', href: '/docs/commands' }}
        >
            {/* Config Files Overview */}
            <section>
                <h2 id="overview">📁 Configuration Files Overview</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 not-prose my-6">
                    {[
                        { file: 'vonixcore-database.toml', desc: 'Database connection and storage', icon: Database },
                        { file: 'vonixcore-protection.toml', desc: 'Block logging and rollback', icon: Server },
                        { file: 'vonixcore-essentials.toml', desc: 'Homes, warps, economy, kits', icon: Settings },
                        { file: 'vonixcore-discord.toml', desc: 'Discord integration', icon: Cloud },
                        { file: 'vonixcore-xpsync.toml', desc: 'XP synchronization', icon: Zap },
                    ].map((item) => {
                        const Icon = item.icon;
                        return (
                            <div key={item.file} className="flex items-center gap-3 p-4 rounded-lg glass-card">
                                <Icon className="h-5 w-5 text-primary flex-shrink-0" />
                                <div>
                                    <code className="text-sm text-primary">{item.file}</code>
                                    <p className="text-sm text-muted-foreground">{item.desc}</p>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </section>

            {/* Database Configuration */}
            <section>
                <h2 id="database">🗄️ Database Configuration</h2>
                <p>
                    <strong>File:</strong> <code>vonixcore-database.toml</code>
                </p>
                <p>
                    VonixCore stores <strong>all data in a single database</strong> and supports multiple database backends:
                </p>

                <div className="overflow-x-auto my-6">
                    <table>
                        <thead>
                            <tr>
                                <th>Type</th>
                                <th>Description</th>
                                <th>Best For</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>sqlite</code></td>
                                <td>Local file (default)</td>
                                <td>Single servers, simplicity</td>
                            </tr>
                            <tr>
                                <td><code>mysql</code></td>
                                <td>MySQL/MariaDB server</td>
                                <td>Multi-server networks</td>
                            </tr>
                            <tr>
                                <td><code>postgresql</code></td>
                                <td>PostgreSQL server</td>
                                <td>Advanced features, scale</td>
                            </tr>
                            <tr>
                                <td><code>turso</code></td>
                                <td>Turso LibSQL edge DB</td>
                                <td>Global edge, low latency</td>
                            </tr>
                            <tr>
                                <td><code>supabase</code></td>
                                <td>Supabase PostgreSQL</td>
                                <td>Serverless, web integration</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Database Type Selection</h3>
                <CodeBlock
                    code={`[database]
# Options: sqlite, mysql, postgresql, turso, supabase
type = "sqlite"`}
                    language="toml"
                    filename="vonixcore-database.toml"
                />
            </section>

            {/* SQLite Configuration */}
            <section>
                <h3 id="sqlite">SQLite Configuration (Default)</h3>
                <p>Best for single-server setups. All data stored in one file.</p>
                <CodeBlock
                    code={`[sqlite]
# Database file (stored in world/vonixcore/)
file = "vonixcore.db"`}
                    language="toml"
                />
                <p>
                    <strong>Location:</strong> <code>&lt;world&gt;/vonixcore/vonixcore.db</code>
                </p>
            </section>

            {/* MySQL Configuration */}
            <section>
                <h3 id="mysql">MySQL/MariaDB Configuration</h3>
                <p>For networks or servers requiring a dedicated database server.</p>
                <CodeBlock
                    code={`[mysql]
host = "localhost"
port = 3306
database = "vonixcore"
username = "root"
password = "your_password"
ssl = false`}
                    language="toml"
                />
                <blockquote>
                    <strong>Required:</strong> Create the database first:
                    <CodeBlock code={`CREATE DATABASE vonixcore;`} language="sql" />
                </blockquote>
            </section>

            {/* PostgreSQL Configuration */}
            <section>
                <h3 id="postgresql">PostgreSQL Configuration</h3>
                <p>For servers requiring PostgreSQL&apos;s advanced features.</p>
                <CodeBlock
                    code={`[postgresql]
host = "localhost"
port = 5432
database = "vonixcore"
username = "postgres"
password = "your_password"
ssl = false`}
                    language="toml"
                />
            </section>

            {/* Turso Configuration */}
            <section>
                <h3 id="turso">Turso Configuration (LibSQL Edge Database)</h3>
                <p>
                    <a href="https://turso.tech" target="_blank" rel="noopener noreferrer">Turso</a> provides globally distributed SQLite-compatible databases.
                </p>
                <CodeBlock
                    code={`[turso]
# Your Turso database URL
url = "libsql://your-database.turso.io"

# Auth token from Turso dashboard (KEEP SECRET!)
auth_token = "your_auth_token"`}
                    language="toml"
                />
                <p><strong>Setup:</strong></p>
                <ol>
                    <li>Create account at <a href="https://turso.tech" target="_blank" rel="noopener noreferrer">turso.tech</a></li>
                    <li>Create a database: <code>turso db create vonixcore</code></li>
                    <li>Get URL: <code>turso db show vonixcore --url</code></li>
                    <li>Get token: <code>turso db tokens create vonixcore</code></li>
                </ol>
            </section>

            {/* Supabase Configuration */}
            <section>
                <h3 id="supabase">Supabase Configuration</h3>
                <p>
                    <a href="https://supabase.com" target="_blank" rel="noopener noreferrer">Supabase</a> provides hosted PostgreSQL with a free tier.
                </p>
                <CodeBlock
                    code={`[supabase]
# Your project's database host
host = "db.xxxxxxxxxxxx.supabase.co"

# Port (5432 for direct, 6543 for pooled)
port = 5432

# Database name (usually 'postgres')
database = "postgres"

# Database password from Supabase dashboard (KEEP SECRET!)
password = "your_database_password"`}
                    language="toml"
                />
                <p><strong>Setup:</strong></p>
                <ol>
                    <li>Create project at <a href="https://supabase.com" target="_blank" rel="noopener noreferrer">supabase.com</a></li>
                    <li>Go to <strong>Settings → Database</strong></li>
                    <li>Copy host, port, and password</li>
                </ol>
            </section>

            {/* Connection Pool Settings */}
            <section>
                <h3 id="pool">Connection Pool Settings</h3>
                <p>Applies to all database types:</p>
                <CodeBlock
                    code={`[pool]
# Maximum connections (5-10 small, 10-20 large)
max_connections = 10

# Connection timeout in milliseconds
timeout_ms = 5000`}
                    language="toml"
                />
            </section>

            {/* Performance Tuning */}
            <section>
                <h3 id="performance">Performance Tuning</h3>
                <CodeBlock
                    code={`[performance]
# Records to batch before writing
batch_size = 500

# Delay between batch writes (ms)
batch_delay_ms = 500

# Auto-purge data older than X days (0 = never)
purge_days = 30`}
                    language="toml"
                />
            </section>

            {/* Protection Configuration */}
            <section>
                <h2 id="protection">🛡️ Protection Configuration</h2>
                <p>
                    <strong>File:</strong> <code>vonixcore-protection.toml</code>
                </p>
                <p>
                    See <a href="/docs/protection">Protection System</a> for detailed usage.
                </p>
                <CodeBlock
                    code={`[protection]
enabled = true
log_retention_days = 30
max_lookup_results = 1000
log_containers = true
log_entity_kills = false
batch_size = 100`}
                    language="toml"
                />
            </section>

            {/* Essentials Configuration */}
            <section>
                <h2 id="essentials">🏠 Essentials Configuration</h2>
                <p>
                    <strong>File:</strong> <code>vonixcore-essentials.toml</code>
                </p>
                <CodeBlock
                    code={`[essentials]
enabled = true

[homes]
enabled = true
default_max_homes = 3
cooldown = 5

[warps]
enabled = true
cooldown = 3

[economy]
enabled = true
starting_balance = 100.0
currency_symbol = "$"
currency_name = "Dollar"
currency_name_plural = "Dollars"

[tpa]
enabled = true
request_timeout = 60
cooldown = 30`}
                    language="toml"
                />
            </section>

            {/* Discord Configuration */}
            <section>
                <h2 id="discord">📱 Discord Configuration</h2>
                <p>
                    <strong>File:</strong> <code>vonixcore-discord.toml</code>
                </p>
                <p>
                    See <a href="/docs/discord">Discord Integration</a> for setup.
                </p>
                <CodeBlock
                    code={`[discord]
enabled = false
bot_token = "YOUR_BOT_TOKEN"
chat_channel_id = "000000000000000000"

[discord.messages]
minecraft_to_discord = "**{player}**: {message}"
discord_to_minecraft = "§9[Discord] §b{user}§r: {message}"

[discord.events]
announce_join = true
announce_leave = true
announce_death = true

[discord.account_linking]
enabled = true
link_code_expiry = 300`}
                    language="toml"
                />
            </section>

            {/* XP Sync Configuration */}
            <section>
                <h2 id="xpsync">📊 XP Sync Configuration</h2>
                <p>
                    <strong>File:</strong> <code>vonixcore-xpsync.toml</code>
                </p>
                <p>
                    See <a href="/docs/xpsync">XP Sync</a> for API details.
                </p>
                <CodeBlock
                    code={`[xpsync]
enabled = false

[api]
endpoint = "https://yoursite.com/api/minecraft/sync/xp"
api_key = "YOUR_API_KEY"
server_name = "Server-1"
sync_interval = 300

[data]
track_playtime = true
track_health = true
track_hunger = false
track_position = false

[advanced]
verbose_logging = false
connection_timeout = 10000
max_retries = 3`}
                    language="toml"
                />
            </section>

            {/* Tips */}
            <section>
                <h2 id="tips">💡 Tips</h2>
                <ol>
                    <li><strong>Always restart</strong> after changing config files</li>
                    <li><strong>Backup database</strong> before switching database types</li>
                    <li><strong>Test locally</strong> before production changes</li>
                    <li><strong>Secure credentials</strong> - never share config files with passwords</li>
                </ol>
            </section>
        </DocPageLayout>
    );
}
