/* The game data as the rest of the site sees it: id-to-name suggestion
   sources for parameters that take a game id, so a player types "diamond sw",
   picks "Diamond Sword", and the preset stores minecraft:diamond_sword without
   anyone ever reading an id.

   The generated gamedata.js ships entries as [id] or [id, name]: a missing
   name means the id derives its own ("diamond_sword" reads back as "Diamond
   Sword"), which is what keeps the file small. derivedName here must stay
   identical to the Java exporter's copy, or omitted names would render
   differently than the export assumed.

   Suggestions never restrict anything. Text that matches no entry is kept as
   typed, so a modded or future id exports exactly as it always did. */

window.CX = window.CX || {};

window.CX.suggest = (function () {
  var data = window.CX_GAMEDATA;

  /* Keyword sets (weather, time, effect kind) are bare vocabulary the mod
     matches literally, not registry ids: their stored value must never gain a
     minecraft: namespace. gamedata.js names them so this file need not guess. */
  var keyword = {};
  ((data && data.keywords) || []).forEach(function (name) {
    keyword[name] = true;
  });

  /* "story/mine_diamond" -> "Mine Diamond"; "ambient.cave" -> "Ambient Cave". */
  function derivedName(id) {
    var tail = id.slice(id.lastIndexOf('/') + 1);
    return tail.replace(/[._]/g, ' ').split(' ').filter(Boolean).map(function (word) {
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
    }).join(' ');
  }

  /* Sources index lazily: [{ id, bare, name, lowerName, lowerBare }] */
  var indexed = {};

  function source(name) {
    if (indexed[name]) {
      return indexed[name];
    }
    var rows = (data && data.sources && data.sources[name]) || [];
    indexed[name] = rows.map(function (row) {
      var bare = row[0];
      var id = keyword[name] || bare.indexOf(':') >= 0 ? bare : 'minecraft:' + bare;
      var display = row.length > 1 ? row[1] : derivedName(bare);
      return {
        id: id,
        bare: bare,
        name: display,
        lowerName: display.toLowerCase(),
        lowerBare: bare.toLowerCase()
      };
    });
    return indexed[name];
  }

  function has(name) {
    return source(name).length > 0;
  }

  /* Strips the default namespace so stored values match the shipped bare ids. */
  function bareOf(value) {
    return value.indexOf('minecraft:') === 0 ? value.slice('minecraft:'.length) : value;
  }

  /* The display name for a stored value, or null when nothing knows it. */
  function displayName(name, value) {
    var text = String(value == null ? '' : value).trim();
    if (!text) {
      return null;
    }
    var bare = bareOf(text).toLowerCase();
    var list = source(name);
    for (var i = 0; i < list.length; i++) {
      if (list[i].lowerBare === bare) {
        return list[i].name;
      }
    }
    return null;
  }

  /* An exact (case-insensitive) match on a display name or an id, as the blur
     handler uses to snap free-typed text onto the entry it clearly means. */
  function resolve(name, text) {
    var query = String(text == null ? '' : text).trim().toLowerCase();
    if (!query) {
      return null;
    }
    var bare = bareOf(query);
    var list = source(name);
    for (var i = 0; i < list.length; i++) {
      if (list[i].lowerName === query || list[i].lowerBare === bare) {
        return list[i];
      }
    }
    return null;
  }

  /* Ranked matches: name prefix beats id prefix beats name substring beats id
     substring, alphabetical within a rank. An empty query lists from the top,
     which is what makes short keyword sources browsable on focus. */
  function search(name, query, limit) {
    var needle = String(query == null ? '' : query).trim().toLowerCase();
    var list = source(name);
    var max = limit || 8;
    if (!needle) {
      return list.slice(0, max);
    }
    var ranked = [];
    for (var i = 0; i < list.length; i++) {
      var entry = list[i];
      var rank = entry.lowerName.indexOf(needle) === 0 ? 0
        : entry.lowerBare.indexOf(needle) === 0 ? 1
          : entry.lowerName.indexOf(needle) >= 0 ? 2
            : entry.lowerBare.indexOf(needle) >= 0 ? 3 : -1;
      if (rank >= 0) {
        ranked.push({ rank: rank, entry: entry });
      }
    }
    ranked.sort(function (a, b) {
      return a.rank - b.rank || (a.entry.lowerName < b.entry.lowerName ? -1 : 1);
    });
    return ranked.slice(0, max).map(function (item) { return item.entry; });
  }

  return {
    gameVersion: data ? data.gameVersion : null,
    derivedName: derivedName,
    has: has,
    bareOf: bareOf,
    displayName: displayName,
    resolve: resolve,
    search: search
  };
})();
