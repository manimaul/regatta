-- Baseline schema for the Rust regatta server (metadata.version = 5).
--
-- This is the schema the Kotlin server (Exposed SchemaUtils.create over the `tables` array in
-- server/src/jvmMain/kotlin/com/mxmariner/regatta/db/Database.kt, after Migration.runMigration)
-- creates in an empty PostgreSQL 16 database. It was produced by running the Kotlin server once
-- against an empty postgres:16-bookworm container, then
--
--     pg_dump --schema-only --no-owner --no-privileges
--
-- with the SET / search_path / \restrict lines and the dump comments stripped. Every CREATE TABLE,
-- CREATE SEQUENCE, sequence ownership, column default, constraint, index and foreign key is kept
-- verbatim. The only addition is the metadata row at the end: a fresh Kotlin install writes
-- currentDbVersion + 10 (15); the Rust server treats a fresh install as version 5 (D6 in
-- docs/explore_implemented/rust_port.md), and only version 5 (or 15) is served.
--
-- The orphan `checkin` table that older production databases carried is not here: the Kotlin
-- 4 -> 5 migration drops it and it is not in the `tables` array. The Rust server applies this file
-- when no `metadata` table exists and never alters the schema otherwise; later changes are new
-- sqlx migrations.

CREATE TABLE public.auth (
    id bigint NOT NULL,
    hash character varying(128) NOT NULL,
    user_name character varying(128) NOT NULL
);

CREATE SEQUENCE public.auth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.auth_id_seq OWNED BY public.auth.id;

CREATE TABLE public.boat (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    sail_number character varying(128) NOT NULL,
    boat_type character varying(128) NOT NULL,
    phrf_rating integer,
    rating_type character varying(128) NOT NULL,
    skipper_id bigint,
    active boolean NOT NULL
);

CREATE SEQUENCE public.boat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.boat_id_seq OWNED BY public.boat.id;

CREATE TABLE public.bracket (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    description character varying(1024),
    active boolean NOT NULL,
    min_r real NOT NULL,
    max_r real NOT NULL,
    race_class bigint NOT NULL
);

CREATE SEQUENCE public.bracket_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.bracket_id_seq OWNED BY public.bracket.id;

CREATE TABLE public.image (
    hash character varying(128) NOT NULL,
    race_id bigint NOT NULL,
    data bytea NOT NULL
);

CREATE TABLE public.metadata (
    name character varying(128) NOT NULL,
    value character varying(128) NOT NULL
);

CREATE TABLE public.orc (
    orc_ref character varying(128) NOT NULL,
    boat_id bigint,
    cert jsonb NOT NULL
);

CREATE TABLE public.person (
    id bigint NOT NULL,
    first character varying(128) NOT NULL,
    last character varying(128) NOT NULL,
    club_member boolean NOT NULL,
    active boolean NOT NULL
);

CREATE SEQUENCE public.person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.person_id_seq OWNED BY public.person.id;

CREATE TABLE public.race (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    series_id bigint,
    rc_id bigint,
    correction_factor integer,
    orc_option character varying(128),
    orc_3 character varying(128),
    orc_5 character varying(128)
);

CREATE SEQUENCE public.race_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.race_id_seq OWNED BY public.race.id;

CREATE TABLE public.racebracketjunction (
    bracket_id bigint NOT NULL,
    race_id bigint NOT NULL,
    class_id bigint NOT NULL
);

CREATE TABLE public.raceclass (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    sort integer NOT NULL,
    rating_type character varying(128) NOT NULL,
    active boolean NOT NULL
);

CREATE SEQUENCE public.raceclass_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.raceclass_id_seq OWNED BY public.raceclass.id;

CREATE TABLE public.raceresults (
    id bigint NOT NULL,
    race_id bigint NOT NULL,
    boat_id bigint NOT NULL,
    end_date timestamp without time zone,
    phrf_rating integer,
    orc_ref character varying(128),
    rating_type character varying(128) NOT NULL,
    hoc integer,
    class_id bigint NOT NULL,
    bracket_id bigint NOT NULL,
    finish_code character varying(128),
    penalty integer
);

CREATE SEQUENCE public.raceresults_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.raceresults_id_seq OWNED BY public.raceresults.id;

CREATE TABLE public.racetime (
    race_id bigint NOT NULL,
    class_id bigint NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL
);

CREATE TABLE public.series (
    id bigint NOT NULL,
    name character varying(1024) NOT NULL,
    sort integer NOT NULL,
    active boolean NOT NULL
);

CREATE SEQUENCE public.series_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.series_id_seq OWNED BY public.series.id;

