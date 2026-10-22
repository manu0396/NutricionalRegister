#!/usr/bin/env bash
#
# Runs the AI gateway the Coach talks to, with the certificate the app's
# debug build trusts (ADR-0016, ADR-0020).
#
# What it does, in order:
#  1. stops any gateway it started before, so a stale process cannot answer;
#  2. issues a development certificate authority and a server certificate for
#     the addresses the Coach can be reached on: the emulator's host alias
#     10.0.2.2, localhost, and this machine's LAN address, so the Coach also
#     answers to a phone on the same network;
#  3. copies the authority into the app's debug resources, so the next build
#     of the app trusts it;
#  4. starts the gateway on port 8443 and checks that it answers.
#
# Then build the app and start it; there is nothing else to configure, because
# gradle.properties already carries the address:
#
#   ./gateway/start-dev.sh
#   ./gradlew :androidApp:installDebug
#
# On a phone, point the app at this machine by name instead of the emulator
# alias: pass the address the script prints.
set -euo pipefail

cd "$(dirname "$0")"

PORT="${NUTRILOG_GATEWAY_PORT:-8443}"
CERT_DIR="dev-certs"
APP_CA="../androidApp/src/debug/res/raw/gateway_ca.pem"
SERVER_LOG="/tmp/nutrilog-gateway-$PORT.log"

LAN_IPS=$(ipconfig getifaddr en0 2>/dev/null || true)
if [ -z "$LAN_IPS" ]; then
  LAN_IPS=$(hostname -I 2>/dev/null | awk '{print $1}' || true)
fi
LAN_IP=$(printf '%s' "$LAN_IPS" | awk '{print $1}')

# 1. Stop the gateway this script owns, and nothing else.
lsof -ti ":$PORT" 2>/dev/null | xargs kill -9 2>/dev/null || true
sleep 0.5

# 2. Certificates, once a day: a new authority is written over the old one and
#    the app picks it up on the next build.
NEEDS_CERT=1
if [ -f "$CERT_DIR/server.pem" ] && [ -f "$CERT_DIR/server.key" ] && [ -f "$CERT_DIR/ca.pem" ]; then
  if [ -z "$(find "$CERT_DIR/server.pem" -mtime +1 2>/dev/null)" ]; then
    NEEDS_CERT=0
  fi
fi

if [ "$NEEDS_CERT" = "1" ]; then
  mkdir -p "$CERT_DIR"
  echo "issuing a development certificate"
  openssl req -x509 -newkey rsa:2048 -nodes -keyout "$CERT_DIR/ca.key" \
    -out "$CERT_DIR/ca.pem" -days 30 -subj "/CN=NutriLog dev CA" >/dev/null 2>&1
  openssl req -newkey rsa:2048 -nodes -keyout "$CERT_DIR/server.key" \
    -out "$CERT_DIR/server.csr" -subj "/CN=10.0.2.2" >/dev/null 2>&1
  {
    echo "subjectAltName=IP:10.0.2.2,IP:127.0.0.1,DNS:localhost"
    if [ -n "$LAN_IP" ]; then
      echo "IP:$LAN_IP"
    fi
  } > "$CERT_DIR/ext.cnf"
  openssl x509 -req -in "$CERT_DIR/server.csr" -CA "$CERT_DIR/ca.pem" \
    -CAkey "$CERT_DIR/ca.key" -days 30 -extfile "$CERT_DIR/ext.cnf" \
    -out "$CERT_DIR/server.pem" >/dev/null 2>&1
  rm -f "$CERT_DIR/server.csr"
fi

# 3. The app's debug build trusts the authority this script issued.
mkdir -p "$(dirname "$APP_CA")"
cp "$CERT_DIR/ca.pem" "$APP_CA"

# 4. Start the gateway and prove it answers.
nohup env PORT="$PORT" \
  AI_GATEWAY_TLS_CERT="$CERT_DIR/server.pem" \
  AI_GATEWAY_TLS_KEY="$CERT_DIR/server.key" \
  node server.mjs > "$SERVER_LOG" 2>&1 &

for _ in 1 2 3 4 5 6 7 8 9 10; do
  if curl -s --cacert "$CERT_DIR/ca.pem" "https://localhost:$PORT/v1/health" | grep -q '"status":"ok"'; then
    echo "the gateway is up on https://localhost:$PORT (log: $SERVER_LOG)"
    echo ""
    echo "  build the app:  ./gradlew :androidApp:installDebug"
    if [ -n "$LAN_IP" ]; then
      echo "  on a phone:     ./gradlew :androidApp:installDebug \\"
      echo "                    -PNUTRILOG_AI_PROXY_URL=https://$LAN_IP:$PORT"
    fi
    exit 0
  fi
  sleep 1
done

echo "the gateway did not come up; the log says:" >&2
tail -20 "$SERVER_LOG" >&2
exit 1
