const { getClientKey, encrypt, decodeWithKey } = require('./encryption');
const { validateUser } = require('./userAuth')

const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const WebSocket = require('ws');

const BASE_DIRECTORY = 'C:/Users/lampr/Desktop/fileStorage';


function startWebSocketServer(port) {
    const wss = new WebSocket.Server({ port: port });
    console.log(`WebSocket Server is running on port ${port}`);

    wss.on('connection', async (ws, req) => {
        const encodedName = req.headers['username'];
        const encodedToken = req.headers['authorization'];
        const clientPublicKey = req.headers['publickey'];

        await getClientKey(clientPublicKey);

        const decodedName = decodeWithKey(encodedName);
        const decodedToken = decodeWithKey(encodedToken);

        if (!validateUser(decodedName, decodedToken)) {
            ws.terminate();
            return;
        }

        ws.on('message', async (message) => {
            const buffer = Buffer.from(message);
            const request = buffer.toString();

            if (request.startsWith('GET:')) {
                handleGetRequest(ws, request);
            } else if (request.startsWith('POST:')) {
                handlePostRequest(ws, request);
            } else if (request.startsWith('DELETE:')) {
                handleDeleteRequest(ws, request);
            } else if (request.startsWith('FIND:')) {
                handleSearchRequest(ws, request);
            } else if (request.startsWith('UPDATE:')) {
                handleUpdateRequest(ws, request);
            }
        });

        ws.on('close', () => {
            console.log('Client disconnected');
        });
    });
}

let pointer = BASE_DIRECTORY;

async function sendToClient(ws, message) {
    const encryptedMessage = await encrypt(message);

    ws.send(encryptedMessage, (error) => {
        if (error) {
            console.error(`Error sending file: ${error.message}`);
        }
    });
}



function handleGetRequest(ws, request) {
    const folderOrFilePath = request.slice('GET:'.length);
    const fullPath = path.join(BASE_DIRECTORY, folderOrFilePath);

    if (fs.existsSync(fullPath)) {
        if (fs.statSync(fullPath).isDirectory()) {
            const items = listFileMetadata(fullPath);
            const itemsJSON = JSON.stringify({'items': items });
            pointer = fullPath;
            const relative_path = path.relative(BASE_DIRECTORY, pointer)

            console.log(`Listing files in: ${fullPath}`);

            sendToClient(ws, `/${relative_path};${itemsJSON}`);
            sendFilePreviews(ws, items);
        } else if (fs.statSync(fullPath).isFile()) {
            console.log(`Sending file: ${fullPath}`);
            sendFileToClient(ws, fullPath);
        }
    }
}

function handlePostRequest(ws, request) {
    const relativeFilePath = request.slice('POST:'.length);
    const relativeDirectory = path.dirname(relativeFilePath);

    const folderPath = path.join(BASE_DIRECTORY, relativeDirectory);
    const filePath = path.join(BASE_DIRECTORY, relativeFilePath);

    if (!fs.existsSync(folderPath)) {
        fs.mkdirSync(folderPath, { recursive: true });
    }

    ws.once('message', (fileData) => {
        console.log(`Received file data for: ${filePath}`);
        saveFileToServer(filePath, fileData);
    });
}

function handleDeleteRequest(ws, request) {
    const folderOrFilePath = request.slice('DELETE:'.length);
    const fullPath = path.join(BASE_DIRECTORY, folderOrFilePath);

    if (fs.existsSync(fullPath)) {
        fs.unlinkSync(fullPath);
        console.log(`Deleted file: ${fullPath}`);
    }
}

function listFileMetadata(fullPath) {
    const items = [];
    const itemsInDirectory = fs.readdirSync(fullPath);

    for (const item of itemsInDirectory) {
        const itemPath = path.join(fullPath, item);
        const relativePath = path.relative(BASE_DIRECTORY, itemPath).replace(/\\/g, '/');
        const itemStat = fs.statSync(itemPath);
        const itemType = itemStat.isFile() ? 'file' : 'folder';

        items.push({
            name: item,
            path: relativePath,
            type: itemType,
            size: itemStat.isFile() ? itemStat.size : null,
            last_modified: new Date(itemStat.mtime).getTime() / 1000
        });
    }

    return items;
}

