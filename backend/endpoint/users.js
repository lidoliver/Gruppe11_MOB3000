import express from 'express';
import passport from 'passport';
import { requireRole } from '../middleware/auth.js';
import db from '../db.js';

const router = express.Router();

// Get all users (admin only)
router.get('/', 
    passport.authenticate('jwt', { session: false }), 
    requireRole('admin'), 
    async (req, res) => {
        console.log('GET /users endpoint reached');
        try {
            console.log('Fetching all users from database');
            const users = await db.getAllUsers();
            console.log('Users retrieved:', users ? users.length : 'none');
            res.status(200).json(users || []);
        } catch (error) {
            console.error('Error fetching users:', error.message);
            console.error('Error stack:', error.stack);
            res.status(500).json({ message: 'Server error', error: error.message });
        }
    }
);

// Search users by email (admin only)
router.get('/search', 
    passport.authenticate('jwt', { session: false }), 
    requireRole('admin'), 
    async (req, res) => {
        const { email } = req.query;
        console.log('Search users by email:', email);
        
        try {
            const users = await db.searchUsers(email);
            console.log('Users found:', users ? users.length : 'none');
            res.status(200).json(users || []);
        } catch (error) {
            console.error('Error searching users:', error.message);
            console.error('Error stack:', error.stack);
            res.status(500).json({ message: 'Server error', error: error.message });
        }
    }
);


/*
    * Denne ruten lar brukeren søke etter andre brukere basert på e-postadresse.
    * Den bruker Passport for autentisering og henter søkeresultatene fra databasen.
    * Parametrene som sendes med forespørselen er brukerens e-postadresse og søketermen.
    * Utviklet av: [Martin Hansen]
    */
router.get('/search-users', 
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const searchTerm = req.query.q;
            
            if (!searchTerm || searchTerm.length < 2) {
                return res.status(400).json({ message: 'Search term must be at least 2 characters' });
            }
            
            // Get the current user's email
            const userEmail = req.user.email;
            
            // Get search results
            const results = await db.searchUsers(searchTerm);
            
            // Remove the current user from results (can't add yourself)
            const filteredResults = results.filter(user => user.epost !== userEmail);
            
            res.status(200).json(filteredResults);
        } catch (error) {
            console.error('Error searching users:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Add these new routes

// Delete a user (admin only)
router.delete('/:email', 
    passport.authenticate('jwt', { session: false }), 
    requireRole('admin'), 
    async (req, res) => {
        try {
            const email = req.params.email;
            
            // Check if user exists
            const user = await db.getBruker(email);
            if (!user) {
                return res.status(404).json({ message: 'User not found' });
            }
            
            // Don't allow deleting another admin
            if (user.role === 'admin') {
                return res.status(403).json({ message: 'Cannot delete admin users' });
            }
            
            const result = await db.deleteBruker(email);
            res.status(200).json({ message: 'User deleted successfully' });
        } catch (error) {
            console.error('Error deleting user:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Ban a user (admin only)
router.put('/:email/ban', 
    passport.authenticate('jwt', { session: false }), 
    requireRole('admin'), 
    async (req, res) => {
        try {
            const email = req.params.email;
            
            // Check if user exists
            const user = await db.getBruker(email);
            if (!user) {
                return res.status(404).json({ message: 'User not found' });
            }
            
            // Don't allow banning another admin
            if (user.role === 'admin') {
                return res.status(403).json({ message: 'Cannot ban admin users' });
            }
            
            // Update user role to 'banned' or create a banned flag
            await db.updateUserRole(email, 'banned');
            res.status(200).json({ message: 'User banned successfully' });
        } catch (error) {
            console.error('Error banning user:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Unban a user (admin only)
router.put('/:email/unban', 
    passport.authenticate('jwt', { session: false }), 
    requireRole('admin'), 
    async (req, res) => {
        try {
            const email = req.params.email;
            
            // Check if user exists
            const user = await db.getBruker(email);
            if (!user) {
                return res.status(404).json({ message: 'User not found' });
            }
            
            // Only unban if user is currently banned
            if (user.role !== 'banned') {
                return res.status(400).json({ message: 'User is not banned' });
            }
            
            // Update user role back to normal user
            await db.updateUserRole(email, 'user');
            res.status(200).json({ message: 'User unbanned successfully' });
        } catch (error) {
            console.error('Error unbanning user:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

export default router;