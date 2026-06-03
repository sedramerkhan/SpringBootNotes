# Spring Boot Notes API

A REST API for managing personal notes, built with Kotlin, Spring Boot, and MongoDB. Supports user authentication via JWT with refresh tokens.

## Tech Stack

- **Kotlin** + **Spring Boot 4**
- **MongoDB** (Spring Data MongoDB)
- **Spring Security** + **JWT** (jjwt 0.12)
- **Kotlin Coroutines** + **Project Reactor**
- **Bean Validation** (jakarta.validation)

## Endpoints

### Auth — `/auth`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and receive access + refresh tokens |
| POST | `/auth/refresh` | Exchange a refresh token for a new token pair |

### Notes — `/notes` *(requires JWT)*

| Method | Path | Description |
|--------|------|-------------|
| POST | `/notes` | Create or update a note |
| GET | `/notes` | Get all notes for the authenticated user |
| GET | `/notes?important=true` | Get only important notes |
| GET | `/notes/{id}` | Get a note by ID |
| DELETE | `/notes/{id}` | Delete a note by ID |

## Setup

### Prerequisites

- JDK 17+
- MongoDB instance (local or Atlas)

### Environment Variables

```
MONGODB_CONNECTION_STRING=mongodb+srv://<user>:<password>@<host>/<db>
JWT_SECRET_BASE64=<base64-encoded-secret>
```

### Run

```bash
./gradlew bootRun
```

The server starts on `http://localhost:8080`.

## Auth Flow

1. Register at `/auth/register` with email + password.
2. Login at `/auth/login` to receive `accessToken` and `refreshToken`.
3. Pass `Authorization: Bearer <accessToken>` on all `/notes` requests.
4. Use `/auth/refresh` when the access token expires.

### Password Rules

Minimum 9 characters, with at least one uppercase letter, one lowercase letter, and one digit.