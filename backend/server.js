// Sondre og Martin Skrev dette!
import db from './db.js';
import express from 'express';
import cors from 'cors';
import * as path from "node:path";
import {fileURLToPath} from 'node:url';
import passport from 'passport';
//import JWT auth routes
import jwtAuth from './routes/jwtAuth.js';
import crypto from 'crypto';

// Import the users endpoint
import usersRoutes from './endpoint/users.js';
import authRoutes, { requireRole, authenticateToken } from './middleware/auth.js';
import friendsRoutes from './endpoint/friends.js';
import messagesRoutes from './endpoint/messages.js';
import meRoutes from './endpoint/me.js';
import clubManagementRoutes from './endpoint/clubManagement.js';
import clubJoinRequestsRoutes from './endpoint/clubJoinRequests.js';
import clubRequestsRoutes from './endpoint/clubRequests.js';
import clubJoinRequestsRouter from './endpoint/clubJoinRequests.js';
import { getPlayerStats } from './endpoint/playerStats.js';

// Import the canEditTrack middleware at the top of the file
import { checkRole, canEditTrack } from './routes/checkRole.js';

const app = express();
const PORT = process.env.PORT || 5000;
const filename = fileURLToPath(import.meta.url);
const dirname = path.dirname(filename);

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(express.json()); //req.body

// Init passport 
app.use(passport.initialize());

// Use JWT auth routes
app.use('/auth', jwtAuth);
app.use('/auth', authRoutes); 
// Add this with your other app.use statements
app.use('/users', usersRoutes);
app.use('/api/friends', friendsRoutes);
app.use('/api/messages', messagesRoutes);
app.use('/api/me', meRoutes);
app.use('/api', clubManagementRoutes);
app.use('/api/club-join-requests', clubJoinRequestsRoutes);
app.use('/api/club-requests', clubRequestsRoutes);
app.use('/api/club-join-requests', clubJoinRequestsRouter);

//ROUTES//

// Player statistics endpoint
app.get('/api/players/:email/stats', getPlayerStats);

