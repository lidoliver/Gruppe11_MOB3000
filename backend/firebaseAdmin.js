// Firebase Admin SDK initialization
import admin from 'firebase-admin';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let serviceAccount;

// Try to get Firebase credentials from Base64 environment variable first (for Heroku)
if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
  console.log('Using Firebase credentials from Base64 environment variable');
  try {
    const base64Credentials = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;
    const credentialsJson = Buffer.from(base64Credentials, 'base64').toString('utf8');
    serviceAccount = JSON.parse(credentialsJson);
    console.log('✅ Successfully decoded Base64 Firebase credentials');
  } catch (error) {
    console.error('❌ Failed to decode Base64 Firebase credentials:', error);
    process.exit(1);
  }
} else {
  // Fallback to local file (for development)
  console.log('Using Firebase credentials from local file');
  try {
    serviceAccount = JSON.parse(
      readFileSync(join(__dirname, 'serviceAccountKey.json'), 'utf8')
    );
    console.log('✅ Successfully loaded local Firebase credentials');
  } catch (error) {
    console.error('❌ Failed to load local Firebase credentials:', error);
    console.error('Please ensure serviceAccountKey.json exists or set FIREBASE_SERVICE_ACCOUNT_BASE64');
    process.exit(1);
  }
}

// Initialize Firebase Admin only if not already initialized
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: 'mob3000-81751',
    databaseURL: 'https://mob3000-81751-default-rtdb.europe-west1.firebasedatabase.app'
  });
  console.log('Firebase Admin initialized successfully');
} else {
  console.log('Firebase Admin already initialized, reusing existing instance');
}

// Export the admin instance and commonly used services
export default admin;
export const auth = admin.auth();
export const firestore = admin.firestore();
export const storage = admin.storage();
export const database = admin.database();

