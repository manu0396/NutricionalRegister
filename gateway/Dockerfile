FROM node:25-alpine

WORKDIR /app

COPY server.mjs provider.mjs coach.mjs fallback.mjs body.mjs rate-limit.mjs ./

# The gateway is stateless: no volume, no session storage, so replicas can be
# added or removed behind a load balancer at any time.
ENV PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1:$PORT/v1/health | grep -q '"status":"ok"' || exit 1

# The provider key is passed at run time with -e ANTHROPIC_API_KEY=...
# and is never baked into an image, never committed and never logged.
USER node

CMD ["node", "server.mjs"]
