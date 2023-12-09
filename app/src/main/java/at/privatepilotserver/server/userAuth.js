const fs = require('fs');
const crypto = require('crypto');

function registerUser(name, token) {
    try {
        const salt = crypto.randomBytes(16).toString('hex');

        const hashedToken = crypto
            .pbkdf2Sync(token, salt, 10000, 64, 'sha256')
            .toString('hex');

        const user = {
            name: name,
            token: {
                hashed: hashedToken,
                salt: salt,
            },
        };

        let usersData = [];
        if (fs.existsSync('users.json')) {
            usersData = JSON.parse(fs.readFileSync('users.json', 'utf8'));
        }

        if (usersData.find(existingUser => existingUser.name === name)) {
            console.error('Username already taken');
            return;
        }

        usersData.push(user);

        fs.writeFileSync('users.json', JSON.stringify(usersData, null, 2), 'utf8');

        console.log('User registered successfully');
    } catch (error) {
        console.error('Error reading or parsing users data:', error);
        return false;
    }
}

function validateUser(name, token) {
    const usersData = JSON.parse(fs.readFileSync('users.json', 'utf8'));

    const user = usersData.find(existingUser => existingUser.name === name);
    if (!user) {
        return false;
    }

    const { hashed: storedHashedToken, salt } = user.token;
    const hashedTokenToValidate = crypto
        .pbkdf2Sync(token, salt, 10000, 64, 'sha256')
        .toString('hex');


    if (hashedTokenToValidate != storedHashedToken){c
        onsole.error('Username already taken');
    }

    return hashedTokenToValidate === storedHashedToken;
}


module.exports = {
    registerUser,
    validateUser
};
