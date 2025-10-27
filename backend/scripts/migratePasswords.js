import postgres from "postgres";
import 'dotenv/config';
import bcrypt from 'bcrypt';
import crypto from 'crypto';

const SALT_ROUNDS = 10;
const sql = postgres(process.env.DB_CONNECTION_STRING, {
    ssl: "require",
    options: '--search_path=mydb'
});

/*
* Denne filen håndterer migrering av passord og e-postadresser i databasen.
* Den bruker bcrypt for å hashe passord og SHA-256 for å hashe e-postadresser.
* Utviklet av: [Martin Hansen]
* Ai-assistanse: github copilot.
* Parametre som sendes med forespørselen er brukerens e-postadresse og passord.
*
*/
async function hashPassword(password) {
    return await bcrypt.hash(password, SALT_ROUNDS);
}

// Function to hash emails
function hashEmail(email) {
    return crypto.createHash('sha256').update(email.toLowerCase()).digest('hex');
}

async function migrateHashingColumns() {
    try {
        console.log('Starting migration of hashing columns...');
        console.log('Checking for email_hash column...');
        try {
            await sql`
                ALTER TABLE mydb.bruker 
                ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
            `;
            console.log('Added email_hash column if it didn\'t exist');
        } catch (err) {
            console.log('Column may already exist:', err.message);
        }
        
        
        console.log('Increasing password column size...');
        try {
            await sql`
                ALTER TABLE mydb.bruker 
                ALTER COLUMN passord TYPE VARCHAR(100);
            `;
            console.log('Password column size increased to VARCHAR(100)');
        } catch (err) {
            console.log('Column may already have sufficient size:', err.message);
        }
        
        
        const users = await sql`SELECT epost, passord FROM mydb.bruker`;
        console.log(`Found ${users.length} users to migrate`);
        
        let migratedPasswordCount = 0;
        let migratedEmailCount = 0;
        
        for (const user of users) {
            
            if (!user.epost || !user.passord) {
                console.log(`Skipping user with empty email or password`);
                continue;
            }
            if (!user.passord.startsWith('$2')) {
                
                const hashedPassword = await hashPassword(user.passord);                
                await sql`
                    UPDATE mydb.bruker
                    SET passord = ${hashedPassword}
                    WHERE epost = ${user.epost}
                `;
                
                migratedPasswordCount++;
                console.log(`Migrated password for ${user.epost}`);
            } else {
                console.log(`Password for ${user.epost} already appears hashed`);
            }
            
            
            const emailHash = hashEmail(user.epost);
            await sql`
                UPDATE mydb.bruker
                SET email_hash = ${emailHash}
                WHERE epost = ${user.epost} AND (email_hash IS NULL OR email_hash = '')
            `;
            migratedEmailCount++;
            console.log(`Generated email hash for ${user.epost}`);
        }
        
        console.log(`Migration complete! Migrated ${migratedPasswordCount} passwords and ${migratedEmailCount} email hashes.`);
    } catch (error) {
        console.error('Error during migration:', error);
    } finally {
        // Close the database connection
        await sql.end();
    }
}

migrateHashingColumns();