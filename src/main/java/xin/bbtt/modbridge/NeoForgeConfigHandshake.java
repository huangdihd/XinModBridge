package xin.bbtt.modbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge (Minecraft 1.20.2+) negotiates its modded network in the CONFIGURATION
 * phase over custom payloads, instead of the LOGIN-phase {@code fml:loginwrapper}
 * mechanism used by older Forge. This handler is reached for every clientbound
 * config-phase custom payload.
 *
 * <p><b>Status: mapped and proven not feasible from a reactive listener.</b> The flow
 * was reverse-engineered against NeoForge 21.11.42 (MC 1.21.11) from
 * {@code NetworkRegistry}/{@code ConfigurationInitialization} and verified on a live
 * server:
 *
 * <pre>
 *   S-&gt;C minecraft:register = [neoforge:register, neoforge:network, c:version, c:register, ...]
 *   S-&gt;C neoforge:register  = 00            (ModdedNetworkQueryPayload, empty map)
 *   C-&gt;S neoforge:register  = 00            (echo) -&gt; server negotiates, replies:
 *   S-&gt;C neoforge:network   = 0201000400    (ModdedNetworkPayload)  ... then STALLS
 * </pre>
 *
 * The modded-network negotiation completes, but the server only registers its
 * {@code CommonVersionTask}/{@code CommonRegisterTask} (which would send
 * {@code c:version}/{@code c:register}) when {@code hasChannel(...)} is already true at
 * the time {@code RegisterConfigurationTasksEvent} fires — i.e. at the very start of the
 * configuration phase, before any client payload is processed. A reactive client that
 * declares its channels via {@code minecraft:register} (verified sent and received here)
 * does so too late to affect task gathering. Completing the NeoForge handshake therefore
 * requires driving the client's configuration-phase state machine the way a real NeoForge
 * client does (declaring channels at config entry) — which is below the mcprotocollib /
 * SessionListener layer this plugin operates at, and out of scope for a reactive bridge.
 *
 * <p>So this handler is a no-op (NeoForge / Forge 1.20.2+ connections are not supported).
 * Run with {@code -Dmodbridge.dumpNeoForge=true} to trace the exchange.
 */
final class NeoForgeConfigHandshake {

    private static final Logger log = LoggerFactory.getLogger("XinModBridge");
    private static boolean warnedOnce = false;

    private NeoForgeConfigHandshake() {
    }

    /**
     * @return the reply payload bytes to send back on the same channel, or {@code null}
     *         if nothing should be sent (currently always null — see class docs).
     */
    static byte[] handle(String channel, byte[] data) {
        if (Boolean.getBoolean("modbridge.dumpNeoForge")) {
            StringBuilder hex = new StringBuilder();
            int n = Math.min(data.length, 512);
            for (int i = 0; i < n; i++) hex.append(String.format("%02x", data[i] & 0xFF));
            log.info("[XinModBridge][nf-dump] channel='{}' len={} hex={}{}", channel, data.length, hex,
                    data.length > n ? "..." : "");
        } else if (channel.startsWith("neoforge:") && !warnedOnce) {
            warnedOnce = true;
            log.warn("[XinModBridge] NeoForge / Forge 1.20.2+ configuration-phase handshake is not "
                    + "supported by this reactive bridge; the connection will not complete.");
        }
        return null;
    }
}
