import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import io
import urllib.error

spec = importlib.util.spec_from_file_location('release_version', Path(__file__).with_name('version.py'))
module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)

class VersionTest(unittest.TestCase):
    @patch.dict('os.environ', {'GITHUB_REPOSITORY': 'owner/repository', 'GH_TOKEN': 'test'})
    def test_published_code_is_read_from_the_existing_release_tag(self):
        with patch.object(module.urllib.request, 'urlopen', return_value=io.BytesIO(b'{"tag_name":"v105.1.0.5","assets":[{"name":"OwnTV.apk","size":100}]}')), \
             patch.object(module, 'published_apk_code', return_value=10510005) as inspect:
            self.assertEqual(10510005, module.previous_published_code())
            inspect.assert_called_once_with('owner/repository', 'v105.1.0.5', 'OwnTV.apk')

    def test_previous_code_comes_from_signed_apk_badging_and_not_the_source_default(self):
        import os
        with tempfile.TemporaryDirectory() as folder:
            tools = Path(folder) / 'build-tools' / '36.0.0'; tools.mkdir(parents=True)
            (tools / ('aapt.exe' if os.name == 'nt' else 'aapt')).touch()
            (tools / ('apksigner.bat' if os.name == 'nt' else 'apksigner')).touch()
            def download(args, **kwargs):
                (Path(args[-1]) / 'OwnTV.apk').write_bytes(b'fixture')
            with patch.dict('os.environ', {'ANDROID_HOME': folder, 'RELEASE_CERT_SHA256': 'a' * 64}), \
                 patch.object(module.subprocess, 'run', side_effect=download), \
                 patch.object(module.subprocess, 'check_output', side_effect=[
                     "package: name='tv.own.owntv' versionCode='350' versionName='105.1.0.5'",
                     'Signer #1 certificate SHA-256 digest: ' + 'a' * 64]):
                self.assertEqual(350, module.published_apk_code('owner/repository', 'v105.1.0.5', 'OwnTV.apk'))

    @patch.dict('os.environ', {'GITHUB_REPOSITORY': 'owner/repository', 'GH_TOKEN': 'test'})
    def test_only_absent_release_allows_no_previous_code(self):
        for status in (404, 403, 500):
            error = urllib.error.HTTPError('https://api.github.com/', status, 'test', {}, None)
            with patch.object(module.urllib.request, 'urlopen', side_effect=error):
                if status == 404: self.assertIsNone(module.previous_published_code())
                else:
                    with self.assertRaises(urllib.error.HTTPError): module.previous_published_code()

    def test_three_and_four_component_versions_use_the_explicit_code(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'version.properties'
            for name in ['105.1.0.6', '105.2.0']:
                path.write_text(f'versionName={name}\nversionCode=10510006\n')
                self.assertEqual(10510006, module.read_version(path, 'v' + name, 10510005)['code'])
                with self.assertRaises(ValueError): module.read_version(path, 'v5.2.1')
                with self.assertRaises(ValueError): module.read_version(path, previous_code=10510006)
    def test_invalid_version_names_and_codes_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'version.properties'
            for name, code in [('1.2', '1'), ('1.2.3-beta', '1'), ('1.2.3.4.5', '1'),
                               ('1.2.3', '-1'), ('1.2.3', '0'), ('1.2.3', '2100000001')]:
                path.write_text(f'versionName={name}\nversionCode={code}\n')
                with self.assertRaises(ValueError): module.read_version(path)
