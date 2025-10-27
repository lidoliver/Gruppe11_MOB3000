import db from './db.js';

(async () => {
    try {
        const email = '123'; // Replace with the email you want to test
        const user = await db.getBruker(email);
        console.log('Test result for getBruker:', user);
    } catch (error) {
        console.error('Error testing getBruker:', error);
    } finally {
        process.exit(); // Exit the script
    }
})();