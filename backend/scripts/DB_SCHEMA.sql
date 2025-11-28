-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS mydb.friends CASCADE;
DROP TABLE IF EXISTS mydb.messages CASCADE;
DROP TABLE IF EXISTS mydb.club_events CASCADE;
DROP TABLE IF EXISTS mydb.club_announcements CASCADE;
DROP TABLE IF EXISTS mydb.club_documents CASCADE;
DROP TABLE IF EXISTS mydb.club_join_requests CASCADE;
DROP TABLE IF EXISTS mydb.club_requests CASCADE;
DROP TABLE IF EXISTS mydb.track_images CASCADE;
DROP TABLE IF EXISTS mydb.game_players CASCADE;
DROP TABLE IF EXISTS mydb.game_sessions CASCADE;
DROP TABLE IF EXISTS mydb.pointcard CASCADE;
DROP TABLE IF EXISTS mydb.hull CASCADE;
DROP TABLE IF EXISTS mydb.bane CASCADE;
DROP TABLE IF EXISTS mydb.bruker CASCADE;
DROP TABLE IF EXISTS mydb.klubb CASCADE;

-- Ensure schema exists
CREATE SCHEMA IF NOT EXISTS mydb;

-- Table: klubb
CREATE TABLE mydb.klubb (
    id_klubb SERIAL PRIMARY KEY,
    klubbnavn VARCHAR(255) NOT NULL UNIQUE,
    sted VARCHAR(255),
    klubbeskrivelse TEXT,
    kontakt_email VARCHAR(254),
    kontakt_telefon VARCHAR(50),
    klubb_bilde TEXT
);

ALTER SEQUENCE mydb.klubb_id_klubb_seq RESTART WITH 1;

-- Table: bruker
CREATE TABLE mydb.bruker (
    epost VARCHAR(255) PRIMARY KEY,
    brukernavn VARCHAR(64) NOT NULL UNIQUE,
    fnavn VARCHAR(100),
    enavn VARCHAR(100),
    passord VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'user' NOT NULL,
    visible BOOLEAN DEFAULT TRUE,
    email_hash VARCHAR(64) NOT NULL,
    klubb_id_klubb INT REFERENCES mydb.klubb(id_klubb) ON DELETE CASCADE
);

CREATE INDEX idx_bruker_klubb_id ON mydb.bruker(klubb_id_klubb);

-- Table: bane
CREATE TABLE mydb.bane (
    bane_id SERIAL PRIMARY KEY,
    navn VARCHAR(255) NOT NULL,
    beskrivelse TEXT,
    total_distance INT DEFAULT 0,
    total_par INT DEFAULT 0,
    antall_hull INT DEFAULT 0,
    markers JSONB,
    lines JSONB,
    ob_lines JSONB,
    klubb_id_klubb INT REFERENCES mydb.klubb(id_klubb) ON DELETE CASCADE,
    rating INT DEFAULT 0,
    bane_bilde TEXT,
    opprettet_dato TIMESTAMP DEFAULT NOW(),
    oppdatert_dato TIMESTAMP
);

ALTER SEQUENCE mydb.bane_bane_id_seq RESTART WITH 1;
CREATE INDEX idx_bane_klubb_id ON mydb.bane(klubb_id_klubb);

-- Table: hull (holes for each course/bane)
CREATE TABLE mydb.hull (
    hull_id SERIAL PRIMARY KEY,
    bane_id_bane INT REFERENCES mydb.bane(bane_id) ON DELETE CASCADE,
    hull_nummer INT NOT NULL,
    tee_latitude DECIMAL(10, 8),
    tee_longitude DECIMAL(11, 8),
    basket_latitude DECIMAL(10, 8),
    basket_longitude DECIMAL(11, 8),
    hole_par INTEGER DEFAULT 3,
    hole_distance INTEGER,
    beskrivelse TEXT,
    opprettet_dato TIMESTAMP DEFAULT NOW(),
    oppdatert_dato TIMESTAMP,
    UNIQUE(bane_id_bane, hull_nummer)
);

ALTER SEQUENCE mydb.hull_hull_id_seq RESTART WITH 1;
CREATE INDEX idx_hull_bane_id ON mydb.hull(bane_id_bane);
CREATE INDEX idx_hull_nummer ON mydb.hull(hull_nummer);

-- Table: pointcard
CREATE TABLE mydb.pointcard (
    pointcard_id SERIAL PRIMARY KEY,
    bane_id INT REFERENCES mydb.bane(bane_id) ON DELETE CASCADE,
    spiller_navn VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    score JSONB,
    total_score INT,
    dato TIMESTAMP DEFAULT NOW(),
    opprettet_dato TIMESTAMP DEFAULT NOW(),
    oppdatert_dato TIMESTAMP
);

ALTER SEQUENCE mydb.pointcard_pointcard_id_seq RESTART WITH 1;
CREATE INDEX idx_pointcard_bane_id ON mydb.pointcard(bane_id);
CREATE INDEX idx_pointcard_spiller ON mydb.pointcard(spiller_navn);

