# Spring Boot Notes API - Beginner-Friendly Guide

## What Is This Project?

This is a **Notes API** built with **Spring Boot** and **Kotlin**. It lets users:
- Register and log in securely
- Create, view, and delete personal notes
- Each note has a title, content, and color

Think of it like a backend for a simple note-taking app (similar to Google Keep or Apple Notes).

---

## How Does It Work?

### 1. **User Authentication (Login/Register)**
- Users create an account with their email and password
- Password is **hashed** (encrypted) before saving to database for security
- After login, users get two **tokens**:
  - **Access Token**: Used to access protected features (expires in 15 minutes)
  - **Refresh Token**: Used to get a new access token when it expires (lasts 30 days)

### 2. **Notes Management**
- Users can create notes with a title, content, and color
- Each note is private - only the owner can see/edit/delete it
- Notes are stored in **MongoDB** database

### 3. **Security**
- Uses **JWT (JSON Web Tokens)** to verify users
- Public endpoints: Registration, login, health check
- Protected endpoints: Creating/viewing/deleting notes (requires login)

---

## Technology Stack (What's Used)

| Technology | Purpose | Simple Explanation |
|------------|---------|-------------------|
| **Spring Boot** | Framework | Makes building web applications easier |
| **Kotlin** | Programming Language | Modern language that runs on Java |
| **MongoDB** | Database | Stores users and notes data |
| **JWT** | Authentication | Digital "ID cards" for logged-in users |
| **BCrypt** | Password Hashing | Encrypts passwords so they're stored safely |
| **Swagger/OpenAPI** | API Documentation | Interactive webpage to test the API |

---

## Project Structure (Where's Everything?)

```
src/main/kotlin/org/oxoho/note/
├── controllers/         # Handles HTTP requests (like GET, POST)
│   ├── AuthController       # Login, register, refresh token
│   ├── NoteController       # Create, view, delete notes
│   └── StatusController     # Health check endpoint
│
├── service/            # Business logic (the "brain" of the app)
│   ├── AuthService          # Handles user registration & login
│   └── NoteService          # Handles note operations
│
├── security/           # Security configuration
│   ├── SecurityConfig       # Defines which URLs need authentication
│   ├── JwtService           # Creates and validates JWT tokens
│   ├── JwtAuthFilter        # Checks tokens on every request
│   └── HashEncoder          # Encrypts passwords with BCrypt
│
├── database/           # Database models and repositories
│   ├── model/
│   │   ├── User             # User data structure
│   │   ├── Note             # Note data structure
│   │   └── RefreshToken     # Refresh token data structure
│   └── repository/          # Database access layer
│
├── dto/                # Data Transfer Objects (request/response formats)
│   ├── AuthDto              # Login/register request formats
│   └── NoteDto              # Note request/response formats
│
├── exception/          # Custom error types
│   └── CustomExceptions     # Defines app-specific errors
│
└── config/             # App configuration
    ├── JwtProperties        # JWT settings from application.properties
    └── OpenApiConfig        # Swagger documentation setup
```

---

## API Endpoints (How to Use It)

### **Authentication Endpoints** (No login required)

#### 1. Register a New User
```
POST /api/v1/auth/register
Body: {
  "email": "user@example.com",
  "password": "MyPassword123"
}
```
**Password Requirements**: At least 9 characters, with uppercase, lowercase, and a number

