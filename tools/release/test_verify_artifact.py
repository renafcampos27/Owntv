import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('verify_artifact', Path(__file__).with_name('verify_artifact.py'))
module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)

class ArtifactTest(unittest.TestCase):
    def test_only_the_expected_release_identity_is_accepted(self):
        badging = "package: name='tv.own.owntv' versionCode='10510006' versionName='105.1.0.6'"
        certs = 'Signer #1 certificate SHA-256 digest: ' + 'a' * 64
        self.assertEqual('a' * 64, module.inspect_metadata(badging, certs, '105.1.0.6', 10510006, 'a' * 64))
        for wrong_badging, wrong_cert, expected in [
            (badging + '\napplication-debuggable', certs, 'a' * 64),
            (badging.replace('10510006','10510005'), certs, 'a' * 64),
            (badging.replace('tv.own.owntv','another.app'), certs, 'a' * 64),
            (badging, certs, 'b' * 64), (badging, certs, ''),
            (badging, certs + '\nSigner #2 certificate SHA-256 digest: ' + 'a' * 64, 'a' * 64),
        ]:
            with self.assertRaises(ValueError):
                module.inspect_metadata(wrong_badging, wrong_cert, '105.1.0.6', 10510006, expected)
