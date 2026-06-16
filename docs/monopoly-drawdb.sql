-- Politechnika Monopoly — schemat PostgreSQL (referencja / migracje)
-- DrawDB NIE importuje tego pliku — uzyj docs/monopoly-drawdb.dbml (format DBML)
-- Odzwierciedla encje JPA (stan po czyszczeniu martwych tabel).

CREATE SCHEMA IF NOT EXISTS monopoly;
SET search_path TO monopoly;

-- ========== UŻYTKOWNICY I PROFIL ==========

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(20)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    first_name      VARCHAR(20),
    last_name       VARCHAR(50),
    date_of_birth   DATE         NOT NULL,
    coins           INTEGER      NOT NULL DEFAULT 1000,
    verified        BOOLEAN      NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    avatar_url      VARCHAR(512),
    banner_url      VARCHAR(512),
    profile_bg_url  VARCHAR(512),
    bio             VARCHAR(200),
    verification_doc_url VARCHAR(255),
    suspended       BOOLEAN      NOT NULL DEFAULT FALSE,
    suspended_until TIMESTAMP,
    suspension_reason VARCHAR(500)
);

CREATE TABLE player_statistics (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE REFERENCES users(id),
    games_played            INTEGER NOT NULL,
    games_won               INTEGER NOT NULL,
    level                   INTEGER NOT NULL,
    elo_points              INTEGER NOT NULL,
    win_streak              INTEGER NOT NULL,
    daily_streak            INTEGER NOT NULL,
    last_spin_date          DATE,
    last_reward             VARCHAR(120),
    available_lootboxes     INTEGER NOT NULL,
    last_lootbox_grant_date DATE,
    pending_wheel_card      VARCHAR(40),
    pending_start_cash_bonus INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE player_achievements (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    code        VARCHAR(30) NOT NULL,
    coin_reward INTEGER     NOT NULL,
    unlocked_at TIMESTAMP   NOT NULL,
    CONSTRAINT uk_achievement_user_code UNIQUE (user_id, code)
);

CREATE TABLE owned_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    item_slug   VARCHAR(60) NOT NULL,
    obtained_at TIMESTAMP   NOT NULL,
    equipped    BOOLEAN     NOT NULL
);

CREATE TABLE friendships (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  BIGINT      NOT NULL REFERENCES users(id),
    addressee_id  BIGINT      NOT NULL REFERENCES users(id),
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP   NOT NULL,
    CONSTRAINT uk_friendship_pair UNIQUE (requester_id, addressee_id)
);

CREATE TABLE profile_comments (
    id         BIGSERIAL PRIMARY KEY,
    author_id  BIGINT       NOT NULL REFERENCES users(id),
    target_id  BIGINT       NOT NULL REFERENCES users(id),
    content    VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP,
    reply      VARCHAR(500),
    reply_at   TIMESTAMP,
    CONSTRAINT uk_profile_comment_author_target UNIQUE (author_id, target_id)
);

CREATE TABLE profile_comment_likes (
    id         BIGSERIAL PRIMARY KEY,
    comment_id BIGINT    NOT NULL REFERENCES profile_comments(id),
    user_id    BIGINT    NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_pc_like_comment_user UNIQUE (comment_id, user_id)
);

CREATE TABLE user_warnings (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id),
    moderator_id BIGINT       NOT NULL REFERENCES users(id),
    message      VARCHAR(500) NOT NULL,
    created_at   TIMESTAMP    NOT NULL
);

-- ========== GRA (lobby + sesja; stan rozgrywki też w RAM) ==========

CREATE TABLE game_sessions (
    id                          BIGSERIAL PRIMARY KEY,
    code                        VARCHAR(8)  NOT NULL UNIQUE,
    name                        VARCHAR(60) NOT NULL,
    status                      VARCHAR(20) NOT NULL,
    created_at                  TIMESTAMP   NOT NULL,
    current_turn                INTEGER     NOT NULL,
    leader_id                   BIGINT,
    pending_purchase_pos        INTEGER,
    pending_purchase_price      INTEGER,
    pending_decider_id          BIGINT,
    pending_payment_debtor_id   BIGINT,
    pending_payment_amount      INTEGER,
    pending_payment_creditor_id BIGINT,
    pending_payment_reason      VARCHAR(120),
    pending_upgrade_pos         INTEGER,
    pending_upgrade_player_id   BIGINT,
    pending_upgrade_cost        INTEGER,
    pending_buyback_pos         INTEGER,
    pending_buyback_victim_id   BIGINT,
    pending_buyback_holder_id   BIGINT,
    pending_buyback_price       INTEGER,
    pending_extra_roll_player_id BIGINT,
    pending_takeover_pos        INTEGER,
    pending_takeover_buyer_id   BIGINT,
    pending_takeover_seller_id  BIGINT,
    pending_takeover_price      INTEGER
);

CREATE TABLE game_players (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT      NOT NULL REFERENCES game_sessions(id),
    user_id          BIGINT      REFERENCES users(id),
    display_name     VARCHAR(30) NOT NULL,
    cash             INTEGER     NOT NULL,
    position         INTEGER     NOT NULL,
    color            VARCHAR(7)  NOT NULL,
    bankrupt         BOOLEAN     NOT NULL,
    turn_order       INTEGER     NOT NULL,
    ready            BOOLEAN     NOT NULL DEFAULT FALSE,
    doubles_count    INTEGER     NOT NULL DEFAULT 0,
    rolls_this_turn  INTEGER     NOT NULL DEFAULT 0,
    skip_next_rent   BOOLEAN     NOT NULL,
    shield_active    BOOLEAN     NOT NULL,
    double_rent_next BOOLEAN     NOT NULL DEFAULT FALSE,
    jail_pass_active BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE game_player_properties (
    player_id BIGINT NOT NULL REFERENCES game_players(id),
    position  INTEGER NOT NULL,
    PRIMARY KEY (player_id, position)
);

CREATE TABLE game_player_property_levels (
    player_id     BIGINT  NOT NULL REFERENCES game_players(id),
    tile_position INTEGER NOT NULL,
    level         INTEGER NOT NULL,
    PRIMARY KEY (player_id, tile_position)
);

CREATE TABLE game_player_hand_cards (
    player_id BIGINT NOT NULL REFERENCES game_players(id),
    card_type VARCHAR(255)
);

CREATE TABLE game_player_landing_counts (
    player_id     BIGINT  NOT NULL REFERENCES game_players(id),
    tile_position INTEGER NOT NULL,
    landing_count INTEGER,
    PRIMARY KEY (player_id, tile_position)
);

CREATE TABLE game_invites (
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT      NOT NULL REFERENCES game_sessions(id),
    inviter_id BIGINT      NOT NULL REFERENCES users(id),
    invitee_id BIGINT      NOT NULL REFERENCES users(id),
    status     VARCHAR(16) NOT NULL,
    created_at TIMESTAMP   NOT NULL
);

-- ========== HISTORIA MECZÓW (ranking) ==========

CREATE TABLE match_history (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id),
    played_at        TIMESTAMP   NOT NULL,
    board_name       VARCHAR(60),
    won              BOOLEAN     NOT NULL,
    placement        INTEGER     NOT NULL,
    players_count    INTEGER     NOT NULL,
    final_cash       INTEGER     NOT NULL,
    duration_minutes INTEGER     NOT NULL,
    elo_change       INTEGER     NOT NULL
);
