package xin.bbtt.modbridge;

import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge (Minecraft 1.20.2+) negotiates its modded network in the CONFIGURATION
 * phase over {@code neoforge:}-namespaced custom payloads, rather than the LOGIN-phase
 * {@code fml:loginwrapper} mechanism used by older Forge.
 *
 * <p><b>Status: extension point only.</b> The exact NeoForge configuration handshake
 * (ModdedNetworkQuery / setup payloads) is not implemented yet, so {@link #handle}
 * returns {@code null} (no reply) to avoid sending malformed packets that would break
 * the connection. This is where the NeoForge spoof handshake will live.
 */
final class NeoForgeConfigHandshake {

    private static final Logger log = LoggerFactory.getLogger("XinModBridge");
    private static final String NAMESPACE = "neoforge";

    private static boolean warnedOnce = false;

    private NeoForgeConfigHandshake() {
    }

    static boolean isNeoForgeChannel(String channel) {
        return channel != null && channel.startsWith(NAMESPACE + ":");
    }

    /**
     * @return the reply payload bytes, or {@code null} if nothing should be sent.
     */
    static byte[] handle(String channel, byte[] data) {
        if (!warnedOnce) {
            warnedOnce = true;
            log.warn("[XinModBridge] NeoForge configuration-phase handshake is not implemented yet "
                    + "(channel '{}'); the connection may be rejected by NeoForge servers.", channel);
        }
        return null;
    }

    static ServerboundCustomPayloadPacket buildReply(String channel, byte[] data) {
        return new ServerboundCustomPayloadPacket(Key.key(channel), data);
    }
}
