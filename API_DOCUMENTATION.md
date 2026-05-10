# Vidya Prayag - API Documentation

This document outlines the available API endpoints in the Vidya Prayag server.

**Base URL**: `http://localhost:8080` (Development)

---

## 1. Authentication

### Signup
Create a new user account (Administrator or Parent).

- **URL**: `/auth/signup`
- **Method**: `POST`
- **Content-Type**: `application/json`

**Request Body**:
```json
{
  "name": "John Doe",
  "contact": "admin@academy.edu",
  "password": "securePassword123",
  "role": "ADMIN"
}
```
*Role options: `"ADMIN"`, `"PARENT"`*

**Success Response**:
- **Status**: `201 Created`
- **Body**:
```json
{
  "token": "mock-token-xxxx-xxxx",
  "userId": "uuid-string",
  "name": "John Doe",
  "role": "ADMIN"
}
```

**Error Responses**:
- `409 Conflict`: User already exists.
- `400 Bad Request`: Invalid request format.

---

### Login
Authenticate an existing user.

- **URL**: `/auth/login`
- **Method**: `POST`
- **Content-Type**: `application/json`

**Request Body**:
```json
{
  "contact": "admin@academy.edu",
  "password": "securePassword123",
  "role": "ADMIN"
}
```

**Success Response**:
- **Status**: `200 OK`
- **Body**:
```json
{
  "token": "mock-token-xxxx-xxxx",
  "userId": "uuid-string",
  "name": "John Doe",
  "role": "ADMIN"
}
```

**Error Responses**:
- `401 Unauthorized`: Invalid credentials or role.
- `400 Bad Request`: Invalid request format.

---

## 2. General

### Health Check / Greeting
A simple endpoint to verify if the server is running.

- **URL**: `/`
- **Method**: `GET`

**Success Response**:
- **Status**: `200 OK`
- **Body**: `Ktor: Hello from [Platform]!`

---

## Usage with Ktor Client (App)

To use these APIs in your Compose Multiplatform app, use the following pattern:

```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun signup(request: SignupRequest): AuthResponse {
    return client.post("http://localhost:8080/auth/signup") {
        setBody(request)
        contentType(ContentType.Application.Json)
    }.body()
}
```

*Note: Replace `localhost` with your machine\'s IP address when testing on physical Android/iOS devices.*
