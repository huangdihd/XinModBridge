package xin.bbtt.modbridge;

/**
 * Tunables for the mod-server login bridge. Build with {@link #builder()}.
 */
public final class ModBridgeOptions {

    private final boolean injectFmlMarker;
    private final String fmlMarkerVersion;
    private final boolean answerUnknownLoginQueries;
    private final boolean handleNeoForgeConfiguration;

    private ModBridgeOptions(Builder b) {
        this.injectFmlMarker = b.injectFmlMarker;
        this.fmlMarkerVersion = b.fmlMarkerVersion;
        this.answerUnknownLoginQueries = b.answerUnknownLoginQueries;
        this.handleNeoForgeConfiguration = b.handleNeoForgeConfiguration;
    }

    /** Default options: reactive FML handshake on, no handshake-address marker injection. */
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

    /** Whether to handle the NeoForge configuration-phase handshake. */
    public boolean handleNeoForgeConfiguration() {
        return handleNeoForgeConfiguration;
    }

    public static final class Builder {
        // On by default: validation against a real Forge 1.20.1 server showed the
        // marker is required — without it the server treats the bot as a vanilla
        // client and never starts the FML handshake.
        private boolean injectFmlMarker = true;
        private String fmlMarkerVersion = "FML3";
        private boolean answerUnknownLoginQueries = true;
        private boolean handleNeoForgeConfiguration = true;

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

        public ModBridgeOptions build() {
            return new ModBridgeOptions(this);
        }
    }
}
