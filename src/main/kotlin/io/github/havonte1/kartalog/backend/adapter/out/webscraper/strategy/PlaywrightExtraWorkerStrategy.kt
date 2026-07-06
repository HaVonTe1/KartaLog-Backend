package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

class PlaywrightExtraWorkerStrategy(
    workerUrl: String,
) : WorkerStrategy(
        id = "playwright-extra-worker",
        displayName = "playwright-extra scraper-worker",
        workerUrl = workerUrl,
    )
