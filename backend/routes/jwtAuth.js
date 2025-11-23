import express from 'express';
import passport from 'passport';
import { Strategy as LocalStrategy } from 'passport-local';
import crypto from 'crypto';
import db from '../db.js';
import { requireRole}  from '../middleware/auth.js';

/*
    * Denne filen håndterer autentisering og autorisering av brukere.
    * Den bruker Passport for autentisering og har ruter for innlogging, registrering og rolleadministrasjon.
    * Utviklet av: [Martin Hansen og Sondre Moe]
    * 
*/
import { Strategy as JwtStrategy, ExtractJwt } from 'passport-jwt';
import jwt from 'jsonwebtoken';
import { JWT_SECRET } from '../config.js';

const router = express.Router();

// Update the Local Strategy for username/password login
passport.use(new LocalStrategy(
{
    usernameField: 'email',
    passwordField: 'password'
},
async function (usernameOrEmail, password, done) {
    try {
        // Determine if input is email or username
        const isEmail = usernameOrEmail.includes('@');
        
        // Get user from database based on input type
        let user;
        if (isEmail) {
            // Search by email
            user = await db.getBruker(usernameOrEmail);
        } else {
            // Search by username
            user = await db.getBrukerByUsername(usernameOrEmail);
        }
        
        if (!user) {
            return done(null, false, { message: 'Incorrect username or email.' });
        }
        
        // Check if user is banned
        if (user.role === 'banned') {
            return done(null, false, { message: 'This account has been banned' });
        }
        
        // Check password using bcrypt
        const passwordMatch = await db.comparePassword(password, user.passord);
        if (!passwordMatch) {
            return done(null, false, { message: 'Incorrect password.' });
        }
        
        return done(null, user);
    } catch (err) {
        return done(err);
    }
}
));

// JWT Strategy for token authentication
const jwtOptions = {
    jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
    secretOrKey: JWT_SECRET
};

passport.use(new JwtStrategy(jwtOptions, async function (jwt_payload, done) {
    console.log('JWT payload:', jwt_payload); // Debugging
    try {
        const user = await db.getBruker(jwt_payload.email);
        if (user) {
            // Check if user is banned
            if (user.role === 'banned') {
                return done(null, false, { message: 'User is banned' });
            }
            return done(null, user);
        } else {
            return done(null, false);
        }
    } catch (err) {
        return done(err, false);
    }
}));

// Login route
router.post('/login', async (req, res, next) => {
    try {
        const { email: usernameOrEmail, password } = req.body;
        
        // Check if input is email or username
        const isEmail = usernameOrEmail.includes('@');
        
        // Get user from database based on input type
        let user;
        if (isEmail) {
            user = await db.getBruker(usernameOrEmail);
        } else {
            user = await db.getBrukerByUsername(usernameOrEmail);
        }
        
        if (!user) {
            return res.status(401).json({ message: 'Incorrect username/email or password' });
        }
        
        // Check if user is banned
        if (user.role === 'banned') {
            return res.status(403).json({ message: 'This account has been banned' });
        }
        
        // Check password using bcrypt
        const passwordMatch = await db.comparePassword(password, user.passord);
        if (!passwordMatch) {
            return res.status(401).json({ message: 'Incorrect username/email or password' });
        }
        
        // Update last_active timestamp on successful login
        await db.updateLastActive(user.epost);

        // Create and send token
        const token = jwt.sign({ email: user.epost, role: user.role }, JWT_SECRET, { expiresIn: '24h' });
        
        // Return token AND actual email (this is the key change)
        return res.json({ 
            token,
            email: user.epost
        });
    } catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({ message: 'Server error during login' });
    }
});

// Register route - fixed to properly create users
router.post('/register', async (req, res) => {
    try {
        const {email, password} = req.body;

        // Check if user already exists
        const existingUser = await db.getBruker(email);
        if (existingUser) {
            return res.status(400).json({ message: 'User already exists' });
        }
        
        // Create new user
        const user = await db.createUser(email, password);
        
        res.status(201).json({message: 'User registered successfully'});
    } catch (err) {
        res.status(500).json({message: err.message});
    }
});

// Gi klubbadmin rolle
router.post('/grant-klubbadmin', 
    passport.authenticate('jwt', {session: false}), // This MUST come first
    requireRole('admin'),                          // This should come second
    async (req, res) => {
        const { email, klubb_id } = req.body;
        
        try {
            const user = await db.getBruker(email);
            if (!user) {
                return res.status(404).json({ message: 'User not found' });
            }

            // Validate klub_id exists if provided
            if (klubb_id) {
                const clubs = await db.getKlubber();
                const clubExists = clubs.some(club => club.id_klubb == klubb_id);
                if (!clubExists) {
                    return res.status(404).json({ message: 'Club not found' });
                }
            } else {
                return res.status(400).json({ message: 'Club ID is required' });
            }

            await db.updateUserRole(email, 'klubbadmin', klubb_id);
            res.status(200).json({ message: 'User granted klubbadmin role and associated with club' });
        } catch (error) {
            console.error(error);
            res.status(500).json({ message: 'Server error' });
        }
    }
);

// Demote a klubbadmin to regular user
router.post('/demote-klubbadmin', 
    passport.authenticate('jwt', {session: false}),
    requireRole('admin'),
    async (req, res) => {
        try {
            const { email } = req.body;
            
            if (!email) {
                return res.status(400).json({ message: 'Email is required' });
            }
            
            const user = await db.demoteKlubbAdmin(email);
            res.status(200).json({ 
                message: 'User demoted successfully',
                user
            });
        } catch (error) {
            console.error('Error demoting klubbadmin:', error);
            res.status(500).json({ message: error.message || 'Server error' });
        }
    }
);

router.put('/edit-club', requireRole('klubbadmin'), async (req, res) => {
    const { klubbId, newInfo } = req.body;
    try {
        await db.updateClubInfo(klubbId, newInfo);
        res.status(200).json({ message: 'Club info updated' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: 'Server error' });
    }
});

// Protected route - fixed syntax error with parentheses
router.get('/protected', passport.authenticate('jwt', { session: false }), (req, res) => {
    res.json({message: 'You are authenticated!', user: req.user});
});

export default router;