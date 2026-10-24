# Public Release Excludes

Development checkpoint archives are working/handoff packages and must not be published verbatim.

The public source exporter excludes:

- `.git/`, `.gradle/`, `build/`, `run/`, `runs/`, `logs/`, `crash-reports/`, IDE/cache directories;
- the local `Docs/` migration and validation notes;
- root-level internal checkpoint files beginning with `CP` followed by a checkpoint number;
- development-only harnesses under `tools/harness/` and top-level Instability checkpoint harnesses;
- obsolete/development-only helper scripts and embedded `tools/gradle-*` distributions;
- compiled `.class` files, logs, ZIP archives and development `.patch` files;
- JARs other than an optional standard `gradle/wrapper/gradle-wrapper.jar` whose checksum is independently pinned;
- original Mystcraft comparison JARs/source archives, Minecraft/NeoForge binaries and other local QA/reference material.

The public package intentionally retains the buildable source tree, Gradle project/wrapper scripts, wrapper bootstrap scripts, public-export/binary-audit tooling, license texts, attribution, modification notice, source/asset provenance, compliance report, known issues, changelog, release checklist and listing template.

Generate and audit a public package with:

```text
python tools/make_public_source_export.py <output.zip>
python tools/audit_public_source_export.py <output.zip>
```

After building the release JAR, audit the exact binary with:

```text
python tools/audit_release_jar.py build/libs/mystcraft-unofficial-nasukocustom-0.1.1.jar
```

A passing package audit is a release gate, not a substitute for re-reading current third-party policies or running the final release-candidate tests.
