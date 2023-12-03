const { startLoginServer } = require('./http');
const { startWebSocketServer } = require('./websocket');

startLoginServer(8081);
startWebSocketServer(8080);
