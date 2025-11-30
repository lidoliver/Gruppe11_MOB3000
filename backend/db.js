import postgres from "postgres";
import 'dotenv/config';
import bcrypt from 'bcrypt';
import crypto from 'crypto';
import { get } from "node:http";


const SALT_ROUNDS = 10; // For bcrypt passord-hashing

async function hashPassword(password) {
    return await bcrypt.hash(password, SALT_ROUNDS);
}

async function comparePassword(plainPassword, hashedPassword) {
    return await bcrypt.compare(plainPassword, hashedPassword);
}

async function changePassword(email, currentPassword, newPassword) {
    try {
        // Get user from database
        const user = await sql`
            SELECT passord FROM mydb.bruker WHERE epost = ${email}
        `;

        if (!user || user.length === 0) {
            throw new Error('User not found');
        }

        // Verify current password
        const passwordMatch = await comparePassword(currentPassword, user[0].passord);
        if (!passwordMatch) {
            throw new Error('Current password is incorrect');
        }

        // Hash new password
        const hashedNewPassword = await hashPassword(newPassword);

        // Update password in database
        const result = await sql`
            UPDATE mydb.bruker
            SET passord = ${hashedNewPassword}
            WHERE epost = ${email}
            RETURNING epost
        `;

        return result[0];
    } catch (error) {
        console.error('Error changing password:', error);
        throw error;
    }
}

function hashEmail(email) {
    // Hasher epostadressen for å beskytte personvern
    return crypto.createHash('sha256').update(email.toLowerCase()).digest('hex');
}

const sql = postgres(process.env.DB_CONNECTION_STRING,{
    ssl: "require",
    options: '--search_path=mydb'
});

export { sql };

async function validateConnection() {
    try {
        await sql`SELECT 1`;
        console.log('✅ PostgreSQL tilkobling vellykket');
    } catch (error) {
        console.error('❌ PostgreSQL tilkobling mislykket:', error);
        process.exit(1); // stopper prosessen
    }
}

async function createUser(email, password) {
    const hashedPassword = await hashPassword(password);
    const emailHash = hashEmail(email); 
    
    const result = await sql`
        INSERT INTO mydb.bruker (epost, passord, brukernavn, email_hash)
        VALUES (${email}, ${hashedPassword}, ${email}, ${emailHash})
        RETURNING epost;
    `;
    return result[0];
}

// fullstendig innlogging funksjon
async function logInn(email, password) {
    const result = await sql`
        SELECT * FROM mydb.bruker WHERE epost = ${email}
    `;
    
    if (result.length === 0) {
        return null; // om bruker ikke finnes
    }
    
    const user = result[0];
    
    // ser om brukeren er utestengt
    if (user.role === 'banned') {
        throw new Error('This account has been banned');
    }
    
    // bruker bycrypt for å sammenligne passord
    const passwordMatch = await comparePassword(password, user.passord);
    
    if (passwordMatch) {
        return user;
    }
    
    return null; // om passordet ikke stemmer 
}

//henter bruker med epost
async function getBruker(email) {
    const result = await sql`
        SELECT epost, enavn, fnavn, brukernavn, passord, role, visible, klubb_id_klubb
        FROM mydb.bruker
        WHERE epost = ${email};
    `;
    return result[0];
}

//henter bruker med brukernavn, nyttig om bruker bruker brukernavn i stedet for epost
async function getBrukerByUsername(username) {
    const result = await sql`
        SELECT epost, enavn, fnavn, brukernavn, passord, role, visible
        FROM mydb.bruker
        WHERE brukernavn = ${username};
    `;
    return result[0];
}

async function redigerBruker(email, bruker) {
    try {
        console.log('Attempting to update user:', { email, bruker });

        if (!email || !bruker) {
            console.error('Missing required parameters:', { email, bruker });
            throw new Error('Missing required parameters');
        }

        
        const currentUser = await sql`
            SELECT * FROM mydb.bruker WHERE epost = ${email}
        `;

        if (!currentUser || currentUser.length === 0) {
            console.error('User not found:', email);
            throw new Error('User not found');
        }

      
        const validatedData = {
            brukernavn: bruker.brukernavn || currentUser[0].brukernavn,
            fnavn: bruker.fnavn || currentUser[0].fnavn,
            enavn: bruker.enavn || currentUser[0].enavn,
            passord: bruker.passord,
            visible: typeof bruker.visible === 'boolean' ? bruker.visible : currentUser[0].visible
        };

        // Validate username doesn't contain @ symbol
        if (validatedData.brukernavn && validatedData.brukernavn.includes('@')) {
            throw new Error('Username cannot contain @ symbol');
        }


        let finalPassword = currentUser[0].passord; 
        if (validatedData.passord && validatedData.passord !== currentUser[0].passord) {
          
            if (!validatedData.passord.startsWith('$2')) {

                finalPassword = await hashPassword(validatedData.passord);
            } else {
                finalPassword = validatedData.passord;
            }
        }


        
        const result = await sql`
            UPDATE mydb.bruker
            SET 
                brukernavn = ${validatedData.brukernavn},
                fnavn = ${validatedData.fnavn},
                enavn = ${validatedData.enavn},
                passord = ${finalPassword},
                visible = ${validatedData.visible}
            WHERE epost = ${email}
            RETURNING epost, brukernavn, fnavn, enavn, visible, role, klubb_id_klubb;
        `;

        if (!result || result.length === 0) {
            console.error('Update failed - no rows returned');
            throw new Error('Update failed');
        }

        const updatedUser = result[0];

        // Fetch klubbnavn if user has a klubb_id_klubb
        if (updatedUser.klubb_id_klubb) {
            const klubb = await sql`
                SELECT klubbnavn FROM mydb.klubb WHERE id_klubb = ${updatedUser.klubb_id_klubb}
            `;
            if (klubb && klubb.length > 0) {
                updatedUser.klubbnavn = klubb[0].klubbnavn;
            }
        }

        return updatedUser;
    } catch (error) {
        console.error('Error in redigerBruker:', error);
        console.error('Error stack:', error.stack);
        throw error;
    }
}  

async function deleteBruker(email) {
    const result = await sql`
        DELETE FROM mydb.bruker
        WHERE epost = ${email}
        RETURNING epost;
    `;
    return result[0];
}
/*
* Denne funksjonen henter poengkort for en bruker fra databasen. 
* Den tar inn brukerens e-postadresse som parameter og returnerer en liste med poengkort.
* Utviklet av: Oliver Fiveland
*/
async function getPoengkort(email) {
    try {
       
        const result = await sql`
            SELECT 
                p.pointcard_id, 
                p.bane_id, 
                p.spiller_navn, 
                p.score,
                p.total_score,
                (p.dato AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Oslo') as dato,
                (p.oppdatert_dato AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Oslo') as oppdatert_dato
            FROM mydb.pointcard p
            WHERE p.spiller_navn = ${email}
            ORDER BY p.opprettet_dato DESC
            LIMIT 10;
        `;
        
        console.log(`Query completed, found ${result.length} records`);
        return result;
    } catch (error) {
        console.error('ERROR IN BASIC QUERY:', error);
        return [];
    }
}

/*
* Denne funksjonen oppretter et nytt poengkort for en bane i databasen.
* Den tar inn bane-ID, spillernavn, og poengdata for hvert hull som parametere.
* Github Copilot hjalp med å lage denne spørringen
* Utviklet av: Oliver Fiveland
*/
async function createPoengkortForCourse(bane_id, spiller_navn, hullPoeng) {
    try {

        const total_score = hullPoeng.reduce((sum, hp) => sum + (hp.poeng || 0), 0);
        
        const fieldValues = hullPoeng.map(hp => 
            `('${hp.hull_id}', ${hp.poeng})`
        ).join(', ');
        
        const query = `
            INSERT INTO mydb.pointcard (
                bane_id, 
                spiller_navn, 
                score,
                total_score,
                dato,
                opprettet_dato
            ) 
            VALUES (
                '${bane_id}', 
                '${spiller_navn}', 
                (SELECT jsonb_agg(json_build_object('hull_id', t.id, 'score', t.score)) FROM (VALUES ${fieldValues}) AS t(id, score)),
                ${total_score},
                (NOW() AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Oslo',
                (NOW() AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Oslo'
            )
            RETURNING pointcard_id, bane_id, spiller_navn, score, total_score, dato, opprettet_dato
        `;
        
        
        const result = await sql.unsafe(query);
        
        if (!result || result.length === 0) {
            throw new Error('No result returned from database insertion');
        }
        
        return result[0];
    } catch (error) {
        console.error('Error in createPoengkortForCourse:', error);
        throw error;
    }
}

