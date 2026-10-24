"""Select the sole APK with the expected actual ABIs, using AGP's metadata rather than ls/head."""
import argparse
import json
from pathlib import Path
import zipfile

def select(folder, abis):
    metadata = json.loads((folder / 'output-metadata.json').read_text())
    matches = []
    for element in metadata['elements']:
        name = element['outputFile']
        if Path(name).name != name: raise ValueError('APK metadata escapes output directory')
        path = folder / name
        if not path.is_file(): continue
        with zipfile.ZipFile(path) as archive:
            present = {n.split('/')[1] for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')}
        if present == set(abis): matches.append(path)
    if len(matches) != 1: raise ValueError('Expected one APK with precisely the required native ABIs')
    return matches[0]

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('folder', type=Path); parser.add_argument('--abis', nargs='+', required=True)
    args = parser.parse_args(); print(select(args.folder, args.abis))
