import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'Permissions - VonixCore Docs',
    description: 'VonixCore permission system guide. Learn about the built-in permission system, LuckPerms integration, and all permission nodes.',
};

export default function PermissionsPage() {
    return (
        <DocPageLayout
            title="Permission System"
            description="VonixCore includes a complete permission system that works standalone or integrates with LuckPerms."
            prevPage={{ title: 'Commands', href: '/docs/commands' }}
            nextPage={{ title: 'Protection', href: '/docs/protection' }}
        >
            {/* LuckPerms Detection */}
            <section>
                <h2 id="detection">🔍 LuckPerms Detection</h2>
                <p>VonixCore automatically detects if LuckPerms is installed:</p>
                <ul>
                    <li><strong>LuckPerms installed</strong>: VonixCore uses LuckPerms for all permission checks, prefixes, and suffixes</li>
                    <li><strong>LuckPerms not installed</strong>: VonixCore uses its built-in permission system with SQLite storage</li>
                </ul>
                <p>You don&apos;t need to configure anything - detection is automatic at server startup.</p>
            </section>

            {/* Built-in System */}
            <section>
                <h2 id="builtin">📋 Built-in Permission System</h2>
                <p>When LuckPerms is not installed, VonixCore provides a full-featured permission system:</p>

                <h3>Features</h3>
                <ul>
                    <li><strong>Groups</strong> with hierarchical inheritance</li>
                    <li><strong>User-specific permissions</strong> that override group permissions</li>
                    <li><strong>Prefixes and Suffixes</strong> for chat formatting</li>
                    <li><strong>Weights</strong> for group priority</li>
                    <li><strong>Wildcard permissions</strong> (<code>vonixcore.*</code>, <code>*</code>)</li>
                    <li><strong>Negative permissions</strong> (set to false to deny)</li>
                </ul>

                <h3>Default Group</h3>
                <p>A <code>default</code> group is automatically created with no permissions. All new players are assigned to this group.</p>
            </section>

            {/* Permission Commands */}
            <section>
                <h2 id="commands">🔧 Permission Commands</h2>
                <p>All commands require OP level 3 or the <code>vonixcore.perm</code> permission.</p>

                <h3>User Commands</h3>
                <CodeBlock code={`/perm user <player> info`} language="bash" />
                <p>View player&apos;s groups, prefix, suffix, and permissions.</p>

                <CodeBlock code={`/perm user <player> group set <group>`} language="bash" />
                <p>Set the player&apos;s primary group.</p>

                <CodeBlock code={`/perm user <player> group add <group>`} language="bash" />
                <p>Add player to an additional group.</p>

                <CodeBlock code={`/perm user <player> group remove <group>`} language="bash" />
                <p>Remove player from a group.</p>

                <CodeBlock code={`/perm user <player> permission set <permission> <true|false>`} language="bash" />
                <p>Set a permission for the player. Use <code>false</code> to explicitly deny.</p>

                <CodeBlock code={`/perm user <player> permission unset <permission>`} language="bash" />
                <p>Remove a permission override from the player.</p>

                <CodeBlock code={`/perm user <player> permission check <permission>`} language="bash" />
                <p>Check if a player has a specific permission.</p>

                <CodeBlock code={`/perm user <player> meta setprefix <prefix>`} language="bash" />
                <p>Set the player&apos;s chat prefix. Use <code>&amp;</code> for color codes.</p>

                <CodeBlock code={`/perm user <player> meta setsuffix <suffix>`} language="bash" />
                <p>Set the player&apos;s chat suffix.</p>

                <h3>Group Commands</h3>
                <CodeBlock code={`/perm group <group> info`} language="bash" />
                <p>View group information.</p>

                <CodeBlock code={`/perm group <group> create`} language="bash" />
                <p>Create a new permission group.</p>

                <CodeBlock code={`/perm group <group> delete`} language="bash" />
                <p>Delete a group. Cannot delete the default group.</p>

                <CodeBlock code={`/perm group <group> permission set <permission> <true|false>`} language="bash" />
                <p>Set a permission for the group.</p>

                <CodeBlock code={`/perm group <group> meta setprefix <prefix>`} language="bash" />
                <p>Set the group&apos;s chat prefix.</p>

                <CodeBlock code={`/perm group <group> meta setweight <weight>`} language="bash" />
                <p>Set the group&apos;s weight (priority). Higher = more important.</p>

                <CodeBlock code={`/perm group <group> parent set <parent>`} language="bash" />
                <p>Set the group&apos;s parent for inheritance.</p>

                <CodeBlock code={`/perm listgroups`} language="bash" />
                <p>List all permission groups.</p>

                <h3>Aliases</h3>
                <ul>
                    <li><code>/lp</code> - Alias for <code>/perm</code></li>
                    <li><code>/permissions</code> - Alias for <code>/perm</code></li>
                </ul>
            </section>

            {/* Prefix and Suffix Formatting */}
            <section>
                <h2 id="formatting">🎨 Prefix and Suffix Formatting</h2>
                <p>Prefixes and suffixes support color codes:</p>

                <h3>Legacy Color Codes</h3>
                <p>Use <code>&amp;</code> followed by a color code:</p>
                <ul>
                    <li><code>&amp;0-9</code> and <code>&amp;a-f</code> for standard colors</li>
                    <li><code>&amp;l</code> = Bold, <code>&amp;o</code> = Italic, <code>&amp;n</code> = Underline</li>
                    <li><code>&amp;r</code> = Reset</li>
                </ul>

                <h3>Hex Colors</h3>
                <p>Use <code>&amp;#RRGGBB</code> for custom colors:</p>
                <ul>
                    <li><code>&amp;#FF5500</code> = Orange</li>
                    <li><code>&amp;#00FF00</code> = Green</li>
                </ul>

                <h3>Example</h3>
                <CodeBlock
                    code={`/perm group admin meta setprefix &c[Admin] 
/perm group vip meta setprefix &#FFD700[VIP]`}
                    language="bash"
                />
            </section>

            {/* Group Inheritance */}
            <section>
                <h2 id="inheritance">📊 Group Inheritance</h2>
                <p>Groups can inherit permissions from a parent group:</p>
                <CodeBlock
                    code={`/perm group admin parent set default
/perm group owner parent set admin`}
                    language="bash"
                />
                <p>This creates a hierarchy:</p>
                <ul>
                    <li><code>owner</code> inherits from <code>admin</code> inherits from <code>default</code></li>
                    <li>Child groups automatically have all parent permissions</li>
                </ul>

                <h3>Priority (Weight)</h3>
                <p>When a player is in multiple groups, the group with the highest weight determines prefix/suffix:</p>
                <CodeBlock
                    code={`/perm group default meta setweight 1
/perm group vip meta setweight 10
/perm group admin meta setweight 100`}
                    language="bash"
                />
            </section>

            {/* Permission Nodes */}
            <section>
                <h2 id="nodes">🔑 Permission Nodes</h2>

                <h3>Core Permissions</h3>
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
                                <td><code>vonixcore.*</code></td>
                                <td>All VonixCore permissions</td>
                            </tr>
                            <tr>
                                <td><code>vonixcore.perm</code></td>
                                <td>Access permission commands</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Teleportation</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td><code>vonixcore.tp</code></td><td>Teleport to players</td></tr>
                            <tr><td><code>vonixcore.tp.others</code></td><td>Teleport other players</td></tr>
                            <tr><td><code>vonixcore.tphere</code></td><td>Teleport players to you</td></tr>
                            <tr><td><code>vonixcore.tpall</code></td><td>Teleport all players</td></tr>
                            <tr><td><code>vonixcore.tppos</code></td><td>Teleport to coordinates</td></tr>
                            <tr><td><code>vonixcore.rtp</code></td><td>Random teleport</td></tr>
                            <tr><td><code>vonixcore.setspawn</code></td><td>Set world spawn</td></tr>
                            <tr><td><code>vonixcore.tpa</code></td><td>TPA requests</td></tr>
                            <tr><td><code>vonixcore.tpahere</code></td><td>TPA here requests</td></tr>
                        </tbody>
                    </table>
                </div>

                <h3>Homes</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td><code>vonixcore.home</code></td><td>Use home commands</td></tr>
                            <tr><td><code>vonixcore.sethome</code></td><td>Set homes</td></tr>
                            <tr><td><code>vonixcore.sethome.multiple.&lt;n&gt;</code></td><td>Set n number of homes</td></tr>
                            <tr><td><code>vonixcore.delhome</code></td><td>Delete homes</td></tr>
                        </tbody>
                    </table>
                </div>

                <h3>Economy</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td><code>vonixcore.balance</code></td><td>Check balance</td></tr>
                            <tr><td><code>vonixcore.balance.others</code></td><td>Check others&apos; balance</td></tr>
                            <tr><td><code>vonixcore.pay</code></td><td>Pay other players</td></tr>
                            <tr><td><code>vonixcore.baltop</code></td><td>View balance leaderboard</td></tr>
                            <tr><td><code>vonixcore.eco</code></td><td>Admin economy commands</td></tr>
                        </tbody>
                    </table>
                </div>

                <h3>Server Management</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td><code>vonixcore.broadcast</code></td><td>Broadcast messages</td></tr>
                            <tr><td><code>vonixcore.gc</code></td><td>View server stats</td></tr>
                            <tr><td><code>vonixcore.lag</code></td><td>View TPS</td></tr>
                            <tr><td><code>vonixcore.invsee</code></td><td>View inventories</td></tr>
                            <tr><td><code>vonixcore.enderchest</code></td><td>Open ender chests</td></tr>
                            <tr><td><code>vonixcore.enderchest.others</code></td><td>Open others&apos; ender chests</td></tr>
                            <tr><td><code>vonixcore.workbench</code></td><td>Virtual workbench</td></tr>
                            <tr><td><code>vonixcore.anvil</code></td><td>Virtual anvil</td></tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Quick Setup Example */}
            <section>
                <h2 id="quicksetup">💡 Quick Setup Example</h2>
                <p>Set up a basic permission structure:</p>
                <CodeBlock
                    code={`# Create groups
/perm group default create
/perm group vip create
/perm group admin create

# Set up inheritance
/perm group vip parent set default
/perm group admin parent set vip

# Set weights
/perm group default meta setweight 1
/perm group vip meta setweight 10
/perm group admin meta setweight 100

# Set prefixes
/perm group default meta setprefix §7[Member] 
/perm group vip meta setprefix §6[VIP] 
/perm group admin meta setprefix §c[Admin] 

# Grant permissions
/perm group default permission set vonixcore.home true
/perm group default permission set vonixcore.tpa true
/perm group vip permission set vonixcore.sethome.multiple.5 true
/perm group admin permission set vonixcore.* true

# Add a player to admin
/perm user Steve group set admin`}
                    language="bash"
                    showLineNumbers
                />
            </section>
        </DocPageLayout>
    );
}
