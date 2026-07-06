package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

class PuppeteerWorkerStrategy(
    workerUrl: String,
) : WorkerStrategy(
        id = "puppeteer-worker",
        displayName = "Puppeteer scraper-worker",
        workerUrl = workerUrl,
    )
