import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'Protection System - VonixCore Docs',
    description: 'CoreProtect-style block logging and rollback system in VonixCore.',
};

export default function ProtectionPage() {
    return (
        <DocPageLayout
            title="Protection System"
            description="The Protection System provides CoreProtect-style block logging and rollback capabilities to protect your server from griefing."
            prevPage={{ title: 'Permissions', href: '/docs/permissions' }}
            nextPage={{ title: 'Economy', href: '/docs/economy' }}
        >
            {/* Overview */}
            <section>
                <h2 id="overview">📋 Overview</h2>
                <p>The protection system logs:</p>
                <ul>
                    <li>Block breaks and placements</li>
                    <li>Container interactions (chests, furnaces, etc.)</li>
                    <li>Entity kills (optional)</li>
                    <li>Player interactions</li>
                </ul>
                <p>All data is stored in the database and can be queried or rolled back.</p>
            </section>

            {/* Configuration */}
            <section>
                <h2 id="configuration">⚙️ Configuration</h2>
                <p><strong>File:</strong> <code>vonixcore-protection.toml</code></p>
                <CodeBlock
                    code={`[protection]
# Enable the protection module
enabled = true

# Days to keep log data (0 = forever)
log_retention_days = 30

# Maximum results per lookup
max_lookup_results = 1000

# Log container (chest) interactions
log_containers = true

# Log entity kills
log_entity_kills = false

# Batch size for async database writes
batch_size = 100`}
                    language="toml"
                    filename="vonixcore-protection.toml"
                />
            </section>

            {/* Lookup Commands */}
            <section>
                <h2 id="lookup">🔍 Lookup Commands</h2>

                <h3>Basic Lookup</h3>
                <CodeBlock code={`/co lookup <parameters>`} language="bash" />
                <p>View block history at a location or by player.</p>

                <h3>Parameters</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Parameter</th>
                                <th>Description</th>
                                <th>Example</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>u:&lt;user&gt;</code></td>
                                <td>Filter by username</td>
                                <td><code>u:Steve</code></td>
                            </tr>
                            <tr>
                                <td><code>t:&lt;time&gt;</code></td>
                                <td>Time range</td>
                                <td><code>t:1d</code> (1 day), <code>t:3h</code> (3 hours)</td>
                            </tr>
                            <tr>
                                <td><code>r:&lt;radius&gt;</code></td>
                                <td>Radius around you</td>
                                <td><code>r:10</code></td>
                            </tr>
                            <tr>
                                <td><code>a:&lt;action&gt;</code></td>
                                <td>Action type</td>
                                <td><code>a:break</code>, <code>a:place</code>, <code>a:container</code></td>
                            </tr>
                            <tr>
                                <td><code>b:&lt;block&gt;</code></td>
                                <td>Block type</td>
                                <td><code>b:diamond_ore</code></td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Examples</h3>
                <CodeBlock
                    code={`# Who broke blocks near me in the last hour?
/co lookup r:5 t:1h a:break

# What did Steve do in the last day?
/co lookup u:Steve t:1d

# Who placed TNT in the last week?
/co lookup t:7d a:place b:tnt`}
                    language="bash"
                />
            </section>

            {/* Rollback Commands */}
            <section>
                <h2 id="rollback">⏪ Rollback Commands</h2>

                <h3>Rollback</h3>
                <CodeBlock code={`/co rollback <parameters>`} language="bash" />
                <p>Undo changes matching the specified parameters.</p>

                <h3>Examples</h3>
                <CodeBlock
                    code={`# Rollback Steve's changes in the last 12 hours within 20 blocks
/co rollback u:Steve t:12h r:20

# Rollback all TNT explosions in the last day
/co rollback t:1d a:explosion r:50

# Rollback block breaks only
/co rollback u:Griefer t:6h r:30 a:break`}
                    language="bash"
                />

                <h3>Restore</h3>
                <CodeBlock code={`/co restore <parameters>`} language="bash" />
                <p>Redo a rollback (restore changes that were rolled back).</p>
                <CodeBlock code={`/co restore u:Steve t:12h r:20`} language="bash" />
            </section>

            {/* Inspector Tool */}
            <section>
                <h2 id="inspector">🔧 Inspector Tool</h2>

                <h3>Toggle Inspector</h3>
                <CodeBlock code={`/co inspect`} language="bash" />
                <p>or</p>
                <CodeBlock code={`/co i`} language="bash" />
                <p>Toggles the block inspector. When enabled, left-clicking or right-clicking a block shows its history.</p>

                <h3>Inspector Output</h3>
                <CodeBlock
                    code={`[VonixCore] Block history for Stone at (100, 64, -200):
- Steve placed 2 hours ago
- Alex broke 6 hours ago
- Steve placed 1 day ago`}
                    language="text"
                />
            </section>

            {/* Time Format */}
            <section>
                <h2 id="time">📊 Time Format</h2>
                <p>The time parameter supports various formats:</p>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Format</th>
                                <th>Meaning</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td><code>1s</code></td><td>1 second</td></tr>
                            <tr><td><code>30m</code></td><td>30 minutes</td></tr>
                            <tr><td><code>6h</code></td><td>6 hours</td></tr>
                            <tr><td><code>1d</code></td><td>1 day</td></tr>
                            <tr><td><code>2w</code></td><td>2 weeks</td></tr>
                            <tr><td><code>1mo</code></td><td>1 month</td></tr>
                        </tbody>
                    </table>
                </div>
                <p>You can combine them:</p>
                <ul>
                    <li><code>1d12h</code> = 1 day and 12 hours</li>
                    <li><code>2w3d</code> = 2 weeks and 3 days</li>
                </ul>
            </section>

            {/* Action Types */}
            <section>
                <h2 id="actions">🎯 Action Types</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Action</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td><code>break</code></td><td>Block breaks</td></tr>
                            <tr><td><code>place</code></td><td>Block placements</td></tr>
                            <tr><td><code>container</code></td><td>Chest/container access</td></tr>
                            <tr><td><code>click</code></td><td>Block interactions</td></tr>
                            <tr><td><code>kill</code></td><td>Entity kills</td></tr>
                            <tr><td><code>explosion</code></td><td>Explosion damage</td></tr>
                        </tbody>
                    </table>
                </div>
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
                            <tr><td><code>vonixcore.protection.lookup</code></td><td>Use lookup commands</td></tr>
                            <tr><td><code>vonixcore.protection.rollback</code></td><td>Use rollback commands</td></tr>
                            <tr><td><code>vonixcore.protection.restore</code></td><td>Use restore commands</td></tr>
                            <tr><td><code>vonixcore.protection.inspect</code></td><td>Use inspector tool</td></tr>
                            <tr><td><code>vonixcore.protection.*</code></td><td>All protection permissions</td></tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Best Practices */}
            <section>
                <h2 id="tips">💡 Best Practices</h2>
                <ol>
                    <li><strong>Regular Purge</strong>: Set <code>log_retention_days</code> to prevent database bloat</li>
                    <li><strong>Backup First</strong>: Always backup before large rollbacks</li>
                    <li><strong>Test Radius</strong>: Start with small radius for complex rollbacks</li>
                    <li><strong>Use Inspector First</strong>: Check block history before rollback</li>
                </ol>
            </section>
        </DocPageLayout>
    );
}
