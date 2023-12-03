const http = require('http');
const { getPublicKey } = require('./encryption');
const { registerUser } = require('./userAuth');

let registerUserEndpointEnabled = true; // server.js changes value

function startLoginServer(port) {
    const server = http.createServer((req, res) => {
        if (req.url === '/public-key' && req.method === 'GET') {
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            res.end(getPublicKey());
        } else if (registerUserEndpointEnabled && req.method === 'POST' && req.url === '/register-user') {
            let name = req.headers['name']
            let data = '';
            req.on('data', (chunk) => {
                data += chunk;
            });
            req.on('end', () => {
                try {
                    registerUser(name, data);

                    res.writeHead(200, { 'Content-Type': 'text/plain' });
                    res.end('User registered successfully');
                } catch (error) {
                    console.error('Error reading request body:', error);
                    res.writeHead(400, { 'Content-Type': 'text/plain' });
                    res.end('Bad Request');
                }
            });
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('Not Found');
        }
    });

    server.listen(port, () => {
        console.log(`Public Key Server is running on port ${port}`);
    });

    return server; // Return the server instance
}

module.exports = {
    startLoginServer,
    enableRegisterUserEndpoint: () => (registerUserEndpointEnabled = true),
    disableRegisterUserEndpoint: () => (registerUserEndpointEnabled = false)
};