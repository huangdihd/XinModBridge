# XinModBridge

> A [xinbot](https://github.com/huangdihd/xinbot) **library plugin** that completes the
> Forge / NeoForge (FML) login handshake, so the bot can join **modded servers** whose
> checks are mod-list / channel based — without bundling any mods.

> ⚠️ **Early / experimental (`0.1.0-SNAPSHOT`, pre-release).** The framework, the
> `fml:loginwrapper` plumbing and the FML3 spoof handshake are implemented and unit-tested
> for internal consistency, **but have not yet been validated against a live Forge/NeoForge
> server.** Message indices and field layouts are version-sensitive and very likely need
> tuning once tested. See **Status** below.

---

## What it does

Joining a modded server depends on the loader, and the loaders are genuinely different — so
there is no single "universal handshake". XinModBridge is a **detect-and-respond** bridge:

| Loader | Handling |
|---|---|
| **Vanilla / Fabric / Quilt** | Nothing needed — they are protocol-vanilla; connect directly. |
| **Forge FML2/FML3** (MC 1.13–1.20.1) | Answer the `fml:loginwrapper` login queries; **echo** the server's mod list / channels / registries back so a mod-less bot is accepted. |
| **NeoForge** (MC 1.20.2+) | Configuration-phase `neoforge:` handshake — **extension point only, not implemented yet.** |
| **Forge FML1** (MC 1.7–1.12) | Not implemented yet. |

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
    // Default: reactive FML login handshake, no hostname marker injection.
    bridge = XinModBridgeProvider.attach(this);

    // Or, for older Forge servers that only switch into FML mode when the
    // handshake hostname carries a marker:
    // bridge = XinModBridgeProvider.attach(this, ModBridgeOptions.builder()
    //         .injectFmlMarker(true).fmlMarkerVersion("FML3").build());
}
```

The listener is unregistered automatically when your plugin is disabled.

It composes with [XinVia](https://github.com/huangdihd/XinVia): a meta plugin can
`depend: [XinVia, XinModBridge]` to reach a server that is both cross-version and modded.

## Status

Implemented & tested:
- Library/plugin scaffold, `XinModBridgeProvider` API, lifecycle, real-xinbot load.
- `fml:loginwrapper` wrap/unwrap and the FML3 ModList **echo** reply + Acknowledge, with an
  internal round-trip unit test.

Needs a real server to validate / still TODO:
- Confirm FML3 message indices & field layouts against an actual Forge 1.16–1.20.1 server.
- FML2 (1.13–1.15) and FML1 (1.7–1.12) variants.
- NeoForge configuration-phase handshake (`neoforge:`), currently a no-op extension point.

## Building

```bash
mvn clean package
```

Java 17 or newer. Drop the jar into xinbot's `plugin` directory.

## License

GPL-3.0-or-later, see [LICENSE](LICENSE).
