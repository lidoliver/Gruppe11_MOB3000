import express from 'express';
import passport from 'passport';
import db from '../db.js';

const router = express.Router();

// Helper to check if user is klubbadmin of the club
async function isKlubbAdmin(userEmail, klubId) {
    try {
        const user = await db.getBruker(userEmail);
        return user && user.role === 'klubbadmin' && user.klubb_id_klubb == klubId;
    } catch (error) {
        console.error('Error checking klubbadmin status:', error);
        return false;
    }
}

// Helper to check if user is member of the club
async function isClubMember(userEmail, klubId) {
    try {
        const user = await db.getBruker(userEmail);
        return user && user.klubb_id_klubb == klubId;
    } catch (error) {
        console.error('Error checking club membership:', error);
        return false;
    }
}

// Get club page info (announcements and events)
router.get('/:klubId',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const userEmail = req.user.epost;

            // Check if user is member of this club
            const isMember = await isClubMember(userEmail, klubId);
            if (!isMember) {
                return res.status(403).json({ message: 'You must be a member of this club to view its page' });
            }

            // Get club info
            const club = await db.getClubById(klubId);
            if (!club) {
                return res.status(404).json({ message: 'Club not found' });
            }

            // Get announcements and events
            const announcements = await db.getClubAnnouncements(klubId, 20);
            const events = await db.getClubEvents(klubId);

            // Add participation status for each event
            const eventsWithParticipation = await Promise.all(
                events.map(async (event) => {
                    const isParticipating = await db.isUserParticipating(event.id, userEmail);
                    return {
                        ...event,
                        is_participating: isParticipating
                    };
                })
            );

            // Check if user is klubbadmin
            const isAdmin = await isKlubbAdmin(userEmail, klubId);

            res.status(200).json({
                club,
                announcements,
                events: eventsWithParticipation,
                isAdmin
            });
        } catch (error) {
            console.error('Error fetching club page:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Create announcement (klubbadmin only)
router.post('/:klubId/announcements',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const userEmail = req.user.epost;
            const { content } = req.body;

            if (!content) {
                return res.status(400).json({ message: 'Content is required' });
            }

            // Check if user is klubbadmin
            const isAdmin = await isKlubbAdmin(userEmail, klubId);
            if (!isAdmin) {
                return res.status(403).json({ message: 'Only club admins can create announcements' });
            }

            const announcement = await db.addClubAnnouncement(klubId, content, userEmail);
            res.status(201).json(announcement);
        } catch (error) {
            console.error('Error creating announcement:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Delete announcement (klubbadmin only)
router.delete('/:klubId/announcements/:announcementId',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const announcementId = parseInt(req.params.announcementId);
            const userEmail = req.user.epost;

            // Check if user is klubbadmin
            const isAdmin = await isKlubbAdmin(userEmail, klubId);
            if (!isAdmin) {
                return res.status(403).json({ message: 'Only club admins can delete announcements' });
            }

            await db.deleteClubAnnouncement(announcementId);
            res.status(200).json({ message: 'Announcement deleted' });
        } catch (error) {
            console.error('Error deleting announcement:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Create event (klubbadmin only)
router.post('/:klubId/events',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const userEmail = req.user.epost;
            const { title, description, eventDate } = req.body;

            console.log('Create event request:', { klubId, userEmail, title, description, eventDate });

            if (!title || !eventDate) {
                console.log('Missing required fields:', { title: !!title, eventDate: !!eventDate });
                return res.status(400).json({ message: 'Title and event date are required' });
            }

            // Check if user is klubbadmin
            const isAdmin = await isKlubbAdmin(userEmail, klubId);
            console.log('User admin check:', { userEmail, klubId, isAdmin });
            if (!isAdmin) {
                return res.status(403).json({ message: 'Only club admins can create events' });
            }

            console.log('Calling db.addClubEvent with:', { klubId, title, description, eventDate, userEmail });
            const event = await db.addClubEvent(klubId, title, description, eventDate, userEmail);
            console.log('Event created successfully:', event);
            res.status(201).json(event);
        } catch (error) {
            console.error('Error creating event:', error);
            console.error('Error stack:', error.stack);
            console.error('Error details:', { name: error.name, message: error.message });
            res.status(500).json({ message: 'Server error', error: error.message });
        }
    }
);

// Delete event (klubbadmin only)
router.delete('/:klubId/events/:eventId',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const eventId = parseInt(req.params.eventId);
            const userEmail = req.user.epost;

            // Check if user is klubbadmin
            const isAdmin = await isKlubbAdmin(userEmail, klubId);
            if (!isAdmin) {
                return res.status(403).json({ message: 'Only club admins can delete events' });
            }

            await db.deleteClubEvent(eventId);
            res.status(200).json({ message: 'Event deleted' });
        } catch (error) {
            console.error('Error deleting event:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Join event (club members)
router.post('/:klubId/events/:eventId/join',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const eventId = parseInt(req.params.eventId);
            const userEmail = req.user.epost;

            // Check if user is member of this club
            const isMember = await isClubMember(userEmail, klubId);
            if (!isMember) {
                return res.status(403).json({ message: 'Only club members can join events' });
            }

            const result = await db.joinEvent(eventId, userEmail);
            if (result) {
                res.status(200).json({ message: 'Successfully joined event' });
            } else {
                res.status(200).json({ message: 'Already joined this event' });
            }
        } catch (error) {
            console.error('Error joining event:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Leave event (club members)
router.delete('/:klubId/events/:eventId/join',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const eventId = parseInt(req.params.eventId);
            const userEmail = req.user.epost;

            // Check if user is member of this club
            const isMember = await isClubMember(userEmail, klubId);
            if (!isMember) {
                return res.status(403).json({ message: 'Only club members can leave events' });
            }

            await db.leaveEvent(eventId, userEmail);
            res.status(200).json({ message: 'Successfully left event' });
        } catch (error) {
            console.error('Error leaving event:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Get event participants
router.get('/:klubId/events/:eventId/participants',
    passport.authenticate('jwt', { session: false }),
    async (req, res) => {
        try {
            const klubId = parseInt(req.params.klubId);
            const eventId = parseInt(req.params.eventId);
            const userEmail = req.user.epost;

            // Check if user is member of this club
            const isMember = await isClubMember(userEmail, klubId);
            if (!isMember) {
                return res.status(403).json({ message: 'Only club members can view participants' });
            }

            const participants = await db.getEventParticipants(eventId);
            res.status(200).json(participants);
        } catch (error) {
            console.error('Error fetching participants:', error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

export default router;

