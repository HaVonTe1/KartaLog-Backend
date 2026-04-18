package io.github.havonte1.tcgwatcher.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TcgWatcherApplication

fun main(args: Array<String>) {
    runApplication<TcgWatcherApplication>(*args)
}
