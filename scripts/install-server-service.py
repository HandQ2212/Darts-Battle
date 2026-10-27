#!/usr/bin/env python3
"""Install this Mac's TLS game server as a user launch agent. Does not configure DNS/router."""
import argparse
import os
from pathlib import Path
import platform
import plistlib
import secrets
import shlex
import shutil
import socket
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import desktop

ROOT = desktop.ROOT
PRIVATE = ROOT / '.local'
SUPPORT = Path.home() / 'Library/Application Support/DartsBattleServer'
PLIST = Path.home() / 'Library/LaunchAgents/com.dartsbattle.server.plist'
LABEL = 'com.dartsbattle.server'


def read_env(path):
    values = {}
    for line in path.read_text().splitlines():
        if line and not line.startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            values[key] = shlex.split(value)[0]
    return values


def private_write(path, text):
    fd = os.open(str(path), os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as file:
        file.write(text)
    path.chmod(0o600)


def prepare_identity():
    envfile = PRIVATE / 'server.env'
    if not envfile.exists():
        raise RuntimeError('Configure MySQL credentials in .local/server.env first; see docs/DESKTOP_MULTIPLAYER_SETUP.md.')
    values = read_env(envfile)
    store = PRIVATE / 'server.p12'
    public = ROOT / 'deployment/server-cert.pem'
    public.parent.mkdir(exist_ok=True)
    if store.exists() and 'DARTS_TLS_PASSWORD' not in values:
        raise RuntimeError('Keystore exists without its saved password; refusing to replace server identity.')
    if not store.exists():
        values['DARTS_TLS_PASSWORD'] = secrets.token_hex(24)
        # Save before invoking keytool so a failure never leaves a keystore with an unknown password.
        private_write(envfile, ''.join(key + '=' + shlex.quote(value) + '\n' for key, value in values.items()))
        environment = os.environ.copy()
        environment['DARTS_TLS_PASSWORD'] = values['DARTS_TLS_PASSWORD']
        previous = os.umask(0o077)
        try:
            desktop.run([desktop.tool('keytool'), '-genkeypair', '-alias', 'darts-server', '-keyalg', 'EC',
                         '-groupname', 'secp256r1', '-sigalg', 'SHA256withECDSA', '-storetype', 'PKCS12',
                         '-keystore', store, '-storepass:env', 'DARTS_TLS_PASSWORD', '-validity', '825',
                         '-dname', 'CN=Darts Battle Server', '-ext', 'KU=digitalSignature', '-ext', 'EKU=serverAuth'],
                        env=environment)
        finally:
            os.umask(previous)
    environment = os.environ.copy()
    environment['DARTS_TLS_PASSWORD'] = values['DARTS_TLS_PASSWORD']
    desktop.run([desktop.tool('keytool'), '-exportcert', '-rfc', '-alias', 'darts-server',
                 '-keystore', store, '-storepass:env', 'DARTS_TLS_PASSWORD', '-file', public], env=environment)
    store.chmod(0o600)
    values.update(DARTS_PORT='8443', DARTS_BIND='127.0.0.1', DARTS_WEBSOCKET_PORT='8444', DARTS_TLS_KEYSTORE=str(store))
    private_write(envfile, ''.join(key + '=' + shlex.quote(value) + '\n' for key, value in values.items()))
    return values


def install():
    if platform.system() != 'Darwin':
        raise RuntimeError('Run on the Mac that will host the game server.')
    values = prepare_identity()
    expected_start = str(SUPPORT / 'run-server.sh')
    if PLIST.exists():
        old = plistlib.loads(PLIST.read_bytes())
        if old.get('ProgramArguments') != ['/bin/sh', expected_start]:
            raise RuntimeError('An unrelated launch agent already uses this name; refusing to overwrite.')
    else:
        with socket.socket() as probe:
            if probe.connect_ex(('127.0.0.1', 8443)) == 0:
                raise RuntimeError('Port 8443 is already in use; stop that listener before installing this server.')
    # The host already has a JDK; keep a small, stable service installation independent of repo builds.
    package_input, _ = desktop.build('server')
    target = f'gui/{os.getuid()}/{LABEL}'
    if PLIST.exists():
        subprocess.run(['/bin/launchctl', 'bootout', target], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    SUPPORT.mkdir(mode=0o700, parents=True, exist_ok=True)
    SUPPORT.chmod(0o700)
    shutil.copy2(package_input / 'darts-server.jar', SUPPORT / 'darts-server.jar')
    (SUPPORT / 'lib').mkdir(exist_ok=True)
    for library in (package_input / 'lib').glob('*.jar'):
        shutil.copy2(library, SUPPORT / 'lib' / library.name)
    shutil.copy2(PRIVATE / 'server.p12', SUPPORT / 'server.p12')
    (SUPPORT / 'server.p12').chmod(0o600)
    values['DARTS_TLS_KEYSTORE'] = str(SUPPORT / 'server.p12')
    private_write(SUPPORT / 'server.env', ''.join(key + '=' + shlex.quote(value) + '\n' for key, value in values.items()))
    java_home = subprocess.check_output(['/usr/libexec/java_home'], text=True).strip()
    launcher = Path(java_home) / 'bin/java'
    private_write(SUPPORT / 'run-server.sh', '#!/bin/sh\nset -eu\nset -a\n. ' + shlex.quote(str(SUPPORT / 'server.env')) + '\nset +a\nexec ' + shlex.quote(str(launcher)) + ' -jar ' + shlex.quote(str(SUPPORT / 'darts-server.jar')) + '\n')
    log = SUPPORT / 'logs'
    log.mkdir(mode=0o700, exist_ok=True)
    plist = {
        'Label': LABEL, 'ProgramArguments': ['/bin/sh', expected_start],
        'RunAtLoad': True, 'KeepAlive': {'SuccessfulExit': False}, 'ThrottleInterval': 10,
        'WorkingDirectory': str(SUPPORT),
        'StandardOutPath': str(log / 'server.log'), 'StandardErrorPath': str(log / 'server-error.log'),
    }
    PLIST.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(str(PLIST), os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'wb') as file:
        plistlib.dump(plist, file)
    desktop.run(['/bin/launchctl', 'bootstrap', f'gui/{os.getuid()}', PLIST])
    print('Installed background server: TLS 8443 and WebSocket 8444 (loopback). Starts at login; restarts after failure.')
    print('Public certificate:', ROOT / 'deployment/server-cert.pem')
    print('Logs:', log)


if __name__ == '__main__':
    try:
        install()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        print('ERROR:', error, file=sys.stderr)
        sys.exit(1)
