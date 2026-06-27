# XinModBridge

> A [xinbot](https://github.com/huangdihd/xinbot) **library plugin** that completes the
> Forge / NeoForge (FML) login handshake, so the bot can join **modded servers** whose
> checks are mod-list / channel based — without bundling any mods.

> ⚠️ **Experimental (`0.1.0-SNAPSHOT`, pre-release).** The Forge **FML2/FML3 login handshake**
> (`fml:loginwrapper` ModList echo + Acknowledge) is **validated against live Forge servers
> 1.15.2, 1.16.5, 1.18.2 and 1.20.1** (the last also with real mods). **NeoForge / Forge
> 1.20.2+** and **legacy FML1 (1.7–1.12)** are **not supported** — see **Status** below for
> why. Also note: connecting a 1.21.x bot to an older modded server still needs version
> translation (e.g. [XinVia](https://github.com/huangdihd/XinVia)), which is separate.

---

## What it does

Joining a modded server depends on the loader, and the loaders are genuinely different — so
there is no single "universal handshake". XinModBridge is a **detect-and-respond** bridge:

| Loader | Handling |
|---|---|
| **Vanilla / Fabric / Quilt** | Nothing needed — they are protocol-vanilla; connect directly. |
| **Forge FML2/FML3** (MC ~1.15–1.20.1) | ✅ Answer the `fml:loginwrapper` login queries; **echo** the server's mod list / channels / registries back so a mod-less bot is accepted. The handshake-address marker must match the server's FML net version: `FML2` for ≤ 1.16.x, `FML3` for ≥ 1.18. |
| **NeoForge** (MC 1.20.2+) | ⛔ Configuration-phase `neoforge:` handshake. Mapped but **not supported** — see Status. |
| **Forge FML1** (MC 1.7–1.12) | ⛔ PLAY-phase `FML\|HS` handshake — **not implemented.** |

It only targets the common **"does the client have these mods/channels"** style of check.
Mods that do **behavioural** checks (compute-and-respond, anti-cheat handshakes) cannot be
spoofed and are out of scope.

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

Code — attach the bridge in your meta plugin's `onEnable()`:

```java
import xin.bbtt.modbridge.XinModBridgeProvider;
import xin.bbtt.modbridge.FmlLoginHandshakeListener;
import xin.bbtt.modbridge.ModBridgeOptions;

private FmlLoginHandshakeListener bridge;

public void onEnable() {
    // Default: reactive FML handshake with the FML3 hostname marker (needed so the
    // server switches into FML mode — without it the bot is treated as vanilla).
    bridge = XinModBridgeProvider.attach(this);

    // For older Forge servers (MC ≤ 1.16.x, FML net version 2), use the FML2 marker:
    // bridge = XinModBridgeProvider.attach(this, ModBridgeOptions.builder()
    //         .fmlMarkerVersion("FML2").build());
}
```

The listener is unregistered automatically when your plugin is disabled.

It composes with [XinVia](https://github.com/huangdihd/XinVia): a meta plugin can
`depend: [XinVia, XinModBridge]` to reach a server that is both cross-version and modded.

## Status

| Target | Status |
|---|---|
| Forge FML2/FML3 — MC 1.15.2, 1.16.5, 1.18.2, 1.20.1 (+ real mods) | ✅ Validated against live servers — bot completes the handshake and joins |
| NeoForge / Forge 1.20.2+ (configuration phase) | ⛔ Not supported (see below) |
| Forge FML1 — MC 1.7–1.12 (`FML\|HS`) | ⛔ Not implemented |

**Why NeoForge can't be done from here.** NeoForge negotiates its modded network in the
CONFIGURATION phase. Its `CommonVersion`/`CommonRegister` tasks are only registered if the
connection already `hasChannel(...)` for them *at the moment `RegisterConfigurationTasksEvent`
fires* — i.e. at the very start of config, before any client payload is processed. A reactive
`SessionListener` that declares channels via `minecraft:register` does so too late (verified:
the replies are sent and received, but the server has already decided its task set). Completing
it requires driving the client's config-phase state machine the way a real NeoForge client does,
which is below the mcprotocollib layer this plugin works at. The full protocol trace is kept in
`NeoForgeConfigHandshake`; run with `-Dmodbridge.dumpNeoForge=true` to reproduce.

**FML1** (1.7–1.12) uses a PLAY-phase `FML|HS` handshake (REGISTER + `FML|HS` ServerHello →
ClientHello → ModList → …). It is reactively feasible but not yet implemented.

## Building

```bash
mvn clean package
```

Java 17 or newer. Drop the jar into xinbot's `plugin` directory.

## License

GPL-3.0-or-later, see [LICENSE](LICENSE).
