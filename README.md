# DerMegaMecha Client

**Built for Performance. Designed for PvP. Fair by Nature.**

A fair-play, client-side Minecraft Forge 1.21.1 PvP customization client for legitimate multiplayer play.

## Server compatibility

HUD features can be disabled when individual servers prohibit a modification or supported integration. This project does not claim universal server compatibility or anti-cheat permission. BedWars stars and public statistics must be sourced only from supported public server APIs or server-provided data.

## Fair-play scope

This client contains no combat automation, reach or hitbox changes, movement modifications, wall-based entity awareness, ESP, tracers, freecam, packet manipulation, inventory automation, or anti-cheat bypasses.

## Requirements

- JDK 21
- Minecraft 1.21.1
- Minecraft Forge 52.x
- A standard Forge installation

## Build

```bash
./gradlew build
```

The distributable JAR is generated under `build/libs/`.

## Run

```bash
./gradlew runClient
```

## Controls

Press **Right Shift** to open the DerMegaMecha settings menu.

## Configuration

Settings are stored at:

```text
config/dermegamecha-client.json
```

## Compatibility

The client uses standard Forge render and key-mapping events. Keep optional entity optimization disabled if it conflicts with another rendering or entity mod.
