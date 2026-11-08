"""Opt-in functional regression: cached settings must follow a newly published Core lock."""
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("snapshot_core", Path(__file__).with_name("snapshot_core.py"))
snapshot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)


@unittest.skipUnless(os.environ.get("OWNTV_RUN_GRADLE_CACHE_TEST") == "1", "Opt-in real Gradle configuration-cache test")
class SettingsCacheTest(unittest.TestCase):
    def test_cached_settings_follow_a_new_core_snapshot_and_then_reuse_it(self):
        with tempfile.TemporaryDirectory(prefix="owntv-settings-cache-") as directory:
            folder = Path(directory)
            app, core = folder / "app", folder / "core"
            app.mkdir()
            for name in snapshot.SOURCE_FILES[:-1]:
                path = core / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("", encoding="utf-8")
            (core / "settings.gradle.kts").write_text('rootProject.name = "ProbeCore"\n', encoding="utf-8")
            (app / "settings.gradle.kts").write_text(
                'rootProject.name = "SnapshotProbe"\napply(from = "custom-core.settings.gradle.kts")\n', encoding="utf-8")
            (app / "build.gradle.kts").write_text("", encoding="utf-8")
            (app / "gradle.properties").write_text(
                "org.gradle.configuration-cache=true\norg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8\n", encoding="utf-8")
            shutil.copyfile(ROOT / "gradle/custom-core.settings.gradle.kts", app / "custom-core.settings.gradle.kts")

            def publish(marker):
                (core / "build.gradle.kts").write_text(
                    'extra["coreVersion"] = "test.1"\n'
                    f'tasks.register("probe") {{ doLast {{ println("core_marker_{marker}") }} }}\n', encoding="utf-8")
                return snapshot.publish_snapshot(core, app)

            def run(label):
                wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
                result = subprocess.run(
                    [str(wrapper), "-p", str(app), ":OwnTV_Core:probe", "--configuration-cache", "--console=plain",
                     "-PowntvUseCoreSnapshot=true"],
                    cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=240,
                )
                output = result.stdout + result.stderr
                evidence = os.environ.get("OWNTV_CACHE_TEST_LOG_DIR")
                if evidence:
                    Path(evidence).mkdir(parents=True, exist_ok=True)
                    (Path(evidence) / f"settings-cache-{label}.log").write_text(output, encoding="utf-8")
                self.assertEqual(0, result.returncode, output)
                self.assertIn("BUILD SUCCESSFUL", output)
                return output

            first = publish("A")
            self.assertIn("core_marker_A", run("initial"))
            # First extraction creates files whose previous absence was a configuration input.
            run("initial-settle")
            warm = run("warm")
            self.assertIn("Reusing configuration cache.", warm)
            self.assertIn("core_marker_A", warm)

            second = publish("B")
            self.assertNotEqual(first["sha256"], second["sha256"])
            updated = run("updated")
            self.assertNotIn("Reusing configuration cache.", updated)
            self.assertIn("core_marker_B", updated)
            self.assertNotIn("core_marker_A", updated)
            self.assertTrue((app / ".gradle/core-snapshot" / second["sha256"] / "OwnTV_Core").is_dir())

            run("updated-settle")
            warm_again = run("updated-warm")
            self.assertIn("Reusing configuration cache.", warm_again)
            self.assertIn("core_marker_B", warm_again)


if __name__ == "__main__":
    unittest.main()
