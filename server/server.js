const net  = require('net');
const http = require('http');

const PORT = process.env.PORT || 36509;

// rooms: { code -> { creator, joiner, board, currentTurn, createdAt } }
const rooms = new Map();
let totalGamesPlayed = 0;

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
      rooms.delete(code);
      return;
    }

    if (isFull(room.board)) {
      send(mover.socket,    'DRAW');
      send(opponent.socket, 'DRAW');
      console.log(`Room ${code}: draw`);
      totalGamesPlayed++;
      rooms.delete(code);
      return;
    }

    room.currentTurn = opponent.symbol;
    send(opponent.socket, 'YOUR_TURN');
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
        const result = getRoomBySocket(socket);
        if (!result) return;
        const { code, room } = result;
        const other = room.creator?.socket === socket ? room.joiner : room.creator;
        if (other?.socket) send(other.socket, 'ERROR|Opponent disconnected');
        rooms.delete(code);
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
