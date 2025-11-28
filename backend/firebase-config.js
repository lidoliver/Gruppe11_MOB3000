// Re-export from firebaseAdmin.js to avoid double initialization
import admin, { database } from './firebaseAdmin.js';

// Use the already initialized database instance
const db = database;

export { db, admin };
