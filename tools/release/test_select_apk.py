import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('select_apk', Path(__file__).with_name('select_apk.py'))
module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)

class SelectionTest(unittest.TestCase):
    def test_actual_abi_selection_refuses_ambiguity_and_path_escape(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for name, abis in [('arm.apk', ['arm64-v8a', 'armeabi-v7a']), ('x86.apk', ['x86_64'])]:
                with zipfile.ZipFile(root / name, 'w') as apk:
                    for abi in abis: apk.writestr('lib/' + abi + '/libtest.so', b'test')
            meta = root / 'output-metadata.json'
            meta.write_text(json.dumps({'elements': [{'outputFile': 'arm.apk'}, {'outputFile': 'x86.apk'}]}))
            self.assertEqual(root / 'x86.apk', module.select(root, ['x86_64']))
            with self.assertRaises(ValueError): module.select(root, ['arm64-v8a'])
            meta.write_text(json.dumps({'elements': [{'outputFile': 'x86.apk'}, {'outputFile': 'x86.apk'}]}))
            with self.assertRaises(ValueError): module.select(root, ['x86_64'])
            meta.write_text(json.dumps({'elements': [{'outputFile': '../x86.apk'}]}))
            with self.assertRaises(ValueError): module.select(root, ['x86_64'])
