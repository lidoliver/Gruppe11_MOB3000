// Firebase Firestore helper for messages
import admin, { firestore } from '../firebaseAdmin.js';

/**
 * Send a message to Firestore
 * This allows the backend to send messages that sync with the mobile app
 */
export async function sendFirestoreMessage(senderEmail, receiverEmail, content) {
    try {
        const conversationId = generateConversationId(senderEmail, receiverEmail);
        const db = firestore;

        // Add message to Firestore
        const messageRef = await db
            .collection('conversations')
            .doc(conversationId)
            .collection('messages')
            .add({
                senderEmail,
                receiverEmail,
                content,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                isRead: false
            });

        console.log(`📨 Message sent to Firestore: ${messageRef.id}`);

        // Update conversation metadata
        await updateConversationMetadata(conversationId, senderEmail, receiverEmail, content);

        return messageRef.id;
    } catch (error) {
        console.error('❌ Error sending Firestore message:', error);
        throw error;
    }
}

/**
 * Update conversation metadata
 */
async function updateConversationMetadata(conversationId, senderEmail, receiverEmail, lastMessage) {
    try {
        const db = firestore;
        const conversationRef = db.collection('conversations').doc(conversationId);
        const conversationDoc = await conversationRef.get();

        const updateData = {
            participants: [senderEmail, receiverEmail],
            lastMessage,
            lastMessageTime: admin.firestore.FieldValue.serverTimestamp(),
            lastMessageSender: senderEmail
        };

        if (conversationDoc.exists) {
            // Increment unread count for receiver
            const currentUnread = conversationDoc.data()[`unreadCount_${receiverEmail}`] || 0;
            updateData[`unreadCount_${receiverEmail}`] = currentUnread + 1;

            await conversationRef.update(updateData);
        } else {
            // Create new conversation
            updateData[`unreadCount_${receiverEmail}`] = 1;
            updateData[`unreadCount_${senderEmail}`] = 0;
            updateData[`contactName_${senderEmail}`] = senderEmail.split('@')[0];
            updateData[`contactName_${receiverEmail}`] = receiverEmail.split('@')[0];

            await conversationRef.set(updateData);
        }

        console.log(`✅ Conversation metadata updated for ${conversationId}`);
    } catch (error) {
        console.error('❌ Error updating conversation metadata:', error);
    }
}

/**
 * Generate consistent conversation ID for two users
 */
function generateConversationId(email1, email2) {
    const sorted = [email1, email2].sort();
    return `${sorted[0]}_${sorted[1]}`;
}

/**
 * Migrate existing PostgreSQL messages to Firestore
 * Run this once to migrate old messages
 */
export async function migrateMessagesToFirestore(messages) {
    try {
        let migrated = 0;

        for (const msg of messages) {
            await sendFirestoreMessage(
                msg.sender_email,
                msg.receiver_email,
                msg.content
            );
            migrated++;
        }

        console.log(`✅ Migrated ${migrated} messages to Firestore`);
        return migrated;
    } catch (error) {
        console.error('❌ Error migrating messages:', error);
        throw error;
    }
}

