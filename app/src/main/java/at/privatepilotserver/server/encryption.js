const crypto = require('crypto');

let privateKey;
let publicKey;
let clientPublicKey;

generateKeyPair().then(({ privateKey: privKey, publicKey: pubKey }) => {
    privateKey = privKey;
    publicKey = pubKey;
});

function getPrivateKey() {
    return privateKey;
}

function getPublicKey() {
    return publicKey;
}

function getClientPublicKey() {
    return clientPublicKey;
}

async function generateKeyPair() {
    const { privateKey: privKey, publicKey: pubKey } = crypto.generateKeyPairSync('rsa', {
        modulusLength: 4096,
        publicKeyEncoding: { type: 'spki', format: 'pem' },
        privateKeyEncoding: { type: 'pkcs1', format: 'pem', cipher: 'aes-256-cbc', passphrase: '' }
    });

    privateKey = privKey;
    publicKey = pubKey;

    return { privateKey, publicKey };
}

async function getClientKey(key) {
    const publicKeyBytes = Uint8Array.from(Buffer.from(key, 'base64'));

    clientPublicKey = await crypto.subtle.importKey(
        'spki',
        publicKeyBytes,
        { name: 'RSA-OAEP', hash: 'SHA-256' },
        true,
        ['encrypt']
    );
}

async function encrypt(originalMessage) {
    try {
        const maxChunkSize = 50;
        const encodedMessage = new TextEncoder().encode(originalMessage);
        const chunks = [];

        for (let i = 0; i < encodedMessage.length; i += maxChunkSize) {
            const chunk = encodedMessage.slice(i, i + maxChunkSize);
            chunks.push(chunk);
        }

        const encryptedChunks = [];

        for (const chunk of chunks) {
            const encryptedBuffer = await crypto.subtle.encrypt(
                { name: 'RSA-OAEP' },
                clientPublicKey,
                chunk
            );

            const encryptedBase64 = Buffer.from(encryptedBuffer).toString('base64');
            encryptedChunks.push(encryptedBase64);
        }

        const encryptedMessage = encryptedChunks.join('*');

        return encryptedMessage;
    } catch (error) {
        console.error(error);
        return '';
    }
}

function decodeWithKey(token) {
    try {
        const buffer = Buffer.from(token, 'base64');
        const decryptedBuffer = crypto.privateDecrypt({
            key: privateKey,
            passphrase: '',
            padding: crypto.constants.RSA_PKCS1_OAEP_PADDING,
            oaepHash: 'sha256',
        }, buffer);

        let decodedMessage = decryptedBuffer.toString('utf-8');

        return decodedMessage;
    } catch (error) {
        console.error('Decryption error:', error);
    }
}

module.exports = {
    getClientKey,
    encrypt,
    decodeWithKey,
    getPublicKey
};
