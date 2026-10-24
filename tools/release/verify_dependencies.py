"""Corroborate Gradle SHA-256 entries against the artifact's official HTTPS repository.

Bootstrap only: review and commit the resulting XML; normal builds use Gradle strict verification.
No wildcard trust, downloaded code execution, or automatic acceptance of a changed artifact.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import hashlib
import json
from pathlib import Path
import re
import urllib.error
import urllib.request
import urllib.parse
import xml.etree.ElementTree as ET

CENTRAL = 'https://repo.maven.apache.org/maven2/'
GOOGLE = 'https://dl.google.com/dl/android/maven2/'
PLUGINS = 'https://plugins.gradle.org/m2/'
NATIVE = 'https://ahxn00.github.io/OwnTV_Core/maven/'
GRADLE_DISTRIBUTIONS = 'https://services.gradle.org/distributions/'

def official_urls(group, name, version, artifact):
    if (group, name) == ('gradle', 'gradle'):
        if not re.fullmatch(r'[0-9]+(?:\.[0-9]+){1,2}', version) or artifact != f'gradle-{version}-src.zip':
            raise ValueError('Unsupported Gradle distribution artifact')
        return [GRADLE_DISTRIBUTIONS + artifact]
    path = '/'.join([group.replace('.', '/'), name, version, artifact])
    roots = [NATIVE] if (group, name) == ('tv.own.owntv', 'libmpv') else (
        [GOOGLE, CENTRAL, PLUGINS] if group.startswith(('androidx.', 'com.android.', 'com.google.'))
        else [CENTRAL, PLUGINS, GOOGLE])
    return [root + path for root in roots]

def corroborate(item):
    group, name, version, artifact, expected = item
    for url in official_urls(group, name, version, artifact):
        try:
            try:
                with urllib.request.urlopen(url + '.sha256', timeout=40) as response:
                    digest = response.read(256).decode('ascii').strip().split()[0].lower()
                if not re.fullmatch('[0-9a-f]{64}', digest): raise ValueError('Invalid repository checksum')
                proof = url + '.sha256'
            except urllib.error.HTTPError as error:
                error.close()
                if error.code not in (403, 404): raise
                digestor = hashlib.sha256()
                total = 0
                with urllib.request.urlopen(url, timeout=90) as response:
                    while chunk := response.read(1024 * 1024):
                        total += len(chunk)
                        if total > 512 * 1024 * 1024: raise ValueError('Unexpectedly large dependency')
                        digestor.update(chunk)
                digest, proof = digestor.hexdigest(), url
            if digest != expected: raise ValueError('Repository SHA-256 differs: ' + url)
            return {'coordinate': ':'.join([group, name, version]), 'artifact': artifact,
                    'sha256': expected, 'proof': proof}
        except urllib.error.HTTPError as error:
            error.close()
            if error.code not in (403, 404): raise
    # Gradle Module Metadata may expose a logical file name different from its Maven URL.
    for url in official_urls(group, name, version, name + '-' + version + '.module'):
        try:
            with urllib.request.urlopen(url, timeout=40) as response: metadata = json.load(response)
            files = [f for variant in metadata.get('variants', [])
                     for f in variant.get('files', []) if f.get('name') == artifact]
            hashes = {f['sha256'] for f in files if f.get('sha256')}
            if hashes:
                if hashes != {expected}: raise ValueError('Official module checksum differs: ' + url)
                return {'coordinate': ':'.join([group, name, version]), 'artifact': artifact,
                        'sha256': expected, 'proof': url}
            if files:
                # E.g. Guava's Android metadata maps its JRE variant to ../<jre-version>/.
                # Hash those actual bytes; absent metadata checksums are never treated as proof.
                resolved = urllib.parse.urljoin(url, files[0]['url'])
                if not resolved.startswith(url.rsplit('/', 2)[0] + '/'):
                    raise ValueError('Module artifact escapes its official component repository')
                digestor = hashlib.sha256(); total = 0
                with urllib.request.urlopen(resolved, timeout=90) as response:
                    while chunk := response.read(1024 * 1024):
                        total += len(chunk)
                        if total > 512 * 1024 * 1024: raise ValueError('Unexpectedly large dependency')
                        digestor.update(chunk)
                if digestor.hexdigest() != expected: raise ValueError('Resolved module artifact differs: ' + resolved)
                return {'coordinate': ':'.join([group, name, version]), 'artifact': artifact,
                        'sha256': expected, 'proof': url, 'resolvedArtifact': resolved}
        except urllib.error.HTTPError as error:
            error.close()
            if error.code not in (403, 404): raise
    raise ValueError('No official artifact found: ' + ':'.join(item[:4]))

def verify(metadata, evidence):
    tree = ET.parse(metadata)
    root = tree.getroot()
    ns = {'v': root.tag.split('}')[0][1:]}
    old = json.loads(evidence.read_text()) if evidence.exists() else []
    known = {(r['coordinate'], r['artifact'], r['sha256']): r for r in old}
    entries, nodes = [], []
    for component in root.findall('v:components/v:component', ns):
        for artifact in component.findall('v:artifact', ns):
            sums = artifact.findall('v:sha256', ns)
            if len(sums) != 1: raise ValueError('Exactly one reviewed SHA-256 is required')
            node = sums[0]
            item = (*[component.attrib[k] for k in ('group', 'name', 'version')], artifact.attrib['name'], node.attrib['value'])
            entries.append(item); nodes.append(node)
    results, todo = [], []
    for item in entries:
        cached = known.get((':'.join(item[:3]), item[3], item[4]))
        allowed = official_urls(*item[:4])
        if item[:2] != ('gradle', 'gradle'):
            allowed += official_urls(*item[:3], item[1] + '-' + item[2] + '.module')
        if cached and cached['proof'] in [u + suffix for u in allowed for suffix in ('', '.sha256')]: results.append(cached)
        else: todo.append(item)
    errors = []
    with ThreadPoolExecutor(max_workers=8) as pool:
        futures = {pool.submit(corroborate, item): item for item in todo}
        for future in as_completed(futures):
            try: results.append(future.result())
            except Exception as error: errors.append(str(error))
    # Keep successful evidence even if a repository is temporarily unavailable; never authorize failures.
    all_results = {(':'.join([r['coordinate'], r['artifact'], r['sha256']])): r for r in old + results}
    evidence.write_text(json.dumps(sorted(all_results.values(), key=lambda r: (r['coordinate'], r['artifact'])), indent=2) + '\n')
    if errors: raise ValueError('\n'.join(errors))
    print(f'{len(entries)} dependency artifacts corroborated against official repositories')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('metadata', type=Path)
    parser.add_argument('--evidence', type=Path, required=True)
    args = parser.parse_args(); verify(args.metadata, args.evidence)