/*
* Denne funksjonen sletter et poengkort fra databasen basert på ID.
* Den tar inn poengkort-ID som parameter og returnerer det slettede poengkortet.
* Utviklet av: Oliver Fiveland
*/
async function deletePoengkort(pointcard_id) {
    const result = await sql`
        DELETE FROM mydb.pointcard
        WHERE pointcard_id = ${pointcard_id}
        RETURNING 
            pointcard_id, 
            bane_id, 
            spiller_navn, 
            opprettet_dato;
    `;
    return result[0];
}

/*
* Denne funksjonen oppdaterer et eksisterende poengkort i databasen.
* Den tar inn poengkort-ID og oppdaterte poengdata som parametere.
* Brukte Githhub Copilot til å lage denne funksjonen.
* Utviklet av: Oliver Fiveland
*/
async function updatePoengkort(pointcard_id, scoreData) {
    try {
        const hullPoeng = scoreData.hullPoeng || scoreData.holes || [];
        
        const total_score = hullPoeng.reduce((sum, hole) => sum + (hole.poeng || 0), 0);
        
        const fieldValues = hullPoeng.map(hp => 
            `('${hp.hull_id}', ${hp.poeng})`
        ).join(', ');
        
        const query = `
            UPDATE mydb.pointcard
            SET 
                score = (SELECT jsonb_agg(json_build_object('hull_id', t.id, 'score', t.score)) FROM (VALUES ${fieldValues}) AS t(id, score)),
                total_score = ${total_score},
                oppdatert_dato = NOW() AT TIME ZONE 'Europe/Oslo'
            WHERE pointcard_id = ${pointcard_id}
            RETURNING pointcard_id, bane_id, spiller_navn, score, total_score, dato, oppdatert_dato
        `;
        
        const result = await sql.unsafe(query);
        
        if (!result || result.length === 0) {
            throw new Error('No result returned from database update');
        }
        
        return result[0];
    } catch (error) {
        console.error('Error in updatePoengkort:', error);
        throw error;
    }
}

async function getCompleteRound(pointcard_id) {
    const result = await sql`
        SELECT p.pointcard_id, p.bane_id, p.spiller_navn, 
               p.score, p.total_score, p.opprettet_dato
        FROM mydb.pointcard p
        WHERE p.pointcard_id = ${pointcard_id};
    `;
    return result[0];
}

async function getBaneInfo(bane_id) {
    try {
        const result = await sql`
            SELECT *
            FROM mydb.bane
            WHERE bane_id = ${bane_id};
        `;

        if (!result || result.length === 0) {
            return null;
        }

        const bane = result[0];

        // Fetch holes data for this bane (including GPS coordinates if available)
        try {
            const holesResult = await sql`
                SELECT
                    hull_id,
                    hull_nummer,
                    tee_latitude,
                    tee_longitude,
                    basket_latitude,
                    basket_longitude,
                    hole_par,
                    hole_distance
                FROM mydb.hull
                WHERE bane_id_bane = ${bane_id}
                ORDER BY hull_nummer;
            `;

            if (holesResult && holesResult.length > 0) {
                bane.holes = holesResult.map(hole => ({
                    holeId: hole.hull_id,
                    holeNumber: hole.hull_nummer,
                    teeLatitude: hole.tee_latitude,
                    teeLongitude: hole.tee_longitude,
                    basketLatitude: hole.basket_latitude,
                    basketLongitude: hole.basket_longitude,
                    par: hole.hole_par || 3,
                    distance: hole.hole_distance
                }));
                console.log(`Bane ${bane_id} includes ${bane.holes.length} holes with GPS data`);
            } else {
                bane.holes = [];
                console.log(`Bane ${bane_id} has no holes data in database`);
            }
        } catch (holesError) {
            console.error(`Error fetching holes for bane ${bane_id}:`, holesError);
            bane.holes = []; // Empty array if error
        }

        return bane;
    } catch (error) {
        console.error(`Error retrieving bane with ID ${bane_id}:`, error);
        throw error;
    }
}

async function getRoundsForUser(email) {
    const result = await sql`
        SELECT p.pointcard_id, p.bane_id, b.navn AS bane_navn, 
               p.dato, p.total_score, p.opprettet_dato
        FROM mydb.pointcard p
        JOIN mydb.bane b ON p.bane_id = b.bane_id
        WHERE p.spiller_navn = ${email}
        ORDER BY p.opprettet_dato DESC
        LIMIT 10;
    `;
    return result;
}

/**
 * Lager en ny bane i databasen
 * @param {Object} trackData - inneholder bane data som: navn, beskrivelse, hull, markører osv.
 * @returns {Promise<Object>} - lagde banen med id
 */
async function createTrack(trackData) {
    try {
        // Handle obLines data - ensure it's an array
        const obLines = Array.isArray(trackData.obLines) ? trackData.obLines : 
                       (trackData.ob_lines && Array.isArray(trackData.ob_lines) ? trackData.ob_lines : []);
        
        // Log for debugging
        console.log(`Creating track ${trackData.trackName} with ${obLines.length} OB lines`);
        console.log(`Club ID for track: ${trackData.klubb_id_klubb}`);
        
        // IMPORTANT: Only set klubb_id_klubb if it's a valid value
        const clubId = trackData.klubb_id_klubb && trackData.klubb_id_klubb > 0 ? 
                      trackData.klubb_id_klubb : null;
        
        const trackResult = await sql`
            INSERT INTO mydb.bane (
                navn, 
                beskrivelse, 
                total_distance, 
                total_par, 
                antall_hull, 
                markers, 
                lines,
                ob_lines, 
                klubb_id_klubb,
                rating,
                bane_bilde,
                opprettet_dato
            ) VALUES (
                ${trackData.trackName}, 
                ${trackData.description || ''}, 
                ${trackData.totalDistance || 0}, 
                ${trackData.totalPar || 0}, 
                ${trackData.holeCount || 0},
                ${JSON.stringify(trackData.markers || [])}::jsonb,
                ${JSON.stringify(trackData.lines || [])}::jsonb,
                ${JSON.stringify(obLines)}::jsonb,
                ${clubId}, 
                ${trackData.rating || 0},
                ${trackData.imageData || null},
                NOW()
            )
            RETURNING bane_id;
        `;
        
        if (!trackResult || trackResult.length === 0 || !trackResult[0].bane_id) {
            throw new Error('Failed to create track - database did not generate ID');
        }
        
        const bane_id = trackResult[0].bane_id;
        console.log(`Successfully created track with ID: ${bane_id}`);
        
        return getTrackById(bane_id);
    } catch (error) {
        console.error('Error in createTrack:', error);
        throw error;
    }
}

/**
 * Get bane med ID
 * @param {number} bane_id - bane ID
 * @returns {Promise<Object>} bane dataen
 */
