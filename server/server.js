const net = require('net');

const PORT = process.env.PORT || 36509;

// rooms: { code -> { creator: {socket, username, symbol}, joiner: {socket, username, symbol} } }
const rooms = new Map();

const WIN_COMBOS = [
  [0,1,2],[3,4,5],[6,7,8],
  [0,3,6],[1,4,7],[2,5,8],
  [0,4,8],[2,4,6]
];

function generateCode() {
  return Math.floor(1000 + Math.random() * 9000).toString();
}

function send(socket, message) {
  try {
    socket.write(message + '\n');
  } catch (e) {}
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
      creator: { socket, username, symbol: 'X' },
      joiner: null,
      board: Array(9).fill(null),
      currentTurn: 'X'   // X always goes first
    });

    send(socket, `ROOM_CREATED|${code}|X`);
    console.log(`Room ${code} created by ${username}`);

  } else if (cmd === 'JOIN_ROOM') {
    const code = parts[1];
    const username = parts[2] || 'Player';
    const room = rooms.get(code);

    if (!room) {
      send(socket, 'ERROR|Room not found');
      return;
    }
    if (room.joiner) {
      send(socket, 'ERROR|Room is full');
      return;
    }

    room.joiner = { socket, username, symbol: 'O' };

    // Tell joiner their symbol AND creator's name
    send(socket, `JOIN_SUCCESS|O`);
    send(socket, `START_GAME|${room.creator.username}`);   // joiner sees creator's name

    // Tell creator that joiner arrived with joiner's name
    send(room.creator.socket, `START_GAME|${username}`);  // creator sees joiner's name

    // X goes first → tell creator (X) it's their turn
    send(room.creator.socket, 'YOUR_TURN');

    console.log(`Room ${code}: ${room.creator.username}(X) vs ${username}(O) — game started`);

  } else if (cmd === 'MOVE') {
    const index = parseInt(parts[1]);
    if (isNaN(index) || index < 0 || index > 8) return;

    const result = getRoomBySocket(socket);
    if (!result) return;
    const { code, room } = result;

    const isCreator = room.creator?.socket === socket;
    const mover    = isCreator ? room.creator : room.joiner;
    const opponent = isCreator ? room.joiner  : room.creator;

    if (!opponent) return;
    if (room.board[index] !== null) return;
    if (room.currentTurn !== mover.symbol) return;  // not their turn

    room.board[index] = mover.symbol;
    send(opponent.socket, `OPPONENT_MOVED|${index}`);

    if (checkWin(room.board, mover.symbol)) {
      send(mover.socket,    'WIN');
      send(opponent.socket, 'LOSE');
      console.log(`Room ${code}: ${mover.username} wins`);
      rooms.delete(code);
      return;
    }

    if (isFull(room.board)) {
      send(mover.socket,    'DRAW');
      send(opponent.socket, 'DRAW');
      console.log(`Room ${code}: draw`);
      rooms.delete(code);
      return;
    }

    // Switch turn
    room.currentTurn = opponent.symbol;
    send(opponent.socket, 'YOUR_TURN');
  }
}

const server = net.createServer((socket) => {
  console.log('Client connected:', socket.remoteAddress);
  let buffer = '';

  socket.on('data', (data) => {
    buffer += data.toString();
    const lines = buffer.split('\n');
    buffer = lines.pop();  // keep incomplete line
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed) handleMessage(socket, trimmed);
    }
  });

  socket.on('close', () => {
    console.log('Client disconnected');
    const result = getRoomBySocket(socket);
    if (!result) return;
    const { code, room } = result;

    // Notify the other player
    const other = room.creator?.socket === socket ? room.joiner : room.creator;
    if (other?.socket) send(other.socket, 'ERROR|Opponent disconnected');
    rooms.delete(code);
  });

  socket.on('error', () => {});
});

server.listen(PORT, () => {
  console.log(`TicTacToe server listening on port ${PORT}`);
});
