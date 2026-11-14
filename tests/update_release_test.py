#!/usr/bin/env python3
"""Exercise release archive validation without publishing an update."""
import importlib
import io
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
release = importlib.import_module('update-release')


class ArchiveValidationTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.old_root, self.old_public = release.ROOT, release.PUBLIC
        self.addCleanup(self.restore)
        (self.root / 'deployment').mkdir()
        shutil.copy2(self.old_root / 'deployment/server-cert.pem', self.root / 'deployment/server-cert.pem')
        shutil.copy2(self.old_root / 'deployment/client.properties', self.root / 'deployment/client.properties')
        self.public = self.root / 'public.der'
        self.public.write_bytes(self.old_public.read_bytes())
        release.ROOT, release.PUBLIC = self.root, self.public

    def restore(self):
        release.ROOT, release.PUBLIC = self.old_root, self.old_public

    def write_archive(self, system, version=None, key=None, host=None):
        jar_bytes = io.BytesIO()
        with zipfile.ZipFile(jar_bytes, 'w') as jar:
            jar.writestr('updater/build.properties', 'version=' + (version or release.desktop.APP_VERSION) + '\n')
            jar.writestr('updater/update-public.der', key if key is not None else self.public.read_bytes())
            profile = (self.root / 'deployment/client.properties').read_text()
            if host is not None:
                profile = profile.replace('game-backsite.handq2212.site', host)
            jar.writestr('server.properties', profile + 'server.tls=true\n')
            jar.writestr('server-cert.pem', (self.root / 'deployment/server-cert.pem').read_bytes())
        root = 'DartsBattle.app/Contents' if system == 'mac' else 'DartsBattle'
        launcher = root + ('/MacOS/DartsBattle' if system == 'mac' else '/DartsBattle.exe')
        archive_path = self.root / (system + '.zip')
        with zipfile.ZipFile(archive_path, 'w') as archive:
            archive.writestr(launcher, b'launcher')
            archive.writestr(root + '/app/darts-client.jar', jar_bytes.getvalue())
            archive.writestr(root + ('/runtime/Contents/Home/release' if system == 'mac' else '/runtime/release'), 'MODULES="java.base java.net.http jdk.crypto.ec"\n')
            fx = 'mac-aarch64' if system == 'mac' else 'win'
            rtc = 'macos-aarch64' if system == 'mac' else 'windows-x86_64'
            archive.writestr(root + f'/app/lib/javafx-graphics-{release.desktop.FX_VERSION}-{fx}.jar', b'fx')
            archive.writestr(root + f'/app/lib/webrtc-java-{release.desktop.WEBRTC_VERSION}-{rtc}.jar', b'rtc')
        return archive_path

    def test_valid_both_platforms(self):
        for system in ('mac', 'windows'):
            release.validate_archive(self.write_archive(system), system)

    def test_reject_wrong_version_key_host_and_platform(self):
        for options in ({'version': '1.0'}, {'key': b'wrong'}, {'host': 'wrong.example'}):
            with self.subTest(options=options), self.assertRaises(RuntimeError):
                release.validate_archive(self.write_archive('windows', **options), 'windows')
        with self.assertRaises(RuntimeError):
            release.validate_archive(self.write_archive('mac'), 'windows')


if __name__ == '__main__':
    unittest.main()
