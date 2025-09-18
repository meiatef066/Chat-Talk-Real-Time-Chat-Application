# ChatTalk - User Stories 

This document provides a clean and concise overview of the **ChatTalk** application's user stories. It outlines the key functionalities, acceptance criteria, and sequence flows for better understanding of the application's behavior.

---

## User Stories and Sequence Flow

### 1. Registration & Email Verification
- **User Story**: As a visitor, I can register with an email and password to create an account.
  - **Acceptance Criteria**:
    - Submitting valid data creates a user and sends a verification code via email.
    - Invalid or duplicate email returns a clear error message.
    - Flow: Frontend → POST `/api/auth/register` → Backend creates user → Sends email with code.

- **User Story**: As a user, I can verify my email using a verification code.
  - **Acceptance Criteria**:
    - Correct code verifies the account.
    - Expired or invalid code returns an error.
    - Flow: Frontend → PUT `/api/auth/verify-email` → Backend verifies code → Account activated.

---

### 2. Authentication & Session Management
- **User Story**: As a user, I can log in to receive an access token (JWT) and refresh token.
  - **Acceptance Criteria**:
    - Valid credentials return tokens.
    - Invalid credentials return a 401 error.
    - Flow: Frontend → POST `/api/auth/login` → Backend validates credentials → Returns tokens.

- **User Story**: As a user, I can refresh my access token.
  - **Acceptance Criteria**:
    - Valid refresh token returns a new access token.
    - Invalid or expired refresh token returns a 400 error.
    - Flow: Frontend → POST `/api/auth/refresh` → Backend validates refresh token → Returns new access token.

- **User Story**: As a user, I can log out.
  - **Acceptance Criteria**:
    - Session is invalidated, last-seen updated, and protected APIs require a new login.
    - Flow: Frontend → POST `/api/auth/logout` → Backend invalidates session → Updates last-seen.

---

### 3. Profile Management
- **User Story**: As a user, I can view and update my profile (first/last name, bio, gender, DOB, phone).
  - **Acceptance Criteria**:
    - Only provided fields are updated.
    - Validation errors are descriptive.
    - Flow: Frontend → GET/PUT `/api/profile/me` → Backend retrieves/updates profile → Returns updated data.

- **User Story**: As a user, I can change my password.
  - **Acceptance Criteria**:
    - Requires old password.
    - Rejects weak or same-as-old passwords.
    - Flow: Frontend → PUT `/api/profile/password` → Backend validates old password → Updates password.

- **User Story**: As a user, I can upload a profile picture.
  - **Acceptance Criteria**:
    - Valid image uploads to Cloudinary, and URL is saved/returned.
    - Flow: Frontend → POST `/api/profile/picture` (multipart) → Backend uploads to Cloudinary → Saves URL.

---

### 4. User Discovery & Contacts
- **User Story**: As a user, I can search for other users and see their relationship status (PENDING/ACCEPTED/BLOCKED/NONE).
  - **Acceptance Criteria**:
    - Results are paginated and include status relative to the user.
    - Flow: Frontend → GET `/api/users/search` → Backend returns paginated user list with status.

- **User Story**: As a user, I can send a friend request and accept/reject incoming requests.
  - **Acceptance Criteria**:
    - Cannot send duplicate requests.
    - Status transitions (PENDING → ACCEPTED/REJECTED) are enforced.
    - Both parties see updated status.
    - Flow: Frontend → POST `/api/contacts/requests` → Backend creates request → Updates status for both users.

- **User Story**: As a user, I can list my friends.
  - **Acceptance Criteria**:
    - Only ACCEPTED relationships are listed.
    - Flow: Frontend → GET `/api/contacts/friends` → Backend returns list of friends.

---

### 5. Chats & Messaging
- **User Story**: As a user, I can create or resume a private chat with another user.
  - **Acceptance Criteria**:
    - Reuses existing private chat if available; otherwise, creates a new one.
    - Flow: Frontend → POST `/api/chats/private?email2=...` → Backend checks/creates chat → Returns chat ID.

- **User Story**: As a user, I can send and receive messages in real time.
  - **Acceptance Criteria**:
    - Messages appear instantly in recipient UI via WebSocket.
    - Flow: Frontend → WebSocket `/ws/messages` → Backend broadcasts message → Updates recipient UI.

- **User Story**: As a user, I can view message history with pagination.
  - **Acceptance Criteria**:
    - Messages are ordered newest-first with consistent pagination.
    - Flow: Frontend → GET `/api/chats/{chatId}/messages` → Backend returns paginated message history.

- **User Story**: As a user, I can edit or delete my messages.
  - **Acceptance Criteria**:
    - Only the author can edit/delete.
    - Edit stores updated timestamp; delete supports "for everyone" where allowed.
    - Flow: Frontend → PUT/DELETE `/api/chats/{chatId}/messages/{messageId}` → Backend updates/deletes message.

- **User Story**: As a user, I can see unread counts and mark chats as read.
  - **Acceptance Criteria**:
    - Read updates decrement unread counts and notify the server.
    - Flow: Frontend → POST `/api/chats/{chatId}/read` → Backend updates read status → Decrements unread count.

---

### 6. Notifications
- **User Story**: As a user, I receive notifications for friend requests and new messages.
  - **Acceptance Criteria**:
    - Notifications are persisted and can be listed, marked as read, or deleted.
    - Flow: Frontend → GET/DELETE/PATCH `/api/notifications` → Backend manages notifications → Returns updated list.

---

### 7. Account Deletion
- **User Story**: As a user, I can soft delete my account if permitted.
  - **Acceptance Criteria**:
    - Deletion is disallowed if the user is the sole admin of a group; errors explain why.
    - Flow: Frontend → DELETE `/api/users/me` → Backend checks constraints → Soft deletes account.

- **User Story**: As an admin, I can hard delete a user account.
  - **Acceptance Criteria**:
    - Removes user and related data irreversibly; guarded by authorization.
    - Flow: Frontend → DELETE `/api/users/{userId}` → Backend verifies admin role → Hard deletes account.

---

## Notes
- **Frontend**: Built with JavaFX, featuring screens for Auth (Register, Verify, Login), Contacts/Search, Chats List, Chat Window, Profile, and Notifications.
- **Backend**: Utilizes REST APIs for auth, profile, contacts, and notifications; WebSocket for real-time messaging and presence.
- **Database**: PostgreSQL with TypeORM for data persistence and relationships.