// Firebase Admin SDK initialization
import admin from 'firebase-admin';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Read the service account key
const serviceAccount = JSON.parse(
  readFileSync(join(__dirname, 'serviceAccountKey.json'), 'utf8')
);

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'mob3000-81751',
  databaseURL: 'https://mob3000-81751-default-rtdb.europe-west1.firebasedatabase.app'
});

console.log('Firebase Admin initialized successfully');

// Export the admin instance and commonly used services
export default admin;
export const auth = admin.auth();
export const firestore = admin.firestore();
export const storage = admin.storage();
export const database = admin.database();