async function getTrackById(bane_id) {
    try {
        // console.log(`Retrieving track with ID: ${bane_id}`); <- debuging redskap
        
        const result = await sql`
            SELECT 
                bane_id,
                navn AS "trackName",
                beskrivelse AS "description",
                total_distance AS "totalDistance",
                total_par AS "totalPar",
                antall_hull AS "holeCount",
                markers,
                lines,
                ob_lines AS "obLines",
                rating,
                bane_bilde AS "imageData",
                klubb_id_klubb,
                opprettet_dato AS "created",
                oppdatert_dato AS "updated"
            FROM mydb.bane
            WHERE bane_id = ${bane_id};
        `;
        
        // forsikre at banen har data
        if (!result || result.length === 0) {
           // console.log(`No track found with ID: ${bane_id}`); <- debuging redskap
            return null;
        }
        
        // Add detailed OB lines debugging
        console.log(`Track ${bane_id} OB lines raw data:`, result[0].obLines);
        console.log(`Track ${bane_id} OB lines type:`, typeof result[0].obLines);
        
        // Process OB lines - ensure they're parsed correctly
        let processedObLines = [];
        if (result[0].obLines) {
            try {
                // If it's already an array, use it directly
                if (Array.isArray(result[0].obLines)) {
                    processedObLines = result[0].obLines;
                    console.log(`Track ${bane_id} OB lines are already an array with length:`, processedObLines.length);
                } 
                // If it's a string, parse it
                else if (typeof result[0].obLines === 'string') {
                    processedObLines = JSON.parse(result[0].obLines);
                    console.log(`Track ${bane_id} OB lines parsed from string:`, processedObLines);
                } 
                // Handle PostgreSQL JSONB format
                else if (typeof result[0].obLines === 'object') {
                    processedObLines = result[0].obLines;
                    console.log(`Track ${bane_id} OB lines are in object format:`, processedObLines);
                }
            } catch (e) {
                console.error(`Error processing OB lines for track ${bane_id}:`, e);
                processedObLines = [];
            }
        }
        
        // Set both fields for consistency
        result[0].obLines = processedObLines;
        result[0].ob_lines = processedObLines;
        
        console.log(`Track ${bane_id} final OB lines count:`, processedObLines.length);
        if (processedObLines.length > 0) {
            console.log(`Track ${bane_id} first OB line sample:`, processedObLines[0]);
        }
        
        // Use the helper function to ensure image fields are consistent
        const track = ensureImageDataFields(result[0]);
        
        // Fetch holes data for this track (including GPS coordinates if available)
        try {
            const holesResult = await sql`
                SELECT
                    hull_id,
                    hull_nummer,
                    tee_latitude,
                    tee_longitude,
                    basket_latitude,
                    basket_longitude,
                    hole_par
                FROM mydb.hull
                WHERE bane_id_bane = ${bane_id}
                ORDER BY hull_nummer;
            `;

            if (holesResult && holesResult.length > 0) {
                track.holes = holesResult.map(hole => ({
                    holeId: hole.hull_id,
                    holeNumber: hole.hull_nummer,
                    teeLatitude: hole.tee_latitude,
                    teeLongitude: hole.tee_longitude,
                    basketLatitude: hole.basket_latitude,
                    basketLongitude: hole.basket_longitude,
                    par: hole.hole_par || 3
                }));
                console.log(`Track ${bane_id} includes ${track.holes.length} holes with GPS data`);
            } else {
                track.holes = [];
                console.log(`Track ${bane_id} has no holes data in database`);
            }
        } catch (holesError) {
            console.error(`Error fetching holes for track ${bane_id}:`, holesError);
            track.holes = []; // Empty array if error
        }

        return track;
    } catch (error) {
        console.error(`Error retrieving track with ID ${bane_id}:`, error);
        throw error;
    }
}

/**
 * Get alle baner i databasen
 * @returns {Promise<Array>} liste av alle baner
 */
async function getAllTracks() {
    try {
        console.log("Retrieving all tracks");
        
        const result = await sql`
            SELECT 
                bane_id,
                navn AS "trackName",
                beskrivelse AS "description",
                total_distance AS "totalDistance",
                total_par AS "totalPar",
                antall_hull AS "holeCount",
                markers,
                lines,
                ob_lines AS "obLines",
                rating,
                bane_bilde AS "imageData",
                klubb_id_klubb,
                opprettet_dato AS "created",
                oppdatert_dato AS "updated"
            FROM mydb.bane
            ORDER BY opprettet_dato DESC;
        `;
        
        // Process each track to include parsed OB lines and ensure image data consistency
        const processedTracks = result.map(track => {
            // Add OB lines processing similar to getTrackById
            let processedObLines = [];
            if (track.obLines) {
                try {
                    // If it's already an array, use it directly
                    if (Array.isArray(track.obLines)) {
                        processedObLines = track.obLines;
                    } 
                    // If it's a string, parse it
                    else if (typeof track.obLines === 'string') {
                        processedObLines = JSON.parse(track.obLines);
                    } 
                    // Handle PostgreSQL JSONB format
                    else if (typeof track.obLines === 'object') {
                        processedObLines = track.obLines;
                    }
                } catch (e) {
                    console.error(`Error processing OB lines for track ${track.bane_id}:`, e);
                    processedObLines = [];
                }
            }
            
            // Set both fields for consistency
            track.obLines = processedObLines;
            track.ob_lines = processedObLines;
            
            // Log OB lines count for debugging
            console.log(`Track ${track.bane_id || track.trackName} OB lines count: ${processedObLines.length}`);
            
            // Also ensure image data fields are consistent
            return ensureImageDataFields(track);
        });
        
        console.log(`Retrieved ${processedTracks.length} tracks, ${processedTracks.filter(t => t.imageData).length} with images, ${processedTracks.filter(t => t.obLines && t.obLines.length > 0).length} with OB lines`);
        return processedTracks;
    } catch (error) {
        console.error("Error retrieving all tracks:", error);
        throw error;
    }
} 

/**
 * oppdaterer en eksisterende bane
 * @param {number} bane_id - bane iden som skal oppdateres
 * @param {Object} trackData - den nye bane dataen
 * @returns {Promise<Object>} den oppdaterte banen
 */
async function updateTrack(bane_id, trackData) {
    try {
        // console.log(`Updating track with ID: ${bane_id}`); <- debuging redskap
        
        // først sikkre at bildefilene er konsistente
        const preparedTrackData = ensureImageDataFields(trackData);
        
        // field sikkrer at vi bruker riktig bildedata feltet
        const imageData = preparedTrackData.imageData || preparedTrackData.bane_bilde || null;
        
       /*   if (imageData) {
            console.log(`Track update includes image data (length: ${imageData.length})`);
            
           if (typeof imageData === 'string' && imageData.startsWith('data:image/')) {
                console.log("✅ Image data format appears valid");
            } else {
                console.warn("⚠️ Image data format may be invalid");
                if (typeof imageData === 'string') {
                    console.warn("First 30 chars:", imageData.substring(0, 30));
                } else {
                    console.warn("Image data type:", typeof imageData);
                }
            }
        } else {
            console.log("Track update has no image data");
        }  dette er for debugging */
        
        const result = await sql`
            UPDATE mydb.bane
            SET 
                navn = ${preparedTrackData.trackName},
                beskrivelse = ${preparedTrackData.description || ''},
                total_distance = ${preparedTrackData.totalDistance || 0},
                total_par = ${preparedTrackData.totalPar || 0},
                antall_hull = ${preparedTrackData.holeCount || 0},
                markers = ${JSON.stringify(preparedTrackData.markers || [])}::jsonb,
                lines = ${JSON.stringify(preparedTrackData.lines || [])}::jsonb,
                ob_lines = ${JSON.stringify(preparedTrackData.obLines || [])}::jsonb,
                rating = ${preparedTrackData.rating || 0},
                bane_bilde = ${imageData},
                oppdatert_dato = NOW()
            WHERE bane_id = ${bane_id}
            RETURNING bane_id;
        `;
        
        if (!result[0]) {
            throw new Error('Track not found');
        }
        
        // returnerer den oppdaterte banen med konsistente bilde felt
        const updatedTrack = await getTrackById(bane_id);
        return updatedTrack;
    } catch (error) {
        console.error(`Error updating track with ID ${bane_id}:`, error);
        throw error;
    }
}

