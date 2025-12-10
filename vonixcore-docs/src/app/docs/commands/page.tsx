import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';
import { CodeBlock } from '@/components/code-block';

export const metadata: Metadata = {
    title: 'Commands Reference - VonixCore Docs',
    description: 'Complete reference of all commands available in VonixCore, organized by category.',
};

export default function CommandsPage() {
    return (
        <DocPageLayout
            title="Commands Reference"
            description="Complete reference of all commands available in VonixCore. Commands are organized by category."
            prevPage={{ title: 'Configuration', href: '/docs/configuration' }}
            nextPage={{ title: 'Permissions', href: '/docs/permissions' }}
        >
            {/* Teleportation Commands */}
            <section>
                <h2 id="teleportation">📍 Teleportation Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/tp &lt;player&gt;</code></td>
                                <td><code>vonixcore.tp</code></td>
                                <td>Teleport to a player</td>
                            </tr>
                            <tr>
                                <td><code>/tp &lt;player&gt; &lt;target&gt;</code></td>
                                <td><code>vonixcore.tp.others</code></td>
                                <td>Teleport a player to another player</td>
                            </tr>
                            <tr>
                                <td><code>/tphere &lt;player&gt;</code></td>
                                <td><code>vonixcore.tphere</code></td>
                                <td>Teleport a player to you</td>
                            </tr>
                            <tr>
                                <td><code>/tpall</code></td>
                                <td><code>vonixcore.tpall</code></td>
                                <td>Teleport all players to you</td>
                            </tr>
                            <tr>
                                <td><code>/tppos &lt;x&gt; &lt;y&gt; &lt;z&gt;</code></td>
                                <td><code>vonixcore.tppos</code></td>
                                <td>Teleport to coordinates</td>
                            </tr>
                            <tr>
                                <td><code>/rtp</code></td>
                                <td><code>vonixcore.rtp</code></td>
                                <td>Random teleport to a safe location</td>
                            </tr>
                            <tr>
                                <td><code>/setspawn</code></td>
                                <td><code>vonixcore.setspawn</code></td>
                                <td>Set the world spawn point</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>TPA (Teleport Request) Commands</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/tpa &lt;player&gt;</code></td>
                                <td><code>vonixcore.tpa</code></td>
                                <td>Request to teleport to a player</td>
                            </tr>
                            <tr>
                                <td><code>/tpahere &lt;player&gt;</code></td>
                                <td><code>vonixcore.tpahere</code></td>
                                <td>Request a player to teleport to you</td>
                            </tr>
                            <tr>
                                <td><code>/tpaccept</code></td>
                                <td><code>vonixcore.tpa</code></td>
                                <td>Accept a teleport request</td>
                            </tr>
                            <tr>
                                <td><code>/tpdeny</code></td>
                                <td><code>vonixcore.tpa</code></td>
                                <td>Deny a teleport request</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Home Commands */}
            <section>
                <h2 id="homes">🏠 Home Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/home [name]</code></td>
                                <td><code>vonixcore.home</code></td>
                                <td>Teleport to your home</td>
                            </tr>
                            <tr>
                                <td><code>/sethome [name]</code></td>
                                <td><code>vonixcore.sethome</code></td>
                                <td>Set a home at your location</td>
                            </tr>
                            <tr>
                                <td><code>/delhome &lt;name&gt;</code></td>
                                <td><code>vonixcore.delhome</code></td>
                                <td>Delete a home</td>
                            </tr>
                            <tr>
                                <td><code>/homes</code></td>
                                <td><code>vonixcore.home</code></td>
                                <td>List all your homes</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Home Limits</h3>
                <ul>
                    <li>Default players get 3 homes</li>
                    <li>Configure with permission: <code>vonixcore.sethome.multiple.&lt;count&gt;</code></li>
                    <li>Example: <code>vonixcore.sethome.multiple.10</code> allows 10 homes</li>
                </ul>
            </section>

            {/* Warp Commands */}
            <section>
                <h2 id="warps">🚀 Warp Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/warp &lt;name&gt;</code></td>
                                <td><code>vonixcore.warp</code></td>
                                <td>Teleport to a warp</td>
                            </tr>
                            <tr>
                                <td><code>/setwarp &lt;name&gt;</code></td>
                                <td><code>vonixcore.setwarp</code></td>
                                <td>Create a new warp</td>
                            </tr>
                            <tr>
                                <td><code>/delwarp &lt;name&gt;</code></td>
                                <td><code>vonixcore.delwarp</code></td>
                                <td>Delete a warp</td>
                            </tr>
                            <tr>
                                <td><code>/warps</code></td>
                                <td><code>vonixcore.warp</code></td>
                                <td>List all warps</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Economy Commands */}
            <section>
                <h2 id="economy">💰 Economy Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/balance</code> or <code>/bal</code></td>
                                <td><code>vonixcore.balance</code></td>
                                <td>Check your balance</td>
                            </tr>
                            <tr>
                                <td><code>/balance &lt;player&gt;</code></td>
                                <td><code>vonixcore.balance.others</code></td>
                                <td>Check another player&apos;s balance</td>
                            </tr>
                            <tr>
                                <td><code>/pay &lt;player&gt; &lt;amount&gt;</code></td>
                                <td><code>vonixcore.pay</code></td>
                                <td>Pay another player</td>
                            </tr>
                            <tr>
                                <td><code>/baltop [page]</code></td>
                                <td><code>vonixcore.baltop</code></td>
                                <td>View richest players</td>
                            </tr>
                            <tr>
                                <td><code>/eco give &lt;player&gt; &lt;amount&gt;</code></td>
                                <td><code>vonixcore.eco</code></td>
                                <td>Give money to a player</td>
                            </tr>
                            <tr>
                                <td><code>/eco take &lt;player&gt; &lt;amount&gt;</code></td>
                                <td><code>vonixcore.eco</code></td>
                                <td>Take money from a player</td>
                            </tr>
                            <tr>
                                <td><code>/eco set &lt;player&gt; &lt;amount&gt;</code></td>
                                <td><code>vonixcore.eco</code></td>
                                <td>Set a player&apos;s balance</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Shop Commands */}
            <section>
                <h2 id="shops">🛒 Shop Commands</h2>

                <h3>GUI Shop Commands</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/shop</code></td>
                                <td><code>vonixcore.shop</code></td>
                                <td>Open admin/server shop GUI</td>
                            </tr>
                            <tr>
                                <td><code>/shop server</code></td>
                                <td><code>vonixcore.shop</code></td>
                                <td>Open server shop GUI</td>
                            </tr>
                            <tr>
                                <td><code>/shop player</code></td>
                                <td><code>vonixcore.shop</code></td>
                                <td>Open player market GUI</td>
                            </tr>
                            <tr>
                                <td><code>/shop player sell</code></td>
                                <td><code>vonixcore.shop.sell</code></td>
                                <td>List held item for sale</td>
                            </tr>
                            <tr>
                                <td><code>/market</code></td>
                                <td><code>vonixcore.shop</code></td>
                                <td>Open player market GUI</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Quick Sell Commands</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/sell hand</code></td>
                                <td><code>vonixcore.sell</code></td>
                                <td>Sell held item to server shop</td>
                            </tr>
                            <tr>
                                <td><code>/sell all</code></td>
                                <td><code>vonixcore.sell</code></td>
                                <td>Sell all sellable items in inventory</td>
                            </tr>
                            <tr>
                                <td><code>/daily</code></td>
                                <td><code>vonixcore.daily</code></td>
                                <td>Claim daily reward</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Chest Shop Commands</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/chestshop create</code></td>
                                <td><code>vonixcore.chestshop</code></td>
                                <td>Start chest shop creation</td>
                            </tr>
                            <tr>
                                <td><code>/chestshop remove</code></td>
                                <td><code>vonixcore.chestshop</code></td>
                                <td>Remove your chest shop</td>
                            </tr>
                            <tr>
                                <td><code>/chestshop cancel</code></td>
                                <td><code>vonixcore.chestshop</code></td>
                                <td>Cancel shop creation</td>
                            </tr>
                            <tr>
                                <td><code>/chestshop info</code></td>
                                <td><code>vonixcore.chestshop</code></td>
                                <td>View chest shop information</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Admin Shop Commands</h3>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/adminshop setprice &lt;item&gt; &lt;buy&gt; &lt;sell&gt;</code></td>
                                <td><code>vonixcore.adminshop</code></td>
                                <td>Set server shop prices</td>
                            </tr>
                            <tr>
                                <td><code>/adminshop list</code></td>
                                <td><code>vonixcore.adminshop</code></td>
                                <td>List all server shop items</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Sign Shops</h3>
                <p>Create sign shops by placing a sign with this format:</p>
                <CodeBlock code={`[Buy] or [Sell]
<quantity>
<item name>
$<price>`} language="text" />
                <p>Example buy sign:</p>
                <CodeBlock code={`[Buy]
16
diamond
$500`} language="text" />
            </section>

            {/* Player Utility Commands */}
            <section>
                <h2 id="utility">👤 Player Utility Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/nick &lt;name&gt;</code></td>
                                <td><code>vonixcore.nick</code></td>
                                <td>Set your nickname (supports color codes)</td>
                            </tr>
                            <tr>
                                <td><code>/nick</code></td>
                                <td><code>vonixcore.nick</code></td>
                                <td>Clear your nickname</td>
                            </tr>
                            <tr>
                                <td><code>/seen &lt;player&gt;</code></td>
                                <td><code>vonixcore.seen</code></td>
                                <td>Check when a player was last online</td>
                            </tr>
                            <tr>
                                <td><code>/whois &lt;player&gt;</code></td>
                                <td><code>vonixcore.whois</code></td>
                                <td>View detailed player information</td>
                            </tr>
                            <tr>
                                <td><code>/ping</code></td>
                                <td><code>vonixcore.ping</code></td>
                                <td>Check your latency</td>
                            </tr>
                            <tr>
                                <td><code>/near [radius]</code></td>
                                <td><code>vonixcore.near</code></td>
                                <td>Find nearby players (default: 100 blocks)</td>
                            </tr>
                            <tr>
                                <td><code>/getpos</code></td>
                                <td><code>vonixcore.getpos</code></td>
                                <td>Display your current coordinates</td>
                            </tr>
                            <tr>
                                <td><code>/playtime</code></td>
                                <td><code>vonixcore.playtime</code></td>
                                <td>Show your total playtime</td>
                            </tr>
                            <tr>
                                <td><code>/list</code></td>
                                <td><code>vonixcore.list</code></td>
                                <td>Enhanced player list</td>
                            </tr>
                            <tr>
                                <td><code>/suicide</code></td>
                                <td><code>vonixcore.suicide</code></td>
                                <td>Kill yourself</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Nickname Color Codes</h3>
                <p>Use <code>&amp;</code> for color codes in nicknames:</p>
                <ul>
                    <li><code>&amp;a</code> = Green, <code>&amp;b</code> = Aqua, <code>&amp;c</code> = Red, etc.</li>
                    <li><code>&amp;#RRGGBB</code> = Hex color (e.g., <code>&amp;#FF5500</code>)</li>
                </ul>
            </section>

            {/* Messaging Commands */}
            <section>
                <h2 id="messaging">💬 Messaging Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/msg &lt;player&gt; &lt;message&gt;</code></td>
                                <td><code>vonixcore.msg</code></td>
                                <td>Send a private message</td>
                            </tr>
                            <tr>
                                <td><code>/tell &lt;player&gt; &lt;message&gt;</code></td>
                                <td><code>vonixcore.msg</code></td>
                                <td>Alias for /msg</td>
                            </tr>
                            <tr>
                                <td><code>/r &lt;message&gt;</code></td>
                                <td><code>vonixcore.msg</code></td>
                                <td>Reply to last message</td>
                            </tr>
                            <tr>
                                <td><code>/reply &lt;message&gt;</code></td>
                                <td><code>vonixcore.msg</code></td>
                                <td>Alias for /r</td>
                            </tr>
                            <tr>
                                <td><code>/ignore &lt;player&gt;</code></td>
                                <td><code>vonixcore.ignore</code></td>
                                <td>Toggle ignoring a player</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Item Commands */}
            <section>
                <h2 id="items">🎒 Item Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/hat</code></td>
                                <td><code>vonixcore.hat</code></td>
                                <td>Wear held item as helmet</td>
                            </tr>
                            <tr>
                                <td><code>/more</code></td>
                                <td><code>vonixcore.more</code></td>
                                <td>Fill held item stack to max</td>
                            </tr>
                            <tr>
                                <td><code>/repair</code></td>
                                <td><code>vonixcore.repair</code></td>
                                <td>Repair held item</td>
                            </tr>
                            <tr>
                                <td><code>/clear</code></td>
                                <td><code>vonixcore.clear</code></td>
                                <td>Clear your inventory</td>
                            </tr>
                            <tr>
                                <td><code>/clear &lt;player&gt;</code></td>
                                <td><code>vonixcore.clear.others</code></td>
                                <td>Clear another player&apos;s inventory</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* World Commands */}
            <section>
                <h2 id="world">🌍 World Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/weather clear</code></td>
                                <td><code>vonixcore.weather</code></td>
                                <td>Set weather to clear</td>
                            </tr>
                            <tr>
                                <td><code>/weather rain</code></td>
                                <td><code>vonixcore.weather</code></td>
                                <td>Set weather to rain</td>
                            </tr>
                            <tr>
                                <td><code>/weather storm</code></td>
                                <td><code>vonixcore.weather</code></td>
                                <td>Set weather to thunderstorm</td>
                            </tr>
                            <tr>
                                <td><code>/sun</code></td>
                                <td><code>vonixcore.weather</code></td>
                                <td>Shortcut for clear weather</td>
                            </tr>
                            <tr>
                                <td><code>/rain</code></td>
                                <td><code>vonixcore.weather</code></td>
                                <td>Shortcut for rain</td>
                            </tr>
                            <tr>
                                <td><code>/storm</code></td>
                                <td><code>vonixcore.weather</code></td>
                                <td>Shortcut for storm</td>
                            </tr>
                            <tr>
                                <td><code>/time set &lt;value&gt;</code></td>
                                <td><code>vonixcore.time</code></td>
                                <td>Set time (day/night/noon/midnight/ticks)</td>
                            </tr>
                            <tr>
                                <td><code>/day</code></td>
                                <td><code>vonixcore.time</code></td>
                                <td>Shortcut for daytime</td>
                            </tr>
                            <tr>
                                <td><code>/night</code></td>
                                <td><code>vonixcore.time</code></td>
                                <td>Shortcut for nighttime</td>
                            </tr>
                            <tr>
                                <td><code>/lightning [player]</code></td>
                                <td><code>vonixcore.lightning</code></td>
                                <td>Strike lightning at player or look position</td>
                            </tr>
                            <tr>
                                <td><code>/smite [player]</code></td>
                                <td><code>vonixcore.lightning</code></td>
                                <td>Alias for /lightning</td>
                            </tr>
                            <tr>
                                <td><code>/ext [player]</code></td>
                                <td><code>vonixcore.ext</code></td>
                                <td>Extinguish fire on player</td>
                            </tr>
                            <tr>
                                <td><code>/afk [message]</code></td>
                                <td><code>vonixcore.afk</code></td>
                                <td>Toggle AFK status with optional message</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Server Management Commands */}
            <section>
                <h2 id="server">🖥️ Server Management Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/broadcast &lt;message&gt;</code></td>
                                <td><code>vonixcore.broadcast</code></td>
                                <td>Broadcast a message to all players</td>
                            </tr>
                            <tr>
                                <td><code>/bc &lt;message&gt;</code></td>
                                <td><code>vonixcore.broadcast</code></td>
                                <td>Alias for /broadcast</td>
                            </tr>
                            <tr>
                                <td><code>/gc</code></td>
                                <td><code>vonixcore.gc</code></td>
                                <td>Show server memory and thread stats</td>
                            </tr>
                            <tr>
                                <td><code>/lag</code></td>
                                <td><code>vonixcore.lag</code></td>
                                <td>Check server performance</td>
                            </tr>
                            <tr>
                                <td><code>/invsee &lt;player&gt;</code></td>
                                <td><code>vonixcore.invsee</code></td>
                                <td>View another player&apos;s inventory</td>
                            </tr>
                            <tr>
                                <td><code>/enderchest [player]</code></td>
                                <td><code>vonixcore.enderchest</code></td>
                                <td>Open ender chest (yours or another&apos;s)</td>
                            </tr>
                            <tr>
                                <td><code>/workbench</code></td>
                                <td><code>vonixcore.workbench</code></td>
                                <td>Open a virtual crafting table</td>
                            </tr>
                            <tr>
                                <td><code>/anvil</code></td>
                                <td><code>vonixcore.anvil</code></td>
                                <td>Open a virtual anvil</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            {/* Permission Commands */}
            <section>
                <h2 id="permissions">🔑 Permission Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/perm user &lt;player&gt; info</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>View player permission info</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; group set &lt;group&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set player&apos;s primary group</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; group add &lt;group&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Add player to a group</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; group remove &lt;group&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Remove player from a group</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; permission set &lt;perm&gt; &lt;true/false&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set a permission</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; permission unset &lt;perm&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Remove a permission</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; permission check &lt;perm&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Check if player has permission</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; meta setprefix &lt;prefix&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set player&apos;s prefix</td>
                            </tr>
                            <tr>
                                <td><code>/perm user &lt;player&gt; meta setsuffix &lt;suffix&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set player&apos;s suffix</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; info</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>View group info</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; create</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Create a new group</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; delete</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Delete a group</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; permission set &lt;perm&gt; &lt;true/false&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set group permission</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; meta setprefix &lt;prefix&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set group prefix</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; meta setsuffix &lt;suffix&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set group suffix</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; meta setweight &lt;weight&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set group weight (priority)</td>
                            </tr>
                            <tr>
                                <td><code>/perm group &lt;group&gt; parent set &lt;parent&gt;</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Set group&apos;s parent</td>
                            </tr>
                            <tr>
                                <td><code>/perm listgroups</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>List all groups</td>
                            </tr>
                            <tr>
                                <td><code>/lp ...</code></td>
                                <td><code>vonixcore.perm</code></td>
                                <td>Alias for /perm</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p>See <a href="/docs/permissions">Permissions</a> for detailed permission system documentation.</p>
            </section>

            {/* VonixCore Admin Commands */}
            <section>
                <h2 id="admin">⚙️ VonixCore Admin Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/vonixcore</code></td>
                                <td><code>vonixcore.admin</code></td>
                                <td>Show VonixCore help</td>
                            </tr>
                            <tr>
                                <td><code>/vonixcore reload</code></td>
                                <td><code>vonixcore.admin</code></td>
                                <td>Reload all configurations</td>
                            </tr>
                            <tr>
                                <td><code>/vonixcore reload &lt;module&gt;</code></td>
                                <td><code>vonixcore.admin</code></td>
                                <td>Reload specific module config</td>
                            </tr>
                            <tr>
                                <td><code>/vonixcore version</code></td>
                                <td><code>vonixcore.admin</code></td>
                                <td>Show VonixCore version</td>
                            </tr>
                            <tr>
                                <td><code>/vonixcore status</code></td>
                                <td><code>vonixcore.admin</code></td>
                                <td>Show enabled/disabled modules</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <h3>Reload Modules</h3>
                <p>Available modules for <code>/vonixcore reload &lt;module&gt;</code>:</p>
                <ul>
                    <li><code>all</code> - Reload all configurations</li>
                    <li><code>database</code> - Database connection settings</li>
                    <li><code>protection</code> - Block logging settings</li>
                    <li><code>essentials</code> - Homes, warps, economy, kits settings</li>
                    <li><code>discord</code> - Discord integration settings</li>
                    <li><code>xpsync</code> - XP sync settings</li>
                    <li><code>auth</code> - Authentication settings</li>
                </ul>
            </section>

            {/* Discord Commands */}
            <section>
                <h2 id="discord">📱 Discord Commands</h2>
                <div className="overflow-x-auto">
                    <table>
                        <thead>
                            <tr>
                                <th>Command</th>
                                <th>Permission</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><code>/vonix discord link</code></td>
                                <td><code>vonixcore.discord.link</code></td>
                                <td>Generate a link code</td>
                            </tr>
                            <tr>
                                <td><code>/vonix discord unlink</code></td>
                                <td><code>vonixcore.discord.link</code></td>
                                <td>Unlink your Discord account</td>
                            </tr>
                            <tr>
                                <td><code>/vonix discord messages &lt;enable|disable&gt;</code></td>
                                <td><code>vonixcore.discord</code></td>
                                <td>Toggle Discord messages from other servers</td>
                            </tr>
                            <tr>
                                <td><code>/vonix discord events &lt;enable|disable&gt;</code></td>
                                <td><code>vonixcore.discord</code></td>
                                <td>Toggle Discord event notifications</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </DocPageLayout>
    );
}
