CREATE TABLE series
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR NOT NULL
);

CREATE TABLE person
(
    id          BIGSERIAL PRIMARY KEY,
    first       VARCHAR NOT NULL,
    last        VARCHAR NOT NULL,
    club_member BOOLEAN NOT NULL
);

CREATE TABLE race_class
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR NOT NULL,
    description VARCHAR NOT NULL
);

CREATE TABLE race
(
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR                       NOT NULL,
    series_id         BIGINT REFERENCES series (id) NOT NULL,
    start_date        TIMESTAMP                     NOT NULL,
    end_date          TIMESTAMP                     NOT NULL,
    correction_factor INTEGER                       NOT NULL,
    rc_id             BIGINT REFERENCES person (id) NOT NULL
);

CREATE TABLE boat
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR                           NOT NULL,
    sail_number VARCHAR                           NOT NULL,
    boat_type   VARCHAR                           NOT NULL,
    phrf_rating INTEGER                           NOT NULL,
    race_class  BIGINT REFERENCES race_class (id) NOT NULL,
    skipper     BIGINT REFERENCES person (id)     NOT NULL
);

CREATE TABLE race_result
(
    id          BIGSERIAL PRIMARY KEY,
    start_date  TIMESTAMP                     NOT NULL,
    end_date    TIMESTAMP                     NOT NULL,
    phrf_rating INTEGER                       NOT NULL,
    boat        BIGINT REFERENCES boat (id)   NOT NULL,
    race        BIGINT REFERENCES race (id)   NOT NULL,
    rc_id       BIGINT REFERENCES person (id) NOT NULL
);
