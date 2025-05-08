const crypto = require('crypto');
const ECKey = require('ec-key');
const jwt = require('jsonwebtoken');
const canonicalize = require('json-canonicalize').canonicalize;

// Generate a test EC key pair
function generateTestKeyPair(context, events, done) {
    try {
        console.log('Generating test key pair...');
        const keyPair = crypto.generateKeyPairSync('ec', {
            namedCurve: 'P-256'
        });
        
        // Convert the keys to PEM format
        const privateKeyPEM = keyPair.privateKey.export({
            type: 'pkcs8',
            format: 'pem'
        });
        
        const publicKeyPEM = keyPair.publicKey.export({
            type: 'spki',
            format: 'pem'
        });
        
        // Store the key pair in context
        context.vars.keyPair = {
            privateKey: privateKeyPEM,
            publicKey: publicKeyPEM
        };
        
        console.log('Key pair generated successfully');
        return done();
    } catch (error) {
        console.error('Error generating key pair:', error);
        return done(error);
    }
}

// Generate test data
function generateTestData(context, events, done) {
    try {
        console.log('Generating test data...');
        context.vars.testData = {
            customerId: crypto.randomBytes(8).toString('hex'),
            phoneNumber: `+${Math.floor(Math.random() * 1000000000)}`,
            email: `test${crypto.randomBytes(4).toString('hex')}@example.com`,
            accountId: crypto.randomBytes(8).toString('hex'),
            timestamp: new Date().toISOString()
        };
        console.log('Test data generated:', context.vars.testData);
        return done();
    } catch (error) {
        console.error('Error generating test data:', error);
        return done(error);
    }
}

// Generate a JWT token
function generateJWT(context, events, done) {
    try {
        // Get current timestamp
        const now = new Date();
        
        // Flatten timestamp to nearest previous 15-minute interval for device registration
        const flattenedMinute = Math.floor(now.getMinutes() / 15) * 15;
        const flattenedTimestamp = new Date(now);
        flattenedTimestamp.setMinutes(flattenedMinute);
        flattenedTimestamp.setSeconds(0);
        flattenedTimestamp.setMilliseconds(0);
        
        // Format timestamp in ISO format without milliseconds
        const timestamp = flattenedTimestamp.toISOString().replace(/\.\d{3}Z$/, 'Z');
        
        // Set the flattened timestamp in context for use in request body
        context.vars.timestamp = timestamp;

        // Create JWT header with JWK
        const header = {
            jwk: context.vars.keyPair.publicKey
        };

        // Determine what data to hash based on the endpoint
        let dataToHash;
        let requestBody;

        // Get the target URL from the scenario context if available
        const targetUrl = context.vars.request?.url || '';

        if (targetUrl.includes('/api/prs/dev/init')) {
            // For device registration, only use the flattened timestamp
            dataToHash = timestamp;
            requestBody = {
                timeStamp: timestamp,
                accountId: context.vars.testData.accountId || '',
                phoneNumber: context.vars.testData.phoneNumber || '',
                email: context.vars.testData.email || '',
                customerId: context.vars.testData.customerId || ''
            };
        } else if (targetUrl.includes('/api/prs/dev/validate')) {
            // For device validation, use the nonce and other parameters
            dataToHash = context.vars.initiationNonce + context.vars.powNonce;
            requestBody = {
                initiationNonce: context.vars.initiationNonce,
                powNonce: context.vars.powNonce,
                powHash: context.vars.powHash
            };
        } else {
            // For other endpoints or when URL is not available, use the original data
            dataToHash = JSON.stringify({
                timestamp: timestamp,
                accountId: context.vars.testData.accountId || '',
                phoneNumber: context.vars.testData.phoneNumber || '',
                email: context.vars.testData.email || '',
                customerId: context.vars.testData.customerId || ''
            });
            requestBody = {
                timestamp: timestamp,
                accountId: context.vars.testData.accountId || '',
                phoneNumber: context.vars.testData.phoneNumber || '',
                email: context.vars.testData.email || '',
                customerId: context.vars.testData.customerId || ''
            };
        }

        // Store request body in context
        context.vars.requestBody = requestBody;

        // The server uses UTF-8 encoding and hex string format for the hash
        const hash = crypto.createHash('sha256')
            .update(Buffer.from(dataToHash, 'utf8'))  // Use UTF-8 encoding like the server
            .digest('hex')  // Convert to hex string like the server
            .toLowerCase();  // Ensure lowercase hex like the server's String.format("%02x", b)

        // Create JWT payload
        const payload = {
            hash: hash,  // This hash is validated by JwtValidator.validateAndExtract
            iat: Math.floor(Date.now() / 1000),
            exp: Math.floor(Date.now() / 1000) + 3600,
            iss: 'https://webank.com',
            sub: '',
            jti: crypto.randomBytes(16).toString('hex')
        };

        // Sign the JWT
        const token = jwt.sign(payload, context.vars.keyPair.privateKey, {
            algorithm: 'ES256',
            header: header
        });

        // Store the JWT token in context
        context.vars.jwtToken = token;

        return done();
    } catch (error) {
        console.error('Error generating JWT:', error);
        return done(error);
    }
}

module.exports = {
    generateTestKeyPair,
    generateTestData,
    generateJWT
};
