# Backend Integration Setup Guide

## Overview
Your Android app is now connected to your Node.js/Express backend server. The app can communicate with your backend API to handle user authentication, profile management, and scorecard data.

## Backend Server Setup

### 1. Start Your Backend Server

Navigate to the backend directory and start the server:

```bash
cd backend
npm install  # if you haven't already
npm start
```

Your server should start on port 5000 (or the port specified in your .env file).

### 2. Database Connection

Make sure your PostgreSQL database is running and properly configured in your `.env` file:

```
DB_CONNECTION_STRING=your_connection_string_here
JWT_SECRET=your_jwt_secret_here
```

## Android App Configuration

### 1. Update Backend URL

The app is currently configured to connect to `http://10.0.2.2:5000/` which is the Android emulator's way of accessing `localhost:5000` on your development machine.

**File:** `app/src/main/java/my/application/data/remote/BackendApi.kt`

```kotlin
private const val BASE_URL = "http://10.0.2.2:5000/"
```

**Update for different scenarios:**

- **Android Emulator (default):** `http://10.0.2.2:5000/`
- **Physical Device (same network):** `http://YOUR_COMPUTER_IP:5000/` (e.g., `http://192.168.1.100:5000/`)
- **Production:** `https://your-backend-domain.com/`

To find your computer's IP address:
- **Windows:** Open Command Prompt and run `ipconfig`, look for IPv4 Address
- **Mac/Linux:** Open Terminal and run `ifconfig` or `ip addr show`

### 2. Network Permissions

The app already has the required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. Clear Text Traffic (Development Only)

If you're using HTTP (not HTTPS) for local development, you may need to allow clear text traffic.

Add to `AndroidManifest.xml` in the `<application>` tag:

```xml
android:usesCleartextTraffic="true"
```

**⚠️ Warning:** Remove this in production! Always use HTTPS for production apps.

## API Structure

### Available Endpoints

The app is configured to use these backend endpoints:

#### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login user

#### User Management
- `GET /bruker/{email}` - Get user profile (requires authentication)
- `PUT /bruker/{email}` - Update user profile (requires authentication)
- `DELETE /bruker/{email}` - Delete user (requires authentication)

#### Scorecards
- `GET /api/pointcard/{email}` - Get user's scorecards (requires authentication)

#### Players
- `GET /api/epost` - Get all players

### Data Models

**UserDto:**
```kotlin
data class UserDto(
    val email: String,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val role: String?,
    val clubId: Int?,
    val clubName: String?
)
```

**ScorecardDto:**
```kotlin
data class ScorecardDto(
    val id: Int,
    val userEmail: String,
    val courseId: Int?,
    val date: String,
    val totalScore: Int
)
```

## Using the Backend in Your App

### Example: User Repository

The `UserRepository` class provides methods to interact with the backend:

```kotlin
val userRepository = UserRepository.getInstance()

// Login
val loginResult = userRepository.login("user@example.com", "password")
if (loginResult.isSuccess) {
    val loginResponse = loginResult.getOrNull()
    // Token is automatically stored in the repository
}

// Get user profile
val userResult = userRepository.getUser("user@example.com")
if (userResult.isSuccess) {
    val user = userResult.getOrNull()
    // Use user data
}

// Get scorecards
val scorecardsResult = userRepository.getScorecards("user@example.com")
if (scorecardsResult.isSuccess) {
    val scorecards = scorecardsResult.getOrNull()
    // Display scorecards
}
```

### Example: Using in ViewModel

The `MyProfileViewModel` demonstrates how to use the repository in a ViewModel:

```kotlin
class MyProfileViewModel(
    private val userRepository: UserRepository = UserRepository.getInstance()
) : ViewModel() {
    
    fun loadUserProfile(email: String) {
        viewModelScope.launch {
            val userResult = userRepository.getUser(email)
            val scorecardsResult = userRepository.getScorecards(email)
            // Update UI state
        }
    }
}
```

### Example: Using in Composable

The `MyProfileScreen` shows how to use the ViewModel in a Composable:

```kotlin
@Composable
fun MyProfileScreen(
    navController: NavController,
    userEmail: String,
    viewModel: MyProfileViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    
    LaunchedEffect(userEmail) {
        viewModel.loadUserProfile(userEmail)
    }
    
    // Display user data from uiState
}
```

## Testing the Connection

### 1. Check Backend is Running

Make sure your backend server is running and accessible:

```bash
curl http://localhost:5000/api/epost
```

### 2. Check from Android Emulator

Run your Android app in the emulator and check the Logcat for network requests. Look for:
- HTTP request logs
- Response codes
- Error messages

### 3. Common Issues

**Cannot connect to server:**
- Verify backend is running
- Check BASE_URL is correct
- Ensure firewall allows connections
- For physical devices, ensure both devices are on the same network

**401 Unauthorized errors:**
- User needs to login first
- Check if auth token is being sent correctly

**Network security errors:**
- Add `android:usesCleartextTraffic="true"` for HTTP development
- Use HTTPS in production

## Next Steps

1. **Implement Login Screen:** Create a login screen that uses `UserRepository.login()`
2. **Store Auth Token:** Consider using DataStore or SharedPreferences to persist the auth token
3. **Add Error Handling:** Display user-friendly error messages
4. **Add Loading States:** Show loading indicators during network requests
5. **Implement Logout:** Clear auth token and navigate to login screen

## Security Considerations

- **Never commit sensitive data:** Keep `.env` files out of version control
- **Use HTTPS in production:** Always use secure connections
- **Validate inputs:** Sanitize user inputs before sending to backend
- **Handle tokens securely:** Don't log tokens, store them securely
- **Implement token refresh:** Add token refresh logic for long-lived sessions

## Resources

- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android Network Security](https://developer.android.com/training/articles/security-config)

