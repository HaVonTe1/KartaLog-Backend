# Change Requests

## 1. Details API

Change Request:
Adding a new API Endpoint for fetching Product-Details.

Goal:
As a client of this backend service I want to call the API with specific ProductID.
The service should respond with a ProductDetailsDTO containing all data available of the requested product.
Just like with the 'search' Endpoint the service should scrape the cardmarket.com website for the desired information.
The fetched data should be used to update the specific product in the database. The current db schema should be fitting.
Just like the existing API the security measures should be applied (circuit breaker, rate limiting and auditing).

API suggestion: GET /collectables/${genre}/${id}

Example:

GET /collectables/pokemon/CM001?lang=de

Currently only "pokemon" is supported for $genre. The QueryParam "lang" is optional with the default: "de".
This example would result in the following Cardmarket URI: http://www.cardmarket.com/

