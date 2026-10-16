import importlib.util
from pathlib import Path
import tempfile
import unittest
import hashlib
import json
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("snapshot_core", Path(__file__).with_name("snapshot_core.py"))
snapshot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)


class SnapshotInputsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name) / "app"
        self.core = Path(self.temporary.name) / "core"
        self.root.mkdir()
        for name in snapshot.SOURCE_FILES[:-1]:
            path = self.core / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('extra["coreVersion"] = "test.1"' if name == "build.gradle.kts" else "input")

    def assert_valid_pair(self, expected):
        lock = json.loads((self.root / "core-source-lock.json").read_text())
        self.assertEqual(expected, lock)
        self.assertEqual(lock["sha256"], hashlib.sha256((self.root / lock["archive"]).read_bytes()).hexdigest())

    def test_publication_is_deterministic_and_preserves_old_archive(self):
        first = snapshot.publish_snapshot(self.core, self.root)
        self.assertEqual(first, snapshot.publish_snapshot(self.core, self.root))
        (self.core / "gradle.properties").write_text("changed")
        second = snapshot.publish_snapshot(self.core, self.root)
        self.assertNotEqual(first["archive"], second["archive"])
        self.assertTrue((self.root / first["archive"]).is_file())
        self.assert_valid_pair(second)

    def test_missing_version_does_not_change_previous_pair(self):
        first = snapshot.publish_snapshot(self.core, self.root)
        (self.core / "build.gradle.kts").write_text("no version")
        with self.assertRaises(ValueError):
            snapshot.publish_snapshot(self.core, self.root)
        self.assert_valid_pair(first)

    def test_corrupt_immutable_archive_is_rejected_instead_of_overwritten(self):
        first = snapshot.publish_snapshot(self.core, self.root)
        archive = self.root / first["archive"]
        archive.write_bytes(b"corrupt")
        old_lock = (self.root / "core-source-lock.json").read_bytes()
        with self.assertRaises(ValueError):
            snapshot.publish_snapshot(self.core, self.root)
        self.assertEqual(b"corrupt", archive.read_bytes())
        self.assertEqual(old_lock, (self.root / "core-source-lock.json").read_bytes())

    def test_failed_input_read_preserves_previous_pair_and_removes_temporary_files(self):
        first = snapshot.publish_snapshot(self.core, self.root)
        with patch.object(Path, "read_bytes", side_effect=OSError("read failed")):
            with self.assertRaises(OSError):
                snapshot.publish_snapshot(self.core, self.root)
        self.assert_valid_pair(first)
        self.assertEqual([], list(self.root.rglob("*.tmp")))

    def test_failure_at_lock_swap_keeps_previous_pair_usable(self):
        first = snapshot.publish_snapshot(self.core, self.root)
        (self.core / "gradle.properties").write_text("new inputs")
        replace = snapshot.os.replace
        def fail_lock(source, destination):
            if Path(destination).name == "core-source-lock.json":
                raise OSError("lock publication failed")
            replace(source, destination)
        with patch.object(snapshot.os, "replace", side_effect=fail_lock):
            with self.assertRaises(OSError):
                snapshot.publish_snapshot(self.core, self.root)
        self.assert_valid_pair(first)
        self.assertEqual([], list(self.root.rglob("*.tmp")))

    def test_packages_build_inputs_without_local_secrets_or_outputs(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            names = ["build.gradle.kts", "core/src/main/Test.kt", "core/schemas/1.json",
                     "player-core/src/test/Test.kt", "tools/i18n/safe_literals.txt",
                     "core/local.properties", "core/src/main/local.properties",
                     "tools/.env", "tools/cache.jks", "tools/__pycache__/script.pyc",
                     "tools/build/output.txt", "tools/runs/private.json", ".git/config",
                     "local.properties", "core/build/compiled.jar"]
            for name in names:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("input", encoding="utf-8")
            self.assertEqual({p.relative_to(root).as_posix() for p in snapshot.build_files(root)},
                             set(names[:5]))


if __name__ == "__main__":
    unittest.main()
