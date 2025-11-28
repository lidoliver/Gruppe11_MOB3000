import express from 'express';
import passport from 'passport';
import db from '../db.js';
import { requireRole } from '../middleware/auth.js';

const router = express.Router();

// Get all clubs (public)
router.get('/clubs',
  passport.authenticate('jwt', {session: false}),
  async (req, res) => {
    try {
      const clubs = await db.getAllClubs();
      res.status(200).json(clubs);
    } catch (error) {
      console.error('Error getting all clubs:', error);
      res.status(500).json({ message: 'Server error' });
    }
  }
);

// Get club by ID
router.get('/clubs/:clubId',
  passport.authenticate('jwt', {session: false}),
  async (req, res) => {
    try {
      const club = await db.getClubById(req.params.clubId);
      if (!club) {
        return res.status(404).json({ message: 'Club not found' });
      }
      res.status(200).json(club);
    } catch (error) {
      console.error('Error getting club:', error);
      res.status(500).json({ message: 'Server error' });
    }
  }
);

// Get club info for logged in klubbadmin
router.get('/my-club', 
  passport.authenticate('jwt', {session: false}),
  requireRole('klubbadmin'),
  async (req, res) => {
    try {
      const userEmail = req.user.epost;
      const club = await db.getClubByAdminEmail(userEmail);
      
      if (!club) {
        return res.status(404).json({ message: 'Club not found' });
      }
      
      res.status(200).json(club);
    } catch (error) {
      console.error('Error getting admin club:', error);
      res.status(500).json({ message: 'Server error' });
    }
  }
);

// Get club members
router.get('/clubs/:clubId/members',
  passport.authenticate('jwt', {session: false}),
  requireRole('klubbadmin'),
  async (req, res) => {
    try {
      const clubId = req.params.clubId;
      const userEmail = req.user.epost;
      
      // Verify the user is admin for this club
      const hasPermission = await db.isClubAdmin(userEmail, clubId);
      if (!hasPermission) {
        return res.status(403).json({ message: 'You are not authorized to view this club\'s members' });
      }
      
      const members = await db.getClubMembers(clubId);
      res.status(200).json(members);
    } catch (error) {
      console.error('Error getting club members:', error);
      res.status(500).json({ message: 'Server error' });
    }
  }
);

// Remove member from club
router.delete('/clubs/:clubId/members/:email',
  passport.authenticate('jwt', {session: false}),
  requireRole('klubbadmin'),
  async (req, res) => {
    try {
      const clubId = req.params.clubId;
      const memberEmail = req.params.email;
      const userEmail = req.user.epost;
      
      // Verify the user is admin for this club
      const hasPermission = await db.isClubAdmin(userEmail, clubId);
      if (!hasPermission) {
        return res.status(403).json({ message: 'You are not authorized to remove members from this club' });
      }
      
      // Prevent removing another klubbadmin
      const memberInfo = await db.getBruker(memberEmail);
      if (memberInfo.role === 'klubbadmin') {
        return res.status(403).json({ message: 'Cannot remove another club admin' });
      }
      
      await db.removeClubMember(memberEmail);
      res.status(200).json({ message: 'Member removed from club' });
    } catch (error) {
      console.error('Error removing club member:', error);
      res.status(500).json({ message: 'Server error' });
    }
  }
);

export default router;