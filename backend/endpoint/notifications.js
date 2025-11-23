// Push Notification endpoint using Firebase Cloud Messaging
import express from 'express';
import { auth as firebaseAuth } from '../firebaseAdmin.js';
import admin from '../firebaseAdmin.js';
import db from '../db.js';
import { sql } from '../db.js';

const router = express.Router();

/**
 * Register FCM token for a user
 * POST /api/notifications/register-token
 * Body: { userEmail, fcmToken }
 */
router.post('/register-token', async (req, res) => {
    try {
        const { userEmail, fcmToken } = req.body;

        if (!userEmail || !fcmToken) {
            return res.status(400).json({ error: 'userEmail and fcmToken are required' });
        }

        // Store the FCM token in your database
        await sql`
            INSERT INTO mydb.user_fcm_tokens (user_epost, fcm_token, created_at, updated_at)
            VALUES (${userEmail}, ${fcmToken}, NOW(), NOW())
            ON CONFLICT (user_epost)
            DO UPDATE SET
                fcm_token = ${fcmToken},
                updated_at = NOW()
        `;


        res.json({
            success: true,
            message: 'FCM token registered successfully'
        });
    } catch (error) {
        console.error('Error registering FCM token:', error);
        res.status(500).json({ error: 'Failed to register FCM token' });
    }
});

/**
 * Send notification to a specific user
 * POST /api/notifications/send
 * Body: { userEmail, title, body, data }
 */
router.post('/send', async (req, res) => {
    try {
        const { userEmail, title, body, data } = req.body;

        if (!userEmail || !title || !body) {
            return res.status(400).json({ error: 'userEmail, title, and body are required' });
        }

        // Get the user's FCM token from database
        const tokenResult = await sql`
            SELECT fcm_token
            FROM mydb.user_fcm_tokens
            WHERE user_epost = ${userEmail}
        `;

        if (tokenResult.length === 0) {
            return res.status(404).json({ error: 'No FCM token found for user' });
        }

        const fcmToken = tokenResult[0].fcm_token;

        // Send the notification
        const message = {
            notification: {
                title,
                body,
            },
            data: data || {},
            token: fcmToken,
        };

        const response = await admin.messaging().send(message);

        res.json({
            success: true,
            messageId: response,
            message: 'Notification sent successfully'
        });
    } catch (error) {
        console.error('Error sending notification:', error);
        res.status(500).json({ error: 'Failed to send notification' });
    }
});

/**
 * Send notification to multiple users
 * POST /api/notifications/send-multiple
 * Body: { userIds: [], title, body, data }
 */
router.post('/send-multiple', async (req, res) => {
    try {
        const { userEmails, title, body, data } = req.body;

        if (!userEmails || !Array.isArray(userEmails) || userEmails.length === 0) {
            return res.status(400).json({ error: 'userEmails array is required' });
        }

        if (!title || !body) {
            return res.status(400).json({ error: 'title and body are required' });
        }

        // Get FCM tokens for all users
        const tokenResult = await sql`
            SELECT fcm_token
            FROM mydb.user_fcm_tokens
            WHERE user_epost = ANY(${userEmails})
        `;

        if (tokenResult.length === 0) {
            return res.status(404).json({ error: 'No FCM tokens found for users' });
        }

        const tokens = tokenResult.map(row => row.fcm_token);

        // Send to multiple devices
        const message = {
            notification: {
                title,
                body,
            },
            data: data || {},
            tokens,
        };

        const response = await admin.messaging().sendEachForMulticast(message);

        res.json({
            success: true,
            successCount: response.successCount,
            failureCount: response.failureCount,
            message: 'Notifications sent'
        });
    } catch (error) {
        console.error('Error sending notifications:', error);
        res.status(500).json({ error: 'Failed to send notifications' });
    }
});

/**
 * Send notification when a new message is received
 * Helper function to be used by your message endpoint
 */
export async function sendMessageNotification(recipientEmail, senderEmail, senderName, messageText) {
    try {
        // Get the recipient's FCM token
        const tokenResult = await sql`
            SELECT fcm_token
            FROM mydb.user_fcm_tokens
            WHERE user_epost = ${recipientEmail}
        `;

        if (tokenResult.length === 0) {
            console.log(`⚠️ No FCM token found for user ${recipientEmail} - user needs to register their device`);
            return null;
        }

        const fcmToken = tokenResult[0].fcm_token;

        // Send the notification
        const message = {
            notification: {
                title: senderName,
                body: messageText,
            },
            data: {
                type: 'message',
                senderEmail: senderEmail,
                senderName,
                message: messageText,
            },
            token: fcmToken,
        };

        const response = await admin.messaging().send(message);
        console.log(`✅ Message notification sent to ${recipientEmail}:`, response);
        return response;
    } catch (error) {
        console.error('Error sending message notification:', error);
        return null;
    }
}

/**
 * Send notification for friend request
 */
export async function sendFriendRequestNotification(recipientEmail, senderEmail, senderName) {
    try {
        const tokenResult = await sql`
            SELECT fcm_token
            FROM mydb.user_fcm_tokens
            WHERE user_epost = ${recipientEmail}
        `;

        if (tokenResult.length === 0) {
            return null;
        }

        const fcmToken = tokenResult[0].fcm_token;

        const message = {
            notification: {
                title: 'New Friend Request',
                body: `${senderName} wants to be your friend`,
            },
            data: {
                type: 'friend_request',
                senderEmail: senderEmail,
                senderName,
            },
            token: fcmToken,
        };

        return await admin.messaging().send(message);
    } catch (error) {
        console.error('Error sending friend request notification:', error);
        return null;
    }
}

/**
 * Send notification for club invitation
 */
export async function sendClubInviteNotification(recipientEmail, clubName, inviterName) {
    try {
        const tokenResult = await sql`
            SELECT fcm_token
            FROM mydb.user_fcm_tokens
            WHERE user_epost = ${recipientEmail}
        `;

        if (tokenResult.length === 0) {
            return null;
        }

        const fcmToken = tokenResult[0].fcm_token;

        const message = {
            notification: {
                title: 'Club Invitation',
                body: `${inviterName} invited you to join ${clubName}`,
            },
            data: {
                type: 'club_invite',
                clubName,
                inviterName,
            },
            token: fcmToken,
        };

        return await admin.messaging().send(message);
    } catch (error) {
        console.error('Error sending club invite notification:', error);
        return null;
    }
}

export default router;

