package com.mxmariner.regatta.data

enum class FinishCode(val weight: Int) {
    TIME(0),
    HOC(1),
    RET(1),
    DNF(1),
    NSC(2),
}