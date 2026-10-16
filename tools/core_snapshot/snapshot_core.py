"""Package the customized Core sources for offline resolution of the modules in TV builds.

Run after validating a Core change: python tools/core_snapshot/snapshot_core.py --core <checkout>
Only build inputs are included; never local.properties, Git state, build outputs or signing files.
"""
import argparse
import hashlib
import json
import os
import tempfile
import re
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIRS = ("core/src", "core/schemas", "player-core/src", "tools", "gradle")
SOURCE_FILES = (
    "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat",
    "core/build.gradle.kts", "core/consumer-rules.pro",
    "player-core/build.gradle.kts", "player-core/consumer-rules.pro", "LICENSE",
)
EXCLUDED_PARTS = {"build", ".gradle", ".git", "__pycache__", "node_modules", "runs"}
EXCLUDED_NAMES = {"local.properties", "keystore.properties", "signing.properties", ".env"}


def build_files(core):
    candidates = [core / relative for relative in SOURCE_FILES]
    for folder in SOURCE_DIRS:
        candidates.extend((core / folder).rglob("*"))
    return sorted({p for p in candidates if p.is_file()
                   and not (set(p.relative_to(core).parts) & EXCLUDED_PARTS)
                   and p.name not in EXCLUDED_NAMES
                   and p.suffix not in {".pyc", ".jks", ".keystore", ".log"}})


def publish_snapshot(core, root=ROOT):
    """Publish immutable content first; atomically switch the lock only after it is complete.

    A crash before the lock swap leaves the previous pair valid. An unreferenced new ZIP is
    harmless and intentionally retained; old snapshots are never deleted by this command.
    """
    core = core.resolve()
    if not all((core / name).is_file() for name in SOURCE_FILES[:-1]):
        raise ValueError("The Core checkout is missing required build inputs")
    version = re.search(r'extra\["coreVersion"\]\s*=\s*"([^"\r\n]+)"',
                        (core / "build.gradle.kts").read_text(encoding="utf-8"))
    if version is None or not version.group(1).strip():
        raise ValueError("Core version is missing")
    files = build_files(core)
    vendor = root / "vendor"
    vendor.mkdir(parents=True, exist_ok=True)
    archive_temp = lock_temp = None
    try:
        with tempfile.NamedTemporaryFile(dir=vendor, suffix=".tmp", delete=False) as stream:
            archive_temp = Path(stream.name)
            with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
                for path in files:
                    entry = zipfile.ZipInfo(path.relative_to(core).as_posix(), (2026, 10, 11, 0, 0, 0))
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(entry, path.read_bytes())
            stream.flush()
            os.fsync(stream.fileno())
        checksum = hashlib.sha256(archive_temp.read_bytes()).hexdigest()
        destination = vendor / f"owntv-core-source-{checksum}.zip"
        if destination.exists():
            if hashlib.sha256(destination.read_bytes()).hexdigest() != checksum:
                raise ValueError("Existing immutable Core archive has an invalid checksum")
        else:
            os.replace(archive_temp, destination)
        lock = {
            "schema": 1, "archive": destination.relative_to(root).as_posix(), "sha256": checksum,
            "version": version.group(1), "files": len(files),
            "description": "Customized TV Core source snapshot; not an upstream release equivalence claim",
        }
        with tempfile.NamedTemporaryFile(dir=root, mode="w", encoding="utf-8", newline="\n",
                                         suffix=".tmp", delete=False) as stream:
            lock_temp = Path(stream.name)
            stream.write(json.dumps(lock, indent=2) + "\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(lock_temp, root / "core-source-lock.json")
        return lock
    finally:
        for temporary in (archive_temp, lock_temp):
            if temporary is not None:
                temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--core", required=True, type=Path)
    args = parser.parse_args()
    try:
        lock = publish_snapshot(args.core)
    except (ValueError, OSError) as failure:
        parser.error(str(failure))
    print(f"Core snapshot: {lock['files']} files, SHA-256 {lock['sha256']}")


if __name__ == "__main__":
    main()