// Rute for å hente spillere fra databasen. laget av Marcus
app.get('/api/epost', async (req, res) => {
    try {
      const spillere = await db.getAllSpillere();
      res.json(spillere);
    } catch (error) {
      console.error('Error fetching players:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  });

  app.get('/bruker/:email', passport.authenticate('jwt', { session: false }), async (req, res) => {
    try {
        console.log(`Fetching user data for email: ${req.params.email}`);
        const bruker = await db.getBruker(req.params.email);
        console.log(`User data fetched from database:`, bruker);
        
        if (bruker) {
            // Don't overwrite existing klubb_id_klubb if it exists
            if (bruker.role === 'klubbadmin' && !bruker.klubb_id_klubb) {
                const klubbInfo = await db.getKlubbForAdmin(req.params.email);
                console.log(`Club info fetched for klubbadmin:`, klubbInfo);
                if (klubbInfo) {
                    bruker.klubb_id_klubb = klubbInfo.id_klubb;
                    bruker.klubbnavn = klubbInfo.klubbnavn;
                }
            }
            
            // Ensure we're sending back the complete user object
            const responseData = {
                epost: bruker.epost,
                role: bruker.role,
                klubb_id_klubb: bruker.klubb_id_klubb, // Make sure this field is included
                brukernavn: bruker.brukernavn,
                fnavn: bruker.fnavn,
                enavn: bruker.enavn
            };
            
            console.log("Final user data being sent to frontend:", responseData);
            res.status(200).json(responseData);
        } else {
            console.warn(`No user found for email: ${req.params.email}`);
            res.status(404).send('User not found');
        }
    } catch (error) {
        console.error('Error in /bruker/:email endpoint:', error);
        res.status(500).send('Internal server error');
    }
});

app.put('/bruker/:email', passport.authenticate('jwt', {session: false}),
     async (req, res) => {
    try {
        console.log('PUT /bruker/:email - Updating user:', req.params.email);
        console.log('Request body:', JSON.stringify(req.body, null, 2));

        const updatedUser = await db.redigerBruker(req.params.email, req.body);

        if(updatedUser) {
            console.log('User updated successfully:', JSON.stringify(updatedUser, null, 2));
            res.status(200).json(updatedUser);
        } else {
            console.warn('User not found:', req.params.email);
            res.status(404).send('User not found');
        }
    } catch (error) {
        console.error('Error updating user:', error.message);
        console.error('Stack:', error.stack);
        res.status(500).json({ message: 'Error updating user', error: error.message });
    }
});

app.delete('/bruker/:email', passport.authenticate('jwt', {session: false}),
     async (req, res) => {

    try {
        const epost = await db.deleteBruker(req.params.email);
        if(epost) {
            res.status(200).json(epost);
        } else {
            res.status(404).send('User not found');
        }
    } catch (error) {
        console.error(error);
        res.status(500).send('error');
    }
});


app.get('/api/pointcard/:email', passport.authenticate('jwt', {session: false}), 
    async (req, res) => {
    try {
        const email = req.params.email;
        console.log(`========================================`);
        console.log(`Fetching scorecards for: ${email}`);
        
        const poengkort = await db.getPoengkort(email);
        console.log(`Scorecard data retrieved: ${poengkort ? poengkort.length : 0} records`);
        if (poengkort && poengkort.length > 0) {
            console.log('First scorecard:', JSON.stringify(poengkort[0], null, 2));
        }
        console.log(`========================================`);

        res.status(200).json(poengkort || []);
        
    } catch (error) {
        console.error('Error fetching poengkort:', error);
        res.status(500).json({ message: 'Serverfeil ved henting av poengkort' });
    }
});

//Api for å opprette et nytt poengkort
//Laget av Oliver Fiveland
//Github Copilot hjalp med å lage denne
app.post('/api/pointcard/course', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { bane_id, spiller_navn, hullPoeng } = req.body;

        
        

        if (!bane_id || !spiller_navn || !Array.isArray(hullPoeng) || hullPoeng.length === 0) {
            return res.status(400).json({ message: 'Mangler nødvendig data for poengkort' });
        }

        
        const courseExists = await db.getBaneInfo(bane_id);
        if (!courseExists) {
            return res.status(400).json({ message: 'Bane med gitt ID finnes ikke' });
        }

        

        const poengkort = await db.createPoengkortForCourse(bane_id, spiller_navn, hullPoeng);
        res.status(201).json(poengkort);
    } catch (error) {
        console.error('Error creating poengkort:', error);
        res.status(500).json({ message: 'Serverfeil ved oppretting av poengkort' });
    }
});


app.get('/api/baneoversikt', async (req, res) => {
    try {
        const baner = await db.getAllTracks();
        res.status(200).json(baner || []);
    } catch (error) {
        console.error('Error fetching courses:', error);
        res.status(500).json({ message: 'Serverfeil ved henting av baner' });
    }
});

app.delete('/api/pointcard/:id', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const id = req.params.id;
        const poengkort = await db.deletePoengkort(id);
        if (poengkort) {
            res.status(200).json(poengkort);
        } else {
            res.status(404).json({ message: 'Poengkort ikke funnet' });
        }
    } catch (error) {
        console.error('Error deleting poengkort:', error);
        res.status(500).json({ message: 'Serverfeil ved sletting av poengkort' });
    }
});

//Api for å oppdatere et poengkort
//Laget av Oliver Fiveland
//Github Copilot hjalp med å lage denne
app.put('/api/pointcard/:id', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const id = req.params.id;
        const { hullPoeng } = req.body; 

        if (!Array.isArray(hullPoeng) || hullPoeng.length === 0) {
            return res.status(400).json({ message: 'Mangler nødvendig data for poengkort' });
        }

        const poengkort = await db.updatePoengkort(id, { hullPoeng });
        if (poengkort) {
            res.status(200).json(poengkort);
        } else {
            res.status(404).json({ message: 'Poengkort ikke funnet' });
        }
    } catch (error) {
        console.error('Error updating poengkort:', error);
        res.status(500).json({ message: 'Serverfeil ved oppdatering av poengkort' });
    }
});


// Add route for getting a complete round
app.get('/api/rounds/:bruker_id/:bane_id/:date', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { bruker_id, bane_id, date } = req.params;
        const round = await db.getCompleteRound(bruker_id, bane_id, date);
        
        if (round && round.length > 0) {
            res.status(200).json(round);
        } else {
            res.status(404).json({ message: 'Ingen runde funnet med gitt informasjon' });
        }
    } catch (error) {
        console.error('Error fetching complete round:', error);
        res.status(500).json({ message: 'Serverfeil ved henting av runde' });
    }
});

// Add route for getting course information
app.get('/api/bane/:bane_id', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const bane_id = req.params.bane_id;
        const bane = await db.getBaneInfo(bane_id);
        
        if (bane) {
            res.status(200).json(bane);
        } else {
            res.status(404).json({ message: 'Bane ikke funnet' });
        }
    } catch (error) {
        console.error('Error fetching course info:', error);
        res.status(500).json({ message: 'Serverfeil ved henting av baneinformasjon' });
    }
});