async function deleteTrack(bane_id) {
    try {
        // bruker transaksjon for å slette banen og dens relaterte data som sørger for at alt eller ingen ting blir slettet
        const result = await sql.begin(async sql => {
            // First check if track exists
            const exists = await sql`
                SELECT bane_id FROM mydb.bane WHERE bane_id = ${bane_id}
            `;

            if (!exists || exists.length === 0) {
                console.log(`No track found with ID: ${bane_id}`);
                return null;
            }

            // Get track data before deletion (for logging/debugging purposes)
            const trackInfo = await sql`
                SELECT 
                    navn, 
                    markers, 
                    lines, 
                    ob_lines
                FROM mydb.bane 
                WHERE bane_id = ${bane_id}
            `;
            
            const obLinesCount = trackInfo[0]?.ob_lines ? 
                (Array.isArray(JSON.parse(trackInfo[0].ob_lines)) ? 
                 JSON.parse(trackInfo[0].ob_lines).length : 0) : 0;
            
            console.log(`Deleting track "${trackInfo[0]?.navn || 'unnamed'}" with ${obLinesCount} OB lines`);

            // Delete in a specific order to handle dependencies
            // 1. Delete game_players entries related to game_sessions for this track
            await sql`
                DELETE FROM mydb.game_players
                WHERE session_id IN (
                    SELECT session_id 
                    FROM mydb.game_sessions 
                    WHERE bane_id = ${bane_id}
                );
            `;

            // 2. Delete game_sessions
            await sql`
                DELETE FROM mydb.game_sessions 
                WHERE bane_id = ${bane_id};
            `;

            // 3. Delete pointcards
            await sql`
                DELETE FROM mydb.pointcard 
                WHERE bane_id = ${bane_id};
            `;
            
            // 4. Delete any track images
            await sql`
                DELETE FROM mydb.track_images
                WHERE bane_id = ${bane_id};
            `;

            // 5. Finally delete the track (which also removes markers, lines and ob_lines since they're stored in the track row)
            const deleted = await sql`
                DELETE FROM mydb.bane
                WHERE bane_id = ${bane_id}
                RETURNING bane_id;
            `;

            return deleted[0];
        });

        // console.log(`Successfully deleted track with ID: ${bane_id}`); <- debuging redskap
        return result;

    } catch (error) {
        console.error(`Error deleting track ${bane_id}:`, error);
        // ser etter spesefikk feil
        if (error.code === '23503') { // Fkey feil
            console.error('Foreign key constraint violation. Check all relations.');
        }
        throw error;
    }
}

//for å hente alle spillere i databasen, om de er satt til synlig. Marcus jobbe her
async function getAllSpillere() {
    try {
        const result = await sql`
            WITH player_stats AS (
                SELECT 
                    p.spiller_navn,
                    COUNT(DISTINCT p.pointcard_id) as total_rounds,
                    SUM(p.total_score) as total_score,
                    ROUND(AVG(p.total_score), 1) as average_score_per_round,
                    MIN(p.total_score) as best_round,
                    MAX(p.total_score) as worst_round,
                    ROUND(AVG(p.total_score / 18), 1) as average_score_per_hole,
                    MAX(p.dato) as last_played
                FROM mydb.pointcard p
                GROUP BY p.spiller_navn
            )
            SELECT 
                b.epost,
                b.brukernavn,
                b.fnavn,
                b.enavn,
                COALESCE(ps.total_rounds, 0) as total_rounds,
                COALESCE(ps.total_score, 0) as total_score,
                COALESCE(ps.average_score_per_round, 0) as average_score_per_round,
                COALESCE(ps.average_score_per_hole, 0) as average_score_per_hole,
                COALESCE(ps.best_round, 0) as best_round,
                COALESCE(ps.worst_round, 0) as worst_round,
                ps.last_played
            FROM 
                mydb.bruker b
            LEFT JOIN 
                player_stats ps ON b.epost = ps.spiller_navn
            WHERE 
                (b.visible = true OR b.visible IS NULL)
            ORDER BY 
                b.brukernavn;
        `;

        return result.map(row => ({
            epost: row.epost,
            brukernavn: row.brukernavn,
            fnavn: row.fnavn,
            enavn: row.enavn,
            statistics: {
                total_rounds: parseInt(row.total_rounds),
                total_score: parseInt(row.total_score),
                average_score_per_round: parseFloat(row.average_score_per_round),
                average_score_per_hole: parseFloat(row.average_score_per_hole),
                best_round: parseInt(row.best_round),
                worst_round: parseInt(row.worst_round),
                last_played: row.last_played ? new Date(row.last_played).toLocaleDateString('nb-NO') : null
            }
        }));
    } catch (error) {
        console.error('Error fetching players:', error);
        throw error;
    }
}

// denne funksjonen oppdaterer brukerens rolle i databasen. denne blir brukt av admin og klubbadmin
async function updateUserRole(email, role, klubb_id = null) {
    // om rollen er klubbadmin, oppdaterer vi også klubb_id_klubb feltet så klubbadmin er tilknyttet en klubb
    if (role === 'klubbadmin' && klubb_id) {
        const result = await sql`
            UPDATE mydb.bruker
            SET role = ${role}, klubb_id_klubb = ${klubb_id}
            WHERE epost = ${email}
            RETURNING epost, role, klubb_id_klubb;
        `;
        return result[0];
    } else {
        // om ikke klubbadmin, eller ingen klubb spesifisert, oppdaterer vi bare rollen
        const result = await sql`
            UPDATE mydb.bruker
            SET role = ${role}
            WHERE epost = ${email}
            RETURNING epost, role;
        `;
        return result[0];
    }
}

// denne funksjonen henter alle brukere i databasen, inkludert klubbadmin og admin om de er satt til synlig, den sorterer også brukerne etter rolle og epost
async function getAllUsers() {
    try {
        console.log('Executing SQL query to get all users');
        const result = await sql`
            SELECT 
                b.epost AS email, 
                b.brukernavn, 
                b.role,
                b.klubb_id_klubb AS club_id,
                k.klubbnavn AS club_name
            FROM mydb.bruker b
            LEFT JOIN mydb.klubb k ON b.klubb_id_klubb = k.id_klubb
            ORDER BY 
                CASE b.role 
                    WHEN 'admin' THEN 1
                    WHEN 'klubbadmin' THEN 2
                    ELSE 3 
                END,
                b.epost;
        `;
        console.log('SQL query successful, returned', result ? result.length : 0, 'users');
        return result || [];
    } catch (error) {
        // console.error('Database error in getAllUsers:', error.message); <- debuging redskap
        // console.error('Error stack:', error.stack);
        // returnerer tom liste om det er en feil
        return [];
    }
}

// søker etter brukere i databasen, det trenger ikke være 1:1, bruker % x % så man kan søke etter deler av epost, brukernavn eller navn
async function searchUsers(searchTerm) {
    try {
        console.log('Executing SQL query to search users with pattern:', searchTerm);
        const result = await sql`
            SELECT
                epost,
                brukernavn,
                fnavn,
                enavn,
                role
            FROM mydb.bruker
            WHERE
                epost ILIKE ${'%' + searchTerm + '%'} OR
                brukernavn ILIKE ${'%' + searchTerm + '%'} OR
                CONCAT(fnavn, ' ', enavn) ILIKE ${'%' + searchTerm + '%'}
            ORDER BY
                CASE WHEN epost = ${searchTerm} THEN 1
                     WHEN brukernavn = ${searchTerm} THEN 2
                     ELSE 3 END,
                brukernavn,
                epost
            LIMIT 10;
        `;
        console.log('SQL search query successful, returned', result ? result.length : 0, 'users');
        return result || [];
    } catch (error) {
        console.error('Database error in searchUsers:', error.message);
        console.error('Error stack:', error.stack);
        return [];
    }
}

async function updateClubInfo(klubbId, newInfo) {
    try {
        console.log("Updating club info for:", klubbId);
        
        // setter direkte inn i databasen med sql template string
        const result = await sql`
            UPDATE mydb.klubb 
            SET klubbnavn = ${newInfo.klubbnavn || null},
                sted = ${newInfo.sted || null}, 
                klubbeskrivelse = ${newInfo.beskrivelse || null},
                kontakt_email = ${newInfo.kontakt_email || null},
                kontakt_telefon = ${newInfo.kontakt_telefon || null}
                ${newInfo.klubb_bilde ? sql`, klubb_bilde = ${newInfo.klubb_bilde}` : sql``}
            WHERE id_klubb = ${klubbId}
            RETURNING id_klubb, klubbnavn, sted, klubbeskrivelse as beskrivelse, kontakt_email, kontakt_telefon, klubb_bilde
        `;
        
        // console.log("Club update result:", result[0]); <- debuging redskap
        return result[0];
    } catch (error) {
        console.error("Error in updateClubInfo:", error);
        throw error;
    }
}

