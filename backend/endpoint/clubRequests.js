// In a new file backend/endpoint/clubRequests.js
import express from 'express';
import passport from 'passport';
import { requireRole } from '../middleware/auth.js'; // Add this line
import db from '../db.js';

const router = express.Router();

// Submit club creation request
router.post('/', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            console.log('Received club request body:', req.body);
            console.log('User:', req.user?.epost);

            const { klubbnavn, sted, beskrivelse } = req.body;
            const userEmail = req.user.epost;
            
            // Basic validation
            if (!klubbnavn) {
                console.error('Missing klubbnavn');
                return res.status(400).json({ message: 'Club name is required' });
            }
            
            if (!beskrivelse) {
                console.error('Missing beskrivelse');
                return res.status(400).json({ message: 'Description is required' });
            }

            console.log('Creating club request:', { userEmail, klubbnavn, sted, beskrivelse: beskrivelse?.substring(0, 50) });
            const result = await db.createClubRequest(userEmail, klubbnavn, sted || null, beskrivelse);
            console.log('Club request created successfully:', result);

            res.status(201).json({
                message: 'Club request submitted successfully',
                requestId: result.id 
            });
        } catch (error) {
            console.error('Error submitting club request:', error);
            console.error('Error stack:', error.stack);
            res.status(500).json({
                message: 'Server error',
                error: process.env.NODE_ENV === 'development' ? error.message : undefined
            });
        }
    }
);

// Get all pending club requests (admin only)
router.get('/', 
    passport.authenticate('jwt', {session: false}),
    requireRole('admin'),
    async (req, res) => {
        try {
            const requests = await db.getPendingClubRequests();
            res.status(200).json(requests);
        } catch (error) {
            console.error('Error fetching club requests:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Approve a club request
router.post('/:id/approve',
    passport.authenticate('jwt', {session: false}),
    requireRole('admin'),
    async (req, res) => {
        try {
            await db.approveClubRequest(req.params.id);
            res.status(200).json({ message: 'Club request approved' });
        } catch (error) {
            console.error('Error approving club request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Reject a club request
router.post('/:id/reject',
    passport.authenticate('jwt', {session: false}),
    requireRole('admin'),
    async (req, res) => {
        try {
            await db.rejectClubRequest(req.params.id);
            res.status(200).json({ message: 'Club request rejected' });
        } catch (error) {
            console.error('Error rejecting club request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

export default router;