// Add route for getting all rounds for a user
app.get('/api/rounds/user/:email', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const email = req.params.email;
        const rounds = await db.getRoundsForUser(email);
        
        if (rounds && rounds.length > 0) {
            res.status(200).json(rounds);
        } else {
            res.status(404).json({ message: 'Ingen runder funnet for denne brukeren' });
        }
    } catch (error) {
        console.error('Error fetching user rounds:', error);
        res.status(500).json({ message: 'Serverfeil ved henting av brukerrunder' });
    }
});

// Add route for deleting an entire round
app.delete('/api/rounds/:bruker_id/:bane_id/:date', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { bruker_id, bane_id, date } = req.params;
        const deleted = await db.deletePoengkort(bruker_id, bane_id, date);
        
        if (deleted && deleted.length > 0) {
            res.status(200).json(deleted);
        } else {
            res.status(404).json({ message: 'Ingen runde funnet med gitt informasjon' });
        }
    } catch (error) {
        console.error('Error deleting round:', error);
        res.status(500).json({ message: 'Serverfeil ved sletting av runde' });
    }
});

// Get clubs - returns all clubs or just user's clubs if they are a member
app.get('/api/klubber/:email?', async (req, res) => {
    try {
        const email = req.params.email || null;
        const klubber = await db.getKlubber(email);
        
        if (klubber && klubber.length > 0) {
            res.status(200).json(klubber);
        } else {
            res.status(404).json({ message: 'Ingen klubber funnet' });
        }
    } catch (error) {
        console.error('Error fetching clubs:', error);
        res.status(500).json({ message: 'Serverfeil ved henting av klubber' });
    }
});

// Get all clubs for admin purposes (simple list for dropdown)
app.get('/api/clubs/list', passport.authenticate('jwt', {session: false}),
    requireRole('admin'),
    async (req, res) => {
    try {
        const klubber = await db.getKlubber();
        
        if (klubber && klubber.length > 0) {
            res.status(200).json(klubber);
        } else {
            res.status(404).json({ message: 'No clubs found' });
        }
    } catch (error) {
        console.error('Error fetching clubs list:', error);
        res.status(500).json({ message: 'Server error fetching clubs' });
    }
});

