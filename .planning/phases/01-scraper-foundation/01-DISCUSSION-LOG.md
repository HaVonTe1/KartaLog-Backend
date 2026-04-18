# Phase 1: Scraper Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-03
**Phase:** 01-scraper-foundation
**Areas discussed:** Language handling, Content parser architecture

---

## Language Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Enum in domain | Define a sealed class or enum of supported CardMarket locales (de, en, fr, it, es, pt, nl, pl) in the domain layer — type-safe, validated at boundaries | ✓ |
| String validation at adapter | Accept strings, validate against a config list at the scraper boundary — simpler but less type-safe | |
| Config-driven locale map | External YAML/JSON config mapping locale codes to labels and URL segments — flexible but adds config overhead | |

**User's choice:** Enum in domain — sealed class or enum for type-safe locale handling

**Notes:** Locale enum covers all CardMarket languages, validated at adapter boundary

---

## Content Parser Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| Translation map | Central map of label translations per language — one parser reads from config instead of hardcoded strings | ✓ |
| CSS selector-based | Parse by DOM structure/attributes instead of text labels — more resilient to language changes and site updates | |
| Language-specific parsers | Separate parser per language — clear but duplicated logic | |

**User's choice:** Translation map — Kotlin data class with nested maps per language

**Notes:**
- Translation map covers both search page labels (pagination "von/of/de") and detail page labels ("Rarität", "Erschienen", "ab", "Preis-Trend")
- Monolithic CardMarketContentParser (286 lines) split into two focused parsers: gallery and details
- Parser returns `Result<Dto>` with typed errors (MissingElement, UnexpectedFormat)

---

## the agent's Discretion

- Exact enum naming conventions and package placement for the locale sealed class
- Specific structure of the translation map data class (flat vs nested)
- Whether to use Kotlin sealed interfaces or sealed classes for parse errors

## Deferred Ideas

- Genre-specific URL patterns for Yu-Gi-Oh and MTG — v2 work
- Additional CardMarket filter/sort parameter handling — Phase 2
- Product detail enrichment beyond CardMarket data — out of scope
