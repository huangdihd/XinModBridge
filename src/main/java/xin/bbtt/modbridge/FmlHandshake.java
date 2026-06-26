package xin.bbtt.modbridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes/decodes the Forge FML login handshake carried over the
 * {@code fml:loginwrapper} login-query channel, and produces "spoof" replies that
 * echo whatever the server requires so a mod-less bot is accepted.
 *
 * <p><b>Status:</b> implemented against Forge <b>FML3</b> (Minecraft 1.16 – 1.20.1)
 * as documented in Forge's {@code NetworkInitialization} / {@code HandshakeMessages}.
 * The message indices and field layouts below are version-sensitive and have
 * <b>not yet been validated against a live server</b>. FML2 (1.13–1.15) and the
 * NeoForge configuration-phase variant differ and are handled elsewhere / TODO.
 */
final class FmlHandshake {

    private static final Logger log = LoggerFactory.getLogger("XinModBridge");

    static final String CH_LOGIN_WRAPPER = "fml:loginwrapper";
    static final String CH_HANDSHAKE = "fml:handshake";

    // fml:handshake message discriminators (Forge FML3, MC 1.16 – 1.20.1).
    private static final int S2C_MOD_LIST = 1;
    private static final int C2S_MOD_LIST_REPLY = 2;
    private static final int S2C_REGISTRY = 3;
    private static final int S2C_CONFIG_DATA = 4;
    private static final int S2C_MOD_DATA = 5;
    private static final int C2S_ACKNOWLEDGE = 99;

    private FmlHandshake() {
    }

    /** True if the channel is one this handler knows how to answer. */
    static boolean isFmlLoginChannel(String channel) {
        return CH_LOGIN_WRAPPER.equals(channel) || CH_HANDSHAKE.equals(channel);
    }

    /**
     * Given the raw data of a clientbound {@code fml:loginwrapper} login query,
     * produce the raw data for the serverbound answer, or {@code null} if no reply
     * is warranted.
     */
    static byte[] handleLoginWrapper(byte[] queryData) {
        ByteBuf in = Unpooled.wrappedBuffer(queryData);
        try {
            String target = ByteBufs.readResourceLocation(in);
            int innerLen = ByteBufs.readVarInt(in);
            ByteBuf inner = in.readSlice(innerLen);

            if (!CH_HANDSHAKE.equals(target)) {
                // Some other channel was wrapped (rare during login); acknowledge it.
                log.debug("[XinModBridge] loginwrapper for unexpected channel '{}', acking", target);
                return wrap(target, encodeAcknowledge());
            }

            int index = ByteBufs.readVarInt(inner);
            switch (index) {
                case S2C_MOD_LIST:
                    return wrap(CH_HANDSHAKE, encodeModListReply(inner));
                case S2C_REGISTRY:
                case S2C_CONFIG_DATA:
                case S2C_MOD_DATA:
                    return wrap(CH_HANDSHAKE, encodeAcknowledge());
                default:
                    log.debug("[XinModBridge] unhandled fml:handshake index {}, acking", index);
                    return wrap(CH_HANDSHAKE, encodeAcknowledge());
            }
        } catch (Exception e) {
            log.warn("[XinModBridge] failed to parse FML loginwrapper, sending empty ack", e);
            return wrap(CH_HANDSHAKE, encodeAcknowledge());
        }
    }

    /** Wraps a fml:handshake message body into a fml:loginwrapper payload. */
    private static byte[] wrap(String target, byte[] message) {
        ByteBuf out = Unpooled.buffer();
        ByteBufs.writeResourceLocation(out, target);
        ByteBufs.writeVarInt(out, message.length);
        out.writeBytes(message);
        return ByteBufs.toArray(out);
    }

    /**
     * Parses the server's S2CModList (mods / channels / registries) and echoes it
     * back as a C2SModListReply — claiming the client has exactly what the server
     * advertised. This is the core "spoof" that gets a mod-less bot accepted.
     */
    private static byte[] encodeModListReply(ByteBuf modList) {
        List<String> mods = readStringList(modList);
        Map<String, String> channels = readStringMap(modList);
        List<String> registries = readStringList(modList);

        log.info("[XinModBridge] FML ModList: {} mods, {} channels, {} registries — echoing back",
                mods.size(), channels.size(), registries.size());

        ByteBuf out = Unpooled.buffer();
        ByteBufs.writeVarInt(out, C2S_MOD_LIST_REPLY);
        // mods: List<String>
        writeStringList(out, mods);
        // channels: Map<ResourceLocation, String>
        writeStringMap(out, channels);
        // registries: Map<ResourceLocation, String> (registry name -> snapshot marker)
        ByteBufs.writeVarInt(out, registries.size());
        for (String registry : registries) {
            ByteBufs.writeResourceLocation(out, registry);
            ByteBufs.writeString(out, "");
        }
        return ByteBufs.toArray(out);
    }

    private static byte[] encodeAcknowledge() {
        ByteBuf out = Unpooled.buffer();
        ByteBufs.writeVarInt(out, C2S_ACKNOWLEDGE);
        return ByteBufs.toArray(out);
    }

    private static List<String> readStringList(ByteBuf buf) {
        int count = ByteBufs.readVarInt(buf);
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(ByteBufs.readString(buf));
        }
        return list;
    }

    private static void writeStringList(ByteBuf buf, List<String> list) {
        ByteBufs.writeVarInt(buf, list.size());
        for (String s : list) {
            ByteBufs.writeString(buf, s);
        }
    }

    private static Map<String, String> readStringMap(ByteBuf buf) {
        int count = ByteBufs.readVarInt(buf);
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = ByteBufs.readResourceLocation(buf);
            String value = ByteBufs.readString(buf);
            map.put(key, value);
        }
        return map;
    }

    private static void writeStringMap(ByteBuf buf, Map<String, String> map) {
        ByteBufs.writeVarInt(buf, map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            ByteBufs.writeResourceLocation(buf, e.getKey());
            ByteBufs.writeString(buf, e.getValue());
        }
    }
}