//bane routes
// Enhance the track creation endpoint with more detailed logging
app.post('/api/tracks', 
    passport.authenticate('jwt', {session: false}), 
    async (req, res) => {
    try {
        const trackData = req.body;
        const userEmail = req.user.epost;
        const userRole = req.user.role;
        
        console.log(`Creating track "${trackData.trackName}" by user ${userEmail} (${userRole})`);
        
        // Log the request data for debugging
        console.log('Creating new track with name:', trackData.trackName);
        console.log('Track has image data:', !!trackData.imageData);
        console.log('Track has OB lines:', Array.isArray(trackData.obLines) ? trackData.obLines.length : 'not an array');
        
        if (!trackData.trackName) {
            return res.status(400).json({ message: 'Track name is required' });
        }
        
        // Add club ID for klubbadmin users
        if (userRole === 'klubbadmin') {
            const userInfo = await db.getBruker(userEmail);
            console.log(`User club info:`, userInfo ? {
                klubb_id_klubb: userInfo.klubb_id_klubb,
                role: userInfo.role
            } : 'No user info found');
            
            if (userInfo && userInfo.klubb_id_klubb) {
                console.log(`Setting track klubb_id_klubb to user's club: ${userInfo.klubb_id_klubb}`);
                trackData.klubb_id_klubb = userInfo.klubb_id_klubb;
                // Also set oppdatert_av for tracking
                trackData.oppdatert_av = userEmail;
            } else {
                console.warn(`WARNING: klubbadmin ${userEmail} doesn't have a klubb_id_klubb!`);
            }
        }
        
        console.log(`Final track data being sent to createTrack:`, {
            trackName: trackData.trackName,
            klubb_id_klubb: trackData.klubb_id_klubb,
            oppdatert_av: trackData.oppdatert_av
        });
        
        // Ensure markers is an array
        if (!Array.isArray(trackData.markers)) {
            trackData.markers = [];
        }
        
        // Ensure lines is an array
        if (!Array.isArray(trackData.lines)) {
            trackData.lines = [];
        }
        
        // Ensure obLines is an array
        if (!Array.isArray(trackData.obLines)) {
            trackData.obLines = [];
        }
        
        // Handle empty values to prevent SQL errors
        trackData.description = trackData.description || '';
        trackData.totalDistance = trackData.totalDistance || 0;
        trackData.totalPar = trackData.totalPar || 0;
        trackData.holeCount = trackData.holeCount || 0;
        trackData.rating = trackData.rating || 0;
        
        const track = await db.createTrack(trackData);
        console.log(`Track created with ID: ${track.bane_id}, club ID: ${track.klubb_id_klubb}`);
        res.status(201).json(track);
    } catch (error) {
        console.error('Error creating track:', error);
        // Send more detailed error information
        res.status(500).json({ 
            message: 'Error saving track to database',
            error: error.message,
            stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
});

// Get all tracks
app.get('/api/tracks', passport.authenticate('jwt', {session: false}), 
    async (req, res) => {
    try {
        const tracks = await db.getAllTracks();
        res.status(200).json(tracks);
    } catch (error) {
        console.error('Error fetching tracks:', error);
        res.status(500).json({ message: 'Error retrieving tracks from database' });
    }
});

// Get track by ID
app.get('/api/tracks/:id', passport.authenticate('jwt', {session: false}), 
    async (req, res) => {
    try {
        const trackId = req.params.id;
        const track = await db.getTrackById(trackId);
        
        if (track) {
            res.status(200).json(track);
        } else {
            res.status(404).json({ message: 'Track not found' });
        }
    } catch (error) {
        console.error('Error fetching track:', error);
        res.status(500).json({ message: 'Error retrieving track from database' });
    }
});

// Update track - add the canEditTrack middleware
app.put('/api/tracks/:id', 
    passport.authenticate('jwt', {session: false}), 
    canEditTrack,  // Add this line
    async (req, res) => {
    try {
        const trackId = req.params.id;
        const trackData = req.body;
        
        if (!trackData.trackName) {
            return res.status(400).json({ message: 'Track name is required' });
        }
        
        const track = await db.updateTrack(trackId, trackData);
        res.status(200).json(track);
    } catch (error) {
        console.error('Error updating track:', error);
        if (error.message === 'Track not found') {
            res.status(404).json({ message: 'Track not found' });
        } else {
            res.status(500).json({ message: 'Error updating track in database' });
        }
    }
});

// Delete track - add the canEditTrack middleware
app.delete('/api/tracks/:id', 
    passport.authenticate('jwt', {session: false}), 
    canEditTrack,  // Add this line
    async (req, res) => {
    try {
        const trackId = req.params.id;
        const result = await db.deleteTrack(trackId);
        
        if (result) {
            res.status(200).json({ message: 'Track deleted successfully', bane_id: result.bane_id });
        } else {
            res.status(404).json({ message: 'Track not found' });
        }
    } catch (error) {
        console.error('Error deleting track:', error);
        res.status(500).json({ message: 'Error deleting track from database' });
    }
});

// PATCH endpoint for partial track updates
app.patch('/api/tracks/:id', 
    passport.authenticate('jwt', {session: false}),
    canEditTrack,  // Ensures only track owners can update
    async (req, res) => {
        try {
            const trackId = req.params.id;
            const updateData = req.body;
            
            // Check if track exists first
            const existingTrack = await db.getTrackById(trackId);
            if (!existingTrack) {
                return res.status(404).json({ message: 'Bane ikke funnet' });
            }
            
            // Unlike PUT, PATCH doesn't require all fields
            // We just update what was provided
            const updatedTrack = await db.updateTrack(trackId, updateData);
            
            console.log(`Track ${trackId} successfully patched with fields: ${Object.keys(updateData).join(', ')}`);
            res.status(200).json(updatedTrack);
        } catch (error) {
            console.error('Error patching track:', error);
            if (error.message === 'Track not found') {
                res.status(404).json({ message: 'Bane ikke funnet' });
            } else {
                res.status(500).json({ message: 'Feil ved oppdatering av bane i databasen' });
            }
        }
    }
);


// Get all images for a track
app.get('/api/tracks/:id/images', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const trackId = req.params.id;
        const images = await db.getTrackImages(trackId);
        res.status(200).json(images);
    } catch (error) {
        console.error('Error fetching track images:', error);
        res.status(500).json({ message: 'Error retrieving track images' });
    }
});

// Add images to a track
app.post('/api/tracks/:id/images', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const trackId = req.params.id;
        const { images } = req.body;
        
        if (!Array.isArray(images)) {
            return res.status(400).json({ message: 'Images must be an array' });
        }
        
        const result = await db.addTrackImages(trackId, images);
        res.status(201).json(result);
    } catch (error) {
        console.error('Error adding track images:', error);
        res.status(500).json({ message: 'Error saving track images' });
    }
});

// Delete a track image
app.delete('/api/tracks/images/:imageId', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const imageId = req.params.imageId;
        await db.deleteTrackImage(imageId);
        res.status(200).json({ message: 'Image deleted successfully' });
    } catch (error) {
        console.error('Error deleting track image:', error);
        res.status(500).json({ message: 'Error deleting track image' });
    }
});

