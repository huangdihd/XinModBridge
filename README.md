# XinModBridge

> A [xinbot](https://github.com/huangdihd/xinbot) **library plugin** that lets the bot join
> **modded servers** — Forge (FML1/2/3) and NeoForge — by completing their handshakes and
> tolerating modded packets, all behind a small API for other meta plugins.

> ⚠️ **Experimental (`0.1.0-SNAPSHOT`, pre-release).** Validated against live servers; see the
> **Status** table for exactly what was tested and the known limits.

---

## What it does

Joining a modded server depends on the loader, and the loaders are genuinely different. XinModBridge
handles each, plus a tolerant decoder so the connection survives modded packets the vanilla protocol
library can't parse:

| Loader | Handling |
|---|---|
| **Vanilla / Fabric / Quilt** | Nothing needed — protocol-vanilla; connect directly. |
| **Forge FML2 / FML3** (MC ~1.15–1.20.1) | LOGIN-phase `fml:loginwrapper` handshake — echo the server's mod list / channels back. Marker must match the server's FML net version: `FML2` for ≤1.16.x, `FML3` for ≥1.18. |
| **Forge FML1** (MC 1.7–1.12) | PLAY-phase `FML\|HS` handshake state machine (ServerHello → ClientHello + ModList → Ack chain). |
| **NeoForge / Forge 1.20.2+** (MC 1.20.2+) | CONFIGURATION-phase handshake, driven by wrapping mcprotocollib's `ClientListener` so the client declares its channels *before* the server gathers its config tasks; answers the config Ping barrier, echoes the modded-network queries, acks the extensible-enum check. |
| **Any modded server (PLAY)** | **Tolerant decoding** — drops individual packets that reference modded registry entries mcprotocollib can't decode, so the bot stays connected. |

## Usage

`plugin.yml`:

```yaml
depend:
  - XinModBridge
```

`pom.xml` (JitPack, `provided` — supplied at runtime by the XinModBridge plugin):

```xml
<dependency>
    <groupId>com.github.huangdihd</groupId>
    <artifactId>XinModBridge</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Attach the bridge in your meta plugin's `onEnable()`:

```java
import xin.bbtt.modbridge.XinModBridgeProvider;
import xin.bbtt.modbridge.FmlLoginHandshakeListener;
import xin.bbtt.modbridge.ModBridgeOptions;

private FmlLoginHandshakeListener bridge;

public void onEnable() {
    // Defaults: FML3 marker, NeoForge handshake, and tolerant decoding all on.
    bridge = XinModBridgeProvider.attach(this);

    // Tuned example — older Forge (FML2), plus declaring a mod's own config channel:
    // bridge = XinModBridgeProvider.attach(this, ModBridgeOptions.builder()
    //         .fmlMarkerVersion("FML2")
    //         .extraConfigChannels("yourmod:sync")
    //         .build());
}
```

The listener is unregistered automatically when your plugin is disabled (or via
`XinModBridgeProvider.detach`).

It composes with [XinVia](https://github.com/huangdihd/XinVia): `depend: [XinVia, XinModBridge]`
to reach a server that is both cross-version and modded.

## API

- **`XinModBridgeProvider`** — `attach(plugin)`, `attach(plugin, options)`, `detach(plugin, listener)`.
- **`ModBridgeOptions`** (builder):
  - `injectFmlMarker(boolean)` / `fmlMarkerVersion(String)` — FML2/FML3 handshake-address marker.
  - `handleNeoForgeConfiguration(boolean)` — drive the NeoForge config-phase handshake.
  - `tolerantDecoding(boolean)` — drop un-decodable modded packets to stay connected.
  - `extraConfigChannels(String...)` — declare additional (mod-specific) config channels.
  - `answerUnknownLoginQueries(boolean)`.
- **`ModLoader`** — `VANILLA / FORGE_FML1 / FORGE_FML2 / FORGE_FML3 / NEOFORGE`.
- **`NeoForgeClientListenerWrapper`** / **`FmlLoginHandshakeListener`** — the session listeners
  `attach` registers; usable directly if you manage registration yourself.

## Status

| Target | Status |
|---|---|
| Forge FML2/FML3 — MC 1.15.2 / 1.16.5 / 1.18.2 / 1.20.1 (+ real mods) | ✅ **Handshake** validated against live servers |
| Forge FML1 — MC 1.12.2 (`FML\|HS`) | ✅ **Handshake** validated against a live server (incl. with JEI) |
| NeoForge — MC 1.21.11 (NeoForge 21.11.42) | ✅ **Joins and stays connected** (real xinbot bot, version-matched) |

**Known limits (honest):**
- **Tolerant decoding drops data it can't parse.** The bot stays *connected* to a modded server,
  but mcprotocollib is a vanilla protocol library — packets carrying modded registry entries are
  dropped, so the bot's view of the world is incomplete. Fine for presence / chat / basic actions;
  not a full modded client.
- **The FML handshake is validated; "joining and staying" was fully exercised on NeoForge** (where
  the bot is version-matched to the server). For FML1/2/3, connecting a 1.21.x bot to those older
  servers additionally requires version translation (e.g. [XinVia](https://github.com/huangdihd/XinVia)),
  which is a separate concern.

Trace any handshake with `-Dmodbridge.dumpNeoForge=true`.

## Building

```bash
mvn clean package
```

Java 17 or newer. Drop the jar into xinbot's `plugin` directory.

## License

GPL-3.0-or-later, see [LICENSE](LICENSE).
