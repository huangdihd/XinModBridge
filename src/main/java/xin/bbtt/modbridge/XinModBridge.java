package xin.bbtt.modbridge;

import xin.bbtt.mcbot.plugin.Plugin;

/**
 * XinModBridge — a xinbot library plugin that lets the bot complete the Forge /
 * NeoForge (FML) login handshake, so it can join modded servers whose checks are
 * mod-list / channel based.
 *
 * <p>This class is just the plugin entry point; the reusable surface is
 * {@link XinModBridgeProvider}. There is no global state to bootstrap, so the
 * lifecycle hooks are intentionally empty.
 */
public class XinModBridge implements Plugin {

    @Override
    public void onLoad() {
    }

    @Override
    public void onUnload() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
