import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'Economy System - VonixCore Docs',
    description: 'Full-featured economy system in VonixCore with persistent balances, transactions, and admin controls.',
};

export default function EconomyPage() {
    return (
        <DocPageLayout
            title="Economy System"
            description="VonixCore includes a full-featured economy system with persistent balances, transactions, and admin controls."
            prevPage={{ title: 'Protection', href: '/docs/protection' }}
            nextPage={{ title: 'Discord', href: '/docs/discord' }}
        >
            {/* Overview */}
            <section>
                <h2 id="overview">📋 Overview</h2>
                <p>The economy system provides:</p>
                <ul>
                    <li>Persistent player balances (SQLite/MySQL)</li>
                    <li>Player-to-player payments</li>
                    <li>Balance leaderboard</li>
                    <li>Admin commands for managing economy</li>
                    <li>Configurable currency names and symbols</li>
                </ul>
            </section>

            {/* Configuration */}
            <section>
                <h2 id="configuration">⚙️ Configuration</h2>
                <p><strong>File:</strong> <code>vonixcore-essentials.toml</code></p>
                <CodeBlock
                    code={`[economy]
# Enable economy system
enabled = true

# Starting balance for new players
starting_balance = 100.0

# Currency symbol (displayed before amounts)
currency_symbol = "$"

# Currency name (singular)
currency_name = "Dollar"

# Currency name (plural)
currency_name_plural = "Dollars"

# Minimum transaction amount
min_transaction = 0.01

# Maximum balance a player can have (0 = unlimited)
max_balance = 0`}
                    language="toml"
                    filename="vonixcore-essentials.toml"
                />

                <h3>Example Configurations</h3>

                <p><strong>Default (Dollars)</strong></p>
                <CodeBlock
                    code={`currency_symbol = "$"
currency_name = "Dollar"
currency_name_plural = "Dollars"`}
                    language="toml"
                />

                <p><strong>Coins</strong></p>
                <CodeBlock
                    code={`currency_symbol = "⛃"
currency_name = "Coin"
currency_name_plural = "Coins"`}
                    language="toml"
                />

                <p><strong>Credits</strong></p>
                <CodeBlock
                    code={`currency_symbol = "¢"
currency_name = "Credit"
currency_name_plural = "Credits"`}
                    language="toml"
                />
            </section>

            {/* Player Commands */}
            <section>
                <h2 id="player-commands">💰 Player Commands</h2>

                <h3>Check Balance</h3>
                <CodeBlock code={`/balance
/bal`} language="bash" />
                <p>Shows your current balance.</p>

                <CodeBlock code={`/balance <player>
/bal <player>`} language="bash" />
                <p>Shows another player&apos;s balance (requires <code>vonixcore.balance.others</code>).</p>

                <h3>Pay Players</h3>
                <CodeBlock code={`/pay <player> <amount>`} language="bash" />
                <p>Send money to another player.</p>
                <p><strong>Example:</strong></p>
                <CodeBlock
                    code={`/pay Steve 100
> You paid Steve $100.00`}
                    language="text"
                />

                <h3>Balance Leaderboard</h3>
                <CodeBlock code={`/baltop
/baltop <page>`} language="bash" />
                <p>View the richest players on the server.</p>
            </section>

            {/* Admin Commands */}
            <section>
                <h2 id="admin-commands">🔧 Admin Commands</h2>
                <p>All admin commands require the <code>vonixcore.eco</code> permission.</p>

                <h3>Give Money</h3>
                <CodeBlock code={`/eco give <player> <amount>`} language="bash" />
                <p>Add money to a player&apos;s balance.</p>

                <h3>Take Money</h3>
                <CodeBlock code={`/eco take <player> <amount>`} language="bash" />
                <p>Remove money from a player&apos;s balance.</p>

                <h3>Set Balance</h3>
                <CodeBlock code={`/eco set <player> <amount>`} language="bash" />
                <p>Set a player&apos;s balance to a specific amount.</p>

                <h3>Reset Economy</h3>
                <CodeBlock code={`/eco reset <player>`} language="bash" />
                <p>Reset a player&apos;s balance to the starting amount.</p>
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
                                <th>Default</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>vonixcore.balance</code></td>
                                <td>Check own balance</td>
                                <td>All players</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.balance.others</code></td>
                                <td>Check others&apos; balance</td>
                                <td>OP</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.pay</code></td>
                                <td>Pay other players</td>
                                <td>All players</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.baltop</code></td>
                                <td>View leaderboard</td>
                                <td>All players</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.eco</code></td>
                                <td>Admin economy commands</td>
                                <td>OP</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.eco.give</code></td>
                                <td>Give money</td>
                                <td>OP</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.eco.take</code></td>
                                <td>Take money</td>
                                <td>OP</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.eco.set</code></td>
                                <td>Set balance</td>
                                <td>OP</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Database Storage */}
            <section>
                <h2 id="database">📊 Database Storage</h2>
                <p>Economy data is stored in the <code>vonixcore_economy</code> table:</p>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Column</th>
                                <th>Type</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>uuid</code></td>
                                <td>VARCHAR(36)</td>
                                <td>Player UUID (primary key)</td>
                            </tr>
                            <tr>
                                <td><code>username</code></td>
                                <td>VARCHAR(16)</td>
                                <td>Last known username</td>
                            </tr>
                            <tr>
                                <td><code>balance</code></td>
                                <td>DOUBLE</td>
                                <td>Current balance</td>
                            </tr>
                            <tr>
                                <td><code>last_transaction</code></td>
                                <td>BIGINT</td>
                                <td>Timestamp of last activity</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* API Integration */}
            <section>
                <h2 id="api">🔌 API Integration</h2>
                <p>For plugin developers, the economy can be accessed programmatically:</p>
                <CodeBlock
                    code={`EconomyManager eco = EconomyManager.getInstance();

// Get balance
double balance = eco.getBalance(playerUUID);

// Modify balance
eco.addBalance(playerUUID, 100.0);
eco.removeBalance(playerUUID, 50.0);
eco.setBalance(playerUUID, 1000.0);

// Check if player can afford
boolean canAfford = eco.hasBalance(playerUUID, 200.0);`}
                    language="java"
                />
            </section>

            {/* Tips */}
            <section>
                <h2 id="tips">💡 Tips</h2>
                <ol>
                    <li><strong>Starting Balance</strong>: Set appropriately for your server&apos;s economy scale</li>
                    <li><strong>Max Balance</strong>: Use to prevent economy inflation</li>
                    <li><strong>Currency Symbol</strong>: Keep short for cleaner display</li>
                    <li><strong>Regular Backups</strong>: Backup database for economy safety</li>
                </ol>
            </section>
        </DocPageLayout>
    );
}