#### 2. Login
```
POST /api/v1/auth/login
Body: {
  "email": "user@example.com",
  "password": "MyPassword123"
}
Response: {
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

#### 3. Refresh Access Token
```
POST /api/v1/auth/refresh
Body: {
  "refreshToken": "eyJhbGc..."
}
Response: {
  "accessToken": "new_token...",
  "refreshToken": "new_refresh_token..."
}
```

### **Notes Endpoints** (Login required - include token in header)

**Important**: Add this header to all notes requests:
```
Authorization: Bearer YOUR_ACCESS_TOKEN
```

#### 1. Create or Update Note
```
POST /api/v1/notes
Body: {
  "id": null,            // Leave null for new note, or provide ID to update
  "title": "My Note",
  "content": "Note content here",
  "color": 4294951175    // Color as a number (e.g., hex color converted)
}
```

#### 2. Get All Your Notes
```
GET /api/v1/notes
Response: [
  {
    "id": "507f1f77bcf86cd799439011",
    "title": "My Note",
    "content": "Note content",
    "color": 4294951175,
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

#### 3. Delete a Note
```
DELETE /api/v1/notes/{noteId}
```

---

## How to Run This Project

### Prerequisites (What You Need Installed)
1. **Java 21** or higher
2. **MongoDB** (running locally or a cloud instance)
3. **Gradle** (included via wrapper)

### Step 1: Set Environment Variables
Create these environment variables (or add to your IDE run configuration):

```bash
# MongoDB connection string
MONGODB_CONNECTION_STRING=mongodb://localhost:27017/notes_db

# JWT secret key (base64 encoded - generate your own!)
JWT_SECRET_BASE64=YOUR_BASE64_SECRET_KEY_HERE
```

**How to generate a secret key**:
```bash
# On Linux/Mac
openssl rand -base64 32

# On Windows (PowerShell)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

### Step 2: Run the Application
```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

The app will start on **http://localhost:8085**

### Step 3: Test It Out
Visit **http://localhost:8085/swagger-ui.html** to see interactive API documentation and test endpoints directly in your browser!

---

## Configuration Explained

**File**: `src/main/resources/application.properties`

```properties
# App runs on port 8085 (instead of default 8080)
server.port=8085

# MongoDB connection (loaded from environment variable)
spring.data.mongodb.uri=${MONGODB_CONNECTION_STRING}

# JWT settings
jwt.secret=${JWT_SECRET_BASE64}           # Secret key for signing tokens
jwt.access-token-validity-ms=900000        # 15 minutes
jwt.refresh-token-validity-ms=2592000000   # 30 days

# Swagger UI available at /swagger-ui.html
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## Security Features Explained

### 1. **Password Security**
- Passwords are **never** stored in plain text
- Uses **BCrypt** hashing (industry standard)
- Even if database is compromised, passwords can't be recovered

### 2. **JWT Token System**
- **Access tokens** are short-lived (15 min) to limit damage if stolen
- **Refresh tokens** are long-lived (30 days) but stored in database (can be revoked)
- Refresh tokens are **hashed** in database for extra security
- Tokens use **one-time use** pattern (old refresh token deleted when used)

### 3. **Authorization**
- Each note belongs to a specific user
- Users can **only** see/edit/delete their own notes
- User ID is extracted from the JWT token to verify ownership

---

## Common Issues & Solutions

### Problem: "Invalid credentials" when logging in
- **Solution**: Make sure email and password match what you registered with
- Check if registration was successful first

### Problem: "Unauthorized" when accessing notes
- **Solution**: Make sure you're including the access token in the `Authorization` header
- Format: `Bearer YOUR_TOKEN_HERE`
- Token might be expired - use refresh token endpoint to get a new one

### Problem: Can't connect to MongoDB
- **Solution**: Verify MongoDB is running and `MONGODB_CONNECTION_STRING` environment variable is set correctly

### Problem: "Invalid token" errors
- **Solution**: Make sure `JWT_SECRET_BASE64` environment variable is set and doesn't change between restarts

---

## Testing the API

### Using Swagger UI (Easiest)
1. Go to http://localhost:8085/swagger-ui.html
2. Try endpoints interactively
3. Swagger remembers your token automatically

### Using Postman/Insomnia
1. Register a user
2. Login to get tokens
3. Add `Authorization: Bearer {accessToken}` header to protected requests
4. When access token expires, use refresh endpoint

### Using curl
```bash
# Register
curl -X POST http://localhost:8085/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test12345"}'

# Login
curl -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test12345"}'

# Create note (replace YOUR_TOKEN)
curl -X POST http://localhost:8085/api/v1/notes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"id":null,"title":"Test","content":"Hello","color":4294951175}'
```

---

## Key Concepts for Beginners

### What is a REST API?
It's a way for programs to talk to each other over the internet using standard HTTP methods:
- **GET**: Retrieve data
- **POST**: Create new data
- **PUT/PATCH**: Update data
- **DELETE**: Remove data

### What is JWT?
Think of it as a digital passport. When you log in, you get a token (passport) that proves who you are. You show this passport every time you want to access protected areas.

### What is MongoDB?
A database that stores data in a flexible JSON-like format (called documents). Unlike traditional databases with tables and rows, MongoDB uses collections and documents.

### What is Dependency Injection?
Spring Boot automatically creates and connects objects for you. Notice constructors like `AuthController(private val authService: AuthService)` - Spring automatically provides the `authService` instance.

---

## Next Steps

Want to learn more? Try:
1. Reading the inline code comments (being added to all files)
2. Modifying the password validation rules
3. Adding new fields to notes (like tags or categories)
4. Implementing note sharing between users
5. Adding pagination to the "get all notes" endpoint

---

## Support & Documentation

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Kotlin Docs**: https://kotlinlang.org/docs/home.html
- **JWT Guide**: https://jwt.io/introduction
- **MongoDB Docs**: https://docs.mongodb.com/

---

## License

This is a learning project - feel free to use and modify as needed!

[3D-Modell](Untitled.glb)
