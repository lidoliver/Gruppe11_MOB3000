import fs from 'fs';
import path from 'path';
import postgres from "postgres";
import 'dotenv/config';

const sql = postgres(process.env.DB_CONNECTION_STRING, {
    ssl: "require",
    options: '--search_path=mydb'
});

async function setupBackupSystem() {
    try {
        console.log('Setting up database backup tables and restore function...');
        
        const sqlFilePath = path.join(__dirname, 'create_backup_tables.sql');
        const sqlScript = fs.readFileSync(sqlFilePath, 'utf8');
        
        // Execute the SQL script
        await sql.unsafe(sqlScript);
        
        console.log('Backup tables and restore function created successfully!');
    } catch (error) {
        console.error('Error setting up backup system:', error);
    } finally {
        await sql.end();
    }
}

setupBackupSystem();