import express from 'express';
import passport from 'passport';
import db from '../db.js';

const router = express.Router();

/*
* Denne filen håndterer alle ruter relatert til venners forespørsel og administrasjon.
* Den bruker Passport for autentisering og har ruter for å sende, godta, avslå og hente venners forespørsel.
* Den har også ruter for å hente vennelisten og fjerne venner.
* Utviklet av [Martin Hansen]
* Ai-assistanse: github copilot. 
*/
router.post('/request', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const { friendEmail } = req.body;
            const userEmail = req.user.epost;
            
            if (userEmail === friendEmail) {
                return res.status(400).json({ message: 'You cannot add yourself as a friend' });
            }
            
            // Check if the friend exists
            const friendExists = await db.getBruker(friendEmail);
            if (!friendExists) {
                return res.status(404).json({ message: 'User not found' });
            }
            
            // Check if already friends or if there's an existing friendship/request in either direction
            const existingFriendship = await db.checkExistingFriendship(userEmail, friendEmail);

            if (existingFriendship) {
                if (existingFriendship.status === 'accepted') {
                    return res.status(400).json({ message: 'Already friends with this user' });
                } else if (existingFriendship.status === 'pending') {
                    if (existingFriendship.user_email === userEmail) {
                        return res.status(400).json({ message: 'Friend request already sent' });
                    } else {
                        return res.status(400).json({ message: 'This user has already sent you a friend request. Check your pending requests.' });
                    }
                }
            }
            
            const request = await db.sendFriendRequest(userEmail, friendEmail);
            res.status(201).json(request);
        } catch (error) {
            console.error('Error sending friend request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

/*
* Demme ruten henter venners forespørsel for den innloggede brukeren.
* Den bruker Passport for autentisering og henter forespørslene fra databasen.
* Parametrene som sendes med forespørselen er brukerens e-postadresse.
*/
router.get('/requests', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            // Add validation and debugging
            if (!req.user) {
                console.error('User object is missing in request');
                return res.status(401).json({ message: 'Authentication failed - no user found' });
            }
            
            if (!req.user.epost) {
                console.error('Email is missing in user object:', req.user);
                return res.status(401).json({ message: 'Authentication failed - no email found' });
            }
            
            const userEmail = req.user.epost;
            console.log('Getting friend requests for user:', userEmail);
            
            const requests = await db.getFriendRequests(userEmail);
            res.status(200).json(requests);
        } catch (error) {
            console.error('Error getting friend requests:', error);
            res.status(500).json({ message: error.message || 'Server error' });
        }
    }
);

/*
* Denne ruten godtar en venners forespørsel.
* Den bruker Passport for autentisering og oppdaterer databasen med den godkjente forespørselen.
* Parametrene som sendes med forespørselen er brukerens e-postadresse og forespørselens ID.
*/
router.post('/accept/:requestId', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const requestId = req.params.requestId;
            const userEmail = req.user.epost;
            
            const result = await db.acceptFriendRequest(requestId, userEmail);
            if (!result) {
                return res.status(404).json({ message: 'Friend request not found or already processed' });
            }
            
            res.status(200).json(result);
        } catch (error) {
            console.error('Error accepting friend request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

/*
* Denne ruten avslår en venners forespørsel.
* Den bruker Passport for autentisering og oppdaterer databasen med den avviste forespørselen.
*/
router.post('/decline/:requestId', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const requestId = req.params.requestId;
            const userEmail = req.user.epost;
            
            const result = await db.declineFriendRequest(requestId, userEmail);
            if (!result) {
                return res.status(404).json({ message: 'Friend request not found' });
            }
            
            res.status(200).json(result);
        } catch (error) {
            console.error('Error declining friend request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

/*
* Denne ruten henter vennelisten for den innloggede brukeren.
* Den bruker Passport for autentisering og henter vennene fra databasen.
* Parametrene som sendes med forespørselen er brukerens e-postadresse.
*/
router.get('/', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            
            if (!req.user) {
                console.error('User object is missing in request');
                return res.status(401).json({ message: 'Authentication failed - no user found' });
            }
            
            if (!req.user.epost) {
                console.error('Email is missing in user object:', req.user);
                return res.status(401).json({ message: 'Authentication failed - no email found' });
            }
            
            const userEmail = req.user.epost;
            console.log('Getting friends for user:', userEmail);
            
            const friends = await db.getFriends(userEmail);
            res.status(200).json(friends);
        } catch (error) {
            console.error('Error getting friends:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

/*
* Denne ruten fjerner en venn fra vennelisten.
* Den bruker Passport for autentisering og oppdaterer databasen med den fjernede vennen.
* Parametrene som sendes med forespørselen er brukerens e-postadresse og vennens e-postadresse.
*/
router.delete('/:friendEmail', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const userEmail = req.user.epost;
            const friendEmail = req.params.friendEmail;
            
            const result = await db.removeFriend(userEmail, friendEmail);
            res.status(200).json({ message: 'Friend removed' });
        } catch (error) {
            console.error('Error removing friend:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

export default router;