// Sikrer at bildedata er kompatible mellom frontend and backend
const ensureImageDataFields = (track) => {
    try {
        if (!track) return track;
        
        // Om banen har imageData men ikke bane_bilde, kopierer vi det
        if (track.imageData && !track.bane_bilde) {
            track.bane_bilde = track.imageData;
            // console.log(`Track ${track.id || track.bane_id}: Copied imageData to bane_bilde`);
        }
        // motsatt vei
        else if (track.bane_bilde && !track.imageData) {
            track.imageData = track.bane_bilde;
            // console.log(`Track ${track.id || track.bane_id}: Copied bane_bilde to imageData`);
        }
        
        return track;
    } catch (error) {
        console.error("Feil ved sikring av bildedata felt:", error);
        return track; // returnerer originalen om det er en feil
    }
};

/*
* Denne funksjonen oppretter en ny spilløkt i databasen.
* Den tar inn sessionId, bane_id og hostEmail som parametere.
* Utviklet av: Martin Hansen
*/
async function createGameSession(sessionId, bane_id, hostEmail) {
    try {
        const result = await sql`
            INSERT INTO mydb.game_sessions (session_id, bane_id, host_email, created_at)
            VALUES (${sessionId}, ${bane_id}, ${hostEmail}, NOW())
            RETURNING *
        `;
        return result[0];
    } catch (err) {
        console.error('Database error creating game session:', err);
        throw err;
    }
}

/*
* Denne funksjonen henter en spilløkt fra databasen basert på sessionId.
* Den tar inn sessionId som parameter og returnerer spilløktens detaljer.
* Utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
*/
async function getGameSession(sessionId) {
    try {
        const result = await sql`
            SELECT 
                gs.session_id, 
                gs.bane_id, 
                b.navn AS bane_navn,
                gs.host_email, 
                gs.created_at
            FROM mydb.game_sessions gs
            JOIN mydb.bane b ON gs.bane_id = b.bane_id
            WHERE gs.session_id = ${sessionId}
        `;
        return result[0];
    } catch (err) {
        console.error('Database error getting game session:', err);
        throw err;
    }
}

/*
* Denne funksjonen legger til en spiller i en spilløkt.
* Den tar inn sessionId og playerEmail som parametere.
* Utviklet av: Martin Hansen
*/
async function addPlayerToSession(sessionId, playerEmail) {
    try {
        
        const existingPlayer = await sql`
            SELECT * FROM mydb.game_players
            WHERE session_id = ${sessionId} AND player_email = ${playerEmail}
        `;
        
        
        if (existingPlayer.length > 0) {
            console.log(`Player ${playerEmail} is already in session ${sessionId}`);
            return existingPlayer[0];
        }
        
        
        const result = await sql`
            INSERT INTO mydb.game_players (session_id, player_email, joined_at)
            VALUES (${sessionId}, ${playerEmail}, NOW())
            RETURNING *
        `;
        return result[0];
    } catch (err) {
        console.error('Database error adding player to session:', err);
        throw err;
    }
}

/*
* Denne funksjonen henter spillerne i en spilløkt basert på sessionId.
* Den tar inn sessionId som parameter og returnerer en liste med spillere.
* Utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
*/
async function getSessionPlayers(sessionId) {
    try {
        const result = await sql`
            SELECT 
                gp.player_email, 
                gp.joined_at, 
                (gp.player_email = gs.host_email) AS is_host
            FROM mydb.game_players gp
            JOIN mydb.game_sessions gs ON gp.session_id = gs.session_id
            WHERE gp.session_id = ${sessionId}
            ORDER BY gp.joined_at ASC
        `;
        return result;
    } catch (err) {
        console.error('Database error getting session players:', err);
        throw err;
    }
}

/*
* Denne funksjonen sjekker om det finnes en eksisterende venneforespørsel eller vennskap
* mellom to brukere (i begge retninger).
* Returnerer forespørselen hvis den finnes, ellers null.
*/
async function checkExistingFriendship(userEmail, friendEmail) {
    const result = await sql`
        SELECT id, user_email, friend_email, status
        FROM mydb.friends
        WHERE (user_email = ${userEmail} AND friend_email = ${friendEmail})
           OR (user_email = ${friendEmail} AND friend_email = ${userEmail})
        LIMIT 1;
    `;
    return result.length > 0 ? result[0] : null;
}

// venne funksjon, sender venneforespørsel til en bruker
async function sendFriendRequest(userEmail, friendEmail) {
    const result = await sql`
        INSERT INTO mydb.friends (user_email, friend_email, status)
        VALUES (${userEmail}, ${friendEmail}, 'pending')
        RETURNING id, user_email, friend_email, status, created_at;
    `;
    return result[0];
}

/*
* Denne funksjonen henter venneforespørslene for en bruker.
* Den tar inn brukerens e-postadresse som parameter og returnerer en liste med venneforespørslene.
* utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
*/
async function getFriendRequests(userEmail) {

    if (!userEmail) {
        console.error('getFriendRequests called with undefined userEmail');
        throw new Error('User email is required');
    }

    try {
        console.log('Fetching friend requests for user:', userEmail);
        const result = await sql`
            SELECT f.id, f.user_email, f.status, f.created_at,
                   b.brukernavn as friend_username
            FROM mydb.friends f
            JOIN mydb.bruker b ON f.user_email = b.epost
            WHERE f.friend_email = ${userEmail} AND f.status = 'pending'
            ORDER BY f.created_at DESC;
        `;
        console.log(`Found ${result.length} friend requests`);
        return result;
    } catch (error) {
        console.error('Database error in getFriendRequests:', error);
        throw error;
    }
}

/*
* Denne funksjonen henter venneforespørsler som brukeren har sendt.
* Returnerer liste med pending requests sent av brukeren.
*/
async function getPendingRequestsSentByUser(userEmail) {
    const result = await sql`
        SELECT id, user_email, friend_email, status, created_at
        FROM mydb.friends
        WHERE user_email = ${userEmail} AND status = 'pending'
        ORDER BY created_at DESC;
    `;
    return result;
}

/*
* Denne funksjonen aksepterer en venneforespørsel.
* Den tar inn ID-en til forespørselen og brukerens e-postadresse som parametere.
* Utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
*/
async function acceptFriendRequest(requestId, userEmail) {
    const result = await sql`
        UPDATE mydb.friends
        SET status = 'accepted', updated_at = NOW()
        WHERE id = ${requestId} AND friend_email = ${userEmail}
        RETURNING id, user_email, friend_email, status, updated_at;
    `;

    if (result.length === 0) {
        return null;
    }


    await sql`
        INSERT INTO mydb.friends (user_email, friend_email, status)
        VALUES (${result[0].friend_email}, ${result[0].user_email}, 'accepted')
        ON CONFLICT (user_email, friend_email)
        DO UPDATE SET status = 'accepted', updated_at = NOW();
    `;

    return result[0];
}

/*
* Denne funksjonen avslår en venneforespørsel.
* Den tar inn ID-en til forespørselen og brukerens e-postadresse som parametere.
* Utviklet av: Martin Hansen
*/
async function declineFriendRequest(requestId, userEmail) {
    const result = await sql`
        DELETE FROM mydb.friends
        WHERE id = ${requestId} AND friend_email = ${userEmail}
        RETURNING id;
    `;
    return result[0];
}

/*
* Denne funksjonen henter vennene til en bruker.
* Den tar inn brukerens e-postadresse som parameter og returnerer en liste med vennene.
* Utviklet av: Martin Hansen
*/
async function getFriends(userEmail) {
    const result = await sql`
        SELECT
            f.id,
            f.user_email,
            f.friend_email,
            f.status,
            f.created_at,
            b.brukernavn as friend_username,
            b.fnavn as friend_first_name,
            b.enavn as friend_last_name,
            b.last_active,
            COALESCE(game_stats.games_played, 0) as games_played,
            game_stats.last_played,
            COALESCE(unread_count.unread_messages, 0) as unread_messages,
            CASE
                WHEN b.last_active IS NULL THEN 'offline'
                WHEN b.last_active > NOW() - INTERVAL '3 minutes' THEN 'online'
                ELSE 'offline'
            END as online_status
        FROM mydb.friends f
        JOIN mydb.bruker b ON f.friend_email = b.epost
        LEFT JOIN (
            SELECT
                spiller_navn,
                COUNT(*) as games_played,
                TO_CHAR(MAX(dato) + INTERVAL '2 hours', 'YYYY-MM-DD"T"HH24:MI:SS') as last_played
            FROM mydb.pointcard
            GROUP BY spiller_navn
        ) game_stats ON f.friend_email = game_stats.spiller_navn
        LEFT JOIN (
            SELECT
                sender_email,
                COUNT(*) as unread_messages
            FROM mydb.messages
            WHERE receiver_email = ${userEmail} AND is_read = FALSE
            GROUP BY sender_email
        ) unread_count ON f.friend_email = unread_count.sender_email
        WHERE f.user_email = ${userEmail} AND f.status = 'accepted'
        ORDER BY
            CASE
                WHEN b.last_active > NOW() - INTERVAL '3 minutes' THEN 0
                ELSE 1
            END,
            b.brukernavn;
    `;
    return result;
}