-- Table: game_sessions
CREATE TABLE mydb.game_sessions (
    session_id UUID PRIMARY KEY,
    bane_id INT REFERENCES mydb.bane(bane_id) ON DELETE CASCADE,
    host_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_game_sessions_bane_id ON mydb.game_sessions(bane_id);
CREATE INDEX idx_game_sessions_host ON mydb.game_sessions(host_email);

-- Table: game_players
CREATE TABLE mydb.game_players (
    id SERIAL PRIMARY KEY,
    session_id UUID REFERENCES mydb.game_sessions(session_id) ON DELETE CASCADE,
    player_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.game_players_id_seq RESTART WITH 1;
CREATE INDEX idx_game_players_session_id ON mydb.game_players(session_id);
CREATE INDEX idx_game_players_player_email ON mydb.game_players(player_email);

-- Table: track_images
CREATE TABLE mydb.track_images (
    id SERIAL PRIMARY KEY,
    bane_id INT REFERENCES mydb.bane(bane_id) ON DELETE CASCADE,
    image_data TEXT NOT NULL
);

ALTER SEQUENCE mydb.track_images_id_seq RESTART WITH 1;
CREATE INDEX idx_track_images_bane_id ON mydb.track_images(bane_id);

-- Table: friends
CREATE TABLE mydb.friends (
    id SERIAL PRIMARY KEY,
    user_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    friend_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

ALTER SEQUENCE mydb.friends_id_seq RESTART WITH 1;
CREATE UNIQUE INDEX uq_friends_pair ON mydb.friends(user_email, friend_email);
CREATE INDEX idx_friends_user ON mydb.friends(user_email);
CREATE INDEX idx_friends_friend ON mydb.friends(friend_email);

-- Table: messages
CREATE TABLE mydb.messages (
    message_id SERIAL PRIMARY KEY,
    sender_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    receiver_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.messages_message_id_seq RESTART WITH 1;
CREATE INDEX idx_messages_sender ON mydb.messages(sender_email);
CREATE INDEX idx_messages_receiver ON mydb.messages(receiver_email);

-- Table: club_requests
CREATE TABLE mydb.club_requests (
    id SERIAL PRIMARY KEY,
    requested_by VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    klubbnavn VARCHAR(255) NOT NULL,
    sted VARCHAR(255),
    beskrivelse TEXT,
    kontakt_email VARCHAR(254),
    kontakt_telefon VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    admin_notes TEXT,
    request_date TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.club_requests_id_seq RESTART WITH 1;
CREATE INDEX idx_club_requests_requested_by ON mydb.club_requests(requested_by);

-- Table: club_join_requests
CREATE TABLE mydb.club_join_requests (
    id SERIAL PRIMARY KEY,
    user_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    klub_id INT REFERENCES mydb.klubb(id_klubb) ON DELETE CASCADE,
    message TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    admin_notes TEXT,
    request_date TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.club_join_requests_id_seq RESTART WITH 1;
CREATE INDEX idx_club_join_requests_user ON mydb.club_join_requests(user_email);
CREATE INDEX idx_club_join_requests_klub ON mydb.club_join_requests(klub_id);

-- Table: club_documents
CREATE TABLE mydb.club_documents (
    id SERIAL PRIMARY KEY,
    klub_id INT REFERENCES mydb.klubb(id_klubb) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    file_data TEXT NOT NULL,
    uploaded_by VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE SET NULL,
    upload_date TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.club_documents_id_seq RESTART WITH 1;
CREATE INDEX idx_club_documents_klub ON mydb.club_documents(klub_id);

-- Table: club_announcements
CREATE TABLE mydb.club_announcements (
    id SERIAL PRIMARY KEY,
    klubb_id INT REFERENCES mydb.klubb(id_klubb) ON DELETE CASCADE,
    content TEXT NOT NULL,
    author_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.club_announcements_id_seq RESTART WITH 1;
CREATE INDEX idx_club_announcements_klubb ON mydb.club_announcements(klubb_id);

-- Table: club_events
CREATE TABLE mydb.club_events (
    id SERIAL PRIMARY KEY,
    klubb_id INT REFERENCES mydb.klubb(id_klubb) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    author_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER SEQUENCE mydb.club_events_id_seq RESTART WITH 1;
CREATE INDEX idx_club_events_klubb ON mydb.club_events(klubb_id);

-- Table: event_participants (tracks who signs up for events)
CREATE TABLE IF NOT EXISTS mydb.event_participants (
    event_id INT REFERENCES mydb.club_events(id) ON DELETE CASCADE,
    user_email VARCHAR(255) REFERENCES mydb.bruker(epost) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (event_id, user_email)
);

CREATE INDEX idx_event_participants_event ON mydb.event_participants(event_id);
CREATE INDEX idx_event_participants_user ON mydb.event_participants(user_email);