// ============= HOLE MANAGEMENT ENDPOINTS =============

// Get all holes for a specific track
app.get('/api/tracks/:trackId/holes', passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { trackId } = req.params;
        const holes = await db.getHolesForTrack(trackId);
        res.status(200).json(holes);
    } catch (error) {
        console.error('Error fetching holes:', error);
        res.status(500).json({ message: 'Error fetching holes' });
    }
});

// Create or update a single hole for a track
app.put('/api/tracks/:trackId/holes/:holeNumber',
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { trackId, holeNumber } = req.params;
        const { teeLatitude, teeLongitude, basketLatitude, basketLongitude, par } = req.body;

        // Validate coordinates
        if (!teeLatitude || !teeLongitude || !basketLatitude || !basketLongitude) {
            return res.status(400).json({
                message: 'Missing required coordinates'
            });
        }

        await db.upsertHoleCoordinates(trackId, parseInt(holeNumber), {
            teeLatitude: parseFloat(teeLatitude),
            teeLongitude: parseFloat(teeLongitude),
            basketLatitude: parseFloat(basketLatitude),
            basketLongitude: parseFloat(basketLongitude),
            par: parseInt(par) || 3
        });

        res.status(200).json({
            message: 'Hole coordinates saved successfully',
            holeNumber: parseInt(holeNumber)
        });
    } catch (error) {
        console.error('Error saving hole coordinates:', error);
        res.status(500).json({ message: 'Error saving hole coordinates' });
    }
});

// Bulk create/update holes for a track
app.post('/api/tracks/:trackId/holes/bulk',
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { trackId } = req.params;
        const { holes } = req.body;

        if (!Array.isArray(holes) || holes.length === 0) {
            return res.status(400).json({
                message: 'Invalid holes data - must be a non-empty array'
            });
        }

        const result = await db.bulkUpsertHoles(trackId, holes);
        res.status(200).json({
            message: 'Holes saved successfully',
            count: result.count
        });
    } catch (error) {
        console.error('Error bulk saving holes:', error);
        res.status(500).json({ message: 'Error bulk saving holes' });
    }
});

// Delete a specific hole
app.delete('/api/tracks/:trackId/holes/:holeNumber',
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { trackId, holeNumber } = req.params;
        await db.deleteHole(trackId, parseInt(holeNumber));
        res.status(200).json({ message: 'Hole deleted successfully' });
    } catch (error) {
        console.error('Error deleting hole:', error);
        res.status(500).json({ message: 'Error deleting hole' });
    }
});

// Delete all holes for a track
app.delete('/api/tracks/:trackId/holes',
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
    try {
        const { trackId } = req.params;
        const result = await db.deleteAllHolesForTrack(trackId);
        res.status(200).json({
            message: 'All holes deleted successfully',
            count: result.count
        });
    } catch (error) {
        console.error('Error deleting holes:', error);
        res.status(500).json({ message: 'Error deleting holes' });
    }
});

// ============= END HOLE MANAGEMENT ENDPOINTS =============

app.get('/api/game-sessions/:sessionId', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
      const { sessionId } = req.params;
      const session = await db.getGameSession(sessionId);
      
      if (!session) {
        return res.status(404).json({ error: 'Game session not found' });
      }
      
      // Also get all players in this session
      const players = await db.getSessionPlayers(sessionId);
      
      res.json({
        ...session,
        players: players || []
      });
    } catch (err) {
      console.error('Error fetching game session:', err);
      res.status(500).json({ error: 'Server error' });
    }
  });
  
  // Also add the create and join endpoints here:
  app.post('/api/game-sessions', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
      const { bane_id, host, totalHoles } = req.body;
      
      // Generate a unique session ID
      const sessionId = crypto.randomUUID();
      
      // Create the session in the database
      await db.createGameSession(sessionId, bane_id, host);
      
      res.status(201).json({ 
        sessionId, 
        message: 'Game session created'
      });
    } catch (err) {
      console.error('Error creating game session:', err);
      res.status(500).json({ 
        error: 'Server error', 
        message: err.message 
      });
    }
  });
  
  app.post('/api/game-sessions/:sessionId/join', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
      const { sessionId } = req.params;
      const { email } = req.body;
      
      // Add player to session
      await db.addPlayerToSession(sessionId, email);
      
      res.json({ message: 'Joined game session successfully' });
    } catch (err) {
      console.error('Error joining game session:', err);
      res.status(500).json({ 
        error: 'Server error',
        message: err.message
      });
    }
  });
  

