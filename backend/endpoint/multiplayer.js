import express from 'express';
import {
  createMultiplayerGame,
  joinMultiplayerGame,
  updatePlayerScore,
  setPlayerReady,
  moveToNextHole,
  startGame,
  endGame,
  removePlayer,
  deleteGameSession
} from '../multiplayer/gameHandler.js';
import db from '../db.js';

const router = express.Router();

// Create new multiplayer game
router.post('/create', async (req, res) => {
  try {
    const { sessionId, baneId } = req.body;
    const hostEmail = req.session.epost;

    if (!hostEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    // Get track info
    const bane = await db.getBaneInfo(baneId);

    await createMultiplayerGame(sessionId, hostEmail, baneId, bane.navn);

    res.json({ success: true, sessionId });
  } catch (error) {
    console.error('Error creating multiplayer game:', error);
    res.status(500).json({ error: error.message });
  }
});

// Join existing game
router.post('/join/:sessionId', async (req, res) => {
  try {
    const { sessionId } = req.params;
    const playerEmail = req.session.epost;

    if (!playerEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    const game = await joinMultiplayerGame(sessionId, playerEmail);

    res.json({ success: true, game });
  } catch (error) {
    console.error('Error joining game:', error);
    res.status(500).json({ error: error.message });
  }
});

// Update score
router.post('/score', async (req, res) => {
  try {
    const { sessionId, holeNumber, score } = req.body;
    const playerEmail = req.session.epost;

    if (!playerEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    await updatePlayerScore(sessionId, playerEmail, holeNumber, score);

    res.json({ success: true });
  } catch (error) {
    console.error('Error updating score:', error);
    res.status(500).json({ error: error.message });
  }
});

// Set ready status
router.post('/ready', async (req, res) => {
  try {
    const { sessionId, ready } = req.body;
    const playerEmail = req.session.epost;

    if (!playerEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    await setPlayerReady(sessionId, playerEmail, ready);

    res.json({ success: true });
  } catch (error) {
    console.error('Error setting ready:', error);
    res.status(500).json({ error: error.message });
  }
});

// Next hole (host only)
router.post('/next-hole', async (req, res) => {
  try {
    const { sessionId } = req.body;
    const hostEmail = req.session.epost;

    if (!hostEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    await moveToNextHole(sessionId, hostEmail);

    res.json({ success: true });
  } catch (error) {
    console.error('Error moving to next hole:', error);
    res.status(500).json({ error: error.message });
  }
});

// Start game
router.post('/start', async (req, res) => {
  try {
    const { sessionId } = req.body;
    const hostEmail = req.session.epost;

    if (!hostEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    await startGame(sessionId, hostEmail);

    res.json({ success: true });
  } catch (error) {
    console.error('Error starting game:', error);
    res.status(500).json({ error: error.message });
  }
});

// End game
router.post('/end', async (req, res) => {
  try {
    const { sessionId } = req.body;

    const finalScores = await endGame(sessionId);

    res.json({ success: true, finalScores });
  } catch (error) {
    console.error('Error ending game:', error);
    res.status(500).json({ error: error.message });
  }
});

// Leave game
router.post('/leave', async (req, res) => {
  try {
    const { sessionId } = req.body;
    const playerEmail = req.session.epost;

    if (!playerEmail) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    await removePlayer(sessionId, playerEmail);

    res.json({ success: true });
  } catch (error) {
    console.error('Error leaving game:', error);
    res.status(500).json({ error: error.message });
  }
});

export default router;