/*
* This function updates the user's last active timestamp to track online status.
* Users are considered online if they were active within the last 5 minutes.
*/
async function updateLastActive(userEmail) {
    try {
        const result = await sql`
            UPDATE mydb.bruker
            SET last_active = CURRENT_TIMESTAMP
            WHERE epost = ${userEmail}
            RETURNING epost, last_active;
        `;
        return result[0];
    } catch (error) {
        console.error('Error updating last active:', error);
        throw error;
    }
}

/*
* Denne funksjonen fjerner en venn fra brukerens venneliste.
* Den tar inn brukerens e-postadresse og vennens e-postadresse som parametere.
* Utviklet av: Martin Hansen
*/
async function removeFriend(userEmail, friendEmail) {
    const result = await sql`
        DELETE FROM mydb.friends
        WHERE (user_email = ${userEmail} AND friend_email = ${friendEmail})
           OR (user_email = ${friendEmail} AND friend_email = ${userEmail})
        RETURNING id;
    `;
    return result;
}

/*
* Denne funksjonen sender en melding mellom to brukere.
* Den tar inn avsenderens e-postadresse, mottakerens e-postadresse og innholdet i meldingen som parametere.
* Utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
*/
async function sendMessage(senderEmail, receiverEmail, content) {
    const result = await sql`
        INSERT INTO mydb.messages (sender_email, receiver_email, content)
        VALUES (${senderEmail}, ${receiverEmail}, ${content})
        RETURNING message_id, sender_email, receiver_email, content, created_at;
    `;
    return result[0];
}

/*
* Denne funksjonen henter samtaler for en bruker.
* Den tar inn brukerens e-postadresse som parameter og returnerer en liste med samtalene.
* Utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
* Denne funksjonen bruker en CTE (Common Table Expression) for å hente samtalene.
*/
async function getConversations(userEmail) {
    try {
      const conversations = await sql`
        WITH conversations AS (
          SELECT
            CASE
              WHEN sender_email = ${userEmail} THEN receiver_email
              ELSE sender_email
            END as contact_email,
            MAX(created_at) as last_message_time,
            COUNT(CASE WHEN receiver_email = ${userEmail} AND is_read = FALSE THEN 1 END) as unread_count,
            (SELECT content FROM mydb.messages
             WHERE (sender_email = ${userEmail} AND receiver_email = CASE
                                                                    WHEN sender_email = ${userEmail} THEN receiver_email
                                                                    ELSE sender_email
                                                                  END)
                OR (receiver_email = ${userEmail} AND sender_email = CASE
                                                                    WHEN sender_email = ${userEmail} THEN receiver_email
                                                                    ELSE sender_email
                                                                    END)
             ORDER BY created_at DESC
             LIMIT 1) as last_message
          FROM mydb.messages
          WHERE sender_email = ${userEmail} OR receiver_email = ${userEmail}
          GROUP BY contact_email
        )
        SELECT
          c.contact_email,
          c.last_message_time,
          c.unread_count,
          c.last_message,
          b.brukernavn as contact_username
        FROM conversations c
        JOIN mydb.bruker b ON c.contact_email = b.epost
        ORDER BY c.last_message_time DESC
      `;
      console.log(`Found ${conversations.length} conversations with ${conversations.reduce((sum, c) => sum + (c.unread_count || 0), 0)} unread messages`);
      return conversations;
    } catch (error) {
      console.error('Feil ved henting av samtaler:', error);
      throw error;
    }
  }

/*
* Denne funksjonen henter meldinger mellom to brukere.
* Den tar inn avsenderens e-postadresse og mottakerens e-postadresse som parametere.
* Den returnerer en liste med meldingene mellom de to brukerne.
* Utviklet av: Martin Hansen
* Ai-assistanse: github copilot.
*/
async function getMessages(userEmail, contactEmail) {
    const result = await sql`
        SELECT m.message_id, m.sender_email, m.receiver_email, m.content,
               m.is_read, m.created_at,
               b.brukernavn as sender_username
        FROM mydb.messages m
        JOIN mydb.bruker b ON m.sender_email = b.epost
        WHERE (m.sender_email = ${userEmail} AND m.receiver_email = ${contactEmail})
           OR (m.sender_email = ${contactEmail} AND m.receiver_email = ${userEmail})
        ORDER BY m.created_at ASC;
    `;
    // Mark messages as read
    await sql`
        UPDATE mydb.messages
        SET is_read = true
        WHERE sender_email = ${contactEmail} AND receiver_email = ${userEmail} AND is_read = false;
    `;

    return result;
}

// skjema for å lage en ny klubb, og lagrer den i databasen for godkjenning av admin
async function createClubRequest(email, klubbnavn, sted, beskrivelse) {
    try {
        const result = await sql`
            INSERT INTO mydb.klubb (kontakt_email, klubbnavn, sted, klubbeskrivelse, status, opprettet_dato)
            VALUES (${email}, ${klubbnavn}, ${sted}, ${beskrivelse}, 'pending', NOW())
            RETURNING id_klubb;
        `;

        return result[0];
    } catch (error) {
        console.error('Error creating club request:', error);
        throw error;
    }
}

/*
* Denne funksjonen henter alle ventende klubbforespørslene.
* Brukes av admin til å se alle forespørsler som venter på godkjenning.
*/
async function getPendingClubRequests() {
    const result = await sql`
        SELECT id_klubb, klubbnavn, sted, klubbeskrivelse, kontakt_email, status, opprettet_dato
        FROM mydb.klubb
        WHERE status = 'pending'
        ORDER BY opprettet_dato DESC;
    `;
    return result;
}

/*
* Denne funksjonen godkjenner en klubbforespørsel.
* Den tar inn ID-en til forespørselen som parameter.
*/
async function approveClubRequest(requestId) {
    const result = await sql`
        UPDATE mydb.klubb
        SET status = 'approved'
        WHERE id_klubb = ${requestId}
        RETURNING id_klubb, klubbnavn, sted, klubbeskrivelse, status;
    `;
    return result[0];
}

/*
* Denne funksjonen avslår en klubbforespørsel.
* Den tar inn ID-en til forespørselen som parameter.
*/
async function rejectClubRequest(requestId) {
    const result = await sql`
        DELETE FROM mydb.klubb
        WHERE id_klubb = ${requestId}
        RETURNING id_klubb;
    `;
    return result[0];
}

/*
* Denne funksjonen oppretter en ny forespørsel om å bli med i en klubb.
* Den tar inn brukerens e-postadresse, klubb-ID og valgfri melding som parametere.
*/
async function createClubJoinRequest(userEmail, klubId, message = null) {
    const result = await sql`
        INSERT INTO mydb.club_join_requests (user_email, klub_id, message, status, request_date)
        VALUES (${userEmail}, ${klubId}, ${message}, 'pending', NOW())
        RETURNING id;
    `;
    return result[0];
}

/*
* Denne funksjonen henter ventende forespørslene om å bli med i en klubb.
* Den tar inn klubb-ID som parameter og returnerer en liste med forespørslene for den klubben.
*/
async function getPendingClubJoinRequests(klubId) {
    const result = await sql`
        SELECT
            r.id,
            r.user_email,
            r.klub_id,
            r.message,
            r.status,
            r.request_date,
            b.brukernavn,
            k.klubbnavn
        FROM mydb.club_join_requests r
        JOIN mydb.klubb k ON r.klub_id = k.id_klubb
        LEFT JOIN mydb.bruker b ON r.user_email = b.epost
        WHERE r.klub_id = ${klubId} AND r.status = 'pending'
        ORDER BY r.request_date DESC;
    `;
    return result;
}

