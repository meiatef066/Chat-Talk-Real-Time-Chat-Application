-- PostgreSQL schema for ChatTalk (clean, production-ready)
-- Note: uses bigint IDs to align with backend Long identifiers

-- =====================
-- Users
-- =====================
CREATE TABLE IF NOT EXISTS users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    phone_number        VARCHAR(50),
    country             VARCHAR(100),
    gender              VARCHAR(20) CHECK (gender IN ('MALE','FEMALE','OTHER')),
    bio                 VARCHAR(500),
    address             VARCHAR(255),
    profile_picture_url VARCHAR(512),
    role                VARCHAR(50)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
    verified            BOOLEAN NOT NULL DEFAULT false,
    is_online           BOOLEAN NOT NULL DEFAULT false,
    last_seen_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- =====================
-- Contacts (friend relationships)
-- =====================
CREATE TABLE IF NOT EXISTS contacts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','BLOCKED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, contact_id)
);

CREATE INDEX IF NOT EXISTS idx_contacts_user ON contacts(user_id);
CREATE INDEX IF NOT EXISTS idx_contacts_contact ON contacts(contact_id);

-- =====================
-- Chats
-- =====================
CREATE TABLE IF NOT EXISTS chats (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100),
    chat_type       VARCHAR(20) NOT NULL CHECK (chat_type IN ('PRIVATE','GROUP')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================
-- Chat Participants
-- =====================
CREATE TABLE IF NOT EXISTS chat_participants (
    id              BIGSERIAL PRIMARY KEY,
    chat_id         BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    participant_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (participant_role IN ('MEMBER','ADMIN')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','LEFT')),
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at         TIMESTAMPTZ,
    UNIQUE (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_participants_chat ON chat_participants(chat_id);
CREATE INDEX IF NOT EXISTS idx_participants_user ON chat_participants(user_id);

-- =====================
-- Messages
-- =====================
CREATE TABLE IF NOT EXISTS messages (
    id              BIGSERIAL PRIMARY KEY,
    chat_id         BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    message_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT','IMAGE','FILE')),
    is_read         BOOLEAN NOT NULL DEFAULT false,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    edited_at       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_time ON messages(chat_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);

-- =====================
-- Notifications
-- =====================
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50) NOT NULL,
    payload         JSONB,
    content         TEXT,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(is_read);

-- =====================
-- Verification Codes (email verify / password reset)
-- =====================
CREATE TABLE IF NOT EXISTS verification_codes (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code            VARCHAR(20) NOT NULL,
    purpose         VARCHAR(30) NOT NULL CHECK (purpose IN ('EMAIL_VERIFY','PASSWORD_RESET')),
    expires_at      TIMESTAMPTZ NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verification_user ON verification_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_code ON verification_codes(code);

CREATE TABLE "User"(
    "Id" UUID NOT NULL,
    "first name" VARCHAR(255) NOT NULL,
    "last name" VARCHAR(255) NOT NULL,
    "email" VARCHAR(255) NOT NULL,
    "password" VARCHAR(255) NOT NULL,
    "phone number" VARCHAR(255) NOT NULL,
    "country" VARCHAR(255) NOT NULL,
    "is online" BOOLEAN NOT NULL,
    "gender" VARCHAR(255) CHECK
        ("gender" IN('')) NOT NULL,
        "profilePicture" VARCHAR(255) NOT NULL,
        "address" VARCHAR(255) NOT NULL,
        "bio" VARCHAR(255) NOT NULL,
        "role" VARCHAR(255)
    CHECK
        ("role" IN('')) NOT NULL,
        "created at" DATE NOT NULL
);
ALTER TABLE
    "User" ADD PRIMARY KEY("Id");
CREATE TABLE "contact"(
    "id" BIGINT NOT NULL,
    "user_id" BIGINT NOT NULL,
    "contact_id" BIGINT NOT NULL,
    "contact status" VARCHAR(255) CHECK
        (
            "contact status" IN('PENDING', 'ACCEPTED', 'BLOCKED')
        ) NOT NULL,
        "created at" TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL
);
ALTER TABLE
    "contact" ADD PRIMARY KEY("id");
COMMENT
ON COLUMN
    "contact"."user_id" IS 'The owner of the contact list';
CREATE TABLE "Message"(
    "id" BIGINT NOT NULL,
    "chat" BIGINT NOT NULL,
    "sender" BIGINT NOT NULL,
    "content" TEXT NOT NULL,
    "message_type" VARCHAR(255) CHECK
        ("message_type" IN('')) NOT NULL,
        "is read" BOOLEAN NOT NULL,
        "sent at" TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL
);
ALTER TABLE
    "Message" ADD PRIMARY KEY("id");
CREATE TABLE "chat"(
    "id" BIGINT NOT NULL,
    "name" VARCHAR(30) NOT NULL,
    "is group" BOOLEAN NOT NULL,
    "create at" TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL
);
ALTER TABLE
    "chat" ADD PRIMARY KEY("id");
CREATE TABLE "chat participation"(
    "id" BIGINT NOT NULL,
    "user id" BIGINT NOT NULL,
    "chat id" BIGINT NOT NULL,
    "join at" TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL
);
ALTER TABLE
    "chat participation" ADD PRIMARY KEY("id");
ALTER TABLE
    "chat participation" ADD CONSTRAINT "chat participation_chat id_foreign" FOREIGN KEY("chat id") REFERENCES "chat"("id");
ALTER TABLE
    "chat participation" ADD CONSTRAINT "chat participation_user id_foreign" FOREIGN KEY("user id") REFERENCES "User"("Id");
ALTER TABLE
    "Message" ADD CONSTRAINT "message_chat_foreign" FOREIGN KEY("chat") REFERENCES "chat"("id");
ALTER TABLE
    "contact" ADD CONSTRAINT "contact_user_id_foreign" FOREIGN KEY("user_id") REFERENCES "User"("Id");