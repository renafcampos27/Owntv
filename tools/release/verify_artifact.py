"""Verify an actual distribution APK, without reading signing passwords or generating a new key."""
import argparse
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path

def inspect_metadata(badging, certificates, version_name, version_code, expected_certificate):
    package = re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging)
    if not package or package.groups() != ('tv.own.owntv', str(version_code), version_name):
        raise ValueError('APK package/version mismatch')
    if 'application-debuggable' in badging:
        raise ValueError('A debug APK cannot be distributed as a release')
    expected = expected_certificate.replace(':', '').strip().lower()
    if not re.fullmatch('[0-9a-f]{64}', expected):
        raise ValueError('An authorized release certificate SHA-256 is required')
    actual = re.findall(r'Signer #[0-9]+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)', certificates)
    if len(actual) != 1 or actual[0].lower() != expected:
        raise ValueError('APK certificate differs from the authorized installed signing identity')
    return expected

def verify(apk, build_tools, name, code, certificate, abis):
    import os
    suffix = '.bat' if os.name == 'nt' else ''
    badging = subprocess.check_output([str(build_tools / ('aapt' + ('.exe' if os.name == 'nt' else ''))), 'dump', 'badging', str(apk)], text=True)
    certs = subprocess.check_output([str(build_tools / ('apksigner' + suffix)), 'verify', '--print-certs', str(apk)], text=True)
    digest = inspect_metadata(badging, certs, name, code, certificate)
    with zipfile.ZipFile(apk) as archive:
        present = {p.split('/')[1] for p in archive.namelist() if p.startswith('lib/') and p.endswith('.so')}
    if present != set(abis):
        raise ValueError('APK native ABIs differ from the expected distribution variant')
    return {'file': apk.name, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'certificateSha256': digest,
            'versionName': name, 'versionCode': code, 'abis': sorted(present)}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('apk', type=Path)
    parser.add_argument('--build-tools', type=Path, required=True)
    parser.add_argument('--name', required=True)
    parser.add_argument('--code', type=int, required=True)
    parser.add_argument('--certificate-sha256', required=True)
    parser.add_argument('--abis', nargs='+', required=True)
    args = parser.parse_args()
    print(json.dumps(verify(args.apk, args.build_tools, args.name, args.code, args.certificate_sha256, args.abis)))
