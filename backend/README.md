# Jarvis Backend

A minimal proxy so the Android app never holds your real AI API key.

## Run locally
```
cd backend
npm install
cp .env.example .env   # fill in ANTHROPIC_API_KEY and JARVIS_DEVICE_TOKEN
npm start
```
Server runs on http://localhost:3000 by default. Test it:
```
curl -X POST http://localhost:3000/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Jarvis-Token: <same value as JARVIS_DEVICE_TOKEN>" \
  -d '{"messages":[{"role":"user","content":"Hello"}]}'
```

## Deploy
Any Node host works (Render, Railway, Fly.io, a small VPS). Set `ANTHROPIC_API_KEY` and
`JARVIS_DEVICE_TOKEN` as environment variables/secrets in that platform's dashboard —
do not put them in code or in git.

Once deployed, note the public HTTPS URL — you'll pass it to the Android build as
`JARVIS_BACKEND_URL`.

## Swapping AI providers
`server.js` calls the Anthropic Messages API. To use a different provider, replace the
`anthropic.messages.create(...)` call in `/api/chat` with that provider's SDK/call — the
Android app doesn't need to change, since it only ever talks to this proxy's `/api/chat`.
