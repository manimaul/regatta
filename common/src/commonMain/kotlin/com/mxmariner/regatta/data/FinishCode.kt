package com.mxmariner.regatta.data

/**
 * How a boat's race ended. The weight orders the non-finishers in a race report (D6).
 * Stored by name in `raceresults.finish_code`.
 */
enum class FinishCode(val weight: Int) {
    TIME(0),
    HOC(1),
    RET(1),
    DNF(1),
    NSC(2),
    /** Did not start (GSI 1.2 late entry, 8.6 started too late): starters + 1, the same as no record. */
    DNS(3),
    /** Disqualified (GSI 4.3-4.5, 6.4, 6.5, 8.9, 12.3.4): starters + 2. */
    DSQ(4),
    /** RC volunteer (GSI 1.4.1.2): scored in the series standings as the average of the boat's other races. */
    DNS_RC(5);

    /** GSI 14.1 / D3: every entry except an RC volunteer or a DNS started the race. */
    val isStarter: Boolean
        get() = this != DNS_RC && this != DNS

    /** GSI 14.1 / D3: timed finishers and HOC boats are finishers. */
    val isFinisher: Boolean
        get() = this == TIME || this == HOC
}