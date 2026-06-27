package xin.bbtt.modbridge;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tunables for the mod-server bridge. Build with {@link #builder()}.
 */
public final class ModBridgeOptions {

    private final boolean injectFmlMarker;
    private final String fmlMarkerVersion;
    private final boolean answerUnknownLoginQueries;
    private final boolean handleNeoForgeConfiguration;
    private final boolean tolerantDecoding;
    private final Set<String> extraConfigChannels;

    private ModBridgeOptions(Builder b) {
        this.injectFmlMarker = b.injectFmlMarker;
        this.fmlMarkerVersion = b.fmlMarkerVersion;
        this.answerUnknownLoginQueries = b.answerUnknownLoginQueries;
        this.handleNeoForgeConfiguration = b.handleNeoForgeConfiguration;
        this.tolerantDecoding = b.tolerantDecoding;
        this.extraConfigChannels = Set.copyOf(b.extraConfigChannels);
    }

    /** Default options: FML2/FML3 marker on, NeoForge handshake on, tolerant decoding on. */
    public static ModBridgeOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Whether to append an FML marker (e.g. {@code \0FML3\0}) to the handshake hostname. */
    public boolean injectFmlMarker() {
        return injectFmlMarker;
    }

    /** The FML marker token to inject, e.g. {@code "FML3"} or {@code "FML2"}. */
    public String fmlMarkerVersion() {
        return fmlMarkerVersion;
    }

    /** Whether to send an empty answer to login queries on unrecognized channels. */
    public boolean answerUnknownLoginQueries() {
        return answerUnknownLoginQueries;
    }

    /** Whether to drive the NeoForge / Forge 1.20.2+ configuration-phase handshake. */
    public boolean handleNeoForgeConfiguration() {
        return handleNeoForgeConfiguration;
    }

    /**
     * Whether to swallow per-packet decode errors so the bot stays connected to modded
     * servers whose packets reference modded registry entries that mcprotocollib's vanilla
     * decoders can't parse. The un-decodable packets are dropped; the connection survives.
     */
    public boolean tolerantDecoding() {
        return tolerantDecoding;
    }

    /**
     * Extra plugin channels to declare to the server during the NeoForge configuration
     * handshake (beyond the built-in NeoForge set), for servers running mods that add their
     * own channels. Use the modern namespaced form, e.g. {@code "yourmod:sync"}.
     */
    public Set<String> extraConfigChannels() {
        return extraConfigChannels;
    }

    public static final class Builder {
        // On by default: a live Forge server only starts the FML handshake when the
        // handshake hostname carries the marker.
        private boolean injectFmlMarker = true;
        private String fmlMarkerVersion = "FML3";
        private boolean answerUnknownLoginQueries = true;
        private boolean handleNeoForgeConfiguration = true;
        private boolean tolerantDecoding = true;
        private final Set<String> extraConfigChannels = new LinkedHashSet<>();

        public Builder injectFmlMarker(boolean value) {
            this.injectFmlMarker = value;
            return this;
        }

        public Builder fmlMarkerVersion(String value) {
            this.fmlMarkerVersion = value;
            return this;
        }

        public Builder answerUnknownLoginQueries(boolean value) {
            this.answerUnknownLoginQueries = value;
            return this;
        }

        public Builder handleNeoForgeConfiguration(boolean value) {
            this.handleNeoForgeConfiguration = value;
            return this;
        }

        public Builder tolerantDecoding(boolean value) {
            this.tolerantDecoding = value;
            return this;
        }

        /** Declare additional config-phase channels (namespaced, e.g. {@code "yourmod:net"}). */
        public Builder extraConfigChannels(String... channels) {
            for (String c : channels) {
                this.extraConfigChannels.add(c);
            }
            return this;
        }

        public ModBridgeOptions build() {
            return new ModBridgeOptions(this);
        }
    }
}
