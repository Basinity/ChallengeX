/* Turns a composed challenge into English.

   The shared-link page is the most-visited page on the site and most of its
   visitors have never heard of a "scope", so it reads out rules as sentences
   rather than as a table of ids. The ids are still there, folded away behind a
   disclosure, because they are the technical truth and someone eventually
   wants them.

   Everything here returns plain strings. Rendering them into the page is the
   caller's job, and every caller does it through textContent, because a shared
   link is input from a stranger. */

window.CX = window.CX || {};

window.CX.phrase = (function () {
  var entries = window.CX.entries;

  /* ---------- the template mini-language ---------- */

  /* A parameter as the template sees it: whether it counts as filled in, and
     what it prints. A true boolean is filled in but prints nothing, so it can
     gate a chunk ("[{baby} baby]") without appearing in the sentence. */
  function rawValueOf(block, name) {
    var value = block.params ? block.params[name] : undefined;
    if (value === true) {
      return { set: true, text: '' };
    }
    if (value === false || value === undefined || value === null) {
      return { set: false, text: '' };
    }
    var text = String(value).trim();
    return { set: text !== '', text: text };
  }

  function valueOf(block, name) {
    var value = rawValueOf(block, name);
    return value.set ? { set: true, text: display(block, name, value.text) } : value;
  }

  /* Sources whose values are ordinary words a phrase weaves into prose
     ("negative", "rain", "noon"): capitalizing those mid-sentence would read
     wrong, so they print as written. */
  var PROSE_SOURCES = { player: true, weather: true, time: true, effect_kind: true };

  /* An id-valued parameter reads by its display name ("Zombie", "Diamonds!"),
     since a sentence is exactly where an id should not appear. Free text
     prints as written; prose keywords and ids nothing knows print as written
     minus any minecraft: prefix, so a sentence never says "minecraft:"; the
     raw ids stay available behind the technical disclosure. */
  function display(block, name, text) {
    var suggest = window.CX.suggest;
    var entry = suggest && entries.get(block.id);
    if (!entry) {
      return text;
    }
    for (var i = 0; i < entry.params.length; i++) {
      var param = entry.params[i];
      if (param.name === name) {
        if (!param.suggests) {
          return text;
        }
        if (PROSE_SOURCES[param.suggests]) {
          return suggest.bareOf(text);
        }
        return suggest.displayName(param.suggests, text) || suggest.bareOf(text);
      }
    }
    return text;
  }

  function render(template, block) {
    if (!template) {
      return '';
    }
    // Drop any [chunk] holding a bare {placeholder} that has no value.
    var out = template.replace(/\[([^\[\]]*)\]/g, function (_, inner) {
      var keep = true;
      inner.replace(/\{([a-z0-9_]+)\}/g, function (match, name) {
        if (!valueOf(block, name).set) {
          keep = false;
        }
        return match;
      });
      return keep ? inner : '';
    });
    // Then substitute what survived, honouring {name?fallback}.
    out = out.replace(/\{([a-z0-9_]+)(?:\?([^}]*))?\}/g, function (_, name, fallback) {
      var value = valueOf(block, name);
      if (value.set) {
        return value.text;
      }
      return fallback === undefined ? '' : fallback;
    });
    return out.replace(/\s+/g, ' ').trim();
  }

  /* ---------- subjects ---------- */

  function joinNames(names, joiner) {
    if (names.length === 1) {
      return names[0];
    }
    if (names.length === 2) {
      return names[0] + ' ' + joiner + ' ' + names[1];
    }
    return names.slice(0, -1).join(', ') + ' ' + joiner + ' ' + names[names.length - 1];
  }

  /* Subjects are singular ("any of A or B", "each of A and B") so the copy
     table's third-person-singular phrases serve them all unchanged. The one
     exception is "they", the per-player effect subject, which takes a plural
     verb: the clause renderer sheds the leading verb's -s for it below. */
  function triggerSubject(scope) {
    if (scope === 'every_player') {
      return 'someone';
    }
    if (Array.isArray(scope) && scope.length) {
      return scope.length === 1 ? scope[0] : 'any of ' + joinNames(scope, 'or');
    }
    return '';
  }

  function effectSubject(scope) {
    if (scope === 'per_player') {
      return 'they';
    }
    if (scope === 'every_player') {
      return 'everyone';
    }
    if (Array.isArray(scope) && scope.length) {
      return scope.length === 1 ? scope[0] : 'each of ' + joinNames(scope, 'and');
    }
    return '';
  }

  /* The short scope note beside a modifier. */
  function scopeNote(scope) {
    if (scope === 'every_player') {
      return 'everyone';
    }
    if (Array.isArray(scope) && scope.length) {
      return 'only ' + joinNames(scope, 'and');
    }
    return '';
  }

  function capitalize(text) {
    return text ? text.charAt(0).toUpperCase() + text.slice(1) : text;
  }

  /* ---------- clauses ---------- */

  /* "they" takes a plural verb and every phrase is written third-person
     singular, so the leading verb sheds its -s: gets → get, is healed → are
     healed, has their → have their, catches fire → catch fire, dies → die. */
  var PLURAL_VERBS = { is: 'are', has: 'have', was: 'were', does: 'do', goes: 'go', dies: 'die' };

  function pluralizeLead(body) {
    var space = body.indexOf(' ');
    var head = space < 0 ? body : body.slice(0, space);
    var rest = space < 0 ? '' : body.slice(space);
    var plural = PLURAL_VERBS[head];
    if (!plural) {
      if (/(ch|sh|ss|x|z)es$/.test(head)) {
        plural = head.slice(0, -2);
      } else if (/[^aeiou]ies$/.test(head)) {
        plural = head.slice(0, -3) + 'y';
      } else if (/[^s]s$/.test(head)) {
        plural = head.slice(0, -1);
      } else {
        plural = head;
      }
    }
    return plural + rest;
  }

  function clause(block, subjectFor) {
    var entry = entries.get(block.id);
    if (!entry) {
      return block.id ? block.id : 'nothing yet';
    }
    var body = render(entry.phrase, block) || entry.name.toLowerCase();
    if (!entry.scoped) {
      return body;
    }
    var subject = subjectFor(block.scope);
    if (subject === 'they') {
      body = pluralizeLead(body);
    }
    return subject ? subject + ' ' + body : body;
  }

  /* A rule as its two halves, so the page can colour them separately. A half
     with nothing chosen yet says so rather than rendering blank. A playerless
     trigger has nobody who "triggers it", and the engine sends a per-player
     effect to everyone there, so the sentence says everyone too. */
  function ruleLine(rule) {
    var triggerEntry = entries.get(rule.trigger.id);
    var playerless = Boolean(triggerEntry && !triggerEntry.scoped);
    var subjectFor = playerless
      ? function (scope) { return effectSubject(scope === 'per_player' ? 'every_player' : scope); }
      : effectSubject;
    return {
      lead: triggerEntry ? triggerEntry.lead : 'When',
      trigger: rule.trigger.id ? clause(rule.trigger, triggerSubject) : 'nothing yet',
      effect: rule.effect.id ? clause(rule.effect, subjectFor) : 'nothing happens'
    };
  }

  /* The whole rule as one sentence, for compact places like a card summary. */
  function ruleSummary(rule) {
    var line = ruleLine(rule);
    var opening = line.lead ? line.lead + ' ' + line.trigger : capitalize(line.trigger);
    return capitalize(opening) + ' → ' + line.effect;
  }

  function goalLine(goal) {
    var entry = entries.get(goal.id);
    if (!entry) {
      return goal.id || 'No goal';
    }
    return render(entry.phrase, goal) || entry.name;
  }

  /* The muted note beside the goal for a non-default decision mode; the
     win-together-anyone default stays silent, as it always has. */
  function goalModeNote(goal) {
    if (goal.mode === 'versus') {
      return 'versus — first player to finish wins';
    }
    if (goal.mode === 'together' && goal.completion === 'everyone') {
      return 'everyone must finish';
    }
    return '';
  }

  function modifierLine(modifier) {
    var entry = entries.get(modifier.id);
    if (!entry) {
      return { name: modifier.id || 'Unknown modifier', detail: '', scope: '' };
    }
    return {
      name: entry.name,
      detail: render(entry.detail, modifier),
      scope: entry.scoped ? scopeNote(modifier.scope) : ''
    };
  }

  function technical(block) {
    var entry = entries.get(block.id);
    if (!entry) {
      return block.id || '';
    }
    var parts = [block.id];
    entry.params.forEach(function (param) {
      // Raw on purpose: this line is the technical truth, ids and all.
      var value = rawValueOf(block, param.name);
      if (value.set) {
        parts.push(param.name + '=' + (param.type === 'BOOL' ? 'true' : value.text));
      }
    });
    if (entry.scoped) {
      parts.push('scope=' + (Array.isArray(block.scope) ? '[' + block.scope.join(', ') + ']'
        : (block.scope || 'unset')));
    }
    return parts.join(' | ');
  }

  return {
    render: render,
    triggerSubject: triggerSubject,
    effectSubject: effectSubject,
    scopeNote: scopeNote,
    ruleLine: ruleLine,
    ruleSummary: ruleSummary,
    goalLine: goalLine,
    goalModeNote: goalModeNote,
    modifierLine: modifierLine,
    technical: technical,
    capitalize: capitalize
  };
})();
