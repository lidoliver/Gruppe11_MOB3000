import express from 'express';
import passport from 'passport';
import db from '../db.js';
import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';
import { JWT_SECRET } from '../config.js';

dotenv.config();

const router = express.Router();

// Authentication middleware
export const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ error: "Access denied. No token provided." });
    }

    try {
        const verified = jwt.verify(token, JWT_SECRET);
        req.user = verified;
        next();
    } catch (err) {
        res.status(401).json({ error: "Invalid token" });
    }
};

// Role-based authorization middleware
export const requireRole = (role) => {
    return (req, res, next) => {
        console.log("User in request:", req.user);
        console.log("Required role:", role);
        console.log("User's role:", req.user?.role);
        
        if (!req.user || req.user.role !== role) {
            return res.status(403).json({ message: 'Access denied. You do not have the required role.' });
        }
        next();
    };
};

// Admin authorization middleware
export const requireAdmin = (req, res, next) => {
    if (!req.user || req.user.role !== 'admin') {
        return res.status(403).json({
            message: 'Access denied.',
        });
    }
    next();
};

// User info route
router.get('/me', passport.authenticate('jwt', {session: false}), (req, res) => {
    // req.user is automatically populated by passport
    res.json({ email: req.user.epost, role: req.user.role });
});

export default router;