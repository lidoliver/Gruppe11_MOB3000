import express from 'express';
import passport from 'passport';
import db from '../db.js';
import { sendMessageNotification } from './notifications.js';
import { sendFirestoreMessage } from '../services/firestoreMessages.js';

const router = express.Router();

/*
* Denne filen håndterer alle ruter relatert til sending og henting av meldinger mellom brukere.
* Den bruker Passport for autentisering og har ruter for å sende meldinger, hente samtaler og hente meldinger i en samtale.
* Utviklet av [Martin Hansen]
* Ai-assistanse: github copilot. 
*/
router.post('/', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const { receiverEmail, content } = req.body;
            const senderEmail = req.user.epost;
            
            if (!receiverEmail || !content) {
                return res.status(400).json({ message: 'Receiver email and content are required' });
            }
            
            // Check if receiver exists
            const receiverExists = await db.getBruker(receiverEmail);
            if (!receiverExists) {
                return res.status(404).json({ message: 'Receiver not found' });
            }
            
            // Check if they are friends
            const friends = await db.getFriends(senderEmail);
            const isFriend = friends.some(friend => friend.friend_email === receiverEmail);
            
            if (!isFriend) {
                return res.status(403).json({ message: 'You can only send messages to friends' });
            }
            
            const message = await db.sendMessage(senderEmail, receiverEmail, content);

            // Also send to Firestore for real-time sync
            try {
                await sendFirestoreMessage(senderEmail, receiverEmail, content);
                console.log('✅ Message synced to Firestore');
            } catch (firestoreError) {
                console.error('⚠️ Failed to sync to Firestore:', firestoreError);
                // Don't fail the request if Firestore fails
            }

            // Send push notification to receiver
            try {
                const sender = await db.getBruker(senderEmail);
                const senderName = sender.brukernavn || sender.fnavn || senderEmail.split('@')[0];

                const notificationResult = await sendMessageNotification(
                    receiverEmail,
                    senderEmail,
                    senderName,
                    content
                );

                if (notificationResult) {
                    console.log(`✅ Push notification sent to ${receiverEmail}`);
                } else {
                    console.log(`⚠️ Push notification not sent to ${receiverEmail} (no FCM token registered)`);
                }
            } catch (notifError) {
                // Don't fail the message send if notification fails
                console.error('❌ Failed to send push notification:', notifError);
            }

            res.status(201).json(message);
        } catch (error) {
            console.error('Error sending message:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

/*
* Denne ruten henter alle samtaler for den innloggede brukeren.
* Den bruker Passport for autentisering og henter samtalene fra databasen.
* Parametrene som sendes med forespørselen er brukerens e-postadresse.
*/
router.get('/conversations', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const userEmail = req.user.epost;
            const conversations = await db.getConversations(userEmail);
            res.status(200).json(conversations);
        } catch (error) {
            console.error('Error getting conversations:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

/*
* Denne ruten henter meldinger mellom den innloggede brukeren og en annen bruker.
* Den bruker Passport for autentisering og henter meldingene fra databasen.
* Parametrene som sendes med forespørselen er brukerens e-postadresse og e-postadressen til kontakten.
*/
router.get('/conversation/:contactEmail', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const userEmail = req.user.epost;
            const contactEmail = req.params.contactEmail;
            
            // Validate they are friends
            const friends = await db.getFriends(userEmail);
            const isFriend = friends.some(friend => friend.friend_email === contactEmail);
            
            if (!isFriend) {
                return res.status(403).json({ message: 'You can only view messages from friends' });
            }
            
            const messages = await db.getMessages(userEmail, contactEmail);
            res.status(200).json(messages);
        } catch (error) {
            console.error('Error getting messages:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

export default router;