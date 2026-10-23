# NasukoCustom 0.1.0 Release Notice

**2026-10-24 — Mystcraft Unofficial NasukoCustom 0.1.0**

The Dimensional Monsters page, its diminishing instability reward, two hostile mob implementations, original Blender mesh/texture sources and animations are NasukoCustom additions made on 2026-10-24. Original upstream PNG/OGG files remain unchanged. Runtime validation also found and corrected generated Archivist profession holders that could not be serialized by Minecraft 26.3.

The first NasukoCustom release combines the Minecraft 26.3 / NeoForge / Java 25 migration and subsequent book interaction, entity simulation and structure persistence fixes. Local migration and validation notes are kept separately in ignored `Docs/`; public attribution and license/provenance notices remain in the repository and binary. Earlier identifiers below are retained as development history.

---

# NasukoCustom 26.3.2 Bug Fix Notice

**2026-10-24 — 0.13.7.06-nasuko.26.3.2**

Correct 26.3 mouse-button mappings, physical book/meteor movement simulation authority, and structure processor codec registration. Extend client/server regressions to exercise book-menu network activation, selected-item dropping and structure processor persistence.

---

# NasukoCustom 26.3 Modification Notice

**2026-10-23 — 0.13.7.06-nasuko.26.3.1**

Display name: **Mystcraft Unofficial NasukoCustom**. Minecraft 26.3, NeoForge 26.3.0.95, Java 25, ModDevGradle 2.0.148.
Derived from ueno96941-Clown/Mystcraft-Legacy-Unofficial-NeoForge-Port at `7e83df32feb5c68e1faee558daab2338ccadbb5d`.
Internal identifiers remain `mystcraft`; this name does not claim authorship of the original Mystcraft or the 1.21.1 port.

Changes include 26.3 registry/data formats, ValueInput/ValueOutput serialization, transactional resource handlers, villager trades, worldgen interfaces, independent Age clocks/weather, dynamic world installation, modern render extraction/submission and RenderPearl sky pipelines, item model definitions, GUI input/extraction, and opt-in runtime integration tests.
Original PNG/OGG assets, license texts and provenance are preserved. See README.md and KNOWN_ISSUES.md for verification and compatibility limits.
The following is the upstream 1.21.1 modification notice, retained as historical attribution.

---

# Modification Notice

**Release baseline:** `0.13.7.06-port.1.0.0-Ueno` / 2026-09-26

This repository is an unofficial, substantially modified port of Mystcraft / Mystcraft Legacy.

The upstream project targeted Minecraft 1.12.2 / Forge. This port adapts it to Minecraft 1.21.1 / NeoForge and Java 21. Major modification areas include dimension lifecycle, Age generation bridges, rendering, networking, data components, GUI/block-entity integration, world-generation compatibility, dynamic Symbol resolution, Crystal Portal vehicle/passenger/projectile transfer, lazy Age load/unload handling, legacy Linking Book behavior, JEI compatibility, Instability restoration/calibration, modded Dense Ores compatibility, ModernFix stronghold-cache integration, startup/runtime cleanup, and build/release tooling.

The port preserves the active legacy Instability gameplay families while adapting implementation details to the modern engine. Dense Ores retains a bounded compatibility extension for modded ores. Port-only QA Symbol Pages, manual Decay helper items, release-disabled telemetry, and the port-only first-login starter/tutorial Folder were removed from the release-facing build. Normal Decay and other Instability gameplay remain available.

Source-level regression tests and maintenance harnesses may exist in the development archive, but they are not runtime JAR content and development-only harnesses are excluded from the public source export. The block-instability profiler remains because its non-ore result participates in live legacy Instability scoring.

The public release identifier is `0.13.7.06-port.1.0.0-Ueno`. `Ueno` is only a compact maintainer/build identifier so this unofficial port can be distinguished from other builds. The displayed project name remains **Mystcraft Legacy (Unofficial 1.21.1 Port)**; the suffix does not imply ownership of Mystcraft or endorsement by the upstream project.

This is **not an official upstream Mystcraft release**. No endorsement by Mystcraft Org, XCompWiz, Mojang/Microsoft, NeoForged, or Cyan is implied.

See `ATTRIBUTION.md`, `LICENSE`, `LICENSES/`, `SOURCE_PROVENANCE.md`, and `COMPLIANCE_AND_LICENSE_NOTICE.md` for attribution, license, provenance, and distribution-boundary information.
