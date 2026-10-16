# Customized OwnTV TV Core

`owntv-core-source.zip` contains only the Core build inputs, including sources, tests,
schemas and build tooling. It is **not an APK** and is not included as a resource in the APK.
`../core-source-lock.json` records its version and SHA-256 identity.

Local and CI builds use this source snapshot instead of resolving an older published Core.
With no local override, Gradle verifies and expands it into the project's `.gradle` cache.
If `owntv.corePath` is configured, Gradle verifies that its build inputs match the snapshot;
it fails explicitly on a difference instead of silently building different code.
CI explicitly selects the snapshot, even if an old machine-local override exists.

After an intentional Core change, validate it, then regenerate the snapshot from the TV Core:

```powershell
python tools/core_snapshot/snapshot_core.py --core C:/Users/renat/Downloads/OwnTV_Core
```

Keep the archive, lock, catalog pin and Core version together in the next project commit.
Do not edit the expanded cached source. No signing keys, local.properties, Git metadata,
build outputs or personal Gradle credentials are copied by the snapshot tool.

The custom version suffix identifies this fork. It does not claim full equivalence with
an upstream Core release. Native libmpv remains resolved from the pinned public dependency.
