package xin.bbtt.modbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy Forge <b>FML1</b> (Minecraft 1.7–1.12) handshake. Unlike FML2/FML3 it runs in
 * the PLAY phase over the legacy {@code FML|HS} plugin channel, as a small state machine
 * (mirroring Forge's {@code FMLHandshakeClientState}):
 *
 * <pre>
 *   S-&gt;C ServerHello(0x00)   -&gt; C: REGISTER + ClientHello(0x01) + ModList(0x02)
 *   S-&gt;C ModList(0x02)       -&gt; C: HandshakeAck(0xFF, phase 2)
 *   S-&gt;C RegistryData(0x03)  -&gt; (repeat while hasMore) then C: HandshakeAck(0xFF, phase 3)
 *   S-&gt;C HandshakeAck(0xFF)  -&gt; C: HandshakeAck(0xFF, phase 4) then phase 5 -&gt; done
 * </pre>
 *
 * <p><b>Validated</b> against a live Forge 1.12.2 server: a native test client using this
 * exact sequence completed the handshake and joined. The client mod list is sent
 * <em>empty</em> — accepted by vanilla-Forge and by mods that don't require client-side
 * presence. Strict modded servers check the client mod list (sent before the server's, so
 * it cannot be echoed); satisfying those needs the server's mod list from a status ping,
 * which a reactive handler doesn't do.
 *
 * <p>Stateful per connection — a {@code ServerHello} resets the state machine.
 */
final class Fml1Handshake {

    private static final Logger log = LoggerFactory.getLogger("XinModBridge");

    // The legacy channel "FML|HS" is invalid as a modern (1.13+) channel key; ViaVersion
    // remaps it to "fml:hs" (and REGISTER content accordingly). A 1.21.x bot reaching a
    // 1.12.2 server through ViaVersion therefore sees the modern names.
    static final String FML_HS_LEGACY = "FML|HS";
    static final String FML_HS_MODERN = "fml:hs";

    // 1=HELLO 2=WAITINGSERVERDATA 3=WAITINGSERVERCOMPLETE 4=PENDINGCOMPLETE 5=COMPLETE 6=DONE
    private int state = 1;

    /** A plugin-channel message the client should send in reply. */
    record Reply(String channel, byte[] data) {
    }

    static boolean isFmlHsChannel(String channel) {
        return FML_HS_LEGACY.equals(channel) || FML_HS_MODERN.equals(channel);
    }

    /** Feed an inbound {@code FML|HS} payload; returns the messages to send back. */
    List<Reply> handle(String inboundChannel, byte[] data) {
        List<Reply> out = new ArrayList<>();
        if (data.length == 0) {
            return out;
        }
        boolean modern = inboundChannel.contains(":"); // "fml:hs" vs legacy "FML|HS"
        String hs = inboundChannel;
        String registerCh = modern ? "minecraft:register" : "REGISTER";
        String registerContent = modern ? "fml:hs\0fml:mp" : "FML|HS\0FML\0FML|MP\0FORGE";
        int disc = data[0] & 0xFF;
        switch (disc) {
            case 0x00: // ServerHello
                state = 1;
                out.add(new Reply(registerCh, registerContent.getBytes(StandardCharsets.UTF_8)));
                out.add(new Reply(hs, new byte[]{0x01, 0x02}));   // ClientHello, FML_PROTOCOL=2
                out.add(new Reply(hs, new byte[]{0x02, 0x00}));   // ModList, count 0 (empty)
                state = 2;
                break;
            case 0x02: // server ModList
                out.add(new Reply(hs, new byte[]{(byte) 0xFF, 0x02}));
                state = 3;
                break;
            case 0x03: // RegistryData (0x03 disc, then hasMore boolean)
                if (data.length > 1 && data[1] == 0) { // hasMore == false -> last
                    out.add(new Reply(hs, new byte[]{(byte) 0xFF, 0x03}));
                    state = 4;
                }
                break;
            case 0xFF: // server HandshakeAck
                if (state == 4) {
                    out.add(new Reply(hs, new byte[]{(byte) 0xFF, 0x04}));
                    state = 5;
                } else if (state == 5) {
                    out.add(new Reply(hs, new byte[]{(byte) 0xFF, 0x05}));
                    state = 6;
                    log.info("[XinModBridge] FML|HS handshake complete (legacy Forge 1.7–1.12)");
                }
                break;
            default:
                break;
        }
        return out;
    }
}
