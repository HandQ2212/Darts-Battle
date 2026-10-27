#!/usr/bin/env python3
"""Connect only the game's subdomain to this Mac; preserve existing website/tunnels."""
import argparse
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import desktop

ROOT=desktop.ROOT
SUPPORT=Path.home()/'Library/Application Support/DartsBattleServer'
PLIST=Path.home()/'Library/LaunchAgents/com.dartsbattle.cloudflared.plist'
CLOUDFLARED='/opt/homebrew/opt/cloudflared/bin/cloudflared'
LABEL='com.dartsbattle.cloudflared'


def run(args, **kwargs):
    return subprocess.run([CLOUDFLARED,*args],check=True,text=True,**kwargs)


def private_file(path,data):
    fd=os.open(str(path),os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
    with os.fdopen(fd,'w') as file:file.write(data)
    path.chmod(0o600)


def connect(hostname):
    if not re.fullmatch(r'[a-zA-Z0-9.-]+',hostname) or '.' not in hostname:
        raise RuntimeError('Use a hostname such as game.handq2212.site, without https:// or a path.')
    if not Path(CLOUDFLARED).exists():raise RuntimeError('Install cloudflared first: brew install cloudflared')
    if not (Path.home()/'.cloudflared/cert.pem').exists():
        raise RuntimeError('Finish Cloudflare authorization first: cloudflared tunnel login')
    if not (SUPPORT/'darts-server.jar').exists():raise RuntimeError('Install the server service first: python3 scripts/install-server-service.py')
    config=SUPPORT/'cloudflared.yml'
    arguments=[CLOUDFLARED,'--no-autoupdate','--config',str(config),'tunnel','run']
    if PLIST.exists() and plistlib.loads(PLIST.read_bytes()).get('ProgramArguments',[])[:6]!=arguments:
        raise RuntimeError('An unrelated launch agent uses this name; refusing to replace it.')
    metadata=ROOT/'.local/cloudflare-game.json'
    record=json.loads(metadata.read_text()) if metadata.exists() else {}
    tunnels=json.loads(run(['tunnel','list','--output','json'],capture_output=True).stdout)
    name='darts-battle-handq2212'
    if record:
        tunnel_id=record['id']
        if not any(t['id']==tunnel_id for t in tunnels):raise RuntimeError('Saved game tunnel was deleted remotely; inspect it before reconnecting.')
    else:
        matches=[t for t in tunnels if t.get('name')==name]
        if matches:
            tunnel_id=matches[0]['id']
            if not (Path.home()/'.cloudflared'/(tunnel_id+'.json')).exists():
                raise RuntimeError('An existing tunnel has this name but no local credentials; refusing to change someone else\'s tunnel.')
        else:
            run(['tunnel','create',name])
            tunnels=json.loads(run(['tunnel','list','--output','json'],capture_output=True).stdout)
            tunnel_id=next(t['id'] for t in tunnels if t.get('name')==name)
        private_file(metadata,json.dumps({'id':tunnel_id,'hostname':hostname})+'\n')
    credentials=Path.home()/'.cloudflared'/(tunnel_id+'.json')
    if not credentials.exists():raise RuntimeError('Missing scoped tunnel credentials: '+str(credentials))
    # No --overwrite-dns: an existing unrelated game DNS record is never replaced silently.
    run(['tunnel','route','dns',tunnel_id,hostname])
    SUPPORT.mkdir(mode=0o700,parents=True,exist_ok=True)
    scoped=SUPPORT/'cloudflare-game-credentials.json'
    shutil.copy2(credentials,scoped);scoped.chmod(0o600)
    configuration={'tunnel':tunnel_id,'credentials-file':str(scoped),
                   'ingress':[{'hostname':hostname,'service':'http://127.0.0.1:8444'}, {'service':'http_status:404'}]}
    private_file(config,json.dumps(configuration,indent=2)+'\n') # JSON is valid YAML, preserving quoted paths.
    arguments.append(tunnel_id)
    logs=SUPPORT/'logs';logs.mkdir(mode=0o700,exist_ok=True)
    agent={'Label':LABEL,'ProgramArguments':arguments,'RunAtLoad':True,'KeepAlive':True,'ThrottleInterval':10,
           'WorkingDirectory':str(SUPPORT),'StandardOutPath':str(logs/'cloudflared.log'),
           'StandardErrorPath':str(logs/'cloudflared-error.log')}
    target=f'gui/{os.getuid()}/{LABEL}'
    if PLIST.exists():subprocess.run(['/bin/launchctl','bootout',target],check=False,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    PLIST.parent.mkdir(parents=True,exist_ok=True)
    fd=os.open(str(PLIST),os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
    with os.fdopen(fd,'wb') as file:plistlib.dump(agent,file)
    subprocess.run(['/bin/launchctl','bootstrap',f'gui/{os.getuid()}',str(PLIST)],check=True)
    private_file(ROOT/'.local/client-profile.properties',f'server.host={hostname}\nserver.port=443\nserver.transport=websocket\n')
    print('Game endpoint:',f'wss://{hostname}/play')
    print('Tunnel starts at login. Existing websites/tunnels were not edited.')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--hostname',default='game.handq2212.site')
    args=parser.parse_args()
    try:connect(args.hostname)
    except (OSError,RuntimeError,subprocess.CalledProcessError,ValueError) as error:
        print('ERROR:',error,file=sys.stderr);sys.exit(1)