//Proxy for å kunne hente geocode data fra Nominatim
//Laget ved hjelp av Githhub Copilot
//Laget av Oliver Fiveland
app.get('/api/geocode/reverse', async (req, res) => {
  try {
    const { lat, lon } = req.query;
    if (!lat || !lon) {
      return res.status(400).json({ error: 'Missing required parameters lat and lon' });
    }

    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}`;
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'DiscGolfApp/1.0'  
      }
    });
    
    if (!response.ok) {
      throw new Error(`Geocoding service response: ${response.status}`);
    }
    
    const data = await response.json();
    res.json(data);
  } catch (error) {
    console.error('Geocoding proxy error:', error);
    res.status(500).json({ error: 'Error fetching location data' });
  }
});

// Add to server.js or a new file like endpoint/clubMembership.js
app.get('/api/clubs/:clubId/membership', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const clubId = req.params.clubId;
            const userEmail = req.user.epost;
            
            const isMember = await db.isClubMember(userEmail, clubId);
            
            if (isMember) {
                res.status(200).json({ isMember: true });
            } else {
                res.status(403).json({ 
                    isMember: false, 
                    message: 'You are not a member of this club' 
                });
            }
        } catch (error) {
            console.error('Error checking club membership:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Add to server.js or create a new endpoint file

// Upload a document (club admin only)
app.post('/api/clubs/:clubId/documents', 
    passport.authenticate('jwt', {session: false}),
    requireRole('klubbadmin'),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const { title, fileData } = req.body;
            const userEmail = req.user.epost;
            
            // Verify the user is admin for this club
            const hasPermission = await db.isClubAdmin(userEmail, clubId);
            if (!hasPermission) {
                return res.status(403).json({ message: 'You are not authorized to upload documents to this club' });
            }
            
            const document = await db.uploadClubDocument(clubId, title, fileData, userEmail);
            res.status(201).json(document);
        } catch (error) {
            console.error('Error uploading document:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Get club documents
app.get('/api/clubs/:clubId/documents', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const userEmail = req.user.epost;
            
            // Check if user is a member of this club
            const isMember = await db.isClubMember(userEmail, clubId);
            if (!isMember) {
                return res.status(403).json({ message: 'You must be a member of this club to access documents' });
            }
            
            const documents = await db.getClubDocuments(clubId);
            res.status(200).json(documents);
        } catch (error) {
            console.error('Error getting documents:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Get specific document
app.get('/api/documents/:documentId', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { documentId } = req.params;
            const userEmail = req.user.epost;
            
            const document = await db.getClubDocument(documentId);
            if (!document) {
                return res.status(404).json({ message: 'Document not found' });
            }
            
            // Check if user is a member of the club that owns this document
            const isMember = await db.isClubMember(userEmail, document.klub_id);
            if (!isMember) {
                return res.status(403).json({ message: 'Access denied' });
            }
            
            res.status(200).json(document);
        } catch (error) {
            console.error('Error getting document:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Delete a document (club admin only)
app.delete('/api/documents/:documentId', 
    passport.authenticate('jwt', {session: false}),
    requireRole('klubbadmin'),
    async (req, res) => {
        try {
            const { documentId } = req.params;
            const userEmail = req.user.epost;
            
            // Get document to check ownership
            const document = await db.getClubDocument(documentId);
            if (!document) {
                return res.status(404).json({ message: 'Document not found' });
            }
            
            // Verify the user is admin for this club
            const hasPermission = await db.isClubAdmin(userEmail, document.klub_id);
            if (!hasPermission) {
                return res.status(403).json({ message: 'You are not authorized to delete this document' });
            }
            
            await db.deleteClubDocument(documentId);
            res.status(200).json({ message: 'Document deleted successfully' });
        } catch (error) {
            console.error('Error deleting document:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Add to server.js with your other routes

// Club announcements 
app.get('/api/clubs/:clubId/announcements', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const limit = req.query.limit ? parseInt(req.query.limit) : null;
            
            // Rest of code...
            const announcements = await db.getClubAnnouncements(clubId, limit);
            res.status(200).json(announcements);
        } catch (error) {
            console.error('Error getting announcements:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

app.post('/api/clubs/:clubId/announcements', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const { content } = req.body;
            const userEmail = req.user.epost;
            
            const isAdmin = await db.isClubAdmin(userEmail, clubId);
            if (!isAdmin && req.user.role !== 'admin') {
                return res.status(403).json({ message: 'Only club admins can add announcements' });
            }
            
            const announcement = await db.addClubAnnouncement(clubId, content, userEmail);
            res.status(201).json(announcement);
        } catch (error) {
            console.error('Error adding announcement:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Club events
app.get('/api/clubs/:clubId/events', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const { limit } = req.query;
            
            const isMember = await db.isClubMember(req.user.epost, clubId);
            if (!isMember) {
                return res.status(403).json({ message: 'You must be a member to view events' });
            }
            
            const events = await db.getClubEvents(clubId, limit || 5);
            res.status(200).json(events);
        } catch (error) {
            console.error('Error getting events:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

app.post('/api/clubs/:clubId/events', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const { title, description, eventDate } = req.body;
            const userEmail = req.user.epost;
            
            const isAdmin = await db.isClubAdmin(userEmail, clubId);
            if (!isAdmin && req.user.role !== 'admin') {
                return res.status(403).json({ message: 'Only club admins can add events' });
            }
            
            const event = await db.addClubEvent(clubId, title, description, eventDate, userEmail);
            res.status(201).json(event);
        } catch (error) {
            console.error('Error adding event:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Add these routes to your server.js file

// Delete announcement (club admin or admin only)
app.delete('/api/announcements/:announcementId', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { announcementId } = req.params;
            const userEmail = req.user.epost;
            
            // Get the announcement to check ownership
            const announcement = await db.getClubAnnouncement(announcementId);
            if (!announcement) {
                return res.status(404).json({ message: 'Announcement not found' });
            }
            
            // Check if user is admin or klubbadmin for this club
            const isAdmin = req.user.role === 'admin';
            const isClubAdmin = await db.isClubAdmin(userEmail, announcement.klubb_id);
            
            if (!isAdmin && !isClubAdmin) {
                return res.status(403).json({ message: 'You are not authorized to delete this announcement' });
            }
            
            await db.deleteClubAnnouncement(announcementId);
            res.status(200).json({ message: 'Announcement deleted successfully' });
        } catch (error) {
            console.error('Error deleting announcement:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Delete event (club admin or admin only)
app.delete('/api/events/:eventId', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { eventId } = req.params;
            const userEmail = req.user.epost;
            
            // Get the event to check ownership
            const event = await db.getClubEvent(eventId);
            if (!event) {
                return res.status(404).json({ message: 'Event not found' });
            }
            
            // Check if user is admin or klubbadmin for this club
            const isAdmin = req.user.role === 'admin';
            const isClubAdmin = await db.isClubAdmin(userEmail, event.klubb_id);
            
            if (!isAdmin && !isClubAdmin) {
                return res.status(403).json({ message: 'You are not authorized to delete this event' });
            }
            
            await db.deleteClubEvent(eventId);
            res.status(200).json({ message: 'Event deleted successfully' });
        } catch (error) {
            console.error('Error deleting event:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Update club info endpoint (klubbadmin only)
app.put('/api/clubs/:clubId', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { clubId } = req.params;
            const userEmail = req.user.epost;
            const updateData = req.body;
            
            console.log(`Updating club ${clubId} with data keys: ${Object.keys(updateData).join(', ')}`);
            
            // Check if user is admin or klubbadmin for this club
            const isAdmin = req.user.role === 'admin';
            const isClubAdmin = await db.isClubAdmin(userEmail, clubId);
            
            if (!isAdmin && !isClubAdmin) {
                return res.status(403).json({ message: 'You are not authorized to update this club' });
            }
            
            // Check if image data is too large
            if (updateData.klubb_bilde && updateData.klubb_bilde.length > 1000000 * 5) { // 5MB limit
                return res.status(413).json({ message: 'Image too large. Please use a smaller image.' });
            }
            
            const updatedClub = await db.updateClubInfo(clubId, updateData);
            res.status(200).json(updatedClub);
        } catch (error) {
            console.error('Error in club update route:', error);
            res.status(500).json({ 
                message: 'Server error while updating club information',
                details: error.message
            });
        }
    }
);

// Add this endpoint to your server.js file

// Reset test user actions
app.post('/api/users/:email/reset-test-account', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { email } = req.params;
            const testEmails = ['bruker@usn.no', 'klubbadmin@usn.no', 'admin@usn.no'];
            
            // Check if user is authorized to perform this action
            if (req.user.epost !== email) {
                return res.status(403).json({ message: 'Not authorized to reset this account' });
            }
            
            // Check if it's a test account by email directly
            if (!testEmails.includes(email)) {
                return res.status(403).json({ message: 'Reset only available for test accounts' });
            }
            
            // Perform the reset
            const summary = await db.resetTestUserActions(email);
            
            res.status(200).json({ 
                message: 'Test account reset successfully',
                summary
            });
            
        } catch (error) {
            console.error('Error resetting test user:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Add this as a new endpoint

// Reset test accounts - separate endpoint for clarity
app.post('/api/test-accounts/reset/:email', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            console.log(`Reset request received for: ${req.params.email}`);
            const { email } = req.params;
            const testEmails = ['bruker@usn.no', 'klubbadmin@usn.no', 'admin@usn.no'];
            
            // Check if user is authorized - they can only reset their own test account
            if (req.user.epost !== email) {
                console.log(`Authorization failed: ${req.user.epost} trying to reset ${email}`);
                return res.status(403).json({ message: 'Not authorized to reset this account' });
            }
            
            // Check if it's a test account
            if (!testEmails.includes(email)) {
                console.log(`Not a test account: ${email}`);
                return res.status(403).json({ message: 'Reset only available for test accounts' });
            }
            
            // Perform the reset
            console.log(`Calling db.resetTestUserActions for ${email}`);
            const summary = await db.resetTestUserActions(email);
            
            console.log(`Reset successful for ${email}, summary:`, summary);
            res.status(200).json({ 
                message: 'Test account reset successfully',
                summary
            });
            
        } catch (error) {
            console.error('Error in reset endpoint:', error);
            res.status(500).json({ 
                message: 'Server error', 
                details: error.message 
            });
        }
    }
);

// Fix the database restore endpoint:
app.post('/api/database/restore', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            // Access email through epost property (not email)
            const email = req.user.epost;
            const testEmails = ['bruker@usn.no', 'klubbadmin@usn.no', 'admin@usn.no'];
            
            console.log('User attempting database restore:', email);
            
            // Only allow test users to restore the database
            if (!testEmails.includes(email)) {
                console.log('Access denied: not a test account');
                return res.status(403).json({ 
                    message: 'Only test accounts can restore the database',
                    userEmail: email 
                });
            }
            
            // Restore the database
            const result = await db.restoreDatabaseFromBackup();
            
            res.status(200).json({ 
                message: 'Database restored successfully',
                success: true
            });
            
        } catch (error) {
            console.error('Error restoring database:', error);
            res.status(500).json({ 
                message: 'Failed to restore database', 
                details: error.message 
            });
        }
    }
);

// Add this new endpoint to verify user's club ID
app.get('/api/my-club', 
    passport.authenticate('jwt', {session: false}), 
    async (req, res) => {
    try {
        const userEmail = req.user.epost;
        console.log(`Getting club info for user: ${userEmail}`);
        
        // Get the user's info including their club_id
        const userInfo = await db.getBruker(userEmail);
        
        if (userInfo && userInfo.klubb_id_klubb) {
            console.log(`Found club ID ${userInfo.klubb_id_klubb} for user ${userEmail}`);
            
            // Also get the club name if available
            let clubName = null;
            try {
                const clubInfo = await db.getKlubbById(userInfo.klubb_id_klubb);
                if (clubInfo) {
                    clubName = clubInfo.klubbnavn;
                }
            } catch (err) {
                console.warn("Could not fetch club name", err);
            }
            
            res.status(200).json({ 
                id_klubb: userInfo.klubb_id_klubb,
                role: userInfo.role,
                clubName: clubName
            });
        } else {
            console.log(`No club association found for user ${userEmail}`);
            res.status(404).json({ message: 'No club association found for this user' });
        }
    } catch (error) {
        console.error('Error fetching user club info:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

// Add this temporary debug endpoint
app.get('/api/debug/user-club', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const userEmail = req.user.epost;
            console.log("Debug: checking user club data for", userEmail);
            
            // Get raw user data from database
            const userResult = await sql`
                SELECT epost, role, klubb_id_klubb 
                FROM mydb.bruker
                WHERE epost = ${userEmail}
            `;
            
            // If user exists and has klubb_id_klubb
            if (userResult && userResult.length > 0) {
                const userData = userResult[0];
                console.log("User data from DB:", userData);
                
                // Get club data if klubb_id_klubb exists
                let clubData = null;
                if (userData.klubb_id_klubb) {
                    const clubResult = await sql`
                        SELECT id_klubb, klubbnavn
                        FROM mydb.klubb
                        WHERE id_klubb = ${userData.klubb_id_klubb}
                    `;
                    clubData = clubResult[0];
                    console.log("Club data from DB:", clubData);
                }
                
                res.status(200).json({
                    user: userData,
                    club: clubData
                });
            } else {
                res.status(404).json({ message: "User not found" });
            }
        } catch (error) {
            console.error("Error in debug endpoint:", error);
            res.status(500).json({ error: error.message });
        }
    }
);

db.validateConnection();

const isProduction = process.env.NODE_ENV === 'production';

if (isProduction) {
  // Only serve static files in production
  app.use(express.static(path.join(dirname, '../frontend/dist')));
  app.get('*', (req, res) => {
    res.sendFile(path.join(dirname, '../frontend/dist/index.html'));
  });
}

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

