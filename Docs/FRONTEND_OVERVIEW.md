### ChatTalk Desktop (JavaFX) — Frontend Overview

This document summarizes the desktop client architecture, major screens, and how it integrates with the backend beyond registration/login.

—

## Architecture
- JavaFX 23 with FXML views and CSS styling
- MV* separation (controllers per screen)
- REST (auth/profile) + WebSocket (messaging, presence)
- Media: images (avatars), sounds for notifications

—

## Major Screens
- Sign Up & Login: registration, email verification, login
- Contacts & Search: search users, send/accept/reject friend requests
- Chats List: private and recent chats, unread counts
- Chat Window: real-time messaging, read markers, edit/delete, sounds
- Profile: view/update profile, change password, update avatar (Cloudinary)
- Notifications: list, mark as read, delete

—

## Backend Integration
- Auth: `/api/auth/*` (register, login, verify, refresh, logout)
- Profile: `/api/profile/me` (GET/PUT), `/picture`, `/change-password`
- Users: `/api/users/search`
- Contacts: `/api/contacts/*`
- Chats: `/api/chats/*` (list, create private, search, recent, participants, leave)
- Messages: `/api/chats/{chatId}/messages` (CRUD, read, unread)
- Notifications: `/api/notifications/*`
- WebSocket: real-time message delivery and presence

—

## Configuration
- Backend base URL and WebSocket endpoint configurable in client settings
- Requires Java 21; run with `mvn javafx:run`


