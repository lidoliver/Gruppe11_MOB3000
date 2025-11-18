import { sql } from '../db.js';

export async function getPlayerStats(req, res) {
    let { email } = req.params;

    try {
        // Decode the email parameter (handles @ symbol in URL)
        email = decodeURIComponent(email);

        console.log('=== PLAYER STATS REQUEST ===');
        console.log('Decoded email:', email);

        // First, check what emails exist in database
        const allEmails = await sql`
            SELECT DISTINCT spiller_navn FROM mydb.pointcard LIMIT 10
        `;
        console.log('Emails in database:', allEmails.map(e => e.spiller_navn));

        // Get overall statistics using postgres template literal syntax
        const statsResult = await sql`
            SELECT
                COUNT(*)::int AS total_games,
                ROUND(AVG(total_score)::numeric, 2) AS average_score,
                MIN(total_score) AS best_score,
                MAX(total_score) AS worst_score,
                MAX(dato) AS last_played_at
            FROM mydb.pointcard
            WHERE spiller_navn = ${email}
        `;

        // Get per-course breakdown
        const courseResult = await sql`
            SELECT
                b.navn AS course_name,
                COUNT(*)::int AS round_count
            FROM mydb.pointcard p
            JOIN mydb.bane b ON p.bane_id = b.bane_id
            WHERE p.spiller_navn = ${email}
            GROUP BY b.navn
            ORDER BY round_count DESC
        `;

        // Transform course data into map
        const totalRoundsByCourse = {};
        courseResult.forEach(row => {
            totalRoundsByCourse[row.course_name] = row.round_count;
        });

        // Extract stats (postgres returns array directly, not .rows)
        const stats = statsResult[0] || {};

        console.log(`Stats found: ${stats.total_games || 0} games for ${email}`);
        console.log('===========================\n');

        res.json({
            email: email,
            totalGames: stats.total_games || 0,
            averageScore: stats.average_score ? parseFloat(stats.average_score) : null,
            bestScore: stats.best_score || null,
            worstScore: stats.worst_score || null,
            lastPlayedAt: stats.last_played_at || null,
            totalRoundsByCourse: totalRoundsByCourse
        });

    } catch (error) {
        console.error('❌ Error fetching player stats:', error);
        console.error('Error details:', error.message);
        console.error('Email param:', email);
        res.status(500).json({
            error: 'Failed to fetch player statistics',
            details: error.message,
            email: email
        });
    }
}
