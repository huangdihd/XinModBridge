package xin.bbtt.modbridge;

import net.kyori.adventure.key.Key;
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
 *   <li>drives the legacy FML1 (1.7–1.12) {@code FML|HS} PLAY-phase handshake via
 *       {@link Fml1Handshake};</li>
 *   <li>optionally appends an FML marker to the handshake hostname so older Forge
 *       servers switch into FML mode.</li>
 * </ul>
 *
 * <p>NeoForge / Forge 1.20.2+ is handled separately by
 * {@link NeoForgeClientListenerWrapper}, which needs to act at config-phase entry.
 */
public final class FmlLoginHandshakeListener extends SessionAdapter {

    private static final Logger log = LoggerFactory.getLogger("XinModBridge");

    private final ModBridgeOptions options;
    private final Fml1Handshake fml1 = new Fml1Handshake();

    public FmlLoginHandshakeListener(ModBridgeOptions options) {
        this.options = options;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundCustomQueryPacket query) {
            handleLoginQuery(session, query);
        } else if (packet instanceof ClientboundCustomPayloadPacket payload) {
            handleCustomPayload(session, payload);
        }
    }

    private void handleCustomPayload(Session session, ClientboundCustomPayloadPacket payload) {
        String channel = payload.getChannel().asString();
        // Legacy Forge 1.7–1.12: the FML|HS handshake runs in the PLAY phase.
        // (NeoForge / Forge 1.20.2+ is handled by NeoForgeClientListenerWrapper instead.)
        if (Fml1Handshake.isFmlHsChannel(channel)) {
            for (Fml1Handshake.Reply reply : fml1.handle(channel, payload.getData())) {
                session.send(new ServerboundCustomPayloadPacket(Key.key(reply.channel()), reply.data()));
            }
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
