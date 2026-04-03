package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket


object CardMarketSearchParams {

    fun paginationToParams(page: Int): Map<String, String> {
        return mapOf(
            "site" to page.toString(),
            "perSite" to "100",
        )
    }

    fun combineAll(
        searchString: String,
        page: Int,
    ): Map<String, String> {
        val params = mutableMapOf<String, String>()

        params["searchString"] = searchString
        params.putAll(paginationToParams(page))

        return params
    }
}
