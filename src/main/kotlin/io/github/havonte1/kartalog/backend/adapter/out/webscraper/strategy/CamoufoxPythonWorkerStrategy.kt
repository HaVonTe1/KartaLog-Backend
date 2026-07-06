package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

class CamoufoxPythonWorkerStrategy(
    workerUrl: String,
) : WorkerStrategy(
        id = "camoufox-python-worker",
        displayName = "Camoufox Python worker (playwright-captcha)",
        workerUrl = workerUrl,
    )
