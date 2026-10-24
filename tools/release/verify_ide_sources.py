"""Resolve reviewed IDE sources and Javadoc with Gradle strict verification (no APK)."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET


def attachment_requests(metadata, fixture):
    root = ET.parse(metadata).getroot()
    ns = {'v': root.tag.split('}')[0][1:]}
    components = {tuple(c.attrib[k] for k in ('group', 'name', 'version')): c
                  for c in root.findall('v:components/v:component', ns)}
    result = []
    for row in json.loads(fixture.read_text(encoding='utf-8')):
        coordinate = tuple(row[k] for k in ('group', 'name', 'version'))
        if coordinate not in components:
            continue  # Core has fewer build plugins than the App.
        if not all(re.fullmatch(r'[A-Za-z0-9_.+-]+', part) for part in coordinate):
            raise ValueError('Invalid Maven coordinate')
        artifact = row['artifact']
        prefix = f'{coordinate[1]}-{coordinate[2]}-'
        gradle_sources = coordinate[:2] == ('gradle', 'gradle') and artifact == prefix + 'src.zip'
        # AndroidX publishes samples under a logical name without '-android', while
        # module metadata maps that name to a differently named physical JAR.
        module_sources = (coordinate[0].startswith('androidx.') and coordinate[1].endswith('-android') and
                          artifact == f'{coordinate[1][:-8]}-{coordinate[2]}-samples-sources.jar')
        if not module_sources and (not artifact.startswith(prefix) or not (
                artifact.endswith(('-sources.jar', '-javadoc.jar')) or gradle_sources)):
            raise ValueError('Unsupported IDE attachment: ' + artifact)
        extension = 'zip' if gradle_sources else 'jar'
        classifier = 'sources' if module_sources else artifact[len(prefix):-4]
        if not re.fullmatch(r'[A-Za-z0-9_.+-]+', classifier):
            raise ValueError('Invalid source classifier')
        matches = [a for a in components[coordinate].findall('v:artifact', ns)
                   if a.attrib['name'] == artifact]
        if len(matches) != 1 or len(matches[0].findall('v:sha256', ns)) != 1:
            raise ValueError('Missing reviewed SHA-256: ' + artifact)
        dependency = ':'.join(coordinate) if module_sources else ':'.join((*coordinate, classifier)) + '@' + extension
        request = dict(dependency=dependency, artifact=artifact, module_sources=module_sources)
        if module_sources:
            docs_type = row.get('docsType', 'sources')
            if not re.fullmatch(r'[A-Za-z0-9_.-]+', docs_type):
                raise ValueError('Invalid documentation type')
            request['docs_type'] = docs_type
        result.append(request)
    if not result or len(result) != len({r['dependency'] for r in result}):
        raise ValueError('Empty or duplicate source fixture')
    return result


def source_dependencies(metadata, fixture):
    return [r['dependency'] for r in attachment_requests(metadata, fixture)]


def missing_cached_attachments(metadata, cache):
    """Read-only audit of every cached IDE filename for reviewed components, including aliases."""
    root = ET.parse(metadata).getroot()
    ns = {'v': root.tag.split('}')[0][1:]}
    missing = set()
    for component in root.findall('v:components/v:component', ns):
        group, name, version = (component.attrib[k] for k in ('group', 'name', 'version'))
        known = {a.attrib['name'] for a in component.findall('v:artifact', ns)
                 if a.findall('v:sha256', ns)}
        for path in (cache / group / name / version).glob('*/*.jar'):
            if path.name.endswith(('-sources.jar', '-javadoc.jar')) and path.name not in known:
                missing.add(':'.join((group, name, version, path.name)))
    return sorted(missing)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('metadata', type=Path)
    parser.add_argument('--wrapper', type=Path, required=True)
    parser.add_argument('--fixture', type=Path,
                        default=Path(__file__).with_name('ide-source-artifacts.json'))
    parser.add_argument('--audit-cache', action='store_true',
                        help='Also reject unreviewed cached sources, samples and Javadoc for these components')
    args = parser.parse_args()
    requests = attachment_requests(args.metadata, args.fixture)
    if args.audit_cache:
        missing = missing_cached_attachments(args.metadata, Path.home() / '.gradle/caches/modules-2/files-2.1')
        if missing:
            raise ValueError('Cached IDE attachments without reviewed hashes:\n' + '\n'.join(missing))
    with tempfile.TemporaryDirectory(prefix='owntv-ide-sources-') as directory:
        project = Path(directory)
        (project / 'gradle').mkdir()
        shutil.copyfile(args.metadata, project / 'gradle/verification-metadata.xml')
        (project / 'settings.gradle').write_text("rootProject.name = 'OwnTVIdeSourcesCheck'\n")
        # Resolve the exact source/documentation archives, without transitive binaries.
        # Each file still passes normal strict SHA-256 verification by Gradle.
        # Separate configurations preserve every requested version (a composite may use
        # different Kotlin/Guava variants, and the wrappers use different Gradle versions).
        declarations = []
        for i, request in enumerate(requests):
            attributes = '''attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType, '__DOCS_TYPE__'))
            }'''.replace('__DOCS_TYPE__', request['docs_type']) if request['module_sources'] else ''
            declarations.append(f"configurations.create('ideSources{i}') {{ transitive = false; {attributes} }}\n"
                                f"dependencies.add('ideSources{i}', '{request['dependency']}')")
        expected_names = ', '.join("'" + r['artifact'] + "'" for r in requests)
        (project / 'build.gradle').write_text('''import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
repositories {
    ivy {
        name = 'Gradle distributions'
        url = uri('https://services.gradle.org/distributions/')
        patternLayout { artifact '[artifact]-[revision]-[classifier].[ext]' }
        metadataSources { artifact() }
        content { includeModule('gradle', 'gradle') }
    }
    maven { url = uri('https://dl.google.com/dl/android/maven2/'); metadataSources { gradleMetadata(); artifact() } }
    // Keep explicit Maven classifiers independent of unrelated module variants (e.g. Guava JRE/Android).
    maven { url = uri('https://repo.maven.apache.org/maven2/'); metadataSources { artifact() } }
    maven { url = uri('https://plugins.gradle.org/m2/'); metadataSources { artifact() } }
}
''' + '\n'.join(declarations) + '''
tasks.register('verifyIdeSources') {
    doLast {
        def expectedNames = [''' + expected_names + ''']
        int count = 0
        configurations.findAll { it.name.startsWith('ideSources') }.each { configuration ->
            int index = configuration.name.substring('ideSources'.length()).toInteger()
            def files = configuration.resolve()
            if (!files.any { it.name == expectedNames[index] }) {
                throw new GradleException('Missing IDE attachment: ' + expectedNames[index])
            }
            count++
        }
        if (count != expectedNames.size()) throw new GradleException('Missing IDE requests')
        println("IDE source/documentation requests verified by Gradle: ${count}")
    }
}
''', encoding='utf-8')
        subprocess.run([str(args.wrapper.resolve()), '--project-dir', str(project),
                        '--console=plain', '--dependency-verification=strict',
                        'verifyIdeSources'], check=True)


if __name__ == '__main__':
    main()