/*
* Denne funksjonen godkjenner en forespørsel om å bli med i en klubb.
* Den tar inn ID-en til forespørselen som parameter.
* Oppdaterer også brukerens klubb_id_klubb felt.
*/
async function approveClubJoinRequest(requestId) {
    // First get the request details
    const request = await sql`
        SELECT user_email, klub_id FROM mydb.club_join_requests
        WHERE id = ${requestId}
    `;

    if (!request || request.length === 0) {
        throw new Error('Join request not found');
    }

    const { user_email, klub_id } = request[0];

    // Update the user's club membership
    await sql`
        UPDATE mydb.bruker
        SET klubb_id_klubb = ${klub_id}
        WHERE epost = ${user_email}
    `;

    // Update the request status
    const result = await sql`
        UPDATE mydb.club_join_requests
        SET status = 'approved'
        WHERE id = ${requestId}
        RETURNING id, user_email, klub_id, status;
    `;
    return result[0];
}

/*
* Denne funksjonen avslår en forespørsel om å bli med i en klubb.
* Den tar inn ID-en til forespørselen som parameter.
*/
async function rejectClubJoinRequest(requestId) {
    const result = await sql`
        DELETE FROM mydb.club_join_requests
        WHERE id = ${requestId}
        RETURNING id;
    `;
    return result[0];
}

/*
* Denne funksjonen nedgraderer en klubbadmin til vanlig medlem.
* Den tar inn brukerens e-postadresse som parameter.
*/
async function demoteKlubbAdmin(email) {
    const result = await sql`
        UPDATE mydb.bruker
        SET role = 'member', klubb_id_klubb = NULL
        WHERE epost = ${email}
        RETURNING epost, role;
    `;
    return result[0];
}

/*
* Denne funksjonen henter klubben til en admin basert på e-postadressen deres.
* Den tar inn brukerens e-postadresse som parameter.
*/
async function getClubByAdminEmail(email) {
    const result = await sql`
        SELECT k.*
        FROM mydb.klubb k
        JOIN mydb.bruker b ON k.id_klubb = b.klubb_id_klubb
        WHERE b.epost = ${email} AND b.role = 'klubbadmin';
    `;
    return result[0];
}

/*
* Sjekker om en bruker er klubbadmin for en bestemt klubb.
* Tar inn brukerens e-postadresse og klubb-ID som parametere.
*/
async function isClubAdmin(userEmail, klubbId) {
    const result = await sql`
        SELECT b.*
        FROM mydb.bruker b
        WHERE b.epost = ${userEmail} AND b.role = 'klubbadmin' AND b.klubb_id_klubb = ${klubbId};
    `;
    return result.length > 0;
}

/*
* Henter medlemmene i en klubb.
* Tar inn klubb-ID som parameter.
*/
async function getClubMembers(klubbId) {
    const result = await sql`
        SELECT
            b.epost,
            b.brukernavn,
            b.fnavn,
            b.enavn,
            b.role
        FROM mydb.bruker b
        WHERE b.klubb_id_klubb = ${klubbId}
        ORDER BY b.brukernavn;
    `;
    return result;
}

/*
* Fjerner et medlem fra en klubb.
* Tar inn klubb-ID og brukerens e-postadresse som parametere.
*/
async function removeClubMember(klubbId, userEmail) {
    const result = await sql`
        UPDATE mydb.bruker
        SET klubb_id_klubb = NULL
        WHERE epost = ${userEmail} AND klubb_id_klubb = ${klubbId}
        RETURNING epost, klubb_id_klubb;
    `;
    return result[0];
}

/*
* Denne funksjonen henter klubbadmin for en spesifikk klubb.
* Den tar inn klubb-ID som parameter.
*/
async function getKlubbForAdmin(klubbId) {
    const result = await sql`
        SELECT b.*
        FROM mydb.bruker b
        WHERE b.klubb_id_klubb = ${klubbId} AND b.role = 'klubbadmin';
    `;
    return result[0];
}

// Alias for isClubAdmin (for backwards compatibility)
async function isClubMember(email, clubId) {
    return isClubAdmin(email, clubId);
}

// henter alle klubber i databasen
async function getKlubber(email = null) {
    if (email) {
        // Return clubs where the user is a member
        const result = await sql`
            SELECT k.*
            FROM mydb.klubb k
            JOIN mydb.bruker b ON k.id_klubb = b.klubb_id_klubb
            WHERE b.epost = ${email}
        `;
        return result;
    } else {
        // Return all clubs
        const result = await sql`
            SELECT * FROM mydb.klubb
            ORDER BY klubbnavn
        `;
        return result;
    }
}

// henter alle klubber med medlemstall
async function getAllClubs() {
    try {
        console.log("Retrieving all clubs");

        const result = await sql`
            SELECT
                id_klubb,
                klubbnavn,
                sted,
                klubbeskrivelse,
                kontakt_email,
                kontakt_telefon,
                (SELECT COUNT(*) FROM mydb.bruker WHERE klubb_id_klubb = klubb.id_klubb) as member_count
            FROM mydb.klubb
            ORDER BY klubbnavn
        `;

        return result;
    } catch (error) {
        console.error("Error retrieving all clubs:", error);
        throw error;
    }
}

// henter en spesifikk klubb
async function getClubById(clubId) {
    const result = await sql`
        SELECT
            id_klubb,
            klubbnavn,
            sted,
            klubbeskrivelse,
            kontakt_email,
            kontakt_telefon
        FROM mydb.klubb
        WHERE id_klubb = ${clubId}
    `;
    return result[0];
}

// Alias for getClubById (Norwegian naming convention)
async function getKlubbById(klubbId) {
    return getClubById(klubbId);
}

// Check om bruker er klubbadmin i en spesifikk klubb
async function isTrackOwnedByUserClub(userEmail, trackId) {
    try {
        console.log(`Checking if track ${trackId} is owned by club of user ${userEmail}`);

        // Get the user's club ID
        const user = await sql`
            SELECT klubb_id_klubb
            FROM mydb.bruker
            WHERE epost = ${userEmail} AND role = 'klubbadmin';
        `;

        if (!user || user.length === 0 || !user[0].klubb_id_klubb) {
            console.log(`User ${userEmail} is not a klubbadmin or has no associated club`);
            return false;
        }

        const userClubId = Number(user[0].klubb_id_klubb);
        console.log(`User's club ID: ${userClubId} (type: ${typeof userClubId})`);

        // Check if track belongs to user's club
        const track = await sql`
            SELECT bane_id, klubb_id_klubb
            FROM mydb.bane
            WHERE bane_id = ${trackId};
        `;

        if (!track || track.length === 0) {
            console.log(`Track ${trackId} not found`);
            return false;
        }

        const trackClubId = Number(track[0].klubb_id_klubb);
        console.log(`Track's club ID: ${trackClubId} (type: ${typeof trackClubId})`);

        // Compare as numbers to avoid type mismatch
        const hasPermission = trackClubId === userClubId;
        console.log(`Permission check result: ${hasPermission}`);

        return hasPermission;
    } catch (error) {
        console.error('Error in isTrackOwnedByUserClub:', error);
        return false;
    }
}

// Club announcements (posts) functions
async function addClubAnnouncement(klubId, content, authorEmail) {
    const result = await sql`
        INSERT INTO mydb.club_announcements (klubb_id, content, author_email, created_at)
        VALUES (${klubId}, ${content}, ${authorEmail}, NOW())
        RETURNING id, klubb_id, content, author_email, created_at;
    `;
    return result[0];
}

async function getClubAnnouncements(klubId, limit = null) {
    const limitClause = limit ? sql`LIMIT ${limit}` : sql``;
    const result = await sql`
        SELECT
            a.id,
            a.klubb_id,
            a.content,
            a.author_email,
            a.created_at,
            b.brukernavn as author_name
        FROM mydb.club_announcements a
        LEFT JOIN mydb.bruker b ON a.author_email = b.epost
        WHERE a.klubb_id = ${klubId}
        ORDER BY a.created_at DESC
        ${limitClause};
    `;
    return result;
}

