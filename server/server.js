const net  = require('net');
const http = require('http');

const PORT = process.env.PORT || 36509;

// rooms: { code -> { creator, joiner, board, currentTurn, createdAt, status } }
const rooms = new Map();
let totalGamesPlayed = 0;

// Quick match queue: [{ socket, username }]
const matchQueue = [];

const WIN_COMBOS = [
  [0,1,2],[3,4,5],[6,7,8],
  [0,3,6],[1,4,7],[2,5,8],
  [0,4,8],[2,4,6]
];

function generateCode() {
  return Math.floor(1000 + Math.random() * 9000).toString();
}

function send(socket, message) {
  try { socket.write(message + '\n'); } catch (e) {}
}

function checkWin(board, symbol) {
  return WIN_COMBOS.some(([a, b, c]) =>
    board[a] === symbol && board[b] === symbol && board[c] === symbol
  );
}

function isFull(board) {
  return board.every(cell => cell !== null);
}

function getRoomBySocket(socket) {
  for (const [code, room] of rooms) {
    if (room.creator?.socket === socket || room.joiner?.socket === socket) {
      return { code, room };
    }
  }
  return null;
}

function handleMessage(socket, line) {
  const parts = line.split('|');
  const cmd = parts[0];

  if (cmd === 'CREATE_ROOM') {
    const username = parts[1] || 'Player';
    let code;
    do { code = generateCode(); } while (rooms.has(code));

    rooms.set(code, {
      creator:    { socket, username, symbol: 'X' },
      joiner:     null,
      board:      Array(9).fill(null),
      currentTurn:'X',
      createdAt:  Date.now(),
      status:     'waiting'
    });

    send(socket, `ROOM_CREATED|${code}|X`);
    console.log(`Room ${code} created by ${username}`);

  } else if (cmd === 'JOIN_ROOM') {
    const code     = parts[1];
    const username = parts[2] || 'Player';
    const room     = rooms.get(code);

    if (!room)        { send(socket, 'ERROR|Room not found'); return; }
    if (room.joiner)  { send(socket, 'ERROR|Room is full');   return; }

    room.joiner = { socket, username, symbol: 'O' };
    room.status = 'playing';

    send(socket, `JOIN_SUCCESS|O`);
    send(socket, `START_GAME|${room.creator.username}`);
    send(room.creator.socket, `START_GAME|${username}`);
    send(room.creator.socket, 'YOUR_TURN');

    console.log(`Room ${code}: ${room.creator.username}(X) vs ${username}(O) — game started`);

  } else if (cmd === 'MOVE') {
    const index = parseInt(parts[1]);
    if (isNaN(index) || index < 0 || index > 8) return;

    const result = getRoomBySocket(socket);
    if (!result) return;
    const { code, room } = result;

    const isCreator = room.creator?.socket === socket;
    const mover     = isCreator ? room.creator : room.joiner;
    const opponent  = isCreator ? room.joiner  : room.creator;

    if (!opponent) return;
    if (room.board[index] !== null) return;
    if (room.currentTurn !== mover.symbol) return;

    room.board[index] = mover.symbol;
    send(opponent.socket, `OPPONENT_MOVED|${index}`);

    if (checkWin(room.board, mover.symbol)) {
      send(mover.socket,    'WIN');
      send(opponent.socket, 'LOSE');
      console.log(`Room ${code}: ${mover.username} wins`);
      totalGamesPlayed++;
      room.status = 'ended';
      room.rematchCreator = false;
      room.rematchJoiner  = false;
      setTimeout(() => { if (rooms.get(code) === room) rooms.delete(code); }, 120000);
      return;
    }

    if (isFull(room.board)) {
      send(mover.socket,    'DRAW');
      send(opponent.socket, 'DRAW');
      console.log(`Room ${code}: draw`);
      totalGamesPlayed++;
      room.status = 'ended';
      room.rematchCreator = false;
      room.rematchJoiner  = false;
      setTimeout(() => { if (rooms.get(code) === room) rooms.delete(code); }, 120000);
      return;
    }

    room.currentTurn = opponent.symbol;
    send(opponent.socket, 'YOUR_TURN');

  } else if (cmd === 'QUICK_MATCH') {
    const username = parts[1] || 'Player';
    // Remove stale queue entries (disconnected sockets)
    for (let i = matchQueue.length - 1; i >= 0; i--) {
      if (matchQueue[i].socket.destroyed) matchQueue.splice(i, 1);
    }
    if (matchQueue.length > 0) {
      const waiter = matchQueue.shift();
      let code;
      do { code = generateCode(); } while (rooms.has(code));
      rooms.set(code, {
        creator:    { socket: waiter.socket, username: waiter.username, symbol: 'X' },
        joiner:     { socket, username, symbol: 'O' },
        board:      Array(9).fill(null),
        currentTurn:'X',
        createdAt:  Date.now(),
        status:     'playing',
        rematchCreator: false,
        rematchJoiner:  false,
      });
      send(waiter.socket, `MATCH_FOUND|${code}|X|${username}`);
      send(socket,         `MATCH_FOUND|${code}|O|${waiter.username}`);
      send(waiter.socket,  `START_GAME|${username}`);
      send(socket,         `START_GAME|${waiter.username}`);
      send(waiter.socket,  'YOUR_TURN');
      console.log(`Quick match: ${waiter.username}(X) vs ${username}(O) — room ${code}`);
    } else {
      matchQueue.push({ socket, username });
      send(socket, 'QUEUE_WAITING');
      console.log(`Quick match queue: ${username} waiting`);
    }

  } else if (cmd === 'CANCEL_MATCH') {
    const idx = matchQueue.findIndex(p => p.socket === socket);
    if (idx !== -1) matchQueue.splice(idx, 1);
    send(socket, 'MATCH_CANCELLED');

  } else if (cmd === 'REMATCH_REQUEST') {
    const code = parts[1];
    const room = rooms.get(code);
    if (!room || room.status !== 'ended') { send(socket, 'ERROR|Rematch not available'); return; }

    const isCreator = room.creator?.socket === socket;
    const isJoiner  = room.joiner?.socket  === socket;
    if (!isCreator && !isJoiner) { send(socket, 'ERROR|Not in this room'); return; }

    if (isCreator) room.rematchCreator = true;
    else           room.rematchJoiner  = true;

    const other = isCreator ? room.joiner : room.creator;
    if (other?.socket) send(other.socket, 'REMATCH_REQUESTED');

    if (room.rematchCreator && room.rematchJoiner) {
      room.board       = Array(9).fill(null);
      room.currentTurn = 'X';
      room.status      = 'playing';
      room.rematchCreator = false;
      room.rematchJoiner  = false;
      send(room.creator.socket, `START_GAME|${room.joiner.username}`);
      send(room.joiner.socket,  `START_GAME|${room.creator.username}`);
      send(room.creator.socket, 'YOUR_TURN');
      console.log(`Room ${code}: rematch started`);
    }

  } else if (cmd === 'REJOIN_ROOM') {
    const code     = parts[1];
    const username = parts[2];
    const room     = rooms.get(code);
    if (!room) { send(socket, 'ERROR|Room expired'); return; }

    const isCreator = room.creator?.username === username;
    const isJoiner  = room.joiner?.username  === username;
    if (!isCreator && !isJoiner) { send(socket, 'ERROR|Not a member of this room'); return; }

    if (isCreator) room.creator = { ...room.creator, socket };
    else           room.joiner  = { ...room.joiner,  socket };

    const symbol  = isCreator ? 'X' : 'O';
    const oppName = isCreator ? (room.joiner?.username || 'Opponent')
                              : (room.creator?.username || 'Opponent');
    send(socket, `REJOIN_SUCCESS|${symbol}|${oppName}|${room.currentTurn}`);

    const other = isCreator ? room.joiner : room.creator;
    if (other?.socket && !other.socket.destroyed) send(other.socket, 'OPPONENT_RECONNECTED');
    if (room.status === 'waiting_rejoin') room.status = 'playing';
    console.log(`Room ${code}: ${username} rejoined`);
  }
}

