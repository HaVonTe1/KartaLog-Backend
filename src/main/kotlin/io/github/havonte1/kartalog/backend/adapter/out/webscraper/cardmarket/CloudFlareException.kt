package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpServerErrorException

class CloudFlareException(
    status: HttpStatusCode,
) : HttpServerErrorException(status)
