#!/usr/bin/env python3
"""Build, run, test and package using JDK 17+ and Python 3; no IDE/Maven required."""
import argparse
import hashlib
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import urllib.request
import urllib.error
import tempfile
import secrets

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / 'build' / 'desktop'
CACHE = ROOT / 'build' / 'dependencies'
FX_VERSION = '17.0.12'
H2_VERSION = '2.3.232'


def tool(name):
    java_home = os.environ.get('JAVA_HOME')
    candidate = Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else '')) if java_home else None
    result = str(candidate) if candidate and candidate.exists() else shutil.which(name)
    if not result:
        raise RuntimeError(f'Cannot find {name}; install JDK 17+ and set JAVA_HOME/PATH.')
    return result


def run(args, **kwargs):
    subprocess.run([str(a) for a in args], check=True, cwd=ROOT, **kwargs)


def download(group, artifact, version, classifier=''):
    filename = f'{artifact}-{version}' + (f'-{classifier}' if classifier else '') + '.jar'
    destination = CACHE / filename
    if destination.exists():
        return destination
    CACHE.mkdir(parents=True, exist_ok=True)
    url = f'https://repo.maven.apache.org/maven2/{group.replace(".", "/")}/{artifact}/{version}/{filename}'
    print('Downloading', filename, flush=True)
    with urllib.request.urlopen(url, timeout=30) as response:
        data = response.read()
    algorithm = 'sha256'
    try:
        response = urllib.request.urlopen(url + '.sha256', timeout=30)
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        # Some OpenJFX 17 artifacts publish only SHA-1. HTTPS authenticates the
        # repository; this digest additionally checks download integrity.
        algorithm = 'sha1'
        response = urllib.request.urlopen(url + '.sha1', timeout=30)
    with response:
        expected = response.read().decode().strip().split()[0]
    if hashlib.new(algorithm, data).hexdigest() != expected:
        raise RuntimeError(f'Checksum mismatch: {filename}')
    destination.write_bytes(data)
    return destination


def javafx():
    props = subprocess.run([tool('java'), '-XshowSettings:properties', '-version'], capture_output=True, text=True, check=True)
    arch = re.search(r'os.arch\s*=\s*(\S+)', props.stderr).group(1)
    arm = arch in ('aarch64', 'arm64')
    system = platform.system()
    if system == 'Windows' and arm:
        raise RuntimeError('JavaFX 17 packaging supports Windows x64 here; use an x64 JDK on Windows.')
    classifier = {'Darwin': 'mac', 'Windows': 'win', 'Linux': 'linux'}[system] + ('-aarch64' if arm else '')
    files = []
    for module in ('base', 'graphics', 'controls', 'fxml', 'media'):
        filename = f'javafx-{module}-{FX_VERSION}-{classifier}.jar'
        bundled = ROOT / 'DartGame_Client' / 'lib' / 'javafx' / filename
        files.append(bundled if bundled.exists() else download('org.openjfx', f'javafx-{module}', FX_VERSION, classifier))
    return files


def load_release_profile(host=None, port=None, certificate=None, transport=None):
    config = {}
    saved = ROOT / '.local' / 'client-profile.properties'
    if not saved.exists():
        saved = ROOT / 'deployment' / 'client.properties'
    if saved.exists():
        for line in saved.read_text().splitlines():
            if line.strip() and not line.lstrip().startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                config[key.strip()] = value.strip()
    host = host or config.get('server.host', '')
    transport = transport or config.get('server.transport', 'websocket')
    port = port or int(config.get('server.port', '443' if transport == 'websocket' else '8443'))
    certificate = Path(certificate) if certificate else ROOT / 'deployment' / 'server-cert.pem'
    if not host or any(char in host for char in '/:@ \r\n') or not 1 <= port <= 65535:
        raise RuntimeError('Set --server-host to the public domain/TCP tunnel hostname and --server-port to its port before packaging.')
    if not certificate.is_file():
        raise RuntimeError('Missing public server certificate: run scripts/install-server-service.py on the owner Mac first.')
    return {'host': host, 'port': port, 'certificate': certificate.resolve(), 'transport': transport}


