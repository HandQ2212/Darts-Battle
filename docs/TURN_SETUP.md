# Cloudflare TURN setup for Darts Battle

Voice/video uses WebRTC between the two clients. The game server only forwards
signaling messages and asks Cloudflare for temporary ICE credentials. Keep the
TURN API token on the server computer; never put it in the client or a release
ZIP.

## Configure the server

1. In Cloudflare Calls, create a TURN key and an API token authorized to create
   credentials for that key.
2. Copy `deployment/turn.properties.example` to `.local/turn.properties`.
3. Fill in the values without quotes:

   ```properties
   turn.keyId=YOUR_TURN_KEY_ID
   turn.apiToken=YOUR_TURN_API_TOKEN
   ```

4. Restart the game server. The Windows `.local/run-internet.ps1` launcher
   automatically points the server at this file. For another server launcher,
   set `DARTS_TURN_CONFIG` to the file path, or set both
   `DARTS_TURN_KEY_ID` and `DARTS_TURN_API_TOKEN` in that server process's
   environment. Environment values override the properties file.

The server requests credentials with a one-hour lifetime only after both
players join a call. It refreshes them during long calls. If configuration is
missing or Cloudflare is unavailable, clients use Cloudflare's public STUN
server and show that relay fallback is not available. Some network pairs will
not connect over STUN alone.

## Security and releases

`.local/turn.properties` is ignored by Git. Do not paste its values into source,
client settings, issue reports, or logs. If the token is exposed, revoke it and
create a replacement in Cloudflare. The game server sends clients only the
short-lived username/password and TURN URLs returned for their call.

The game server must be reachable through the configured TLS/WSS domain. A
Cloudflare Tunnel routes game signaling; WebRTC media uses Cloudflare TURN when
direct peer-to-peer connectivity is blocked. Publishing a tunnel alone does
not enable the TURN relay.
