# ChallengeX web builder

The companion site. It composes a challenge and exports it as a preset JSON file or as a shareable link; the mod imports either one. Destined for `challengexmc.com`.

Static and client-side only: no backend, no database, no network requests, no build step. Opening `index.html` straight off disk works, which is also why the catalog ships as a script assigning a global rather than as a `.json` a browser would refuse to `fetch` over `file://`.

## Layout

```
index.html          the landing page, and the shared-link view at /#c=<preset>
build/index.html    the builder, so its served URL is /build
privacy/index.html  the privacy policy, served as /privacy
assets/css          fonts.css (self-hosted faces) and app.css (the whole system)
assets/js           see below
test/run.js         headless checks, and the fixtures :core:test parses
```

Served over http(s), no URL says a filename: both pages are directory indexes, so links read `/` and `/build`. Opened off disk the filenames stay, because `file://` has no directory index; `link.js` picks per protocol at runtime.
The root `.gitignore` carries an exception for `web/build/`, which its blanket `build/` rule would otherwise drop.

The scripts, in load order and by responsibility:

| file | what it owns |
| --- | --- |
| `catalog.js` | **generated** from the mod's registries; never edit |
| `copy.js` | display names, picker blurbs, and the English phrase per entry |
| `entries.js` | the two joined and indexed by id |
| `ui.js` | DOM helpers, toast, download, clipboard |
| `preset.js` | the challenge model, validation, and the preset JSON format |
| `phrase.js` | composing entries into readable sentences |
| `link.js` | base64url encoding of a preset into the URL fragment |
| `builder.js` | the builder page |
| `share.js` | the shared-link page |
| `home.js` | the landing page, and the landing-or-shared decision |

## Regenerating the catalog

The builder renders every form from `assets/js/catalog.js`, which is generated out of `core`'s registries so the site cannot drift from the mod. After adding or changing a catalog entry:

```
./gradlew :core:exportCatalog
```

Nothing about an individual entry is hardcoded in a screen. A new trigger appears in the picker, gets a parameter form, and gets a scope control with no change here. It renders without a blurb until one is added to `copy.js`.

## Tests

```
node web/test/run.js     # the model, validation, links, and phrasing
./gradlew :core:test     # includes PresetContractTest
```

`run.js` needs no dependencies. Besides asserting, it writes real site exports into `test/out/`, which `PresetContractTest` in `:core` then parses with the mod's actual `PresetCodec`. That pairing is the point: the site and the mod are separate artifacts in different languages sharing one JSON contract with no compiler between them, so the contract is tested from both ends.
Those fixtures are committed, and the root `.gitignore` carries an exception to keep them. Re-run `run.js` after changing anything about the export, and commit the fixture diff along with it.

## Notes

- The roster (the player names a specific-player scope picks from) is a builder convenience and is not part of the preset. A preset coming back in rebuilds it from the names its scopes actually mention.
- The builder never rejects a combination for being unplayable, self-defeating, or repeated. Only structural problems block an export: a missing required parameter, a missing required scope, or a rule with only one half.
- A shared link is input from a stranger, so every page builds its content as DOM nodes through `ui.el` and never as an HTML string.
- Fonts are self-hosted rather than linked from Google, so the page makes no third-party request and keeps working offline.
