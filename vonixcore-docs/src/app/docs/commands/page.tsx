import { Metadata } from 'next';
import { DocPageLayout } from '@/components/doc-page-layout';

export const metadata: Metadata = {
    title: 'Commands Reference - VonixCore Docs',
    description: 'Complete reference of all commands available in VonixCore organized by category.',
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

                <h3 id="tpa">TPA (Teleport Request) Commands</h3>
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
                                <td><code>/discord link</code></td>
                                <td><code>vonixcore.discord.link</code></td>
                                <td>Generate a link code</td>
                            </tr>
                            <tr>
                                <td><code>/discord unlink</code></td>
                                <td><code>vonixcore.discord.link</code></td>
                                <td>Unlink your Discord account</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </DocPageLayout>
    );
}
