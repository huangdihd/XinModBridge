package xin.bbtt.modbridge;

/**
 * The mod-loader family a server uses, as far as it affects the login handshake.
 *
 * <p>Fabric / Quilt are intentionally absent: they do not change the protocol and
 * need no handshake from a client (a vanilla connection joins them directly). Only
 * the FML family (Forge / NeoForge) requires a login-time negotiation.
 */
public enum ModLoader {
    /** Vanilla, Fabric, Quilt — no FML handshake; connect as a vanilla client. */
    VANILLA,
    /** Legacy Forge, Minecraft 1.7.10 – 1.12.2 (FML|HS channel). Not yet implemented. */
    FORGE_FML1,
    /** Forge with FML2 networking, roughly Minecraft 1.13 – 1.15. */
    FORGE_FML2,
    /** Forge with FML3 networking, roughly Minecraft 1.16 – 1.20.1. */
    FORGE_FML3,
    /** NeoForge, Minecraft 1.20.2+, handshake runs in the CONFIGURATION phase. */
    NEOFORGE
}
