## Change Request: extend the database

### Summary

The apps database needs to be populated with the catalog data from all pokemon cards. The db schema needs to be extended and adjusted. The data needs to be loaded so its usable for the app.

### To do

* the project dir contains a file: quicksearch.db
  * this file is a SQLite database with 3 intresting tables:
    * qs_pokemon_card
      * contains all known pokemon cards with:
        * id: internal id:  can be ignored
        * cm_page_id:  cardmarket id for the details page
        * cm_product_id: cardmarket id for the image uri
        * code: the sanatized code of the card
        * name_de, name_en, name_fr: the names of the card in german, english and french
        * set_id: the internal set id for the relation of a card to a set
        * tcgp_id: TCGPlayer ID
    * qs_pokemon_sets
      * id: internal id
      * abbreciation: short for of the sets name
      * cm_product_id: cardmarket id
      * code: the sanatized code of the card
      * name_de, name_en, name_fr: the names of the set in german, english and french
      * official: the amount of cards in the set (official)
      * tcgp_id: TCGPlayer ID
      * total: the amount of cards in the set including all secrets and in-official ones
      * series_id: the internal id of the series this sets belongs to
    * qs_pokemon_series
      * id: the internal series id
      * name_de, name_en, name_fr: the names of the series in german, english and french
* the data of this sqlite db needs to be converted to SQL file which can be automatically loaded into the postreSQL db of the app
* the current schema of tcgwatcherdb.watcher does not include tables for 'series' or 'sets'. This needs to be added via liquibase changesets.
* the current schema of the Product table also need to be extended.
* The SQLite db contains some incomplete data with empty columns. Theese needs to be ignored and skipped.
* 
