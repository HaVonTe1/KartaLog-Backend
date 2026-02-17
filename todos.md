# Change Requests

## 1. Details API

Adding a new API Endpoint for fetching Product-Details.

Goal:
As a client of this backend service I want to call the API for a specific product and get as much details for it as possible. The 'search' endpoint only provide some data from the galary view but every product has a details page.

The service should respond with a ProductDetailsDTO containing all data available of the requested product.
Just like with the 'search' Endpoint the service should scrape the cardmarket.com website for the desired information.
The fetched data should be used to update the specific product in the database. The current db schema will have to be extended.
Just like the existing API the security measures should be applied (circuit breaker, rate limiting and auditing).

API suggestion: GET /collectables/${id}?genre=${genre}&type=${type}&lang=${lang}&setname=${setname}

Example:

GET /collectables/pokemon/CM001?genre=pokement&type=singles&lang=de&setname=setx

Currently only "pokemon" is supported for $genre. The QueryParam "lang" is optional with the default: "de".
This example would result in the following Cardmarket URI: http://www.cardmarket.com/de/pokemon/products/singles/setx/cm001

The pattern is: http://www.cardmarket.com/${lang}/${genre}/products/${type}/${setname}/${id}

Cardmarket should answer with page where i already have a working parsing code:
in src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt
the method: parseProductDetails
it is currently unsused but should work out of the box.

The CardmarketSellOfferDto contains sellOffers. The sellOffers should be persisted in the DB too and transfered in the Response DTO too.