def build(target, profile=None, client_libraries=None):
    directory = BUILD / target
    classes = directory / 'classes'
    package_input = directory / 'input'
    # Only replace this script's generated output, never IDE or source folders.
    for folder in (classes, package_input):
        if folder.exists():
            shutil.rmtree(folder)
        folder.mkdir(parents=True)
    sources = list((ROOT / 'DartGame_Common' / 'src').rglob('*.java'))
    source_root = ROOT / ('DartGame_Server' if target == 'server' else 'DartGame_Client') / 'src'
    sources += [p for p in source_root.rglob('*.java') if target != 'server' or 'view' not in p.relative_to(source_root).parts]
    libraries = ([ROOT / 'DartGame_Server/lib/mysql-connector-j-9.4.0.jar',
                  download('org.java-websocket', 'Java-WebSocket', '1.6.0'),
                  download('org.slf4j', 'slf4j-api', '2.0.13'),
                  download('org.slf4j', 'slf4j-nop', '2.0.13')] if target == 'server' else (client_libraries if client_libraries is not None else javafx()))
    # javac @argfile handles Windows command length limits and paths with spaces.
    arguments = ['--release', '17', '-encoding', 'UTF-8', '-cp', os.pathsep.join(map(str, libraries)), '-d', str(classes), *map(str, sources)]
    argfile = directory / 'javac.args'
    argfile.write_text('\n'.join('"' + value.replace('\\', '/').replace('"', '\\"') + '"' for value in arguments), encoding='utf-8')
    run([tool('javac'), '-J-Dfile.encoding=UTF-8', '@' + str(argfile)])
    if target == 'client':
        for resource in source_root.rglob('*'):
            if resource.is_file() and resource.suffix != '.java':
                destination = classes / resource.relative_to(source_root)
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(resource, destination)
    if target == 'client' and profile:
        (classes / 'server.properties').write_text('server.host=' + profile['host'] + '\nserver.port=' + str(profile['port']) + '\nserver.tls=true\nserver.transport=' + profile['transport'] + '\n', encoding='utf-8')
        shutil.copy2(profile['certificate'], classes / 'server-cert.pem')
    lib = package_input / 'lib'
    lib.mkdir()
    for library in libraries:
        shutil.copy2(library, lib / library.name)
    main = 'dartgame_server.DartGame_Server' if target == 'server' else 'btl_ltm_n3.Launcher'
    # Manifest lines must be folded at 72 bytes. Entries are ASCII and short filenames.
    classpath = 'Class-Path: ' + ' '.join('lib/' + p.name for p in libraries)
    folded = []
    while len(classpath) > 70:
        folded.append(classpath[:70])
        classpath = ' ' + classpath[70:]
    folded.append(classpath)
    manifest = directory / 'MANIFEST.MF'
    manifest.write_text('Manifest-Version: 1.0\nMain-Class: ' + main + '\n' + '\n'.join(folded) + '\n\n', encoding='utf-8')
    jar = package_input / f'darts-{target}.jar'
    run([tool('jar'), '--create', '--file', jar, '--manifest', manifest, '-C', classes, '.'])
    print('Built', jar, flush=True)
    return package_input, main


def package(target, package_type, profile=None):
    if target == 'client' and profile is None:
        profile = load_release_profile()
    folder, main = build(target, profile)
    output = ROOT / 'dist' / (platform.system().lower() + '-' + platform.machine())
    name = 'DartsBattle' if target == 'client' else 'DartsBattleServer'
    args = [tool('jpackage'), '--type', package_type, '--input', folder, '--dest', output,
            '--name', name, '--app-version', '2.1.0', '--vendor', 'Darts Battle',
            '--main-jar', f'darts-{target}.jar', '--main-class', main,
            '--java-options', '-Dfile.encoding=UTF-8']
    if target == 'client':
        args += ['--java-options', '--module-path=$APPDIR/lib', '--java-options', '--add-modules=javafx.controls,javafx.fxml,javafx.media']
    if platform.system() == 'Windows':
        if target == 'server':
            args += ['--win-console']
        if package_type in ('exe', 'msi'):
            args += ['--win-dir-chooser', '--win-menu', '--win-shortcut', '--win-per-user-install']
    if platform.system() == 'Darwin':
        # Some JDK distributions leave the vendor's launcher signature invalid
        # after jpackage changes it. Sign the finished bundle, then verify it
        # before publishing either the app or an installer containing it.
        # This local ad-hoc signature does not replace Developer ID notarization
        # required for normal distribution to other Macs.
        output.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix='.package-', dir=output) as temporary:
            staging = Path(temporary)
            args[args.index('--type') + 1] = 'app-image'
            args[args.index('--dest') + 1] = staging
            run(args)
            app = staging / (name + '.app')
            run(['/usr/bin/codesign', '--force', '--deep', '--sign', '-', app])
            run(['/usr/bin/codesign', '--verify', '--deep', '--strict', '--verbose=2', app])
            if package_type == 'app-image':
                artifacts = [app]
            else:
                installers = staging / 'installers'
                run([tool('jpackage'), '--type', package_type, '--app-image', app,
                     '--dest', installers, '--name', name, '--app-version', '2.1.0'])
                artifacts = list(installers.iterdir())
            for artifact in artifacts:
                destination = output / artifact.name
                if destination.is_dir():
                    shutil.rmtree(destination)
                elif destination.exists():
                    destination.unlink()
                shutil.move(str(artifact), destination)
    else:
        run(args)
    print('Packaged under', output)


def tests(h2_jar):
    build('server')
    h2 = Path(h2_jar).resolve() if h2_jar else download('com.h2database', 'h2', H2_VERSION)
    classes = BUILD / 'server' / 'classes'
    sources = sorted((ROOT / 'tests').glob('*IntegrationTest.java'))
    test_cp = str(classes) + os.pathsep + str(BUILD / 'server/input/lib/*')
    run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-cp', test_cp, '-d', classes, *sources])
    run([tool('java'), '-cp', os.pathsep.join(map(str, [classes, h2, BUILD / 'server/input/lib/*'])), 'MultiplayerIntegrationTest'])
    tls_tests(classes, h2)


