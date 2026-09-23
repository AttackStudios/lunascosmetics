// Luna's Cosmetics relay: lets players with the mod see each other's cosmetics on
// servers that don't run the mod. One room per Minecraft server address.
//
//   npm install && PORT=8091 node server.js
//
// Then in the game: Luna's Cosmetics > Settings > Sync relay = ws://<host>:8091
const { WebSocketServer } = require('ws');

const PORT = parseInt(process.env.PORT || '8091', 10);
const MAX_MODEL = 2_000_000;
const rooms = new Map(); // room -> { sockets:Set, loadouts:Map<uuid,json>, models:Map<hash,[chunks]> }

function room(name) {
  if (!rooms.has(name)) rooms.set(name, { sockets: new Set(), loadouts: new Map(), models: new Map() });
  return rooms.get(name);
}

function send(ws, obj) {
  if (ws.readyState === 1) ws.send(JSON.stringify(obj));
}

function refs(json) {
  return [...String(json).matchAll(/custom:([0-9a-f]{40})/g)].map(m => m[1]);
}

function sendModels(ws, r, hashes) {
  for (const h of hashes) {
    const chunks = r.models.get(h);
    if (chunks && chunks.every(Boolean)) chunks.forEach((data, index) =>
      send(ws, { type: 'chunk', hash: h, index, total: chunks.length, data }));
  }
}

function prune(r) {
  const live = new Set();
  for (const j of r.loadouts.values()) refs(j).forEach(h => live.add(h));
  for (const h of r.models.keys()) if (!live.has(h)) r.models.delete(h);
}

const wss = new WebSocketServer({ port: PORT, maxPayload: 256 * 1024 });
wss.on('connection', ws => {
  ws.room = null;
  ws.uuid = null;
  ws.on('message', raw => {
    let m;
    try { m = JSON.parse(raw); } catch { return; }
    if (m.type === 'hello' && typeof m.room === 'string' && m.room.length < 200) {
      ws.room = m.room;
      const r = room(ws.room);
      r.sockets.add(ws);
      send(ws, { type: 'snapshot', loadouts: Object.fromEntries(r.loadouts) });
      sendModels(ws, r, new Set([...r.loadouts.values()].flatMap(refs)));
      return;
    }
    if (!ws.room) return;
    const r = room(ws.room);
    if (m.type === 'loadout' && typeof m.json === 'string' && m.json.length < 4000 && /^[0-9a-f-]{36}$/.test(m.uuid)) {
      ws.uuid = m.uuid;
      r.loadouts.set(m.uuid, m.json);
      for (const o of r.sockets) if (o !== ws) {
        send(o, { type: 'loadout', uuid: m.uuid, json: m.json });
        sendModels(o, r, refs(m.json));
      }
      prune(r);
    } else if (m.type === 'chunk' && /^[0-9a-f]{40}$/.test(m.hash)) {
      const total = m.total | 0, index = m.index | 0;
      if (total <= 0 || total * 28000 > MAX_MODEL || index < 0 || index >= total) return;
      if (!r.models.has(m.hash)) r.models.set(m.hash, new Array(total).fill(null));
      const chunks = r.models.get(m.hash);
      if (chunks.length !== total || chunks[index]) return;
      chunks[index] = m.data;
      if (chunks.every(Boolean)) {
        for (const o of r.sockets) if (o !== ws) sendModels(o, r, [m.hash]);
      }
    }
  });
  ws.on('close', () => {
    if (!ws.room) return;
    const r = room(ws.room);
    r.sockets.delete(ws);
    if (ws.uuid) {
      r.loadouts.delete(ws.uuid);
      for (const o of r.sockets) send(o, { type: 'leave', uuid: ws.uuid });
      prune(r);
    }
    if (r.sockets.size === 0) rooms.delete(ws.room);
  });
});
console.log(`Luna's Cosmetics relay on :${PORT}`);
