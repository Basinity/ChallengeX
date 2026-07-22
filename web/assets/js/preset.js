/* The challenge model, and the only place that knows the preset JSON format.

   Two shapes are in play and they are deliberately different:

   The working shape, which the builder edits, holds every parameter as the raw
   text sitting in its input. Empty means "not filled in", which is exactly what
   an optional parameter left alone should mean, and it lets a half-typed number
   exist without the model rejecting it mid-keystroke.

   The preset shape is the contract with the mod, produced only at export:
   values typed per the catalog, empty optionals dropped, scope written as
   "per_player" / "every_player" / an array of names, and a block's scope key
   present exactly when its catalog entry is scoped.

   Validation is structural and nothing else. A challenge that is unplayable,
   self-defeating, or repeated four times over is valid here on purpose; the
   only failures are the ones that would make the mod reject the file. */

window.CX = window.CX || {};

window.CX.preset = (function () {
  var entries = window.CX.entries;

  var nextLocalId = 0;
  function localId(prefix) {
    nextLocalId += 1;
    return prefix + '-' + nextLocalId;
  }

  /* ---------- construction ---------- */

  function blankBlock(kind) {
    var block = { uid: localId(kind), id: null, params: {}, scope: null };
    if (kind === 'goal') {
      // How the goal decides the run: win together (anyone or everyone
      // reaching it) or a versus race. The cooperative default matches what
      // the mod assumes when the fields are absent.
      block.mode = 'together';
      block.completion = 'anyone';
    }
    return block;
  }

  function blankChallenge() {
    return { name: '', players: [], rules: [], goal: null, modifiers: [] };
  }

  function blankRule() {
    return { uid: localId('rule'), trigger: blankBlock('trigger'), effect: blankBlock('effect') };
  }

  /* Points a block at a catalog entry, clearing anything the old entry owned. */
  function assign(block, entryId) {
    var entry = entries.get(entryId);
    block.id = entryId;
    block.params = {};
    block.scope = entry && entry.scoped ? null : null;
    return block;
  }

  function copyBlock(block) {
    return {
      uid: localId(block.uid.split('-')[0]),
      id: block.id,
      params: Object.assign({}, block.params),
      scope: Array.isArray(block.scope) ? block.scope.slice() : block.scope
    };
  }

  function copyRule(rule) {
    return { uid: localId('rule'), trigger: copyBlock(rule.trigger), effect: copyBlock(rule.effect) };
  }

  /* ---------- parameter typing ---------- */

  /* The raw input text for one parameter, or '' when it has none. Booleans are
     held as real booleans because a checkbox has no meaningful empty state. */
  function rawValue(block, param) {
    var value = block.params[param.name];
    if (param.type === 'BOOL') {
      return value === true;
    }
    return value === undefined || value === null ? '' : String(value);
  }

  function isBlank(block, param) {
    var value = rawValue(block, param);
    return param.type === 'BOOL' ? value === false : value.trim() === '';
  }

  /* Converts one raw value to its JSON form, or returns an error string. A
     blank value yields undefined, which the caller drops from the output. */
  function typed(block, param) {
    if (isBlank(block, param)) {
      return { value: undefined };
    }
    if (param.type === 'BOOL') {
      return { value: true };
    }
    var text = rawValue(block, param).trim();
    if (param.type === 'STRING') {
      return { value: text };
    }
    var number = Number(text);
    if (text === '' || !isFinite(number)) {
      return { error: 'must be a number' };
    }
    if (param.type === 'INT') {
      if (!Number.isInteger(number)) {
        return { error: 'must be a whole number' };
      }
      return { value: number };
    }
    return { value: number };
  }

  /* ---------- scope ---------- */

  function scopeIsSet(scope) {
    if (scope === 'per_player' || scope === 'every_player') {
      return true;
    }
    return Array.isArray(scope) && scope.length > 0;
  }

  /* An array scope is chosen but empty: the player picked "specific players"
     and then ticked nobody, which the codec rejects. */
  function scopeIsEmptyList(scope) {
    return Array.isArray(scope) && scope.length === 0;
  }

  /* ---------- validation ---------- */

  /* Every problem carries the uid of the card it belongs to, so the summary
     rail can send the reader straight at it. */
  function problem(uid, text) {
    return { uid: uid, text: text };
  }

  function blockProblems(block, where, problems) {
    if (!block.id) {
      return;
    }
    var entry = entries.get(block.id);
    if (!entry) {
      problems.push(problem(block.uid, where + ': unknown id ' + block.id));
      return;
    }
    entry.params.forEach(function (param) {
      if (param.required && isBlank(block, param)) {
        problems.push(problem(block.uid, where + ': ' + entry.name + ' needs ' + param.name));
        return;
      }
      var result = typed(block, param);
      if (result.error) {
        problems.push(problem(block.uid, where + ': ' + param.name + ' ' + result.error));
      }
    });
    if (entry.scoped && !scopeIsSet(block.scope)) {
      problems.push(problem(block.uid, where + ': ' + (scopeIsEmptyList(block.scope)
        ? 'pick at least one player'
        : 'choose who this applies to')));
    }
  }

  function problems(challenge) {
    var found = [];
    if (!challenge.name || !challenge.name.trim()) {
      found.push(problem('challenge-name', 'The challenge needs a name'));
    }
    challenge.rules.forEach(function (rule, index) {
      var where = 'Rule ' + (index + 1);
      if (!rule.trigger.id) {
        found.push(problem(rule.uid, where + ': no trigger chosen'));
      }
      if (!rule.effect.id) {
        found.push(problem(rule.uid, where + ': no effect chosen'));
      }
      blockProblems(rule.trigger, where + ' trigger', found);
      blockProblems(rule.effect, where + ' effect', found);
    });
    if (challenge.goal) {
      blockProblems(challenge.goal, 'Goal', found);
    }
    challenge.modifiers.forEach(function (modifier, index) {
      var entry = entries.get(modifier.id);
      blockProblems(modifier, entry ? entry.name : 'Modifier ' + (index + 1), found);
    });
    return found;
  }

  /* ---------- export ---------- */

  function blockJson(block) {
    var entry = entries.get(block.id);
    var json = { id: block.id };
    var params = {};
    var any = false;
    entry.params.forEach(function (param) {
      var result = typed(block, param);
      if (result.value !== undefined) {
        params[param.name] = result.value;
        any = true;
      }
    });
    if (any) {
      json.params = params;
    }
    if (entry.scoped && scopeIsSet(block.scope)) {
      json.scope = Array.isArray(block.scope) ? block.scope.slice().sort() : block.scope;
    }
    return json;
  }

  /* The preset object exactly as the mod's codec reads it. Keys the codec
     treats as optional are omitted rather than written empty. */
  function toPreset(challenge) {
    var preset = {
      schemaVersion: entries.schemaVersion,
      name: (challenge.name || '').trim()
    };
    if (challenge.rules.length) {
      preset.rules = challenge.rules.map(function (rule) {
        return { trigger: blockJson(rule.trigger), effect: blockJson(rule.effect) };
      });
    }
    if (challenge.goal && challenge.goal.id) {
      preset.goal = blockJson(challenge.goal);
      // Defaults stay off the wire, exactly as the mod's codec writes them:
      // "mode" only for versus, "completion" only for everyone.
      if (challenge.goal.mode === 'versus') {
        preset.goal.mode = 'versus';
      } else if (challenge.goal.completion === 'everyone') {
        preset.goal.completion = 'everyone';
      }
    }
    if (challenge.modifiers.length) {
      preset.modifiers = challenge.modifiers.map(blockJson);
    }
    return preset;
  }

  function stringify(challenge) {
    return JSON.stringify(toPreset(challenge), null, 2) + '\n';
  }

  /* ---------- import ---------- */

  function readBlock(raw, kind) {
    var block = blankBlock(kind);
    if (!raw || typeof raw !== 'object' || typeof raw.id !== 'string') {
      return block;
    }
    block.id = raw.id;
    var entry = entries.get(raw.id);
    if (raw.params && typeof raw.params === 'object') {
      Object.keys(raw.params).forEach(function (key) {
        var value = raw.params[key];
        block.params[key] = typeof value === 'boolean' ? value : String(value);
      });
    }
    if (entry && entry.scoped) {
      if (raw.scope === 'per_player' || raw.scope === 'every_player') {
        block.scope = raw.scope;
      } else if (Array.isArray(raw.scope)) {
        block.scope = raw.scope.filter(function (name) { return typeof name === 'string'; });
      }
    }
    return block;
  }

  /* Reads a preset back into the working shape. Throws with a readable reason
     rather than returning something half-built, because both callers (the file
     drop and the shared link) want to show the reason and stop. */
  function fromPreset(raw) {
    if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
      throw new Error('the preset must be a JSON object');
    }
    if (typeof raw.schemaVersion !== 'number') {
      throw new Error('missing or non-numeric schemaVersion');
    }
    if (raw.schemaVersion > entries.schemaVersion) {
      throw new Error('preset written for schema v' + raw.schemaVersion
        + ', this site reads up to v' + entries.schemaVersion);
    }
    var challenge = blankChallenge();
    challenge.name = typeof raw.name === 'string' ? raw.name : '';
    if (Array.isArray(raw.rules)) {
      challenge.rules = raw.rules.map(function (rule) {
        return {
          uid: localId('rule'),
          trigger: readBlock(rule && rule.trigger, 'trigger'),
          effect: readBlock(rule && rule.effect, 'effect')
        };
      });
    }
    if (raw.goal) {
      challenge.goal = readBlock(raw.goal, 'goal');
      challenge.goal.mode = raw.goal.mode === 'versus' ? 'versus' : 'together';
      challenge.goal.completion = challenge.goal.mode === 'together'
        && raw.goal.completion === 'everyone' ? 'everyone' : 'anyone';
    }
    if (Array.isArray(raw.modifiers)) {
      challenge.modifiers = raw.modifiers.map(function (modifier) {
        return readBlock(modifier, 'modifier');
      });
    }
    challenge.players = rosterOf(challenge);
    return challenge;
  }

  function parse(text) {
    var raw;
    try {
      raw = JSON.parse(text);
    } catch (error) {
      throw new Error('not valid JSON: ' + error.message);
    }
    return fromPreset(raw);
  }

  /* ---------- roster ---------- */

  /* The roster is a builder convenience, not part of the preset, so a preset
     coming back in rebuilds it from the names its scopes actually mention. */
  function rosterOf(challenge) {
    var names = [];
    function collect(block) {
      if (block && Array.isArray(block.scope)) {
        block.scope.forEach(function (name) {
          if (names.indexOf(name) < 0) {
            names.push(name);
          }
        });
      }
    }
    challenge.rules.forEach(function (rule) {
      collect(rule.trigger);
      collect(rule.effect);
    });
    challenge.modifiers.forEach(collect);
    return names.sort();
  }

  /* A filename the mod will accept: no separators, no dots, safe to type into
     chat after /challengex import. */
  function slug(name) {
    var text = (name || '').trim().toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    return text || 'challenge';
  }

  return {
    blankChallenge: blankChallenge,
    blankRule: blankRule,
    blankBlock: blankBlock,
    assign: assign,
    copyBlock: copyBlock,
    copyRule: copyRule,
    rawValue: rawValue,
    isBlank: isBlank,
    scopeIsSet: scopeIsSet,
    problems: problems,
    toPreset: toPreset,
    stringify: stringify,
    fromPreset: fromPreset,
    parse: parse,
    rosterOf: rosterOf,
    slug: slug
  };
})();
