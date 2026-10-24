# Release checks

`version.properties` at the project root is the single source for `versionName` and
`versionCode`. Keep the existing signing key. This change does not bump the version,
generate a key, or publish anything.

- Local Sync/compile/tests work without release secrets. A release without a configured
  key stays unsigned; it never silently uses the debug key.
- Before tag publication, configure the existing keystore secrets and repository variable
  `RELEASE_CERT_SHA256` with its authorized certificate fingerprint. The value must be
  established from the actual authorized signing identity, not invented from a filename.
- Tag names must equal `v<versionName>`. CI inspects the preceding GitHub release's actual
  signed APK (package/version/certificate); historical source defaults are insufficient
  because older workflows allowed environment overrides. Unavailable evidence fails the
  release gate. Only a missing first release
  permits no previous code. Non-tag builds use the same explicit local version.
- `select_apk.py` chooses one real ABI match through AGP metadata. `verify_artifact.py`
  verifies package, name/code, absence of debug, actual ABIs and certificate on the APK.
  The distribution job checks hashes again and requires successful build and Android jobs.
- Actions refs are pinned in workflows; `actions-lock.json` records the official refs used.
  Updating a ref requires review and corresponding tests, not an automatic latest-tag fetch.

Normal builds use Gradle's strict dependency verification. The reviewed SHA-256 files are
`gradle/verification-metadata.xml` in App and Core. Updating dependencies requires explicit
regeneration followed by independent corroboration; never accept a new local hash alone:

```powershell
python tools/release/verify_dependencies.py gradle/verification-metadata.xml --evidence tools/release/dependency-provenance.json
python tools/release/verify_dependencies.py C:/Users/renat/Downloads/OwnTV_Core/gradle/verification-metadata.xml --evidence tools/release/dependency-provenance.json
python -m unittest discover -s tools/release -p 'test_*.py'
```

The verifier uses official Google Maven, Maven Central, Plugin Portal and the configured
OwnTV native repository. It checks repository SHA-256, hashes actual bytes when needed,
or verifies the official module metadata's SHA-256. Logical artifact names and explicitly
mapped variants are supported; a mismatching digest is an error. The evidence JSON is
review material to commit with the metadata. It is not an exemption from Gradle checks.

Android Studio also resolves sources and Javadoc for plugins and libraries during sync.
A successful Kotlin compile alone does not exercise those source/documentation requests.
`ide-source-artifacts.json` (legacy filename) records 409 exact IDE attachments checked
on 2026-10-24, including the Gradle 9.8.0/9.7.1 source ZIPs, Kotlin/KSP variants,
library sources and 22 Javadoc JARs. Additional library attachments were inventoried
from downloaded files belonging to components already in the reviewed build metadata.
Every new SHA-256 was independently corroborated against official repositories;
the local cache alone was never treated as proof.
Run the following additional checks with `JAVA_HOME` pointing to the Android Studio JDK:

```powershell
python tools/release/verify_ide_sources.py gradle/verification-metadata.xml --wrapper ./gradlew.bat --audit-cache
python tools/release/verify_ide_sources.py C:/Users/renat/Downloads/OwnTV_Core/gradle/verification-metadata.xml --wrapper C:/Users/renat/Downloads/OwnTV_Core/gradlew.bat --audit-cache
./gradlew.bat -I tools/release/verify_ide_model.init.gradle :verifyIdeBuildscriptSources :OwnTV_Core:verifyIdeBuildscriptSources --no-configuration-cache --dependency-verification=strict
```

The probe resolves those exact source/documentation files in a temporary Gradle project
with strict verification and the supplied metadata. Core selects 275 matching attachments.
AndroidX sample aliases are resolved through their official module documentation variant,
which may contain both regular sources and sample sources under different logical names.
The fixture records the published `docsType` (usually `sources`; Material Icons 1.7.8
uses `fake-sources`), and Gradle verifies that module metadata and every selected file.
The probe requires each expected filename to be returned; it never guesses a classifier
from an aliased filename. `--audit-cache` also reports every cached source, sample or
Javadoc attachment missing an exact hash for a reviewed component, without filtering
filenames by the module prefix. This audit is read-only and never authorizes a new file.
Each attachment resolves in its own configuration so version conflict resolution cannot
silently skip an older requested version. The init-script check additionally queries
sources from the actual buildscript dependency graphs of App and included Core.
It also checks each returned file against the exact component/artifact SHA-256 in
that build's XML: source-query results alone can defer verification failures until
a subsequent resolution. Run the same init-script task on standalone Core as well,
because its wrapper can select different transitive Kotlin source versions.
It does not modify the working builds or produce an APK. On future dependency updates,
refresh the attachment fixture from actual IDE requests and corroborate newly needed hashes;
never disable verification or trust every source/documentation archive through a filename pattern.

The Android CI job targets API 26 and 34. Its presence in YAML and local compilation of
instrumented sources do not prove that those remote emulator tests ran or that playback
works on an actual box. See `docs/IMPLEMENTACAO_SUBFASES_RESTANTES_2026-10-24.md`.