ALTER TABLE ONLY public.auth ALTER COLUMN id SET DEFAULT nextval('public.auth_id_seq'::regclass);

ALTER TABLE ONLY public.boat ALTER COLUMN id SET DEFAULT nextval('public.boat_id_seq'::regclass);

ALTER TABLE ONLY public.bracket ALTER COLUMN id SET DEFAULT nextval('public.bracket_id_seq'::regclass);

ALTER TABLE ONLY public.person ALTER COLUMN id SET DEFAULT nextval('public.person_id_seq'::regclass);

ALTER TABLE ONLY public.race ALTER COLUMN id SET DEFAULT nextval('public.race_id_seq'::regclass);

ALTER TABLE ONLY public.raceclass ALTER COLUMN id SET DEFAULT nextval('public.raceclass_id_seq'::regclass);

ALTER TABLE ONLY public.raceresults ALTER COLUMN id SET DEFAULT nextval('public.raceresults_id_seq'::regclass);

ALTER TABLE ONLY public.series ALTER COLUMN id SET DEFAULT nextval('public.series_id_seq'::regclass);

ALTER TABLE ONLY public.auth
    ADD CONSTRAINT auth_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.boat
    ADD CONSTRAINT boat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bracket
    ADD CONSTRAINT bracket_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT idx_boat_race_unique UNIQUE (boat_id, race_id);

ALTER TABLE ONLY public.image
    ADD CONSTRAINT image_pkey PRIMARY KEY (hash);

ALTER TABLE ONLY public.image
    ADD CONSTRAINT image_race_id_unique UNIQUE (race_id);

ALTER TABLE ONLY public.metadata
    ADD CONSTRAINT metadata_pkey PRIMARY KEY (name);

ALTER TABLE ONLY public.series
    ADD CONSTRAINT nameidx UNIQUE (name);

ALTER TABLE ONLY public.orc
    ADD CONSTRAINT orc_pkey PRIMARY KEY (orc_ref);

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_first_last_unique UNIQUE (first, last);

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.race
    ADD CONSTRAINT race_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT racebracketjunction_unique UNIQUE (race_id, class_id, bracket_id);

ALTER TABLE ONLY public.raceclass
    ADD CONSTRAINT raceclass_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT raceresults_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.racetime
    ADD CONSTRAINT racetime_race_class_unique UNIQUE (race_id, class_id);

ALTER TABLE ONLY public.series
    ADD CONSTRAINT series_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.auth
    ADD CONSTRAINT user_name_idx UNIQUE (user_name);

CREATE INDEX boat_skipper_id ON public.boat USING btree (skipper_id);

CREATE INDEX orc_boat_id ON public.orc USING btree (boat_id);

CREATE INDEX race_series_id ON public.race USING btree (series_id);

CREATE INDEX racebracketjunction_bracket_id ON public.racebracketjunction USING btree (bracket_id);

CREATE INDEX racebracketjunction_class_id ON public.racebracketjunction USING btree (class_id);

CREATE INDEX raceresults_race_id ON public.raceresults USING btree (race_id);

ALTER TABLE ONLY public.boat
    ADD CONSTRAINT fk_boat_skipper_id__id FOREIGN KEY (skipper_id) REFERENCES public.person(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.bracket
    ADD CONSTRAINT fk_bracket_race_class__id FOREIGN KEY (race_class) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.image
    ADD CONSTRAINT fk_image_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.orc
    ADD CONSTRAINT fk_orc_boat_id__id FOREIGN KEY (boat_id) REFERENCES public.boat(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.race
    ADD CONSTRAINT fk_race_rc_id__id FOREIGN KEY (rc_id) REFERENCES public.person(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.race
    ADD CONSTRAINT fk_race_series_id__id FOREIGN KEY (series_id) REFERENCES public.series(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT fk_racebracketjunction_bracket_id__id FOREIGN KEY (bracket_id) REFERENCES public.bracket(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT fk_racebracketjunction_class_id__id FOREIGN KEY (class_id) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT fk_racebracketjunction_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_boat_id__id FOREIGN KEY (boat_id) REFERENCES public.boat(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_bracket_id__id FOREIGN KEY (bracket_id) REFERENCES public.bracket(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_class_id__id FOREIGN KEY (class_id) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_orc_ref__orc_ref FOREIGN KEY (orc_ref) REFERENCES public.orc(orc_ref) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.racetime
    ADD CONSTRAINT fk_racetime_class_id__id FOREIGN KEY (class_id) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.racetime
    ADD CONSTRAINT fk_racetime_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

INSERT INTO metadata (name, value) VALUES ('version', '5');
