package xin.bbtt.modbridge;

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.plugin.Plugin;

/**
 * Public entry point of the XinModBridge library.
 *
 * <p>A meta plugin that wants to join a Forge/NeoForge server declares
 * {@code depend: [XinModBridge]} in its {@code plugin.yml} and, in its
 * {@code onEnable()}, calls {@link #attach(Plugin)}. The returned listener drives
 * the FML login handshake reactively; it is unregistered automatically when the
 * owning plugin is disabled, or explicitly via {@link #detach(Plugin, FmlLoginHandshakeListener)}.
 *
 * <pre>{@code
 * private FmlLoginHandshakeListener bridge;
 *
 * public void onEnable() {
 *     bridge = XinModBridgeProvider.attach(this);
 * }
 * }</pre>
 */
public final class XinModBridgeProvider {

    private XinModBridgeProvider() {
    }

    /** Attaches the FML handshake bridge with {@link ModBridgeOptions#defaults()}. */
    public static FmlLoginHandshakeListener attach(Plugin owner) {
        return attach(owner, ModBridgeOptions.defaults());
    }

    /**
     * Registers the FML handshake bridge on the bot's session under {@code owner}.
     *
     * @return the listener, so the caller can {@link #detach} it early if needed
     */
    public static FmlLoginHandshakeListener attach(Plugin owner, ModBridgeOptions options) {
        FmlLoginHandshakeListener listener = new FmlLoginHandshakeListener(options);
        Bot.INSTANCE.addPacketListener(listener, owner);
        return listener;
    }

    /** Removes a previously {@link #attach}ed bridge listener. */
    public static void detach(Plugin owner, FmlLoginHandshakeListener listener) {
        Bot.INSTANCE.removePacketListener(listener, owner);
    }
}
