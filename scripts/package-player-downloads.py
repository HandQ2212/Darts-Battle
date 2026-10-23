#!/usr/bin/env python3
"""Collect Mac DMG and cross-build a portable Windows x64 client with its JRE."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile

import desktop


def fetch(url):
    # curl uses the platform TLS/network stack, which also works on owner Macs
    # whose system Python HTTP client is blocked by the download endpoint.
    curl = shutil.which('curl')
    if curl:
        return subprocess.run([curl, '--fail', '--location', '--silent', '--show-error',
                               '--max-time', '120', url], check=True, capture_output=True).stdout
    with urllib.request.urlopen(url, timeout=90) as response:
        return response.read()


def windows_runtime():
    cache = desktop.CACHE / 'windows-jre'
    cache.mkdir(parents=True, exist_ok=True)
    metadata = cache / 'release.json'
    if not metadata.exists():
        url = 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jre&os=windows'
        package = json.loads(fetch(url))[0]['binary']['package']
        metadata.write_text(json.dumps(package, indent=2) + '\n')
    package = json.loads(metadata.read_text())
    archive = cache / Path(package['name']).name
    if not archive.exists():
        print('Downloading Windows Java runtime:', archive.name, flush=True)
        data = fetch(package['link'])
        if hashlib.sha256(data).hexdigest() != package['checksum']:
            raise RuntimeError('Windows runtime checksum mismatch')
        archive.write_bytes(data)
    if hashlib.sha256(archive.read_bytes()).hexdigest() != package['checksum']:
        raise RuntimeError('Cached Windows runtime checksum mismatch')
    return archive, package


def main():
    output = desktop.ROOT / 'dist/Player-Downloads'
    mac = output / 'Mac'
    windows = output / 'Windows'
    for folder in (mac, windows):
        folder.mkdir(parents=True, exist_ok=True)
    dmg = desktop.ROOT / 'dist/darwin-arm64/DartsBattle-2.1.0.dmg'
    if not dmg.is_file():
        raise RuntimeError('Build the Mac DMG first: python3 scripts/desktop.py package client --type dmg')
    shutil.copy2(dmg, mac / 'DartsBattle-2.1.0-macOS-AppleSilicon.dmg')
    libraries = [desktop.download('org.openjfx', 'javafx-' + module, desktop.FX_VERSION, 'win')
                 for module in ('base', 'graphics', 'controls', 'fxml', 'media')]
    folder, _ = desktop.build('client', desktop.load_release_profile(), client_libraries=libraries)
    archive, runtime_metadata = windows_runtime()
    with tempfile.TemporaryDirectory(prefix='darts-windows-') as temporary:
        staging = Path(temporary)
        app = staging / 'DartsBattle'
        app.mkdir()
        shutil.copytree(folder, app / 'app')
        extracted = staging / 'jre'
        with zipfile.ZipFile(archive) as source:
            source.extractall(extracted)
        roots = [path for path in extracted.iterdir() if path.is_dir()]
        if len(roots) != 1 or not (roots[0] / 'bin/javaw.exe').is_file():
            raise RuntimeError('Unexpected Windows JRE layout')
        shutil.move(str(roots[0]), app / 'runtime')
        (app / 'DartsBattle.cmd').write_bytes(
            b'@echo off\r\nsetlocal\r\n'
            b'cd /d "%~dp0"\r\n'
            b'start "" "%~dp0runtime\\bin\\javaw.exe" '
            b'--module-path "%~dp0app\\lib" '
            b'--add-modules javafx.controls,javafx.fxml,javafx.media '
            b'-Dfile.encoding=UTF-8 -jar "%~dp0app\\darts-client.jar"\r\n')
        (app / 'HUONG-DAN.txt').write_text(
            'DARTS BATTLE 2.1 - WINDOWS x64\n\n'
            '1. Giai nen TOAN BO file ZIP (Extract All). Khong chay truc tiep trong ZIP.\n'
            '2. Mo thu muc DartsBattle, nhap dup DartsBattle.cmd de choi.\n'
            '3. Dang ky/dang nhap. Hai nguoi dung hai tai khoan rieng de moi nhau.\n\n'
            'Khong can cai Java/MySQL/Cloudflare. Giu nguyen thu muc app va runtime.\n'
            'Tu ket noi game.handq2212.site. May chu Mac phai bat va khong sleep.\n'
            'Ban portable chua duoc chay thu tren may Windows thuc te.\n', encoding='utf-8')
        (app / 'runtime-source.json').write_text(json.dumps(runtime_metadata, indent=2) + '\n')
        shutil.make_archive(str(windows / 'DartsBattle-2.1.0-Windows-x64'), 'zip', staging, 'DartsBattle')
    (mac / 'HUONG-DAN.txt').write_text(
        'DARTS BATTLE 2.1 - macOS APPLE SILICON\n\n'
        'Danh cho Mac chip Apple Silicon. Khong dung cho Mac Intel.\n'
        'Mo DMG, keo DartsBattle vao Applications, mo app va dang ky/dang nhap.\n'
        'Tu ket noi game.handq2212.site, khong can cai Java/MySQL/Cloudflare.\n'
        'Ban thu nghiem chua Developer ID/notarization; macOS co the chan ban tai ve.\n', encoding='utf-8')
    (output / 'DOC-TRUOC.txt').write_text(
        'CAC FILE GUI CHO NGUOI CHOI\n\n'
        'Windows/: Gui file ZIP cho Windows x64. Giai nen het, mo DartsBattle.cmd.\n'
        'Mac/: Gui file DMG cho Mac Apple Silicon. Chua co ban Mac Intel.\n\n'
        'Chi gui bo cai client trong cac thu muc nay, khong gui source/server/.local.\n'
        'May chu Mac phai bat, khong sleep va dang dang nhap macOS.\n', encoding='utf-8')
    print('Player downloads:', output)


if __name__ == '__main__':
    main()
