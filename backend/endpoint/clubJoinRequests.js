import express from 'express';
import passport from 'passport';
import { requireRole } from '../middleware/auth.js';
import db from '../db.js';

const router = express.Router();

// Submit club join request
router.post('/', 
    passport.authenticate('jwt', {session: false}),
    async (req, res) => {
        try {
            const { klubId, message } = req.body;
            const userEmail = req.user.epost;
            
            if (!klubId) {
                return res.status(400).json({ message: 'Club ID is required' });
            }
            
            const result = await db.createClubJoinRequest(userEmail, klubId, message);
            res.status(201).json({ 
                message: 'Join request submitted successfully',
                requestId: result.id 
            });
        } catch (error) {
            console.error('Error submitting join request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Get pending join requests for a club (klubadmin only)
router.get('/club/:klubId', 
    passport.authenticate('jwt', {session: false}),
    requireRole('klubbadmin'),
    async (req, res) => {
        try {
            const requests = await db.getPendingClubJoinRequests(req.params.klubId);
            res.status(200).json(requests);
        } catch (error) {
            console.error('Error fetching join requests:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Approve a join request
router.post('/:id/approve',
    passport.authenticate('jwt', {session: false}),
    requireRole('klubbadmin'),
    async (req, res) => {
        try {
            await db.approveClubJoinRequest(req.params.id);
            res.status(200).json({ message: 'Join request approved' });
        } catch (error) {
            console.error('Error approving join request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Reject a join request
router.post('/:id/reject',
    passport.authenticate('jwt', {session: false}),
    requireRole('klubbadmin'),
    async (req, res) => {
        try {
            await db.rejectClubJoinRequest(req.params.id);
            res.status(200).json({ message: 'Join request rejected' });
        } catch (error) {
            console.error('Error rejecting join request:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

export default router;