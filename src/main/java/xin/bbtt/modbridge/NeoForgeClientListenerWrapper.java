package xin.bbtt.modbridge;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.ClientListener;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Makes the bot able to join <b>NeoForge / Forge 1.20.2+</b> servers. It does two things a
 * plain reactive {@code SessionListener} cannot:
 *
 * <ol>
 *   <li><b>Drives the configuration-phase handshake at the right time.</b> NeoForge only
 *       registers its {@code CommonVersion}/{@code CommonRegister} tasks when the connection
 *       already {@code hasChannel(...)} for them at the instant {@code RegisterConfigurationTasksEvent}
 *       fires — i.e. right at config start, before any client packet is normally processed.
 *       So this wraps mcprotocollib's internal {@link ClientListener} with a proxy that, the
 *       moment login is acknowledged, proactively sends a {@code minecraft:register} declaring
 *       the client's channels. It then answers the config Ping (a sync barrier mcprotocollib
 *       doesn't auto-answer), echoes the modded-network query payloads, and acknowledges the
 *       extensible-enum check, so every config task finishes and the bot reaches PLAY.</li>
 *   <li><b>Tolerant decoding.</b> Modded servers send PLAY packets referencing modded registry
 *       entries that mcprotocollib's vanilla decoders can't parse. A netty handler swallows
 *       those per-packet decode errors so one un-decodable modded packet doesn't drop the
 *       whole connection — the bot stays online.</li>
 * </ol>
 *
 * <p>Inspired by the {@code ClientListenerProxy} pattern used elsewhere in the xinbot ecosystem.
 */
public final class NeoForgeClientListenerWrapper extends SessionAdapter {

    private static final Logger LOG = LoggerFactory.getLogger("XinModBridge");
    private static final String TOLERANT_NAME = "modbridge-tolerant";
    private static final boolean DUMP = Boolean.getBoolean("modbridge.dumpNeoForge");

    private final ModBridgeOptions options;

    public NeoForgeClientListenerWrapper(ModBridgeOptions options) {
        this.options = options;
    }

    @Override
    public void connected(ConnectedEvent event) {
        Session session = event.getSession();

        if (options.handleNeoForgeConfiguration()) {
            for (SessionListener listener : new ArrayList<>(session.getListeners())) {
                if (listener instanceof ClientListener clientListener) {
                    session.removeListener(listener);
                    session.addListener(new Proxy(clientListener, channelDeclaration()));
                    if (DUMP) {
                        LOG.info("[XinModBridge] wrapped ClientListener for NeoForge handshake");
                    }
                }
            }
        }

        if (options.tolerantDecoding()) {
            Channel channel = session.getChannel();
            if (channel != null && channel.pipeline().get("manager") != null
                    && channel.pipeline().get(TOLERANT_NAME) == null) {
                channel.pipeline().addBefore("manager", TOLERANT_NAME, new TolerantInbound());
            }
        }
    }

    /** The {@code minecraft:register} channel list: built-in NeoForge set + caller extras. */
    private String channelDeclaration() {
        Set<String> channels = new LinkedHashSet<>(java.util.Arrays.asList(
                "c:version", "c:register",
                "neoforge:register", "neoforge:network", "neoforge:modded_network_setup_failed",
                "neoforge:extensible_enum_data", "neoforge:extensible_enum_ack",
                "neoforge:recipe_content", "neoforge:recipes",
                "neoforge:advanced_add_entity", "neoforge:advanced_container_set_data",
                "neoforge:advanced_open_screen", "neoforge:auxiliary_light_data",
                "neoforge:custom_time_packet", "neoforge:sync_attachments"));
        channels.addAll(options.extraConfigChannels());
        return String.join("\0", channels);
    }

    /** Swallows per-packet decode errors (un-parseable modded packets) to keep the link up. */
    private static final class TolerantInbound extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof DecoderException) {
                if (DUMP) {
                    LOG.info("[XinModBridge] dropped un-decodable modded packet: {}", cause.getMessage());
                }
                return; // swallow — do not propagate / close the channel
            }
            ctx.fireExceptionCaught(cause);
        }
    }

    private static final class Proxy extends SessionAdapter {

        private final ClientListener origin;
        private final byte[] registerPayload;
        private boolean declared = false;

        Proxy(ClientListener origin, String channelDeclaration) {
            this.origin = origin;
            this.registerPayload = channelDeclaration.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void packetReceived(Session session, Packet packet) {
            // Login finished -> the original listener sends LoginAcknowledged and switches to
            // CONFIGURATION; immediately afterwards declare our channels, before the server
            // gathers its configuration tasks.
            if (packet instanceof ClientboundLoginFinishedPacket) {
                origin.packetReceived(session, packet);
                if (!declared) {
                    declared = true;
                    session.send(new ServerboundCustomPayloadPacket(Key.key("minecraft:register"), registerPayload));
                    if (DUMP) {
                        LOG.info("[XinModBridge] declared config channels for NeoForge handshake");
                    }
                }
                return;
            }

            // NeoForge uses a configuration-phase Ping as a sync barrier; answer it so the
            // server continues (mcprotocollib does not auto-pong during configuration).
            if (packet instanceof ClientboundPingPacket ping) {
                session.send(new ServerboundPongPacket(ping.getId()));
            }

            // Echo the modded-network handshake queries so each config task finishes.
            if (packet instanceof ClientboundCustomPayloadPacket payload) {
                String channel = payload.getChannel().asString();
                if (channel.equals("neoforge:register") || channel.equals("c:version") || channel.equals("c:register")) {
                    session.send(new ServerboundCustomPayloadPacket(payload.getChannel(), payload.getData()));
                } else if (channel.equals("neoforge:extensible_enum_data")) {
                    session.send(new ServerboundCustomPayloadPacket(Key.key("neoforge:extensible_enum_ack"), new byte[0]));
                }
            }

            origin.packetReceived(session, packet);
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            origin.disconnected(event);
        }
    }
}
