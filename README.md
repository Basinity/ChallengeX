# ChallengeX

Compose your own "Minecraft, but..." challenge from building blocks: rules (a trigger paired with an effect), an optional goal, and modifiers. Server-side Fabric mod for Minecraft 26.2; vanilla clients can join.

Early development. Nothing is released yet.

## Modules

- `core` — the platform-agnostic challenge engine. No Minecraft dependency.
- `fabric` — the Fabric adapter: maps server events onto the engine and executes its effects.
- `web` — the companion builder site, static and client-side only. See `web/README.md`.

## Building

```
./gradlew build
```

Requires Java 25 (via Gradle toolchains).

The site renders its forms from a catalog generated out of `core`'s registries, so regenerate it after changing a catalog entry:

```
./gradlew :core:exportCatalog
```
