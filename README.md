# ChallengeX

Compose your own "Minecraft, but..." challenge from building blocks: rules (a trigger paired with an effect), an optional goal, and restraints. Server-side Fabric mod for Minecraft 26.2; vanilla clients can join.

Early development. Nothing is released yet.

## Modules

- `core` — the platform-agnostic challenge engine. No Minecraft dependency.
- `fabric` — the Fabric adapter: maps server events onto the engine and executes its effects.

## Building

```
./gradlew build
```

Requires Java 25 (via Gradle toolchains).