// ── HTTP admin API ─────────────────────────────────────────────────────────────
const httpServer = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, DELETE, OPTIONS');
  res.setHeader('Content-Type', 'application/json');

  if (req.method === 'OPTIONS') {
    res.writeHead(204); res.end(); return;
  }

  // GET / or /health — for UptimeRobot / keep-alive pings
  if (req.method === 'GET' && (req.url === '/' || req.url === '/health')) {
    res.writeHead(200);
    res.end(JSON.stringify({
      status:      'ok',
      rooms:       rooms.size,
      queue:       matchQueue.length,
      uptime:      Math.floor(process.uptime()),
      timestamp:   new Date().toISOString(),
    }));
    return;
  }

  // GET /admin/rooms
  if (req.method === 'GET' && req.url === '/admin/rooms') {
    const list = [...rooms.entries()].map(([code, r]) => ({
      code,
      creator:     r.creator?.username  || null,
      joiner:      r.joiner?.username   || null,
      status:      r.status             || (r.joiner ? 'playing' : 'waiting'),
      currentTurn: r.currentTurn,
      board:       r.board,
      createdAt:   r.createdAt,
    }));
    res.writeHead(200);
    res.end(JSON.stringify({ rooms: list, total: list.length, totalGamesPlayed }));
    return;
  }

  // DELETE /admin/rooms/:code
  if (req.method === 'DELETE' && req.url.startsWith('/admin/rooms/')) {
    const code = req.url.split('/').pop();
    const room = rooms.get(code);
    if (room) {
      if (room.creator?.socket) send(room.creator.socket, 'ERROR|Room closed by admin');
      if (room.joiner?.socket)  send(room.joiner.socket,  'ERROR|Room closed by admin');
      rooms.delete(code);
      res.writeHead(200);
      res.end(JSON.stringify({ ok: true, deleted: code }));
    } else {
      res.writeHead(404);
      res.end(JSON.stringify({ ok: false, error: 'Room not found' }));
    }
    return;
  }

  // DELETE /admin/rooms  (clear all inactive)
  if (req.method === 'DELETE' && req.url === '/admin/rooms') {
    let cleared = 0;
    for (const [code, room] of rooms) {
      if (room.status === 'waiting') {
        rooms.delete(code); cleared++;
      }
    }
    res.writeHead(200);
    res.end(JSON.stringify({ ok: true, cleared }));
    return;
  }

  // GET /admin/stats
  if (req.method === 'GET' && req.url === '/admin/stats') {
    res.writeHead(200);
    res.end(JSON.stringify({
      activeRooms:    rooms.size,
      waitingRooms:   [...rooms.values()].filter(r => !r.joiner).length,
      playingRooms:   [...rooms.values()].filter(r => r.joiner).length,
      totalGamesPlayed,
      uptime:         process.uptime(),
    }));
    return;
  }

  res.writeHead(404);
  res.end(JSON.stringify({ error: 'Not found' }));
});

