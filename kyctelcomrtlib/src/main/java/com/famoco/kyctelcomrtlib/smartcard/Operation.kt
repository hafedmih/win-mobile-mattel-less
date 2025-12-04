package com.famoco.kyctelcomrtlib.smartcard

enum class Operation {
    IDLE,
    ASK_CARD_NUMBER,
    ASK_IDENTITY,
    ASK_MATCH_ON_CARD
}