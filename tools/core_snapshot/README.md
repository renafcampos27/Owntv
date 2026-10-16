# Customized TV Core snapshot

After validating a change in the TV Core checkout:

```powershell
python tools/core_snapshot/snapshot_core.py --core C:/Users/renat/Downloads/OwnTV_Core
python -m unittest discover -s tools/core_snapshot -p 'test_*.py'
```

Build resolution follows `core-source-lock.json`, including its archive path and SHA-256.
The ZIP is now named `vendor/owntv-core-source-<sha256>.zip`. Publish the ZIP and lock together
in version control after reviewing them. An upstream catalogue pin alone does not update this Core.

Publication validates the version and reads all selected inputs before making the ZIP visible.
It writes and flushes temporary files, publishes immutable content, then replaces the lock with
one atomic rename on the same filesystem. A failure before that rename leaves the old pair usable;
afterwards, the lock references a complete ZIP. Temporary files are removed on ordinary failures.
A process/power interruption can leave temporary or unreferenced files; it cannot make the old lock
reference a partially overwritten ZIP. Filesystem/device durability after power loss remains subject
to the operating system and storage guarantees.

The tool never removes old archives, including the legacy fixed-name ZIP. An unused ZIP can be
removed manually only after confirming that no required lock, branch or recovery point references
it. Do not rename or overwrite a ZIP referenced by a lock. Failed publication may leave a harmless
unreferenced new ZIP that can be reused by the next successful run.

If a local `owntv.corePath` points at changed, unpublished sources, Gradle intentionally rejects
that mismatch even though the previous ZIP/lock pair is still valid. To validate the recorded
snapshot independently of that override, use `-PowntvUseCoreSnapshot=true`; for example:

```powershell
.\gradlew.bat :app:compileStandardDebugKotlin '-PowntvUseCoreSnapshot=true'
```
