import importlib.util
import io
import json
from pathlib import Path
import unittest
from unittest.mock import patch
import urllib.error

spec = importlib.util.spec_from_file_location('verify_dependencies', Path(__file__).with_name('verify_dependencies.py'))
module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)

class DependencyProofTest(unittest.TestCase):
    def test_gradle_sources_are_checked_against_official_distribution(self):
        expected = 'a' * 64
        with patch.object(module.urllib.request, 'urlopen', return_value=io.BytesIO(expected.encode())) as request:
            proof = module.corroborate(('gradle', 'gradle', '9.8.0', 'gradle-9.8.0-src.zip', expected))
            request.assert_called_once_with('https://services.gradle.org/distributions/gradle-9.8.0-src.zip.sha256', timeout=40)
            self.assertEqual(expected, proof['sha256'])
        with self.assertRaises(ValueError):
            module.official_urls('gradle', 'gradle', '9.8.0', '../unreviewed.zip')

    def test_a_local_digest_is_rejected_when_the_publisher_differs(self):
        with patch.object(module.urllib.request, 'urlopen', return_value=io.BytesIO(b'a' * 64)):
            with self.assertRaises(ValueError): module.corroborate(('junit', 'junit', '4.13.2', 'junit.jar', 'b' * 64))

    def test_logical_artifact_names_require_matching_official_module_metadata(self):
        def response(url, **kwargs):
            if url.endswith('.module'):
                return io.BytesIO(json.dumps({'variants': [{'files': [{'name': 'runtime.aar', 'sha256': 'a' * 64}]}]}).encode())
            raise urllib.error.HTTPError(url, 404, 'Not found', {}, None)
        with patch.object(module.urllib.request, 'urlopen', side_effect=response):
            proof = module.corroborate(('androidx.compose.runtime', 'runtime-android', '1.12.0', 'runtime.aar', 'a' * 64))
            self.assertEqual(module.GOOGLE + 'androidx/compose/runtime/runtime-android/1.12.0/runtime-android-1.12.0.module', proof['proof'])
            with self.assertRaises(ValueError):
                module.corroborate(('androidx.compose.runtime', 'runtime-android', '1.12.0', 'runtime.aar', 'b' * 64))
