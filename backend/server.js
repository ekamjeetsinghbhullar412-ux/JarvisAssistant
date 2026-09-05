// Jarvis backend proxy
// Purpose: the Android app calls THIS server, never the AI provider directly, so the real
// API key never ships inside the APK (where it could be extracted by anyone).
//
// Deploy this anywhere that can run Node (Render, Railway, Fly.io, a small VPS, etc.),
// keep ANTHROPIC_API_KEY and JARVIS_DEVICE_TOKEN as environment variables / secrets there
// (never commit them), then point the Android app's JARVIS_BACKEND_URL gradle property at
// this server's public URL.

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const Anthropic = require('@anthropic-ai/sdk');

const app = express();
app.use(express.json({ limit: '200kb' }));
app.use(cors()); // tighten to your app's origin/needs in production

// Basic shared-secret auth so random clients on the internet can't use your key/quota.
// Generate a long random string and put the SAME value in this server's env and in the
// Android app's local.properties (never hard-code it into source control).
const DEVICE_TOKEN = process.env.JARVIS_DEVICE_TOKEN;
app.use((req, res, next) => {
  const token = req.header('X-Jarvis-Token');
  if (!DEVICE_TOKEN || token !== DEVICE_TOKEN) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  next();
});

// Prevent abuse / runaway costs.
app.use(rateLimit({ windowMs: 60 * 1000, max: 30 }));

const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

app.post('/api/chat', async (req, res) => {
  try {
    const { messages } = req.body;
    if (!Array.isArray(messages) || messages.length === 0) {
      return res.status(400).json({ error: 'messages array is required' });
    }

    const systemMessage = messages.find((m) => m.role === 'system');
    const conversation = messages
      .filter((m) => m.role !== 'system')
      .map((m) => ({ role: m.role === 'assistant' ? 'assistant' : 'user', content: m.content }));

    const response = await anthropic.messages.create({
      model: 'claude-sonnet-4-6',
      max_tokens: 600,
      system: systemMessage ? systemMessage.content : undefined,
      messages: conversation
    });

    const reply = response.content
      .filter((block) => block.type === 'text')
      .map((block) => block.text)
      .join('\n');

    res.json({ reply: reply || "I don't have a response for that." });
  } catch (err) {
    console.error('AI backend error:', err);
    res.status(502).json({ error: 'AI provider request failed' });
  }
});

app.get('/health', (req, res) => res.json({ status: 'ok' }));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Jarvis backend listening on port ${PORT}`));
