import { sql } from '../db.js';

/**
 * Get all games played by a player on a specific course
 * Returns detailed game information including hole-by-hole scores
 */
export async function getPlayerCourseGames(req, res) {
    let { email, courseName } = req.params;

    try {
        // Decode the email parameter (handles @ symbol in URL)
        email = decodeURIComponent(email);
        courseName = decodeURIComponent(courseName);

        console.log('=== HENTER BANEHISTORIKK ===');
        console.log('Email:', email);
        console.log('Bane:', courseName);

        // Get detailed games for this player on this specific course
        const gamesResult = await sql`
            SELECT
                p.pointcard_id,
                p.bane_id,
                p.total_score,
                p.score as hole_scores,
                (p.dato AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Oslo') as played_at,
                b.navn as course_name,
                b.total_par,
                b.antall_hull
            FROM mydb.pointcard p
            JOIN mydb.bane b ON p.bane_id = b.bane_id
            WHERE p.spiller_navn = ${email}
            AND b.navn = ${courseName}
            ORDER BY p.dato DESC
        `;

        // Get hole details for the course
        const holesResult = await sql`
            SELECT
                h.hull_id,
                h.hull_nummer,
                h.hole_par,
                h.hole_distance
            FROM mydb.hull h
            JOIN mydb.bane b ON h.bane_id_bane = b.bane_id
            WHERE b.navn = ${courseName}
            ORDER BY h.hull_nummer
        `;

        // Calculate statistics for each hole
        const holeStats = {};
        holesResult.forEach(hole => {
            holeStats[hole.hull_id] = {
                holeNumber: hole.hull_nummer,
                par: hole.hole_par,
                distance: hole.hole_distance,
                scores: [],
                averageScore: 0,
                bestScore: null,
                worstScore: null
            };
        });

        // Process games and populate hole statistics
        const games = gamesResult.map(game => {
            const holeScores = game.hole_scores || [];

            // Add scores to hole statistics
            holeScores.forEach(hs => {
                if (holeStats[hs.hull_id]) {
                    holeStats[hs.hull_id].scores.push(hs.score);
                }
            });

            return {
                gameId: game.pointcard_id,
                courseId: game.bane_id,
                totalScore: game.total_score,
                coursePar: game.total_par,
                playedAt: game.played_at,
                holeScores: holeScores.map(hs => ({
                    holeId: hs.hull_id,
                    score: hs.score
                }))
            };
        });

        // Calculate hole statistics
        Object.keys(holeStats).forEach(holeId => {
            const hole = holeStats[holeId];
            if (hole.scores.length > 0) {
                hole.averageScore = (hole.scores.reduce((a, b) => a + b, 0) / hole.scores.length).toFixed(1);
                hole.bestScore = Math.min(...hole.scores);
                hole.worstScore = Math.max(...hole.scores);
            }
            delete hole.scores; // Remove raw scores from response
        });

        // Convert holeStats to array and sort by hole number
        const holeStatsArray = Object.entries(holeStats).map(([holeId, stats]) => ({
            holeId: parseInt(holeId),
            ...stats
        })).sort((a, b) => a.holeNumber - b.holeNumber);

        console.log(`Fant ${games.length} runder for ${email} på ${courseName}`);
        console.log('===========================\n');

        res.json({
            email: email,
            courseName: courseName,
            totalGames: games.length,
            games: games,
            holeStatistics: holeStatsArray
        });

    } catch (error) {
        console.error('❌ Feil ved henting av banehistorikk:', error);
        console.error('Feilmelding:', error.message);
        res.status(500).json({
            error: 'Kunne ikke hente banehistorikk',
            details: error.message
        });
    }
}

