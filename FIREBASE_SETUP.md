# Firebase Setup Guide

This guide will help team members set up Firebase for both the Android app and backend server.

## Prerequisites

- Node.js 18.0.0 or higher
- npm 9.0.0 or higher
- Android Studio
- Access to the Firebase project (mob3000-81751)

## Table of Contents

1. [Getting Firebase Console Access](#1-getting-firebase-console-access)
2. [Android App Setup](#2-android-app-setup)
3. [Backend Server Setup](#3-backend-server-setup)
4. [Verification](#4-verification)
5. [Troubleshooting](#5-troubleshooting)

---

## 1. Getting Firebase Console Access

To download the necessary configuration files, you need access to the Firebase project.

### Request Access
1. Ask the project admin to add you to the Firebase project
2. Provide your Google account email
3. You'll receive an invitation email from Firebase
4. Accept the invitation and you'll have access to the console

### Firebase Console
- URL: https://console.firebase.google.com/
- Project Name: **mob3000-81751**

---

## 2. Android App Setup

### Download `google-services.json`

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select the **mob3000-81751** project
3. Click the **Settings gear icon** → **Project settings**
4. Scroll down to **Your apps** section
5. Find the Android app (look for the Android icon)
6. Click **Download google-services.json** button
7. Save the file

### Place the File

Copy `google-services.json` to the following location:
```
Gruppe11_MOB3000/
└── app/
    └── google-services.json  ← Place it here
```

**Important:** This file is already in `.gitignore` and should **NEVER** be committed to Git.

### Verify Android Setup

1. Open the project in Android Studio
2. Check that `app/google-services.json` exists
3. Build the project - it should compile without Firebase-related errors

---

## 3. Backend Server Setup

### Download Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select the **mob3000-81751** project
3. Click **Settings gear icon** → **Project settings**
4. Go to the **Service accounts** tab
5. Click **Generate new private key**
6. Confirm by clicking **Generate key**
7. A JSON file will be downloaded - this is your `serviceAccountKey.json`

### Place the File

Copy `serviceAccountKey.json` to the backend folder:
```
Gruppe11_MOB3000/
└── backend/
    └── serviceAccountKey.json  ← Place it here
```

**Important:** This file contains sensitive credentials and is already in `.gitignore`. It should **NEVER** be committed to Git.

### Install Dependencies

Open a terminal in the backend directory and run:

```bash
cd backend
npm install
```

### Start the Backend Server

```bash
npm start
```

Or for development with auto-restart:
```bash
npm run watch
```

You should see:
```
Firebase Admin initialized successfully
Server running on port XXXX
```

---

## 4. Verification

### Check Android App
1. Build the project in Android Studio
2. Look for Firebase initialization in logs
3. Test Firebase features (authentication, notifications, etc.)

### Check Backend Server
1. Start the server with `npm start`
2. Look for "Firebase Admin initialized successfully" message
3. Test endpoints that use Firebase services

### Common Verification Issues
- **File not found errors**: Check that files are in the correct directories
- **Permission errors**: Ensure the service account has proper roles in Firebase
- **Network errors**: Check your internet connection and firewall settings

---

## 5. Troubleshooting

### "google-services.json not found"
- Ensure the file is in `app/` directory (not `app/src/`)
- Rebuild the project in Android Studio
- Check that the file is not in `.gitignore` accidentally

### "serviceAccountKey.json not found"
- Ensure the file is in `backend/` directory
- Check file permissions (should be readable)
- Verify the filename matches exactly: `serviceAccountKey.json`

### "Invalid credentials" or "Authentication failed"
- Download a fresh service account key from Firebase Console
- Ensure the key corresponds to the correct project (mob3000-81751)
- Check that the JSON file is valid (not corrupted)

### Firebase project mismatch
- Verify `projectId: 'mob3000-81751'` in `backend/firebaseAdmin.js`
- Check that your `google-services.json` is from the correct project
- Look for project ID in both files - they should match

### Permission denied in Firebase Console
- Contact the project admin to grant you appropriate access
- You need at least "Viewer" role to download configuration files
- For generating service account keys, you need "Editor" or higher

---

## Security Best Practices

### ⚠️ NEVER Do This:
- ❌ Commit `google-services.json` to Git
- ❌ Commit `serviceAccountKey.json` to Git
- ❌ Share credentials via email or chat
- ❌ Post credentials in public channels
- ❌ Upload credentials to public file sharing services

### ✅ Always Do This:
- ✅ Keep credentials in `.gitignore`
- ✅ Download fresh files from Firebase Console
- ✅ Use secure channels for sharing (if absolutely necessary)
- ✅ Rotate service account keys if compromised
- ✅ Request your own Firebase Console access

---

## Additional Resources

- [Firebase Console](https://console.firebase.google.com/)
- [Firebase Admin SDK Documentation](https://firebase.google.com/docs/admin/setup)
- [Firebase Android Setup Guide](https://firebase.google.com/docs/android/setup)

---

## Need Help?

If you encounter issues not covered in this guide:
1. Check the Firebase Console for project status
2. Verify your access permissions
3. Contact the project admin or team lead
4. Check Firebase status page: https://status.firebase.google.com/

---

**Last Updated:** November 23, 2025