function sendFileToClient(ws, filePath) {
    fs.readFile(filePath, (err, fileData) => {
        if (err) {
            console.error(`Error reading file: ${err.message}`);
        } else {
            console.log(`Sending file data for: ${filePath}`);
            ws.send(fileData); //quickfix to send files to client, currently unencrypted
        }
    });
}

function saveFileToServer(filePath, fileData) {
    console.log(`Saving file to: ${filePath}`);
    fs.writeFile(filePath, fileData, (err) => {
        if (err) {
            console.error(`Error saving file: ${err.message}`);
        } else {
            console.log(`Saved file to: ${filePath}`);
        }
    });
}

function handleSearchRequest(ws, request) {
    const searchQuery = request.slice('FIND:'.length);
    const searchRegex = new RegExp(searchQuery, 'i');
    const searchResults = searchFiles(pointer, searchRegex);
    const resultsJSON = JSON.stringify({ 'items': searchResults });

    const relative_path = path.relative(BASE_DIRECTORY, pointer)

    console.log(`Search results for query "${searchQuery}" in ${pointer}:`);
    sendToClient(ws, `/${relative_path};${resultsJSON}`);
}

function searchFiles(fullPath, searchRegex) {
    const items = [];
    const itemsInDirectory = fs.readdirSync(fullPath);

    for (const item of itemsInDirectory) {
        const itemPath = path.join(fullPath, item);
        const itemStat = fs.statSync(itemPath);
        if (searchRegex.test(item)) {
            const relativePath = path.relative(BASE_DIRECTORY, itemPath).replace(/\\/g, '/');
            const itemType = itemStat.isFile() ? 'file' : 'folder';

            items.push({
                name: item,
                path: relativePath,
                type: itemType,
                size: itemStat.isFile() ? itemStat.size : null,
                last_modified: new Date(itemStat.mtime).getTime() / 1000
            });
        }

        if (itemStat.isDirectory()) {
            const subdirectoryResults = searchFiles(itemPath, searchRegex);
            items.push(...subdirectoryResults);
        }
    }

    return items;
}

function handleUpdateRequest(ws, request) {
    const [sourcePath, destinationPath] = request.slice('UPDATE:'.length).split(';');
    const fullSourcePath = path.join(BASE_DIRECTORY, sourcePath);
    const fullDestinationPath = path.join(BASE_DIRECTORY, destinationPath);

    console.log(`Source Path: ${fullSourcePath}`);
    console.log(`Destination Path: ${fullDestinationPath}`);

    try {
        if (fs.existsSync(fullSourcePath)) {
            const isSourceDirectory = fs.lstatSync(fullSourcePath).isDirectory();
            let finalDestinationPath;

            if (isSourceDirectory) {
                finalDestinationPath = path.join(fullDestinationPath, path.basename(fullSourcePath));
            } else {
                finalDestinationPath = path.join(fullDestinationPath, path.basename(fullSourcePath));
            }

            console.log(`Final Destination Path: ${finalDestinationPath}`);
            fs.renameSync(fullSourcePath, finalDestinationPath);
            console.log(`Moved file/directory from ${fullSourcePath} to ${finalDestinationPath}`);
        } else {
            console.error(`Source path does not exist: ${fullSourcePath}`);
        }
    } catch (error) {
        console.error(`Error moving file/directory: ${error.message}`);
    }
}

async function sendFilePreviews(ws, items) {
    for (const item of items) {
        const filePath = path.join(BASE_DIRECTORY, item.path);
        const fileExtension = path.extname(filePath).toLowerCase();
        let preview;

        if (/\.(jpg|png|gif)$/i.test(fileExtension)) {
            preview = await generateImagePreview(filePath);
        }

        if (preview != null) {
            console.log(`Sending preview for file: ${item.name}`);
            sendToClient(ws, `base64;${item.path};${preview}`);
        }
    }
}

async function generateImagePreview(filePath) {
    try {
        const imageBuffer = fs.readFileSync(filePath);
        const resizedBuffer = await sharp(imageBuffer)
            .resize({ width: 120, height: 150 })
            .toBuffer();

        const base64Preview = resizedBuffer.toString('base64');
        return base64Preview;
    } catch (error) {
        console.error(`Error generating preview for ${filePath}: ${error.message}`);
        return null;
    }
}

module.exports = {
    startWebSocketServer,
};
