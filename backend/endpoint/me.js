import express from 'express';
import passport from 'passport';
import db from '../db.js';

const router = express.Router();

// Get current user's role
router.get('/me', passport.authenticate('jwt', { session: false }), async (req, res) => {
    console.log('Authenticated user:', req.user); // Debugging
    try {
        const user = await db.getBruker(req.user.epost);
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }
        res.status(200).json({ role: user.role });
    } catch (error) {
        console.error('Error fetching user role:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

export default router;