// ── Protocol multiplexer: same port for TCP game + HTTP admin ─────────────────
const server = net.createServer((socket) => {
  socket.once('data', (chunk) => {
    const peek = chunk.toString('utf8', 0, 8);

    if (peek.startsWith('GET ')    ||
        peek.startsWith('POST ')   ||
        peek.startsWith('DELETE ') ||
        peek.startsWith('OPTIONS') ||
        peek.startsWith('HEAD ')) {
      // Route to HTTP handler
      httpServer.emit('connection', socket);
      socket.unshift(chunk);
    } else {
      // Game client — handle TCP protocol
      console.log('Game client connected:', socket.remoteAddress);
      let buffer = '';

      const onData = (data) => {
        buffer += data.toString();
        const lines = buffer.split('\n');
        buffer = lines.pop();
        for (const line of lines) {
          const trimmed = line.trim();
          if (trimmed) handleMessage(socket, trimmed);
        }
      };

      socket.on('data', onData);

      socket.on('close', () => {
        // Remove from quick-match queue if waiting
        const qIdx = matchQueue.findIndex(p => p.socket === socket);
        if (qIdx !== -1) matchQueue.splice(qIdx, 1);

        const result = getRoomBySocket(socket);
        if (!result) return;
        const { code, room } = result;
        const other = room.creator?.socket === socket ? room.joiner : room.creator;
        if (other?.socket) send(other.socket, 'OPPONENT_DISCONNECTED');

        if (room.status === 'playing') {
          room.status = 'waiting_rejoin';
          // Delete room if opponent doesn't rejoin within 30 seconds
          setTimeout(() => {
            if (rooms.get(code) === room && room.status === 'waiting_rejoin') {
              rooms.delete(code);
            }
          }, 30000);
        } else if (room.status === 'ended') {
          // already handled by 2-min timeout
        } else {
          rooms.delete(code);
        }
      });

      socket.on('error', () => {});

      // Re-process first chunk through game protocol
      onData(chunk);
    }
  });

  socket.on('error', () => {});
});

server.listen(PORT, () => {
  console.log(`TicTacToe server listening on port ${PORT} (TCP game + HTTP admin)`);
});
