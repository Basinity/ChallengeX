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
  function valueOf(block, name) {
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

  /* Both subjects are deliberately singular ("any of A or B", "each of A and
     B") so one verb form serves every phrase in the copy table. */
  function triggerSubject(scope) {
    if (scope === 'every_player') {
      return 'anyone';
    }
    if (Array.isArray(scope) && scope.length) {
      return scope.length === 1 ? scope[0] : 'any of ' + joinNames(scope, 'or');
    }
    return '';
  }

  function effectSubject(scope) {
    if (scope === 'per_player') {
      return 'whoever triggers it';
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
    return subject ? subject + ' ' + body : body;
  }

  /* A rule as its two halves, so the page can colour them separately. A half
     with nothing chosen yet says so rather than rendering blank. */
  function ruleLine(rule) {
    var triggerEntry = entries.get(rule.trigger.id);
    return {
      lead: triggerEntry ? triggerEntry.lead : 'When',
      trigger: rule.trigger.id ? clause(rule.trigger, triggerSubject) : 'nothing yet',
      effect: rule.effect.id ? clause(rule.effect, effectSubject) : 'nothing happens'
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
      var value = valueOf(block, param.name);
      if (value.set) {
        parts.push(param.name + '=' + (param.type === 'BOOL' ? 'true' : value.text));
      }
    });
    if (entry.scoped) {
      parts.push('scope=' + (Array.isArray(block.scope) ? '[' + block.scope.join(', ') + ']'
        : (block.scope || 'unset')));
    }
    return parts.join(' · ');
  }

  return {
    render: render,
    triggerSubject: triggerSubject,
    effectSubject: effectSubject,
    scopeNote: scopeNote,
    ruleLine: ruleLine,
    ruleSummary: ruleSummary,
    goalLine: goalLine,
    modifierLine: modifierLine,
    technical: technical,
    capitalize: capitalize
  };
})();
