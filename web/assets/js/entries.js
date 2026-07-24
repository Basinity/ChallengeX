/* The catalog as the rest of the site sees it: the generated structure joined
   to the hand-written copy, indexed by id.

   Nothing downstream reads window.CX_CATALOG or window.CX_COPY directly, so
   the join happens once and an entry always arrives complete. An entry present
   in the catalog but absent from the copy still renders, under a name derived
   from its id. */

window.CX = window.CX || {};

window.CX.entries = (function () {
  var catalog = window.CX_CATALOG;
  var copy = window.CX_COPY || {};

  if (!catalog) {
    throw new Error('catalog.js did not load: run ./gradlew :core:exportCatalog');
  }

  var KINDS = ['trigger', 'effect', 'modifier'];
  var LISTS = { trigger: 'triggers', effect: 'effects', modifier: 'modifiers' };

  /* "trigger.item_picked_up" with no copy entry becomes "Item picked up". */
  function nameFromId(id) {
    var key = id.slice(id.indexOf('.') + 1).replace(/_/g, ' ');
    return key.charAt(0).toUpperCase() + key.slice(1);
  }

  var byId = {};
  var byKind = {};

  KINDS.forEach(function (kind) {
    byKind[kind] = (catalog[LISTS[kind]] || []).map(function (raw) {
      var text = copy[raw.id] || {};
      var entry = {
        id: raw.id,
        kind: kind,
        scoped: raw.scoped,
        params: raw.params,
        name: text.name || nameFromId(raw.id),
        blurb: text.blurb || '',
        phrase: text.phrase || '',
        detail: text.detail || '',
        // Reader-facing labels for parameters restricted to a closed set,
        // keyed by parameter name then by value.
        values: text.values || {},
        // Acts on the run itself rather than on the world, so it leads its
        // kind and is drawn apart from the rest.
        runControl: text.runControl === true,
        // Most triggers read as "When <subject> <phrase>"; a few supply their own opening.
        lead: text.lead === undefined ? 'When' : text.lead
      };
      byId[entry.id] = entry;
      return entry;
    });
    // Stable, so everything else keeps the catalog's own order.
    byKind[kind].sort(function (a, b) {
      return (b.runControl ? 1 : 0) - (a.runControl ? 1 : 0);
    });
  });

  function requiredParams(entry) {
    return entry.params.filter(function (param) { return param.required; });
  }

  return {
    catalogVersion: catalog.catalogVersion,
    schemaVersion: catalog.schemaVersion,
    kinds: KINDS,

    all: function (kind) { return byKind[kind] || []; },
    get: function (id) { return byId[id] || null; },
    count: function (kind) { return (byKind[kind] || []).length; },
    total: function () {
      return KINDS.reduce(function (sum, kind) { return sum + byKind[kind].length; }, 0);
    },

    /* Which scope values this side accepts. */
    scopes: function (kind) { return (catalog.scopes && catalog.scopes[kind]) || []; },

    requiredParams: requiredParams,

    /* Free-text search over the name, the id, and the blurb. */
    search: function (kind, query) {
      var needle = (query || '').trim().toLowerCase();
      var list = byKind[kind] || [];
      if (!needle) {
        return list;
      }
      return list.filter(function (entry) {
        return entry.name.toLowerCase().indexOf(needle) >= 0
          || entry.id.toLowerCase().indexOf(needle) >= 0
          || entry.blurb.toLowerCase().indexOf(needle) >= 0;
      });
    }
  };
})();
