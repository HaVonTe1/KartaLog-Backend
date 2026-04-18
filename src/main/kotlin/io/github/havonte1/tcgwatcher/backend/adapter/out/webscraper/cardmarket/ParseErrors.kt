package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

sealed interface ParseError {
    val message: String
}

class MissingElement(message: String) : ParseError, RuntimeException(message) {
    override val message: String = message
}

class UnexpectedFormat(message: String) : ParseError, RuntimeException(message) {
    override val message: String = message
}
