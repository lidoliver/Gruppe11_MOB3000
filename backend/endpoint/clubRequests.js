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
            const { klubbnavn, sted, beskrivelse } = req.body;
            const userEmail = req.user.epost;
            
            // Basic validation
            if (!klubbnavn) {
                return res.status(400).json({ message: 'Club name is required' });
            }
            
            const result = await db.createClubRequest(userEmail, klubbnavn, sted, beskrivelse);
            res.status(201).json({ 
                message: 'Club request submitted successfully',
                requestId: result.id 
            });
        } catch (error) {
            console.error('Error submitting club request:', error);
            res.status(500).json({ message: 'Server error' });
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