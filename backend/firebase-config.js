// Re-export from firebaseAdmin.js to avoid double initialization
import admin from './firebaseAdmin.js';
import { getDatabase } from 'firebase-admin/database';


const db = getDatabase();

export { db, admin };
