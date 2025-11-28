import { db } from '../firebase-config.js';

/**
 * Encode email for Firebase key
 * Firebase keys cannot contain: . $ # [ ] /
 */
function encodeEmail(email) {
  return email.replace(/\./g, ',').replace(/@/g, '_at_');
}

/**
 * Decode Firebase key back to email
 */
function decodeEmail(key) {
  return key.replace(/,/g, '.').replace(/_at_/g, '@');
}

/**
 * Create a new multiplayer game session
 */
export async function createMultiplayerGame(sessionId, hostEmail, baneId, baneName) {
  const gameRef = db.ref(`games/${sessionId}`);
  const encodedEmail = encodeEmail(hostEmail);

  await gameRef.set({
    sessionId,
    baneId,
    baneName,
    hostEmail,
    status: 'waiting', // waiting, active, finished
    currentHole: 1,
    createdAt: Date.now(),
    players: {
      [encodedEmail]: {
        email: hostEmail,
        isHost: true,
        joinedAt: Date.now(),
        ready: false,
        scores: {}
      }
    }
  });

  return sessionId;
}

/**
 * Join an existing game
 */
export async function joinMultiplayerGame(sessionId, playerEmail) {
  const gameRef = db.ref(`games/${sessionId}`);
  const snapshot = await gameRef.once('value');

  if (!snapshot.exists()) {
    throw new Error('Game session not found');
  }

  const encodedEmail = encodeEmail(playerEmail);
  const playerRef = gameRef.child(`players/${encodedEmail}`);
  await playerRef.set({
    email: playerEmail,
    isHost: false,
    joinedAt: Date.now(),
    ready: false,
    scores: {}
  });

  return snapshot.val();
}

/**
 * Update player score for a hole
 */
export async function updatePlayerScore(sessionId, playerEmail, holeNumber, score) {
  const encodedEmail = encodeEmail(playerEmail);
  const scoreRef = db.ref(`games/${sessionId}/players/${encodedEmail}/scores/${holeNumber}`);
  await scoreRef.set({
    score,
    timestamp: Date.now()
  });
}

/**
 * Set player ready status
 */
export async function setPlayerReady(sessionId, playerEmail, ready) {
  const encodedEmail = encodeEmail(playerEmail);
  const readyRef = db.ref(`games/${sessionId}/players/${encodedEmail}/ready`);
  await readyRef.set(ready);
}

/**
 * Move to next hole (host only)
 */
export async function moveToNextHole(sessionId, hostEmail) {
  const gameRef = db.ref(`games/${sessionId}`);
  const snapshot = await gameRef.once('value');
  const game = snapshot.val();

  if (game.hostEmail !== hostEmail) {
    throw new Error('Only host can advance holes');
  }

  await gameRef.child('currentHole').set(game.currentHole + 1);

  // Reset ready status for all players
  const playersRef = gameRef.child('players');
  const playersSnapshot = await playersRef.once('value');
  const players = playersSnapshot.val();

  for (const encodedEmail in players) {
    await playersRef.child(`${encodedEmail}/ready`).set(false);
  }
}

/**
 * Start the game (host only)
 */
export async function startGame(sessionId, hostEmail) {
  const gameRef = db.ref(`games/${sessionId}`);
  const snapshot = await gameRef.once('value');
  const game = snapshot.val();

  if (game.hostEmail !== hostEmail) {
    throw new Error('Only host can start game');
  }

  await gameRef.child('status').set('active');
}

/**
 * End the game
 */
export async function endGame(sessionId) {
  const gameRef = db.ref(`games/${sessionId}`);
  await gameRef.child('status').set('finished');

  // Calculate final scores
  const snapshot = await gameRef.once('value');
  const game = snapshot.val();
  const finalScores = {};

  for (const encodedEmail in game.players) {
    const playerEmail = game.players[encodedEmail].email; // Use the email stored in player data
    let totalScore = 0;
    const scores = game.players[encodedEmail].scores || {};

    for (const hole in scores) {
      totalScore += scores[hole].score || 0;
    }

    finalScores[playerEmail] = totalScore;
  }

  await gameRef.child('finalScores').set(finalScores);
  return finalScores;
}

/**
 * Remove player from game
 */
export async function removePlayer(sessionId, playerEmail) {
  const encodedEmail = encodeEmail(playerEmail);
  const playerRef = db.ref(`games/${sessionId}/players/${encodedEmail}`);
  await playerRef.remove();
}

/**
 * Listen to game updates (for real-time sync)
 */
export function subscribeToGame(sessionId, callback) {
  const gameRef = db.ref(`games/${sessionId}`);

  gameRef.on('value', (snapshot) => {
    callback(snapshot.val());
  });

  // Return unsubscribe function
  return () => gameRef.off();
}

/**
 * Delete game session
 */
export async function deleteGameSession(sessionId) {
  const gameRef = db.ref(`games/${sessionId}`);
  await gameRef.remove();
}
