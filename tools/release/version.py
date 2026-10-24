"""One version source for local builds and CI; never infer a version code by splitting a tag."""
import argparse
import json
import re
import os
import subprocess
import urllib.request
import urllib.error
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def published_apk_code(repository, tag, name):
    # Historical workflows could override the source's versionCode via environment variables.
    # Inspect the distributed signed bytes, rather than assuming the tagged source is authoritative.
    from verify_artifact import inspect_metadata
    sdk = Path(os.environ['ANDROID_HOME'])
    executable = '.exe' if os.name == 'nt' else ''
    signer = 'apksigner.bat' if os.name == 'nt' else 'apksigner'
    candidates = sorted(p for p in (sdk / 'build-tools').iterdir()
                        if (p / ('aapt' + executable)).is_file() and (p / signer).is_file())
    if not candidates: raise ValueError('Android build tools are required to inspect the previous distributed APK')
    tools = candidates[-1]
    with tempfile.TemporaryDirectory(prefix='owntv-release-check-') as folder:
        subprocess.run(['gh', 'release', 'download', tag, '--repo', repository, '--pattern', name, '--dir', folder],
                       check=True, capture_output=True, text=True)
        apk = Path(folder) / name
        if not apk.is_file() or apk.stat().st_size > 512 * 1024 * 1024:
            raise ValueError('Previous distributed APK is missing or too large')
        badging = subprocess.check_output([str(tools / ('aapt' + executable)), 'dump', 'badging', str(apk)], text=True)
        match = re.search(r"package: name='tv\.own\.owntv' versionCode='([0-9]+)'", badging)
        if not match: raise ValueError('Previous APK has an unexpected package or version')
        certificates = subprocess.check_output([str(tools / signer), 'verify', '--print-certs', str(apk)], text=True)
        code = int(match[1])
        inspect_metadata(badging, certificates, tag[1:], code, os.environ['RELEASE_CERT_SHA256'])
        return code

def previous_published_code():
    repository = os.environ['GITHUB_REPOSITORY']
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
        raise ValueError('Invalid repository identity')
    request = urllib.request.Request('https://api.github.com/repos/' + repository + '/releases/latest',
        headers={'Authorization': 'Bearer ' + os.environ['GH_TOKEN'], 'Accept': 'application/vnd.github+json'})
    try:
        with urllib.request.urlopen(request, timeout=30) as response: release = json.load(response)
    except urllib.error.HTTPError as error:
        error.close()
        if error.code == 404: return None
        raise
    tag = release['tag_name']
    if not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+(?:\.[0-9]+)?', tag):
        raise ValueError('Previous release requires an explicit version-code migration')
    names = [asset['name'] for asset in release.get('assets', [])]
    chosen = next((n for n in ('OwnTV.apk', 'OwnTV-' + tag + '.apk') if n in names), None)
    if chosen is None: raise ValueError('Cannot identify the previous distributed ARM APK for verification')
    size = next(a.get('size', 0) for a in release['assets'] if a['name'] == chosen)
    if not isinstance(size, int) or not 0 < size <= 512 * 1024 * 1024:
        raise ValueError('Previous release asset has an invalid size')
    return published_apk_code(repository, tag, chosen)

def read_version(path=ROOT / 'version.properties', tag=None, previous_code=None):
    fields = dict(line.strip().split('=', 1) for line in path.read_text(encoding='utf-8').splitlines()
                  if line.strip() and not line.lstrip().startswith('#'))
    name = fields['versionName']
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+(?:\.[0-9]+)?', name):
        raise ValueError('Invalid version name')
    if not re.fullmatch(r'[1-9][0-9]*', fields['versionCode']):
        raise ValueError('Invalid version code')
    code = int(fields['versionCode'])
    if code > 2_100_000_000:
        raise ValueError('Version code exceeds Android distribution limit')
    if tag is not None and tag != 'v' + name:
        raise ValueError('Release tag differs from version.properties')
    if previous_code is not None and code <= previous_code:
        raise ValueError('Version code must exceed the previous distributed release')
    return {'name': name, 'code': code}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--tag')
    parser.add_argument('--previous-code', type=int)
    parser.add_argument('--json', action='store_true')
    parser.add_argument('--github-release', action='store_true')
    args = parser.parse_args()
    previous = previous_published_code() if args.github_release else args.previous_code
    result = read_version(tag=args.tag, previous_code=previous)
    print(json.dumps(result) if args.json else f"name={result['name']}\ncode={result['code']}")
