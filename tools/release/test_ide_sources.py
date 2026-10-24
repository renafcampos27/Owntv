import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('verify_ide_sources', Path(__file__).with_name('verify_ide_sources.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class IdeSourcesTest(unittest.TestCase):
    def test_normal_and_gradle_variant_classifiers(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            metadata = directory / 'metadata.xml'
            fixture = directory / 'fixture.json'
            files = ['plugin-1.0-sources.jar', 'plugin-1.0-gradle96-sources.jar']
            metadata.write_text('<verification-metadata xmlns="urn:test"><components>'
                                '<component group="org.example" name="plugin" version="1.0">' +
                                ''.join(f'<artifact name="{f}"><sha256 value="' + 'a' * 64 + '"/></artifact>' for f in files) +
                                '</component></components></verification-metadata>')
            fixture.write_text(json.dumps([dict(group='org.example', name='plugin', version='1.0', artifact=f) for f in files]))
            self.assertEqual(['org.example:plugin:1.0:sources@jar', 'org.example:plugin:1.0:gradle96-sources@jar'],
                             module.source_dependencies(metadata, fixture))
            # A binary checksum must never silently authorize its unreviewed source JAR.
            metadata.write_text(metadata.read_text().replace('<sha256 value="' + 'a' * 64 + '"/>', '', 1))
            with self.assertRaisesRegex(ValueError, 'Missing reviewed SHA-256'):
                module.source_dependencies(metadata, fixture)

    def test_all_reported_sources_have_reviewed_checksums(self):
        app = Path(__file__).resolve().parents[2]
        dependencies = module.source_dependencies(app / 'gradle/verification-metadata.xml',
                                                  Path(__file__).with_name('ide-source-artifacts.json'))
        self.assertEqual(409, len(dependencies))
        self.assertIn('org.jetbrains.kotlin:kotlin-stdlib:2.4.0:sources@jar', dependencies)
        self.assertIn('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0:sources@jar', dependencies)
        self.assertIn('gradle:gradle:9.8.0:src@zip', dependencies)
        self.assertIn('gradle:gradle:9.7.1:src@zip', dependencies)

    def test_gradle_distribution_sources_need_their_own_checksum(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            metadata = directory / 'metadata.xml'
            fixture = directory / 'fixture.json'
            metadata.write_text('<verification-metadata xmlns="urn:test"><components>'
                                '<component group="gradle" name="gradle" version="9.8.0">'
                                '<artifact name="gradle-9.8.0-src.zip"><sha256 value="' + 'a' * 64 +
                                '"/></artifact></component></components></verification-metadata>')
            fixture.write_text(json.dumps([dict(group='gradle', name='gradle', version='9.8.0',
                                               artifact='gradle-9.8.0-src.zip')]))
            self.assertEqual(['gradle:gradle:9.8.0:src@zip'], module.source_dependencies(metadata, fixture))

    def test_javadoc_requires_its_own_checksum(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            metadata = directory / 'metadata.xml'
            fixture = directory / 'fixture.json'
            metadata.write_text('<verification-metadata xmlns="urn:test"><components>'
                                '<component group="org.example" name="library" version="1.0">'
                                '<artifact name="library-1.0.jar"><sha256 value="' + 'a' * 64 +
                                '"/></artifact></component></components></verification-metadata>')
            fixture.write_text(json.dumps([dict(group='org.example', name='library', version='1.0',
                                               artifact='library-1.0-javadoc.jar')]))
            with self.assertRaisesRegex(ValueError, 'Missing reviewed SHA-256'):
                module.source_dependencies(metadata, fixture)
            metadata.write_text(metadata.read_text().replace('</component>',
                                '<artifact name="library-1.0-javadoc.jar"><sha256 value="' + 'b' * 64 +
                                '"/></artifact></component>'))
            self.assertEqual(['org.example:library:1.0:javadoc@jar'], module.source_dependencies(metadata, fixture))

    def test_androidx_sample_alias_resolves_through_module_sources_variant(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            metadata = directory / 'metadata.xml'
            fixture = directory / 'fixture.json'
            artifact = 'library-1.0-samples-sources.jar'
            metadata.write_text('<verification-metadata xmlns="urn:test"><components>'
                                '<component group="androidx.example" name="library-android" version="1.0">'
                                f'<artifact name="{artifact}"><sha256 value="' + 'a' * 64 +
                                '"/></artifact></component></components></verification-metadata>')
            fixture.write_text(json.dumps([dict(group='androidx.example', name='library-android',
                                               version='1.0', artifact=artifact)]))
            self.assertEqual([dict(dependency='androidx.example:library-android:1.0',
                                   artifact=artifact, module_sources=True, docs_type='sources')],
                             module.attachment_requests(metadata, fixture))
            row = json.loads(fixture.read_text())[0]
            row['docsType'] = 'fake-sources'
            fixture.write_text(json.dumps([row]))
            self.assertEqual('fake-sources', module.attachment_requests(metadata, fixture)[0]['docs_type'])
            metadata.write_text(metadata.read_text().replace('<sha256 value="' + 'a' * 64 + '"/>', ''))
            with self.assertRaisesRegex(ValueError, 'Missing reviewed SHA-256'):
                module.source_dependencies(metadata, fixture)

    def test_cache_audit_detects_aliases_without_matching_the_module_filename_prefix(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            metadata = directory / 'metadata.xml'
            artifact = 'library-1.0-samples-sources.jar'
            cached = directory / 'cache/androidx.example/library-android/1.0/digest' / artifact
            cached.parent.mkdir(parents=True)
            cached.write_bytes(b'sample')
            metadata.write_text('<verification-metadata xmlns="urn:test"><components>'
                                '<component group="androidx.example" name="library-android" version="1.0"/>'
                                '</components></verification-metadata>')
            self.assertEqual(['androidx.example:library-android:1.0:' + artifact],
                             module.missing_cached_attachments(metadata, directory / 'cache'))

    def test_locked_core_covers_both_wrappers_embedded_kotlin_sources(self):
        app = Path(__file__).resolve().parents[2]
        lock = json.loads((app / 'core-source-lock.json').read_text())
        with tempfile.TemporaryDirectory() as directory, zipfile.ZipFile(app / lock['archive']) as archive:
            metadata = Path(directory) / 'metadata.xml'
            metadata.write_bytes(archive.read('gradle/verification-metadata.xml'))
            dependencies = module.source_dependencies(metadata, Path(__file__).with_name('ide-source-artifacts.json'))
            for version in ['2.4.0', '2.4.10']:
                for name in ['kotlin-stdlib', 'kotlin-reflect']:
                    self.assertIn(f'org.jetbrains.kotlin:{name}:{version}:sources@jar', dependencies)
            self.assertIn('gradle:gradle:9.8.0:src@zip', dependencies)
            for coordinate in ['com.google.zxing:core:3.5.4',
                               'com.github.albfernandez:juniversalchardet:2.5.0',
                               'com.google.guava:listenablefuture:1.0']:
                self.assertIn(coordinate + ':javadoc@jar', dependencies)
            for coordinate in ['androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.10.0',
                               'androidx.navigationevent:navigationevent-android:1.0.2']:
                self.assertIn(coordinate, dependencies)


if __name__ == '__main__':
    unittest.main()
