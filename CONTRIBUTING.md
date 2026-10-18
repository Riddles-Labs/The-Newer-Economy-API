# Contributing to the TNE API

API fixes, documentation improvements, and integration examples are welcome.
Run `./mvnw clean verify` (or `.\mvnw.cmd clean verify` on Windows) with JDK 25.
Also build the example with `./mvnw -f docs/plugin-api-example/pom.xml clean verify`
when changing documentation, dependencies, or examples.

Keep public API types independent of Bukkit, platform implementations, SQL, and
TNECORE internals. Keep financial amounts precise with `BigDecimal`. Document
asynchronous results, idempotency, permission ownership, and recovery behavior.
Contract changes need matching runtime implementation and compatibility review.
Adding a method here alone does not add that feature to an installed server.

## Maintainer updates

This is the public API source for TNE 1.0.64. Its initial `src/main` and `src/test`
files are exact copies of the API module from the matching runtime release:

- Runtime source revision: `182769eb9cb6b0acf33b8237488dcc7963f00a6e`.
- API module Git tree: `88e4dc8bef5e70ada1467c84baef563889776cb2`.

For a new release, synchronize only the reviewed API source/tests and the developer
guides/example from that release. Update the standalone POM, dependency examples,
version notes, and source revision together. Preserve this repository's MIT license,
standalone build, and CI. Never copy private Git history or runtime implementation
modules into this repository.

Publish matching runtime and API versions through the existing release process.
Previously published Maven coordinates and version directories remain immutable.
Keep the existing public Maven URL working when changing developer-facing hosting.
