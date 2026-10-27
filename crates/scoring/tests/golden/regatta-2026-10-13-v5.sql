--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14 (Debian 16.14-1.pgdg12+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg12+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.auth (
    id bigint NOT NULL,
    hash character varying(128) NOT NULL,
    user_name character varying(128) NOT NULL
);


ALTER TABLE public.auth OWNER TO regatta_admin;

--
-- Name: auth_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.auth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.auth_id_seq OWNER TO regatta_admin;

--
-- Name: auth_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.auth_id_seq OWNED BY public.auth.id;


--
-- Name: boat; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.boat (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    sail_number character varying(128) NOT NULL,
    boat_type character varying(128) NOT NULL,
    phrf_rating integer,
    skipper_id bigint,
    active boolean NOT NULL,
    rating_type character varying(128) NOT NULL
);


ALTER TABLE public.boat OWNER TO regatta_admin;

--
-- Name: boat_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.boat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.boat_id_seq OWNER TO regatta_admin;

--
-- Name: boat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.boat_id_seq OWNED BY public.boat.id;


--
-- Name: bracket; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.bracket (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    description character varying(1024),
    active boolean NOT NULL,
    min_r real NOT NULL,
    max_r real NOT NULL,
    race_class bigint NOT NULL
);


ALTER TABLE public.bracket OWNER TO regatta_admin;

--
-- Name: image; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.image (
    hash character varying(128) NOT NULL,
    race_id bigint NOT NULL,
    data bytea NOT NULL
);


ALTER TABLE public.image OWNER TO regatta_admin;

--
-- Name: metadata; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.metadata (
    name character varying(128) NOT NULL,
    value character varying(128) NOT NULL
);


ALTER TABLE public.metadata OWNER TO regatta_admin;

--
-- Name: orc; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.orc (
    orc_ref character varying(128) NOT NULL,
    boat_id bigint,
    cert jsonb NOT NULL
);


ALTER TABLE public.orc OWNER TO regatta_admin;

--
-- Name: person; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.person (
    id bigint NOT NULL,
    first character varying(128) NOT NULL,
    last character varying(128) NOT NULL,
    club_member boolean NOT NULL,
    active boolean NOT NULL
);


ALTER TABLE public.person OWNER TO regatta_admin;

--
-- Name: person_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.person_id_seq OWNER TO regatta_admin;

--
-- Name: person_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.person_id_seq OWNED BY public.person.id;


--
-- Name: race; Type: TABLE; Schema: public; Owner: regatta_admin
--

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


ALTER TABLE public.race OWNER TO regatta_admin;

--
-- Name: race_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.race_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.race_id_seq OWNER TO regatta_admin;

--
-- Name: race_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.race_id_seq OWNED BY public.race.id;


--
-- Name: racebracketjunction; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.racebracketjunction (
    bracket_id bigint NOT NULL,
    race_id bigint NOT NULL,
    class_id bigint NOT NULL
);


ALTER TABLE public.racebracketjunction OWNER TO regatta_admin;

--
-- Name: raceclass; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.raceclass (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    active boolean NOT NULL,
    sort integer NOT NULL,
    rating_type character varying(128) NOT NULL
);


ALTER TABLE public.raceclass OWNER TO regatta_admin;

--
-- Name: raceclass_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.raceclass_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.raceclass_id_seq OWNER TO regatta_admin;

--
-- Name: raceclass_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.raceclass_id_seq OWNED BY public.bracket.id;


--
-- Name: raceclasscategory_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.raceclasscategory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.raceclasscategory_id_seq OWNER TO regatta_admin;

--
-- Name: raceclasscategory_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.raceclasscategory_id_seq OWNED BY public.raceclass.id;


--
-- Name: raceresults; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.raceresults (
    id bigint NOT NULL,
    race_id bigint NOT NULL,
    boat_id bigint NOT NULL,
    end_date timestamp without time zone,
    phrf_rating integer,
    hoc integer,
    start_code integer,
    penalty integer,
    finish_code character varying(128),
    class_id bigint NOT NULL,
    bracket_id bigint NOT NULL,
    rating_type character varying(128) NOT NULL,
    orc_ref character varying(128)
);


ALTER TABLE public.raceresults OWNER TO regatta_admin;

--
-- Name: raceresults_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.raceresults_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.raceresults_id_seq OWNER TO regatta_admin;

--
-- Name: raceresults_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.raceresults_id_seq OWNED BY public.raceresults.id;


--
-- Name: racetime; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.racetime (
    class_id bigint NOT NULL,
    race_id bigint NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL
);


ALTER TABLE public.racetime OWNER TO regatta_admin;

--
-- Name: series; Type: TABLE; Schema: public; Owner: regatta_admin
--

CREATE TABLE public.series (
    id bigint NOT NULL,
    name character varying(1024) NOT NULL,
    active boolean NOT NULL,
    sort integer NOT NULL
);


ALTER TABLE public.series OWNER TO regatta_admin;

--
-- Name: series_id_seq; Type: SEQUENCE; Schema: public; Owner: regatta_admin
--

CREATE SEQUENCE public.series_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.series_id_seq OWNER TO regatta_admin;

--
-- Name: series_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: regatta_admin
--

ALTER SEQUENCE public.series_id_seq OWNED BY public.series.id;


--
-- Name: auth id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.auth ALTER COLUMN id SET DEFAULT nextval('public.auth_id_seq'::regclass);


--
-- Name: boat id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.boat ALTER COLUMN id SET DEFAULT nextval('public.boat_id_seq'::regclass);


--
-- Name: bracket id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.bracket ALTER COLUMN id SET DEFAULT nextval('public.raceclass_id_seq'::regclass);


--
-- Name: person id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.person ALTER COLUMN id SET DEFAULT nextval('public.person_id_seq'::regclass);


--
-- Name: race id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.race ALTER COLUMN id SET DEFAULT nextval('public.race_id_seq'::regclass);


--
-- Name: raceclass id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceclass ALTER COLUMN id SET DEFAULT nextval('public.raceclasscategory_id_seq'::regclass);


--
-- Name: raceresults id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults ALTER COLUMN id SET DEFAULT nextval('public.raceresults_id_seq'::regclass);


--
-- Name: series id; Type: DEFAULT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.series ALTER COLUMN id SET DEFAULT nextval('public.series_id_seq'::regclass);


--
-- Data for Name: auth; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.auth (id, hash, user_name) FROM stdin;
\.


--
-- Data for Name: boat; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.boat (id, name, sail_number, boat_type, phrf_rating, skipper_id, active, rating_type) FROM stdin;
6	Windsong	88	Mirage 30	\N	27	t	CruisingNonFlyingSails
50	Gardyloo	56500	IMX-38	63	71	t	PHRF
47	Lola		S/J 29	\N	67	t	CruisingNonFlyingSails
1	Waymaker	USA 59793	Tartan 37 DK	150	23	t	PHRF
106	Amidala	621	J/105	93	130	t	PHRF
44	Maria Cristina	69054	J/29	\N	66	t	CruisingNonFlyingSails
108	Aurora	29718	R-anc 33	159	132	t	PHRF
110	3187	3187	Lido	\N	134	t	CruisingNonFlyingSails
112	2181	2181	Lido	\N	136	t	CruisingNonFlyingSails
114	2181A	2181A	Lido	\N	138	t	CruisingNonFlyingSails
116	4839	4839	Lido	\N	140	t	CruisingNonFlyingSails
21	Ripley	1210	Thunderbird	195	19	t	CruisingNonFlyingSails
37	Bella	12	Multichine 28	\N	58	t	CruisingNonFlyingSails
16	Boginka	59746	J/30	141	18	t	CruisingNonFlyingSails
25	Saoirse	1036791	Freedom 36	\N	62	t	CruisingNonFlyingSails
8	Whisper (Blue)		C&C 29	\N	28	t	CruisingNonFlyingSails
54	Larklight		San Juan 23	\N	75	t	CruisingNonFlyingSails
55	Red Baron		Evetts 31	\N	76	t	CruisingNonFlyingSails
94	Windswept		Newport Mark II 30	\N	117	t	CruisingNonFlyingSails
97	Ratatouille	87919	Beneteau First 345	\N	28	t	CruisingNonFlyingSails
88	Kitsune	69235	Calgan Marine Crown 34	\N	108	t	CruisingNonFlyingSails
89	WHO-AH	19972	People's 37	\N	109	t	CruisingNonFlyingSails
57	Peace	29	Ericson 29 TM	\N	79	t	CruisingNonFlyingSails
91	Thane		Tartan 37	\N	107	t	CruisingNonFlyingSails
56	Land Denial	865	Newport 30	\N	78	t	CruisingNonFlyingSails
93	Scallywag		Catalina 38	\N	116	t	CruisingNonFlyingSails
96	Iris	952	Cal 20	\N	119	t	CruisingNonFlyingSails
12	Flicka	69262	J/35	72	4	t	PHRF
15	Goes to 11	19	Antrim 27	93	11	t	PHRF
13	Solitaire	52	Santa Cruz 27	159	13	t	PHRF
11	Kahuna	USA 27	Aerodyne 38	48	2	t	PHRF
19	Slick	69299	J/29	120	17	t	PHRF
20	Intrepid	97848	J/35	72	5	t	PHRF
22	Jeopardy	161	J/109	69	1	t	PHRF
36	Zephyr	69097	J/30	141	10	t	PHRF
24	Stardust	1120	Sun Odesy	\N	60	t	CruisingFlyingSails
3	Blue Jay		Beneteau	\N	22	t	CruisingFlyingSails
2	Whisper	343	Islander 36	\N	21	t	CruisingFlyingSails
34	Klatawa	98588	Catalina 380	\N	55	t	CruisingFlyingSails
51	Leucothea	29179	X-yacht 119	\N	72	t	CruisingFlyingSails
53	Origami	063	Corsair Dash 750	\N	74	t	CruisingFlyingSails
33	Blue Moon	30148	C&C 40 II	\N	16	t	CruisingFlyingSails
98	Black Elise II		Hanse 400e	\N	120	t	CruisingFlyingSails
101	Tatoosh	39020	Islander 36	\N	122	t	CruisingNonFlyingSails
103	Simple Pleasure	H43	Hunter 34	\N	\N	t	CruisingNonFlyingSails
62	EQUUS	60919	Jeanneau 509	63	7	t	ORC_PHRF
39	Aleshanee	69027	Ericson 35	144	61	t	PHRF
23	Blue Max	18715	Aphrodite 101	144	14	t	PHRF
48	Sidewinder	59718	Olson 30	99	69	t	PHRF
26	Flash	50330	J/130	33	6	t	PHRF
29	Out of the Blue	USA60159	Landing School 30	99	52	t	PHRF
30	Strider	49589	Hotfoot 31	126	53	t	PHRF
27	Grace E	40622	J/35	72	51	t	PHRF
38	Skosh	73	Moore 24	168	59	t	PHRF
41	Bucket List	ANTR27-2	Antrum	93	64	t	PHRF
40	Magic Dragon	69153	Hobie 33	105	63	t	PHRF
49	Last Chance	35	Melges 24	90	70	t	PHRF
14	Les Chevaux Blancs	79052	BENE35S5-1	132	29	t	PHRF
73	Athena	69708	Beneteau First 435	75	91	t	PHRF
17	Bella Rose	70925	Beneteau First 375	135	30	t	PHRF
43	Keet	97600	J/33	93	111	t	PHRF
95	Scoundrel	18199	Olson 30	96	118	t	PHRF
31	Obsession	69598	Frers 38	93	54	t	PHRF
100	Afterglow	62	Beneteau First 36	54	121	t	PHRF
104	Ratfish	69061	Evelyn 32-2	93	128	t	PHRF
63	Wicked Sister	USA 3	Farr 36	18	6	t	ORC_PHRF
28	Zulu Tango	USA 7395	FARR395-ODR	42	8	t	ORC_PHRF
35	Grazia	570	Etchells E-22	141	56	t	CruisingNonFlyingSails
102	Lucia		Cal 29	200	31	t	CruisingFlyingSails
18	Short Circuit	69800	Tripp 33	84	12	t	PHRF
52	ZZZAP	180	Hobie 33	105	73	t	PHRF
90	DYNO	69174	Soverel 33	93	110	t	PHRF
109	Pleiades	10169	Newport 28	200	133	t	CruisingFlyingSails
111	90	90	Lido	\N	135	t	CruisingNonFlyingSails
113	6102	6102	Lido	\N	137	t	CruisingNonFlyingSails
115	4231	4231	Lido	\N	139	t	CruisingNonFlyingSails
117	4231A	4231A	Lido	200	141	t	CruisingNonFlyingSails
5	Work Release	61396	Jeaneau SO 37	162	25	t	CruisingNonFlyingSails
105	Emoyeni	16246	Islander Bahama 30	200	129	t	CruisingNonFlyingSails
99	Yonder Star	24601	Catalina 320	200	79	t	CruisingNonFlyingSails
\.


--
-- Data for Name: bracket; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.bracket (id, name, description, active, min_r, max_r, race_class) FROM stdin;
18	All	\N	t	-1000	1000	3
19	All	\N	t	-1000	1000	4
29	All		t	-1000	1000	5
30	All	\N	t	-1000	1000	8
62	All	\N	t	-1000	1000	35
63	All	\N	t	-1000	1000	36
28	All		t	-1000	1000	7
31	All	\N	t	-1000	1000	9
1	I	Rating 10 to 59	t	10	59	1
16	I/II	Rating 10 to 72	t	10	72	1
2	II	Rating 60 to 72	t	60	72	1
17	III/IV	\N	t	73	200	2
3	III	Rating 73 to 119	t	73	119	2
4	IV	Rating 120 to 200	t	120	200	2
64	I	\N	t	-1000	69	37
66	I & II	\N	t	-1000	93	37
65	II	\N	t	70	93	37
67	III	\N	t	94	139	38
69	III & IV	\N	t	94	1000	38
68	IV	\N	t	140	1000	38
72	All	\N	t	-1000	1000	40
\.


--
-- Data for Name: image; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.image (hash, race_id, data) FROM stdin;
\.


--
-- Data for Name: metadata; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.metadata (name, value) FROM stdin;
version	5
\.


--
-- Data for Name: orc; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.orc (orc_ref, boat_id, cert) FROM stdin;
04560003NFK	62	{"BIN": "USA60919", "APHD": 495.0, "APHT": 1.2121, "Class": "Sun Oddysey 519", "ILCWA": 622.3, "RefNo": "04560003NFK", "C_Type": "CLUB", "CertNo": "US60919", "SailNo": "USA 60919", "Builder": "Jeanneau", "NatAuth": "USA", "Designer": "Philippe Briand", "Division": "C", "IssueDate": "2025-04-08T19:32:22.815Z", "YachtName": "EQUUS", "TMF_Inshore": 0.9642, "US_5B_H_TOD": 503.8, "US_5B_H_TOT": 1.191, "US_5B_L_TOD": 1020.9, "US_5B_L_TOT": 0.5877, "US_5B_M_TOD": 605.0, "US_5B_M_TOT": 0.9917, "US_AP_H_TOD": 412.7, "US_AP_H_TOT": 1.4537, "US_AP_L_TOD": 736.6, "US_AP_L_TOT": 0.8146, "US_AP_M_TOD": 483.0, "US_AP_M_TOT": 1.2422, "US_5B_LM_TOD": 730.9, "US_5B_LM_TOT": 0.8209, "US_5B_MH_TOD": 534.6, "US_5B_MH_TOT": 1.1223, "US_AP_LM_TOD": 569.0, "US_AP_LM_TOT": 1.0546, "US_AP_MH_TOD": 435.6, "US_AP_MH_TOT": 1.3774, "US_PREDDN_TOD": 529.7, "US_PREDDN_TOT": 1.1327, "US_PREDRC_TOD": 491.7, "US_PREDRC_TOT": 1.2204, "US_PREDUP_TOD": 563.5, "US_PREDUP_TOT": 1.0647, "TN_Inshore_High": 1.1417, "TN_Offshore_Low": 0.9032, "US_PREDDN_H_TOD": 389.9, "US_PREDDN_H_TOT": 1.539, "US_PREDDN_L_TOD": 807.5, "US_PREDDN_L_TOT": 0.7431, "US_PREDDN_M_TOD": 484.5, "US_PREDDN_M_TOT": 1.2385, "US_PREDRC_H_TOD": 386.5, "US_PREDRC_H_TOT": 1.5523, "US_PREDRC_L_TOD": 716.0, "US_PREDRC_L_TOT": 0.838, "US_PREDRC_M_TOD": 453.2, "US_PREDRC_M_TOT": 1.3238, "US_PREDUP_H_TOD": 459.1, "US_PREDUP_H_TOT": 1.3069, "US_PREDUP_L_TOD": 816.3, "US_PREDUP_L_TOT": 0.735, "US_PREDUP_M_TOD": 516.9, "US_PREDUP_M_TOT": 1.1608, "US_WL6040_H_TOD": 524.5, "US_WL6040_H_TOT": 1.1439, "US_WL6040_L_TOD": 1028.3, "US_WL6040_L_TOT": 0.5835, "US_WL6040_M_TOD": 616.5, "US_WL6040_M_TOT": 0.9732, "TND_Offshore_Low": 664.3, "TN_Offshore_High": 1.3983, "US_PREDDN_LM_TOD": 582.1, "US_PREDDN_LM_TOT": 1.0308, "US_PREDDN_MH_TOD": 424.5, "US_PREDDN_MH_TOT": 1.4133, "US_PREDRC_LM_TOD": 529.6, "US_PREDRC_LM_TOT": 1.133, "US_PREDRC_MH_TOD": 410.4, "US_PREDRC_MH_TOT": 1.4622, "US_PREDUP_LM_TOD": 602.9, "US_PREDUP_LM_TOT": 0.9952, "US_PREDUP_MH_TOD": 475.3, "US_PREDUP_MH_TOT": 1.2624, "US_WL6040_LM_TOD": 739.8, "US_WL6040_LM_TOT": 0.811, "US_WL6040_MH_TOD": 550.9, "US_WL6040_MH_TOT": 1.0891, "TND_Offshore_High": 429.1, "TN_Inshore_Medium": 0.9713, "US_TN_Inshore_Low": 0.6612, "TN_Offshore_Medium": 1.2209, "TND_Offshore_Medium": 491.4}
04560003L6C	\N	{"BIN": "USA 7395", "APHD": 498.9, "APHT": 1.2027, "Class": "FARR 395", "ILCWA": 613.4, "RefNo": "04560003L6C", "C_Type": "CLUB", "CertNo": "US7395", "SailNo": "USA 7395", "Builder": "CARROLL", "NatAuth": "USA", "Designer": "FARR", "Division": "R", "IssueDate": "2025-03-19T14:25:27.666Z", "YachtName": "ZULU TANGO", "TMF_Inshore": 0.9781, "US_5B_H_TOD": 513.7, "US_5B_H_TOT": 1.1681, "US_5B_L_TOD": 918.2, "US_5B_L_TOT": 0.6534, "US_5B_M_TOD": 601.2, "US_5B_M_TOT": 0.9979, "US_AP_H_TOD": 427.5, "US_AP_H_TOT": 1.4035, "US_AP_L_TOD": 674.8, "US_AP_L_TOT": 0.8891, "US_AP_M_TOD": 491.4, "US_AP_M_TOT": 1.221, "US_5B_LM_TOD": 689.9, "US_5B_LM_TOT": 0.8696, "US_5B_MH_TOD": 551.4, "US_5B_MH_TOT": 1.0882, "US_AP_LM_TOD": 550.3, "US_AP_LM_TOT": 1.0904, "US_AP_MH_TOD": 454.6, "US_AP_MH_TOT": 1.3198, "US_PREDDN_TOD": 516.9, "US_PREDDN_TOT": 1.1609, "US_PREDRC_TOD": 489.2, "US_PREDRC_TOT": 1.2264, "US_PREDUP_TOD": 561.2, "US_PREDUP_TOT": 1.0692, "TN_Inshore_High": 1.1075, "TN_Offshore_Low": 0.971, "US_PREDDN_H_TOD": 390.7, "US_PREDDN_H_TOT": 1.5359, "US_PREDDN_L_TOD": 732.8, "US_PREDDN_L_TOT": 0.8188, "US_PREDDN_M_TOD": 485.4, "US_PREDDN_M_TOT": 1.236, "US_PREDRC_H_TOD": 399.8, "US_PREDRC_H_TOT": 1.5006, "US_PREDRC_L_TOD": 652.9, "US_PREDRC_L_TOT": 0.919, "US_PREDRC_M_TOD": 464.0, "US_PREDRC_M_TOT": 1.2931, "US_PREDUP_H_TOD": 482.2, "US_PREDUP_H_TOT": 1.2443, "US_PREDUP_L_TOD": 742.4, "US_PREDUP_L_TOT": 0.8082, "US_PREDUP_M_TOD": 528.7, "US_PREDUP_M_TOT": 1.1348, "US_WL6040_H_TOD": 538.0, "US_WL6040_H_TOT": 1.1152, "US_WL6040_L_TOD": 924.2, "US_WL6040_L_TOT": 0.6492, "US_WL6040_M_TOD": 614.5, "US_WL6040_M_TOT": 0.9764, "TND_Offshore_Low": 617.9, "TN_Offshore_High": 1.3407, "US_PREDDN_LM_TOD": 556.9, "US_PREDDN_LM_TOT": 1.0774, "US_PREDDN_MH_TOD": 434.4, "US_PREDDN_MH_TOT": 1.3811, "US_PREDRC_LM_TOD": 515.9, "US_PREDRC_LM_TOT": 1.163, "US_PREDRC_MH_TOD": 427.8, "US_PREDRC_MH_TOT": 1.4027, "US_PREDUP_LM_TOD": 583.7, "US_PREDUP_LM_TOT": 1.0278, "US_PREDUP_MH_TOD": 499.4, "US_PREDUP_MH_TOT": 1.2013, "US_WL6040_LM_TOD": 699.2, "US_WL6040_LM_TOT": 0.8581, "US_WL6040_MH_TOD": 569.2, "US_WL6040_MH_TOT": 1.054, "TND_Offshore_High": 447.5, "TN_Inshore_Medium": 0.9844, "US_TN_Inshore_Low": 0.7256, "TN_Offshore_Medium": 1.2068, "TND_Offshore_Medium": 497.2}
04560004KJP	28	{"BIN": "USA 7395", "APHD": 502.7, "APHT": 1.1936, "Class": "FARR 395", "ILCWA": 618.0, "RefNo": "04560004KJP", "C_Type": "CLUB", "CertNo": "US7395", "SailNo": "USA 7395", "Builder": "CARROLL", "NatAuth": "USA", "Designer": "FARR", "Division": "C", "IssueDate": "2026-04-08T12:37:54.155Z", "YachtName": "ZULU TANGO", "TMF_Inshore": 0.9708, "US_5B_H_TOD": 519.5, "US_5B_H_TOT": 1.155, "US_5B_L_TOD": 929.1, "US_5B_L_TOT": 0.6458, "US_5B_M_TOD": 606.0, "US_5B_M_TOT": 0.9902, "US_AP_H_TOD": 433.3, "US_AP_H_TOT": 1.3846, "US_AP_L_TOD": 682.1, "US_AP_L_TOT": 0.8797, "US_AP_M_TOD": 495.2, "US_AP_M_TOT": 1.2115, "US_5B_LM_TOD": 694.0, "US_5B_LM_TOT": 0.8645, "US_5B_MH_TOD": 556.0, "US_5B_MH_TOT": 1.0791, "US_AP_LM_TOD": 553.3, "US_AP_LM_TOT": 1.0844, "US_AP_MH_TOD": 458.9, "US_AP_MH_TOT": 1.3074, "US_PREDDN_TOD": 519.8, "US_PREDDN_TOT": 1.1543, "US_PREDRC_TOD": 493.2, "US_PREDRC_TOT": 1.2164, "US_PREDUP_TOD": 567.6, "US_PREDUP_TOT": 1.0571, "TN_Inshore_High": 1.098, "TN_Offshore_Low": 0.967, "US_PREDDN_H_TOD": 395.2, "US_PREDDN_H_TOT": 1.5181, "US_PREDDN_L_TOD": 738.2, "US_PREDDN_L_TOT": 0.8128, "US_PREDDN_M_TOD": 487.3, "US_PREDDN_M_TOT": 1.2314, "US_PREDRC_H_TOD": 405.8, "US_PREDRC_H_TOT": 1.4787, "US_PREDRC_L_TOD": 658.5, "US_PREDRC_L_TOT": 0.9112, "US_PREDRC_M_TOD": 467.1, "US_PREDRC_M_TOT": 1.2845, "US_PREDUP_H_TOD": 489.4, "US_PREDUP_H_TOT": 1.226, "US_PREDUP_L_TOD": 751.9, "US_PREDUP_L_TOT": 0.798, "US_PREDUP_M_TOD": 534.0, "US_PREDUP_M_TOT": 1.1236, "US_WL6040_H_TOD": 545.0, "US_WL6040_H_TOT": 1.101, "US_WL6040_L_TOD": 936.0, "US_WL6040_L_TOT": 0.641, "US_WL6040_M_TOD": 620.2, "US_WL6040_M_TOT": 0.9674, "TND_Offshore_Low": 620.5, "TN_Offshore_High": 1.3271, "US_PREDDN_LM_TOD": 558.3, "US_PREDDN_LM_TOT": 1.0746, "US_PREDDN_MH_TOD": 436.7, "US_PREDDN_MH_TOT": 1.3739, "US_PREDRC_LM_TOD": 518.2, "US_PREDRC_LM_TOT": 1.1578, "US_PREDRC_MH_TOD": 431.5, "US_PREDRC_MH_TOT": 1.3903, "US_PREDUP_LM_TOD": 588.1, "US_PREDUP_LM_TOT": 1.0202, "US_PREDUP_MH_TOD": 505.1, "US_PREDUP_MH_TOT": 1.1878, "US_WL6040_LM_TOD": 704.1, "US_WL6040_LM_TOT": 0.8522, "US_WL6040_MH_TOD": 574.9, "US_WL6040_MH_TOT": 1.0436, "TND_Offshore_High": 452.1, "TN_Inshore_Medium": 0.9768, "US_TN_Inshore_Low": 0.72, "TN_Offshore_Medium": 1.1979, "TND_Offshore_Medium": 500.9}
04560003WR7	\N	{"BIN": "US59793", "APHD": 628.4, "APHT": 0.9548, "Class": "TARTAN 37 DK", "ILCWA": 777.1, "RefNo": "04560003WR7", "C_Type": "NSCL", "CertNo": "US59793", "SailNo": "USA 59793", "Builder": "TARTAN MARINE", "NatAuth": "USA", "Designer": "S&S", "Division": "C", "IssueDate": "2025-08-18T12:16:26.988Z", "YachtName": "WAYMAKER", "TMF_Inshore": 0.7721, "US_5B_H_TOD": 632.3, "US_5B_H_TOT": 0.949, "US_5B_L_TOD": 1239.7, "US_5B_L_TOT": 0.484, "US_5B_M_TOD": 758.2, "US_5B_M_TOT": 0.7913, "US_AP_H_TOD": 535.7, "US_AP_H_TOT": 1.1201, "US_AP_L_TOD": 920.2, "US_AP_L_TOT": 0.652, "US_AP_M_TOD": 615.0, "US_AP_M_TOT": 0.9757, "US_5B_LM_TOD": 905.4, "US_5B_LM_TOT": 0.6627, "US_5B_MH_TOD": 668.5, "US_5B_MH_TOT": 0.8975, "US_AP_LM_TOD": 713.3, "US_AP_LM_TOT": 0.8412, "US_AP_MH_TOD": 559.2, "US_AP_MH_TOT": 1.073, "US_PREDDN_TOD": 687.0, "US_PREDDN_TOT": 0.8733, "US_PREDRC_TOD": 625.1, "US_PREDRC_TOT": 0.9598, "US_PREDUP_TOD": 689.4, "US_PREDUP_TOT": 0.8703, "TN_Inshore_High": 0.9126, "TN_Offshore_Low": 0.7311, "US_PREDDN_H_TOD": 514.7, "US_PREDDN_H_TOT": 1.1657, "US_PREDDN_L_TOD": 1039.4, "US_PREDDN_L_TOT": 0.5773, "US_PREDDN_M_TOD": 628.5, "US_PREDDN_M_TOT": 0.9547, "US_PREDRC_H_TOD": 508.1, "US_PREDRC_H_TOT": 1.1809, "US_PREDRC_L_TOD": 883.3, "US_PREDRC_L_TOT": 0.6793, "US_PREDRC_M_TOD": 580.0, "US_PREDRC_M_TOT": 1.0345, "US_PREDUP_H_TOD": 581.7, "US_PREDUP_H_TOT": 1.0315, "US_PREDUP_L_TOD": 950.2, "US_PREDUP_L_TOT": 0.6314, "US_PREDUP_M_TOD": 641.4, "US_PREDUP_M_TOT": 0.9354, "US_WL6040_H_TOD": 655.1, "US_WL6040_H_TOT": 0.9159, "US_WL6040_L_TOD": 1222.7, "US_WL6040_L_TOT": 0.4907, "US_WL6040_M_TOD": 765.6, "US_WL6040_M_TOT": 0.7837, "TND_Offshore_Low": 820.7, "TN_Offshore_High": 1.0867, "US_PREDDN_LM_TOD": 755.4, "US_PREDDN_LM_TOT": 0.7943, "US_PREDDN_MH_TOD": 550.3, "US_PREDDN_MH_TOT": 1.0902, "US_PREDRC_LM_TOD": 669.0, "US_PREDRC_LM_TOT": 0.8968, "US_PREDRC_MH_TOD": 530.4, "US_PREDRC_MH_TOT": 1.1312, "US_PREDUP_LM_TOD": 729.1, "US_PREDUP_LM_TOT": 0.823, "US_PREDUP_MH_TOD": 597.2, "US_PREDUP_MH_TOT": 1.0046, "US_WL6040_LM_TOD": 902.8, "US_WL6040_LM_TOT": 0.6646, "US_WL6040_MH_TOD": 685.7, "US_WL6040_MH_TOT": 0.875, "TND_Offshore_High": 552.1, "TN_Inshore_Medium": 0.7752, "US_TN_Inshore_Low": 0.5415, "TN_Offshore_Medium": 0.9602, "TND_Offshore_Medium": 624.9}
04560003WR8	\N	{"BIN": "US59793", "APHD": 607.5, "APHT": 0.9876, "Class": "TARTAN 37 DK", "ILCWA": 750.7, "RefNo": "04560003WR8", "C_Type": "DHCL", "CertNo": "US59793", "SailNo": "USA 59793", "Builder": "TARTAN MARINE", "NatAuth": "USA", "Designer": "S&S", "Division": "C", "IssueDate": "2025-08-18T12:17:23.081Z", "YachtName": "WAYMAKER", "TMF_Inshore": 0.7992, "US_5B_H_TOD": 638.6, "US_5B_H_TOT": 0.9395, "US_5B_L_TOD": 1144.3, "US_5B_L_TOT": 0.5243, "US_5B_M_TOD": 734.1, "US_5B_M_TOT": 0.8174, "US_AP_H_TOD": 532.6, "US_AP_H_TOT": 1.1266, "US_AP_L_TOD": 852.2, "US_AP_L_TOT": 0.7041, "US_AP_M_TOD": 595.6, "US_AP_M_TOT": 1.0074, "US_5B_LM_TOD": 853.2, "US_5B_LM_TOT": 0.7032, "US_5B_MH_TOD": 668.0, "US_5B_MH_TOT": 0.8982, "US_AP_LM_TOD": 677.9, "US_AP_LM_TOT": 0.8851, "US_AP_MH_TOD": 551.9, "US_AP_MH_TOT": 1.0871, "US_PREDDN_TOD": 644.4, "US_PREDDN_TOT": 0.9311, "US_PREDRC_TOD": 603.9, "US_PREDRC_TOT": 0.9936, "US_PREDUP_TOD": 680.8, "US_PREDUP_TOT": 0.8813, "TN_Inshore_High": 0.9103, "TN_Offshore_Low": 0.7774, "US_PREDDN_H_TOD": 507.0, "US_PREDDN_H_TOT": 1.1834, "US_PREDDN_L_TOD": 931.9, "US_PREDDN_L_TOT": 0.6439, "US_PREDDN_M_TOD": 595.8, "US_PREDDN_M_TOT": 1.0071, "US_PREDRC_H_TOD": 503.1, "US_PREDRC_H_TOT": 1.1926, "US_PREDRC_L_TOD": 829.6, "US_PREDRC_L_TOT": 0.7233, "US_PREDRC_M_TOD": 564.0, "US_PREDRC_M_TOT": 1.0639, "US_PREDUP_H_TOD": 585.2, "US_PREDUP_H_TOT": 1.0253, "US_PREDUP_L_TOD": 919.4, "US_PREDUP_L_TOT": 0.6526, "US_PREDUP_M_TOD": 636.2, "US_PREDUP_M_TOT": 0.9432, "US_WL6040_H_TOD": 663.1, "US_WL6040_H_TOT": 0.9049, "US_WL6040_L_TOD": 1145.9, "US_WL6040_L_TOT": 0.5236, "US_WL6040_M_TOD": 748.3, "US_WL6040_M_TOT": 0.8018, "TND_Offshore_Low": 771.8, "TN_Offshore_High": 1.0985, "US_PREDDN_LM_TOD": 694.9, "US_PREDDN_LM_TOT": 0.8635, "US_PREDDN_MH_TOD": 538.2, "US_PREDDN_MH_TOT": 1.1147, "US_PREDRC_LM_TOD": 639.4, "US_PREDRC_LM_TOT": 0.9384, "US_PREDRC_MH_TOD": 523.9, "US_PREDRC_MH_TOT": 1.1453, "US_PREDUP_LM_TOD": 714.2, "US_PREDUP_LM_TOT": 0.8401, "US_PREDUP_MH_TOD": 598.8, "US_PREDUP_MH_TOT": 1.002, "US_WL6040_LM_TOD": 862.0, "US_WL6040_LM_TOT": 0.6961, "US_WL6040_MH_TOD": 687.8, "US_WL6040_MH_TOT": 0.8724, "TND_Offshore_High": 546.2, "TN_Inshore_Medium": 0.8045, "US_TN_Inshore_Low": 0.5833, "TN_Offshore_Medium": 0.9945, "TND_Offshore_Medium": 603.3}
04560003WR9	\N	{"BIN": "US59793", "APHD": 604.7, "APHT": 0.9923, "Class": "TARTAN 37 DK", "ILCWA": 746.5, "RefNo": "04560003WR9", "C_Type": "CLUB", "CertNo": "US59793", "SailNo": "USA 59793", "Builder": "TARTAN MARINE", "NatAuth": "USA", "Designer": "S&S", "Division": "C", "IssueDate": "2025-08-18T12:19:45.802Z", "YachtName": "WAYMAKER", "TMF_Inshore": 0.8037, "US_5B_H_TOD": 631.4, "US_5B_H_TOT": 0.9502, "US_5B_L_TOD": 1147.1, "US_5B_L_TOT": 0.5231, "US_5B_M_TOD": 729.5, "US_5B_M_TOT": 0.8225, "US_AP_H_TOD": 527.9, "US_AP_H_TOT": 1.1366, "US_AP_L_TOD": 852.6, "US_AP_L_TOT": 0.7037, "US_AP_M_TOD": 592.5, "US_AP_M_TOT": 1.0126, "US_5B_LM_TOD": 851.6, "US_5B_LM_TOT": 0.7046, "US_5B_MH_TOD": 661.8, "US_5B_MH_TOT": 0.9066, "US_AP_LM_TOD": 676.4, "US_AP_LM_TOT": 0.887, "US_AP_MH_TOD": 548.0, "US_AP_MH_TOT": 1.0949, "US_PREDDN_TOD": 643.6, "US_PREDDN_TOT": 0.9323, "US_PREDRC_TOD": 602.0, "US_PREDRC_TOT": 0.9967, "US_PREDUP_TOD": 676.8, "US_PREDUP_TOT": 0.8865, "TN_Inshore_High": 0.9193, "TN_Offshore_Low": 0.7774, "US_PREDDN_H_TOD": 505.0, "US_PREDDN_H_TOT": 1.1882, "US_PREDDN_L_TOD": 933.4, "US_PREDDN_L_TOT": 0.6428, "US_PREDDN_M_TOD": 594.5, "US_PREDDN_M_TOT": 1.0092, "US_PREDRC_H_TOD": 500.2, "US_PREDRC_H_TOT": 1.1995, "US_PREDRC_L_TOD": 830.0, "US_PREDRC_L_TOT": 0.7229, "US_PREDRC_M_TOD": 561.7, "US_PREDRC_M_TOT": 1.0682, "US_PREDUP_H_TOD": 578.4, "US_PREDUP_H_TOT": 1.0374, "US_PREDUP_L_TOD": 920.4, "US_PREDUP_L_TOT": 0.6519, "US_PREDUP_M_TOD": 631.4, "US_PREDUP_M_TOT": 0.9503, "US_WL6040_H_TOD": 654.3, "US_WL6040_H_TOT": 0.917, "US_WL6040_L_TOD": 1148.7, "US_WL6040_L_TOT": 0.5223, "US_WL6040_M_TOD": 742.6, "US_WL6040_M_TOT": 0.808, "TND_Offshore_Low": 771.8, "TN_Offshore_High": 1.1068, "US_PREDDN_LM_TOD": 694.7, "US_PREDDN_LM_TOT": 0.8637, "US_PREDDN_MH_TOD": 536.5, "US_PREDDN_MH_TOT": 1.1183, "US_PREDRC_LM_TOD": 638.2, "US_PREDRC_LM_TOT": 0.9401, "US_PREDRC_MH_TOD": 521.3, "US_PREDRC_MH_TOT": 1.151, "US_PREDUP_LM_TOD": 711.8, "US_PREDUP_LM_TOT": 0.843, "US_PREDUP_MH_TOD": 592.9, "US_PREDUP_MH_TOT": 1.012, "US_WL6040_LM_TOD": 859.7, "US_WL6040_LM_TOT": 0.6979, "US_WL6040_MH_TOD": 680.2, "US_WL6040_MH_TOT": 0.882, "TND_Offshore_High": 542.1, "TN_Inshore_Medium": 0.8091, "US_TN_Inshore_Low": 0.5826, "TN_Offshore_Medium": 0.9992, "TND_Offshore_Medium": 600.5}
04560004KI4	63	{"BIN": "US3603", "APHD": 476.0, "APHT": 1.2606, "Class": "FARR 36 OD", "ILCWA": 584.9, "RefNo": "04560004KI4", "C_Type": "CLUB", "CertNo": "US3603", "SailNo": "USA 3", "Builder": "BETTS", "NatAuth": "USA", "Designer": "B.FARR", "Division": "R", "IssueDate": "2026-04-07T16:51:21.001Z", "YachtName": "WICKED SISTER", "TMF_Inshore": 1.0258, "US_5B_H_TOD": 471.1, "US_5B_H_TOT": 1.2735, "US_5B_L_TOD": 861.2, "US_5B_L_TOT": 0.6967, "US_5B_M_TOD": 577.2, "US_5B_M_TOT": 1.0396, "US_AP_H_TOD": 397.5, "US_AP_H_TOT": 1.5094, "US_AP_L_TOD": 638.5, "US_AP_L_TOT": 0.9397, "US_AP_M_TOD": 470.9, "US_AP_M_TOT": 1.2742, "US_5B_LM_TOD": 656.1, "US_5B_LM_TOT": 0.9145, "US_5B_MH_TOD": 515.9, "US_5B_MH_TOT": 1.163, "US_AP_LM_TOD": 525.5, "US_AP_LM_TOT": 1.1417, "US_AP_MH_TOD": 427.6, "US_AP_MH_TOT": 1.4033, "US_PREDDN_TOD": 484.5, "US_PREDDN_TOT": 1.2383, "US_PREDRC_TOD": 463.5, "US_PREDRC_TOT": 1.2945, "US_PREDUP_TOD": 537.9, "US_PREDUP_TOT": 1.1154, "TN_Inshore_High": 1.1909, "TN_Offshore_Low": 1.0297, "US_PREDDN_H_TOD": 349.2, "US_PREDDN_H_TOT": 1.718, "US_PREDDN_L_TOD": 692.8, "US_PREDDN_L_TOT": 0.8661, "US_PREDDN_M_TOD": 457.5, "US_PREDDN_M_TOT": 1.3113, "US_PREDRC_H_TOD": 370.9, "US_PREDRC_H_TOT": 1.6178, "US_PREDRC_L_TOD": 620.2, "US_PREDRC_L_TOT": 0.9674, "US_PREDRC_M_TOD": 441.0, "US_PREDRC_M_TOT": 1.3607, "US_PREDUP_H_TOD": 461.4, "US_PREDUP_H_TOT": 1.3004, "US_PREDUP_L_TOD": 697.1, "US_PREDUP_L_TOT": 0.8607, "US_PREDUP_M_TOD": 511.0, "US_PREDUP_M_TOT": 1.1743, "US_WL6040_H_TOD": 501.9, "US_WL6040_H_TOT": 1.1955, "US_WL6040_L_TOD": 865.5, "US_WL6040_L_TOT": 0.6933, "US_WL6040_M_TOD": 592.0, "US_WL6040_M_TOT": 1.0134, "TND_Offshore_Low": 582.7, "TN_Offshore_High": 1.4308, "US_PREDDN_LM_TOD": 529.1, "US_PREDDN_LM_TOT": 1.1341, "US_PREDDN_MH_TOD": 395.2, "US_PREDDN_MH_TOT": 1.5182, "US_PREDRC_LM_TOD": 493.8, "US_PREDRC_LM_TOT": 1.2151, "US_PREDRC_MH_TOD": 398.2, "US_PREDRC_MH_TOT": 1.5069, "US_PREDUP_LM_TOD": 558.8, "US_PREDUP_LM_TOT": 1.0738, "US_PREDUP_MH_TOD": 478.6, "US_PREDUP_MH_TOT": 1.2537, "US_WL6040_LM_TOD": 665.6, "US_WL6040_LM_TOT": 0.9014, "US_WL6040_MH_TOD": 538.2, "US_WL6040_MH_TOT": 1.1148, "TND_Offshore_High": 419.3, "TN_Inshore_Medium": 1.024, "US_TN_Inshore_Low": 0.7729, "TN_Offshore_Medium": 1.2574, "TND_Offshore_Medium": 477.2}
\.


--
-- Data for Name: person; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.person (id, first, last, club_member, active) FROM stdin;
2	John	Leitzinger	t	t
3	Ron	Holbrook	t	t
4	Eric	Johnson	t	t
5	Pat	Robinson	f	t
6	Jerry	McKay	f	t
7	Dean	Conti	f	t
8	Zachery	Tuck	t	t
10	Barker	Glassley	t	t
11	Jeremy	Bush	t	t
12	Jay	Pyles	t	t
13	Chris	Caudill	f	t
14	Charles	Hendrick	f	t
16	Forrest	German	f	t
17	Nelson	Grove	t	t
18	Jean	Fedor	t	t
19	Aaron	Milligan	t	t
21	Greg	Greene	t	t
22	Rick	Garrity	f	t
25	Ross	Chamberlain	f	t
27	Angie	Littleton	t	t
23	William	Kamp	t	t
1	Ed	Pinkham	t	t
29	Gordon	Kells-Murphy	f	t
30	Tony	Polizzi	t	t
31	Danny	Kenck	t	t
51	Brian	White	t	t
52	Edmond	Jacobs	f	t
53	Paul	Case	f	t
54	Stephen	Ryan	t	t
55	Bruce	Campbell	f	t
56	Marshall	Jett	f	t
57	William	Barker	t	t
58	Dave	Cross	f	t
59	Ian	Sprenger	f	t
60	Dave	Stromquist	f	t
61	Connie	Peterson	f	t
62	Timothy	McMorrow	t	t
63	Jon	Knudson	f	t
64	Fred	Creitz	f	t
65	Steve	Wagner	t	t
66	Matt	Thomas	t	t
69	Mike / Brad	Jones	f	t
70	Mark	Anderson	f	t
71	Eric	Nelson	t	t
72	Barry	Ward	f	t
73	Greg/Vanessa	Zaputil	f	t
74	Robert	Silvernagel	f	t
75	Bryce	Reynolds	f	t
76	Martin	Buehler	f	t
78	Jason	Hanford	t	t
79	Emily	Wood	t	t
107	Ian	Crouse	f	t
108	Ryan	Olmstead	t	t
109	Ian	Gegg	t	t
111	Grant	Meumann	f	t
116	Kurt	Hrubant	t	t
91	Mike	Clidas	f	t
117	Edward	Lansing	t	t
118	Andrew	Nelson	f	t
119	Jacob	Hinton	f	t
28	Brad	Olney	t	t
120	Dana	Bachellerie	t	t
121	Greg	Overton	t	t
122	Brock	VanRavenswaay	t	t
128	Johan	Zimmerman	f	t
129	Kyle	Roethle	t	t
130	Michael	Hamilton	f	t
32	Christine	Nelson	t	t
67	Frank	Kors	t	t
132	Derek	DeCouteau	f	t
133	Julian	Williams	f	t
134	Chuck	Lido	f	t
135	Todd	Lido	f	t
136	Brian	Lido	f	t
137	Travis	Lido	f	t
138	Aaron	Lido	f	t
139	Breanna	Lido	f	t
140	Michael	Lido	f	t
141	Erica	Lido	f	t
110	Kurt	Kulesza	t	t
\.


--
-- Data for Name: race; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.race (id, name, series_id, rc_id, correction_factor, orc_option, orc_3, orc_5) FROM stdin;
28	Summer Series Windseekers #2	15	\N	550	\N	\N	\N
24	Late Spring Windseekers #2	14	\N	600	\N	\N	\N
3	Harbor Series #3	1	31	600	\N	\N	\N
17	Spring Single/Double Handed Race	\N	31	600	\N	\N	\N
30	Summer Series Windseekers #3	15	\N	600	\N	\N	\N
31	Summer Series Windseekers #4	15	31	550	\N	\N	\N
36	Late Summer Windseekers #5	17	\N	600	\N	\N	\N
42	Summer Series Windseekers #5	15	\N	550	\N	\N	\N
1	Harbor Series #1	1	31	600	\N	\N	\N
2	Harbor Series #2	1	31	600	\N	\N	\N
43	Late Summer Windseekers #1	17	\N	550	\N	\N	\N
44	Late Summer Windseekers #2	17	\N	550	\N	\N	\N
45	Late Summer Windseekers #3	17	\N	600	\N	\N	\N
47	Three Hour Tour #3	16	\N	600	\N	\N	\N
34	Late Summer Windseekers #4	17	\N	650	\N	\N	\N
18	Early Spring Windseekers #1	13	\N	550	\N	\N	\N
19	Early Spring Windseekers #2	13	\N	600	\N	\N	\N
20	Early Spring Windseekers #3	13	\N	550	\N	\N	\N
35	Vashon Challenge	\N	\N	550	\N	\N	\N
32	Three Hour Tour #1	16	\N	550	\N	\N	\N
33	Three Hour Tour #2	16	\N	600	\N	\N	\N
21	Early Spring Windseekers #4	13	\N	600	\N	\N	\N
37	Awards Race and Ceremony	\N	31	550	\N	\N	\N
38	Point Series #1	18	31	550	\N	\N	\N
22	Early Spring Windseekers #5	13	\N	480	\N	\N	\N
23	Late Spring Windseekers #1	14	\N	550	\N	\N	\N
25	Late Spring Windseekers #3	14	\N	550	\N	\N	\N
26	Late Spring Windseekers #4	14	\N	480	\N	\N	\N
27	Late Spring Windseekers #5	14	\N	480	\N	\N	\N
29	Summer Series Windseekers #1	15	\N	550	\N	\N	\N
46	Point Series #2	18	31	550	\N	\N	\N
39	Memorial Single/Double Handed Race	\N	\N	550	\N	\N	\N
40	Point Series #3	18	\N	480	\N	\N	\N
48	Harbor Series #1	1	\N	600	\N	\N	\N
41	Point Series #4	18	\N	550	\N	\N	\N
49	Harbor Series #2	1	\N	480	\N	\N	\N
50	Harbor Series #3	1	\N	600	\N	\N	\N
84	Point Series #3	18	\N	600	\N	\N	\N
52	Spring Single - Double Handed Race	\N	\N	550	\N	\N	\N
53	Early Spring Windseekers #1	13	\N	600	\N	\N	\N
66	Summer Series Windseekers #2	15	\N	550	\N	\N	\N
54	Early Spring Windseekers #2	13	\N	480	\N	\N	\N
92	Early Spring Windseekers #3	13	67	480	FiveBandWindwardLeeward	Low	LowMedium
55	Early Spring Windseekers #3	13	\N	550	\N	\N	\N
57	Early Spring Windseekers #5	13	\N	480	\N	\N	\N
58	Late Spring Windseekers #1	14	\N	550	\N	\N	\N
59	Late Spring Windseekers #2	14	\N	600	\N	\N	\N
67	Ladies take the Lead	\N	\N	600	\N	\N	\N
56	Early Spring Windseekers #4	13	\N	600	\N	\N	\N
68	Summer Series Windseekers #3	15	\N	600	\N	\N	\N
69	Summer Series Windseekers #4	15	\N	550	\N	\N	\N
77	Vashon Challenge	\N	\N	550	\N	\N	\N
78	Late Summer Windseekers #5	17	\N	550	\N	\N	\N
79	Awards Race	\N	\N	550	\N	\N	\N
70	Three Hour Tour #2	16	\N	600	\N	\N	\N
71	Summer Series Windseekers #5	15	\N	550	\N	\N	\N
60	Late Spring Windseekers #3	14	31	550	\N	\N	\N
61	Late Spring Windseekers #4	14	\N	650	\N	\N	\N
62	Late Spring Windseekers #5	14	\N	550	\N	\N	\N
64	Summer Series Windseekers #1	15	\N	550	\N	\N	\N
65	Three Hour Tour #1	16	\N	600	\N	\N	\N
72	Late Summer Windseekers #1	17	\N	600	\N	\N	\N
73	Late Summer Windseekers #2	17	\N	600	\N	\N	\N
74	Late Summer Windseekers #3	17	\N	600	\N	\N	\N
75	Three Hour Tour #3	16	\N	550	\N	\N	\N
76	Late Summer Windseekers #4	17	\N	550	\N	\N	\N
106	Summer Series Windseekers #3	15	67	650	FiveBandWindwardLeeward	Low	LowMedium
102	Summer Series Windseekers #2	15	67	480	FiveBandWindwardLeeward	Low	MediumHigh
97	Late Spring Windseekers #3	14	67	600	FiveBandWindwardLeeward	Low	LowMedium
80	Point Series #1	18	\N	550	\N	\N	\N
82	Point Series #2	18	\N	600	\N	\N	\N
83	Memorial Single/Double Handed Race	\N	\N	600	\N	\N	\N
115	Awards Race and Ceremony	\N	67	550	FiveBandWindwardLeeward	Low	LowMedium
88	Harbor Series #3	1	67	550	FiveBandWindwardLeeward	Low	LowMedium
86	Harbor Series #1	1	67	650	FiveBandWindwardLeeward	Low	Low
87	Harbor Series #2	1	67	650	FiveBandWindwardLeeward	Low	Low
91	Early Spring Windseekers #2	13	67	600	FiveBandWindwardLeeward	Low	LowMedium
99	Late Spring Windseekers #4	14	31	480	FiveBandWindwardLeeward	Low	LowMedium
94	Early Spring Windseekers #5	13	67	550	FiveBandWindwardLeeward	Low	LowMedium
100	Late Spring Windseekers #5	14	67	600	FiveBandWindwardLeeward	Low	LowMedium
96	Late Spring Windseekers #2	14	67	550	FiveBandWindwardLeeward	Low	LowMedium
109	Late Summer Windseekers  #1	17	\N	600	FiveBandWindwardLeeward	Low	LowMedium
104	Three Hour Tour #2	16	67	550	FiveBandWindwardLeeward	Low	LowMedium
114	Vashon Challenge	\N	67	550	FiveBandWindwardLeeward	Low	LowMedium
105	Three Hour Tour #3	16	67	550	FiveBandWindwardLeeward	Low	LowMedium
110	Late Summer Windseekers  #2	17	67	550	FiveBandWindwardLeeward	Low	LowMedium
113	Late Summer Windseekers  #5	17	67	550	FiveBandWindwardLeeward	Low	LowMedium
89	Spring Single/Double Handed Race	\N	67	650	FiveBandWindwardLeeward	Low	Low
90	Early Spring Windseekers #1	13	67	600	FiveBandWindwardLeeward	Low	LowMedium
93	Early Spring Windseekers #4	13	67	650	FiveBandWindwardLeeward	Low	LowMedium
95	Late Spring Windseekers #1	14	67	480	FiveBandWindwardLeeward	Low	LowMedium
98	Ladies Take The Lead	\N	31	550	FiveBandWindwardLeeward	Low	LowMedium
136	Lido 14 Late-Summer/Fall Series #6B	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
137	Lido 14 Late-Summer/Fall Series #6C	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
116	Memorial Single/Double Handed Race	\N	67	550	FiveBandWindwardLeeward	Low	LowMedium
129	Lido 14 Late-Summer/Fall Windsday Series #1B	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
130	Lido 14 Late-Summer/Fall Windsday Series #1C	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
131	Lido 14 Late-Summer/Fall Windsday Series #1D	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
123	Lido 14 Late-Summer/Fall Windsday Series #1A	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
132	Lido 14 Late-Summer/Fall Windsday Series #3B	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
133	Lido 14 Late-Summer/Fall Windsday Series #3C	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
101	Summer Series Windseekers #1	15	67	600	TripleNumberAllPurpose	Low	LowMedium
125	Lido 14 Late-Summer/Fall Windsday Series #3A	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
117	Point Series #1	18	67	600	FiveBandWindwardLeeward	Low	LowMedium
103	Three Hour Tour #1	16	31	550	FiveBandWindwardLeeward	Low	LowMedium
119	Point Series #3	18	\N	600	FiveBandWindwardLeeward	Low	LowMedium
121	Point Series #4	18	\N	600	FiveBandWindwardLeeward	Low	LowMedium
118	Point Series #2	18	67	600	FiveBandWindwardLeeward	Low	LowMedium
107	Summer Series Windseekers #4	15	67	550	FiveBandWindwardLeeward	Low	LowMedium
108	Summer Series Windseekers #5	15	67	550	FiveBandWindwardLeeward	Low	LowMedium
127	Lido 14 Late-Summer/Fall Windsay Series #5A	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
111	Late Summer Windseekers  #3	17	67	600	FiveBandWindwardLeeward	Low	LowMedium
112	Late Summer Windseekers  #4	17	67	550	FiveBandWindwardLeeward	Low	LowMedium
124	Lido 14 Late-Summer/Fall Windsday Series #2	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
126	Lido 14 Late-Summer/Fall Windsday Series #4	19	\N	600	FiveBandWindwardLeeward	Low	LowMedium
134	Lido 14 Late-Summer/Fall Windsay Series #5B	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
135	Lido 14 Late-Summer/Fall Windsay Series #5C	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
128	Lido 14 Late-Summer/Fall Series #6A	19	67	600	FiveBandWindwardLeeward	Low	LowMedium
\.


--
-- Data for Name: racebracketjunction; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.racebracketjunction (bracket_id, race_id, class_id) FROM stdin;
1	24	1
2	24	1
3	24	2
4	24	2
19	24	4
18	24	3
1	28	1
2	28	1
3	28	2
4	28	2
19	28	4
18	28	3
1	35	1
2	35	1
3	35	2
4	35	2
19	35	4
18	35	3
16	1	1
17	1	2
19	1	4
18	1	3
16	2	1
17	2	2
19	2	4
18	2	3
16	33	1
17	33	2
19	33	4
18	33	3
1	42	1
2	42	1
1	18	1
2	18	1
3	18	2
4	18	2
19	18	4
18	18	3
3	42	2
4	42	2
19	42	4
18	42	3
1	43	1
2	43	1
3	43	2
4	43	2
19	43	4
18	43	3
1	45	1
2	45	1
3	45	2
4	45	2
19	45	4
18	45	3
1	34	1
2	34	1
3	34	2
4	34	2
19	34	4
18	34	3
1	23	1
2	23	1
1	20	1
2	20	1
3	20	2
4	20	2
19	20	4
18	20	3
1	30	1
2	30	1
3	30	2
4	30	2
3	23	2
4	23	2
19	23	4
18	23	3
1	25	1
2	25	1
3	25	2
4	25	2
19	25	4
18	25	3
1	27	1
2	27	1
3	27	2
4	27	2
19	30	4
18	30	3
1	31	1
2	31	1
3	31	2
4	31	2
19	27	4
18	27	3
19	31	4
18	31	3
1	76	1
2	76	1
3	76	2
4	76	2
19	76	4
18	76	3
1	62	1
2	62	1
3	62	2
4	62	2
19	62	4
18	62	3
28	77	7
31	77	9
30	77	8
29	77	5
1	69	1
2	69	1
3	69	2
4	69	2
19	69	4
18	69	3
16	3	1
17	3	2
19	3	4
18	3	3
1	21	1
2	21	1
3	21	2
4	21	2
16	17	1
17	17	2
19	17	4
18	17	3
19	21	4
18	21	3
2	46	1
1	71	1
2	71	1
3	71	2
4	71	2
19	71	4
18	71	3
1	73	1
2	73	1
3	73	2
4	73	2
19	73	4
18	73	3
1	96	1
2	96	1
3	96	2
4	96	2
19	96	4
18	96	3
1	97	1
2	97	1
3	97	2
1	46	1
3	46	2
4	46	2
19	46	4
18	46	3
1	39	1
2	39	1
3	39	2
4	39	2
19	39	4
18	39	3
16	32	1
17	32	2
19	32	4
18	32	3
1	40	1
2	40	1
1	36	1
2	36	1
3	36	2
4	36	2
19	36	4
18	36	3
2	37	1
1	37	1
1	44	1
2	44	1
3	44	2
4	44	2
19	44	4
18	44	3
3	40	2
4	40	2
19	40	4
18	40	3
16	48	1
17	48	2
16	47	1
17	47	2
19	47	4
18	47	3
1	22	1
2	22	1
3	22	2
4	22	2
19	22	4
18	22	3
19	48	4
18	48	3
3	37	2
4	37	2
19	37	4
18	37	3
1	38	1
2	38	1
16	86	1
17	86	2
19	86	4
18	86	3
64	109	37
65	109	37
68	109	38
67	109	38
3	38	2
4	38	2
1	26	1
2	26	1
3	26	2
4	26	2
19	26	4
18	26	3
1	19	1
2	19	1
3	19	2
4	19	2
19	19	4
18	19	3
19	38	4
18	38	3
19	109	4
18	109	3
72	124	40
1	29	1
2	29	1
3	29	2
4	29	2
19	29	4
18	29	3
1	60	1
2	60	1
3	60	2
4	60	2
19	60	4
18	60	3
1	64	1
2	64	1
3	64	2
4	64	2
19	64	4
18	64	3
1	92	1
2	92	1
3	92	2
4	92	2
19	92	4
18	92	3
1	93	1
2	93	1
1	66	1
2	66	1
3	66	2
4	66	2
19	66	4
18	66	3
16	67	1
17	67	2
3	93	2
4	93	2
19	93	4
18	93	3
1	78	1
2	78	1
3	78	2
4	78	2
19	78	4
18	78	3
1	95	1
2	95	1
3	95	2
4	95	2
64	115	37
65	115	37
67	115	38
68	115	38
19	95	4
18	95	3
19	115	4
4	97	2
19	97	4
18	97	3
19	67	4
18	67	3
1	72	1
2	72	1
3	72	2
4	72	2
19	72	4
18	72	3
1	74	1
2	74	1
3	74	2
4	74	2
19	74	4
18	74	3
16	87	1
17	87	2
19	87	4
18	87	3
1	90	1
2	90	1
3	90	2
4	90	2
19	90	4
18	90	3
1	41	1
2	41	1
3	41	2
4	41	2
19	41	4
18	41	3
16	49	1
17	49	2
19	49	4
18	49	3
16	50	1
17	50	2
19	50	4
18	50	3
16	98	1
16	52	1
17	52	2
19	52	4
18	52	3
1	75	1
2	75	1
3	75	2
4	75	2
1	53	1
2	53	1
3	53	2
4	53	2
19	53	4
18	53	3
19	75	4
18	75	3
1	61	1
2	61	1
3	61	2
4	61	2
19	61	4
18	61	3
1	65	1
2	65	1
3	65	2
4	65	2
1	54	1
2	54	1
3	54	2
4	54	2
19	54	4
18	54	3
19	65	4
18	65	3
17	98	2
19	98	4
1	55	1
2	55	1
3	55	2
4	55	2
19	55	4
18	55	3
18	98	3
1	100	1
1	68	1
2	68	1
3	68	2
4	68	2
1	57	1
2	57	1
3	57	2
4	57	2
19	57	4
18	57	3
19	68	4
18	68	3
1	70	1
2	70	1
3	70	2
4	70	2
19	70	4
18	70	3
1	58	1
2	58	1
3	58	2
4	58	2
19	58	4
18	58	3
2	100	1
3	100	2
4	100	2
19	100	4
1	59	1
2	59	1
3	59	2
4	59	2
19	59	4
18	59	3
18	100	3
64	107	37
65	107	37
67	107	38
1	79	1
2	79	1
1	56	1
2	56	1
3	56	2
4	56	2
19	56	4
18	56	3
3	79	2
4	79	2
19	79	4
18	79	3
68	107	38
64	101	37
65	101	37
67	101	38
68	101	38
19	101	4
18	101	3
19	107	4
18	107	3
64	108	37
65	108	37
67	108	38
68	108	38
19	108	4
18	108	3
64	111	37
65	111	37
67	111	38
68	111	38
16	88	1
28	114	7
31	114	9
29	114	5
30	114	8
62	114	35
63	114	36
18	115	3
72	130	40
72	131	40
72	133	40
17	88	2
19	88	4
18	88	3
16	89	1
66	117	37
64	104	37
65	104	37
67	104	38
68	104	38
19	104	4
18	104	3
69	117	38
19	117	4
18	117	3
66	119	37
69	119	38
19	119	4
18	119	3
66	118	37
69	118	38
19	118	4
18	118	3
72	134	40
72	135	40
72	136	40
62	116	35
63	116	36
19	111	4
18	111	3
65	103	37
64	103	37
67	103	38
68	103	38
19	103	4
18	103	3
1	80	1
2	80	1
3	80	2
4	80	2
19	80	4
18	80	3
1	82	1
2	82	1
3	82	2
4	82	2
19	82	4
18	82	3
1	84	1
2	84	1
3	84	2
4	84	2
19	84	4
18	84	3
62	83	35
63	83	36
17	89	2
19	89	4
18	89	3
1	91	1
2	91	1
4	91	2
3	91	2
19	91	4
18	91	3
1	94	1
2	94	1
3	94	2
4	94	2
19	94	4
18	94	3
72	126	40
64	113	37
65	113	37
67	113	38
68	113	38
19	113	4
1	99	1
2	99	1
3	99	2
4	99	2
19	99	4
18	99	3
18	113	3
72	129	40
64	106	37
65	106	37
67	106	38
68	106	38
19	106	4
18	106	3
64	102	37
65	102	37
67	102	38
68	102	38
19	102	4
18	102	3
72	123	40
72	132	40
72	125	40
66	121	37
69	121	38
19	121	4
64	110	37
65	110	37
67	110	38
68	110	38
19	110	4
18	110	3
64	105	37
65	105	37
67	105	38
68	105	38
19	105	4
18	105	3
64	112	37
65	112	37
67	112	38
68	112	38
19	112	4
18	112	3
18	121	3
72	127	40
72	128	40
72	137	40
\.


--
-- Data for Name: raceclass; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.raceclass (id, name, active, sort, rating_type) FROM stdin;
37	Fast (93-)	t	0	ORC_PHRF
38	Half Fast (94+)	t	1	ORC_PHRF
1	Fast (72-)	t	2	ORC_PHRF
2	Half Fast (73+)	t	3	ORC_PHRF
7	Double Handed	t	4	ORC_PHRF
9	Single Handed	t	5	ORC_PHRF
4	Windseekers FS	t	6	CruisingFlyingSails
3	Windseekers NFS	t	7	CruisingNonFlyingSails
5	Windseekers FS Double Handed	t	8	CruisingFlyingSails
8	Windseeker FS Single Handed	t	9	CruisingFlyingSails
35	Windseekers NFS Double Handed	t	10	CruisingNonFlyingSails
36	Windseekers NFS Single Handed	t	11	CruisingNonFlyingSails
40	Lido 14	t	12	CruisingNonFlyingSails
\.


--
-- Data for Name: raceresults; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.raceresults (id, race_id, boat_id, end_date, phrf_rating, hoc, start_code, penalty, finish_code, class_id, bracket_id, rating_type, orc_ref) FROM stdin;
1549	86	11	2026-01-24 20:31:34	48	\N	\N	\N	TIME	1	16	PHRF	\N
1604	89	50	2026-03-28 19:24:11	63	\N	\N	\N	TIME	1	16	PHRF	\N
1661	92	88	2026-04-23 03:27:31	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1737	95	26	2026-05-14 02:42:16	33	\N	\N	\N	TIME	1	1	PHRF	\N
1792	97	15	2026-05-28 03:26:24	93	\N	\N	\N	TIME	2	3	PHRF	\N
1853	100	39	2026-06-11 03:01:17	144	\N	\N	\N	TIME	2	4	PHRF	\N
1913	102	99	2026-06-25 03:01:18	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1976	106	12	2026-07-02 03:27:33	72	\N	\N	\N	TIME	37	65	PHRF	\N
2035	104	11	2026-07-11 21:07:33	48	\N	\N	\N	TIME	37	64	PHRF	\N
2147	92	19	\N	120	\N	\N	\N	DNS_RC	2	4	PHRF	\N
2091	109	1	\N	150	\N	\N	\N	RET	38	68	PHRF	\N
2215	114	16	2026-08-16 03:04:51	141	\N	\N	\N	TIME	7	28	PHRF	\N
2270	129	114	2026-08-27 01:14:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2365	118	23	2026-09-19 19:58:30	144	\N	\N	\N	TIME	38	69	PHRF	\N
1332	73	6	2025-07-31 03:11:45	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
96	18	24	2024-04-11 03:20:40	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
102	18	8	2024-04-11 03:23:34	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
103	18	6	2024-04-11 03:28:21	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
104	18	37	2024-04-11 03:29:44	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
207	21	24	2024-05-02 03:19:22	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
43	2	20	\N	72	\N	\N	\N	\N	1	16	PHRF	\N
212	20	6	2024-04-25 03:13:05	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
77	17	24	2024-03-30 19:14:44	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
78	17	8	2024-03-30 19:20:08	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
80	17	25	2024-03-30 19:40:26	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
122	19	24	2024-04-18 02:57:59	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
129	19	8	2024-04-18 03:08:07	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
130	19	6	2024-04-18 03:09:03	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1550	86	28	2026-01-24 20:35:35	42	\N	\N	\N	TIME	1	16	ORC_PHRF	04560003L6C
1605	89	28	2026-03-28 19:26:13	42	\N	\N	\N	TIME	1	16	ORC_PHRF	04560003L6C
1683	93	28	2026-04-30 03:11:30	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1738	95	12	2026-05-14 02:42:28	72	\N	\N	\N	TIME	1	2	PHRF	\N
1793	97	27	2026-05-28 03:26:40	72	\N	\N	\N	TIME	1	2	PHRF	\N
1854	100	105	2026-06-11 03:01:57	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1914	102	6	2026-06-25 03:02:51	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1977	106	28	2026-07-02 03:34:04	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
2036	104	63	2026-07-11 21:13:39	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
2092	109	63	\N	18	\N	\N	\N	RET	37	64	ORC_PHRF	04560004KI4
2148	94	98	\N	\N	\N	\N	\N	DNS_RC	4	19	CruisingFlyingSails	\N
2216	114	109	\N	200	\N	\N	\N	RET	5	29	CruisingFlyingSails	\N
2271	129	111	2026-08-27 01:15:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2366	118	21	2026-09-19 19:58:55	195	\N	\N	\N	TIME	38	69	PHRF	\N
213	20	37	2024-04-25 03:13:38	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
155	20	24	2024-04-25 03:04:19	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
225	22	24	2024-05-09 02:43:34	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
230	22	6	2024-05-09 02:50:49	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
231	22	37	2024-05-09 02:53:41	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
283	23	6	2024-05-16 03:39:02	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
284	23	8	2024-05-16 03:43:43	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
285	23	37	2024-05-16 03:47:40	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
335	24	25	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
542	33	47	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
544	30	50	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
571	42	54	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1551	86	15	2026-01-24 20:36:55	93	\N	\N	\N	TIME	2	17	PHRF	\N
1606	89	15	2026-03-28 19:28:37	93	\N	\N	\N	TIME	2	17	PHRF	\N
1684	93	11	2026-04-30 03:25:11	48	\N	\N	\N	TIME	1	1	PHRF	\N
1739	95	22	2026-05-14 02:43:59	69	\N	\N	\N	TIME	1	2	PHRF	\N
1794	97	18	2026-05-28 03:26:55	81	\N	\N	\N	TIME	2	3	PHRF	\N
1855	100	106	2026-06-11 03:02:48	93	\N	\N	\N	TIME	2	3	PHRF	\N
1916	102	93	2026-06-25 03:16:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1978	106	90	2026-07-02 03:36:02	96	\N	\N	\N	TIME	38	67	PHRF	\N
2037	104	18	2026-07-11 21:19:44	84	\N	\N	\N	TIME	37	65	PHRF	\N
2093	109	6	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2149	106	16	\N	141	\N	\N	\N	DNS_RC	38	68	PHRF	\N
2217	113	53	2026-08-20 02:50:59	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2272	129	113	2026-08-27 01:16:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2367	118	16	2026-09-19 20:16:13	141	\N	\N	\N	TIME	38	69	PHRF	\N
356	25	44	2024-05-30 02:59:52	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
358	25	6	2024-05-30 03:00:47	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
362	25	8	2024-05-30 03:04:41	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
364	25	47	2024-05-30 03:17:29	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
378	26	44	2024-06-06 02:48:40	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
381	26	6	2024-06-06 02:52:12	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
400	27	6	2024-06-13 03:10:07	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
403	27	44	2024-06-13 03:12:27	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
421	29	44	2024-06-20 02:46:33	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
422	29	6	2024-06-20 02:49:31	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
432	29	47	2024-06-20 03:01:06	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1555	86	12	2026-01-24 20:52:00	72	\N	\N	\N	TIME	1	16	PHRF	\N
1607	89	90	2026-03-28 19:40:06	96	\N	\N	\N	TIME	2	17	PHRF	\N
1685	93	12	2026-04-30 03:28:16	72	\N	\N	\N	TIME	1	2	PHRF	\N
1740	95	29	2026-05-14 02:48:34	99	\N	\N	\N	TIME	2	3	PHRF	\N
1795	97	30	2026-05-28 03:27:15	126	\N	\N	\N	TIME	2	4	PHRF	\N
1856	100	30	2026-06-11 03:02:53	126	\N	\N	\N	TIME	2	4	PHRF	\N
1917	102	28	2026-06-25 02:57:48	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
1979	106	48	2026-07-02 03:36:57	99	\N	\N	\N	TIME	38	67	PHRF	\N
2038	104	52	2026-07-11 21:22:19	105	\N	\N	\N	TIME	38	67	PHRF	\N
2094	109	94	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2150	108	15	\N	93	\N	\N	\N	DNS_RC	37	65	PHRF	\N
2218	113	93	2026-08-20 02:54:34	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2273	129	116	2026-08-27 01:17:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2368	118	93	2026-09-19 21:06:07	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
467	28	6	2024-06-27 03:39:29	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
471	28	47	2024-06-27 03:54:18	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
469	28	8	2024-06-27 03:48:05	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
475	30	44	2024-07-04 02:53:18	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
479	30	47	2024-07-04 03:00:56	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
478	30	6	2024-07-04 02:58:40	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
482	30	8	2024-07-04 03:01:07	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
505	31	44	2024-07-11 03:23:05	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
507	31	6	2024-07-11 03:29:26	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
509	31	8	2024-07-11 03:31:25	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
512	31	47	2024-07-11 03:45:28	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
543	33	8	2024-07-13 23:32:41	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
501	31	50	\N	\N	\N	\N	1	\N	3	18	CruisingNonFlyingSails	\N
547	42	50	2024-07-18 03:05:43	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
552	42	44	2024-07-18 03:32:11	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
563	42	47	2024-07-18 04:02:46	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1557	86	36	2026-01-24 20:57:01	141	\N	\N	\N	TIME	2	17	PHRF	\N
1608	89	36	2026-03-28 19:50:41	141	\N	\N	\N	TIME	2	17	PHRF	\N
1686	93	18	2026-04-30 03:28:18	81	\N	\N	\N	TIME	2	3	PHRF	\N
1741	95	15	2026-05-14 02:48:39	93	\N	\N	\N	TIME	2	3	PHRF	\N
1796	97	56	2026-05-28 03:27:42	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1857	100	36	2026-06-11 03:03:16	141	\N	\N	\N	TIME	2	4	PHRF	\N
1918	102	11	2026-06-25 02:58:36	48	\N	\N	\N	TIME	37	64	PHRF	\N
1980	106	15	2026-07-02 03:40:14	93	\N	\N	\N	TIME	37	65	PHRF	\N
2039	104	29	2026-07-11 21:23:06	99	\N	\N	\N	TIME	38	67	PHRF	\N
2095	109	99	\N	\N	\N	\N	\N	RET	4	19	CruisingFlyingSails	\N
2151	109	93	\N	\N	\N	\N	\N	DNS_RC	3	18	CruisingNonFlyingSails	\N
2219	113	35	2026-08-20 02:54:53	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2274	130	112	2026-08-27 01:21:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2369	118	94	2026-09-19 21:16:52	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
589	43	44	2024-07-25 03:15:55	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
612	44	44	2024-08-01 03:01:43	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1345	73	102	2025-07-31 03:31:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
640	45	44	2024-08-08 03:29:35	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
641	45	8	2024-08-08 03:38:07	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
642	45	47	2024-08-08 03:51:02	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
643	45	25	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
652	47	47	2024-08-10 22:26:40	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
671	34	44	2024-08-15 03:33:29	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1558	86	13	2026-01-24 20:59:02	159	\N	\N	\N	TIME	2	17	PHRF	\N
1609	89	98	2026-03-28 19:52:05	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1687	93	50	2026-04-30 03:28:45	63	\N	\N	\N	TIME	1	2	PHRF	\N
1742	95	19	2026-05-14 02:53:10	120	\N	\N	\N	TIME	2	4	PHRF	\N
1797	97	90	2026-05-28 03:30:47	96	\N	\N	\N	TIME	2	3	PHRF	\N
1858	100	16	2026-06-11 03:03:36	141	\N	\N	\N	TIME	2	4	PHRF	\N
1919	102	15	2026-06-25 03:03:24	93	\N	\N	\N	TIME	37	65	PHRF	\N
1981	106	41	2026-07-02 03:42:13	93	\N	\N	\N	TIME	37	65	PHRF	\N
2040	104	36	2026-07-11 21:30:30	141	\N	\N	\N	TIME	38	68	PHRF	\N
2153	105	15	2026-08-08 21:49:47	93	\N	\N	\N	TIME	37	65	PHRF	\N
2096	109	36	\N	141	\N	\N	\N	RET	38	68	PHRF	\N
2220	113	99	2026-08-20 03:00:14	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2275	130	113	2026-08-27 01:22:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2370	118	88	2026-09-19 21:51:19	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
685	35	47	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
688	36	44	2024-08-22 02:42:06	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
696	36	55	2024-08-22 03:07:09	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
703	36	47	2024-08-22 03:27:42	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
721	37	55	2024-08-24 20:21:44	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1346	73	57	2025-07-31 03:34:25	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
758	39	57	2024-10-05 19:22:32	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
759	39	56	2024-10-05 19:24:04	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
774	40	8	2024-10-19 19:44:48	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
678	34	12	2024-08-15 03:07:42	72	\N	\N	\N	\N	1	2	PHRF	\N
679	35	26	2024-08-17 21:11:30	33	\N	\N	\N	\N	1	1	PHRF	\N
680	35	43	2024-08-17 21:35:24	93	\N	\N	\N	\N	2	3	PHRF	\N
681	35	40	2024-08-17 21:45:47	105	\N	\N	\N	\N	2	3	PHRF	\N
682	35	36	2024-08-17 21:52:21	141	\N	\N	\N	\N	2	4	PHRF	\N
1559	86	98	2026-01-24 21:10:14	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1610	89	5	2026-03-28 19:58:02	162	\N	\N	\N	TIME	2	17	PHRF	\N
1688	93	29	2026-04-30 03:34:14	99	\N	\N	\N	TIME	2	3	PHRF	\N
1743	95	5	2026-05-14 02:54:02	162	\N	\N	\N	TIME	2	4	PHRF	\N
1798	97	41	2026-05-28 03:30:53	93	\N	\N	\N	TIME	2	3	PHRF	\N
1859	100	5	2026-06-11 03:04:03	162	\N	\N	\N	TIME	2	4	PHRF	\N
1920	102	27	2026-06-25 03:04:48	72	\N	\N	\N	TIME	37	65	PHRF	\N
1982	106	18	2026-07-02 03:42:23	84	\N	\N	\N	TIME	37	65	PHRF	\N
2041	104	16	2026-07-11 21:41:07	141	\N	\N	\N	TIME	38	68	PHRF	\N
2097	110	53	2026-07-30 02:54:54	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2154	105	41	2026-08-08 21:54:41	93	\N	\N	\N	TIME	37	65	PHRF	\N
2221	113	63	2026-08-20 02:53:22	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
2276	130	115	2026-08-27 01:23:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2371	118	35	2026-09-19 21:52:04	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
818	39	36	2024-10-05 18:57:13	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
819	39	5	2024-10-05 19:00:16	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
820	39	63	2024-10-05 19:01:55	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
821	39	16	2024-10-05 19:07:22	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
825	52	8	2025-03-29 19:23:04	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
785	41	41	2024-11-02 19:25:29	93	\N	\N	\N	\N	2	3	PHRF	\N
847	53	6	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
848	53	88	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
849	53	57	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
850	53	56	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
869	54	91	2025-04-17 03:44:11	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
871	54	6	2025-04-17 03:57:44	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
872	54	8	2025-04-17 03:57:56	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
873	54	57	2025-04-17 03:59:30	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
874	54	88	2025-04-17 04:00:00	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
877	54	93	\N	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
879	55	91	2025-04-24 02:53:02	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
881	55	93	2025-04-24 03:08:54	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
882	55	57	2025-04-24 03:09:33	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
883	55	88	2025-04-24 03:14:47	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
884	55	56	2025-04-24 03:18:03	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
885	55	94	2025-04-24 03:25:08	\N	\N	\N	\N	\N	3	18	CruisingNonFlyingSails	\N
1560	86	90	2026-01-24 21:14:06	96	\N	\N	\N	TIME	2	17	PHRF	\N
1611	89	105	2026-03-28 19:59:24	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1689	93	34	2026-04-30 03:33:29	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1744	95	30	2026-05-14 02:55:24	126	\N	\N	\N	TIME	2	4	PHRF	\N
1799	97	36	2026-05-28 03:32:31	141	\N	\N	\N	TIME	2	4	PHRF	\N
1860	100	1	2026-06-11 03:04:58	150	\N	\N	\N	TIME	2	4	PHRF	\N
1921	102	63	2026-06-25 03:06:20	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
1983	106	29	2026-07-02 03:42:24	99	\N	\N	\N	TIME	38	67	PHRF	\N
2042	104	105	2026-07-11 21:50:35	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2098	110	35	2026-07-30 02:55:01	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
930	56	91	2025-05-01 02:54:38	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2155	105	52	2026-08-08 22:02:06	105	\N	\N	\N	TIME	38	67	PHRF	\N
931	56	6	2025-05-01 02:55:08	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2222	113	11	2026-08-20 02:53:35	48	\N	\N	\N	TIME	37	64	PHRF	\N
2277	130	110	2026-08-27 01:24:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2373	127	112	2026-09-24 01:01:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
933	56	94	2025-05-01 03:02:42	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
934	56	57	2025-05-01 03:06:21	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
935	56	93	2025-05-01 03:06:50	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
936	56	88	2025-05-01 03:12:44	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
937	56	56	2025-05-01 03:25:48	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
949	57	91	2025-05-08 03:23:49	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
975	58	91	\N	\N	\N	\N	\N	NSC	3	18	CruisingNonFlyingSails	\N
976	58	6	2025-05-15 03:47:08	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
978	58	57	2025-05-15 03:57:27	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
979	58	56	2025-05-15 04:01:37	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
987	59	91	2025-05-22 03:08:27	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
988	59	94	2025-05-22 03:09:48	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1561	86	16	2026-01-24 21:20:10	141	\N	\N	\N	TIME	2	17	PHRF	\N
1612	89	21	2026-03-28 20:00:04	195	\N	\N	\N	TIME	2	17	PHRF	\N
1690	93	99	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
1800	97	1	2026-05-28 03:34:25	150	\N	\N	\N	TIME	2	4	PHRF	\N
1861	100	21	2026-06-11 03:11:18	195	\N	\N	\N	TIME	2	4	PHRF	\N
1922	102	50	2026-06-25 03:07:52	63	\N	\N	\N	TIME	37	64	PHRF	\N
1745	95	34	2026-05-14 02:56:00	153	\N	\N	\N	TIME	2	4	PHRF	\N
1984	106	19	2026-07-02 03:42:56	120	\N	\N	\N	TIME	38	67	PHRF	\N
2043	104	93	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2099	110	6	2026-07-30 02:58:23	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2156	105	29	2026-08-08 22:03:08	99	\N	\N	\N	TIME	38	67	PHRF	\N
2223	113	12	2026-08-20 02:54:08	72	\N	\N	\N	TIME	37	65	PHRF	\N
2278	130	114	2026-08-27 01:25:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2374	127	115	2026-09-24 01:02:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
997	59	6	2025-05-22 03:20:08	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
998	59	93	2025-05-22 03:21:23	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1001	59	57	2025-05-22 03:23:45	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1003	59	88	2025-05-22 03:27:54	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1007	59	56	2025-05-22 03:32:49	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1022	60	91	2025-05-29 03:20:50	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1023	60	94	2025-05-29 03:23:23	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1024	60	93	2025-05-29 03:23:37	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1025	60	6	2025-05-29 03:25:39	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1027	60	57	2025-05-29 03:38:43	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1034	60	88	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1055	61	96	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1061	61	91	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1062	61	57	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1064	61	97	2025-06-05 04:07:19	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
991	59	29	2025-05-22 03:15:00	99	\N	\N	\N	TIME	2	3	PHRF	\N
993	59	48	\N	99	\N	\N	\N	NSC	2	3	PHRF	\N
1068	61	56	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1088	62	93	2025-06-12 03:54:20	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1562	86	73	2026-01-24 21:23:04	75	\N	\N	\N	TIME	2	17	PHRF	\N
1613	89	16	2026-03-28 20:01:07	141	\N	\N	\N	TIME	2	17	PHRF	\N
1691	93	93	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1746	95	73	2026-05-14 02:56:02	75	\N	\N	\N	TIME	2	3	PHRF	\N
1801	97	16	2026-05-28 03:34:30	141	\N	\N	\N	TIME	2	4	PHRF	\N
1862	100	27	2026-06-11 03:05:17	72	\N	\N	\N	TIME	1	2	PHRF	\N
1923	102	22	2026-06-25 03:08:14	69	\N	\N	\N	TIME	37	64	PHRF	\N
1985	106	6	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2045	108	19	2026-07-16 02:56:56	120	\N	\N	\N	TIME	38	67	PHRF	\N
2100	110	11	2026-07-30 03:03:24	48	\N	\N	\N	TIME	37	64	PHRF	\N
2157	105	11	2026-08-08 22:05:07	48	\N	\N	\N	TIME	37	64	PHRF	\N
2224	113	27	2026-08-20 02:54:59	72	\N	\N	\N	TIME	37	65	PHRF	\N
2279	130	116	2026-08-27 01:26:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2375	127	111	2026-09-24 01:03:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1090	62	6	2025-06-12 03:56:33	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1092	62	57	2025-06-12 03:58:28	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1095	62	91	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1340	73	88	2025-07-31 03:26:52	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1114	64	93	2025-06-19 03:24:35	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1120	64	94	2025-06-19 03:35:43	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1121	64	57	2025-06-19 03:36:26	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1123	64	47	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1096	64	48	2025-06-19 02:50:16	99	\N	\N	\N	TIME	2	3	PHRF	\N
1097	64	19	2025-06-19 02:51:05	120	\N	\N	\N	TIME	2	4	PHRF	\N
1098	64	29	2025-06-19 02:52:06	99	\N	\N	\N	TIME	2	3	PHRF	\N
1099	64	15	2025-06-19 02:52:40	93	\N	\N	\N	TIME	2	3	PHRF	\N
1371	74	94	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1373	74	93	\N	\N	4	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1093	62	94	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1094	62	56	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1100	64	43	2025-06-19 02:52:54	93	\N	\N	\N	TIME	2	3	PHRF	\N
1101	64	18	2025-06-19 02:53:26	81	\N	\N	\N	TIME	2	3	PHRF	\N
1350	73	35	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1152	66	91	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1187	67	97	2025-06-29 19:56:44	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1190	67	94	2025-06-29 20:00:31	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1191	67	56	2025-06-29 20:01:32	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1563	86	5	2026-01-24 21:54:20	162	\N	\N	\N	TIME	2	17	PHRF	\N
1614	89	1	2026-03-28 20:07:39	150	\N	\N	\N	TIME	2	17	PHRF	\N
1692	93	94	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1747	95	41	2026-05-14 02:57:34	93	\N	\N	\N	TIME	2	3	PHRF	\N
1802	97	106	2026-05-28 03:36:09	93	\N	\N	\N	TIME	2	3	PHRF	\N
1863	100	28	2026-06-11 03:06:42	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1924	102	12	2026-06-25 03:09:34	72	\N	\N	\N	TIME	37	65	PHRF	\N
1986	106	94	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2046	108	29	2026-07-16 02:58:19	99	\N	\N	\N	TIME	38	67	PHRF	\N
2101	110	28	2026-07-30 03:05:18	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
2158	105	18	2026-08-08 22:05:12	84	\N	\N	\N	TIME	37	65	PHRF	\N
2225	113	50	2026-08-20 02:57:06	63	\N	\N	\N	TIME	37	64	PHRF	\N
2280	130	111	2026-08-27 01:27:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2376	127	113	2026-09-24 01:04:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1211	68	6	2025-07-03 03:39:20	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1214	68	93	2025-07-03 03:44:16	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1216	68	94	2025-07-03 03:44:49	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1217	68	88	2025-07-03 03:56:53	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1218	68	57	2025-07-03 03:59:17	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1221	69	91	2025-07-10 03:17:52	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1222	69	97	2025-07-10 03:20:32	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1224	69	94	2025-07-10 03:28:10	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1225	69	93	2025-07-10 03:30:18	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1230	69	57	2025-07-10 03:33:05	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1239	69	56	2025-07-10 03:53:17	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1259	70	97	2025-07-12 21:56:57	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1261	70	47	2025-07-12 22:00:01	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1262	70	88	2025-07-12 22:07:11	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1263	70	56	2025-07-12 22:19:28	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1266	71	50	2025-07-17 02:47:28	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1267	71	91	2025-07-17 02:48:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1270	71	97	2025-07-17 02:57:25	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1271	71	101	2025-07-17 02:59:25	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1272	71	93	2025-07-17 03:05:47	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1273	71	88	2025-07-17 03:12:05	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1564	86	21	2026-01-24 22:31:55	195	\N	\N	\N	TIME	2	17	PHRF	\N
1615	89	56	2026-03-28 20:08:40	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1693	93	30	\N	126	\N	\N	\N	RET	2	4	PHRF	\N
1748	95	1	2026-05-14 02:58:23	150	\N	\N	\N	TIME	2	4	PHRF	\N
1803	97	5	2026-05-28 03:36:44	162	\N	\N	\N	TIME	2	4	PHRF	\N
1864	100	50	2026-06-11 03:10:59	63	\N	\N	\N	TIME	1	2	PHRF	\N
1925	102	48	2026-06-25 03:11:13	99	\N	\N	\N	TIME	38	67	PHRF	\N
1987	106	34	\N	153	\N	\N	\N	RET	38	68	PHRF	\N
2047	108	30	2026-07-16 02:58:37	126	\N	\N	\N	TIME	38	67	PHRF	\N
2102	110	27	2026-07-30 03:07:57	72	\N	\N	\N	TIME	37	65	PHRF	\N
2159	105	36	2026-08-08 22:42:43	141	\N	\N	\N	TIME	38	68	PHRF	\N
2226	113	15	2026-08-20 02:58:02	93	\N	\N	\N	TIME	37	65	PHRF	\N
2281	131	112	2026-08-27 01:31:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2377	127	110	2026-09-24 01:05:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1316	72	97	2025-07-24 03:13:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1321	72	93	2025-07-24 03:28:17	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1330	73	97	2025-07-31 03:10:06	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1352	73	56	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
752	46	8	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1037	60	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1065	61	6	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1351	73	93	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
209	21	8	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
211	21	6	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
210	21	37	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
337	24	44	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
595	43	47	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1565	86	99	2026-01-24 22:36:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1616	89	99	2026-03-28 20:10:54	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1694	93	36	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1749	95	36	2026-05-14 03:00:21	141	\N	\N	\N	TIME	2	4	PHRF	\N
1865	100	11	2026-06-11 03:13:22	48	\N	\N	\N	TIME	1	1	PHRF	\N
1926	102	18	2026-06-25 03:11:14	81	\N	\N	\N	TIME	37	65	PHRF	\N
1804	97	34	2026-05-28 03:36:54	153	\N	\N	\N	TIME	2	4	PHRF	\N
1988	106	31	\N	93	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2048	108	48	2026-07-16 02:58:54	99	\N	\N	\N	TIME	38	67	PHRF	\N
2103	110	12	2026-07-30 03:09:10	72	\N	\N	\N	TIME	37	65	PHRF	\N
2160	105	16	2026-08-08 22:57:09	141	\N	\N	\N	TIME	38	68	PHRF	\N
2227	113	22	2026-08-20 03:00:45	69	\N	\N	\N	TIME	37	64	PHRF	\N
2282	131	113	2026-08-27 01:32:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2382	134	112	2026-09-24 01:11:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1415	77	104	2025-08-16 22:30:26	93	\N	\N	\N	TIME	9	31	PHRF	\N
1414	77	100	2025-08-16 22:23:04	54	\N	\N	\N	TIME	9	31	PHRF	\N
1383	75	47	2025-08-09 22:00:37	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1384	75	88	2025-08-09 22:09:16	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1374	75	63	2025-08-09 21:03:14	18	\N	\N	\N	TIME	1	1	PHRF	\N
1376	75	15	2025-08-09 21:08:51	93	\N	\N	\N	TIME	2	3	PHRF	\N
1377	75	29	2025-08-09 21:09:41	99	\N	\N	\N	TIME	2	3	PHRF	\N
1378	75	36	2025-08-09 21:20:26	141	\N	\N	\N	TIME	2	4	PHRF	\N
1405	76	6	2025-08-14 03:28:20	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1406	76	97	2025-08-14 03:29:24	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1407	76	103	2025-08-14 03:30:43	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1408	76	102	2025-08-14 03:30:58	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1413	76	94	2025-08-14 03:35:25	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1379	75	90	2025-08-09 21:27:54	96	\N	\N	\N	TIME	2	3	PHRF	\N
1375	75	18	2025-08-09 21:08:21	81	\N	\N	\N	TIME	2	3	PHRF	\N
1380	75	16	2025-08-09 21:31:49	141	\N	\N	\N	TIME	2	4	PHRF	\N
1385	76	11	2025-08-14 02:47:46	48	\N	\N	\N	TIME	1	1	PHRF	\N
1161	66	94	\N	\N	5	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1159	66	57	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1441	78	102	2025-08-21 03:07:35	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1432	78	6	2025-08-21 03:01:11	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1436	78	97	2025-08-21 03:02:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1449	78	93	2025-08-21 03:14:52	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1451	78	94	2025-08-21 03:21:30	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1454	78	88	2025-08-21 03:29:41	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1566	87	11	2026-01-31 19:57:52	48	\N	\N	\N	TIME	1	16	PHRF	\N
1617	89	106	2026-03-28 19:47:15	93	\N	\N	\N	TIME	2	17	PHRF	\N
1695	93	5	\N	162	\N	\N	\N	DNF	2	4	PHRF	\N
1750	95	106	2026-05-14 03:15:31	93	\N	\N	\N	TIME	2	3	PHRF	\N
1805	97	93	2026-05-28 03:38:33	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1866	100	12	2026-06-11 03:15:22	72	\N	\N	\N	TIME	1	2	PHRF	\N
1927	102	29	2026-06-25 03:13:40	99	\N	\N	\N	TIME	38	67	PHRF	\N
1989	106	35	\N	141	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2049	108	36	2026-07-16 03:02:04	141	\N	\N	\N	TIME	38	68	PHRF	\N
2104	110	22	2026-07-30 03:11:12	69	\N	\N	\N	TIME	37	64	PHRF	\N
2161	105	99	\N	\N	\N	\N	\N	RET	4	19	CruisingFlyingSails	\N
2228	113	18	2026-08-20 03:01:04	84	\N	\N	\N	TIME	37	65	PHRF	\N
2283	131	111	2026-08-27 01:33:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2383	134	115	2026-09-24 01:12:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1509	83	15	2025-10-04 19:44:46	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1510	83	90	2025-10-04 19:51:41	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1511	83	100	2025-10-04 19:54:56	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1512	83	36	2025-10-04 19:56:57	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1514	83	16	2025-10-04 20:18:22	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1515	83	98	2025-10-04 20:49:40	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1516	83	99	2025-10-04 21:05:58	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1517	83	21	2025-10-04 20:15:05	\N	\N	\N	\N	TIME	36	63	CruisingNonFlyingSails	\N
1518	83	102	2025-10-04 21:00:17	\N	\N	\N	\N	TIME	36	63	CruisingNonFlyingSails	\N
1465	79	93	2025-08-23 19:43:25	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1488	80	93	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1505	82	97	2025-09-20 19:23:05	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1508	82	93	2025-09-20 20:05:31	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1455	78	15	\N	93	\N	\N	\N	DNS_RC	2	3	PHRF	\N
1519	84	63	2025-10-18 19:00:10	18	\N	\N	\N	TIME	1	1	PHRF	\N
1548	61	88	\N	\N	\N	\N	\N	DNS_RC	3	18	CruisingNonFlyingSails	\N
1567	87	28	2026-01-31 20:02:33	42	\N	\N	\N	TIME	1	16	ORC_PHRF	04560003L6C
1618	90	63	2026-04-09 03:01:14	18	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KI4
1696	93	106	\N	93	\N	\N	\N	DNF	2	3	PHRF	\N
1751	95	35	2026-05-14 03:17:48	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1806	97	39	2026-05-28 03:40:19	144	\N	\N	\N	TIME	2	4	PHRF	\N
1867	100	22	2026-06-11 03:29:18	69	\N	\N	\N	TIME	1	2	PHRF	\N
1928	102	73	2026-06-25 03:13:39	75	\N	\N	\N	TIME	37	65	PHRF	\N
1990	106	50	\N	63	\N	\N	\N	RET	37	64	PHRF	\N
2050	108	34	2026-07-16 03:02:50	153	\N	\N	\N	TIME	38	68	PHRF	\N
2105	110	50	2026-07-30 03:11:46	63	\N	\N	\N	TIME	37	64	PHRF	\N
2162	105	93	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2229	113	29	2026-08-20 03:03:52	99	\N	\N	\N	TIME	38	67	PHRF	\N
2284	131	114	2026-08-27 01:34:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2384	134	113	2026-09-24 01:13:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1411	76	57	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1568	87	19	2026-01-31 20:22:38	120	\N	\N	\N	TIME	2	17	PHRF	\N
1619	90	18	2026-04-09 02:44:43	81	\N	\N	\N	TIME	2	3	PHRF	\N
1752	95	94	2026-05-14 03:22:22	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1807	97	21	2026-05-28 03:46:48	195	\N	\N	\N	TIME	2	4	PHRF	\N
1697	93	41	2026-04-30 03:34:58	93	\N	\N	\N	TIME	2	3	PHRF	\N
1929	102	41	2026-06-25 03:14:12	93	\N	\N	\N	TIME	37	65	PHRF	\N
1991	106	36	\N	141	\N	\N	\N	RET	38	68	PHRF	\N
2051	108	1	2026-07-16 03:05:36	150	\N	\N	\N	TIME	38	68	PHRF	\N
2106	110	18	2026-07-30 03:14:16	84	\N	\N	\N	TIME	37	65	PHRF	\N
2230	113	30	2026-08-20 03:06:13	126	\N	\N	\N	TIME	38	67	PHRF	\N
2285	131	115	2026-08-27 01:35:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2385	134	111	2026-09-24 01:14:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1569	87	36	2026-01-31 20:18:36	141	\N	\N	\N	TIME	2	17	PHRF	\N
1620	90	15	2026-04-09 02:44:47	93	\N	\N	\N	TIME	2	3	PHRF	\N
1698	93	63	\N	18	\N	\N	\N	DNF	1	1	ORC_PHRF	04560004KI4
1753	95	99	2026-05-14 03:22:41	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1808	97	35	2026-05-28 03:51:27	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1930	102	19	2026-06-25 03:15:24	120	\N	\N	\N	TIME	38	67	PHRF	\N
1992	106	11	2026-07-02 03:52:51	48	\N	\N	\N	TIME	37	64	PHRF	\N
2052	108	16	2026-07-16 03:06:21	141	\N	\N	\N	TIME	38	68	PHRF	\N
2107	110	99	2026-07-30 03:14:42	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2164	99	5	\N	162	\N	\N	\N	DNS_RC	2	4	PHRF	\N
2231	113	19	2026-08-20 03:08:42	120	\N	\N	\N	TIME	38	67	PHRF	\N
2286	131	116	2026-08-27 01:36:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2386	134	110	2026-09-24 01:15:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1570	87	12	2026-01-31 20:26:22	72	\N	\N	\N	TIME	1	16	PHRF	\N
1621	90	29	2026-04-09 02:46:17	99	\N	\N	\N	TIME	2	3	PHRF	\N
1699	93	1	\N	150	\N	\N	\N	DNF	2	4	PHRF	\N
1754	95	6	2026-05-14 03:26:58	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1809	97	88	2026-05-28 03:56:40	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1931	102	36	2026-06-25 03:17:04	141	\N	\N	\N	TIME	38	68	PHRF	\N
1994	106	30	2026-07-02 03:50:50	126	\N	\N	\N	TIME	38	67	PHRF	\N
2053	108	53	2026-07-16 03:06:37	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2108	110	41	2026-07-30 03:15:56	93	\N	\N	\N	TIME	37	65	PHRF	\N
2165	99	12	2026-06-04 03:44:00	72	\N	\N	\N	TIME	1	2	PHRF	\N
2232	113	41	2026-08-20 03:09:47	93	\N	\N	\N	TIME	37	65	PHRF	\N
2287	131	110	2026-08-27 01:37:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2387	135	112	2026-09-24 01:21:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1571	87	90	2026-01-31 20:27:13	96	\N	\N	\N	TIME	2	17	PHRF	\N
1622	90	30	2026-04-09 02:51:44	126	\N	\N	\N	TIME	2	4	PHRF	\N
1700	93	27	\N	72	\N	\N	\N	DNF	1	2	PHRF	\N
1755	95	93	2026-05-14 03:49:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1814	98	36	2026-05-30 18:27:20	141	\N	\N	\N	TIME	2	17	PHRF	\N
1932	102	34	2026-06-25 03:16:40	153	\N	\N	\N	TIME	38	68	PHRF	\N
1995	106	106	\N	93	\N	\N	\N	DNF	37	65	PHRF	\N
2054	108	5	2026-07-16 03:07:01	162	\N	\N	\N	TIME	38	68	PHRF	\N
2109	110	19	2026-07-30 03:16:30	120	\N	\N	\N	TIME	38	67	PHRF	\N
2166	99	27	2026-06-04 03:34:00	72	\N	\N	\N	TIME	1	2	PHRF	\N
2233	113	73	2026-08-20 03:11:03	75	\N	\N	\N	TIME	37	65	PHRF	\N
2316	125	110	2026-09-10 01:01:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2388	135	115	2026-09-24 01:22:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1572	87	13	2026-01-31 20:29:56	159	\N	\N	\N	TIME	2	17	PHRF	\N
1623	90	41	2026-04-09 02:52:16	93	\N	\N	\N	TIME	2	3	PHRF	\N
1701	93	15	\N	93	\N	\N	\N	DNF	2	3	PHRF	\N
1756	96	28	2026-05-21 02:44:55	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1815	98	98	2026-05-30 18:31:27	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1875	101	28	2026-06-18 02:52:40	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
1933	102	5	2026-06-25 03:18:20	162	\N	\N	\N	TIME	38	68	PHRF	\N
1997	106	105	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
2055	108	21	2026-07-16 03:11:28	195	\N	\N	\N	TIME	38	68	PHRF	\N
2110	110	30	2026-07-30 03:18:40	126	\N	\N	\N	TIME	38	67	PHRF	\N
2167	112	19	2026-08-13 02:42:47	120	\N	\N	\N	TIME	38	67	PHRF	\N
2234	113	56	2026-08-20 03:04:24	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2317	125	112	2026-09-10 01:02:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2389	135	111	2026-09-24 01:23:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1573	87	16	2026-01-31 20:51:26	141	\N	\N	\N	TIME	2	17	PHRF	\N
1624	90	106	2026-04-09 02:52:38	93	\N	\N	\N	TIME	2	3	PHRF	\N
1702	93	39	\N	144	\N	\N	\N	DNF	2	4	PHRF	\N
1757	96	63	2026-05-21 02:45:38	18	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KI4
1816	98	16	2026-05-30 18:32:34	141	\N	\N	\N	TIME	2	17	PHRF	\N
1876	101	63	2026-06-18 02:54:37	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
1934	102	30	2026-06-25 03:18:44	126	\N	\N	\N	TIME	38	67	PHRF	\N
1998	106	56	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2057	108	6	2026-07-16 03:20:47	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2111	110	29	2026-07-30 03:21:22	99	\N	\N	\N	TIME	38	67	PHRF	\N
2168	112	36	2026-08-13 02:43:10	141	\N	\N	\N	TIME	38	68	PHRF	\N
2235	113	88	2026-08-20 03:08:57	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2318	125	111	2026-09-10 01:03:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2390	135	113	2026-09-24 01:24:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1574	87	15	2026-01-31 21:05:14	93	\N	\N	\N	TIME	2	17	PHRF	\N
1625	90	36	2026-04-09 02:54:07	141	\N	\N	\N	TIME	2	4	PHRF	\N
1703	93	16	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1758	96	27	2026-05-21 02:48:01	72	\N	\N	\N	TIME	1	2	PHRF	\N
1817	98	5	2026-05-30 18:35:01	162	\N	\N	\N	TIME	2	17	PHRF	\N
1877	101	11	2026-06-18 02:55:50	48	\N	\N	\N	TIME	37	64	PHRF	\N
1935	102	90	2026-06-25 03:20:21	96	\N	\N	\N	TIME	38	67	PHRF	\N
1999	106	88	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2058	108	11	2026-07-16 03:26:02	48	\N	\N	\N	TIME	37	64	PHRF	\N
2112	110	56	2026-07-30 03:21:34	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2169	112	29	2026-08-13 02:45:53	99	\N	\N	\N	TIME	38	67	PHRF	\N
2236	113	6	2026-08-20 03:15:57	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2319	125	114	2026-09-10 01:04:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2391	135	110	2026-09-24 01:25:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1575	87	18	2026-01-31 21:47:56	81	\N	\N	\N	TIME	2	17	PHRF	\N
1626	90	5	2026-04-09 02:54:41	162	\N	\N	\N	TIME	2	4	PHRF	\N
1704	93	90	\N	96	\N	\N	\N	DNF	2	3	PHRF	\N
1759	96	12	2026-05-21 02:51:06	72	\N	\N	\N	TIME	1	2	PHRF	\N
1818	92	31	\N	93	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1878	101	27	2026-06-18 02:58:43	72	\N	\N	\N	TIME	37	65	PHRF	\N
1936	102	1	2026-06-25 03:20:30	150	\N	\N	\N	TIME	38	68	PHRF	\N
2001	106	93	\N	\N	4	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2059	108	63	2026-07-16 03:33:56	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
2113	110	34	2026-07-30 03:23:06	153	\N	\N	\N	TIME	38	68	PHRF	\N
2170	112	30	2026-08-13 02:46:51	126	\N	\N	\N	TIME	38	67	PHRF	\N
2237	113	34	2026-08-20 03:17:58	153	\N	\N	\N	TIME	38	68	PHRF	\N
2320	125	113	2026-09-10 01:05:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2393	128	115	2026-10-01 02:01:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1418	77	1	2025-08-16 23:05:42	150	\N	\N	\N	TIME	7	28	PHRF	\N
1416	77	15	2025-08-16 22:40:24	93	\N	\N	\N	TIME	7	28	PHRF	\N
1419	77	36	2025-08-16 23:09:32	141	\N	\N	\N	TIME	7	28	PHRF	\N
1417	77	40	2025-08-16 22:59:24	105	\N	\N	\N	TIME	7	28	PHRF	\N
1462	79	15	2025-08-23 19:40:36	93	\N	\N	\N	TIME	2	3	PHRF	\N
1576	87	22	2026-01-31 22:05:29	69	\N	\N	\N	TIME	1	16	PHRF	\N
1627	90	39	2026-04-09 02:55:52	144	\N	\N	\N	TIME	2	4	PHRF	\N
1705	93	88	\N	\N	4	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1760	96	29	2026-05-21 02:55:39	99	\N	\N	\N	TIME	2	3	PHRF	\N
1819	99	105	2026-06-04 03:11:33	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1879	101	15	2026-06-18 03:01:39	93	\N	\N	\N	TIME	37	65	PHRF	\N
1937	102	16	2026-06-25 03:25:32	141	\N	\N	\N	TIME	38	68	PHRF	\N
2002	106	1	\N	150	\N	\N	\N	DNF	38	68	PHRF	\N
2060	108	50	\N	63	\N	\N	\N	RET	37	64	PHRF	\N
2114	110	88	2026-07-30 03:24:49	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2171	112	5	2026-08-13 02:47:36	162	\N	\N	\N	TIME	38	68	PHRF	\N
2238	113	36	2026-08-20 03:21:39	141	\N	\N	\N	TIME	38	68	PHRF	\N
2321	132	112	2026-09-10 01:11:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2394	128	112	2026-10-01 01:02:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1534	84	5	2025-10-18 20:28:15	162	\N	\N	\N	TIME	2	4	PHRF	\N
1577	87	98	2026-01-31 22:15:34	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1628	90	1	2026-04-09 02:56:55	150	\N	\N	\N	TIME	2	4	PHRF	\N
1706	94	50	2026-05-07 03:04:11	63	\N	\N	\N	TIME	1	2	PHRF	\N
1761	96	50	2026-05-21 02:55:40	63	\N	\N	\N	TIME	1	2	PHRF	\N
1820	99	98	2026-06-04 03:16:19	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1880	101	29	2026-06-18 03:03:24	99	\N	\N	\N	TIME	38	67	PHRF	\N
1938	102	39	2026-06-25 03:28:34	144	\N	\N	\N	TIME	38	68	PHRF	\N
2003	106	5	\N	162	\N	\N	\N	DNF	38	68	PHRF	\N
2061	108	28	2026-07-16 03:36:59	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
2115	110	36	2026-07-30 03:25:59	141	\N	\N	\N	TIME	38	68	PHRF	\N
2172	112	39	2026-08-13 02:49:17	144	\N	\N	\N	TIME	38	68	PHRF	\N
2239	113	39	2026-08-20 03:24:44	144	\N	\N	\N	TIME	38	68	PHRF	\N
2322	132	110	2026-09-10 01:12:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2395	128	110	2026-10-01 01:03:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1578	87	5	2026-01-31 22:15:42	162	\N	\N	\N	TIME	2	17	PHRF	\N
1629	90	34	2026-04-09 02:57:14	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1707	94	28	2026-05-07 03:06:41	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1762	96	22	2026-05-21 02:56:58	69	\N	\N	\N	TIME	1	2	PHRF	\N
1821	99	31	2026-06-04 03:16:57	93	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1881	101	18	2026-06-18 03:03:24	81	\N	\N	\N	TIME	37	65	PHRF	\N
1939	102	21	2026-06-25 03:44:20	195	\N	\N	\N	TIME	38	68	PHRF	\N
2004	106	39	\N	144	\N	\N	\N	DNF	38	68	PHRF	\N
2062	108	27	2026-07-16 03:37:03	72	\N	\N	\N	TIME	37	65	PHRF	\N
2116	110	5	2026-07-30 03:27:13	162	\N	\N	\N	TIME	38	68	PHRF	\N
2173	112	34	2026-08-13 02:49:50	153	\N	\N	\N	TIME	38	68	PHRF	\N
2241	113	5	2026-08-20 03:23:10	162	\N	\N	\N	TIME	38	68	PHRF	\N
2323	132	114	2026-09-10 01:13:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2396	128	117	2026-10-01 01:04:00	200	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1420	77	98	2025-08-16 23:14:50	\N	\N	\N	\N	TIME	5	29	CruisingFlyingSails	\N
1421	77	99	2025-08-17 00:35:31	\N	\N	\N	\N	TIME	5	29	CruisingFlyingSails	\N
1535	84	98	2025-10-18 20:41:08	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
619	44	47	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
675	34	47	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
674	34	55	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
875	54	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
954	57	94	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
955	57	57	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
956	57	56	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1066	61	93	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1067	61	94	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1122	64	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1132	65	47	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1133	65	56	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1308	72	28	2025-07-24 03:05:41	42	\N	\N	\N	TIME	1	1	PHRF	\N
1309	72	22	2025-07-24 03:06:13	69	\N	\N	\N	TIME	1	2	PHRF	\N
1310	72	18	2025-07-24 03:06:46	81	\N	\N	\N	TIME	2	3	PHRF	\N
1342	73	100	2025-07-31 03:29:02	54	\N	\N	\N	TIME	1	1	PHRF	\N
1314	72	30	2025-07-24 03:13:20	126	\N	\N	\N	TIME	2	4	PHRF	\N
1315	72	17	2025-07-24 03:05:23	135	\N	\N	\N	TIME	2	4	PHRF	\N
1317	72	36	2025-07-24 03:13:53	141	\N	\N	\N	TIME	2	4	PHRF	\N
1319	72	100	2025-07-24 03:15:20	54	\N	\N	\N	TIME	1	1	PHRF	\N
1320	72	16	2025-07-24 03:18:19	141	\N	\N	\N	TIME	2	4	PHRF	\N
1313	72	35	2025-07-24 03:13:19	141	\N	\N	\N	TIME	2	4	PHRF	\N
1331	73	27	2025-07-31 03:10:08	72	\N	\N	\N	TIME	1	2	PHRF	\N
1328	73	11	2025-07-31 03:08:20	48	\N	\N	\N	TIME	1	1	PHRF	\N
1329	73	28	2025-07-31 03:08:34	42	\N	\N	\N	TIME	1	1	PHRF	\N
1360	74	36	2025-08-07 03:07:05	141	\N	\N	\N	TIME	2	4	PHRF	\N
1366	74	17	2025-08-07 03:15:42	135	\N	\N	\N	TIME	2	4	PHRF	\N
1311	72	34	2025-07-24 03:08:41	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1312	72	2	2025-07-24 03:13:18	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1318	72	53	2025-07-24 03:14:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
683	35	56	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
734	38	3	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
735	38	2	\N	\N	2	\N	\N	HOC	4	19	CruisingFlyingSails	\N
751	46	3	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
800	48	56	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
54	2	1	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
55	2	3	\N	\N	2	\N	\N	HOC	4	19	CruisingFlyingSails	\N
336	24	34	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
338	24	3	\N	\N	3	\N	\N	HOC	4	19	CruisingFlyingSails	\N
339	24	1	\N	\N	4	\N	\N	HOC	4	19	CruisingFlyingSails	\N
673	34	2	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
775	40	56	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
814	50	56	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
1157	66	6	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1158	66	93	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1131	65	99	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
1325	73	34	2025-07-31 02:58:19	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1160	66	56	\N	\N	4	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1162	66	88	\N	\N	6	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1274	71	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1322	72	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1323	72	88	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1324	72	57	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1370	74	57	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1579	87	99	2026-01-31 22:58:38	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
39	1	16	2024-01-20 22:23:55	141	\N	\N	\N	\N	2	17	PHRF	\N
1630	90	21	2026-04-09 02:57:43	195	\N	\N	\N	TIME	2	4	PHRF	\N
1708	94	11	2026-05-07 03:07:19	48	\N	\N	\N	TIME	1	1	PHRF	\N
1763	96	18	2026-05-21 02:57:19	81	\N	\N	\N	TIME	2	3	PHRF	\N
1822	99	99	2026-06-04 03:23:03	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1882	101	12	2026-06-18 03:04:47	72	\N	\N	\N	TIME	37	65	PHRF	\N
2005	107	31	2026-07-09 02:58:16	93	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2063	108	22	2026-07-16 03:40:38	69	\N	\N	\N	TIME	37	64	PHRF	\N
2117	110	16	2026-07-30 03:28:14	141	\N	\N	\N	TIME	38	68	PHRF	\N
2174	112	1	2026-08-13 02:53:11	150	\N	\N	\N	TIME	38	68	PHRF	\N
2242	113	16	2026-08-20 03:31:08	141	\N	\N	\N	TIME	38	68	PHRF	\N
2324	132	111	2026-09-10 01:14:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2397	128	111	2026-10-01 01:05:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1372	74	88	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1412	76	93	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
189	21	26	2024-05-02 02:42:27	33	\N	\N	\N	\N	1	1	PHRF	\N
46	2	14	2024-02-03 21:27:07	132	\N	\N	\N	\N	2	17	PHRF	\N
47	2	13	2024-02-03 21:48:03	159	\N	\N	\N	\N	2	17	PHRF	\N
48	2	19	2024-02-03 22:57:38	120	\N	\N	\N	\N	2	17	PHRF	\N
49	2	17	\N	129	\N	\N	\N	\N	2	17	PHRF	\N
50	2	21	\N	195	\N	\N	\N	\N	2	17	PHRF	\N
51	2	15	\N	93	\N	\N	\N	\N	2	17	PHRF	\N
52	2	18	\N	86	\N	\N	\N	\N	2	17	PHRF	\N
191	21	27	2024-05-02 02:44:11	72	\N	\N	\N	\N	1	2	PHRF	\N
1343	73	90	2025-07-31 03:30:00	96	\N	\N	\N	TIME	2	3	PHRF	\N
57	3	11	2024-02-10 20:53:23	48	\N	\N	\N	\N	1	16	PHRF	\N
58	3	12	2024-02-10 22:51:22	72	\N	\N	\N	\N	1	16	PHRF	\N
59	3	22	2024-02-10 23:05:39	69	\N	\N	\N	\N	1	16	PHRF	\N
60	3	20	\N	72	\N	\N	\N	\N	1	16	PHRF	\N
61	3	15	2024-02-10 20:52:38	93	\N	\N	\N	\N	2	17	PHRF	\N
62	3	14	2024-02-11 00:20:41	132	\N	\N	\N	\N	2	17	PHRF	\N
63	3	13	\N	159	\N	\N	\N	\N	2	17	PHRF	\N
64	3	17	\N	129	\N	\N	\N	\N	2	17	PHRF	\N
65	3	23	\N	144	\N	\N	\N	\N	2	17	PHRF	\N
66	3	18	\N	86	\N	\N	\N	\N	2	17	PHRF	\N
67	3	21	\N	195	\N	\N	\N	\N	2	17	PHRF	\N
68	3	5	\N	162	\N	\N	\N	\N	2	17	PHRF	\N
193	21	22	2024-05-02 02:45:43	69	\N	\N	\N	\N	1	2	PHRF	\N
195	21	20	2024-05-02 02:51:05	72	\N	\N	\N	\N	1	2	PHRF	\N
88	18	29	2024-04-11 03:07:01	99	\N	\N	\N	\N	2	3	PHRF	\N
89	18	30	2024-04-11 03:10:05	126	\N	\N	\N	\N	2	4	PHRF	\N
90	18	31	2024-04-11 03:11:56	93	\N	\N	\N	\N	2	3	PHRF	\N
91	18	18	2024-04-11 03:12:01	86	\N	\N	\N	\N	2	3	PHRF	\N
197	21	15	2024-05-02 02:52:44	93	\N	\N	\N	\N	2	3	PHRF	\N
199	21	19	2024-05-02 02:54:31	120	\N	\N	\N	\N	2	4	PHRF	\N
95	18	16	2024-04-11 03:14:32	141	\N	\N	\N	\N	2	4	PHRF	\N
97	18	35	2024-04-11 03:20:37	141	\N	\N	\N	\N	2	4	PHRF	\N
98	18	36	2024-04-11 03:20:50	141	\N	\N	\N	\N	2	4	PHRF	\N
201	21	31	2024-05-02 02:57:50	93	\N	\N	\N	\N	2	3	PHRF	\N
100	18	5	2024-04-11 03:22:08	162	\N	\N	\N	\N	2	4	PHRF	\N
106	18	38	2024-04-11 03:11:57	168	\N	\N	\N	\N	2	4	PHRF	\N
107	19	26	2024-04-18 02:48:23	33	\N	\N	\N	\N	1	1	PHRF	\N
34	1	11	2024-01-20 22:05:29	48	\N	\N	\N	\N	1	16	PHRF	\N
35	1	12	2024-01-20 22:15:44	72	\N	\N	\N	\N	1	16	PHRF	\N
36	1	13	2024-01-20 22:25:04	159	\N	\N	\N	\N	2	17	PHRF	\N
38	1	15	2024-01-20 22:06:45	93	\N	\N	\N	\N	2	17	PHRF	\N
1631	90	35	2026-04-09 03:02:04	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1709	94	27	2026-05-07 03:07:53	72	\N	\N	\N	TIME	1	2	PHRF	\N
1764	96	15	2026-05-21 02:57:40	93	\N	\N	\N	TIME	2	3	PHRF	\N
1823	99	88	2026-06-04 03:27:27	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1883	101	22	2026-06-18 03:05:12	69	\N	\N	\N	TIME	37	64	PHRF	\N
2006	107	53	2026-07-09 03:00:10	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2064	108	56	2026-07-16 03:40:47	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2118	110	39	2026-07-30 03:29:23	144	\N	\N	\N	TIME	38	68	PHRF	\N
2175	112	16	2026-08-13 02:53:32	141	\N	\N	\N	TIME	38	68	PHRF	\N
2243	113	21	\N	195	\N	\N	\N	DNF	38	68	PHRF	\N
2325	132	113	2026-09-10 01:15:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2398	128	113	2026-10-01 01:06:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
108	19	11	2024-04-18 02:40:20	48	\N	\N	\N	\N	1	1	PHRF	\N
41	1	18	2024-01-20 22:12:53	86	\N	\N	\N	\N	2	17	PHRF	\N
37	1	14	2024-01-20 22:20:13	132	\N	\N	\N	\N	2	17	PHRF	\N
40	1	17	2024-01-20 22:22:46	129	\N	\N	\N	\N	2	17	PHRF	\N
109	19	27	2024-04-18 02:41:40	72	\N	\N	\N	\N	1	2	PHRF	\N
110	19	12	2024-04-18 02:43:19	72	\N	\N	\N	\N	1	2	PHRF	\N
214	22	28	2024-05-09 02:46:44	39	\N	\N	\N	\N	1	1	PHRF	\N
112	19	22	2024-04-18 02:43:59	69	\N	\N	\N	\N	1	2	PHRF	\N
113	19	20	2024-04-18 02:45:54	72	\N	\N	\N	\N	1	2	PHRF	\N
114	19	31	2024-04-18 02:49:14	93	\N	\N	\N	\N	2	3	PHRF	\N
115	19	15	2024-04-18 02:50:02	93	\N	\N	\N	\N	2	3	PHRF	\N
116	19	18	2024-04-18 02:50:07	86	\N	\N	\N	\N	2	3	PHRF	\N
216	22	26	2024-05-09 02:49:16	33	\N	\N	\N	\N	1	1	PHRF	\N
218	22	12	2024-05-09 02:51:53	72	\N	\N	\N	\N	1	2	PHRF	\N
220	22	20	2024-05-09 02:57:07	72	\N	\N	\N	\N	1	2	PHRF	\N
222	22	15	2024-05-09 03:02:12	93	\N	\N	\N	\N	2	3	PHRF	\N
74	17	17	2024-03-30 19:07:34	129	\N	\N	\N	\N	2	17	PHRF	\N
75	17	5	2024-03-30 19:08:46	162	\N	\N	\N	\N	2	17	PHRF	\N
76	17	16	2024-03-30 19:09:08	141	\N	\N	\N	\N	2	17	PHRF	\N
81	18	22	2024-04-11 02:49:26	69	\N	\N	\N	\N	1	2	PHRF	\N
82	18	27	2024-04-11 02:51:11	72	\N	\N	\N	\N	1	2	PHRF	\N
83	18	11	2024-04-11 02:52:16	48	\N	\N	\N	\N	1	1	PHRF	\N
85	18	26	2024-04-11 02:57:01	33	\N	\N	\N	\N	1	1	PHRF	\N
86	18	12	2024-04-11 03:00:46	72	\N	\N	\N	\N	1	2	PHRF	\N
87	18	15	2024-04-11 03:06:05	93	\N	\N	\N	\N	2	3	PHRF	\N
117	19	36	2024-04-18 02:52:37	141	\N	\N	\N	\N	2	4	PHRF	\N
118	19	38	2024-04-18 02:54:40	168	\N	\N	\N	\N	2	4	PHRF	\N
119	19	30	2024-04-18 02:55:30	126	\N	\N	\N	\N	2	4	PHRF	\N
121	19	39	2024-04-18 02:57:05	145	\N	\N	\N	\N	2	4	PHRF	\N
123	19	16	2024-04-18 02:58:06	141	\N	\N	\N	\N	2	4	PHRF	\N
125	19	5	2024-04-18 02:59:19	162	\N	\N	\N	\N	2	4	PHRF	\N
126	19	35	2024-04-18 03:00:04	141	\N	\N	\N	\N	2	4	PHRF	\N
190	21	11	2024-05-02 02:43:13	48	\N	\N	\N	\N	1	1	PHRF	\N
192	21	12	2024-05-02 02:45:26	72	\N	\N	\N	\N	1	2	PHRF	\N
194	21	29	2024-05-02 02:49:50	99	\N	\N	\N	\N	2	3	PHRF	\N
196	21	18	2024-05-02 02:51:42	86	\N	\N	\N	\N	2	3	PHRF	\N
198	21	41	2024-05-02 02:52:53	87	\N	\N	\N	\N	2	3	PHRF	\N
202	21	36	2024-05-02 02:59:35	141	\N	\N	\N	\N	2	4	PHRF	\N
204	21	39	2024-05-02 03:13:11	145	\N	\N	\N	\N	2	4	PHRF	\N
208	21	35	2024-05-02 03:33:36	141	\N	\N	\N	\N	2	4	PHRF	\N
150	20	29	2024-04-25 02:47:28	99	\N	\N	\N	\N	2	3	PHRF	\N
138	20	15	2024-04-25 02:47:39	93	\N	\N	\N	\N	2	3	PHRF	\N
1582	87	73	\N	75	\N	\N	\N	RET	2	17	PHRF	\N
1632	90	28	2026-04-09 03:03:57	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1710	94	34	2026-05-07 03:08:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1765	96	41	2026-05-21 03:01:58	93	\N	\N	\N	TIME	2	3	PHRF	\N
1824	99	11	2026-06-04 03:30:48	48	\N	\N	\N	TIME	1	1	PHRF	\N
1884	101	50	2026-06-18 03:06:18	63	\N	\N	\N	TIME	37	64	PHRF	\N
2007	107	63	2026-07-09 03:03:22	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
2065	108	12	2026-07-16 03:41:56	72	\N	\N	\N	TIME	37	65	PHRF	\N
2119	110	1	2026-07-30 03:29:42	150	\N	\N	\N	TIME	38	68	PHRF	\N
2176	112	31	2026-08-13 02:56:41	93	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2244	115	27	2026-08-22 19:04:16	72	\N	\N	\N	TIME	37	65	PHRF	\N
2326	133	110	2026-09-10 01:21:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2399	136	112	2026-10-01 01:11:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1333	73	22	2025-07-31 03:13:00	69	\N	\N	\N	TIME	1	2	PHRF	\N
140	20	31	2024-04-25 02:49:51	93	\N	\N	\N	\N	2	3	PHRF	\N
156	20	41	2024-04-25 02:49:19	87	\N	\N	\N	\N	2	3	PHRF	\N
215	22	11	2024-05-09 02:47:14	48	\N	\N	\N	\N	1	1	PHRF	\N
141	20	18	2024-04-25 02:55:35	86	\N	\N	\N	\N	2	3	PHRF	\N
217	22	22	2024-05-09 02:51:11	69	\N	\N	\N	\N	1	2	PHRF	\N
145	20	5	2024-04-25 03:01:39	162	\N	\N	\N	\N	2	4	PHRF	\N
144	20	39	\N	145	\N	\N	\N	\N	2	4	PHRF	\N
219	22	27	2024-05-09 02:54:01	72	\N	\N	\N	\N	1	2	PHRF	\N
143	20	16	2024-04-25 03:00:37	141	\N	\N	\N	\N	2	4	PHRF	\N
149	20	35	2024-04-25 03:01:45	141	\N	\N	\N	\N	2	4	PHRF	\N
142	20	30	2024-04-25 02:51:09	126	\N	\N	\N	\N	2	4	PHRF	\N
221	22	29	2024-05-09 03:00:48	99	\N	\N	\N	\N	2	3	PHRF	\N
131	20	11	2024-04-25 03:11:53	48	\N	\N	\N	\N	1	1	PHRF	\N
223	22	31	2024-05-09 03:05:37	93	\N	\N	\N	\N	2	3	PHRF	\N
136	20	27	2024-04-25 03:17:43	72	\N	\N	\N	\N	1	2	PHRF	\N
132	20	26	2024-04-25 03:22:30	33	\N	\N	\N	\N	1	1	PHRF	\N
135	20	12	2024-04-25 03:22:55	72	\N	\N	\N	\N	1	2	PHRF	\N
134	20	22	2024-04-25 03:23:09	69	\N	\N	\N	\N	1	2	PHRF	\N
137	20	20	\N	72	\N	\N	\N	\N	1	2	PHRF	\N
159	18	28	2024-04-11 02:54:18	39	\N	\N	\N	\N	1	1	PHRF	\N
160	19	28	2024-04-18 02:43:26	39	\N	\N	\N	\N	1	1	PHRF	\N
161	20	28	2024-04-25 03:14:35	39	\N	\N	\N	\N	1	1	PHRF	\N
162	20	36	2024-04-25 02:52:54	141	\N	\N	\N	\N	2	4	PHRF	\N
226	22	19	2024-05-09 03:09:48	120	\N	\N	\N	\N	2	4	PHRF	\N
232	22	30	2024-05-09 03:13:54	126	\N	\N	\N	\N	2	4	PHRF	\N
233	22	41	2024-05-09 03:14:13	87	\N	\N	\N	\N	2	3	PHRF	\N
234	22	5	2024-05-09 03:16:16	162	\N	\N	\N	\N	2	4	PHRF	\N
235	22	36	2024-05-09 03:16:53	141	\N	\N	\N	\N	2	4	PHRF	\N
236	22	16	2024-05-09 03:17:39	141	\N	\N	\N	\N	2	4	PHRF	\N
237	22	35	2024-05-09 03:18:53	141	\N	\N	\N	\N	2	4	PHRF	\N
267	23	29	2024-05-16 02:45:54	99	\N	\N	\N	\N	2	3	PHRF	\N
268	23	19	2024-05-16 02:47:02	120	\N	\N	\N	\N	2	4	PHRF	\N
269	23	30	2024-05-16 02:48:42	126	\N	\N	\N	\N	2	4	PHRF	\N
270	23	41	2024-05-16 02:50:09	87	\N	\N	\N	\N	2	3	PHRF	\N
271	23	18	2024-05-16 02:53:38	86	\N	\N	\N	\N	2	3	PHRF	\N
272	23	36	2024-05-16 02:56:15	141	\N	\N	\N	\N	2	4	PHRF	\N
273	23	16	2024-05-16 02:58:26	141	\N	\N	\N	\N	2	4	PHRF	\N
275	23	5	2024-05-16 03:00:39	162	\N	\N	\N	\N	2	4	PHRF	\N
278	23	35	2024-05-16 03:19:11	141	\N	\N	\N	\N	2	4	PHRF	\N
280	23	28	2024-05-16 03:35:01	39	\N	\N	\N	\N	1	1	PHRF	\N
286	23	11	2024-05-16 03:47:57	48	\N	\N	\N	\N	1	1	PHRF	\N
287	23	26	2024-05-16 03:49:08	33	\N	\N	\N	\N	1	1	PHRF	\N
1584	88	11	2026-02-07 20:05:22	48	\N	\N	\N	TIME	1	16	PHRF	\N
1633	90	11	2026-04-09 03:04:48	48	\N	\N	\N	TIME	1	1	PHRF	\N
1711	94	12	2026-05-07 03:10:17	72	\N	\N	\N	TIME	1	2	PHRF	\N
1766	96	19	2026-05-21 03:01:59	120	\N	\N	\N	TIME	2	4	PHRF	\N
1825	99	35	2026-06-04 03:32:02	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1885	101	48	2026-06-18 03:07:00	99	\N	\N	\N	TIME	38	67	PHRF	\N
2008	107	27	2026-07-09 03:06:13	72	\N	\N	\N	TIME	37	65	PHRF	\N
2066	108	41	2026-07-16 03:42:13	93	\N	\N	\N	TIME	37	65	PHRF	\N
2120	110	21	\N	195	\N	\N	\N	DNF	38	68	PHRF	\N
2177	112	105	2026-08-13 03:01:13	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2245	115	11	2026-08-22 19:07:10	48	\N	\N	\N	TIME	37	64	PHRF	\N
2327	133	112	2026-09-10 01:22:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2400	136	115	2026-10-01 01:12:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
242	23	12	\N	72	\N	\N	\N	\N	1	2	PHRF	\N
243	23	20	\N	72	\N	\N	\N	\N	1	2	PHRF	\N
241	23	22	\N	69	\N	\N	\N	\N	1	2	PHRF	\N
244	23	27	\N	72	\N	\N	\N	\N	1	2	PHRF	\N
248	23	31	\N	93	\N	\N	\N	\N	2	3	PHRF	\N
288	23	15	\N	93	\N	\N	\N	\N	2	3	PHRF	\N
332	24	35	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
334	24	5	\N	162	\N	\N	\N	\N	2	4	PHRF	\N
200	21	30	\N	126	\N	\N	\N	\N	2	4	PHRF	\N
384	26	30	\N	126	\N	\N	\N	\N	2	4	PHRF	\N
385	26	16	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
386	26	35	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
387	26	11	\N	48	\N	\N	1	\N	1	1	PHRF	\N
388	26	12	\N	72	\N	\N	\N	\N	1	2	PHRF	\N
389	26	39	\N	145	\N	\N	\N	\N	2	4	PHRF	\N
390	26	38	\N	168	\N	\N	\N	\N	2	4	PHRF	\N
402	27	35	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
472	28	35	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
526	31	49	\N	90	\N	\N	\N	\N	2	3	PHRF	\N
496	30	49	\N	90	\N	\N	\N	\N	2	3	PHRF	\N
568	42	20	\N	72	\N	\N	\N	\N	1	2	PHRF	\N
569	42	16	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
317	24	11	2024-05-23 02:58:44	48	\N	\N	\N	\N	1	1	PHRF	\N
318	24	22	2024-05-23 03:02:39	69	\N	\N	\N	\N	1	2	PHRF	\N
319	24	27	2024-05-23 03:05:22	72	\N	\N	\N	\N	1	2	PHRF	\N
320	24	26	2024-05-23 03:11:39	33	\N	\N	\N	\N	1	1	PHRF	\N
321	24	29	2024-05-23 03:13:05	99	\N	\N	\N	\N	2	3	PHRF	\N
322	24	12	2024-05-23 03:13:14	72	\N	\N	\N	\N	1	2	PHRF	\N
323	24	19	2024-05-23 03:18:10	120	\N	\N	\N	\N	2	4	PHRF	\N
324	24	30	2024-05-23 03:20:42	126	\N	\N	\N	\N	2	4	PHRF	\N
325	24	18	2024-05-23 03:25:37	86	\N	\N	\N	\N	2	3	PHRF	\N
326	24	36	2024-05-23 03:26:41	141	\N	\N	\N	\N	2	4	PHRF	\N
327	24	20	2024-05-23 03:28:41	72	\N	\N	\N	\N	1	2	PHRF	\N
328	24	43	2024-05-23 03:33:06	93	\N	\N	\N	\N	2	3	PHRF	\N
329	24	41	2024-05-23 03:35:08	87	\N	\N	\N	\N	2	3	PHRF	\N
330	24	39	2024-05-23 03:43:29	145	\N	\N	\N	\N	2	4	PHRF	\N
1344	73	36	2025-07-31 03:30:06	141	\N	\N	\N	TIME	2	4	PHRF	\N
1353	74	11	2025-08-07 02:49:41	48	\N	\N	\N	TIME	1	1	PHRF	\N
1361	74	90	2025-08-07 03:08:45	96	\N	\N	\N	TIME	2	3	PHRF	\N
340	25	28	2024-05-30 02:47:15	39	\N	\N	\N	\N	1	1	PHRF	\N
341	25	29	2024-05-30 02:48:10	99	\N	\N	\N	\N	2	3	PHRF	\N
342	25	11	2024-05-30 02:48:58	48	\N	\N	\N	\N	1	1	PHRF	\N
343	25	12	2024-05-30 02:49:39	72	\N	\N	\N	\N	1	2	PHRF	\N
344	25	22	2024-05-30 02:50:11	69	\N	\N	\N	\N	1	2	PHRF	\N
1585	88	28	2026-02-07 20:06:38	42	\N	\N	\N	TIME	1	16	ORC_PHRF	04560003L6C
1634	90	27	2026-04-09 03:09:02	72	\N	\N	\N	TIME	1	2	PHRF	\N
1712	94	26	2026-05-07 03:12:18	33	\N	\N	\N	TIME	1	1	PHRF	\N
1767	96	73	\N	75	\N	\N	\N	NSC	2	3	PHRF	\N
1826	99	28	2026-06-04 03:32:00	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1886	101	90	2026-06-18 03:11:19	96	\N	\N	\N	TIME	38	67	PHRF	\N
2009	107	28	2026-07-09 03:06:32	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
2067	108	18	2026-07-16 03:46:34	84	\N	\N	\N	TIME	37	65	PHRF	\N
2123	111	36	2026-08-06 03:01:16	141	\N	\N	\N	TIME	38	68	PHRF	\N
2178	112	63	2026-08-13 03:02:25	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
2246	115	12	2026-08-22 19:16:27	72	\N	\N	\N	TIME	37	65	PHRF	\N
2328	133	114	2026-09-10 01:23:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2401	136	113	2026-10-01 01:13:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
345	25	15	2024-05-30 02:50:25	93	\N	\N	\N	\N	2	3	PHRF	\N
346	25	26	2024-05-30 02:50:40	33	\N	\N	\N	\N	1	1	PHRF	\N
347	25	19	2024-05-30 02:51:05	120	\N	\N	\N	\N	2	4	PHRF	\N
348	25	30	2024-05-30 02:51:21	126	\N	\N	\N	\N	2	4	PHRF	\N
349	25	18	2024-05-30 02:51:38	86	\N	\N	\N	\N	2	3	PHRF	\N
350	25	43	2024-05-30 02:53:07	93	\N	\N	\N	\N	2	3	PHRF	\N
351	25	41	2024-05-30 02:53:32	87	\N	\N	\N	\N	2	3	PHRF	\N
352	25	36	2024-05-30 02:53:53	141	\N	\N	\N	\N	2	4	PHRF	\N
354	25	5	2024-05-30 02:55:40	162	\N	\N	\N	\N	2	4	PHRF	\N
365	26	28	2024-06-06 02:54:25	39	\N	\N	\N	\N	1	1	PHRF	\N
366	26	27	2024-06-06 02:55:06	72	\N	\N	\N	\N	1	2	PHRF	\N
367	26	26	2024-06-06 02:59:26	33	\N	\N	\N	\N	1	1	PHRF	\N
370	26	15	2024-06-06 03:04:27	93	\N	\N	\N	\N	2	3	PHRF	\N
371	26	41	2024-06-06 03:09:55	87	\N	\N	\N	\N	2	3	PHRF	\N
372	26	18	2024-06-06 03:11:32	86	\N	\N	\N	\N	2	3	PHRF	\N
373	26	19	2024-06-06 03:11:45	120	\N	\N	\N	\N	2	4	PHRF	\N
374	26	36	2024-06-06 03:14:31	141	\N	\N	\N	\N	2	4	PHRF	\N
375	26	31	2024-06-06 03:15:02	93	\N	\N	\N	\N	2	3	PHRF	\N
376	26	5	2024-06-06 03:33:38	162	\N	\N	\N	\N	2	4	PHRF	\N
369	26	29	2024-06-06 03:03:27	99	\N	\N	\N	\N	2	3	PHRF	\N
368	26	22	2024-06-06 02:59:44	69	\N	\N	\N	\N	1	2	PHRF	\N
391	27	11	2024-06-13 03:04:34	48	\N	\N	\N	\N	1	1	PHRF	\N
398	27	28	2024-06-13 03:08:14	39	\N	\N	\N	\N	1	1	PHRF	\N
399	27	27	2024-06-13 03:09:32	72	\N	\N	\N	\N	1	2	PHRF	\N
401	27	22	2024-06-13 03:10:49	69	\N	\N	\N	\N	1	2	PHRF	\N
404	27	12	2024-06-13 03:10:11	72	\N	\N	\N	\N	1	2	PHRF	\N
405	27	48	2024-06-13 03:18:07	99	\N	\N	\N	\N	2	3	PHRF	\N
406	27	15	2024-06-13 03:18:39	93	\N	\N	\N	\N	2	3	PHRF	\N
407	27	20	2024-06-13 03:19:21	72	\N	\N	\N	\N	1	2	PHRF	\N
408	27	29	2024-06-13 03:19:59	99	\N	\N	\N	\N	2	3	PHRF	\N
409	27	31	2024-06-13 03:20:03	93	\N	\N	\N	\N	2	3	PHRF	\N
410	27	30	2024-06-13 03:23:06	126	\N	\N	\N	\N	2	4	PHRF	\N
411	27	18	2024-06-13 03:23:16	86	\N	\N	\N	\N	2	3	PHRF	\N
412	27	19	2024-06-13 03:23:32	120	\N	\N	\N	\N	2	4	PHRF	\N
413	27	41	2024-06-13 03:23:35	87	\N	\N	\N	\N	2	3	PHRF	\N
414	27	36	2024-06-13 03:35:35	141	\N	\N	\N	\N	2	4	PHRF	\N
415	27	26	2024-06-13 03:43:53	33	\N	\N	\N	\N	1	1	PHRF	\N
416	27	5	2024-06-13 03:39:44	162	\N	\N	\N	\N	2	4	PHRF	\N
417	27	16	2024-06-13 03:37:39	141	\N	\N	\N	\N	2	4	PHRF	\N
423	29	11	2024-06-20 02:52:53	48	\N	\N	\N	\N	1	1	PHRF	\N
424	29	28	2024-06-20 02:54:32	39	\N	\N	\N	\N	1	1	PHRF	\N
425	29	27	2024-06-20 02:55:29	72	\N	\N	\N	\N	1	2	PHRF	\N
1635	90	22	2026-04-09 03:14:27	69	\N	\N	\N	TIME	1	2	PHRF	\N
1713	94	22	2026-05-07 03:13:36	69	\N	\N	\N	TIME	1	2	PHRF	\N
1768	96	36	2026-05-21 03:05:16	141	\N	\N	\N	TIME	2	4	PHRF	\N
1827	99	94	2026-06-04 03:38:23	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1887	101	19	2026-06-18 03:12:02	120	\N	\N	\N	TIME	38	67	PHRF	\N
2010	107	11	2026-07-09 03:07:46	48	\N	\N	\N	TIME	37	64	PHRF	\N
2068	108	94	2026-07-16 03:48:40	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2124	111	18	2026-08-06 03:01:40	84	\N	\N	\N	TIME	37	65	PHRF	\N
2179	112	11	2026-08-13 03:03:25	48	\N	\N	\N	TIME	37	64	PHRF	\N
2247	115	50	2026-08-22 19:16:35	63	\N	\N	\N	TIME	37	64	PHRF	\N
2329	133	111	2026-09-10 01:24:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2402	136	110	2026-10-01 01:14:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
426	29	26	2024-06-20 02:56:17	33	\N	\N	\N	\N	1	1	PHRF	\N
427	29	22	2024-06-20 02:57:13	69	\N	\N	\N	\N	1	2	PHRF	\N
428	29	12	2024-06-20 02:57:29	72	\N	\N	\N	\N	1	2	PHRF	\N
430	29	15	2024-06-20 02:59:49	93	\N	\N	\N	\N	2	3	PHRF	\N
431	29	20	2024-06-20 03:01:07	72	\N	\N	\N	\N	1	2	PHRF	\N
433	29	48	2024-06-20 03:02:40	99	\N	\N	\N	\N	2	3	PHRF	\N
434	29	29	2024-06-20 03:03:18	99	\N	\N	\N	\N	2	3	PHRF	\N
435	29	19	2024-06-20 03:03:25	120	\N	\N	\N	\N	2	4	PHRF	\N
436	29	31	2024-06-20 03:04:24	93	\N	\N	\N	\N	2	3	PHRF	\N
437	29	30	2024-06-20 03:05:02	126	\N	\N	\N	\N	2	4	PHRF	\N
438	29	18	2024-06-20 03:05:50	86	\N	\N	\N	\N	2	3	PHRF	\N
439	29	36	2024-06-20 03:06:43	141	\N	\N	\N	\N	2	4	PHRF	\N
441	29	5	2024-06-20 03:11:48	162	\N	\N	\N	\N	2	4	PHRF	\N
442	29	16	2024-06-20 03:13:34	141	\N	\N	\N	\N	2	4	PHRF	\N
443	32	26	2024-06-22 21:56:37	33	\N	\N	\N	\N	1	16	PHRF	\N
444	32	15	2024-06-22 22:08:07	93	\N	\N	\N	\N	2	17	PHRF	\N
445	32	29	2024-06-22 22:09:11	99	\N	\N	\N	\N	2	17	PHRF	\N
446	32	18	2024-06-22 22:22:38	86	\N	\N	\N	\N	2	17	PHRF	\N
447	32	36	2024-06-22 22:24:01	141	\N	\N	\N	\N	2	17	PHRF	\N
440	29	41	2024-06-20 03:11:09	93	\N	\N	\N	\N	2	3	PHRF	\N
449	28	26	2024-06-27 02:51:02	33	\N	\N	\N	\N	1	1	PHRF	\N
450	28	11	2024-06-27 02:51:03	48	\N	\N	\N	\N	1	1	PHRF	\N
451	28	28	2024-06-27 02:54:17	39	\N	\N	\N	\N	1	1	PHRF	\N
452	28	20	2024-06-27 02:58:30	72	\N	\N	\N	\N	1	2	PHRF	\N
453	28	22	2024-06-27 03:02:03	69	\N	\N	\N	\N	1	2	PHRF	\N
454	28	15	2024-06-27 03:02:54	93	\N	\N	\N	\N	2	3	PHRF	\N
455	28	19	2024-06-27 03:04:34	120	\N	\N	\N	\N	2	4	PHRF	\N
456	28	30	2024-06-27 03:07:28	126	\N	\N	\N	\N	2	4	PHRF	\N
457	28	29	2024-06-27 03:08:14	99	\N	\N	\N	\N	2	3	PHRF	\N
458	28	48	2024-06-27 03:08:21	99	\N	\N	\N	\N	2	3	PHRF	\N
459	28	41	2024-06-27 03:09:28	93	\N	\N	\N	\N	2	3	PHRF	\N
460	28	31	2024-06-27 03:11:22	93	\N	\N	\N	\N	2	3	PHRF	\N
461	28	36	2024-06-27 03:11:53	141	\N	\N	\N	\N	2	4	PHRF	\N
462	28	18	2024-06-27 03:13:15	86	\N	\N	\N	\N	2	3	PHRF	\N
464	28	5	2024-06-27 03:23:04	162	\N	\N	\N	\N	2	4	PHRF	\N
465	28	16	2024-06-27 03:24:59	141	\N	\N	\N	\N	2	4	PHRF	\N
483	30	27	2024-07-04 02:58:00	72	\N	\N	\N	\N	1	2	PHRF	\N
484	30	28	2024-07-04 02:58:45	39	\N	\N	\N	\N	1	1	PHRF	\N
485	30	48	2024-07-04 03:00:24	99	\N	\N	\N	\N	2	3	PHRF	\N
486	30	12	2024-07-04 03:00:28	72	\N	\N	\N	\N	1	2	PHRF	\N
487	30	26	2024-07-04 03:03:35	33	\N	\N	\N	\N	1	1	PHRF	\N
488	30	22	2024-07-04 03:04:26	69	\N	\N	\N	\N	1	2	PHRF	\N
489	30	29	2024-07-04 03:04:35	99	\N	\N	\N	\N	2	3	PHRF	\N
1588	88	50	2026-02-07 20:17:23	63	\N	\N	\N	TIME	1	16	PHRF	\N
1636	90	12	2026-04-09 03:15:12	72	\N	\N	\N	TIME	1	2	PHRF	\N
1714	94	99	2026-05-07 03:14:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1769	96	16	2026-05-21 03:05:44	141	\N	\N	\N	TIME	2	4	PHRF	\N
1828	99	50	2026-06-04 03:39:00	63	\N	\N	\N	TIME	1	2	PHRF	\N
1888	101	36	2026-06-18 03:12:45	141	\N	\N	\N	TIME	38	68	PHRF	\N
2011	107	12	2026-07-09 03:13:05	72	\N	\N	\N	TIME	37	65	PHRF	\N
2069	108	88	2026-07-16 03:50:32	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2125	111	19	2026-08-06 03:02:56	120	\N	\N	\N	TIME	38	67	PHRF	\N
2180	112	93	2026-08-13 03:04:19	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2248	115	18	2026-08-22 19:18:44	84	\N	\N	\N	TIME	37	65	PHRF	\N
2330	133	113	2026-09-10 01:25:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2403	136	111	2026-10-01 01:15:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
490	30	19	2024-07-04 03:05:23	120	\N	\N	\N	\N	2	4	PHRF	\N
491	30	41	2024-07-04 03:07:43	93	\N	\N	\N	\N	2	3	PHRF	\N
492	30	31	2024-07-04 03:09:20	93	\N	\N	\N	\N	2	3	PHRF	\N
493	30	30	2024-07-04 03:09:52	126	\N	\N	\N	\N	2	4	PHRF	\N
494	30	18	2024-07-04 03:12:13	86	\N	\N	\N	\N	2	3	PHRF	\N
495	30	36	2024-07-04 03:13:00	141	\N	\N	\N	\N	2	4	PHRF	\N
513	31	11	2024-07-11 03:29:01	48	\N	\N	\N	\N	1	1	PHRF	\N
514	31	27	2024-07-11 03:31:45	72	\N	\N	\N	\N	1	2	PHRF	\N
515	31	28	2024-07-11 03:33:57	39	\N	\N	\N	\N	1	1	PHRF	\N
516	31	12	2024-07-11 03:36:38	72	\N	\N	\N	\N	1	2	PHRF	\N
517	31	26	2024-07-11 03:36:46	33	\N	\N	\N	\N	1	1	PHRF	\N
518	31	48	2024-07-11 03:38:46	99	\N	\N	\N	\N	2	3	PHRF	\N
519	31	15	2024-07-11 03:38:54	93	\N	\N	\N	\N	2	3	PHRF	\N
520	31	29	2024-07-11 03:40:26	99	\N	\N	\N	\N	2	3	PHRF	\N
521	31	30	2024-07-11 03:42:43	126	\N	\N	\N	\N	2	4	PHRF	\N
522	31	19	2024-07-11 03:43:16	120	\N	\N	\N	\N	2	4	PHRF	\N
523	31	18	2024-07-11 03:44:23	86	\N	\N	\N	\N	2	3	PHRF	\N
524	31	41	2024-07-11 03:44:03	93	\N	\N	\N	\N	2	3	PHRF	\N
525	31	31	2024-07-11 03:45:16	93	\N	\N	\N	\N	2	3	PHRF	\N
527	31	36	2024-07-11 03:48:47	141	\N	\N	\N	\N	2	4	PHRF	\N
528	31	20	2024-07-11 03:48:54	72	\N	\N	\N	\N	1	2	PHRF	\N
529	31	16	2024-07-11 03:53:15	141	\N	\N	\N	\N	2	4	PHRF	\N
530	31	5	2024-07-11 03:57:12	162	\N	\N	\N	\N	2	4	PHRF	\N
531	33	29	2024-07-13 22:26:51	99	\N	\N	\N	\N	2	17	PHRF	\N
532	33	15	2024-07-13 22:30:35	93	\N	\N	\N	\N	2	17	PHRF	\N
533	33	52	2024-07-13 22:46:00	105	\N	\N	\N	\N	2	17	PHRF	\N
534	33	18	2024-07-13 22:49:43	86	\N	\N	\N	\N	2	17	PHRF	\N
535	33	11	2024-07-13 23:02:50	48	\N	\N	\N	\N	1	16	PHRF	\N
536	33	26	2024-07-13 23:08:44	33	\N	\N	\N	\N	1	16	PHRF	\N
537	33	12	2024-07-13 23:09:21	72	\N	\N	\N	\N	1	16	PHRF	\N
538	33	36	2024-07-13 23:22:05	141	\N	\N	\N	\N	2	17	PHRF	\N
539	33	16	2024-07-13 23:23:25	141	\N	\N	\N	\N	2	17	PHRF	\N
545	42	11	2024-07-18 03:29:52	48	\N	\N	\N	\N	1	1	PHRF	\N
553	42	27	2024-07-18 03:33:26	72	\N	\N	\N	\N	1	2	PHRF	\N
554	42	28	2024-07-18 03:34:38	39	\N	\N	\N	\N	1	1	PHRF	\N
556	42	26	2024-07-18 03:39:29	33	\N	\N	\N	\N	1	1	PHRF	\N
557	42	48	2024-07-18 03:45:44	99	\N	\N	\N	\N	2	3	PHRF	\N
558	42	22	2024-07-18 03:47:05	69	\N	\N	\N	\N	1	2	PHRF	\N
559	42	29	2024-07-18 03:51:05	99	\N	\N	\N	\N	2	3	PHRF	\N
560	42	41	2024-07-18 03:56:11	93	\N	\N	\N	\N	2	3	PHRF	\N
562	42	18	2024-07-18 04:01:31	86	\N	\N	\N	\N	2	3	PHRF	\N
561	42	19	2024-07-18 04:00:04	120	\N	\N	\N	\N	2	4	PHRF	\N
1590	88	12	2026-02-07 20:25:27	72	\N	\N	\N	TIME	1	16	PHRF	\N
1637	90	50	\N	63	\N	\N	\N	RET	1	2	PHRF	\N
1715	94	15	2026-05-07 03:14:57	93	\N	\N	\N	TIME	2	3	PHRF	\N
1770	96	39	2026-05-21 03:06:19	144	\N	\N	\N	TIME	2	4	PHRF	\N
1829	99	93	2026-06-04 03:39:48	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1889	101	30	2026-06-18 03:17:22	126	\N	\N	\N	TIME	38	67	PHRF	\N
2012	107	48	2026-07-09 03:14:10	99	\N	\N	\N	TIME	38	67	PHRF	\N
2126	111	30	2026-08-06 03:01:47	126	\N	\N	\N	TIME	38	67	PHRF	\N
2181	112	27	2026-08-13 03:05:12	72	\N	\N	\N	TIME	37	65	PHRF	\N
2249	115	93	2026-08-22 19:20:29	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2331	117	11	2026-09-12 19:59:28	48	\N	\N	\N	TIME	37	66	PHRF	\N
2406	136	117	2026-10-01 01:16:00	200	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
564	42	43	2024-07-18 04:04:31	93	\N	\N	\N	\N	2	3	PHRF	\N
565	42	31	2024-07-18 04:09:46	93	\N	\N	\N	\N	2	3	PHRF	\N
566	42	36	2024-07-18 04:18:01	141	\N	\N	\N	\N	2	4	PHRF	\N
567	42	5	2024-07-18 04:18:28	162	\N	\N	\N	\N	2	4	PHRF	\N
570	42	15	2024-07-18 03:43:09	93	\N	\N	\N	\N	2	3	PHRF	\N
572	43	11	2024-07-25 02:43:18	48	\N	\N	\N	\N	1	1	PHRF	\N
573	43	27	2024-07-25 02:43:19	72	\N	\N	\N	\N	1	2	PHRF	\N
574	43	26	2024-07-25 02:48:37	33	\N	\N	\N	\N	1	1	PHRF	\N
575	43	19	2024-07-25 02:54:21	120	\N	\N	\N	\N	2	4	PHRF	\N
576	43	29	2024-07-25 02:56:20	99	\N	\N	\N	\N	2	3	PHRF	\N
577	43	15	2024-07-25 02:57:40	93	\N	\N	\N	\N	2	3	PHRF	\N
578	43	20	2024-07-25 02:58:40	72	\N	\N	\N	\N	1	2	PHRF	\N
579	43	41	2024-07-25 02:59:11	93	\N	\N	\N	\N	2	3	PHRF	\N
580	43	43	2024-07-25 02:59:17	93	\N	\N	\N	\N	2	3	PHRF	\N
581	43	18	2024-07-25 02:59:33	86	\N	\N	\N	\N	2	3	PHRF	\N
583	43	36	2024-07-25 03:02:44	141	\N	\N	\N	\N	2	4	PHRF	\N
584	43	39	2024-07-25 03:05:29	145	\N	\N	\N	\N	2	4	PHRF	\N
585	43	5	2024-07-25 03:06:54	162	\N	\N	\N	\N	2	4	PHRF	\N
586	43	16	2024-07-25 03:08:01	141	\N	\N	\N	\N	2	4	PHRF	\N
593	43	28	2024-07-25 02:47:30	39	\N	\N	\N	\N	1	1	PHRF	\N
594	43	50	2024-07-25 02:48:11	63	\N	\N	\N	\N	1	2	PHRF	\N
1335	73	12	2025-07-31 03:17:33	72	\N	\N	\N	TIME	1	2	PHRF	\N
596	43	35	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
598	44	15	2024-08-01 02:53:11	93	\N	\N	\N	\N	2	3	PHRF	\N
599	44	29	2024-08-01 02:53:53	99	\N	\N	\N	\N	2	3	PHRF	\N
600	44	31	2024-08-01 02:54:15	93	\N	\N	\N	\N	2	3	PHRF	\N
601	44	36	2024-08-01 02:55:20	141	\N	\N	\N	\N	2	4	PHRF	\N
603	44	43	2024-08-01 02:56:02	93	\N	\N	\N	\N	2	3	PHRF	\N
604	44	18	2024-08-01 02:56:31	86	\N	\N	\N	\N	2	3	PHRF	\N
605	44	5	2024-08-01 02:56:47	162	\N	\N	\N	\N	2	4	PHRF	\N
608	44	41	2024-08-01 02:58:46	93	\N	\N	\N	\N	2	3	PHRF	\N
609	44	16	2024-08-01 02:59:08	141	\N	\N	\N	\N	2	4	PHRF	\N
610	44	19	2024-08-01 03:00:03	120	\N	\N	\N	\N	2	4	PHRF	\N
615	44	28	2024-08-01 03:12:15	39	\N	\N	\N	\N	1	1	PHRF	\N
617	44	27	2024-08-01 03:14:44	72	\N	\N	\N	\N	1	2	PHRF	\N
618	44	11	2024-08-01 03:18:34	48	\N	\N	\N	\N	1	1	PHRF	\N
616	44	50	2024-08-01 03:14:33	63	\N	\N	\N	\N	1	2	PHRF	\N
620	45	11	2024-08-08 02:45:25	48	\N	\N	\N	\N	1	1	PHRF	\N
621	45	28	2024-08-08 02:50:45	39	\N	\N	\N	\N	1	1	PHRF	\N
623	45	12	2024-08-08 02:56:14	72	\N	\N	\N	\N	1	2	PHRF	\N
624	45	15	2024-08-08 02:58:27	93	\N	\N	\N	\N	2	3	PHRF	\N
625	45	29	2024-08-08 02:59:10	99	\N	\N	\N	\N	2	3	PHRF	\N
626	45	41	2024-08-08 03:01:36	93	\N	\N	\N	\N	2	3	PHRF	\N
1591	88	15	2026-02-07 20:25:42	93	\N	\N	\N	TIME	2	17	PHRF	\N
1638	90	56	2026-04-09 03:24:12	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1716	94	35	2026-05-07 03:15:45	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1771	96	5	2026-05-21 03:07:14	162	\N	\N	\N	TIME	2	4	PHRF	\N
1830	99	22	2026-06-04 03:42:49	69	\N	\N	\N	TIME	1	2	PHRF	\N
1890	101	73	2026-06-18 03:18:32	75	\N	\N	\N	TIME	37	65	PHRF	\N
1950	103	105	2026-06-27 22:48:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2013	107	19	2026-07-09 03:14:40	120	\N	\N	\N	TIME	38	67	PHRF	\N
2071	108	106	\N	93	\N	\N	\N	DNF	37	65	PHRF	\N
2127	111	15	2026-08-06 03:03:14	93	\N	\N	\N	TIME	37	65	PHRF	\N
2182	112	21	2026-08-13 03:06:19	195	\N	\N	\N	TIME	38	68	PHRF	\N
2250	115	35	2026-08-22 19:22:01	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2332	117	63	2026-09-12 19:59:50	18	\N	\N	\N	TIME	37	66	ORC_PHRF	04560004KI4
2407	137	112	2026-10-01 01:21:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
627	45	18	2024-08-08 03:03:03	86	\N	\N	\N	\N	2	3	PHRF	\N
628	45	19	2024-08-08 03:03:29	120	\N	\N	\N	\N	2	4	PHRF	\N
629	45	36	2024-08-08 03:07:45	141	\N	\N	\N	\N	2	4	PHRF	\N
631	45	5	2024-08-08 03:10:10	162	\N	\N	\N	\N	2	4	PHRF	\N
633	45	22	2024-08-08 03:13:21	69	\N	\N	\N	\N	1	2	PHRF	\N
634	45	16	2024-08-08 03:14:03	141	\N	\N	\N	\N	2	4	PHRF	\N
635	45	26	2024-08-08 03:14:35	33	\N	\N	\N	\N	1	1	PHRF	\N
636	45	39	2024-08-08 03:19:45	145	\N	\N	\N	\N	2	4	PHRF	\N
622	45	50	2024-08-08 02:51:56	63	\N	\N	\N	\N	1	2	PHRF	\N
644	47	11	2024-08-10 21:03:18	48	\N	\N	\N	\N	1	16	PHRF	\N
645	47	29	2024-08-10 21:04:48	99	\N	\N	\N	\N	2	17	PHRF	\N
646	47	15	2024-08-10 21:05:07	93	\N	\N	\N	\N	2	17	PHRF	\N
647	47	52	2024-08-10 21:06:15	105	\N	\N	\N	\N	2	17	PHRF	\N
648	47	43	2024-08-10 21:07:12	93	\N	\N	\N	\N	2	17	PHRF	\N
649	47	36	2024-08-10 21:12:59	141	\N	\N	\N	\N	2	17	PHRF	\N
650	47	16	2024-08-10 21:30:54	141	\N	\N	\N	\N	2	17	PHRF	\N
653	34	11	2024-08-15 02:56:19	48	\N	\N	\N	\N	1	1	PHRF	\N
654	34	28	2024-08-15 02:57:53	39	\N	\N	\N	\N	1	1	PHRF	\N
655	34	27	2024-08-15 03:00:23	72	\N	\N	\N	\N	1	2	PHRF	\N
656	34	15	2024-08-15 03:00:59	93	\N	\N	\N	\N	2	3	PHRF	\N
657	34	26	2024-08-15 03:01:31	33	\N	\N	\N	\N	1	1	PHRF	\N
658	34	19	2024-08-15 03:06:57	120	\N	\N	\N	\N	2	4	PHRF	\N
659	34	29	2024-08-15 03:04:44	99	\N	\N	\N	\N	2	3	PHRF	\N
660	34	41	2024-08-15 03:12:05	93	\N	\N	\N	\N	2	3	PHRF	\N
661	34	38	2024-08-15 03:15:53	168	\N	\N	\N	\N	2	4	PHRF	\N
662	34	30	2024-08-15 03:16:26	126	\N	\N	\N	\N	2	4	PHRF	\N
663	34	36	2024-08-15 03:17:03	141	\N	\N	\N	\N	2	4	PHRF	\N
664	34	39	2024-08-15 03:17:51	145	\N	\N	\N	\N	2	4	PHRF	\N
665	34	31	2024-08-15 03:18:17	93	\N	\N	\N	\N	2	3	PHRF	\N
666	34	18	2024-08-15 03:18:19	86	\N	\N	\N	\N	2	3	PHRF	\N
667	34	43	2024-08-15 03:18:31	93	\N	\N	\N	\N	2	3	PHRF	\N
669	34	5	2024-08-15 03:26:37	162	\N	\N	\N	\N	2	4	PHRF	\N
670	34	16	2024-08-15 03:26:38	141	\N	\N	\N	\N	2	4	PHRF	\N
1354	74	27	2025-08-07 02:54:07	72	\N	\N	\N	TIME	1	2	PHRF	\N
684	35	5	\N	162	\N	\N	\N	\N	2	4	PHRF	\N
692	36	27	2024-08-22 02:50:11	72	\N	\N	\N	\N	1	2	PHRF	\N
693	36	12	2024-08-22 02:53:05	72	\N	\N	\N	\N	1	2	PHRF	\N
694	36	22	2024-08-22 02:53:40	69	\N	\N	\N	\N	1	2	PHRF	\N
695	36	26	2024-08-22 02:57:31	33	\N	\N	\N	\N	1	1	PHRF	\N
697	36	15	2024-08-22 03:12:37	93	\N	\N	\N	\N	2	3	PHRF	\N
698	36	29	2024-08-22 03:13:01	99	\N	\N	\N	\N	2	3	PHRF	\N
699	36	30	2024-08-22 03:14:50	126	\N	\N	\N	\N	2	4	PHRF	\N
1592	88	22	2026-02-07 20:28:14	69	\N	\N	\N	TIME	1	16	PHRF	\N
1639	90	88	2026-04-09 03:26:47	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1717	94	18	2026-05-07 03:17:05	81	\N	\N	\N	TIME	2	3	PHRF	\N
1772	96	1	2026-05-21 03:07:38	150	\N	\N	\N	TIME	2	4	PHRF	\N
1831	99	29	2026-06-04 03:46:29	99	\N	\N	\N	TIME	2	3	PHRF	\N
1891	101	105	2026-06-18 03:20:23	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2014	107	18	2026-07-09 03:14:56	84	\N	\N	\N	TIME	37	65	PHRF	\N
2072	108	90	2026-07-16 03:55:55	93	\N	\N	\N	TIME	37	65	PHRF	\N
2128	111	5	2026-08-06 03:03:42	162	\N	\N	\N	TIME	38	68	PHRF	\N
2183	112	35	2026-08-13 03:08:54	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2251	115	15	2026-08-22 19:25:06.902	93	\N	\N	\N	TIME	37	65	PHRF	\N
2333	117	15	2026-09-12 20:10:07	93	\N	\N	\N	TIME	37	66	PHRF	\N
2408	137	115	2026-10-01 01:22:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
700	36	19	2024-08-22 03:17:33	120	\N	\N	\N	\N	2	4	PHRF	\N
701	36	43	2024-08-22 03:16:14	93	\N	\N	\N	\N	2	3	PHRF	\N
702	36	41	2024-08-22 03:21:18	93	\N	\N	\N	\N	2	3	PHRF	\N
704	36	18	2024-08-22 03:28:30	86	\N	\N	\N	\N	2	3	PHRF	\N
705	36	5	\N	162	\N	\N	\N	\N	2	4	PHRF	\N
706	36	36	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
707	36	16	\N	141	\N	\N	\N	\N	2	4	PHRF	\N
709	37	27	2024-08-24 19:05:28	72	\N	\N	\N	\N	1	2	PHRF	\N
710	37	12	2024-08-24 19:09:51	72	\N	\N	\N	\N	1	2	PHRF	\N
711	37	22	2024-08-24 19:10:12	69	\N	\N	\N	\N	1	2	PHRF	\N
712	37	19	2024-08-24 19:19:21	120	\N	\N	\N	\N	2	4	PHRF	\N
713	37	18	2024-08-24 19:23:15	86	\N	\N	\N	\N	2	3	PHRF	\N
714	37	43	2024-08-24 19:26:58	93	\N	\N	\N	\N	2	3	PHRF	\N
715	37	36	2024-08-24 19:28:42	141	\N	\N	\N	\N	2	4	PHRF	\N
716	37	5	2024-08-24 19:34:10	162	\N	\N	\N	\N	2	4	PHRF	\N
717	37	16	2024-08-24 19:40:19	141	\N	\N	\N	\N	2	4	PHRF	\N
722	38	11	2024-09-07 20:05:46	48	\N	\N	\N	\N	1	1	PHRF	\N
723	38	12	2024-09-07 20:31:02	72	\N	\N	\N	\N	1	2	PHRF	\N
724	38	22	2024-09-07 20:59:24	69	\N	\N	\N	\N	1	2	PHRF	\N
725	38	29	2024-09-07 21:00:25	99	\N	\N	\N	\N	2	3	PHRF	\N
726	38	15	2024-09-07 21:01:07	93	\N	\N	\N	\N	2	3	PHRF	\N
727	38	18	2024-09-07 21:04:06	86	\N	\N	\N	\N	2	3	PHRF	\N
728	38	19	2024-09-07 21:06:46	120	\N	\N	\N	\N	2	4	PHRF	\N
729	38	43	2024-09-07 21:11:02	93	\N	\N	\N	\N	2	3	PHRF	\N
730	38	41	2024-09-07 21:11:25	93	\N	\N	\N	\N	2	3	PHRF	\N
731	38	23	2024-09-07 21:14:10	144	\N	\N	\N	\N	2	4	PHRF	\N
732	38	36	2024-09-07 21:19:17	141	\N	\N	\N	\N	2	4	PHRF	\N
733	38	16	2024-09-07 21:42:02	141	\N	\N	\N	\N	2	4	PHRF	\N
736	46	11	2024-09-21 19:29:05	48	\N	\N	\N	\N	1	1	PHRF	\N
738	46	30	\N	126	\N	\N	\N	\N	2	4	PHRF	\N
739	46	29	2024-09-21 20:53:15	99	\N	\N	\N	\N	2	3	PHRF	\N
740	46	15	2024-09-21 20:56:04	93	\N	\N	\N	\N	2	3	PHRF	\N
741	46	12	2024-09-21 20:57:21	72	\N	\N	\N	\N	1	2	PHRF	\N
742	46	19	2024-09-21 20:57:34	120	\N	\N	\N	\N	2	4	PHRF	\N
743	46	13	2024-09-21 21:01:34	159	\N	\N	\N	\N	2	4	PHRF	\N
744	46	41	2024-09-21 21:11:53	93	\N	\N	\N	\N	2	3	PHRF	\N
745	46	22	2024-09-21 21:12:39	69	\N	\N	\N	\N	1	2	PHRF	\N
746	46	43	2024-09-21 21:12:50	93	\N	\N	\N	\N	2	3	PHRF	\N
747	46	23	2024-09-21 21:15:53	144	\N	\N	\N	\N	2	4	PHRF	\N
748	46	36	2024-09-21 21:16:02	141	\N	\N	\N	\N	2	4	PHRF	\N
749	46	14	2024-09-21 21:25:26	132	\N	\N	\N	\N	2	4	PHRF	\N
750	46	16	2024-09-21 21:47:26	141	\N	\N	\N	\N	2	4	PHRF	\N
737	46	33	\N	102	\N	\N	\N	\N	2	3	PHRF	\N
1593	88	19	2026-02-07 20:38:04	120	\N	\N	\N	TIME	2	17	PHRF	\N
1718	94	29	2026-05-07 03:17:30	99	\N	\N	\N	TIME	2	3	PHRF	\N
1773	96	106	2026-05-21 03:08:07	93	\N	\N	\N	TIME	2	3	PHRF	\N
1832	99	19	2026-06-04 03:48:41	120	\N	\N	\N	TIME	2	4	PHRF	\N
1892	101	5	2026-06-18 03:21:38	162	\N	\N	\N	TIME	38	68	PHRF	\N
2015	107	15	2026-07-09 03:17:20	93	\N	\N	\N	TIME	37	65	PHRF	\N
2073	109	11	2026-07-23 03:03:03	48	\N	\N	\N	TIME	37	64	PHRF	\N
2129	111	63	2026-08-06 03:03:47	18	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KI4
2184	112	12	2026-08-13 03:12:55	72	\N	\N	\N	TIME	37	65	PHRF	\N
2252	115	6	2026-08-22 19:27:59	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2334	117	18	2026-09-12 20:10:53	84	\N	\N	\N	TIME	37	66	PHRF	\N
2409	137	110	2026-10-01 01:23:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
1336	73	19	2025-07-31 03:17:54	120	\N	\N	\N	TIME	2	4	PHRF	\N
1355	74	12	2025-08-07 02:57:49	72	\N	\N	\N	TIME	1	2	PHRF	\N
760	40	11	2024-10-19 18:32:56	48	\N	\N	\N	\N	1	1	PHRF	\N
761	40	62	2024-10-19 18:34:35	63	\N	\N	\N	\N	1	2	PHRF	\N
762	40	12	2024-10-19 18:35:04	72	\N	\N	\N	\N	1	2	PHRF	\N
763	40	22	2024-10-19 18:37:44	69	\N	\N	\N	\N	1	2	PHRF	\N
765	40	15	2024-10-19 18:43:52	93	\N	\N	\N	\N	2	3	PHRF	\N
766	40	18	2024-10-19 18:46:59	86	\N	\N	\N	\N	2	3	PHRF	\N
767	40	19	2024-10-19 18:47:53	120	\N	\N	\N	\N	2	4	PHRF	\N
768	40	41	2024-10-19 18:56:38	93	\N	\N	\N	\N	2	3	PHRF	\N
769	40	5	2024-10-19 19:02:43	162	\N	\N	\N	\N	2	4	PHRF	\N
770	40	16	2024-10-19 19:04:42	141	\N	\N	\N	\N	2	4	PHRF	\N
771	40	36	2024-10-19 19:03:41	141	\N	\N	\N	\N	2	4	PHRF	\N
772	40	23	2024-10-19 19:06:24	144	\N	\N	\N	\N	2	4	PHRF	\N
776	41	11	2024-11-02 18:54:32	48	\N	\N	\N	\N	1	1	PHRF	\N
777	41	28	2024-11-02 18:55:09	39	\N	\N	\N	\N	1	1	PHRF	\N
778	41	12	2024-11-02 19:07:01	72	\N	\N	\N	\N	1	2	PHRF	\N
779	41	22	2024-11-02 19:08:06	69	\N	\N	\N	\N	1	2	PHRF	\N
780	41	19	2024-11-02 19:17:59	120	\N	\N	\N	\N	2	4	PHRF	\N
781	41	18	2024-11-02 19:18:23	86	\N	\N	\N	\N	2	3	PHRF	\N
782	41	62	2024-11-02 19:20:32	63	\N	\N	\N	\N	1	2	PHRF	\N
784	41	43	2024-11-02 19:21:05	93	\N	\N	\N	\N	2	3	PHRF	\N
786	41	36	2024-11-02 19:53:29	141	\N	\N	\N	\N	2	4	PHRF	\N
787	41	14	2024-11-02 19:55:03	132	\N	\N	\N	\N	2	4	PHRF	\N
788	41	16	2024-11-02 20:08:53	141	\N	\N	\N	\N	2	4	PHRF	\N
789	41	23	2024-11-02 20:29:45	144	\N	\N	\N	\N	2	4	PHRF	\N
792	48	63	2025-01-11 20:45:05	15	\N	\N	\N	\N	1	16	PHRF	\N
793	48	11	2025-01-11 20:51:26	48	\N	\N	\N	\N	1	16	PHRF	\N
794	48	19	2025-01-11 21:17:53	120	\N	\N	\N	\N	2	17	PHRF	\N
1337	73	29	2025-07-31 03:20:13	99	\N	\N	\N	TIME	2	3	PHRF	\N
796	48	15	2025-01-11 21:22:44	93	\N	\N	\N	\N	2	17	PHRF	\N
797	48	36	2025-01-11 22:00:33	141	\N	\N	\N	\N	2	17	PHRF	\N
798	48	16	2025-01-11 22:10:28	141	\N	\N	\N	\N	2	17	PHRF	\N
799	48	5	2025-01-11 22:11:21	162	\N	\N	\N	\N	2	17	PHRF	\N
801	49	11	2025-02-01 19:50:51	48	\N	\N	\N	\N	1	16	PHRF	\N
802	49	15	2025-02-01 20:04:17	93	\N	\N	\N	\N	2	17	PHRF	\N
803	49	14	2025-02-01 20:12:10	132	\N	\N	\N	\N	2	17	PHRF	\N
804	49	12	2025-02-01 19:59:18	72	\N	\N	\N	\N	1	16	PHRF	\N
805	49	43	2025-02-01 20:01:48	93	\N	\N	\N	\N	2	17	PHRF	\N
806	49	16	2025-02-01 20:17:21	141	\N	\N	\N	\N	2	17	PHRF	\N
807	49	5	2025-02-01 20:13:22	162	\N	\N	\N	\N	2	17	PHRF	\N
808	49	36	2025-02-01 20:14:01	141	\N	\N	\N	\N	2	17	PHRF	\N
1594	88	98	2026-02-07 20:41:57	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1642	92	28	2026-04-23 02:41:34	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1719	94	93	2026-05-07 03:18:42	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1774	96	98	2026-05-21 03:10:46	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1833	99	15	2026-06-04 03:50:49	93	\N	\N	\N	TIME	2	3	PHRF	\N
1893	101	39	2026-06-18 03:23:21	144	\N	\N	\N	TIME	38	68	PHRF	\N
1953	103	56	\N	\N	1	\N	\N	HOC	4	19	CruisingFlyingSails	\N
2016	107	22	2026-07-09 03:19:46	69	\N	\N	\N	TIME	37	64	PHRF	\N
2074	109	28	2026-07-23 03:04:10	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
2130	111	29	2026-08-06 03:04:09	99	\N	\N	\N	TIME	38	67	PHRF	\N
2185	112	41	2026-08-13 03:14:38	93	\N	\N	\N	TIME	37	65	PHRF	\N
2253	115	29	2026-08-22 19:32:32	99	\N	\N	\N	TIME	38	67	PHRF	\N
2335	117	12	2026-09-12 20:13:15	72	\N	\N	\N	TIME	37	66	PHRF	\N
2410	137	117	2026-10-01 01:24:00	200	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
809	50	63	2025-02-08 21:54:32	15	\N	\N	\N	\N	1	16	PHRF	\N
810	50	43	2025-02-08 22:35:13	93	\N	\N	\N	\N	2	17	PHRF	\N
811	50	36	2025-02-08 22:38:45	141	\N	\N	\N	\N	2	17	PHRF	\N
812	50	15	2025-02-08 22:45:04	93	\N	\N	\N	\N	2	17	PHRF	\N
813	50	11	\N	48	\N	\N	\N	\N	1	16	PHRF	\N
1347	73	39	\N	145	\N	\N	\N	DNF	2	4	PHRF	\N
1356	74	43	2025-08-07 03:01:40	93	\N	\N	\N	TIME	2	3	PHRF	\N
815	50	16	2025-02-08 23:24:00	141	\N	\N	\N	\N	2	17	PHRF	\N
816	50	5	2025-02-08 23:29:27	162	\N	\N	\N	\N	2	17	PHRF	\N
817	50	23	2025-02-08 23:16:14	144	\N	\N	\N	\N	2	17	PHRF	\N
822	40	63	2024-10-19 18:40:10	15	\N	\N	\N	\N	1	1	PHRF	\N
823	48	50	2025-01-11 21:20:20	63	\N	\N	\N	\N	1	16	PHRF	\N
824	52	28	2025-03-29 19:19:41	39	\N	\N	\N	\N	1	16	PHRF	\N
826	52	50	2025-03-29 19:23:44	63	\N	\N	\N	\N	1	16	PHRF	\N
827	52	36	2025-03-29 19:37:41	141	\N	\N	\N	\N	2	17	PHRF	\N
829	52	5	2025-03-29 19:41:00	162	\N	\N	\N	\N	2	17	PHRF	\N
830	52	16	2025-03-29 19:49:42	141	\N	\N	\N	\N	2	17	PHRF	\N
831	53	63	2025-04-10 02:43:25	12	\N	\N	\N	\N	1	1	PHRF	\N
832	53	27	2025-04-10 02:45:26	72	\N	\N	\N	\N	1	2	PHRF	\N
833	53	28	2025-04-10 02:46:09	42	\N	\N	\N	\N	1	1	PHRF	\N
834	53	22	2025-04-10 02:50:26	69	\N	\N	\N	\N	1	2	PHRF	\N
835	53	50	2025-04-10 03:01:09	63	\N	\N	\N	\N	1	2	PHRF	\N
836	53	39	2025-04-10 03:03:21	145	\N	\N	\N	\N	2	4	PHRF	\N
837	53	43	2025-04-10 03:03:31	93	\N	\N	\N	\N	2	3	PHRF	\N
838	53	36	2025-04-10 03:04:45	141	\N	\N	\N	\N	2	4	PHRF	\N
839	53	30	2025-04-10 03:05:03	126	\N	\N	\N	\N	2	4	PHRF	\N
840	53	90	2025-04-10 03:05:13	96	\N	\N	\N	\N	2	3	PHRF	\N
841	53	5	2025-04-10 03:07:10	162	\N	\N	\N	\N	2	4	PHRF	\N
843	53	31	2025-04-10 03:11:34	93	\N	\N	\N	\N	2	3	PHRF	\N
844	53	16	2025-04-10 03:12:45	141	\N	\N	\N	\N	2	4	PHRF	\N
846	53	1	2025-04-10 03:15:43	150	\N	\N	\N	\N	2	4	PHRF	\N
842	53	17	2025-04-10 03:08:22	135	\N	\N	\N	\N	2	4	PHRF	\N
851	53	29	2025-04-10 02:57:07	99	\N	\N	\N	\N	2	3	PHRF	\N
852	54	11	2025-04-17 03:05:58	48	\N	\N	\N	\N	1	1	PHRF	\N
853	54	27	2025-04-17 03:08:46	72	\N	\N	\N	\N	1	2	PHRF	\N
854	54	63	2025-04-17 03:08:57	12	\N	\N	\N	\N	1	1	PHRF	\N
855	54	28	2025-04-17 03:10:17	42	\N	\N	\N	\N	1	1	PHRF	\N
856	54	50	2025-04-17 03:15:02	63	\N	\N	\N	\N	1	2	PHRF	\N
857	54	22	2025-04-17 03:17:20	69	\N	\N	\N	\N	1	2	PHRF	\N
858	54	48	2025-04-17 03:19:56	99	\N	\N	\N	\N	2	3	PHRF	\N
860	54	43	2025-04-17 03:22:49	93	\N	\N	\N	\N	2	3	PHRF	\N
861	54	30	\N	126	\N	\N	\N	\N	2	4	PHRF	\N
1595	88	73	2026-02-07 20:43:24	75	\N	\N	\N	TIME	2	17	PHRF	\N
1643	92	11	2026-04-23 02:41:03	48	\N	\N	\N	TIME	1	1	PHRF	\N
1720	94	36	2026-05-07 03:18:51	141	\N	\N	\N	TIME	2	4	PHRF	\N
1775	96	6	2026-05-21 03:31:45	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1834	99	30	2026-06-04 03:53:03	126	\N	\N	\N	TIME	2	4	PHRF	\N
1894	101	1	2026-06-18 03:31:59	150	\N	\N	\N	TIME	38	68	PHRF	\N
1954	103	88	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2017	107	41	2026-07-09 03:19:49	93	\N	\N	\N	TIME	37	65	PHRF	\N
2075	109	27	2026-07-23 03:11:16	72	\N	\N	\N	TIME	37	65	PHRF	\N
2131	111	27	2026-08-06 03:04:14	72	\N	\N	\N	TIME	37	65	PHRF	\N
2186	112	18	2026-08-13 03:15:32	84	\N	\N	\N	TIME	37	65	PHRF	\N
2254	115	36	2026-08-22 19:41:28	141	\N	\N	\N	TIME	38	68	PHRF	\N
2336	117	29	2026-09-12 20:15:22	99	\N	\N	\N	TIME	38	69	PHRF	\N
2411	137	111	2026-10-01 01:25:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
862	54	31	\N	93	\N	\N	\N	\N	2	3	PHRF	\N
865	54	16	2025-04-17 03:38:15	141	\N	\N	\N	\N	2	4	PHRF	\N
866	54	36	2025-04-17 03:38:16	141	\N	\N	\N	\N	2	4	PHRF	\N
867	54	5	2025-04-17 03:39:06	162	\N	\N	\N	\N	2	4	PHRF	\N
870	54	15	2025-04-17 03:20:48	93	\N	\N	\N	\N	2	3	PHRF	\N
876	54	39	\N	145	\N	\N	\N	\N	2	4	PHRF	\N
863	54	73	2025-04-17 03:35:48	75	\N	\N	\N	\N	2	3	PHRF	\N
886	55	28	2025-04-24 02:56:28	42	\N	\N	\N	\N	1	1	PHRF	\N
887	55	11	2025-04-24 02:57:19	48	\N	\N	\N	\N	1	1	PHRF	\N
888	55	27	2025-04-24 02:59:02	72	\N	\N	\N	\N	1	2	PHRF	\N
889	55	63	2025-04-24 03:02:50	12	\N	\N	\N	\N	1	1	PHRF	\N
890	55	12	2025-04-24 03:05:21	72	\N	\N	\N	\N	1	2	PHRF	\N
891	55	22	2025-04-24 03:06:41	69	\N	\N	\N	\N	1	2	PHRF	\N
892	55	15	2025-04-24 03:11:50	93	\N	\N	\N	\N	2	3	PHRF	\N
1338	73	15	2025-07-31 03:21:14	93	\N	\N	\N	TIME	2	3	PHRF	\N
894	55	43	2025-04-24 03:20:23	93	\N	\N	\N	\N	2	3	PHRF	\N
895	55	90	2025-04-24 03:20:35	96	\N	\N	\N	\N	2	3	PHRF	\N
896	55	36	2025-04-24 03:21:16	141	\N	\N	\N	\N	2	4	PHRF	\N
897	55	16	2025-04-24 03:31:30	141	\N	\N	\N	\N	2	4	PHRF	\N
1348	73	17	\N	135	\N	\N	\N	DNF	2	4	PHRF	\N
1357	74	15	2025-08-07 03:02:44	93	\N	\N	\N	TIME	2	3	PHRF	\N
901	55	48	\N	99	\N	\N	\N	\N	2	3	PHRF	\N
902	55	50	\N	63	\N	\N	\N	\N	1	2	PHRF	\N
906	55	73	\N	75	\N	\N	\N	\N	2	3	PHRF	\N
907	56	63	2025-05-01 02:27:29	12	\N	\N	\N	TIME	1	1	PHRF	\N
908	56	27	2025-05-01 02:30:45	72	\N	\N	\N	TIME	1	2	PHRF	\N
909	56	11	2025-05-01 02:31:47	48	\N	\N	\N	TIME	1	1	PHRF	\N
910	56	28	2025-05-01 02:32:21	42	\N	\N	\N	TIME	1	1	PHRF	\N
911	56	22	2025-05-01 02:33:08	69	\N	\N	\N	TIME	1	2	PHRF	\N
913	56	50	2025-05-01 02:34:16	63	\N	\N	\N	TIME	1	2	PHRF	\N
914	56	12	2025-05-01 02:34:50	72	\N	\N	\N	TIME	1	2	PHRF	\N
915	56	95	2025-05-01 02:34:43	96	\N	\N	\N	TIME	2	3	PHRF	\N
916	56	29	2025-05-01 02:36:33	99	\N	\N	\N	TIME	2	3	PHRF	\N
917	56	19	2025-05-01 02:37:20	120	\N	\N	\N	TIME	2	4	PHRF	\N
918	56	90	2025-05-01 02:37:47	96	\N	\N	\N	TIME	2	3	PHRF	\N
919	56	43	2025-05-01 02:38:02	93	\N	\N	\N	TIME	2	3	PHRF	\N
920	56	15	2025-05-01 02:38:15	93	\N	\N	\N	TIME	2	3	PHRF	\N
921	56	36	2025-05-01 02:39:27	141	\N	\N	\N	TIME	2	4	PHRF	\N
922	56	5	2025-05-01 02:40:15	162	\N	\N	\N	TIME	2	4	PHRF	\N
924	56	31	2025-05-01 02:43:50	93	\N	\N	\N	TIME	2	3	PHRF	\N
925	56	39	2025-05-01 02:44:52	145	\N	\N	\N	TIME	2	4	PHRF	\N
1596	88	5	2026-02-07 20:44:29	162	\N	\N	\N	TIME	2	17	PHRF	\N
1644	92	27	2026-04-23 02:43:07	72	\N	\N	\N	TIME	1	2	PHRF	\N
1721	94	30	2026-05-07 03:20:18	126	\N	\N	\N	TIME	2	4	PHRF	\N
1776	96	99	2026-05-21 03:33:42	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1835	99	18	2026-06-04 03:54:54	81	\N	\N	\N	TIME	2	3	PHRF	\N
1895	101	16	2026-06-18 03:32:14	141	\N	\N	\N	TIME	38	68	PHRF	\N
1955	103	47	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2018	107	50	2026-07-09 03:20:21	63	\N	\N	\N	TIME	37	64	PHRF	\N
2076	109	41	2026-07-23 03:14:24	93	\N	\N	\N	TIME	37	65	PHRF	\N
2132	111	11	2026-08-06 03:04:28	48	\N	\N	\N	TIME	37	64	PHRF	\N
2187	112	15	2026-08-13 03:16:35	93	\N	\N	\N	TIME	37	65	PHRF	\N
2255	115	99	2026-08-22 19:43:44	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2337	117	41	2026-09-12 20:18:18	93	\N	\N	\N	TIME	37	66	PHRF	\N
927	56	73	2025-05-01 02:46:53	75	\N	\N	\N	TIME	2	3	PHRF	\N
928	56	16	2025-05-01 02:47:36	141	\N	\N	\N	TIME	2	4	PHRF	\N
929	56	1	2025-05-01 02:48:28	150	\N	\N	\N	TIME	2	4	PHRF	\N
923	56	17	2025-05-01 02:41:02	135	\N	\N	\N	TIME	2	4	PHRF	\N
900	55	31	\N	93	\N	\N	\N	NSC	2	3	PHRF	\N
898	55	1	\N	150	\N	\N	\N	DNF	2	4	PHRF	\N
899	55	39	\N	145	\N	\N	\N	NSC	2	4	PHRF	\N
905	55	17	\N	135	\N	\N	\N	NSC	2	4	PHRF	\N
904	55	5	\N	162	\N	\N	\N	NSC	2	4	PHRF	\N
903	55	30	\N	126	\N	\N	\N	NSC	2	4	PHRF	\N
912	56	18	2025-05-01 02:33:56	81	\N	\N	\N	TIME	2	3	PHRF	\N
893	55	18	2025-04-24 03:13:19	81	\N	\N	\N	TIME	2	3	PHRF	\N
859	54	18	2025-04-17 03:21:48	81	\N	\N	\N	TIME	2	3	PHRF	\N
938	57	28	2025-05-08 02:57:06	42	\N	\N	\N	TIME	1	1	PHRF	\N
939	57	11	2025-05-08 02:58:38	48	\N	\N	\N	TIME	1	1	PHRF	\N
940	57	27	2025-05-08 03:03:59	72	\N	\N	\N	TIME	1	2	PHRF	\N
941	57	22	2025-05-08 03:04:02	69	\N	\N	\N	TIME	1	2	PHRF	\N
942	57	12	2025-05-08 03:07:13	72	\N	\N	\N	TIME	1	2	PHRF	\N
943	57	15	2025-05-08 03:12:39	93	\N	\N	\N	TIME	2	3	PHRF	\N
944	57	19	2025-05-08 03:13:43	120	\N	\N	\N	TIME	2	4	PHRF	\N
945	57	43	2025-05-08 03:15:07	93	\N	\N	\N	TIME	2	3	PHRF	\N
946	57	95	2025-05-08 03:16:28	96	\N	\N	\N	TIME	2	3	PHRF	\N
947	57	1	2025-05-08 03:18:41	150	\N	\N	\N	TIME	2	4	PHRF	\N
948	57	36	2025-05-08 03:20:25	141	\N	\N	\N	TIME	2	4	PHRF	\N
950	57	5	2025-05-08 03:32:10	162	\N	\N	\N	TIME	2	4	PHRF	\N
951	57	17	2025-05-08 03:34:59	135	\N	\N	\N	TIME	2	4	PHRF	\N
952	57	16	2025-05-08 03:43:35	141	\N	\N	\N	TIME	2	4	PHRF	\N
953	57	73	\N	75	\N	\N	\N	RET	2	3	PHRF	\N
957	58	11	2025-05-15 02:57:08	48	\N	\N	\N	TIME	1	1	PHRF	\N
958	58	28	2025-05-15 02:58:53	42	\N	\N	\N	TIME	1	1	PHRF	\N
959	58	27	2025-05-15 03:00:05	72	\N	\N	\N	TIME	1	2	PHRF	\N
960	58	63	2025-05-15 03:00:32	12	\N	\N	\N	TIME	1	1	PHRF	\N
961	58	12	2025-05-15 03:01:36	72	\N	\N	\N	TIME	1	2	PHRF	\N
962	58	22	2025-05-15 03:05:16	69	\N	\N	\N	TIME	1	2	PHRF	\N
963	58	19	2025-05-15 03:09:49	120	\N	\N	\N	TIME	2	4	PHRF	\N
964	58	15	2025-05-15 03:11:33	93	\N	\N	\N	TIME	2	3	PHRF	\N
965	58	95	2025-05-15 03:11:34	96	\N	\N	\N	TIME	2	3	PHRF	\N
966	58	29	2025-05-15 03:13:34	99	\N	\N	\N	TIME	2	3	PHRF	\N
967	58	18	2025-05-15 03:12:30	81	\N	\N	\N	TIME	2	3	PHRF	\N
968	58	90	2025-05-15 03:19:01	96	\N	\N	\N	TIME	2	3	PHRF	\N
1597	88	36	2026-02-07 20:44:42	141	\N	\N	\N	TIME	2	17	PHRF	\N
1645	92	26	2026-04-23 02:43:40	33	\N	\N	\N	TIME	1	1	PHRF	\N
1722	94	73	2026-05-07 03:21:01	75	\N	\N	\N	TIME	2	3	PHRF	\N
1778	96	11	2026-05-21 02:46:20	48	\N	\N	\N	TIME	1	1	PHRF	\N
1836	99	36	2026-06-04 03:58:56	141	\N	\N	\N	TIME	2	4	PHRF	\N
1896	101	34	2026-06-18 03:32:43	153	\N	\N	\N	TIME	38	68	PHRF	\N
1957	103	21	\N	195	\N	\N	\N	RET	38	68	PHRF	\N
2019	107	29	2026-07-09 03:22:01	99	\N	\N	\N	TIME	38	67	PHRF	\N
2077	109	12	2026-07-23 03:15:52	72	\N	\N	\N	TIME	37	65	PHRF	\N
2133	111	53	2026-08-06 03:04:33	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2188	112	22	2026-08-13 03:18:31	69	\N	\N	\N	TIME	37	64	PHRF	\N
2256	115	94	2026-08-22 19:53:36.701	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2338	117	36	2026-09-12 20:20:22	141	\N	\N	\N	TIME	38	69	PHRF	\N
2413	116	21	2026-10-03 19:12:53	195	\N	\N	\N	TIME	36	63	CruisingNonFlyingSails	\N
969	58	36	2025-05-15 03:19:32	141	\N	\N	\N	TIME	2	4	PHRF	\N
970	58	5	2025-05-15 03:24:10	162	\N	\N	\N	TIME	2	4	PHRF	\N
971	58	1	2025-05-15 03:24:22	150	\N	\N	\N	TIME	2	4	PHRF	\N
972	58	16	2025-05-15 03:25:08	141	\N	\N	\N	TIME	2	4	PHRF	\N
974	58	17	\N	135	\N	\N	\N	NSC	2	4	PHRF	\N
982	59	28	2025-05-22 03:00:07	42	\N	\N	\N	TIME	1	1	PHRF	\N
983	59	63	2025-05-22 03:03:25	12	\N	\N	\N	TIME	1	1	PHRF	\N
984	59	11	2025-05-22 03:05:36	48	\N	\N	\N	TIME	1	1	PHRF	\N
985	59	27	2025-05-22 03:05:46	72	\N	\N	\N	TIME	1	2	PHRF	\N
986	59	22	2025-05-22 03:07:46	69	\N	\N	\N	TIME	1	2	PHRF	\N
989	59	12	2025-05-22 03:11:45	72	\N	\N	\N	TIME	1	2	PHRF	\N
990	59	19	2025-05-22 03:13:58	120	\N	\N	\N	TIME	2	4	PHRF	\N
994	59	15	2025-05-22 03:16:33	93	\N	\N	\N	TIME	2	3	PHRF	\N
995	59	43	2025-05-22 03:17:19	93	\N	\N	\N	TIME	2	3	PHRF	\N
996	59	18	2025-05-22 03:17:27	81	\N	\N	\N	TIME	2	3	PHRF	\N
999	59	36	2025-05-22 03:22:15	141	\N	\N	\N	TIME	2	4	PHRF	\N
1000	59	17	2025-05-22 03:22:45	135	\N	\N	\N	TIME	2	4	PHRF	\N
1002	59	90	2025-05-22 03:23:57	96	\N	\N	\N	TIME	2	3	PHRF	\N
1004	59	5	2025-05-22 03:29:03	162	\N	\N	\N	TIME	2	4	PHRF	\N
1005	59	30	2025-05-22 03:30:21	126	\N	\N	\N	TIME	2	4	PHRF	\N
1006	59	16	2025-05-22 03:31:22	141	\N	\N	\N	TIME	2	4	PHRF	\N
1008	59	1	2025-05-22 03:43:03	150	\N	\N	\N	TIME	2	4	PHRF	\N
1009	59	50	\N	63	\N	\N	\N	RET	1	2	PHRF	\N
1010	60	15	2025-05-29 02:51:32	93	\N	\N	\N	TIME	2	3	PHRF	\N
1011	60	19	2025-05-29 02:53:39	120	\N	\N	\N	TIME	2	4	PHRF	\N
1012	60	29	2025-05-29 02:54:16	99	\N	\N	\N	TIME	2	3	PHRF	\N
1013	60	43	2025-05-29 02:54:18	93	\N	\N	\N	TIME	2	3	PHRF	\N
1014	60	95	2025-05-29 02:57:15	96	\N	\N	\N	TIME	2	3	PHRF	\N
1015	60	18	2025-05-29 02:57:38	81	\N	\N	\N	TIME	2	3	PHRF	\N
1016	60	36	2025-05-29 02:57:48	141	\N	\N	\N	TIME	2	4	PHRF	\N
1017	60	30	2025-05-29 02:59:12	126	\N	\N	\N	TIME	2	4	PHRF	\N
1018	60	17	2025-05-29 02:58:28	135	\N	\N	\N	TIME	2	4	PHRF	\N
1019	60	90	2025-05-29 03:01:42	96	\N	\N	\N	TIME	2	3	PHRF	\N
1021	60	35	2025-05-29 03:15:25	141	\N	\N	\N	TIME	2	4	PHRF	\N
1028	60	63	2025-05-29 03:27:21	12	\N	\N	\N	TIME	1	1	PHRF	\N
1029	60	11	2025-05-29 03:27:45	48	\N	\N	\N	TIME	1	1	PHRF	\N
1030	60	22	2025-05-29 03:33:05	69	\N	\N	\N	TIME	1	2	PHRF	\N
1031	60	27	2025-05-29 03:33:52	72	\N	\N	\N	TIME	1	2	PHRF	\N
1032	60	12	2025-05-29 03:38:15	72	\N	\N	\N	TIME	1	2	PHRF	\N
1033	60	50	2025-05-29 03:41:29	63	\N	\N	\N	TIME	1	2	PHRF	\N
1598	88	13	2026-02-07 20:53:35	159	\N	\N	\N	TIME	2	17	PHRF	\N
1646	92	12	2026-04-23 02:48:07	72	\N	\N	\N	TIME	1	2	PHRF	\N
1723	94	39	2026-05-07 03:28:11	144	\N	\N	\N	TIME	2	4	PHRF	\N
1779	96	35	\N	141	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1837	99	16	2026-06-04 04:04:37	141	\N	\N	\N	TIME	2	4	PHRF	\N
1897	101	106	2026-06-18 03:36:18	93	\N	\N	\N	TIME	37	65	PHRF	\N
1958	103	15	2026-06-27 22:05:25	93	\N	\N	\N	TIME	37	65	PHRF	\N
2020	107	6	2026-07-09 03:22:22	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2134	111	1	2026-08-06 03:06:36	150	\N	\N	\N	TIME	38	68	PHRF	\N
2189	112	6	2026-08-13 03:20:19	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2078	109	29	2026-07-23 03:17:46	99	\N	\N	\N	TIME	38	67	PHRF	\N
2257	115	5	2026-08-22 19:58:41	162	\N	\N	\N	TIME	38	68	PHRF	\N
2339	117	73	2026-09-12 20:29:39	75	\N	\N	\N	TIME	37	66	PHRF	\N
2415	116	5	2026-10-03 19:16:05	162	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1035	60	48	\N	99	\N	\N	\N	RET	2	3	PHRF	\N
1339	73	18	2025-07-31 03:24:14	81	\N	\N	\N	TIME	2	3	PHRF	\N
1349	73	16	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1358	74	29	2025-08-07 03:05:23	99	\N	\N	\N	TIME	2	3	PHRF	\N
1040	61	63	2025-06-05 02:35:45	12	\N	\N	\N	TIME	1	1	PHRF	\N
1041	61	11	2025-06-05 02:39:08	48	\N	\N	\N	TIME	1	1	PHRF	\N
1042	61	50	2025-06-05 02:49:35	63	\N	\N	\N	TIME	1	2	PHRF	\N
1043	61	27	2025-06-05 02:50:21	72	\N	\N	\N	TIME	1	2	PHRF	\N
1044	61	22	2025-06-05 03:05:43	69	\N	\N	\N	TIME	1	2	PHRF	\N
1045	61	19	2025-06-05 03:09:29	120	\N	\N	\N	TIME	2	4	PHRF	\N
1046	61	12	2025-06-05 03:09:54	72	\N	\N	\N	TIME	1	2	PHRF	\N
1047	61	18	2025-06-05 03:09:43	81	\N	\N	\N	TIME	2	3	PHRF	\N
1048	61	15	2025-06-05 03:11:19	93	\N	\N	\N	TIME	2	3	PHRF	\N
1049	61	29	2025-06-05 03:12:07	99	\N	\N	\N	TIME	2	3	PHRF	\N
1050	61	90	2025-06-05 03:12:37	96	\N	\N	\N	TIME	2	3	PHRF	\N
1051	61	95	2025-06-05 03:12:56	96	\N	\N	\N	TIME	2	3	PHRF	\N
1052	61	30	2025-06-05 03:14:11	126	\N	\N	\N	TIME	2	4	PHRF	\N
1053	61	36	2025-06-05 03:21:45	141	\N	\N	\N	TIME	2	4	PHRF	\N
1054	61	43	2025-06-05 03:23:11	93	\N	\N	\N	TIME	2	3	PHRF	\N
1364	74	30	2025-08-07 03:09:50	126	\N	\N	\N	TIME	2	4	PHRF	\N
1056	61	39	\N	145	\N	\N	\N	RET	2	4	PHRF	\N
1058	61	17	\N	135	\N	\N	\N	RET	2	4	PHRF	\N
1059	61	35	\N	141	\N	\N	\N	RET	2	4	PHRF	\N
1060	61	16	\N	141	\N	\N	\N	RET	2	4	PHRF	\N
1069	62	11	2025-06-12 03:04:37	48	\N	\N	\N	TIME	1	1	PHRF	\N
1070	62	63	2025-06-12 03:05:03	12	\N	\N	\N	TIME	1	1	PHRF	\N
1071	62	27	2025-06-12 03:06:59	72	\N	\N	\N	TIME	1	2	PHRF	\N
1072	62	50	2025-06-12 03:08:44	63	\N	\N	\N	TIME	1	2	PHRF	\N
1073	62	12	2025-06-12 03:10:47	72	\N	\N	\N	TIME	1	2	PHRF	\N
1074	62	22	2025-06-12 03:11:27	69	\N	\N	\N	TIME	1	2	PHRF	\N
1075	62	15	2025-06-12 03:15:00	93	\N	\N	\N	TIME	2	3	PHRF	\N
1076	62	18	2025-06-12 03:16:32	81	\N	\N	\N	TIME	2	3	PHRF	\N
1077	62	19	2025-06-12 03:16:48	120	\N	\N	\N	TIME	2	4	PHRF	\N
1078	62	29	2025-06-12 03:17:08	99	\N	\N	\N	TIME	2	3	PHRF	\N
1080	62	90	2025-06-12 03:19:52	96	\N	\N	\N	TIME	2	3	PHRF	\N
1079	62	30	2025-06-12 03:25:02	126	\N	\N	\N	TIME	2	4	PHRF	\N
1081	62	36	2025-06-12 03:27:35	141	\N	\N	\N	TIME	2	4	PHRF	\N
1084	62	17	2025-06-12 03:35:58	135	\N	\N	\N	TIME	2	4	PHRF	\N
1085	62	39	2025-06-12 03:37:04	145	\N	\N	\N	TIME	2	4	PHRF	\N
1086	62	1	2025-06-12 03:37:43	150	\N	\N	\N	TIME	2	4	PHRF	\N
1599	88	16	2026-02-07 20:56:06	141	\N	\N	\N	TIME	2	17	PHRF	\N
1647	92	22	2026-04-23 02:49:37	69	\N	\N	\N	TIME	1	2	PHRF	\N
1724	94	90	2026-05-07 03:29:09	96	\N	\N	\N	TIME	2	3	PHRF	\N
1780	96	88	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1838	99	90	2026-06-04 04:05:06	96	\N	\N	\N	TIME	2	3	PHRF	\N
1959	103	28	2026-06-27 22:07:44	42	\N	\N	\N	TIME	37	64	ORC_PHRF	04560004KJP
2021	107	56	2026-07-09 03:23:40	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2135	111	12	2026-08-06 03:06:12	72	\N	\N	\N	TIME	37	65	PHRF	\N
2190	112	99	2026-08-13 03:21:35	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1899	101	21	2026-06-18 03:45:13	195	\N	\N	\N	TIME	38	68	PHRF	\N
2079	109	19	2026-07-23 03:21:33	120	\N	\N	\N	TIME	38	67	PHRF	\N
2258	115	16	2026-08-22 20:02:20	141	\N	\N	\N	TIME	38	68	PHRF	\N
2340	117	16	2026-09-12 20:55:00	141	\N	\N	\N	TIME	38	69	PHRF	\N
1087	62	16	2025-06-12 03:49:43	141	\N	\N	\N	TIME	2	4	PHRF	\N
1089	62	35	2025-06-12 03:56:18	141	\N	\N	\N	TIME	2	4	PHRF	\N
1102	64	30	2025-06-19 02:53:30	126	\N	\N	\N	TIME	2	4	PHRF	\N
1103	64	36	2025-06-19 02:57:49	141	\N	\N	\N	TIME	2	4	PHRF	\N
1105	64	39	2025-06-19 03:03:41	145	\N	\N	\N	TIME	2	4	PHRF	\N
1107	64	16	2025-06-19 03:04:36	141	\N	\N	\N	TIME	2	4	PHRF	\N
1108	64	17	2025-06-19 03:05:26	135	\N	\N	\N	TIME	2	4	PHRF	\N
1109	64	1	2025-06-19 03:08:43	150	\N	\N	\N	TIME	2	4	PHRF	\N
1111	64	35	2025-06-19 03:18:06	141	\N	\N	\N	TIME	2	4	PHRF	\N
1113	64	11	2025-06-19 03:20:57	48	\N	\N	\N	TIME	1	1	PHRF	\N
1116	64	28	2025-06-19 03:27:00	42	\N	\N	\N	TIME	1	1	PHRF	\N
1117	64	22	2025-06-19 03:29:00	69	\N	\N	\N	TIME	1	2	PHRF	\N
1118	64	12	2025-06-19 03:29:18	72	\N	\N	\N	TIME	1	2	PHRF	\N
1119	64	27	2025-06-19 03:29:36	72	\N	\N	\N	TIME	1	2	PHRF	\N
1124	65	12	2025-06-21 21:39:29	72	\N	\N	\N	TIME	1	2	PHRF	\N
1125	65	18	2025-06-21 21:43:35	81	\N	\N	\N	TIME	2	3	PHRF	\N
1126	65	15	2025-06-21 21:43:44	93	\N	\N	\N	TIME	2	3	PHRF	\N
1127	65	29	2025-06-21 21:46:06	99	\N	\N	\N	TIME	2	3	PHRF	\N
1128	65	36	2025-06-21 21:57:24	141	\N	\N	\N	TIME	2	4	PHRF	\N
1129	65	16	2025-06-21 22:30:58	141	\N	\N	\N	TIME	2	4	PHRF	\N
1359	74	18	2025-08-07 03:05:59	81	\N	\N	\N	TIME	2	3	PHRF	\N
1369	74	16	2025-08-07 03:28:34	141	\N	\N	\N	TIME	2	4	PHRF	\N
1163	66	39	2025-06-26 03:38:22	145	\N	\N	\N	TIME	2	4	PHRF	\N
1164	66	17	2025-06-26 03:49:05	135	\N	\N	\N	TIME	2	4	PHRF	\N
1166	66	16	2025-06-26 03:37:46	141	\N	\N	\N	TIME	2	4	PHRF	\N
1167	66	90	2025-06-26 03:28:19	96	\N	\N	\N	TIME	2	3	PHRF	\N
1168	66	12	2025-06-26 03:14:32	72	\N	\N	\N	TIME	1	2	PHRF	\N
1169	66	15	2025-06-26 03:23:39	93	\N	\N	\N	TIME	2	3	PHRF	\N
1170	66	27	2025-06-26 03:10:58	72	\N	\N	\N	TIME	1	2	PHRF	\N
1171	66	35	2025-06-26 03:43:50	141	\N	\N	\N	TIME	2	4	PHRF	\N
1172	66	11	2025-06-26 03:07:27	48	\N	\N	\N	TIME	1	1	PHRF	\N
1173	66	43	2025-06-26 03:21:21	93	\N	\N	\N	TIME	2	3	PHRF	\N
1175	66	28	2025-06-26 03:10:34	42	\N	\N	\N	TIME	1	1	PHRF	\N
1176	66	36	2025-06-26 03:28:52	141	\N	\N	\N	TIME	2	4	PHRF	\N
1178	66	1	2025-06-26 03:41:26	150	\N	\N	\N	TIME	2	4	PHRF	\N
1179	66	30	2025-06-26 03:30:15	126	\N	\N	\N	TIME	2	4	PHRF	\N
1180	66	19	2025-06-26 03:21:29	120	\N	\N	\N	TIME	2	4	PHRF	\N
1181	66	48	2025-06-26 03:20:45	99	\N	\N	\N	TIME	2	3	PHRF	\N
1182	66	18	2025-06-26 03:22:04	81	\N	\N	\N	TIME	2	3	PHRF	\N
1600	88	99	2026-02-07 21:14:38	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1648	92	15	2026-04-23 02:51:58	93	\N	\N	\N	TIME	2	3	PHRF	\N
1725	94	88	2026-05-07 03:29:24	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1781	96	94	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1839	99	1	2026-06-04 04:06:50	150	\N	\N	\N	TIME	2	4	PHRF	\N
1900	101	94	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1960	103	18	2026-06-27 22:21:03	84	\N	\N	\N	TIME	37	65	PHRF	\N
2022	107	30	2026-07-09 03:32:52	126	\N	\N	\N	TIME	38	67	PHRF	\N
2136	111	93	2026-08-06 03:07:10	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2191	112	73	2026-08-13 03:29:22	75	\N	\N	\N	TIME	37	65	PHRF	\N
2080	109	30	2026-07-23 03:22:05	126	\N	\N	\N	TIME	38	67	PHRF	\N
2259	115	21	2026-08-22 20:23:23	195	\N	\N	\N	TIME	38	68	PHRF	\N
2341	117	28	\N	42	\N	\N	\N	RET	37	66	ORC_PHRF	04560004KJP
2417	116	16	2026-10-03 19:22:52	141	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1183	66	29	2025-06-26 03:22:12	99	\N	\N	\N	TIME	2	3	PHRF	\N
1184	67	50	2025-06-29 19:53:40	63	\N	\N	\N	TIME	1	16	PHRF	\N
1185	67	36	2025-06-29 19:54:12	141	\N	\N	\N	TIME	2	17	PHRF	\N
1189	67	16	2025-06-29 19:59:51	141	\N	\N	\N	TIME	2	17	PHRF	\N
1192	68	11	2025-07-03 02:59:51	48	\N	\N	\N	TIME	1	1	PHRF	\N
1194	68	63	2025-07-03 03:03:54	18	\N	\N	\N	TIME	1	1	PHRF	\N
1195	68	27	2025-07-03 03:04:43	72	\N	\N	\N	TIME	1	2	PHRF	\N
1196	68	12	2025-07-03 03:07:36	72	\N	\N	\N	TIME	1	2	PHRF	\N
1198	68	28	2025-07-03 03:08:37	42	\N	\N	\N	TIME	1	1	PHRF	\N
1200	68	50	2025-07-03 03:15:07	63	\N	\N	\N	TIME	1	2	PHRF	\N
1201	68	48	2025-07-03 03:15:58	99	\N	\N	\N	TIME	2	3	PHRF	\N
1202	68	15	2025-07-03 03:16:46	93	\N	\N	\N	TIME	2	3	PHRF	\N
1203	68	19	2025-07-03 03:18:55	120	\N	\N	\N	TIME	2	4	PHRF	\N
1204	68	29	2025-07-03 03:20:00	99	\N	\N	\N	TIME	2	3	PHRF	\N
1205	68	90	2025-07-03 03:20:47	96	\N	\N	\N	TIME	2	3	PHRF	\N
1206	68	18	2025-07-03 03:21:37	81	\N	\N	\N	TIME	2	3	PHRF	\N
1207	68	43	2025-07-03 03:22:19	93	\N	\N	\N	TIME	2	3	PHRF	\N
1208	68	30	2025-07-03 03:25:30	126	\N	\N	\N	TIME	2	4	PHRF	\N
1209	68	36	2025-07-03 03:28:08	141	\N	\N	\N	TIME	2	4	PHRF	\N
1212	68	17	2025-07-03 03:39:48	135	\N	\N	\N	TIME	2	4	PHRF	\N
1213	68	1	2025-07-03 03:41:38	150	\N	\N	\N	TIME	2	4	PHRF	\N
1215	68	35	2025-07-03 03:44:41	141	\N	\N	\N	TIME	2	4	PHRF	\N
1227	69	63	2025-07-10 03:32:00	18	\N	\N	\N	TIME	1	1	PHRF	\N
1228	69	11	2025-07-10 03:32:33	48	\N	\N	\N	TIME	1	1	PHRF	\N
1229	69	28	2025-07-10 03:32:51	42	\N	\N	\N	TIME	1	1	PHRF	\N
1231	69	27	2025-07-10 03:39:26	72	\N	\N	\N	TIME	1	2	PHRF	\N
1232	69	12	2025-07-10 03:46:56	72	\N	\N	\N	TIME	1	2	PHRF	\N
1233	69	15	2025-07-10 03:47:26	93	\N	\N	\N	TIME	2	3	PHRF	\N
1234	69	48	2025-07-10 03:47:39	99	\N	\N	\N	TIME	2	3	PHRF	\N
1235	69	19	2025-07-10 03:48:42	120	\N	\N	\N	TIME	2	4	PHRF	\N
1236	69	18	2025-07-10 03:49:14	81	\N	\N	\N	TIME	2	3	PHRF	\N
1237	69	29	2025-07-10 03:50:56	99	\N	\N	\N	TIME	2	3	PHRF	\N
1238	69	43	2025-07-10 03:51:21	93	\N	\N	\N	TIME	2	3	PHRF	\N
1240	69	36	2025-07-10 03:57:47	141	\N	\N	\N	TIME	2	4	PHRF	\N
1241	69	90	2025-07-10 03:58:48	96	\N	\N	\N	TIME	2	3	PHRF	\N
1341	73	30	2025-07-31 03:26:52	126	\N	\N	\N	TIME	2	4	PHRF	\N
1243	69	1	\N	150	\N	\N	\N	DNF	2	4	PHRF	\N
1244	69	16	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1245	69	30	\N	126	\N	\N	\N	RET	2	4	PHRF	\N
1602	87	50	\N	63	\N	\N	\N	RET	1	16	PHRF	\N
1649	92	18	2026-04-23 02:53:18	81	\N	\N	\N	TIME	2	3	PHRF	\N
1726	94	1	2026-05-07 03:32:27	150	\N	\N	\N	TIME	2	4	PHRF	\N
1782	96	93	\N	\N	3	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1840	99	106	2026-06-04 04:09:25	93	\N	\N	\N	TIME	2	3	PHRF	\N
1901	101	6	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
1961	103	41	2026-06-27 22:22:00	93	\N	\N	\N	TIME	37	65	PHRF	\N
2023	107	36	2026-07-09 03:39:04	141	\N	\N	\N	TIME	38	68	PHRF	\N
2137	111	73	2026-08-06 03:07:49	75	\N	\N	\N	TIME	37	65	PHRF	\N
2192	112	88	2026-08-13 03:33:58	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2081	109	16	2026-07-23 03:25:40	141	\N	\N	\N	TIME	38	68	PHRF	\N
2260	123	110	2026-08-27 01:01:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2342	117	94	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2418	116	56	\N	\N	\N	\N	\N	RET	35	62	CruisingNonFlyingSails	\N
1246	69	17	\N	135	\N	\N	\N	RET	2	4	PHRF	\N
1247	69	50	\N	63	\N	\N	\N	DNF	1	2	PHRF	\N
1242	69	31	2025-07-10 03:59:47	93	\N	\N	\N	TIME	2	3	PHRF	\N
1248	70	63	2025-07-12 21:01:26	18	\N	\N	\N	TIME	1	1	PHRF	\N
1249	70	28	2025-07-12 21:02:53	42	\N	\N	\N	TIME	1	1	PHRF	\N
1250	70	15	2025-07-12 21:09:09	93	\N	\N	\N	TIME	2	3	PHRF	\N
1251	70	29	2025-07-12 21:09:21	99	\N	\N	\N	TIME	2	3	PHRF	\N
1252	70	18	2025-07-12 21:11:05	81	\N	\N	\N	TIME	2	3	PHRF	\N
1253	70	12	2025-07-12 21:12:25	72	\N	\N	\N	TIME	1	2	PHRF	\N
1254	70	52	2025-07-12 21:12:57	105	\N	\N	\N	TIME	2	3	PHRF	\N
1255	70	36	2025-07-12 21:30:58	141	\N	\N	\N	TIME	2	4	PHRF	\N
1257	70	13	\N	159	\N	\N	\N	RET	2	4	PHRF	\N
1258	70	16	2025-07-12 21:45:16	141	\N	\N	\N	TIME	2	4	PHRF	\N
1275	71	11	2025-07-17 03:23:37	48	\N	\N	\N	TIME	1	1	PHRF	\N
1276	71	28	2025-07-17 03:23:45	42	\N	\N	\N	TIME	1	1	PHRF	\N
1277	71	27	2025-07-17 03:26:46	72	\N	\N	\N	TIME	1	2	PHRF	\N
1278	71	12	2025-07-17 03:30:16	72	\N	\N	\N	TIME	1	2	PHRF	\N
1279	71	22	2025-07-17 03:31:45	69	\N	\N	\N	TIME	1	2	PHRF	\N
1280	71	15	2025-07-17 03:35:23	93	\N	\N	\N	TIME	2	3	PHRF	\N
1281	71	19	2025-07-17 03:36:45	120	\N	\N	\N	TIME	2	4	PHRF	\N
1282	71	29	2025-07-17 03:36:54	99	\N	\N	\N	TIME	2	3	PHRF	\N
1283	71	48	2025-07-17 03:38:47	99	\N	\N	\N	TIME	2	3	PHRF	\N
1284	71	43	2025-07-17 03:38:51	93	\N	\N	\N	TIME	2	3	PHRF	\N
1285	71	18	2025-07-17 03:42:05	81	\N	\N	\N	TIME	2	3	PHRF	\N
1286	71	30	2025-07-17 03:47:30	126	\N	\N	\N	TIME	2	4	PHRF	\N
1287	71	36	2025-07-17 03:49:15	141	\N	\N	\N	TIME	2	4	PHRF	\N
1288	71	39	2025-07-17 03:54:24	145	\N	\N	\N	TIME	2	4	PHRF	\N
1289	71	31	2025-07-17 04:04:25	93	\N	\N	\N	TIME	2	3	PHRF	\N
1290	71	16	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1291	71	90	\N	96	\N	\N	\N	DNF	2	3	PHRF	\N
1292	71	1	\N	150	\N	\N	\N	DNF	2	4	PHRF	\N
1293	71	17	\N	135	\N	\N	\N	DNF	2	4	PHRF	\N
1294	71	35	\N	141	\N	\N	\N	DNF	2	4	PHRF	\N
1296	72	27	2025-07-24 02:53:39	72	\N	\N	\N	TIME	1	2	PHRF	\N
1297	72	11	2025-07-24 02:57:22	48	\N	\N	\N	TIME	1	1	PHRF	\N
1298	72	29	2025-07-24 03:01:31	99	\N	\N	\N	TIME	2	3	PHRF	\N
1299	72	15	2025-07-24 03:01:50	93	\N	\N	\N	TIME	2	3	PHRF	\N
1300	72	26	2025-07-24 03:02:13	33	\N	\N	\N	TIME	1	1	PHRF	\N
1301	72	90	2025-07-24 03:02:44	96	\N	\N	\N	TIME	2	3	PHRF	\N
1302	72	19	2025-07-24 03:03:59	120	\N	\N	\N	TIME	2	4	PHRF	\N
1650	92	29	2026-04-23 02:53:31	99	\N	\N	\N	TIME	2	3	PHRF	\N
1727	94	16	2026-05-07 03:34:48	141	\N	\N	\N	TIME	2	4	PHRF	\N
1783	97	63	2026-05-28 03:10:45	18	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KI4
1902	101	31	2026-06-18 03:55:03	93	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1962	103	29	2026-06-27 22:23:20	99	\N	\N	\N	TIME	38	67	PHRF	\N
1841	99	34	2026-06-04 04:13:30	153	\N	\N	\N	TIME	2	4	PHRF	\N
2024	107	5	2026-07-09 03:42:27	162	\N	\N	\N	TIME	38	68	PHRF	\N
2138	111	31	2026-08-06 03:08:54	93	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2193	112	56	2026-08-13 03:38:46	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2082	109	5	2026-07-23 03:25:58	162	\N	\N	\N	TIME	38	68	PHRF	\N
2261	123	114	2026-08-27 01:02:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2343	117	93	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2419	116	99	2026-10-03 19:23:19	200	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1303	72	31	2025-07-24 03:04:10	93	\N	\N	\N	TIME	2	3	PHRF	\N
1305	72	12	2025-07-24 03:04:23	72	\N	\N	\N	TIME	1	2	PHRF	\N
1307	72	39	2025-07-24 03:05:23	145	\N	\N	\N	TIME	2	4	PHRF	\N
1386	76	63	2025-08-14 02:49:46	18	\N	\N	\N	TIME	1	1	PHRF	\N
1387	76	27	2025-08-14 02:53:06	72	\N	\N	\N	TIME	1	2	PHRF	\N
1388	76	22	2025-08-14 02:55:34	69	\N	\N	\N	TIME	1	2	PHRF	\N
1389	76	12	2025-08-14 02:57:54	72	\N	\N	\N	TIME	1	2	PHRF	\N
1390	76	43	2025-08-14 03:01:00	93	\N	\N	\N	TIME	2	3	PHRF	\N
1391	76	15	2025-08-14 03:05:43	93	\N	\N	\N	TIME	2	3	PHRF	\N
1392	76	29	2025-08-14 03:05:58	99	\N	\N	\N	TIME	2	3	PHRF	\N
1393	76	90	2025-08-14 03:06:58	96	\N	\N	\N	TIME	2	3	PHRF	\N
1394	76	18	2025-08-14 03:07:30	81	\N	\N	\N	TIME	2	3	PHRF	\N
1395	76	30	2025-08-14 03:08:56	126	\N	\N	\N	TIME	2	4	PHRF	\N
1397	76	36	2025-08-14 03:10:03	141	\N	\N	\N	TIME	2	4	PHRF	\N
1400	76	5	2025-08-14 03:14:26	162	\N	\N	\N	TIME	2	4	PHRF	\N
1402	76	39	2025-08-14 03:19:05	145	\N	\N	\N	TIME	2	4	PHRF	\N
1404	76	16	2025-08-14 03:27:06	141	\N	\N	\N	TIME	2	4	PHRF	\N
1409	76	35	2025-08-14 03:34:54	141	\N	\N	\N	TIME	2	4	PHRF	\N
1410	76	21	2025-08-14 03:34:59	195	\N	\N	\N	TIME	2	4	PHRF	\N
1424	78	63	2025-08-21 02:50:29	18	\N	\N	\N	TIME	1	1	PHRF	\N
1425	78	11	2025-08-21 02:50:39	48	\N	\N	\N	TIME	1	1	PHRF	\N
1429	78	27	2025-08-21 02:54:23	72	\N	\N	\N	TIME	1	2	PHRF	\N
1430	78	22	2025-08-21 02:56:23	69	\N	\N	\N	TIME	1	2	PHRF	\N
1437	78	19	2025-08-21 03:03:06	120	\N	\N	\N	TIME	2	4	PHRF	\N
1438	78	43	2025-08-21 03:03:45	93	\N	\N	\N	TIME	2	3	PHRF	\N
1439	78	30	2025-08-21 03:05:05	126	\N	\N	\N	TIME	2	4	PHRF	\N
1440	78	36	2025-08-21 03:07:09	141	\N	\N	\N	TIME	2	4	PHRF	\N
1446	78	35	\N	141	\N	\N	\N	RET	2	4	PHRF	\N
1447	78	90	2025-08-21 03:09:54	96	\N	\N	\N	TIME	2	3	PHRF	\N
1448	78	39	2025-08-21 03:12:06	145	\N	\N	\N	TIME	2	4	PHRF	\N
1431	78	12	2025-08-21 02:57:32	72	\N	\N	\N	TIME	1	2	PHRF	\N
1433	78	18	2025-08-21 03:02:13	81	\N	\N	\N	TIME	2	3	PHRF	\N
1434	78	31	2025-08-21 03:02:23	93	\N	\N	\N	TIME	2	3	PHRF	\N
1435	78	29	2025-08-21 03:02:35	99	\N	\N	\N	TIME	2	3	PHRF	\N
1450	78	5	2025-08-21 03:15:10	162	\N	\N	\N	TIME	2	4	PHRF	\N
1452	78	16	2025-08-21 03:23:33	141	\N	\N	\N	TIME	2	4	PHRF	\N
1453	78	21	2025-08-21 03:26:51	195	\N	\N	\N	TIME	2	4	PHRF	\N
1520	84	11	2025-10-18 19:04:05	48	\N	\N	\N	TIME	1	1	PHRF	\N
1521	84	12	2025-10-18 19:08:58	72	\N	\N	\N	TIME	1	2	PHRF	\N
1651	92	30	2026-04-23 02:57:05	126	\N	\N	\N	TIME	2	4	PHRF	\N
1728	94	5	\N	162	\N	\N	\N	DNF	2	4	PHRF	\N
1784	97	28	2026-05-28 03:12:55	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1842	99	21	\N	195	\N	\N	\N	DNF	2	4	PHRF	\N
1903	101	99	2026-06-18 03:58:14	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1963	103	12	2026-06-27 22:28:56	72	\N	\N	\N	TIME	37	65	PHRF	\N
2025	107	1	2026-07-09 03:44:26	150	\N	\N	\N	TIME	38	68	PHRF	\N
2083	109	53	2026-07-23 03:26:50	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
2139	111	16	2026-08-06 03:08:52	141	\N	\N	\N	TIME	38	68	PHRF	\N
2206	114	104	2026-08-15 23:54:22	93	\N	\N	\N	TIME	9	31	PHRF	\N
2262	123	115	2026-08-27 01:03:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2357	118	11	2026-09-19 19:13:38	48	\N	\N	\N	TIME	37	66	PHRF	\N
2420	116	105	2026-10-03 19:32:00	200	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1522	84	15	2025-10-18 19:13:19	93	\N	\N	\N	TIME	2	3	PHRF	\N
1458	79	63	2025-08-23 19:31:38	18	\N	\N	\N	TIME	1	1	PHRF	\N
1460	79	11	2025-08-23 19:37:30	48	\N	\N	\N	TIME	1	1	PHRF	\N
1461	79	27	2025-08-23 19:39:29	72	\N	\N	\N	TIME	1	2	PHRF	\N
1463	79	29	2025-08-23 19:41:17	99	\N	\N	\N	TIME	2	3	PHRF	\N
1464	79	18	2025-08-23 19:42:48	81	\N	\N	\N	TIME	2	3	PHRF	\N
1466	79	90	2025-08-23 19:52:11	96	\N	\N	\N	TIME	2	3	PHRF	\N
1467	79	36	2025-08-23 19:52:40	141	\N	\N	\N	TIME	2	4	PHRF	\N
1468	79	5	2025-08-23 19:56:05	162	\N	\N	\N	TIME	2	4	PHRF	\N
1469	79	43	\N	93	\N	\N	\N	NSC	2	3	PHRF	\N
1470	79	16	2025-08-23 20:19:58	141	\N	\N	\N	TIME	2	4	PHRF	\N
1523	84	19	2025-10-18 19:29:13	120	\N	\N	\N	TIME	2	4	PHRF	\N
1524	84	22	2025-10-18 19:30:48	69	\N	\N	\N	TIME	1	2	PHRF	\N
1525	84	30	2025-10-18 19:38:06	126	\N	\N	\N	TIME	2	4	PHRF	\N
1526	84	18	2025-10-18 19:42:57	81	\N	\N	\N	TIME	2	3	PHRF	\N
1527	84	41	2025-10-18 19:43:41	93	\N	\N	\N	TIME	2	3	PHRF	\N
1528	84	90	2025-10-18 19:51:01	96	\N	\N	\N	TIME	2	3	PHRF	\N
1471	80	63	2025-09-06 19:13:38	18	\N	\N	\N	TIME	1	1	PHRF	\N
1472	80	11	2025-09-06 19:17:41	48	\N	\N	\N	TIME	1	1	PHRF	\N
1473	80	31	\N	93	\N	\N	\N	RET	2	3	PHRF	\N
1474	80	22	2025-09-06 19:25:07	69	\N	\N	\N	TIME	1	2	PHRF	\N
1475	80	12	2025-09-06 19:45:03	72	\N	\N	\N	TIME	1	2	PHRF	\N
1476	80	15	2025-09-06 19:53:57	93	\N	\N	\N	TIME	2	3	PHRF	\N
1477	80	18	2025-09-06 19:55:36	81	\N	\N	\N	TIME	2	3	PHRF	\N
1478	80	19	2025-09-06 20:02:14	120	\N	\N	\N	TIME	2	4	PHRF	\N
1479	80	90	2025-09-06 20:55:08	96	\N	\N	\N	TIME	2	3	PHRF	\N
1480	80	5	2025-09-06 21:01:44	162	\N	\N	\N	TIME	2	4	PHRF	\N
1481	80	36	2025-09-06 21:05:34	141	\N	\N	\N	TIME	2	4	PHRF	\N
1482	80	30	2025-09-06 21:06:20	126	\N	\N	\N	TIME	2	4	PHRF	\N
1483	80	13	2025-09-06 21:12:01	159	\N	\N	\N	TIME	2	4	PHRF	\N
1484	80	14	2025-09-06 21:15:50	132	\N	\N	\N	TIME	2	4	PHRF	\N
1485	80	23	2025-09-06 21:18:59	144	\N	\N	\N	TIME	2	4	PHRF	\N
1486	80	41	2025-09-06 21:25:09	93	\N	\N	\N	TIME	2	3	PHRF	\N
1489	82	11	2025-09-20 18:30:16	48	\N	\N	\N	TIME	1	1	PHRF	\N
1490	82	27	2025-09-20 18:43:21	72	\N	\N	\N	TIME	1	2	PHRF	\N
1491	82	15	2025-09-20 18:45:44	93	\N	\N	\N	TIME	2	3	PHRF	\N
1492	82	90	2025-09-20 18:46:41	96	\N	\N	\N	TIME	2	3	PHRF	\N
1493	82	22	2025-09-20 18:46:45	69	\N	\N	\N	TIME	1	2	PHRF	\N
1494	82	18	2025-09-20 18:47:53	81	\N	\N	\N	TIME	2	3	PHRF	\N
1495	82	30	2025-09-20 18:48:15	126	\N	\N	\N	TIME	2	4	PHRF	\N
1652	92	5	2026-04-23 02:58:25	162	\N	\N	\N	TIME	2	4	PHRF	\N
1729	94	106	\N	93	\N	\N	\N	DNF	2	3	PHRF	\N
1785	97	99	2026-05-28 03:17:09	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1845	99	63	\N	18	\N	\N	\N	RET	1	1	ORC_PHRF	04560004KI4
1904	101	56	2026-06-18 03:59:10	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1964	103	90	2026-06-27 22:25:42	96	\N	\N	\N	TIME	38	67	PHRF	\N
2026	107	106	2026-07-09 03:45:56.233	93	\N	\N	\N	TIME	37	65	PHRF	\N
2084	109	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2140	111	41	2026-08-06 03:13:19	93	\N	\N	\N	TIME	37	65	PHRF	\N
2207	114	29	2026-08-16 00:39:33	99	\N	\N	\N	TIME	7	28	PHRF	\N
2263	123	112	2026-08-27 01:04:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2358	118	15	2026-09-19 19:28:41	93	\N	\N	\N	TIME	37	66	PHRF	\N
2422	116	88	2026-10-03 19:42:18	\N	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
1496	82	19	2025-09-20 18:48:37	120	\N	\N	\N	TIME	2	4	PHRF	\N
1497	82	41	2025-09-20 18:51:05	93	\N	\N	\N	TIME	2	3	PHRF	\N
1498	82	36	2025-09-20 18:51:51	141	\N	\N	\N	TIME	2	4	PHRF	\N
1499	82	13	2025-09-20 18:52:04	159	\N	\N	\N	TIME	2	4	PHRF	\N
1500	82	63	2025-09-20 18:52:44	18	\N	\N	\N	TIME	1	1	PHRF	\N
1501	82	14	2025-09-20 18:55:24	132	\N	\N	\N	TIME	2	4	PHRF	\N
1502	82	23	2025-09-20 19:07:32	144	\N	\N	\N	TIME	2	4	PHRF	\N
1503	82	21	2025-09-20 19:08:02	195	\N	\N	\N	TIME	2	4	PHRF	\N
1504	82	16	2025-09-20 19:19:37	141	\N	\N	\N	TIME	2	4	PHRF	\N
1529	84	36	2025-10-18 19:56:02	141	\N	\N	\N	TIME	2	4	PHRF	\N
1530	84	13	2025-10-18 19:57:58	159	\N	\N	\N	TIME	2	4	PHRF	\N
1531	84	14	2025-10-18 19:58:32	132	\N	\N	\N	TIME	2	4	PHRF	\N
1532	84	23	2025-10-18 20:13:00	144	\N	\N	\N	TIME	2	4	PHRF	\N
1533	84	16	2025-10-18 20:24:31	141	\N	\N	\N	TIME	2	4	PHRF	\N
1536	84	50	2025-10-18 19:37:00	63	\N	\N	\N	TIME	1	2	PHRF	\N
1538	49	19	\N	120	\N	\N	\N	DNS_RC	2	17	PHRF	\N
1539	53	15	\N	93	\N	\N	\N	DNS_RC	2	3	PHRF	\N
1540	55	19	\N	120	\N	\N	\N	DNS_RC	2	4	PHRF	\N
1541	54	29	\N	99	\N	\N	\N	DNS_RC	2	3	PHRF	\N
1543	58	43	\N	93	\N	\N	\N	DNS_RC	2	3	PHRF	\N
1544	68	16	\N	141	\N	\N	\N	DNS_RC	2	4	PHRF	\N
1545	74	19	\N	120	\N	\N	\N	DNS_RC	2	4	PHRF	\N
1546	75	19	\N	120	\N	\N	\N	DNS_RC	2	4	PHRF	\N
1115	64	63	2025-06-19 03:25:41	18	\N	\N	\N	TIME	1	1	PHRF	\N
1177	66	63	2025-06-26 03:07:45	18	\N	\N	\N	TIME	1	1	PHRF	\N
56	2	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
93	18	33	2024-04-11 03:12:28	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
99	18	3	2024-04-11 03:21:14	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
203	21	33	2024-05-02 03:06:14	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
205	21	3	2024-05-02 03:17:49	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
101	18	1	2024-04-11 03:22:14	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
105	18	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
73	3	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
71	3	3	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
79	17	2	2024-03-30 19:26:21	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
120	19	33	2024-04-18 02:55:52	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
124	19	34	2024-04-18 02:58:43	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
127	19	3	2024-04-18 03:02:03	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
128	19	1	2024-04-18 03:02:28	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
94	18	34	2024-04-11 03:13:35	\N	\N	\N	2	\N	4	19	CruisingFlyingSails	\N
1653	92	50	2026-04-23 03:09:46	63	\N	\N	\N	TIME	1	2	PHRF	\N
1730	94	94	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1786	97	98	2026-05-28 03:08:45	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1846	100	29	2026-06-11 02:49:16	99	\N	\N	\N	TIME	2	3	PHRF	\N
1905	101	93	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1965	103	36	2026-06-27 22:49:07	141	\N	\N	\N	TIME	38	68	PHRF	\N
2027	107	34	2026-07-09 03:47:15	153	\N	\N	\N	TIME	38	68	PHRF	\N
2085	109	50	\N	63	\N	\N	\N	RET	37	64	PHRF	\N
2141	111	98	\N	\N	\N	\N	\N	RET	4	19	CruisingFlyingSails	\N
2208	114	36	2026-08-16 00:45:31	141	\N	\N	\N	TIME	7	28	PHRF	\N
2264	123	113	2026-08-27 01:05:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2359	118	12	2026-09-19 19:31:56	72	\N	\N	\N	TIME	37	66	PHRF	\N
2423	116	35	2026-10-03 19:47:48	141	\N	\N	\N	TIME	35	62	CruisingNonFlyingSails	\N
206	21	34	2024-05-02 03:18:45	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
146	20	3	2024-04-25 03:04:18	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
147	20	34	2024-04-25 02:58:31	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
148	20	33	2024-04-25 02:57:47	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
152	20	1	2024-04-25 03:00:35	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
224	22	33	2024-05-09 02:43:05	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
158	20	2	2024-04-25 03:14:37	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
227	22	3	2024-05-09 02:43:55	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
228	22	1	2024-05-09 02:45:11	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
229	22	34	2024-05-09 02:45:23	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
274	23	33	2024-05-16 02:58:55	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
276	23	34	2024-05-16 03:01:57	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
282	23	1	2024-05-16 03:37:15	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
259	23	2	2024-05-16 03:37:03	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
277	23	3	2024-05-16 03:14:35	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
331	24	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
279	23	24	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
333	24	24	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
468	28	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
541	33	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
1334	73	2	2025-07-31 03:15:11	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
353	25	33	2024-05-30 02:55:30	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
355	25	34	2024-05-30 02:57:14	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
357	25	3	2024-05-30 03:00:06	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
359	25	1	2024-05-30 03:01:31	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
361	25	2	2024-05-30 03:03:24	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
377	26	34	2024-06-06 02:47:37	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
379	26	24	2024-06-06 02:49:18	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
380	26	3	2024-06-06 02:51:32	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
382	26	2	2024-06-06 02:54:00	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
383	26	1	2024-06-06 02:54:06	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
392	27	33	2024-06-13 02:52:03	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
393	27	34	2024-06-13 02:57:24	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
394	27	24	2024-06-13 02:57:53	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
395	27	3	2024-06-13 02:59:05	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
396	27	1	2024-06-13 03:02:47	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
397	27	2	2024-06-13 03:07:40	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
418	29	33	2024-06-20 02:41:15	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
419	29	34	2024-06-20 02:43:58	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
420	29	3	2024-06-20 02:45:12	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
1655	92	90	2026-04-23 03:00:42	96	\N	\N	\N	TIME	2	3	PHRF	\N
1731	94	56	\N	\N	2	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
1787	97	11	2026-05-28 03:14:09	48	\N	\N	\N	TIME	1	1	PHRF	\N
1847	100	19	2026-06-11 02:49:30	120	\N	\N	\N	TIME	2	4	PHRF	\N
1908	102	53	2026-06-25 02:50:38	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1967	103	52	2026-06-27 22:13:43	105	\N	\N	\N	TIME	38	67	PHRF	\N
2028	107	39	2026-07-09 03:48:48	144	\N	\N	\N	TIME	38	68	PHRF	\N
2086	109	22	\N	69	\N	\N	\N	RET	37	64	PHRF	\N
2142	111	6	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2209	114	53	2026-08-16 01:36:04	\N	\N	\N	\N	TIME	5	29	CruisingFlyingSails	\N
2265	123	111	2026-08-27 01:06:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2360	118	63	2026-09-19 19:37:19	18	\N	\N	\N	TIME	37	66	ORC_PHRF	04560004KI4
429	29	2	2024-06-20 02:58:11	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
448	32	2	2024-06-22 22:49:12	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
463	28	33	2024-06-27 03:12:45	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
466	28	34	2024-06-27 03:23:47	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
473	30	33	2024-07-04 02:46:45	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
474	30	24	2024-07-04 02:52:26	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
476	30	1	2024-07-04 02:56:12	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
477	30	3	2024-07-04 02:56:47	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
481	30	2	2024-07-04 03:01:02	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
502	31	51	2024-07-11 03:14:57	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
503	31	33	2024-07-11 03:17:08	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
504	31	34	2024-07-11 03:20:55	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
506	31	1	2024-07-11 03:25:09	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
508	31	2	2024-07-11 03:30:59	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
510	31	24	2024-07-11 03:31:35	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
511	31	3	2024-07-11 03:32:44	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
540	33	3	2024-07-13 23:25:54	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
546	42	51	2024-07-18 03:05:30	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
548	42	33	2024-07-18 03:12:20	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
549	42	1	2024-07-18 03:19:44	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
550	42	3	2024-07-18 03:19:55	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
551	42	2	2024-07-18 03:28:07	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
555	42	53	2024-07-18 03:18:20	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
582	43	51	2024-07-25 02:59:58	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
587	43	33	2024-07-25 03:07:15	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
588	43	34	2024-07-25 03:14:15	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
590	43	3	2024-07-25 03:17:28	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
591	43	2	2024-07-25 03:21:41	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
592	43	1	2024-07-25 03:22:32	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
597	44	51	2024-08-01 02:52:18	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
602	44	33	2024-08-01 02:55:34	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
606	44	3	2024-08-01 02:57:44	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
607	44	34	2024-08-01 02:58:30	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
611	44	53	2024-08-01 03:00:24	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
613	44	2	2024-08-01 03:04:31	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
614	44	1	2024-08-01 03:04:41	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
630	45	51	2024-08-08 03:09:03	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
632	45	33	2024-08-08 03:12:57	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
637	45	34	2024-08-08 03:21:18	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
1656	92	106	2026-04-23 03:01:24	93	\N	\N	\N	TIME	2	3	PHRF	\N
1733	95	11	2026-05-14 02:38:15	48	\N	\N	\N	TIME	1	1	PHRF	\N
1788	97	12	2026-05-28 03:19:42	72	\N	\N	\N	TIME	1	2	PHRF	\N
1848	100	18	2026-06-11 02:50:12	81	\N	\N	\N	TIME	2	3	PHRF	\N
1909	102	105	2026-06-25 02:51:49	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1972	100	56	2026-06-11 03:59:30	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2029	107	16	2026-07-09 03:55:34	141	\N	\N	\N	TIME	38	68	PHRF	\N
2087	109	88	\N	\N	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2143	111	22	2026-08-06 03:32:36	69	\N	\N	\N	TIME	37	64	PHRF	\N
2210	114	5	2026-08-16 01:42:13	162	\N	\N	\N	TIME	7	28	PHRF	\N
2266	123	116	2026-08-27 01:07:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2361	118	18	2026-09-19 19:34:11	84	\N	\N	\N	TIME	37	66	PHRF	\N
638	45	1	2024-08-08 03:26:33	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
639	45	3	2024-08-08 03:28:45	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
651	47	3	2024-08-10 21:31:07	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
668	34	51	2024-08-15 03:22:31	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
672	34	3	2024-08-15 03:38:44	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
1362	74	53	2025-08-07 03:09:39	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1367	74	50	2025-08-07 03:17:52	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
676	34	33	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
677	34	34	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
686	36	51	2024-08-22 02:33:40	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
687	36	33	2024-08-22 02:36:45	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
689	36	34	2024-08-22 02:41:24	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
690	36	2	2024-08-22 02:47:19	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
691	36	3	2024-08-22 02:48:57	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
708	36	1	2024-08-22 02:47:29	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
718	37	1	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
719	37	3	2024-08-24 19:44:44	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
720	37	2	2024-08-24 19:53:44	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
753	46	2	\N	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
1363	74	1	2025-08-07 03:12:50	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1368	74	33	2025-08-07 03:25:06	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
773	40	2	2024-10-19 19:33:40	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
790	41	2	2024-11-02 20:51:51	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
791	41	56	2024-11-02 23:08:17	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
828	52	57	2025-03-29 19:38:09	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
845	53	34	2025-04-10 03:13:53	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
864	54	34	2025-04-17 03:34:13	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
868	54	2	2025-04-17 03:43:32	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
878	55	34	2025-04-24 02:50:07	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
880	55	2	2025-04-24 03:02:07	\N	\N	\N	\N	\N	4	19	CruisingFlyingSails	\N
926	56	34	2025-05-01 02:45:49	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
932	56	2	2025-05-01 02:59:59	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
973	58	34	2025-05-15 03:26:38	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
977	58	33	\N	\N	\N	\N	\N	RET	4	19	CruisingFlyingSails	\N
980	59	34	2025-05-22 02:57:20	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
981	59	33	2025-05-22 02:58:52	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
992	59	2	2025-05-22 03:15:33	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1020	60	34	2025-05-29 03:08:30	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1657	92	16	2026-04-23 03:03:21	141	\N	\N	\N	TIME	2	4	PHRF	\N
1734	95	27	2026-05-14 02:40:12	72	\N	\N	\N	TIME	1	2	PHRF	\N
1789	97	50	2026-05-28 03:21:13	63	\N	\N	\N	TIME	1	2	PHRF	\N
1849	100	15	2026-06-11 02:50:21	93	\N	\N	\N	TIME	2	3	PHRF	\N
1910	102	31	2026-06-25 02:58:52	93	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1973	100	35	2026-06-11 04:04:16	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2031	107	21	2026-07-09 03:57:21	195	\N	\N	\N	TIME	38	68	PHRF	\N
2144	111	56	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2088	109	31	\N	93	\N	\N	\N	RET	3	18	CruisingNonFlyingSails	\N
2212	114	99	\N	\N	\N	\N	\N	RET	5	29	CruisingFlyingSails	\N
2267	129	110	2026-08-27 01:11:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2362	118	29	2026-09-19 19:33:41	99	\N	\N	\N	TIME	38	69	PHRF	\N
1026	60	2	2025-05-29 03:31:42	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1036	60	33	\N	\N	\N	\N	\N	DNF	4	19	CruisingFlyingSails	\N
1057	61	33	\N	\N	\N	\N	\N	RET	4	19	CruisingFlyingSails	\N
1063	61	34	2025-06-05 03:42:51	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1082	62	33	2025-06-12 03:35:10	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1083	62	34	2025-06-12 03:35:23	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1091	62	2	2025-06-12 03:57:46	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1104	64	53	2025-06-19 03:02:33	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1106	64	33	2025-06-19 03:04:13	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1112	64	2	2025-06-19 03:19:43	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1130	65	98	2025-06-21 22:42:35	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1365	74	34	2025-08-07 03:12:51	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1165	66	33	2025-06-26 03:49:57	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1174	66	34	2025-06-26 03:40:40	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1186	67	98	2025-06-29 19:54:14	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1188	67	99	2025-06-29 19:58:26	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1193	68	53	2025-07-03 03:02:46	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1197	68	33	2025-07-03 03:08:08	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1210	68	2	2025-07-03 03:38:03	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1219	69	34	2025-07-10 03:13:01	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1220	69	33	2025-07-10 03:15:20	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1223	69	53	2025-07-10 03:22:01	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1226	69	2	2025-07-10 03:31:59	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1256	70	98	2025-07-12 21:31:08	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1260	70	99	2025-07-12 21:59:14	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1264	71	100	2025-07-17 02:45:06	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1265	71	34	2025-07-17 02:46:27	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1268	71	33	2025-07-17 02:49:53	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1269	71	53	2025-07-17 02:53:04	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1295	71	2	2025-07-17 02:57:05	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1304	72	50	2025-07-24 03:04:12	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1306	72	33	2025-07-24 03:04:38	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1326	73	1	2025-07-31 03:00:36	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1327	73	33	2025-07-31 03:02:37	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1381	75	98	2025-08-09 21:31:54	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1382	75	99	2025-08-09 21:49:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1396	76	34	2025-08-14 03:09:30	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1658	92	36	2026-04-23 03:04:37	141	\N	\N	\N	TIME	2	4	PHRF	\N
1735	95	28	2026-05-14 02:40:46	42	\N	\N	\N	TIME	1	1	ORC_PHRF	04560004KJP
1790	97	19	2026-05-28 03:23:50	120	\N	\N	\N	TIME	2	4	PHRF	\N
1911	102	88	2026-06-25 02:59:47	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1851	100	34	2026-06-11 02:56:40	153	\N	\N	\N	TIME	2	4	PHRF	\N
1974	100	93	2026-06-11 04:07:15	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2032	107	93	2026-07-09 04:01:25	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2145	110	15	\N	93	\N	\N	\N	DNS_RC	37	65	PHRF	\N
2089	109	21	\N	195	\N	\N	\N	RET	38	68	PHRF	\N
2213	114	108	2026-08-16 01:07:46	159	\N	\N	\N	TIME	7	28	PHRF	\N
2268	129	112	2026-08-27 01:12:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2363	118	36	2026-09-19 19:47:20	141	\N	\N	\N	TIME	38	69	PHRF	\N
1399	76	50	2025-08-14 03:12:59	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1401	76	1	2025-08-14 03:17:20	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1403	76	33	2025-08-14 03:19:41	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1422	78	34	2025-08-21 02:44:30	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1423	78	50	2025-08-21 02:44:39	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1426	78	1	2025-08-21 02:50:41	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1427	78	53	2025-08-21 02:50:45	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1428	78	33	2025-08-21 02:52:36	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1456	79	50	2025-08-23 19:17:39	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1457	79	98	2025-08-23 19:29:29	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1459	79	99	2025-08-23 19:34:55	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1487	80	98	2025-09-06 21:42:01	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1506	82	102	2025-09-20 19:26:05	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1507	82	98	2025-09-20 19:27:29	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1537	84	102	2025-10-18 21:42:48	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1542	57	2	\N	\N	\N	\N	\N	DNS_RC	4	19	CruisingFlyingSails	\N
1547	78	2	\N	\N	\N	\N	\N	DNS_RC	4	19	CruisingFlyingSails	\N
1660	92	34	2026-04-23 03:08:00	\N	\N	\N	\N	TIME	4	19	CruisingFlyingSails	\N
1736	95	50	2026-05-14 02:42:03	63	\N	\N	\N	TIME	1	2	PHRF	\N
1791	97	29	2026-05-28 03:25:46	99	\N	\N	\N	TIME	2	3	PHRF	\N
1852	100	90	2026-06-11 02:57:48	96	\N	\N	\N	TIME	2	3	PHRF	\N
1912	102	94	2026-06-25 03:00:35	\N	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
1975	100	88	\N	\N	1	\N	\N	HOC	3	18	CruisingNonFlyingSails	\N
2033	107	35	2026-07-09 04:07:10	141	\N	\N	\N	TIME	3	18	CruisingNonFlyingSails	\N
2090	109	18	\N	84	\N	\N	\N	RET	37	65	PHRF	\N
2269	129	115	2026-08-27 01:13:00	\N	\N	\N	\N	TIME	40	72	CruisingNonFlyingSails	\N
2364	118	13	2026-09-19 19:48:43	159	\N	\N	\N	TIME	38	69	PHRF	\N
\.


--
-- Data for Name: racetime; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.racetime (class_id, race_id, start_date, end_date) FROM stdin;
1	18	2024-04-11 02:00:00	2024-04-11 03:30:00
2	18	2024-04-11 02:05:00	2024-04-11 03:30:00
4	18	2024-04-11 02:10:00	2024-04-11 03:30:00
3	18	2024-04-11 02:10:00	2024-04-11 03:30:00
1	26	2024-06-06 02:00:00	2024-06-06 04:00:00
2	26	2024-06-06 02:05:00	2024-06-06 04:00:00
4	26	2024-06-06 02:10:00	2024-06-06 04:00:00
3	26	2024-06-06 02:10:00	2024-06-06 04:00:00
1	37	2024-08-24 18:00:00	2024-08-24 21:00:00
2	37	2024-08-24 18:05:00	2024-08-24 21:05:00
4	37	2024-08-24 18:10:00	2024-08-24 21:10:00
3	37	2024-08-24 18:10:00	2024-08-24 21:10:00
1	29	2024-06-20 02:00:00	2024-06-20 04:00:00
2	29	2024-06-20 02:05:00	2024-06-20 04:00:00
4	29	2024-06-20 02:10:00	2024-06-20 04:00:00
3	29	2024-06-20 02:10:00	2024-06-20 04:00:00
1	38	2024-09-07 17:25:00	2024-09-07 23:25:00
2	38	2024-09-07 17:30:00	2024-09-07 23:30:00
1	1	2024-01-20 18:00:00	2024-01-21 00:00:00
2	1	2024-01-20 18:00:00	2024-01-21 00:00:00
4	1	2024-01-20 18:00:00	2024-01-21 00:00:00
3	1	2024-01-20 18:00:00	2024-01-21 00:00:00
1	2	2024-02-03 18:00:00	2024-02-04 00:00:00
2	2	2024-02-03 18:00:00	2024-02-04 00:00:00
4	2	2024-02-03 18:00:00	2024-02-04 00:00:00
3	2	2024-02-03 18:00:00	2024-02-04 00:00:00
4	38	2024-09-07 17:35:00	2024-09-07 23:35:00
3	38	2024-09-07 17:35:00	2024-09-07 23:35:00
1	36	2024-08-22 02:00:00	2024-08-22 03:30:00
2	36	2024-08-22 02:05:00	2024-08-22 03:30:00
4	36	2024-08-22 02:10:00	2024-08-22 03:30:00
3	36	2024-08-22 02:10:00	2024-08-22 03:30:00
1	20	2024-04-25 02:00:00	2024-04-25 03:30:00
2	20	2024-04-25 02:05:00	2024-04-25 03:30:00
4	20	2024-04-25 02:10:00	2024-04-25 03:30:00
3	20	2024-04-25 02:10:00	2024-04-25 03:30:00
1	44	2024-08-01 02:00:00	2024-08-01 04:00:00
2	44	2024-08-01 02:05:00	2024-08-01 04:00:00
4	44	2024-08-01 02:10:00	2024-08-01 04:00:00
3	44	2024-08-01 02:15:00	2024-08-01 04:00:00
1	21	2024-05-02 02:00:00	2024-05-02 03:30:00
2	21	2024-05-02 02:05:00	2024-05-02 03:30:00
4	21	2024-05-02 02:10:00	2024-05-02 03:30:00
3	21	2024-05-02 02:10:00	2024-05-02 03:30:00
1	45	2024-08-08 02:15:00	2024-08-08 03:25:00
2	45	2024-08-08 02:20:00	2024-08-08 03:50:00
4	45	2024-08-08 02:25:00	2024-08-08 03:55:00
3	45	2024-08-08 02:25:00	2024-08-08 03:55:00
1	34	2024-08-15 02:00:00	2024-08-15 03:30:00
2	34	2024-08-15 02:05:00	2024-08-15 03:30:00
4	34	2024-08-15 02:10:00	2024-08-15 03:30:00
3	34	2024-08-15 02:10:00	2024-08-15 03:30:00
1	35	2024-08-17 17:40:00	2024-08-17 22:40:00
2	35	2024-08-17 17:40:00	2024-08-17 22:40:00
4	35	2024-08-17 17:40:00	2024-08-17 22:40:00
3	35	2024-08-17 17:40:00	2024-08-17 22:40:00
1	22	2024-05-09 02:00:00	2024-05-09 03:30:00
2	22	2024-05-09 02:05:00	2024-05-09 03:30:00
4	22	2024-05-09 02:10:00	2024-05-09 03:30:00
3	22	2024-05-09 02:10:00	2024-05-09 03:30:00
1	33	2024-07-13 20:10:00	2024-07-13 23:10:00
2	33	2024-07-13 20:30:00	2024-07-13 23:30:00
4	33	2024-07-13 20:35:00	2024-07-13 23:35:00
3	33	2024-07-13 20:35:00	2024-07-13 23:35:00
1	42	2024-07-18 02:15:00	2024-07-18 04:00:00
1	24	2024-05-23 02:00:00	2024-05-23 04:00:00
2	24	2024-05-23 02:05:00	2024-05-23 04:00:00
4	24	2024-05-23 02:10:00	2024-05-23 04:00:00
3	24	2024-05-23 02:10:00	2024-05-23 04:00:00
2	42	2024-07-18 02:20:00	2024-07-18 04:00:00
4	42	2024-07-18 02:25:00	2024-07-18 04:00:00
3	42	2024-07-18 02:25:00	2024-07-18 04:00:00
37	108	2026-07-16 02:00:00	2026-07-16 04:00:00
38	108	2026-07-16 02:05:00	2026-07-16 04:05:00
4	108	2026-07-16 02:10:00	2026-07-16 04:10:00
3	108	2026-07-16 02:10:00	2026-07-16 04:10:00
1	62	2025-06-12 02:00:00	2025-06-12 04:00:00
2	62	2025-06-12 02:05:00	2025-06-12 04:00:00
4	62	2025-06-12 02:10:00	2025-06-12 04:00:00
3	62	2025-06-12 02:10:00	2025-06-12 04:00:00
1	78	2025-08-21 02:00:00	2025-08-21 03:30:00
2	78	2025-08-21 02:05:00	2025-08-21 03:35:00
4	78	2025-08-21 02:10:00	2025-08-21 03:40:00
3	78	2025-08-21 02:10:00	2025-08-21 03:40:00
1	69	2025-07-10 02:00:00	2025-07-10 04:00:00
2	69	2025-07-10 02:05:00	2025-07-10 04:05:00
4	69	2025-07-10 02:10:00	2025-07-10 04:10:00
3	69	2025-07-10 02:10:00	2025-07-10 04:10:00
1	71	2025-07-17 02:00:00	2025-07-17 04:00:00
2	71	2025-07-17 02:05:00	2025-07-17 04:05:00
4	71	2025-07-17 02:10:00	2025-07-17 04:10:00
3	71	2025-07-17 02:10:00	2025-07-17 04:10:00
1	73	2025-07-31 02:00:00	2025-07-31 03:30:00
2	73	2025-07-31 02:05:00	2025-07-31 03:35:00
4	73	2025-07-31 02:10:00	2025-07-31 03:40:00
3	73	2025-07-31 02:10:00	2025-07-31 03:40:00
37	103	2026-06-27 20:00:00	2026-06-27 23:00:00
38	103	2026-06-27 20:05:00	2026-06-27 23:05:00
4	103	2026-06-27 20:10:00	2026-06-27 23:10:00
3	103	2026-06-27 20:10:00	2026-06-27 23:10:00
1	3	2024-02-10 18:00:00	2024-02-11 00:00:00
2	3	2024-02-10 18:05:00	2024-02-11 00:00:00
4	3	2024-02-10 18:10:00	2024-02-11 00:00:00
3	3	2024-02-10 18:10:00	2024-02-11 00:00:00
1	17	2024-03-30 18:00:00	2024-03-30 21:00:00
2	17	2024-03-30 18:00:00	2024-03-30 21:00:00
4	17	2024-03-30 18:05:00	2024-03-30 21:00:00
3	17	2024-03-30 18:05:00	2024-03-30 21:00:00
1	27	2024-06-13 02:00:00	2024-06-13 04:00:00
2	27	2024-06-13 02:05:00	2024-06-13 04:00:00
4	27	2024-06-13 02:10:00	2024-06-13 04:00:00
3	27	2024-06-13 02:10:00	2024-06-13 04:00:00
1	43	2024-07-25 02:00:00	2024-07-25 03:30:00
2	43	2024-07-25 02:05:00	2024-07-25 03:35:00
4	43	2024-07-25 02:10:00	2024-07-25 03:40:00
3	43	2024-07-25 02:10:00	2024-07-25 03:40:00
1	28	2024-06-27 02:00:00	2024-06-27 04:00:00
2	28	2024-06-27 02:05:00	2024-06-27 04:00:00
4	28	2024-06-27 02:10:00	2024-06-27 04:00:00
3	28	2024-06-27 02:10:00	2024-06-27 04:00:00
1	47	2024-08-10 19:55:00	2024-08-10 23:00:00
37	107	2026-07-09 02:10:00	2026-07-09 04:10:00
38	107	2026-07-09 02:15:00	2026-07-09 04:15:00
4	107	2026-07-09 02:20:00	2026-07-09 04:20:00
3	107	2026-07-09 02:20:00	2026-07-09 04:20:00
2	47	2024-08-10 20:00:00	2024-08-10 23:00:00
4	47	2024-08-10 20:00:00	2024-08-10 23:00:00
3	47	2024-08-10 20:00:00	2024-08-10 23:00:00
1	19	2024-04-18 02:00:00	2024-04-18 03:30:00
2	19	2024-04-18 02:05:00	2024-04-18 03:30:00
4	19	2024-04-18 02:10:00	2024-04-18 03:30:00
3	19	2024-04-18 02:10:00	2024-04-18 03:30:00
1	30	2024-07-04 02:05:00	2024-07-04 04:00:00
2	30	2024-07-04 02:10:00	2024-07-04 04:00:00
4	30	2024-07-04 02:15:00	2024-07-04 04:00:00
3	30	2024-07-04 02:15:00	2024-07-04 04:00:00
1	31	2024-07-11 02:35:00	2024-07-11 04:00:00
2	31	2024-07-11 02:40:00	2024-07-11 04:00:00
4	31	2024-07-11 02:45:00	2024-07-11 04:00:00
3	31	2024-07-11 02:45:00	2024-07-11 04:00:00
1	46	2024-09-21 17:00:00	2024-09-21 22:00:00
2	46	2024-09-21 17:05:00	2024-09-21 22:05:00
4	46	2024-09-21 17:10:00	2024-09-21 22:10:00
3	46	2024-09-21 17:10:00	2024-09-21 22:10:00
1	39	2024-10-05 17:55:00	2024-10-05 21:00:00
2	39	2024-10-05 18:00:00	2024-10-05 21:00:00
4	39	2024-10-05 18:05:00	2024-10-05 21:00:00
3	39	2024-10-05 18:10:00	2024-10-05 21:00:00
1	32	2024-06-22 20:00:00	2024-06-22 23:00:00
2	32	2024-06-22 20:00:00	2024-06-22 23:00:00
4	32	2024-06-22 20:00:00	2024-06-22 23:00:00
3	32	2024-06-22 20:00:00	2024-06-22 23:00:00
1	23	2024-05-16 02:00:00	2024-05-16 04:00:00
2	23	2024-05-16 02:05:00	2024-05-16 04:00:00
4	23	2024-05-16 02:10:00	2024-05-16 04:00:00
3	23	2024-05-16 02:10:00	2024-05-16 04:00:00
1	25	2024-05-30 02:10:00	2024-05-30 04:00:00
2	25	2024-05-30 02:15:00	2024-05-30 04:00:00
4	25	2024-05-30 02:20:00	2024-05-30 04:00:00
3	25	2024-05-30 02:20:00	2024-05-30 04:00:00
1	40	2024-10-19 17:30:00	2024-10-19 22:00:00
2	40	2024-10-19 17:35:00	2024-10-19 22:00:00
4	40	2024-10-19 17:40:00	2024-10-19 22:00:00
3	40	2024-10-19 17:40:00	2024-10-19 22:00:00
1	48	2025-01-11 18:00:00	2025-01-12 00:00:00
2	48	2025-01-11 18:00:00	2025-01-12 00:00:00
4	48	2025-01-11 18:00:00	2025-01-12 00:00:00
3	48	2025-01-11 18:00:00	2025-01-12 00:00:00
1	75	2025-08-09 20:00:00	2025-08-09 23:00:00
2	75	2025-08-09 20:00:00	2025-08-09 23:00:00
4	75	2025-08-09 20:05:00	2025-08-09 23:05:00
3	75	2025-08-09 20:05:00	2025-08-09 23:05:00
1	60	2025-05-29 02:00:00	2025-05-29 04:00:00
2	60	2025-05-29 02:05:00	2025-05-29 04:00:00
4	60	2025-05-29 02:10:00	2025-05-29 04:00:00
3	60	2025-05-29 02:10:00	2025-05-29 04:00:00
1	64	2025-06-19 02:00:00	2025-06-19 04:00:00
2	64	2025-06-19 02:05:00	2025-06-19 04:05:00
4	64	2025-06-19 02:10:00	2025-06-19 04:10:00
3	64	2025-06-19 02:10:00	2025-06-19 04:10:00
1	66	2025-06-26 02:00:00	2025-06-26 04:00:00
2	66	2025-06-26 02:05:00	2025-06-26 04:05:00
4	66	2025-06-26 02:10:00	2025-06-26 04:10:00
3	66	2025-06-26 02:10:00	2025-06-26 04:10:00
1	67	2025-06-29 17:00:00	2025-06-29 23:00:00
2	67	2025-06-29 17:00:00	2025-06-29 23:00:00
4	67	2025-06-29 17:05:00	2025-06-29 23:05:00
3	67	2025-06-29 17:05:00	2025-06-29 23:05:00
1	88	2026-02-07 18:00:00	2026-02-08 01:00:00
2	88	2026-02-07 18:05:00	2026-02-08 01:05:00
4	88	2026-02-07 18:10:00	2026-02-08 01:10:00
3	88	2026-02-07 18:10:00	2026-02-08 01:10:00
37	102	2026-06-25 02:00:00	2026-06-25 04:00:00
1	89	2026-03-28 18:05:00	2026-03-28 21:05:00
1	79	2025-08-23 18:00:00	2025-08-23 21:00:00
2	79	2025-08-23 18:05:00	2025-08-23 21:05:00
4	79	2025-08-23 18:10:00	2025-08-23 21:10:00
1	72	2025-07-24 02:08:00	2025-07-24 03:38:00
2	72	2025-07-24 02:13:00	2025-07-24 03:43:00
4	72	2025-07-24 02:18:00	2025-07-24 03:48:00
3	72	2025-07-24 02:18:00	2025-07-24 03:48:00
3	79	2025-08-23 18:10:00	2025-08-23 21:10:00
2	89	2026-03-28 18:10:00	2026-03-28 21:10:00
4	89	2026-03-28 18:15:00	2026-03-28 21:15:00
3	89	2026-03-28 18:15:00	2026-03-28 21:15:00
1	90	2026-04-09 02:00:00	2026-04-09 04:00:00
38	102	2026-06-25 02:05:00	2026-06-25 04:05:00
4	102	2026-06-25 02:10:00	2026-06-25 04:10:00
3	102	2026-06-25 02:10:00	2026-06-25 04:10:00
37	105	2026-08-08 20:00:00	2026-08-08 23:00:00
38	105	2026-08-08 20:05:00	2026-08-08 23:05:00
4	105	2026-08-08 20:10:00	2026-08-08 23:10:00
3	105	2026-08-08 20:10:00	2026-08-08 23:10:00
37	109	2026-07-23 02:00:00	2026-07-23 03:30:00
38	109	2026-07-23 02:05:00	2026-07-23 03:35:00
2	90	2026-04-09 02:05:00	2026-04-09 04:05:00
4	90	2026-04-09 02:10:00	2026-04-09 04:10:00
3	90	2026-04-09 02:15:00	2026-04-09 04:15:00
1	93	2026-04-30 02:00:00	2026-04-30 04:00:00
2	93	2026-04-30 02:05:00	2026-04-30 04:05:00
4	93	2026-04-30 02:10:00	2026-04-30 04:10:00
3	93	2026-04-30 02:15:00	2026-04-30 04:10:00
4	109	2026-07-23 02:10:00	2026-07-23 03:40:00
3	109	2026-07-23 02:15:00	2026-07-23 03:45:00
40	124	2026-09-03 01:00:00	2026-09-03 02:30:00
37	115	2026-08-22 18:00:00	2026-08-22 21:00:00
1	98	2026-05-30 17:40:00	2026-05-30 23:00:00
2	98	2026-05-30 17:40:00	2026-05-30 23:00:00
4	98	2026-05-30 17:40:00	2026-05-30 23:05:00
3	98	2026-05-30 17:40:00	2026-05-30 23:05:00
1	100	2026-06-11 02:00:00	2026-06-11 04:00:00
2	100	2026-06-11 02:05:00	2026-06-11 04:05:00
4	100	2026-06-11 02:10:00	2026-06-11 04:10:00
3	100	2026-06-11 02:10:00	2026-06-11 04:10:00
37	104	2026-07-11 20:00:00	2026-07-11 23:00:00
38	104	2026-07-11 20:05:00	2026-07-11 23:05:00
4	104	2026-07-11 20:10:00	2026-07-11 23:10:00
3	104	2026-07-11 20:10:00	2026-07-11 23:10:00
1	74	2025-08-07 02:00:00	2025-08-07 03:30:00
2	74	2025-08-07 02:05:00	2025-08-07 03:35:00
4	74	2025-08-07 02:10:00	2025-08-07 03:40:00
3	74	2025-08-07 02:10:00	2025-08-07 03:40:00
38	115	2026-08-22 18:05:00	2026-08-22 21:05:00
4	115	2026-08-22 18:10:00	2026-08-22 21:10:00
3	115	2026-08-22 18:10:00	2026-08-22 21:10:00
37	118	2026-09-19 17:55:00	2026-09-19 22:55:00
38	118	2026-09-19 18:00:00	2026-09-19 23:00:00
4	118	2026-09-19 18:05:00	2026-09-19 23:05:00
3	118	2026-09-19 18:05:00	2026-09-19 23:05:00
40	135	2026-09-24 01:20:00	2026-09-24 01:29:00
40	137	2026-10-01 01:20:00	2026-10-01 01:29:00
1	41	2024-11-02 17:00:00	2024-11-02 23:00:00
2	41	2024-11-02 17:05:00	2024-11-02 23:05:00
4	41	2024-11-02 17:10:00	2024-11-02 23:10:00
3	41	2024-11-02 17:10:00	2024-11-02 23:10:00
1	49	2025-02-01 18:00:00	2025-02-02 00:00:00
2	49	2025-02-01 18:00:00	2025-02-02 00:00:00
4	49	2025-02-01 18:00:00	2025-02-02 00:00:00
3	49	2025-02-01 18:00:00	2025-02-02 00:00:00
1	50	2025-02-08 18:00:00	2025-02-09 01:00:00
2	50	2025-02-08 18:00:00	2025-02-09 01:00:00
4	50	2025-02-08 18:00:00	2025-02-09 01:00:00
3	50	2025-02-08 18:00:00	2025-02-09 01:00:00
1	52	2025-03-29 18:00:00	2025-03-29 21:00:00
2	52	2025-03-29 18:05:00	2025-03-29 21:05:00
4	52	2025-03-29 18:10:00	2025-03-29 21:10:00
3	52	2025-03-29 18:10:00	2025-03-29 21:10:00
37	110	2026-07-30 02:05:00	2026-07-30 03:35:00
1	53	2025-04-10 02:20:00	2025-04-10 03:50:00
2	53	2025-04-10 02:25:00	2025-04-10 03:55:00
4	53	2025-04-10 02:30:00	2025-04-10 04:00:00
3	53	2025-04-10 02:30:00	2025-04-10 04:00:00
38	110	2026-07-30 02:10:00	2026-07-30 03:40:00
4	110	2026-07-30 02:15:00	2026-07-30 03:45:00
3	110	2026-07-30 02:15:00	2026-07-30 03:45:00
1	61	2025-06-05 02:00:00	2025-06-05 04:00:00
2	61	2025-06-05 02:05:00	2025-06-05 04:05:00
4	61	2025-06-05 02:10:00	2025-06-05 04:10:00
3	61	2025-06-05 02:10:00	2025-06-05 04:10:00
1	54	2025-04-17 02:20:00	2025-04-17 03:50:00
2	54	2025-04-17 02:25:00	2025-04-17 03:55:00
4	54	2025-04-17 02:30:00	2025-04-17 04:00:00
3	54	2025-04-17 02:30:00	2025-04-17 04:00:00
1	65	2025-06-21 20:00:00	2025-06-21 23:00:00
2	65	2025-06-21 20:00:00	2025-06-21 23:00:00
4	65	2025-06-21 20:05:00	2025-06-21 23:05:00
3	65	2025-06-21 20:05:00	2025-06-21 23:05:00
1	55	2025-04-24 02:00:00	2025-04-24 03:30:00
2	55	2025-04-24 02:05:00	2025-04-24 03:35:00
4	55	2025-04-24 02:10:00	2025-04-24 03:40:00
3	55	2025-04-24 02:10:00	2025-04-24 03:40:00
1	76	2025-08-14 02:00:00	2025-08-14 03:30:00
2	76	2025-08-14 02:05:00	2025-08-14 03:35:00
4	76	2025-08-14 02:10:00	2025-08-14 03:40:00
3	76	2025-08-14 02:10:00	2025-08-14 03:40:00
1	57	2025-05-08 02:15:00	2025-05-08 03:45:00
2	57	2025-05-08 02:20:00	2025-05-08 03:50:00
4	57	2025-05-08 02:25:00	2025-05-08 03:55:00
3	57	2025-05-08 02:25:00	2025-05-08 03:55:00
1	68	2025-07-03 02:00:00	2025-07-03 04:00:00
2	68	2025-07-03 02:05:00	2025-07-03 04:05:00
4	68	2025-07-03 02:10:00	2025-07-03 04:10:00
3	68	2025-07-03 02:10:00	2025-07-03 04:10:00
1	70	2025-07-12 20:00:00	2025-07-12 23:00:00
2	70	2025-07-12 20:05:00	2025-07-12 23:05:00
4	70	2025-07-12 20:10:00	2025-07-12 23:10:00
3	70	2025-07-12 20:10:00	2025-07-12 23:10:00
1	58	2025-05-15 02:15:00	2025-05-15 04:15:00
2	58	2025-05-15 02:20:00	2025-05-15 04:20:00
4	58	2025-05-15 02:25:00	2025-05-15 04:25:00
3	58	2025-05-15 02:25:00	2025-05-15 04:25:00
1	91	2026-04-16 02:00:00	2026-04-16 04:00:00
2	91	2026-04-16 02:05:00	2026-04-16 04:05:00
4	91	2026-04-16 02:10:00	2026-04-16 04:10:00
3	91	2026-04-16 02:15:00	2026-04-16 04:15:00
1	59	2025-05-22 02:00:00	2025-05-22 04:00:00
2	59	2025-05-22 02:05:00	2025-05-22 04:00:00
4	59	2025-05-22 02:10:00	2025-05-22 04:00:00
3	59	2025-05-22 02:10:00	2025-05-22 04:00:00
7	77	2025-08-16 17:10:00	2025-08-17 03:10:00
9	77	2025-08-16 17:05:00	2025-08-17 03:05:00
8	77	2025-08-16 17:05:00	2025-08-17 03:05:00
1	56	2025-05-01 02:00:00	2025-05-01 03:30:00
2	56	2025-05-01 02:05:00	2025-05-01 03:35:00
4	56	2025-05-01 02:10:00	2025-05-01 03:40:00
3	56	2025-05-01 02:10:00	2025-05-01 03:40:00
7	114	2026-08-15 17:05:00	2026-08-16 03:05:00
1	95	2026-05-14 02:00:00	2026-05-14 04:00:00
5	77	2025-08-16 17:10:00	2025-08-17 03:10:00
2	95	2026-05-14 02:05:00	2026-05-14 04:05:00
4	95	2026-05-14 02:10:00	2026-05-14 04:10:00
3	95	2026-05-14 02:10:00	2026-05-14 04:10:00
1	96	2026-05-21 02:00:00	2026-05-21 04:00:00
9	114	2026-08-15 17:00:00	2026-08-16 03:00:00
2	96	2026-05-21 02:05:00	2026-05-21 04:05:00
4	96	2026-05-21 02:10:00	2026-05-21 04:10:00
3	96	2026-05-21 02:15:00	2026-05-21 04:10:00
1	99	2026-06-04 02:10:00	2026-06-04 04:10:00
5	114	2026-08-15 17:05:00	2026-08-16 03:05:00
8	114	2026-08-15 17:00:00	2026-08-16 03:00:00
35	114	2026-08-15 17:05:00	2026-08-16 03:05:00
36	114	2026-08-15 17:00:00	2026-08-16 03:00:00
2	99	2026-06-04 02:15:00	2026-06-04 04:15:00
4	99	2026-06-04 02:20:00	2026-06-04 04:20:00
3	99	2026-06-04 02:20:00	2026-06-04 04:20:00
40	129	2026-08-27 01:10:00	2026-08-27 01:19:00
40	131	2026-08-27 01:30:00	2026-08-27 01:39:00
40	132	2026-09-10 01:10:00	2026-09-10 01:19:00
37	119	2026-10-17 17:00:00	2026-10-17 23:00:00
38	119	2026-10-17 17:05:00	2026-10-17 23:05:00
4	119	2026-10-17 17:10:00	2026-10-17 23:10:00
3	119	2026-10-17 17:10:00	2026-10-17 23:01:00
40	127	2026-09-24 01:00:00	2026-09-24 02:00:00
40	134	2026-09-24 01:10:00	2026-09-24 01:19:00
40	128	2026-10-01 01:00:00	2026-10-01 01:09:00
35	116	2026-10-03 18:05:00	2026-10-03 21:05:00
36	116	2026-10-03 18:00:00	2026-10-03 21:00:00
37	111	2026-08-06 02:20:00	2026-08-06 03:50:00
38	111	2026-08-06 02:25:00	2026-08-06 03:55:00
1	87	2026-01-31 18:00:00	2026-02-01 00:00:00
2	87	2026-01-31 18:00:00	2026-02-01 00:00:00
4	87	2026-01-31 18:05:00	2026-02-01 00:05:00
3	87	2026-01-31 18:05:00	2026-02-01 00:05:00
4	111	2026-08-06 02:30:00	2026-08-06 04:00:00
3	111	2026-08-06 02:15:00	2026-08-06 03:45:00
37	112	2026-08-13 02:00:00	2026-08-13 03:30:00
38	112	2026-08-13 02:05:00	2026-08-13 03:35:00
4	112	2026-08-13 02:10:00	2026-08-13 03:40:00
3	112	2026-08-13 02:10:00	2026-08-13 03:40:00
40	126	2026-09-17 01:00:00	2026-09-17 02:00:00
37	113	2026-08-20 02:00:00	2026-08-20 03:30:00
38	113	2026-08-20 02:05:00	2026-08-20 03:35:00
4	113	2026-08-20 02:10:00	2026-08-20 03:40:00
3	113	2026-08-20 02:10:00	2026-08-20 03:40:00
1	80	2025-09-06 17:00:00	2025-09-06 23:00:00
2	80	2025-09-06 17:05:00	2025-09-06 23:05:00
4	80	2025-09-06 17:10:00	2025-09-06 23:10:00
3	80	2025-09-06 17:00:00	2025-09-06 23:00:00
1	82	2025-09-20 17:00:00	2025-09-20 22:00:00
2	82	2025-09-20 17:05:00	2025-09-20 22:05:00
4	82	2025-09-20 17:10:00	2025-09-20 22:10:00
3	82	2025-09-20 17:10:00	2025-09-20 22:10:00
37	101	2026-06-18 02:00:00	2026-06-18 04:00:00
38	101	2026-06-18 02:05:00	2026-06-18 04:05:00
4	101	2026-06-18 02:10:00	2026-06-18 04:10:00
3	101	2026-06-18 02:10:00	2026-06-18 04:10:00
1	92	2026-04-23 02:00:00	2026-04-23 03:30:00
1	84	2025-10-18 17:00:00	2025-10-18 22:00:00
2	84	2025-10-18 17:05:00	2025-10-18 22:05:00
4	84	2025-10-18 17:10:00	2025-10-18 22:10:00
3	84	2025-10-18 17:10:00	2025-10-18 22:10:00
2	92	2026-04-23 02:05:00	2026-04-23 03:35:00
4	92	2026-04-23 02:10:00	2026-04-23 03:40:00
3	92	2026-04-23 02:10:00	2026-04-23 03:40:00
35	83	2025-10-04 18:20:00	2025-10-04 21:20:00
36	83	2025-10-04 18:15:00	2025-10-04 21:15:00
40	130	2026-08-27 01:20:00	2026-08-27 01:29:00
40	123	2026-08-27 01:00:00	2026-08-27 01:09:00
1	94	2026-05-07 02:00:00	2026-05-07 03:30:00
2	94	2026-05-07 02:05:00	2026-05-07 03:35:00
4	94	2026-05-07 02:10:00	2026-05-07 03:40:00
3	94	2026-05-07 02:15:00	2026-05-07 03:40:00
40	133	2026-09-10 01:20:00	2026-09-10 01:29:00
40	125	2026-09-10 01:00:00	2026-09-10 01:09:00
37	117	2026-09-12 17:20:00	2026-09-12 23:20:00
1	86	2026-01-24 18:00:00	2026-01-25 00:00:00
2	86	2026-01-24 18:00:00	2026-01-25 00:00:00
4	86	2026-01-24 18:05:00	2026-01-25 00:05:00
3	86	2026-01-24 18:05:00	2026-01-25 00:05:00
38	117	2026-09-12 17:25:00	2026-09-12 23:25:00
1	97	2026-05-28 02:00:00	2026-05-28 04:00:00
2	97	2026-05-28 02:05:00	2026-05-28 04:05:00
4	97	2026-05-28 02:10:00	2026-05-28 04:10:00
3	97	2026-05-28 02:15:00	2026-05-28 04:10:00
4	117	2026-09-12 17:30:00	2026-09-12 23:30:00
37	106	2026-07-02 02:00:00	2026-07-02 04:00:00
38	106	2026-07-02 02:05:00	2026-07-02 04:05:00
4	106	2026-07-02 02:10:00	2026-07-02 04:10:00
3	106	2026-07-02 02:10:00	2026-07-02 04:10:00
3	117	2026-09-12 17:30:00	2026-09-12 23:30:00
37	121	2026-10-31 17:00:00	2026-10-31 23:00:00
38	121	2026-10-31 17:05:00	2026-10-31 23:05:00
4	121	2026-10-31 17:10:00	2026-10-31 23:10:00
3	121	2026-10-31 17:10:00	2026-10-31 23:10:00
40	136	2026-10-01 01:10:00	2026-10-01 01:19:00
\.


--
-- Data for Name: series; Type: TABLE DATA; Schema: public; Owner: regatta_admin
--

COPY public.series (id, name, active, sort) FROM stdin;
1	Harbor Series	t	0
18	Point Series	t	1
13	Early Spring Windseekers	t	2
14	Late Spring Windseekers	t	3
15	Summer Series Windseekers	t	4
16	Three Hour Tour	t	5
17	Late Summer Windseekers	t	6
19	Lido 14 Late-Summer/Fall Windsday Series	t	7
\.


--
-- Name: auth_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.auth_id_seq', 3, true);


--
-- Name: boat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.boat_id_seq', 117, true);


--
-- Name: person_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.person_id_seq', 141, true);


--
-- Name: race_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.race_id_seq', 137, true);


--
-- Name: raceclass_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.raceclass_id_seq', 72, true);


--
-- Name: raceclasscategory_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.raceclasscategory_id_seq', 40, true);


--
-- Name: raceresults_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.raceresults_id_seq', 2424, true);


--
-- Name: series_id_seq; Type: SEQUENCE SET; Schema: public; Owner: regatta_admin
--

SELECT pg_catalog.setval('public.series_id_seq', 19, true);


--
-- Name: auth auth_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.auth
    ADD CONSTRAINT auth_pkey PRIMARY KEY (id);


--
-- Name: boat boat_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.boat
    ADD CONSTRAINT boat_pkey PRIMARY KEY (id);


--
-- Name: raceresults idx_boat_race_unique; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT idx_boat_race_unique UNIQUE (boat_id, race_id);


--
-- Name: image image_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.image
    ADD CONSTRAINT image_pkey PRIMARY KEY (hash);


--
-- Name: metadata metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.metadata
    ADD CONSTRAINT metadata_pkey PRIMARY KEY (name);


--
-- Name: series nameidx; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.series
    ADD CONSTRAINT nameidx UNIQUE (name);


--
-- Name: orc orc_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.orc
    ADD CONSTRAINT orc_pkey PRIMARY KEY (orc_ref);


--
-- Name: person person_first_last_unique; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_first_last_unique UNIQUE (first, last);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


--
-- Name: race race_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.race
    ADD CONSTRAINT race_pkey PRIMARY KEY (id);


--
-- Name: bracket raceclass_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.bracket
    ADD CONSTRAINT raceclass_pkey PRIMARY KEY (id);


--
-- Name: raceclass raceclasscategory_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceclass
    ADD CONSTRAINT raceclasscategory_pkey PRIMARY KEY (id);


--
-- Name: raceresults raceresults_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT raceresults_pkey PRIMARY KEY (id);


--
-- Name: series series_pkey; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.series
    ADD CONSTRAINT series_pkey PRIMARY KEY (id);


--
-- Name: auth user_name_idx; Type: CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.auth
    ADD CONSTRAINT user_name_idx UNIQUE (user_name);


--
-- Name: boat_skipper_id; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE INDEX boat_skipper_id ON public.boat USING btree (skipper_id);


--
-- Name: image_race_id_unique; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE UNIQUE INDEX image_race_id_unique ON public.image USING btree (race_id);


--
-- Name: orc_boat_id; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE INDEX orc_boat_id ON public.orc USING btree (boat_id);


--
-- Name: race_series_id; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE INDEX race_series_id ON public.race USING btree (series_id);


--
-- Name: racebracketjunction_bracket_id; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE INDEX racebracketjunction_bracket_id ON public.racebracketjunction USING btree (bracket_id);


--
-- Name: racebracketjunction_class_id; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE INDEX racebracketjunction_class_id ON public.racebracketjunction USING btree (class_id);


--
-- Name: racebracketjunction_unique; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE UNIQUE INDEX racebracketjunction_unique ON public.racebracketjunction USING btree (race_id, class_id, bracket_id);


--
-- Name: raceresults_race_id; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE INDEX raceresults_race_id ON public.raceresults USING btree (race_id);


--
-- Name: racetime_race_class_unique; Type: INDEX; Schema: public; Owner: regatta_admin
--

CREATE UNIQUE INDEX racetime_race_class_unique ON public.racetime USING btree (race_id, class_id);


--
-- Name: boat fk_boat_skipper_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.boat
    ADD CONSTRAINT fk_boat_skipper_id__id FOREIGN KEY (skipper_id) REFERENCES public.person(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: bracket fk_bracket_race_class__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.bracket
    ADD CONSTRAINT fk_bracket_race_class__id FOREIGN KEY (race_class) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: image fk_image_race_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.image
    ADD CONSTRAINT fk_image_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orc fk_orc_boat_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.orc
    ADD CONSTRAINT fk_orc_boat_id__id FOREIGN KEY (boat_id) REFERENCES public.boat(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: race fk_race_rc_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.race
    ADD CONSTRAINT fk_race_rc_id__id FOREIGN KEY (rc_id) REFERENCES public.person(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: race fk_race_series_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.race
    ADD CONSTRAINT fk_race_series_id__id FOREIGN KEY (series_id) REFERENCES public.series(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: racebracketjunction fk_racebracketjunction_bracket_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT fk_racebracketjunction_bracket_id__id FOREIGN KEY (bracket_id) REFERENCES public.bracket(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: racebracketjunction fk_racebracketjunction_class_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT fk_racebracketjunction_class_id__id FOREIGN KEY (class_id) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: racebracketjunction fk_racebracketjunction_race_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.racebracketjunction
    ADD CONSTRAINT fk_racebracketjunction_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: raceresults fk_raceresults_boat_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_boat_id__id FOREIGN KEY (boat_id) REFERENCES public.boat(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: raceresults fk_raceresults_bracket_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_bracket_id__id FOREIGN KEY (bracket_id) REFERENCES public.bracket(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: raceresults fk_raceresults_class_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_class_id__id FOREIGN KEY (class_id) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: raceresults fk_raceresults_orc_ref__orc_ref; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_orc_ref__orc_ref FOREIGN KEY (orc_ref) REFERENCES public.orc(orc_ref) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: raceresults fk_raceresults_race_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.raceresults
    ADD CONSTRAINT fk_raceresults_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: racetime fk_racetime_class_cat_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.racetime
    ADD CONSTRAINT fk_racetime_class_cat_id__id FOREIGN KEY (class_id) REFERENCES public.raceclass(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: racetime fk_racetime_race_id__id; Type: FK CONSTRAINT; Schema: public; Owner: regatta_admin
--

ALTER TABLE ONLY public.racetime
    ADD CONSTRAINT fk_racetime_race_id__id FOREIGN KEY (race_id) REFERENCES public.race(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--


