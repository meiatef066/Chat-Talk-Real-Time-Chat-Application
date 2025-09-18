### ChatTalk API Specification

This document lists REST endpoints exposed by the Spring Boot backend. All responses are JSON unless otherwise specified. Authenticated endpoints require Bearer JWT in the Authorization header.

—

## Conventions
- Base URL: http://localhost:8080
- Global prefix: `/api`
- Auth header: `Authorization: Bearer <ACCESS_TOKEN>`

—

## Authentication
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Register new user (email verification code sent) | No |
| POST | `/api/auth/login` | Login, returns access/refresh tokens | No |
| PUT | `/api/auth/verify-email` | Verify email by code | No |
| POST | `/api/auth/forget-password` | Send password reset code | No |
| POST | `/api/auth/reset-password` | Reset password via code | No |
| POST | `/api/auth/refresh` | Refresh access token using refresh token | No |
| DELETE | `/api/auth/logout` | Logout current user | Yes |

—

## Profile
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| GET | `/api/profile/me` | Get current user profile | Yes |
| PUT | `/api/profile/me` | Update profile fields | Yes |
| POST | `/api/profile/me/picture` | Upload/update profile picture (multipart) | Yes |
| PUT | `/api/profile/me/change-password` | Change password (old/new) | Yes |

—

## User Search
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| GET | `/api/users/search` | Search users with relationship status (query, page, size) | Yes |

—

## Contacts & Friends
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/api/contacts/requests` | Send friend request (`receiverEmail`) | Yes |
| GET | `/api/contacts/requests/pending` | List pending requests | Yes |
| POST | `/api/contacts/requests/{requestId}/accept` | Accept a request | Yes |
| POST | `/api/contacts/requests/{requestId}/reject` | Reject a request | Yes |
| GET | `/api/contacts/friends` | List accepted friends | Yes |

—

## Chats
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/api/chats/private` | Create or get private chat with `email2` | Yes |
| GET | `/api/chats/private` | List user private chats | Yes |
| GET | `/api/chats` | List all user chats | Yes |
| GET | `/api/chats/{chatId}` | Get chat by id | Yes |
| PUT | `/api/chats/{chatId}/name` | Update chat name (`newName`) | Yes |
| DELETE | `/api/chats/{chatId}` | Delete private chat | Yes |
| GET | `/api/chats/search` | Search chats by name (`q`, `page`, `size`) | Yes |
| GET | `/api/chats/type/{chatType}` | Get chats by type | Yes |
| GET | `/api/chats/recent` | Get recent chats (`limit`) | Yes |
| GET | `/api/chats/{chatId}/participants` | List participants | Yes |
| POST | `/api/chats/{chatId}/leave` | Leave chat | Yes |
| GET | `/api/chats/{chatId}/message-count` | Count messages in chat | Yes |
| GET | `/api/chats/count` | Count user chats | Yes |
| GET | `/api/chats/exists` | Check if private chat exists (`email2`) | Yes |

—

## Messages
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| GET | `/api/chats/{chatId}/messages` | Paginated messages (`page`, `size`) | Yes |
| POST | `/api/chats/{chatId}/messages` | Send message (JSON body) | Yes |
| PATCH | `/api/chats/{chatId}/read` | Mark chat as read | Yes |
| GET | `/api/chats/{chatId}/unread` | Get unread count for user | Yes |
| PATCH | `/api/chats/{chatId}/messages/{messageId}` | Edit message (`content`) | Yes |
| DELETE | `/api/chats/{chatId}/messages/{messageId}` | Delete message (`forEveryone` optional) | Yes |

—

## Notifications
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| GET | `/api/notifications` | List notifications for current user | Yes |
| DELETE | `/api/notifications` | Delete all notifications | Yes |
| DELETE | `/api/notifications/{id}` | Delete specific notification | Yes |
| PATCH | `/api/notifications/{id}/read` | Mark notification as read | Yes |

—

## User Deletion
| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| DELETE | `/api/users/{userId}` | Soft-delete a user by id | Yes |
| DELETE | `/api/users/{userId}/hard` | Hard-delete a user by id | Yes |
| GET | `/api/users/{userId}/can-delete` | Check if user can be deleted | Yes |
| DELETE | `/api/users/me` | Soft-delete current user | Yes |

—

## Error Handling
- Standardized response wrapper with `status`, `statusCode`, `message`, `data`.
- Validation errors return 400 with descriptive message.
- Secured endpoints return 401 if token is missing/invalid; 403 for insufficient privileges.

—

## OpenAPI/Swagger
- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`


