#!/usr/bin/env python3
"""Create update archives and sign the active Windows release on the owner's machine."""
import argparse
import base64
import hashlib
import io
from pathlib import Path
import platform
import re
import shutil
import ssl
import subprocess
import tempfile
import zipfile
import desktop

ROOT = desktop.ROOT
OUTPUT = ROOT / 'dist' / 'updates'
KEY = ROOT / '.local' / 'update-signing' / 'private.pem'
PUBLIC = ROOT / 'DartGame_Client' / 'src' / 'updater' / 'update-public.der'
BASE = 'https://github.com/HandQ2212/Darts-Battle/releases/latest/download/'
# macOS client releases are paused. Signing can still run on the owner's Mac.
RELEASE_PLATFORMS = ('windows',)


def run(*args):
    subprocess.run(list(map(str, args)), check=True)


def archive():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    system = platform.system()
    if system == 'Darwin' and platform.machine() == 'arm64':
        app = ROOT / 'dist/darwin-arm64/DartsBattle.app'
        if not app.is_dir():
            raise RuntimeError('Build the macOS app-image first.')
        run('/usr/bin/codesign', '--verify', '--deep', '--strict', app)
        target = OUTPUT / 'DartsBattle-mac.zip'
        target.unlink(missing_ok=True)
        run('/usr/bin/ditto', '-c', '-k', '--sequesterRsrc', '--keepParent', app, target)
    elif system == 'Windows' and platform.machine().lower() in ('amd64', 'x86_64'):
        app = ROOT / 'dist/windows-AMD64/DartsBattle'
        if not app.is_dir():
            app = ROOT / 'dist/windows-amd64/DartsBattle'
        if not (app / 'DartsBattle.exe').is_file():
            raise RuntimeError('Build the Windows app-image first.')
        target = Path(shutil.make_archive(str(OUTPUT / 'DartsBattle-windows'), 'zip', app.parent, app.name))
    else:
        raise RuntimeError('Build each archive on its target platform.')
    print('Created', target)


def validate_archive(path, system):
    """Refuse to sign a stale, wrong-platform or differently configured app image."""
    root = 'DartsBattle.app/Contents' if system == 'mac' else 'DartsBattle'
    launcher = root + ('/MacOS/DartsBattle' if system == 'mac' else '/DartsBattle.exe')
    jar_name = root + '/app/darts-client.jar'
    runtime = root + ('/runtime/Contents/Home/release' if system == 'mac' else '/runtime/release')
    fx = 'mac-aarch64' if system == 'mac' else 'win'
    rtc = 'macos-aarch64' if system == 'mac' else 'windows-x86_64'
    required = [launcher, jar_name, runtime,
                root + f'/app/lib/javafx-graphics-{desktop.FX_VERSION}-{fx}.jar',
                root + f'/app/lib/webrtc-java-{desktop.WEBRTC_VERSION}-{rtc}.jar']
    try:
        with zipfile.ZipFile(path) as archive_file:
            names = archive_file.namelist()
            if any(names.count(name) != 1 for name in required):
                raise RuntimeError(f'{path.name}: missing or duplicated launcher, runtime or platform library')
            if archive_file.getinfo(runtime).file_size > 65536:
                raise RuntimeError(f'{path.name}: runtime metadata is too large')
            runtime_info = archive_file.read(runtime).decode('ascii')
            modules = next((line.partition('=')[2].strip('"').split() for line in runtime_info.splitlines() if line.startswith('MODULES=')), [])
            if not {'java.net.http', 'jdk.crypto.ec'}.issubset(modules):
                raise RuntimeError(f'{path.name}: runtime is missing updater modules')
            info = archive_file.getinfo(jar_name)
            if info.file_size > 100 * 1024 * 1024:
                raise RuntimeError(f'{path.name}: client JAR is too large')
            with zipfile.ZipFile(io.BytesIO(archive_file.read(info))) as jar:
                for member in ('updater/build.properties', 'updater/update-public.der', 'server.properties', 'server-cert.pem'):
                    if jar.namelist().count(member) != 1:
                        raise RuntimeError(f'{path.name}: missing or duplicated {member}')
                    if jar.getinfo(member).file_size > 65536:
                        raise RuntimeError(f'{path.name}: {member} is too large')
                if jar.read('updater/build.properties').decode('ascii').strip() != 'version=' + desktop.APP_VERSION:
                    raise RuntimeError(f'{path.name}: version does not match APP_VERSION')
                if jar.read('updater/update-public.der') != PUBLIC.read_bytes():
                    raise RuntimeError(f'{path.name}: update public key does not match')
                if ssl.PEM_cert_to_DER_cert(jar.read('server-cert.pem').decode('ascii')) != ssl.PEM_cert_to_DER_cert((ROOT / 'deployment/server-cert.pem').read_text()):
                    raise RuntimeError(f'{path.name}: server certificate does not match deployment/server-cert.pem')
                actual = dict(line.split('=', 1) for line in jar.read('server.properties').decode('utf-8').splitlines() if '=' in line)
                expected = dict(line.split('=', 1) for line in (ROOT / 'deployment/client.properties').read_text().splitlines() if '=' in line and not line.lstrip().startswith('#'))
                if any(actual.get(k) != v for k, v in expected.items()) or actual.get('server.tls') != 'true':
                    raise RuntimeError(f'{path.name}: server profile does not match deployment/client.properties')
    except (zipfile.BadZipFile, KeyError, UnicodeError, ValueError) as exc:
        raise RuntimeError(f'{path.name}: invalid archive: {exc}') from exc


def manifest():
    if not KEY.is_file():
        raise RuntimeError('Missing local signing key: ' + str(KEY))
    if not re.fullmatch(r'[0-9]{1,6}(\.[0-9]{1,6}){1,3}', desktop.APP_VERSION):
        raise RuntimeError('Invalid version')
    current_public = subprocess.check_output(['openssl', 'pkey', '-in', str(KEY), '-pubout', '-outform', 'DER'])
    if current_public != PUBLIC.read_bytes():
        raise RuntimeError('Signing key does not match the embedded public key.')
    paths = {name: OUTPUT / ('DartsBattle-' + name + '.zip') for name in RELEASE_PLATFORMS}
    missing = [path.name for path in paths.values() if not path.is_file()]
    if missing:
        raise RuntimeError('Missing release archive in dist/updates: ' + ', '.join(missing))
    for name, path in paths.items():
        validate_archive(path, name)
    values = ['version=' + desktop.APP_VERSION]
    for name, path in paths.items():
        sha = hashlib.sha256()
        with path.open('rb') as archive_file:
            for chunk in iter(lambda: archive_file.read(1024 * 1024), b''):
                sha.update(chunk)
        digest = sha.hexdigest()
        values.extend((name + '.url=' + BASE + path.name, name + '.sha256=' + digest))
    payload = ('\n'.join(values) + '\n').encode('ascii')
    with tempfile.TemporaryDirectory(prefix='darts-sign-') as temp:
        message = Path(temp) / 'message'
        signature = Path(temp) / 'signature'
        message.write_bytes(payload)
        run('openssl', 'pkeyutl', '-sign', '-rawin', '-inkey', KEY, '-in', message, '-out', signature)
        (OUTPUT / 'darts-update.txt').write_bytes(base64.b64encode(signature.read_bytes()) + b'\n' + payload)
    print('Signed', OUTPUT / 'darts-update.txt')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('archive', 'manifest'))
    args = parser.parse_args()
    try:
        archive() if args.command == 'archive' else manifest()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as exc:
        parser.exit(1, 'ERROR: ' + str(exc) + '\n')
