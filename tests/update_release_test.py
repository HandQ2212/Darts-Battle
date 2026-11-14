#!/usr/bin/env python3
"""Exercise release archive validation without publishing an update."""
import importlib
import base64
import hashlib
import io
from pathlib import Path
import shutil
import subprocess
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
        self.old_root, self.old_public, self.old_output, self.old_key = release.ROOT, release.PUBLIC, release.OUTPUT, release.KEY
        self.addCleanup(self.restore)
        (self.root / 'deployment').mkdir()
        shutil.copy2(self.old_root / 'deployment/server-cert.pem', self.root / 'deployment/server-cert.pem')
        shutil.copy2(self.old_root / 'deployment/client.properties', self.root / 'deployment/client.properties')
        self.public = self.root / 'public.der'
        self.public.write_bytes(self.old_public.read_bytes())
        release.ROOT, release.PUBLIC = self.root, self.public
        release.OUTPUT = self.root / 'dist/updates'
        release.OUTPUT.mkdir(parents=True)
        release.KEY = self.root / 'signing-private.pem'

    def restore(self):
        release.ROOT, release.PUBLIC = self.old_root, self.old_public
        release.OUTPUT, release.KEY = self.old_output, self.old_key

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
        archive_path = release.OUTPUT / ('DartsBattle-' + system + '.zip')
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

    def prepare_signing_key(self):
        if not shutil.which('openssl'):
            self.skipTest('OpenSSL is required for the signing integration checks')
        subprocess.run(['openssl', 'genpkey', '-algorithm', 'Ed25519', '-out', str(release.KEY)],
                       check=True, capture_output=True)
        self.public.write_bytes(subprocess.check_output(
            ['openssl', 'pkey', '-in', str(release.KEY), '-pubout', '-outform', 'DER']))

    def assert_windows_manifest(self, archive_path):
        signed = (release.OUTPUT / 'darts-update.txt').read_bytes()
        prefix, payload = signed.split(b'\n', 1)
        expected = ('version=' + release.desktop.APP_VERSION + '\n'
                    'windows.url=' + release.BASE + 'DartsBattle-windows.zip\n'
                    'windows.sha256=' + hashlib.sha256(archive_path.read_bytes()).hexdigest() + '\n')
        self.assertEqual(payload.decode('ascii'), expected)
        message = self.root / 'message'
        signature = self.root / 'signature'
        message.write_bytes(payload)
        signature.write_bytes(base64.b64decode(prefix, validate=True))
        subprocess.run(['openssl', 'pkeyutl', '-verify', '-pubin', '-inkey', str(self.public),
                        '-keyform', 'DER', '-rawin', '-in', str(message), '-sigfile', str(signature)],
                       check=True, capture_output=True)

    def test_sign_windows_manifest_without_mac_archive(self):
        self.prepare_signing_key()
        archive_path = self.write_archive('windows')
        release.manifest()
        self.assert_windows_manifest(archive_path)

    def test_sign_windows_manifest_ignores_stale_mac_archive(self):
        self.prepare_signing_key()
        archive_path = self.write_archive('windows')
        (release.OUTPUT / 'DartsBattle-mac.zip').write_bytes(b'stale, invalid Mac archive')
        release.manifest()
        self.assert_windows_manifest(archive_path)

    def test_manifest_requires_windows_archive(self):
        self.prepare_signing_key()
        self.write_archive('mac')
        with self.assertRaisesRegex(RuntimeError, 'DartsBattle-windows.zip'):
            release.manifest()
        self.assertFalse((release.OUTPUT / 'darts-update.txt').exists())

    def test_manifest_rejects_stale_windows_archive(self):
        self.prepare_signing_key()
        self.write_archive('windows', version='1.0')
        with self.assertRaisesRegex(RuntimeError, 'version does not match'):
            release.manifest()
        self.assertFalse((release.OUTPUT / 'darts-update.txt').exists())

    def test_manifest_rejects_different_signing_key(self):
        self.prepare_signing_key()
        self.write_archive('windows')
        subprocess.run(['openssl', 'genpkey', '-algorithm', 'Ed25519', '-out', str(release.KEY)],
                       check=True, capture_output=True)
        with self.assertRaisesRegex(RuntimeError, 'Signing key does not match'):
            release.manifest()
        self.assertFalse((release.OUTPUT / 'darts-update.txt').exists())


if __name__ == '__main__':
    unittest.main()
