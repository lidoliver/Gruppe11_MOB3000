import db from '../db.js';

const checkRole = (requiredRole) => {
    return (req, res, next) => {
        // Role hierarchy: 'admin' > 'clubadmin' > 'user'
        const roleValues = {
            'user': 1,
            'clubadmin': 2,
            'admin': 3
        };
        
        if (!req.user) {
            return res.status(401).json({ message: 'Unauthorized - No user found' });
        }
        
        // Get user's role from JWT payload
        const userRole = req.user.role || 'user';
        
        // Check if user's role has sufficient permissions
        if (roleValues[userRole] >= roleValues[requiredRole]) {
            return next();
        } else {
            return res.status(403).json({ 
                message: 'Forbidden - Insufficient permissions' 
            });
        }
    };
};

// Check if user can edit a specific club
const canEditClub = async (req, res, next) => {
    try {
        const clubId = req.params.id || req.body.id_klubb;
        const userEmail = req.user.epost; 
        
        // If user is admin, allow access immediately
        if (req.user.role === 'admin') {
            return next();
        }
        
        // If user is club admin, check if they are admin of this club
        if (req.user.role === 'clubadmin') {
            const isClubAdmin = await db.isClubAdmin(userEmail, clubId);
            if (isClubAdmin) {
                return next();
            }
        }
        
        // If we got here, user doesn't have permission
        return res.status(403).json({ 
            message: 'Forbidden - You do not have permission to edit this club' 
        });
    } catch (error) {
        console.error('Error checking club permissions:', error);
        return res.status(500).json({ message: 'Server error' });
    }
};

// Check if user can edit a specific track
const canEditTrack = async (req, res, next) => {
    try {
        const trackId = req.params.id || req.body.id;
        const userEmail = req.user.epost;
        
        // If user is admin, allow access immediately
        if (req.user.role === 'admin') {
            return next();
        }
        
        if (req.user.role === 'klubbadmin') {
            const hasPermission = await db.isTrackOwnedByUserClub(userEmail, trackId);
            if (hasPermission) {
                return next();
            }
        }
        
        // If we got here, user doesn't have permission
        return res.status(403).json({ 
            message: 'Forbidden - You do not have permission to edit this track' 
        });
    } catch (error) {
        console.error('Error checking track permissions:', error);
        return res.status(500).json({ message: 'Server error' });
    }
};

export {
    checkRole,
    canEditClub,
    canEditTrack
};