def tls_tests(classes, h2):
    with tempfile.TemporaryDirectory(prefix='darts-tls-test-') as directory:
        directory = Path(directory)
        environment = os.environ.copy()
        environment['DARTS_TEST_TLS_PASSWORD'] = secrets.token_hex(24)
        for name in ('trusted', 'other'):
            run([tool('keytool'), '-genkeypair', '-alias', 'server', '-keyalg', 'EC', '-groupname', 'secp256r1',
                 '-storetype', 'PKCS12', '-keystore', directory / (name + '.p12'), '-storepass:env',
                 'DARTS_TEST_TLS_PASSWORD', '-validity', '7', '-dname', 'CN=' + name],
                env=environment, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        run([tool('keytool'), '-exportcert', '-rfc', '-alias', 'server', '-keystore', directory / 'trusted.p12',
             '-storepass:env', 'DARTS_TEST_TLS_PASSWORD', '-file', directory / 'trusted.pem'],
            env=environment, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        run([tool('java'), '-cp', str(classes) + os.pathsep + str(BUILD / 'server/input/lib/*'), 'TlsIntegrationTest', directory / 'trusted.p12', directory / 'trusted.pem', directory / 'other.p12'], env=environment)
        environment['DARTS_TEST_TLS_STORE'] = str(directory / 'trusted.p12')
        environment['DARTS_TEST_TLS_CERT'] = str(directory / 'trusted.pem')
        run([tool('java'), '-Ddarts.test.websocket=true', '-cp', os.pathsep.join(map(str, [classes, h2, BUILD / 'server/input/lib/*'])), 'MultiplayerIntegrationTest'], env=environment)




def ui_test():
    build('client')
    classes = BUILD / 'client' / 'classes'
    libs = BUILD / 'client' / 'input' / 'lib'
    cases = ('ClientUiSmokeTest', 'DartThrowEffectsTest')
    run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-cp', str(classes) + os.pathsep + str(libs / '*'), '-d', classes, *[ROOT / ('tests/' + case + '.java') for case in cases]])
    for case in cases:
        run([tool('java'), '-Djavafx.cachedir=' + str(BUILD / 'javafx-cache'), '--module-path', libs, '--add-modules', 'javafx.controls,javafx.fxml,javafx.media', '-cp', classes, case])


def probe(endpoint=None, certificate=None):
    profile = load_release_profile(certificate=certificate)
    endpoint = endpoint or ('wss://' + profile['host'] + ':' + str(profile['port']) + '/play')
    with tempfile.TemporaryDirectory(prefix='darts-probe-') as directory:
        sources = list((ROOT / 'DartGame_Common/src').rglob('*.java')) + [ROOT / 'tests/InternetConnectionProbe.java']
        run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-d', directory, *sources])
        run([tool('java'), '-cp', directory, 'InternetConnectionProbe', endpoint, profile['certificate']])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('build', 'package', 'run', 'test', 'ui-test', 'probe'))
    parser.add_argument('target', nargs='?', choices=('client', 'server', 'all'), default='all')
    parser.add_argument('--type', choices=('app-image', 'dmg', 'pkg', 'exe', 'msi', 'deb', 'rpm'), default='app-image')
    parser.add_argument('--endpoint', help='Probe URL, default configured public WSS endpoint')
    parser.add_argument('--transport', choices=('tcp', 'websocket'), help='Release transport (default WebSocket for Cloudflare Tunnel)')
    parser.add_argument('--server-host', help='Public hostname configured once by the server owner')
    parser.add_argument('--server-port', type=int, help='Public TCP port (default 8443)')
    parser.add_argument('--server-cert', help='Public certificate to pin in release clients')
    parser.add_argument('--h2-jar', help='Existing H2 2.3.232 JAR to run tests offline')
    args = parser.parse_args()
    if args.command == 'probe':
        probe(args.endpoint, args.server_cert)
    elif args.command == 'test':
        tests(args.h2_jar)
    elif args.command == 'ui-test':
        ui_test()
    else:
        for target in (('server', 'client') if args.target == 'all' else (args.target,)):
            if args.command == 'package':
                profile = load_release_profile(args.server_host, args.server_port, args.server_cert, args.transport) if target == 'client' else None
                package(target, args.type, profile)
            else:
                profile = load_release_profile(args.server_host, args.server_port, args.server_cert, args.transport) if args.command == 'run' and target == 'client' else None
                folder, _ = build(target, profile)
                if args.command == 'run':
                    if args.target == 'all':
                        parser.error('run requires server or client')
                    extra = ['--module-path', folder / 'lib', '--add-modules', 'javafx.controls,javafx.fxml,javafx.media'] if target == 'client' else []
                    run([tool('java'), *extra, '-jar', folder / f'darts-{target}.jar'])


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print('ERROR:', error, file=sys.stderr)
        sys.exit(1)
