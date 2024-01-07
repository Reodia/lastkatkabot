CREATE TABLE ADMIN_USER (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE BLACKLISTED_USER (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE BLACKLISTED_CHAT (
    chat_id BIGINT PRIMARY KEY
);

CREATE TABLE SETTINGS (
    id VARCHAR(255) PRIMARY KEY,
    data VARCHAR(255) NOT NULL
);

CREATE TABLE BNC_GAME_MESSAGE (
    game_id BIGINT NOT NULL,
    message_id INT NOT NULL,
    PRIMARY KEY (game_id, message_id)
);

CREATE TABLE BNC_GAME_SAVE (
    id BIGINT PRIMARY KEY,
    game VARCHAR(1000) NOT NULL,
    edit_date TIMESTAMP NOT NULL
);

CREATE TABLE CAKE (
    id IDENTITY PRIMARY KEY,
    filling VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE FEEDBACK (
    id IDENTITY PRIMARY KEY,
    message VARCHAR(1000) NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    chat_id BIGINT NOT NULL,
    chat_title VARCHAR(100),
    message_id INT NOT NULL,
    replied BOOLEAN NOT NULL DEFAULT FALSE
);