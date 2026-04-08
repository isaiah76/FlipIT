CREATE DATABASE IF NOT EXISTS flipit;
USE flipit;

CREATE TABLE IF NOT EXISTS users (
id INT AUTO_INCREMENT PRIMARY KEY,
username VARCHAR(50) COLLATE utf8mb4_bin UNIQUE NOT NULL,
password VARCHAR(255) NOT NULL,
role ENUM('user', 'moderator', 'admin') DEFAULT 'user',
is_active BOOLEAN DEFAULT TRUE,
profile_picture MEDIUMBLOB NULL,
last_username_change TIMESTAMP NULL DEFAULT NULL,
last_password_change TIMESTAMP NULL DEFAULT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_blocks (
device_id VARCHAR(36),
username VARCHAR(50) COLLATE utf8mb4_bin,
failed_attempts INT DEFAULT 0,
last_failed_attempt TIMESTAMP NULL DEFAULT NULL,
locked_until TIMESTAMP NULL,
PRIMARY KEY (device_id, username)
);

CREATE TABLE IF NOT EXISTS uploaded_files (
id INT AUTO_INCREMENT PRIMARY KEY,
user_id INT NOT NULL,
file_name VARCHAR(255) NOT NULL,
file_type VARCHAR(10) NOT NULL,
file_size BIGINT DEFAULT 0,
file_data MEDIUMBLOB NULL,
uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS decks (
id INT AUTO_INCREMENT PRIMARY KEY,
user_id INT NOT NULL,
file_id INT NULL,
title VARCHAR(150) NOT NULL,
description TEXT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
published_at TIMESTAMP NULL DEFAULT NULL,
is_public BOOLEAN DEFAULT FALSE,
is_disabled BOOLEAN DEFAULT FALSE,
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (file_id) REFERENCES uploaded_files(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS cards (
id INT AUTO_INCREMENT PRIMARY KEY,
deck_id INT NOT NULL,
question TEXT NOT NULL,
answer_a TEXT NOT NULL,
answer_b TEXT NOT NULL,
answer_c TEXT NOT NULL,
answer_d TEXT NOT NULL,
correct_answer CHAR(1) NOT NULL,
FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS card_progress (
user_id INT NOT NULL,
card_id INT NOT NULL,
answered BOOLEAN DEFAULT FALSE,
selected_answer CHAR(1),
PRIMARY KEY (user_id, card_id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deck_tags (
deck_id INT NOT NULL,
tag_name VARCHAR(30) NOT NULL,
PRIMARY KEY (deck_id, tag_name),
FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS saved_decks (
user_id INT NOT NULL,
deck_id INT NOT NULL,
saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (user_id, deck_id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deck_reports (
id INT AUTO_INCREMENT PRIMARY KEY,
deck_id INT NOT NULL,
user_id INT NOT NULL,
reason TEXT NOT NULL,
status ENUM('pending', 'resolved', 'dismissed') DEFAULT 'pending',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS highscores (
user_id INT NOT NULL,
deck_id INT NOT NULL,
best_score INT DEFAULT 0,
total_cards INT DEFAULT 0,
saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (user_id, deck_id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE
);

-- admin
INSERT IGNORE INTO users (username, password, role) VALUES
('admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'admin');

-- indexes
ALTER TABLE cards ADD INDEX cards_deck_id (deck_id);
ALTER TABLE card_progress ADD INDEX card_progress_card_id (card_id);
ALTER TABLE card_progress ADD INDEX card_progress_user_id (user_id);
ALTER TABLE saved_decks ADD INDEX saved_decks_user_id (user_id);
ALTER TABLE decks ADD INDEX decks_public_disabled_published (is_public, is_disabled, published_at);

-- view deck details
CREATE OR REPLACE VIEW view_deck_details AS
SELECT decks.id, decks.user_id, decks.file_id, decks.title, decks.description, decks.is_public, decks.is_disabled, decks.created_at,
decks.published_at, users.username AS creator_name, uploaded_files.file_name AS source_file_name, COUNT(DISTINCT cards.id) AS card_count,
GROUP_CONCAT(DISTINCT deck_tags.tag_name SEPARATOR ',') AS tags FROM decks
JOIN users ON decks.user_id = users.id
LEFT JOIN uploaded_files ON decks.file_id = uploaded_files.id
LEFT JOIN cards ON cards.deck_id = decks.id
LEFT JOIN deck_tags ON deck_tags.deck_id = decks.id
GROUP BY decks.id, decks.user_id, decks.file_id, decks.title, decks.description, decks.is_public, decks.is_disabled,
decks.created_at, decks.published_at, users.username, uploaded_files.file_name;

-- reset deck progress
DELIMITER //
CREATE PROCEDURE reset_deck_progress(IN p_user_id INT, IN p_deck_id INT)
BEGIN
DELETE card_progress FROM card_progress
JOIN cards ON cards.id = card_progress.card_id
WHERE card_progress.user_id = p_user_id AND cards.deck_id = p_deck_id;
END //
DELIMITER ;

-- backup
mysqldump -h interchange.proxy.rlwy.net -P 27408 -u root -p railway > railway_backup.sql

-- restore
mysql -h interchange.proxy.rlwy.net -P 27408 -u root -p railway < railway_backup.sql

