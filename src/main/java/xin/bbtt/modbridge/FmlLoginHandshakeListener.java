package xin.bbtt.modbridge;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the Forge/NeoForge login handshake reactively:
 * <ul>
 *   <li>answers {@code fml:loginwrapper} login queries during the LOGIN phase
 *       (Forge FML2/FML3) with spoofed replies via {@link FmlHandshake};</li>
 *   <li>answers NeoForge handshake custom-payloads during the CONFIGURATION phase
 *       via {@link NeoForgeConfigHandshake};</li>
 *   <li>optionally appends an FML marker to the handshake hostname so older Forge
 *       servers switch into FML mode.</li>
 * </ul>
 */
public final class FmlLoginHandshakeListener extends SessionAdapter {

    private static final Logger log = LoggerFactory.getLogger("XinModBridge");

    private final ModBridgeOptions options;

    public FmlLoginHandshakeListener(ModBridgeOptions options) {
        this.options = options;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundCustomQueryPacket query) {
            handleLoginQuery(session, query);
        } else if (packet instanceof ClientboundCustomPayloadPacket payload
                && options.handleNeoForgeConfiguration()) {
            handleConfigurationPayload(session, payload);
        }
    }

    private void handleLoginQuery(Session session, ClientboundCustomQueryPacket query) {
        String channel = query.getChannel().asString();
        if (FmlHandshake.isFmlLoginChannel(channel)) {
            byte[] reply = FmlHandshake.handleLoginWrapper(query.getData());
            session.send(new ServerboundCustomQueryAnswerPacket(query.getMessageId(), reply));
            log.debug("[XinModBridge] answered FML login query on '{}' (id {})", channel, query.getMessageId());
        } else if (options.answerUnknownLoginQueries()) {
            // Tell the server we acknowledge but support nothing extra on this channel.
            session.send(new ServerboundCustomQueryAnswerPacket(query.getMessageId(), new byte[0]));
            log.debug("[XinModBridge] empty-answered unknown login query on '{}'", channel);
        }
    }

    private void handleConfigurationPayload(Session session, ClientboundCustomPayloadPacket payload) {
        String channel = payload.getChannel().asString();
        byte[] reply = NeoForgeConfigHandshake.handle(channel, payload.getData());
        if (reply != null) {
            session.send(new ServerboundCustomPayloadPacket(payload.getChannel(), reply));
            if (Boolean.getBoolean("modbridge.dumpNeoForge")) {
                log.info("[XinModBridge][nf-send] -> '{}' ({} bytes)", channel, reply.length);
            }
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        if (!options.injectFmlMarker()) {
            return;
        }
        if (event.getPacket() instanceof ClientIntentionPacket intention) {
            String marker = "\0" + options.fmlMarkerVersion() + "\0";
            String host = intention.getHostname();
            if (!host.contains(marker)) {
                event.setPacket(intention.withHostname(host + marker));
                log.info("[XinModBridge] injected '{}' marker into handshake hostname", options.fmlMarkerVersion());
            }
        }
    }
}