async function getClubAnnouncement(announcementId) {
    const result = await sql`
        SELECT
            a.id,
            a.klubb_id,
            a.content,
            a.author_email,
            a.created_at,
            b.brukernavn as author_name
        FROM mydb.club_announcements a
        LEFT JOIN mydb.bruker b ON a.author_email = b.epost
        WHERE a.id = ${announcementId};
    `;
    return result[0];
}

async function deleteClubAnnouncement(announcementId) {
    const result = await sql`
        DELETE FROM mydb.club_announcements
        WHERE id = ${announcementId}
        RETURNING id;
    `;
    return result[0];
}

// Club events functions
async function addClubEvent(klubId, title, description, eventDate, authorEmail) {
    const result = await sql`
        INSERT INTO mydb.club_events (klubb_id, title, description, event_date, author_email, created_at)
        VALUES (${klubId}, ${title}, ${description}, ${eventDate}, ${authorEmail}, NOW())
        RETURNING id, klubb_id, title, description, event_date, author_email, created_at;
    `;
    const event = result[0];
    // Add participant_count (will be 0 for new events)
    event.participant_count = 0;
    return event;
}

async function getClubEvents(klubId, limit = null) {
    const limitClause = limit ? sql`LIMIT ${limit}` : sql``;
    const result = await sql`
        SELECT
            e.id,
            e.klubb_id,
            e.title,
            e.description,
            e.event_date,
            e.author_email,
            e.created_at,
            b.brukernavn as author_name,
            (SELECT COUNT(*) FROM mydb.event_participants WHERE event_id = e.id) as participant_count
        FROM mydb.club_events e
        LEFT JOIN mydb.bruker b ON e.author_email = b.epost
        WHERE e.klubb_id = ${klubId}
        ORDER BY e.event_date ASC
        ${limitClause};
    `;
    return result;
}

async function getClubEvent(eventId) {
    const result = await sql`
        SELECT
            e.id,
            e.klubb_id,
            e.title,
            e.description,
            e.event_date,
            e.author_email,
            e.created_at,
            b.brukernavn as author_name,
            (SELECT COUNT(*) FROM mydb.event_participants WHERE event_id = e.id) as participant_count
        FROM mydb.club_events e
        LEFT JOIN mydb.bruker b ON e.author_email = b.epost
        WHERE e.id = ${eventId};
    `;
    return result[0];
}

async function deleteClubEvent(eventId) {
    const result = await sql`
        DELETE FROM mydb.club_events
        WHERE id = ${eventId}
        RETURNING id;
    `;
    return result[0];
}

// Event participants functions
async function joinEvent(eventId, userEmail) {
    const result = await sql`
        INSERT INTO mydb.event_participants (event_id, user_email, joined_at)
        VALUES (${eventId}, ${userEmail}, NOW())
        ON CONFLICT (event_id, user_email) DO NOTHING
        RETURNING event_id, user_email, joined_at;
    `;
    return result[0];
}

async function leaveEvent(eventId, userEmail) {
    const result = await sql`
        DELETE FROM mydb.event_participants
        WHERE event_id = ${eventId} AND user_email = ${userEmail}
        RETURNING event_id, user_email;
    `;
    return result[0];
}

async function getEventParticipants(eventId) {
    const result = await sql`
        SELECT
            ep.event_id,
            ep.user_email,
            ep.joined_at,
            b.brukernavn,
            b.fnavn,
            b.enavn
        FROM mydb.event_participants ep
        LEFT JOIN mydb.bruker b ON ep.user_email = b.epost
        WHERE ep.event_id = ${eventId}
        ORDER BY ep.joined_at ASC;
    `;
    return result;
}

async function isUserParticipating(eventId, userEmail) {
    const result = await sql`
        SELECT event_id FROM mydb.event_participants
        WHERE event_id = ${eventId} AND user_email = ${userEmail};
    `;
    return result.length > 0;
}

// Stub functions for club documents (not yet implemented)
async function uploadClubDocument(klubId, title, fileData, uploadedBy) {
    console.warn('uploadClubDocument not fully implemented');
    return { id: 0 };
}

async function getClubDocuments(klubId, limit = 5) {
    console.warn('getClubDocuments not fully implemented');
    return [];
}

async function getClubDocument(documentId) {
    console.warn('getClubDocument not fully implemented');
    return null;
}

async function deleteClubDocument(documentId) {
    console.warn('deleteClubDocument not fully implemented');
    return null;
}

// Hole coordinate functions
async function upsertHoleCoordinates(baneId, holeNumber, coordinates) {
    console.warn('upsertHoleCoordinates not fully implemented');
    return { success: true };
}

async function bulkUpsertHoles(baneId, holes) {
    console.warn('bulkUpsertHoles not fully implemented');
    return { success: true, count: 0 };
}

async function getHolesForTrack(baneId) {
    console.warn('getHolesForTrack not fully implemented');
    return [];
}

async function deleteHole(baneId, holeNumber) {
    console.warn('deleteHole not fully implemented');
    return { success: true };
}

async function deleteAllHolesForTrack(baneId) {
    console.warn('deleteAllHolesForTrack not fully implemented');
    return { success: true, count: 0 };
}

/*
* Denne funksjonen gjenoppretter databasen fra en sikkerhetskopi.
* Dette er en stub implementasjon som logger en advarsel.
*/
async function restoreDatabaseFromBackup() {
    console.warn('restoreDatabaseFromBackup is a stub - actual backup restore should be done manually or via database tools');
    return {
        success: true,
        message: 'Backup restore functionality is not implemented. Please restore database manually.'
    };
}

// Add to your existing export default
export default {
     validateConnection,
     createUser,
     logInn, 
     deleteBruker, 
     redigerBruker, 
     getBruker, 
     getBrukerByUsername, 
     getPoengkort, 
     createPoengkortForCourse, 
     deletePoengkort, 
     updatePoengkort, 
     getCompleteRound, 
     getBaneInfo, 
     getRoundsForUser, 
     getKlubber, 
     getCompleteRound, 
     createTrack, 
     getTrackById, 
     getAllTracks,
     updateTrack,
     deleteTrack,
     getAllSpillere,
     createGameSession, 
     getGameSession, 
     addPlayerToSession, 
     getSessionPlayers, 
     getAllUsers, 
     searchUsers, 
     updateUserRole, 
     getAllUsers, 
     checkExistingFriendship,
     sendFriendRequest,
     getFriendRequests,
     getPendingRequestsSentByUser,
     acceptFriendRequest,
     declineFriendRequest,
     getFriends, 
     removeFriend,
     updateLastActive,
     sendMessage,
     getConversations, 
     getMessages, 
     createClubRequest, 
     getPendingClubRequests, 
     approveClubRequest, 
     rejectClubRequest, 
     createClubJoinRequest, 
     getPendingClubJoinRequests, 
     approveClubJoinRequest, 
     rejectClubJoinRequest, 
     demoteKlubbAdmin, 
     getClubByAdminEmail, 
     isClubAdmin, 
     getClubMembers, 
     removeClubMember, 
     getClubByAdminEmail, 
     isClubMember, 
     getKlubbForAdmin, 
     hashPassword, 
     comparePassword,
     changePassword,
     hashEmail,
     uploadClubDocument, 
     getClubDocuments, 
     getClubDocument, 
     deleteClubDocument, 
     addClubAnnouncement, 
     getClubAnnouncements, 
     addClubEvent, 
     getClubEvents, 
     deleteClubAnnouncement, 
     deleteClubEvent, 
     getClubAnnouncement, 
     getClubEvent, 
     updateClubInfo,
     restoreDatabaseFromBackup,
     isTrackOwnedByUserClub,
     getKlubbById,
     upsertHoleCoordinates,
     bulkUpsertHoles,
     getHolesForTrack,
     deleteHole,
     deleteAllHolesForTrack,
     getAllClubs,
     getClubById,
     joinEvent,
     leaveEvent,
     getEventParticipants,
     isUserParticipating,
};
